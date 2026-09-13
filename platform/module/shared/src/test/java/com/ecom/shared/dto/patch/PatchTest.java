package com.ecom.shared.dto.patch;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PatchTest {

   record Command(Patch<@Size(max = 5) String> description) {
   }

   private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

   @Test
   void anUndefinedPatchDoesNothing() {
      List<String> applied = new ArrayList<>();

      Patch.<String>undefined().ifSpecified(applied::add);

      assertThat(Patch.<String>undefined().isUndefined()).isTrue();
      assertThat(Patch.<String>undefined().isSpecified()).isFalse();
      assertThat(applied).isEmpty();
   }

   @Test
   void aPatchSpecifiedAsNullClearsTheField() {
      List<String> applied = new ArrayList<>();
      applied.add("old");

      Patch.<String>of(null).ifSpecified(value -> {
         applied.clear();
         applied.add(value);
      });

      assertThat(applied).containsExactly((String) null);
      assertThat(Patch.<String>of(null).isSpecified()).isTrue();
   }

   @Test
   void aPatchWithAValueSetsTheField() {
      List<String> applied = new ArrayList<>();

      Patch.of("new text").ifSpecified(applied::add);

      assertThat(applied).containsExactly("new text");
   }

   @Test
   void jsonNullableConvertsToTheSameThreeStates() {
      assertThat(Patch.from(JsonNullable.<String>undefined()).isUndefined()).isTrue();
      assertThat(Patch.from(null).isUndefined()).isTrue();

      Patch<String> cleared = Patch.from(JsonNullable.<String>of(null));
      assertThat(cleared.isSpecified()).isTrue();
      assertThat(cleared.value()).isNull();

      assertThat(Patch.from(JsonNullable.of("x")).value()).isEqualTo("x");
   }

   @Test
   void helpersFallBackOnlyWhenUndefined() {
      assertThat(Patch.<String>undefined().orElse("fallback")).isEqualTo("fallback");
      assertThat(Patch.<String>of(null).orElse("fallback")).isNull();
      assertThat(Patch.of("x").orElse("fallback")).isEqualTo("x");
      assertThat(Patch.of("x").map(String::length).value()).isEqualTo(1);
      assertThat(Patch.<String>undefined().map(String::length).isUndefined()).isTrue();
      assertThat(Patch.<String>undefined().toJsonNullable().isPresent()).isFalse();
      assertThat(Patch.of("x").toJsonNullable().get()).isEqualTo("x");
   }

   @Test
   void validationLooksInsideASpecifiedPatch() {
      Set<ConstraintViolation<Command>> violations = validator.validate(new Command(Patch.of("too long text")));

      assertThat(violations).hasSize(1);
      assertThat(violations.iterator().next().getPropertyPath().toString()).startsWith("description");
   }

   @Test
   void validationAcceptsAShortValueAnUndefinedPatchAndANullClear() {
      assertThat(validator.validate(new Command(Patch.of("ok")))).isEmpty();
      assertThat(validator.validate(new Command(Patch.undefined()))).isEmpty();
      assertThat(validator.validate(new Command(Patch.of(null)))).isEmpty();
   }
}
