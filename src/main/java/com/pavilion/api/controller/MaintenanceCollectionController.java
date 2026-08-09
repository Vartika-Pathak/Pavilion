package com.pavilion.api.controller;

import com.pavilion.api.dto.TransactionsDtos.BackfillMaintenanceCollectionsRequest;
import com.pavilion.api.dto.TransactionsDtos.BackfillMaintenanceCollectionsResponse;
import com.pavilion.api.dto.TransactionsDtos.ConfirmMaintenancePaymentRequest;
import com.pavilion.api.dto.TransactionsDtos.MaintenanceCollectionRequest;
import com.pavilion.api.dto.TransactionsDtos.MaintenanceCollectionResponse;
import com.pavilion.api.dto.TransactionsDtos.MyMaintenanceDueResponse;
import com.pavilion.api.dto.TransactionsDtos.PayMaintenanceResult;
import com.pavilion.api.entity.Building;
import com.pavilion.api.entity.Flat;
import com.pavilion.api.entity.MaintenanceCollection;
import com.pavilion.api.entity.MaintenanceRate;
import com.pavilion.api.entity.User;
import com.pavilion.api.exception.ApiException;
import com.pavilion.api.repository.BuildingRepository;
import com.pavilion.api.repository.FlatRepository;
import com.pavilion.api.repository.MaintenanceCollectionRepository;
import com.pavilion.api.repository.MaintenanceRateRepository;
import com.pavilion.api.service.StripeService;
import com.pavilion.api.service.StripeService.StripeSessionResult;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/maintenance-collections")
@PreAuthorize("hasRole('ADMIN')")
public class MaintenanceCollectionController {

    private static final List<String> BACKFILL_PAYMENT_MODES = List.of("upi", "bank_transfer", "cash");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final MaintenanceCollectionRepository maintenanceCollectionRepository;
    private final FlatRepository flatRepository;
    private final BuildingRepository buildingRepository;
    private final MaintenanceRateRepository maintenanceRateRepository;
    private final StripeService stripeService;

    @Value("${app.public-url:http://localhost:5173}")
    private String defaultOrigin;

    public MaintenanceCollectionController(
            MaintenanceCollectionRepository maintenanceCollectionRepository,
            FlatRepository flatRepository,
            BuildingRepository buildingRepository,
            MaintenanceRateRepository maintenanceRateRepository,
            StripeService stripeService) {
        this.maintenanceCollectionRepository = maintenanceCollectionRepository;
        this.flatRepository = flatRepository;
        this.buildingRepository = buildingRepository;
        this.maintenanceRateRepository = maintenanceRateRepository;
        this.stripeService = stripeService;
    }

    private static String currentMonth() {
        return LocalDate.now(ZoneOffset.UTC).format(MONTH_FORMAT);
    }

    private Flat myFlatOrThrow(User user) {
        return flatRepository.findByResidentId(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No flat assigned to your account yet"));
    }

    private long expectedAmountPaiseFor(Flat flat) {
        return maintenanceRateRepository.findByFlatType(flat.getFlatType())
                .map(MaintenanceRate::getMonthlyAmountPaise)
                .orElse(0L);
    }

    private long collectedAmountPaiseFor(Flat flat, String month) {
        return maintenanceCollectionRepository.findByFlatIdAndForMonth(flat.getId(), month).stream()
                .mapToLong(MaintenanceCollection::getAmountPaise)
                .sum();
    }

    private long dueAmountPaiseFor(Flat flat, String month) {
        return Math.max(expectedAmountPaiseFor(flat) - collectedAmountPaiseFor(flat, month), 0);
    }

    // The current month's due for the signed-in resident's own flat — same computation as the
    // admin Due List report, scoped to one flat.
    @GetMapping("/mine/due")
    @PreAuthorize("isAuthenticated()")
    public MyMaintenanceDueResponse myDue(@AuthenticationPrincipal User user) {
        Flat flat = myFlatOrThrow(user);
        String month = currentMonth();
        String buildingName = buildingRepository.findById(flat.getBuildingId()).map(Building::getName).orElse(null);
        long expected = expectedAmountPaiseFor(flat);
        long collected = collectedAmountPaiseFor(flat, month);
        return new MyMaintenanceDueResponse(
                month, buildingName, flat.getFlatNumber(), expected, collected, Math.max(expected - collected, 0));
    }

    // Starts a Stripe Checkout session for the current month's due, in full — no partial
    // payments, matching how the admin's Due List already treats a month as one all-or-nothing
    // amount per flat.
    @PostMapping("/pay")
    @PreAuthorize("isAuthenticated()")
    public PayMaintenanceResult payMaintenance(
            @AuthenticationPrincipal User user, @RequestHeader(value = "Origin", required = false) String originHeader) {
        Flat flat = myFlatOrThrow(user);
        String month = currentMonth();
        long due = dueAmountPaiseFor(flat, month);
        if (due <= 0) {
            return new PayMaintenanceResult("nothing_due", null);
        }

        if (!stripeService.isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Payments aren't configured on this server yet");
        }

        String buildingName = buildingRepository.findById(flat.getBuildingId()).map(Building::getName).orElse("");
        String origin = (originHeader != null && !originHeader.isBlank()) ? originHeader : defaultOrigin;
        var session = stripeService.createMaintenanceCheckoutSession(
                due, buildingName, flat.getFlatNumber(), flat.getId(), month, origin);
        return new PayMaintenanceResult("requires_payment", session.url());
    }

    @PostMapping("/confirm")
    @PreAuthorize("isAuthenticated()")
    public MaintenanceCollectionResponse confirmMaintenancePayment(
            @AuthenticationPrincipal User user, @Valid @RequestBody ConfirmMaintenancePaymentRequest body) {
        if (!stripeService.isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Payments aren't configured on this server yet");
        }

        Optional<MaintenanceCollection> alreadyConfirmed = maintenanceCollectionRepository.findByStripeSessionId(body.sessionId());
        if (alreadyConfirmed.isPresent()) {
            MaintenanceCollection existing = alreadyConfirmed.get();
            Flat flat = flatRepository.findById(existing.getFlatId()).orElse(null);
            String buildingName = flat != null ? buildingRepository.findById(flat.getBuildingId()).map(Building::getName).orElse(null) : null;
            return MaintenanceCollectionResponse.from(existing, buildingName, flat != null ? flat.getFlatNumber() : null);
        }

        StripeSessionResult session = stripeService.retrieveSession(body.sessionId());
        if (!"paid".equals(session.paymentStatus())) {
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED, "Payment was not completed");
        }

        Map<String, String> metadata = session.metadata() == null ? Map.of() : session.metadata();
        String flatIdStr = metadata.get("flatId");
        String forMonth = metadata.get("forMonth");
        Flat flat = myFlatOrThrow(user);
        if (flatIdStr == null || forMonth == null || !flatIdStr.equals(String.valueOf(flat.getId()))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This payment session doesn't belong to you");
        }

        if (maintenanceCollectionRepository.existsByFlatIdAndForMonth(flat.getId(), forMonth)) {
            if (session.paymentIntentId() != null) {
                stripeService.refund(session.paymentIntentId());
            }
            throw new ApiException(HttpStatus.CONFLICT,
                    "This month was already paid while you were paying — you've been refunded.");
        }

        MaintenanceCollection collection = new MaintenanceCollection();
        collection.setFlatId(flat.getId());
        collection.setPayerName(user.getName());
        collection.setAmountPaise(session.amountTotal() != null ? session.amountTotal() : 0L);
        collection.setPaymentDate(LocalDate.now(ZoneOffset.UTC).toString());
        collection.setPaymentMode("online");
        collection.setForMonth(forMonth);
        collection.setNotes("Paid online via Stripe");
        collection.setStripeSessionId(body.sessionId());
        collection = maintenanceCollectionRepository.save(collection);

        String buildingName = buildingRepository.findById(flat.getBuildingId()).map(Building::getName).orElse(null);
        return MaintenanceCollectionResponse.from(collection, buildingName, flat.getFlatNumber());
    }

    @GetMapping
    public List<MaintenanceCollectionResponse> listMaintenanceCollections(
            @RequestParam(required = false) Long flatId, @RequestParam(required = false) String forMonth) {
        List<MaintenanceCollection> collections;
        if (flatId != null && forMonth != null) {
            collections = maintenanceCollectionRepository.findByFlatIdAndForMonth(flatId, forMonth);
        } else if (flatId != null) {
            collections = maintenanceCollectionRepository.findByFlatId(flatId);
        } else if (forMonth != null) {
            collections = maintenanceCollectionRepository.findByForMonth(forMonth);
        } else {
            collections = maintenanceCollectionRepository.findAll();
        }

        Map<Long, Flat> flatsById = new HashMap<>();
        Map<Long, String> buildingNamesById = new HashMap<>();
        for (Flat flat : flatRepository.findAll()) {
            flatsById.put(flat.getId(), flat);
        }
        for (Building building : buildingRepository.findAll()) {
            buildingNamesById.put(building.getId(), building.getName());
        }

        return collections.stream()
                .map(collection -> {
                    Flat flat = flatsById.get(collection.getFlatId());
                    String buildingName = flat != null ? buildingNamesById.get(flat.getBuildingId()) : null;
                    String flatNumber = flat != null ? flat.getFlatNumber() : null;
                    return MaintenanceCollectionResponse.from(collection, buildingName, flatNumber);
                })
                .toList();
    }

    @PostMapping
    public ResponseEntity<MaintenanceCollectionResponse> createMaintenanceCollection(
            @Valid @RequestBody MaintenanceCollectionRequest body) {
        Flat flat = flatRepository.findById(body.flatId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Unknown flat"));
        Building building = buildingRepository.findById(flat.getBuildingId()).orElse(null);

        MaintenanceCollection collection = new MaintenanceCollection();
        collection.setFlatId(body.flatId());
        collection.setPayerName(body.payerName());
        collection.setAmountPaise(body.amountPaise());
        collection.setPaymentDate(body.paymentDate());
        collection.setPaymentMode(body.paymentMode());
        collection.setForMonth(body.forMonth());
        collection.setReferenceNumber(body.referenceNumber());
        collection.setNotes(body.notes());
        collection = maintenanceCollectionRepository.save(collection);

        String buildingName = building != null ? building.getName() : null;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MaintenanceCollectionResponse.from(collection, buildingName, flat.getFlatNumber()));
    }

    // One-time (but safely re-runnable) historical seed for months before the current one —
    // mostly full on-time payments per flat's rate, with a small fraction of flat/month pairs
    // left unpaid each month to look realistic. Never touches a flat/month pair that already
    // has a collection.
    @PostMapping("/backfill")
    public BackfillMaintenanceCollectionsResponse backfillMaintenanceCollections(
            @Valid @RequestBody(required = false) BackfillMaintenanceCollectionsRequest body) {
        int months = body != null && body.months() != null ? body.months() : 3;
        List<String> targetMonths = trailingMonthsBeforeCurrent(months);

        List<Flat> flats = flatRepository.findAll();
        Map<String, Long> rateByFlatType = new HashMap<>();
        for (MaintenanceRate rate : maintenanceRateRepository.findAll()) {
            rateByFlatType.put(rate.getFlatType(), rate.getMonthlyAmountPaise());
        }

        Set<String> existingKeys = new HashSet<>();
        for (MaintenanceCollection collection : maintenanceCollectionRepository.findAll()) {
            existingKeys.add(collection.getFlatId() + ":" + collection.getForMonth());
        }

        int createdCount = 0;
        int skippedCount = 0;

        for (String month : targetMonths) {
            for (Flat flat : flats) {
                String key = flat.getId() + ":" + month;
                if (existingKeys.contains(key)) {
                    skippedCount++;
                    continue;
                }
                Long amountPaise = rateByFlatType.get(flat.getFlatType());
                boolean isLate = amountPaise == null || amountPaise <= 0 || isSimulatedLate(flat.getId(), month);
                if (isLate) {
                    skippedCount++;
                    continue;
                }

                MaintenanceCollection collection = new MaintenanceCollection();
                collection.setFlatId(flat.getId());
                collection.setPayerName("Flat " + flat.getFlatNumber() + " Resident");
                collection.setAmountPaise(amountPaise);
                collection.setPaymentDate(month + "-05");
                collection.setPaymentMode(BACKFILL_PAYMENT_MODES.get((int) (flat.getId() % BACKFILL_PAYMENT_MODES.size())));
                collection.setForMonth(month);
                collection.setNotes("Backfilled historical record");
                maintenanceCollectionRepository.save(collection);
                createdCount++;
            }
        }

        return new BackfillMaintenanceCollectionsResponse(targetMonths, createdCount, skippedCount);
    }

    // Months strictly before the current month, oldest first — e.g. count=3 in August gives
    // ["2026-05", "2026-06", "2026-07"]. Never touches the current month, which the admin is
    // expected to enter live.
    private List<String> trailingMonthsBeforeCurrent(int count) {
        List<String> months = new ArrayList<>();
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        for (int i = count; i >= 1; i--) {
            months.add(now.minusMonths(i).format(formatter));
        }
        return months;
    }

    // Deterministic per flat/month, not random — so a re-run always makes the same flats
    // "late" instead of eventually filling every one of them in and defeating the point.
    private boolean isSimulatedLate(Long flatId, String month) {
        String key = flatId + ":" + month;
        int hash = 0;
        for (int i = 0; i < key.length(); i++) {
            hash = hash * 31 + key.charAt(i);
        }
        return Math.abs(hash) % 10 == 0;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMaintenanceCollection(@PathVariable Long id) {
        if (!maintenanceCollectionRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Maintenance collection not found");
        }
        maintenanceCollectionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
