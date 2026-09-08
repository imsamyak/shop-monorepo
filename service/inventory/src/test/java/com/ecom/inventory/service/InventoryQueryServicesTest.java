package com.ecom.inventory.service;

import com.ecom.inventory.domain.entity.InventoryItem;
import com.ecom.inventory.domain.entity.InventoryReservation;
import com.ecom.inventory.domain.entity.ReservationStatus;
import com.ecom.inventory.domain.exception.item.InventoryNotFoundException;
import com.ecom.inventory.domain.exception.reservation.ReservationNotFoundException;
import com.ecom.inventory.port.in.usecase.item.dto.query.GetInventoriesQuery;
import com.ecom.inventory.port.in.usecase.item.dto.query.GetInventoryQuery;
import com.ecom.inventory.port.in.usecase.item.dto.query.ListInventoryQuery;
import com.ecom.inventory.port.in.usecase.item.dto.result.InventoriesResult;
import com.ecom.inventory.port.in.usecase.item.dto.result.InventoryPageResult;
import com.ecom.inventory.port.in.usecase.item.dto.result.InventoryResult;
import com.ecom.inventory.port.in.usecase.reservation.dto.query.GetReservationQuery;
import com.ecom.inventory.port.in.usecase.reservation.dto.query.ListReservationsQuery;
import com.ecom.inventory.port.in.usecase.reservation.dto.result.ReservationListResult;
import com.ecom.inventory.port.in.usecase.reservation.dto.result.ReservationResult;
import com.ecom.inventory.port.out.persistence.item.InventoryPage;
import com.ecom.inventory.port.out.persistence.item.LoadInventoryPort;
import com.ecom.inventory.port.out.persistence.reservation.LoadReservationPort;
import com.ecom.inventory.service.item.GetInventoriesService;
import com.ecom.inventory.service.item.GetInventoryService;
import com.ecom.inventory.service.item.ListInventoryService;
import com.ecom.inventory.service.item.mapper.InventoryMapper;
import com.ecom.inventory.service.reservation.GetReservationService;
import com.ecom.inventory.service.reservation.ListReservationsService;
import com.ecom.inventory.service.reservation.mapper.ReservationMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryQueryServicesTest {

   private final InventoryMapper inventoryMapper = new InventoryMapper();
   private final ReservationMapper reservationMapper = new ReservationMapper();

   @Mock
   LoadInventoryPort loadInventoryPort;

   @Mock
   LoadReservationPort loadReservationPort;

   private static InventoryItem item(long skuId, int total) {
      return InventoryItem.of(skuId, UUID.randomUUID(), total);
   }

   private static InventoryReservation reservation(String reference, long skuId, int quantity) {
      return InventoryReservation.of(reference, skuId, quantity, Instant.now().plusSeconds(600));
   }

   // ------------------------------------------------------------------ stock

   @Test
   void getInventoryReturnsTheStockLevels() {
      InventoryItem item = item(7, 10).reserve(4);
      when(loadInventoryPort.loadBySkuId(7L)).thenReturn(Optional.of(item));

      InventoryResult result = new GetInventoryService(loadInventoryPort, inventoryMapper).execute(new GetInventoryQuery(7L));

      assertThat(result.skuId()).isEqualTo(7L);
      assertThat(result.totalQuantity()).isEqualTo(10);
      assertThat(result.reservedQuantity()).isEqualTo(4);
      assertThat(result.availableQuantity()).isEqualTo(6);
      assertThat(result.quantityChange()).isZero();
   }

   @Test
   void getInventoryFailsWhenTheSkuHasNoItem() {
      when(loadInventoryPort.loadBySkuId(8L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> new GetInventoryService(loadInventoryPort, inventoryMapper).execute(new GetInventoryQuery(8L)))
            .isInstanceOf(InventoryNotFoundException.class);
   }

   @Test
   void getInventoriesReturnsOnlyTheItemsThatExistAndLooksUpEachSkuOnce() {
      when(loadInventoryPort.loadBySkuIds(any())).thenReturn(List.of(item(1, 5), item(3, 0)));

      InventoriesResult result = new GetInventoriesService(loadInventoryPort, inventoryMapper)
            .execute(new GetInventoriesQuery(List.of(1L, 2L, 3L, 3L)));

      assertThat(result.items()).extracting(InventoryResult::skuId).containsExactly(1L, 3L);
      verify(loadInventoryPort).loadBySkuIds(Set.of(1L, 2L, 3L));
   }

   @Test
   void listInventoryMapsThePageAndPointsToTheNextOne() {
      when(loadInventoryPort.loadPage(1, 2)).thenReturn(new InventoryPage(List.of(item(3, 1), item(4, 2)), 7, 4));

      InventoryPageResult result = new ListInventoryService(loadInventoryPort, inventoryMapper).execute(new ListInventoryQuery(1, 2));

      assertThat(result.items()).extracting(InventoryResult::skuId).containsExactly(3L, 4L);
      assertThat(result.page()).isEqualTo(1);
      assertThat(result.size()).isEqualTo(2);
      assertThat(result.totalElements()).isEqualTo(7);
      assertThat(result.totalPages()).isEqualTo(4);
      assertThat(result.cursor()).isEqualTo("2");
   }

   @Test
   void theLastPageHasNoNextCursor() {
      when(loadInventoryPort.loadPage(3, 2)).thenReturn(new InventoryPage(List.of(item(7, 1)), 7, 4));

      assertThat(new ListInventoryService(loadInventoryPort, inventoryMapper).execute(new ListInventoryQuery(3, 2)).cursor()).isNull();
   }

   // ------------------------------------------------------------------ reservations

   @Test
   void getReservationReturnsTheReservationWithItsStatus() {
      InventoryReservation reservation = reservation("chk-1", 5, 2);
      UUID id = UUID.randomUUID();
      when(loadReservationPort.loadById(id)).thenReturn(Optional.of(reservation));

      ReservationResult result = new GetReservationService(loadReservationPort, reservationMapper).execute(new GetReservationQuery(id));

      assertThat(result.referenceId()).isEqualTo("chk-1");
      assertThat(result.skuId()).isEqualTo(5L);
      assertThat(result.quantity()).isEqualTo(2);
      assertThat(result.status()).isEqualTo(ReservationStatus.PENDING);
   }

   @Test
   void getReservationFailsWhenUnknown() {
      UUID id = UUID.randomUUID();
      when(loadReservationPort.loadById(id)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> new GetReservationService(loadReservationPort, reservationMapper).execute(new GetReservationQuery(id)))
            .isInstanceOf(ReservationNotFoundException.class);
   }

   @Test
   void listReservationsWithoutStatusReturnsEveryLineOrderedBySku() {
      when(loadReservationPort.findByReferenceId("chk-1")).thenReturn(List.of(reservation("chk-1", 9, 1), reservation("chk-1", 2, 3)));

      ReservationListResult result = new ListReservationsService(loadReservationPort, reservationMapper)
            .execute(new ListReservationsQuery("chk-1", null));

      assertThat(result.items()).extracting(ReservationResult::skuId).containsExactly(2L, 9L);
   }

   @Test
   void listReservationsWithStatusUsesTheFilter() {
      InventoryReservation released = reservation("chk-1", 4, 1);
      released.release();
      when(loadReservationPort.findByReferenceIdAndStatus("chk-1", ReservationStatus.RELEASED)).thenReturn(List.of(released));

      ReservationListResult result = new ListReservationsService(loadReservationPort, reservationMapper)
            .execute(new ListReservationsQuery("chk-1", ReservationStatus.RELEASED));

      assertThat(result.items()).extracting(ReservationResult::status).containsExactly(ReservationStatus.RELEASED);
   }

   @Test
   void listReservationsOfAnUnknownCheckoutIsEmpty() {
      when(loadReservationPort.findByReferenceId("nope")).thenReturn(List.of());

      assertThat(new ListReservationsService(loadReservationPort, reservationMapper).execute(new ListReservationsQuery("nope", null)).items())
            .isEmpty();
   }
}
