package com.pavilion.api.controller;

import com.pavilion.api.AbstractIntegrationTest;
import com.pavilion.api.entity.AmenityBooking;
import com.pavilion.api.entity.User;
import com.pavilion.api.repository.AmenityBookingRepository;
import com.pavilion.api.service.StripeService;
import com.pavilion.api.service.StripeService.CheckoutSessionResult;
import com.pavilion.api.service.StripeService.StripeSessionResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AmenityControllerTest extends AbstractIntegrationTest {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    @Autowired
    private AmenityBookingRepository amenityBookingRepository;

    @MockBean
    private StripeService stripeService;

    private String futureDate() {
        return LocalDate.now().plusDays(30).format(DATE);
    }

    private String pastDate() {
        return LocalDate.now().minusDays(1).format(DATE);
    }

    @Test
    void listAmenitiesRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/amenities")).andExpect(status().isUnauthorized());
    }

    @Test
    void listAmenitiesReturnsTheStaticCatalog() throws Exception {
        User resident = createUser("resident");
        mockMvc.perform(get("/api/amenities").cookie(sessionCookie(resident)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].id").value("clubhouse"))
                .andExpect(jsonPath("$[2].requiresPayment").value(true));
    }

    @Test
    void availabilityForAnUnknownAmenityFails() throws Exception {
        User resident = createUser("resident");
        mockMvc.perform(get("/api/amenities/availability")
                        .cookie(sessionCookie(resident))
                        .param("amenityId", "rooftop_bar")
                        .param("date", futureDate()))
                .andExpect(status().isNotFound());
    }

    @Test
    void bookingAFreeAmenityConfirmsImmediately() throws Exception {
        User resident = createUser("resident");
        String date = futureDate();

        mockMvc.perform(post("/api/amenities/bookings")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("{\"amenityId\":\"clubhouse\",\"bookingDate\":\"" + date + "\",\"slot\":\"morning\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("confirmed"))
                .andExpect(jsonPath("$.booking.amenityName").value("Clubhouse"))
                .andExpect(jsonPath("$.booking.amountPaidCents").value(0))
                .andExpect(jsonPath("$.booking.residentFlatNumber").value(resident.getFlatNumber()));

        mockMvc.perform(get("/api/amenities/availability")
                        .cookie(sessionCookie(resident))
                        .param("amenityId", "clubhouse")
                        .param("date", date))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookedSlots.length()").value(1))
                .andExpect(jsonPath("$.bookedSlots[0]").value("morning"));
    }

    @Test
    void bookingAnAlreadyPassedSlotFails() throws Exception {
        User resident = createUser("resident");
        mockMvc.perform(post("/api/amenities/bookings")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("{\"amenityId\":\"clubhouse\",\"bookingDate\":\"" + pastDate() + "\",\"slot\":\"morning\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bookingAnAlreadyBookedSlotFails() throws Exception {
        User residentA = createUser("resident");
        User residentB = createUser("resident");
        String date = futureDate();
        String body = "{\"amenityId\":\"clubhouse\",\"bookingDate\":\"" + date + "\",\"slot\":\"evening\"}";

        mockMvc.perform(post("/api/amenities/bookings").cookie(sessionCookie(residentA))
                        .contentType("application/json").content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/amenities/bookings").cookie(sessionCookie(residentB))
                        .contentType("application/json").content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void bookingAnUnknownAmenityFails() throws Exception {
        User resident = createUser("resident");
        mockMvc.perform(post("/api/amenities/bookings")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("{\"amenityId\":\"rooftop_bar\",\"bookingDate\":\"" + futureDate() + "\",\"slot\":\"morning\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void bookingAPaidAmenityWithoutStripeConfiguredFails() throws Exception {
        User resident = createUser("resident");
        mockMvc.perform(post("/api/amenities/bookings")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("{\"amenityId\":\"tennis_court\",\"bookingDate\":\"" + futureDate() + "\",\"slot\":\"morning\"}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void bookingAPaidAmenityStartsAStripeCheckout() throws Exception {
        User resident = createUser("resident");
        when(stripeService.isConfigured()).thenReturn(true);
        when(stripeService.createCheckoutSession(any(), anyString(), anyString(), any(), anyString()))
                .thenReturn(new CheckoutSessionResult("cs_test_123", "https://checkout.stripe.com/pay/cs_test_123"));

        mockMvc.perform(post("/api/amenities/bookings")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("{\"amenityId\":\"tennis_court\",\"bookingDate\":\"" + futureDate() + "\",\"slot\":\"morning\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("requires_payment"))
                .andExpect(jsonPath("$.checkoutUrl").value("https://checkout.stripe.com/pay/cs_test_123"));
    }

    @Test
    void myBookingsOnlyShowsTheCallersOwnBookings() throws Exception {
        User residentA = createUser("resident");
        User residentB = createUser("resident");
        String date = futureDate();

        mockMvc.perform(post("/api/amenities/bookings").cookie(sessionCookie(residentA))
                .contentType("application/json")
                .content("{\"amenityId\":\"clubhouse\",\"bookingDate\":\"" + date + "\",\"slot\":\"morning\"}"));

        mockMvc.perform(get("/api/amenities/bookings/mine").cookie(sessionCookie(residentB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/amenities/bookings/mine").cookie(sessionCookie(residentA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].amenityId").value("clubhouse"));
    }

    @Test
    void confirmWithoutStripeConfiguredFails() throws Exception {
        User resident = createUser("resident");
        mockMvc.perform(post("/api/amenities/bookings/confirm")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("{\"sessionId\":\"cs_test_123\"}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void confirmWithAnUnpaidSessionFails() throws Exception {
        User resident = createUser("resident");
        when(stripeService.isConfigured()).thenReturn(true);
        when(stripeService.retrieveSession("cs_test_123"))
                .thenReturn(new StripeSessionResult("unpaid", null, null, Map.of()));

        mockMvc.perform(post("/api/amenities/bookings/confirm")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("{\"sessionId\":\"cs_test_123\"}"))
                .andExpect(status().isPaymentRequired());
    }

    @Test
    void confirmWithASessionBelongingToSomeoneElseFails() throws Exception {
        User resident = createUser("resident");
        User otherResident = createUser("resident");
        when(stripeService.isConfigured()).thenReturn(true);
        when(stripeService.retrieveSession("cs_test_123")).thenReturn(new StripeSessionResult(
                "paid", "pi_123", 1000L,
                Map.of("residentId", String.valueOf(otherResident.getId()), "amenityId", "tennis_court",
                        "bookingDate", futureDate(), "slot", "morning")));

        mockMvc.perform(post("/api/amenities/bookings/confirm")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("{\"sessionId\":\"cs_test_123\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void confirmCreatesTheBookingOnSuccessfulPayment() throws Exception {
        User resident = createUser("resident");
        String date = futureDate();
        when(stripeService.isConfigured()).thenReturn(true);
        when(stripeService.retrieveSession("cs_test_123")).thenReturn(new StripeSessionResult(
                "paid", "pi_123", 1000L,
                Map.of("residentId", String.valueOf(resident.getId()), "amenityId", "tennis_court",
                        "bookingDate", date, "slot", "morning")));

        mockMvc.perform(post("/api/amenities/bookings/confirm")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("{\"sessionId\":\"cs_test_123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amenityId").value("tennis_court"))
                .andExpect(jsonPath("$.amountPaidCents").value(1000))
                .andExpect(jsonPath("$.bookingDate").value(date));

        verify(stripeService, never()).refund(anyString());
    }

    @Test
    void confirmingTheSameSessionTwiceReturnsTheExistingBookingWithoutRecheckingStripe() throws Exception {
        User resident = createUser("resident");
        String date = futureDate();
        when(stripeService.isConfigured()).thenReturn(true);
        when(stripeService.retrieveSession("cs_test_123")).thenReturn(new StripeSessionResult(
                "paid", "pi_123", 1000L,
                Map.of("residentId", String.valueOf(resident.getId()), "amenityId", "tennis_court",
                        "bookingDate", date, "slot", "morning")));

        String content = "{\"sessionId\":\"cs_test_123\"}";
        mockMvc.perform(post("/api/amenities/bookings/confirm").cookie(sessionCookie(resident))
                        .contentType("application/json").content(content))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/amenities/bookings/confirm").cookie(sessionCookie(resident))
                        .contentType("application/json").content(content))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amenityId").value("tennis_court"));

        verify(stripeService, times(1)).retrieveSession("cs_test_123");
    }

    @Test
    void confirmRefundsWhenTheSlotWasTakenWhilePaying() throws Exception {
        User resident = createUser("resident");
        String date = futureDate();

        AmenityBooking existing = new AmenityBooking();
        existing.setResidentId(999L);
        existing.setAmenityId("tennis_court");
        existing.setBookingDate(date);
        existing.setSlot("morning");
        existing.setAmountPaidCents(1000);
        amenityBookingRepository.save(existing);

        when(stripeService.isConfigured()).thenReturn(true);
        when(stripeService.retrieveSession("cs_test_123")).thenReturn(new StripeSessionResult(
                "paid", "pi_123", 1000L,
                Map.of("residentId", String.valueOf(resident.getId()), "amenityId", "tennis_court",
                        "bookingDate", date, "slot", "morning")));

        mockMvc.perform(post("/api/amenities/bookings/confirm")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("{\"sessionId\":\"cs_test_123\"}"))
                .andExpect(status().isConflict());

        verify(stripeService, times(1)).refund("pi_123");
    }

    @Test
    void confirmRefundsWhenTheSlotPassedWhilePaying() throws Exception {
        User resident = createUser("resident");
        when(stripeService.isConfigured()).thenReturn(true);
        when(stripeService.retrieveSession("cs_test_123")).thenReturn(new StripeSessionResult(
                "paid", "pi_123", 1000L,
                Map.of("residentId", String.valueOf(resident.getId()), "amenityId", "tennis_court",
                        "bookingDate", pastDate(), "slot", "morning")));

        mockMvc.perform(post("/api/amenities/bookings/confirm")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("{\"sessionId\":\"cs_test_123\"}"))
                .andExpect(status().isConflict());

        verify(stripeService, times(1)).refund("pi_123");
    }

    @Test
    void confirmRejectsAMalformedRequestBody() throws Exception {
        User resident = createUser("resident");
        mockMvc.perform(post("/api/amenities/bookings/confirm")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bookRejectsAnInvalidSlot() throws Exception {
        User resident = createUser("resident");
        mockMvc.perform(post("/api/amenities/bookings")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("{\"amenityId\":\"clubhouse\",\"bookingDate\":\"" + futureDate() + "\",\"slot\":\"midnight\"}"))
                .andExpect(status().isBadRequest());
    }
}
