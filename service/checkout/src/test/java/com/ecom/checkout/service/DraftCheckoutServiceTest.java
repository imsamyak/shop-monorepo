package com.ecom.checkout.service;

import com.ecom.checkout.domain.entity.Cart;
import com.ecom.checkout.domain.exception.CheckoutItemsRequiredException;
import com.ecom.checkout.port.in.usecase.dto.command.DraftCheckoutCmd;
import com.ecom.checkout.port.in.usecase.dto.result.RecordedCheckout;
import com.ecom.checkout.port.in.usecase.record.RecordCheckouts;
import com.ecom.checkout.port.out.client.catalog.LoadSkuPricePort;
import com.ecom.checkout.port.out.client.inventory.ReleaseStockPort;
import com.ecom.checkout.port.out.client.inventory.ReserveStockPort;
import com.ecom.checkout.port.out.persistence.LoadCartPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.assertj.core.api.Assertions.assertThat;

class DraftCheckoutServiceTest {

   private final LoadSkuPricePort prices = mock(LoadSkuPricePort.class);
   private final ReserveStockPort reserve = mock(ReserveStockPort.class);
   private final ReleaseStockPort release = mock(ReleaseStockPort.class);
   private final RecordCheckouts.RecordDraft record = mock(RecordCheckouts.RecordDraft.class);
   private final LoadCartPort loadCart = mock(LoadCartPort.class);
   private final DraftCheckoutService service = new DraftCheckoutService(prices, reserve, release, loadCart, record);

   private final UUID reservationId = UUID.randomUUID();

   @BeforeEach
   void setUp() {
      when(prices.loadPrices(anyCollection())).thenReturn(
            Map.of(2L, new LoadSkuPricePort.SkuPrice(2L, UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN)));
      when(reserve.reserve(anyString(), anyLong(), anyList()))
            .thenReturn(List.of(new ReserveStockPort.ReservedStock(2L, reservationId)));
   }


   private DraftCheckoutCmd cmd() {
      return new DraftCheckoutCmd(UUID.randomUUID(), List.of(new DraftCheckoutCmd.ItemCmd(2L, 1)), 900);
   }

   @Test
   void releasesTheReservationsWhenRecordingTheCheckoutFails() {
      when(record.execute(any())).thenThrow(new IllegalStateException("database down"));

      assertThatThrownBy(() -> service.execute(cmd())).isInstanceOf(IllegalStateException.class);

      @SuppressWarnings("unchecked")
      ArgumentCaptor<Collection<UUID>> released = ArgumentCaptor.forClass(Collection.class);
      verify(release).release(released.capture());
      assertThat(released.getValue()).containsExactly(reservationId);
   }

   @Test
   void theOriginalFailureWinsEvenIfTheCompensationAlsoFails() {
      when(record.execute(any())).thenThrow(new IllegalStateException("database down"));
      doThrow(new RuntimeException("inventory down too")).when(release).release(anyCollection());

      assertThatThrownBy(() -> service.execute(cmd()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("database down");
   }

   @Test
   void withoutItemsTheDraftIsMadeFromTheCart() {
      when(record.execute(any())).thenReturn(new RecordedCheckout(null, true));
      UUID customer = UUID.randomUUID();
      Cart cart = Cart.empty(customer);
      cart.add(2L, 3);
      when(loadCart.loadByCustomerId(customer)).thenReturn(Optional.of(cart));

      service.execute(new DraftCheckoutCmd(customer, List.of(), 900));

      verify(reserve).reserve(anyString(), anyLong(), eq(List.of(new ReserveStockPort.StockLine(2L, 3))));
   }

   @Test
   void explicitItemsAreUsedAndTheCartIsNotRead() {
      when(record.execute(any())).thenReturn(new RecordedCheckout(null, true));
      service.execute(cmd());

      verifyNoInteractions(loadCart);
   }

   @Test
   void withNeitherItemsNorACartNothingIsCalled() {
      UUID customer = UUID.randomUUID();
      when(loadCart.loadByCustomerId(customer)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.execute(new DraftCheckoutCmd(customer, null, 900)))
            .isInstanceOf(CheckoutItemsRequiredException.class);
      verifyNoInteractions(prices, reserve, release, record);
   }

   @Test
   void nothingIsReservedWhenTheSkuIsUnknown() {
      when(prices.loadPrices(anyCollection())).thenReturn(Map.of());

      assertThatThrownBy(() -> service.execute(cmd()))
            .isInstanceOf(com.ecom.checkout.domain.exception.SkuNotAvailableException.class);
      verifyNoInteractions(reserve, release, record);
   }
}
