package com.ecom.shared.validator;

import com.ecom.shared.entity.DomainEntity;
import com.ecom.shared.exception.conflict.VersionConflictException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntityVersionValidatorTest {

   static class TestEntity extends DomainEntity {
      @Override
      public String getEntityName() {
         return "TEST_ENTITY";
      }
   }

   private final EntityVersionValidator validator = new EntityVersionValidator();

   @Test
   void shouldPassWhenVersionsMatch() {
      TestEntity entity = new TestEntity();
      assertThatCode(() -> validator.validate(entity, 0)).doesNotThrowAnyException();
   }

   @Test
   void shouldFailWithConflictWhenVersionsMismatch() {
      TestEntity entity = new TestEntity();
      assertThatThrownBy(() -> validator.validate(entity, 1))
          .isInstanceOfSatisfying(VersionConflictException.class, ex -> {
             assertThat(ex.getHttpStatus().value()).isEqualTo(409);
             assertThat(ex.getEntityName()).isEqualTo("TEST_ENTITY");
             assertThat(ex.getExpectedVersion()).isEqualTo(1);
             assertThat(ex.getActualVersion()).isZero();
          });
   }

   @Test
   void shouldThrowWhenEntityIsNull() {
      assertThatThrownBy(() -> validator.validate(null, 0))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Entity cannot be null for version validation");
   }
}
