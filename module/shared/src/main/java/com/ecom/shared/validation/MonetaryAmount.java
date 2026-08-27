package com.ecom.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.OverridesAttribute;
import jakarta.validation.Payload;
import jakarta.validation.constraints.DecimalMin;

import java.lang.annotation.*;

@DecimalMin(value = "0.00", inclusive = true)
@Target({
    ElementType.ANNOTATION_TYPE,
    ElementType.FIELD,
    ElementType.METHOD,
    ElementType.PARAMETER,
    ElementType.RECORD_COMPONENT,
    ElementType.TYPE_USE
})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Documented
public @interface MonetaryAmount {

    String message() default "{jakarta.validation.constraints.DecimalMin.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    @OverridesAttribute(constraint = DecimalMin.class, name = "value")
    String min() default "0.00";

    @OverridesAttribute(constraint = DecimalMin.class, name = "inclusive")
    boolean inclusive() default true;
}
