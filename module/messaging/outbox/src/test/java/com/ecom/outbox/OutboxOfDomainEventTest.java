package com.ecom.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ecom.outbox.internal.aspect.DefaultOutboxEventFactory;
import com.ecom.outbox.internal.persistence.OutboxEvent;
import com.ecom.shared.messaging.DomainEvent;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

// A record that is not nested in an signature interface has no signature type, so it must be rejected
record TopLevelEvent(String id) implements DomainEvent {
   @Override
   public String aggregateId() {
      return id;
   }
}

class OutboxOfDomainEventTest {

   private final ObjectMapper mapper = new ObjectMapper();
   private final OutboxEventFactory factory = new DefaultOutboxEventFactory(mapper, 65536);
   private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

   @Test
   void aggregateTypeIsTheNameOfTheInterfaceTheRecordIsNestedIn() {
      OutboxEvent outboxEvent = factory.create(new Order.Placed(UUID.randomUUID(), 5.0));
      assertEquals("order", outboxEvent.getAggregateType());
   }

   @Test
   void envelopeCarriesAggregateActionAndData() throws IOException {
      Order.Placed event = new Order.Placed(UUID.randomUUID(), 5.0);
      OutboxEvent outboxEvent = factory.create(event);

      DefaultOutboxEventFactory.Envelope envelope = mapper.readValue(outboxEvent.getPayload(), DefaultOutboxEventFactory.Envelope.class);
      assertEquals("order", envelope.aggregate());
      assertEquals("placed", envelope.action());
      assertNotNull(envelope.data());
   }

   @Test
   void aggregateIdIsExactlyWhatTheEventReturns() {
      UUID id = UUID.randomUUID();
      assertEquals(id.toString(), factory.create(new Order.Placed(id, 1.0)).getAggregateId());

      assertEquals("1007", factory.create(new Invoice.Issued(1007L)).getAggregateId());

      UUID customer = UUID.randomUUID();
      assertEquals(customer + ":EU", factory.create(new Shipment.Sent(customer, "EU")).getAggregateId());
   }

   @Test
   void resultPassesTheOutboxValidationRules() {
      OutboxEvent outboxEvent = factory.create(new Order.Placed(UUID.randomUUID(), 1.0));
      assertTrue(validator.validate(outboxEvent).isEmpty());
   }

   @Test
   void nullAggregateIdIsRejectedWithAMessageNamingTheEvent() {
      IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> factory.create(new Broken.NoId()));
      assertTrue(ex.getMessage().contains("broken"), ex.getMessage());
   }

   @Test
   void blankAggregateIdIsRejectedWithAMessageNamingTheEvent() {
      IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> factory.create(new Broken.BlankId()));
      assertTrue(ex.getMessage().contains("broken"), ex.getMessage());
   }

   @Test
   void aTopLevelRecordHasNoAggregateTypeAndIsRejected() {
      IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> factory.create(new TopLevelEvent("1")));
      assertTrue(ex.getMessage().contains("TopLevelEvent"), ex.getMessage());
   }

   @Test
   void anAnonymousEventIsRejected() {
      DomainEvent anonymous = new DomainEvent() {
         @Override
         public String aggregateId() {
            return "x";
         }
      };
      assertThrows(IllegalArgumentException.class, () -> factory.create(anonymous));
   }

   @Test
   void aLambdaEventIsRejected() {
      DomainEvent lambda = () -> "x";
      assertThrows(IllegalArgumentException.class, () -> factory.create(lambda));
   }

   @Test
   void aNullEventIsRejected() {
      assertThrows(NullPointerException.class, () -> factory.create(null));
   }

   @Test
   void failsWhenPayloadExceedsMaxLength() {
      OutboxEventFactory smallFactory = new DefaultOutboxEventFactory(mapper, 10);
      IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> smallFactory.create(new Order.Placed(UUID.randomUUID(), 5.0)));
      assertTrue(ex.getMessage().contains("exceeds outbox.max-payload-length"), ex.getMessage());
   }

   sealed interface Order extends DomainEvent {
      UUID orderId();

      @Override
      default String aggregateId() {
         return orderId().toString();
      }

      record Placed(UUID orderId, double total) implements Order {}
   }

   sealed interface Invoice extends DomainEvent {
      record Issued(long number) implements Invoice {
         @Override
         public String aggregateId() {
            return String.valueOf(number);
         }
      }
   }

   sealed interface Shipment extends DomainEvent {
      record Sent(UUID customerId, String region) implements Shipment {
         @Override
         public String aggregateId() {
            return customerId + ":" + region;
         }
      }
   }

   sealed interface Broken extends DomainEvent {
      record NoId() implements Broken {
         @Override
         public String aggregateId() {
            return null;
         }
      }

      record BlankId() implements Broken {
         @Override
         public String aggregateId() {
            return "   ";
         }
      }
   }
}
