package com.ecom.inventory.port.out.persistence.reservation;

import com.ecom.inventory.domain.entity.InventoryReservation;

import java.util.List;

public interface SaveReservationPort {

   InventoryReservation save(InventoryReservation reservation);

   List<InventoryReservation> saveAll(List<InventoryReservation> reservations);
}
