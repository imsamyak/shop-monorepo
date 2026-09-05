package com.ecom.checkout.service;

import com.ecom.checkout.domain.entity.Cart;
import com.ecom.checkout.port.in.usecase.dto.result.CartResult;
import com.ecom.checkout.port.out.persistence.LoadCartPort;
import com.ecom.checkout.port.out.persistence.SaveCartPort;
import com.ecom.checkout.service.mapper.CartMapper;
import com.ecom.shared.exception.conflict.VersionConflictException;
import com.ecom.shared.validator.EntityVersionValidator;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartEditorTest {

   private final LoadCartPort load = mock(LoadCartPort.class);
   private final SaveCartPort save = mock(SaveCartPort.class);
   private final CartEditor editor = new CartEditor(load, save, new EntityVersionValidator(), new CartMapper());

   private final UUID customer = UUID.randomUUID();

   @Test
   void aCustomerWithoutACartGetsOneOnTheFirstEdit() {
      when(load.loadByCustomerId(customer)).thenReturn(Optional.empty());
      when(save.save(any())).thenAnswer(call -> call.getArgument(0));

      CartResult result = editor.edit(customer, null, cart -> cart.add(5L, 2));

      assertThat(result.customerId()).isEqualTo(customer);
      assertThat(result.items()).singleElement().satisfies(i -> {
         assertThat(i.skuId()).isEqualTo(5L);
         assertThat(i.quantity()).isEqualTo(2);
      });
      verify(save).save(any());
   }

   @Test
   void anEditThatChangesNothingIsNotSavedAndAMissingCartStaysEmpty() {
      when(load.loadByCustomerId(customer)).thenReturn(Optional.empty());

      CartResult result = editor.edit(customer, null, cart -> cart.remove(5L));

      assertThat(result.items()).isEmpty();
      assertThat(result.version()).isZero();
      assertThat(result.updatedAt()).isNull();
      verify(save, never()).save(any());
   }

   @Test
   void aStaleVersionIsAConflictAndNothingIsChanged() {
      Cart stored = Cart.empty(customer);
      when(load.loadByCustomerId(customer)).thenReturn(Optional.of(stored));

      assertThatThrownBy(() -> editor.edit(customer, 3L, cart -> cart.add(5L, 1)))
            .isInstanceOf(VersionConflictException.class);
      assertThat(stored.getItems()).isEmpty();
      verify(save, never()).save(any());
   }

   @Test
   void theCurrentVersionIsAccepted() {
      Cart stored = Cart.empty(customer);
      when(load.loadByCustomerId(customer)).thenReturn(Optional.of(stored));
      when(save.save(any())).thenAnswer(call -> call.getArgument(0));

      editor.edit(customer, 0L, cart -> cart.add(5L, 1));

      verify(save).save(stored);
   }
}
