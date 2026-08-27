package com.ecom.shared.exception.notfound;

import com.ecom.shared.exception.ErrorCode;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
public class EntityNotFoundException extends NotFoundException {

    private final String entityName;
    private final Object identifier;

    public EntityNotFoundException(String entityName, Object identifier) {
        super(
            String.format("%s not found with identifier: %s", entityName, identifier),
            ErrorCode.RESOURCE_NOT_FOUND,
            Map.of("entityName", entityName, "identifier", String.valueOf(identifier))
        );
        this.entityName = entityName;
        this.identifier = identifier;
    }

    @Builder
    public static EntityNotFoundException of(String entityName, Object identifier) {
        return new EntityNotFoundException(entityName, identifier);
    }
}
