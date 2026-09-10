package com.ecom.inventory.adapter.in.web.controller.reservation;

import com.ecom.inventory.adapter.in.web.controller.reservation.dto.response.ReservationListResponse;
import com.ecom.inventory.adapter.in.web.controller.reservation.dto.response.ReservationResponse;
import com.ecom.inventory.port.in.usecase.reservation.GetReservationUseCase;
import com.ecom.inventory.port.in.usecase.reservation.ListReservationsUseCase;
import com.ecom.inventory.port.in.usecase.reservation.dto.query.GetReservationQuery;
import com.ecom.inventory.port.in.usecase.reservation.dto.query.ListReservationsQuery;
import com.ecom.security.RequireRole;
import com.ecom.security.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/inventory/reservations")
@RequireRole({UserRole.ROLE_SYSTEM, UserRole.ROLE_ADMIN})
@RequiredArgsConstructor
public class ReservationQueryController {

   private final GetReservationUseCase getReservationUseCase;
   private final ListReservationsUseCase listReservationsUseCase;

   @GetMapping("/{reservationId}")
   public ResponseEntity<ReservationResponse> get(@PathVariable UUID reservationId) {
      return ResponseEntity.ok(ReservationResponse.from(getReservationUseCase.execute(new GetReservationQuery(reservationId))));
   }

   @GetMapping
   public ResponseEntity<ReservationListResponse> list(@Valid ListReservationsQuery query) {
      return ResponseEntity.ok(ReservationListResponse.from(listReservationsUseCase.execute(query)));
   }
}
