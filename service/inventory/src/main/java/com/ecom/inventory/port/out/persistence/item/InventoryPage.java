package com.ecom.inventory.port.out.persistence.item;

import com.ecom.inventory.domain.entity.InventoryItem;

import java.util.List;

public record InventoryPage(List<InventoryItem> items, long totalElements, int totalPages) {
}
