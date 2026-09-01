package com.ecom.inventory.adapter.in.web.controller.reservation;

import com.ecom.inventory.adapter.in.web.controller.reservation.dto.request.ReservationIdsRequest;
import com.ecom.inventory.adapter.in.web.controller.reservation.dto.request.ReserveInventoryRequest;
import com.ecom.inventory.adapter.in.web.controller.reservation.dto.response.ExpireReservationsResponse;
import com.ecom.inventory.adapter.in.web.controller.reservation.dto.response.ReservationsResponse;
import com.ecom.inventory.port.in.usecase.reservation.CommitReservationUseCase;
import com.ecom.inventory.port.in.usecase.reservation.ExpireReservationsUseCase;
import com.ecom.inventory.port.in.usecase.reservation.ReleaseReservationUseCase;
import com.ecom.inventory.port.in.usecase.reservation.ReserveInventoryUseCase;
import com.ecom.inventory.port.in.usecase.reservation.dto.command.ExpireReservationsCmd;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reservation API used by other services (checkout) with a system token, and by administrators. Every operation takes
 * and applies a whole batch atomically, and commit / release are idempotent so callers can retry safely.
 */
@RestController
@RequestMapping("/inventory/reservations")
@RequiredArgsConstructor
public class ReservationController {

   private final ReserveInventoryUseCase reserveInventoryUseCase;
   private final CommitReservationUseCase commitReservationUseCase;
   private final ReleaseReservationUseCase releaseReservationUseCase;
   private final ExpireReservationsUseCase expireReservationsUseCase;

   @PostMapping
   public ResponseEntity<ReservationsResponse> reserve(@Valid @RequestBody ReserveInventoryRequest request) {
      return ResponseEntity.status(201)
            .body(ReservationsResponse.from(reserveInventoryUseCase.execute(request.toCommand())));
   }

   @PostMapping("/commit")
   public ResponseEntity<ReservationsResponse> commit(@Valid @RequestBody ReservationIdsRequest request) {
      return ResponseEntity.ok(ReservationsResponse.from(commitReservationUseCase.execute(request.toCommitCommand())));
   }

   @PostMapping("/release")
   public ResponseEntity<ReservationsResponse> release(@Valid @RequestBody ReservationIdsRequest request) {
      return ResponseEntity.ok(ReservationsResponse.from(releaseReservationUseCase.execute(request.toReleaseCommand())));
   }

   @PostMapping("/expire")
   public ResponseEntity<ExpireReservationsResponse> expireReservations() {
      return ResponseEntity.ok(ExpireReservationsResponse.from(expireReservationsUseCase.execute(new ExpireReservationsCmd())));
   }
}
