package com.ecom.checkout.service;

import com.ecom.checkout.domain.exception.SkuNotAvailableException;
import com.ecom.checkout.port.in.usecase.DraftCheckoutUseCase;
import com.ecom.checkout.port.in.usecase.dto.command.DraftCheckoutCmd;
import com.ecom.checkout.port.in.usecase.dto.result.CheckoutResult;
import com.ecom.checkout.port.in.usecase.record.RecordCheckouts;
import com.ecom.checkout.port.out.client.catalog.LoadSkuPricePort;
import com.ecom.checkout.port.out.client.catalog.LoadSkuPricePort.SkuPrice;
import com.ecom.checkout.port.out.client.inventory.ReleaseStockPort;
import com.ecom.checkout.port.out.client.inventory.ReserveStockPort;
import com.ecom.checkout.port.out.client.inventory.ReserveStockPort.ReservedStock;
import com.ecom.checkout.port.out.client.inventory.ReserveStockPort.StockLine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Creates a draft checkout:
 * <ol>
 *   <li>looks up the real price and seller of every SKU (client prices are never used),</li>
 *   <li>reserves all the stock in ONE atomic call to inventory, keyed by the new checkout id,</li>
 *   <li>records the checkout in a short local transaction (which also writes the outbox event).</li>
 * </ol>
 * No database transaction is open while the network calls run. If recording fails after stock was reserved, the
 * reservations are released; should that release fail too they expire on their own in inventory.
 */
@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class DraftCheckoutService implements DraftCheckoutUseCase {

   private final LoadSkuPricePort loadSkuPricePort;
   private final ReserveStockPort reserveStockPort;
   private final ReleaseStockPort releaseStockPort;
   private final RecordCheckouts.RecordDraft recordDraft;

   @Override
   public CheckoutResult execute(DraftCheckoutCmd cmd) {
      UUID customerId = cmd.customerId();

      Map<Long, Integer> wanted = new TreeMap<>();
      for (DraftCheckoutCmd.ItemCmd item : cmd.items()) {
         wanted.merge(item.skuId(), item.quantity(), Integer::sum);
      }

      Map<Long, SkuPrice> prices = loadSkuPricePort.loadPrices(wanted.keySet());
      for (Long skuId : wanted.keySet()) {
         if (!prices.containsKey(skuId)) {
            throw SkuNotAvailableException.of(skuId);
         }
      }

      UUID checkoutId = UUID.randomUUID();
      Instant expiresAt = Instant.now().plusSeconds(cmd.ttlSeconds());

      List<StockLine> lines = wanted.entrySet().stream()
            .map(e -> new StockLine(e.getKey(), e.getValue()))
            .toList();
      Map<Long, UUID> reservationBySku = reserveStockPort.reserve(checkoutId.toString(), cmd.ttlSeconds(), lines).stream()
            .collect(Collectors.toMap(ReservedStock::skuId, ReservedStock::reservationId));

      List<RecordCheckouts.DraftCmd.Item> items = new ArrayList<>();
      for (Map.Entry<Long, Integer> line : wanted.entrySet()) {
         SkuPrice price = prices.get(line.getKey());
         items.add(new RecordCheckouts.DraftCmd.Item(
               line.getKey(), price.sellerId(), line.getValue(), price.price(), reservationBySku.get(line.getKey())));
      }

      try {
         return recordDraft.execute(new RecordCheckouts.DraftCmd(checkoutId, customerId, expiresAt, items)).checkout();
      } catch (RuntimeException failure) {
         releaseQuietly(reservationBySku.values(), checkoutId);
         throw failure;
      }
   }

   private void releaseQuietly(java.util.Collection<UUID> reservationIds, UUID checkoutId) {
      try {
         releaseStockPort.release(reservationIds);
      } catch (RuntimeException releaseFailure) {
         log.error("Could not release the reservations of failed checkout {}; they will expire on their own",
               checkoutId, releaseFailure);
      }
   }
}
