package com.ecom.inventory.adapter.in.web.controller.reservation.dto.request;

import com.ecom.inventory.port.in.usecase.reservation.dto.command.CommitReservationsCmd;
import com.ecom.inventory.port.in.usecase.reservation.dto.command.ReleaseReservationsCmd;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record ReservationIdsRequest(
      @NotEmpty(message = "At least one reservation id is required")
      @Size(max = 100, message = "At most 100 reservations per call")
      List<@NotNull UUID> reservationIds
) {

   public CommitReservationsCmd toCommitCommand() {
      return new CommitReservationsCmd(reservationIds);
   }

   public ReleaseReservationsCmd toReleaseCommand() {
      return new ReleaseReservationsCmd(reservationIds);
   }
}
