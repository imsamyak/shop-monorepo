package com.ecom.checkout.domain.entity;

import com.ecom.checkout.domain.exception.CheckoutExpiredException;
import com.ecom.checkout.domain.exception.EmptyCheckoutException;
import com.ecom.checkout.domain.exception.InvalidCheckoutStateException;
import com.ecom.shared.exception.domain.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckoutTest {

   private static final UUID SELLER = UUID.randomUUID();

   private CheckoutItem item(int quantity, String price) {
      return CheckoutItem.of(1L, SELLER, quantity, new BigDecimal(price), UUID.randomUUID());
   }

   private Checkout draft(Instant expiresAt) {
      return Checkout.draft(UUID.randomUUID(), UUID.randomUUID(), expiresAt, List.of(item(2, "10.50"), item(1, "4.00")));
   }

   @Test
   void totalIsTheSumOfTheItemSubtotals() {
      Checkout checkout = draft(Instant.now().plusSeconds(60));

      assertThat(checkout.getTotalAmount()).isEqualByComparingTo("25.00");
      assertThat(checkout.getStatus()).isEqualTo(CheckoutStatus.DRAFT);
   }

   @Test
   void aCheckoutNeedsItemsACustomerAndAnExpiry() {
      UUID id = UUID.randomUUID();
      Instant later = Instant.now().plusSeconds(60);

      assertThatThrownBy(() -> Checkout.draft(id, UUID.randomUUID(), later, List.of())).isInstanceOf(EmptyCheckoutException.class);
      assertThatThrownBy(() -> Checkout.draft(id, null, later, List.of(item(1, "1")))).isInstanceOf(BusinessRuleException.class);
      assertThatThrownBy(() -> Checkout.draft(id, UUID.randomUUID(), null, List.of(item(1, "1")))).isInstanceOf(BusinessRuleException.class);
   }

   @Test
   void itemsValidateTheirInputs() {
      assertThatThrownBy(() -> item(0, "1")).isInstanceOf(BusinessRuleException.class);
      assertThatThrownBy(() -> item(1, "-1")).isInstanceOf(BusinessRuleException.class);
      assertThatThrownBy(() -> CheckoutItem.of(1L, null, 1, BigDecimal.ONE, null)).isInstanceOf(BusinessRuleException.class);
   }

   @Test
   void commitWorksOnlyOnAFreshDraft() {
      Checkout checkout = draft(Instant.now().plusSeconds(60));

      checkout.commit(Instant.now());

      assertThat(checkout.getStatus()).isEqualTo(CheckoutStatus.COMMITTED);
      assertThatThrownBy(() -> checkout.commit(Instant.now())).isInstanceOf(InvalidCheckoutStateException.class);
      assertThatThrownBy(checkout::cancel).isInstanceOf(InvalidCheckoutStateException.class);
   }

   @Test
   void anExpiredDraftCannotBeCommitted() {
      Checkout checkout = draft(Instant.now().minusSeconds(1));

      assertThatThrownBy(() -> checkout.commit(Instant.now())).isInstanceOf(CheckoutExpiredException.class);
      assertThat(checkout.getStatus()).isEqualTo(CheckoutStatus.DRAFT);
   }

   @Test
   void expiryOnlyAppliesToOverdueDrafts() {
      Checkout fresh = draft(Instant.now().plusSeconds(60));
      Checkout overdue = draft(Instant.now().minusSeconds(1));

      assertThat(fresh.expire(Instant.now())).isFalse();
      assertThat(overdue.expire(Instant.now())).isTrue();
      assertThat(overdue.getStatus()).isEqualTo(CheckoutStatus.EXPIRED);
      assertThat(overdue.expire(Instant.now())).isFalse();
   }
}
