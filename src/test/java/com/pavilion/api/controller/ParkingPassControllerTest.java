package com.pavilion.api.controller;

import com.pavilion.api.AbstractIntegrationTest;
import com.pavilion.api.entity.ParkingPass;
import com.pavilion.api.entity.User;
import com.pavilion.api.repository.ParkingPassRepository;
import com.pavilion.api.service.StripeService;
import com.pavilion.api.service.StripeService.CheckoutSessionResult;
import com.pavilion.api.service.StripeService.StripeSessionResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ParkingPassControllerTest extends AbstractIntegrationTest {

    @Autowired
    private ParkingPassRepository parkingPassRepository;

    @MockBean
    private StripeService stripeService;

    @Test
    void myPassRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/parking")).andExpect(status().isUnauthorized());
    }

    @Test
    void myPassIsNullWhenNotPurchased() throws Exception {
        User resident = createUser("resident");
        mockMvc.perform(get("/api/parking").cookie(sessionCookie(resident)))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void myPassReturnsTheFlatsPassWhenPurchased() throws Exception {
        User resident = createUser("resident");
        savePass(resident.getFlatNumber());

        mockMvc.perform(get("/api/parking").cookie(sessionCookie(resident)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flatNumber").value(resident.getFlatNumber()));
    }

    @Test
    void purchaseFailsWithoutStripeConfigured() throws Exception {
        User resident = createUser("resident");
        mockMvc.perform(post("/api/parking/purchase").cookie(sessionCookie(resident)))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void purchaseStartsAStripeCheckoutSession() throws Exception {
        User resident = createUser("resident");
        when(stripeService.isConfigured()).thenReturn(true);
        when(stripeService.createParkingPassCheckoutSession(anyLong(), anyString(), anyLong(), anyString()))
                .thenReturn(new CheckoutSessionResult("cs_test_123", "https://checkout.stripe.com/pay/cs_test_123"));

        mockMvc.perform(post("/api/parking/purchase").cookie(sessionCookie(resident)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("requires_payment"))
                .andExpect(jsonPath("$.checkoutUrl").value("https://checkout.stripe.com/pay/cs_test_123"));
    }

    @Test
    void purchaseFailsWhenTheFlatAlreadyHasAPass() throws Exception {
        User resident = createUser("resident");
        savePass(resident.getFlatNumber());
        when(stripeService.isConfigured()).thenReturn(true);

        mockMvc.perform(post("/api/parking/purchase").cookie(sessionCookie(resident)))
                .andExpect(status().isConflict());
    }

    @Test
    void confirmFailsWithoutStripeConfigured() throws Exception {
        User resident = createUser("resident");
        mockMvc.perform(post("/api/parking/confirm")
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

        mockMvc.perform(post("/api/parking/confirm")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("{\"sessionId\":\"cs_test_123\"}"))
                .andExpect(status().isPaymentRequired());
    }

    @Test
    void confirmWithASessionBelongingToSomeoneElseFails() throws Exception {
        User resident = createUser("resident");
        when(stripeService.isConfigured()).thenReturn(true);
        when(stripeService.retrieveSession("cs_test_123")).thenReturn(new StripeSessionResult(
                "paid", "pi_123", 500000L,
                Map.of("residentId", "999999", "flatNumber", resident.getFlatNumber())));

        mockMvc.perform(post("/api/parking/confirm")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("{\"sessionId\":\"cs_test_123\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void confirmCreatesThePassOnSuccessfulPayment() throws Exception {
        User resident = createUser("resident");
        when(stripeService.isConfigured()).thenReturn(true);
        when(stripeService.retrieveSession("cs_test_123")).thenReturn(new StripeSessionResult(
                "paid", "pi_123", 500000L,
                Map.of("residentId", String.valueOf(resident.getId()), "flatNumber", resident.getFlatNumber())));

        mockMvc.perform(post("/api/parking/confirm")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("{\"sessionId\":\"cs_test_123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flatNumber").value(resident.getFlatNumber()))
                .andExpect(jsonPath("$.amountPaidCents").value(500000));

        verify(stripeService, never()).refund(anyString());
    }

    @Test
    void confirmingTheSameSessionTwiceReturnsTheExistingPassWithoutRecheckingStripe() throws Exception {
        User resident = createUser("resident");
        when(stripeService.isConfigured()).thenReturn(true);
        when(stripeService.retrieveSession("cs_test_123")).thenReturn(new StripeSessionResult(
                "paid", "pi_123", 500000L,
                Map.of("residentId", String.valueOf(resident.getId()), "flatNumber", resident.getFlatNumber())));

        String content = "{\"sessionId\":\"cs_test_123\"}";
        mockMvc.perform(post("/api/parking/confirm").cookie(sessionCookie(resident))
                        .contentType("application/json").content(content))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/parking/confirm").cookie(sessionCookie(resident))
                        .contentType("application/json").content(content))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flatNumber").value(resident.getFlatNumber()));

        verify(stripeService, times(1)).retrieveSession("cs_test_123");
    }

    @Test
    void confirmRefundsWhenTheFlatAlreadyHasAPassWhilePaying() throws Exception {
        User resident = createUser("resident");
        savePass(resident.getFlatNumber());

        when(stripeService.isConfigured()).thenReturn(true);
        when(stripeService.retrieveSession("cs_test_456")).thenReturn(new StripeSessionResult(
                "paid", "pi_456", 500000L,
                Map.of("residentId", String.valueOf(resident.getId()), "flatNumber", resident.getFlatNumber())));

        mockMvc.perform(post("/api/parking/confirm")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("{\"sessionId\":\"cs_test_456\"}"))
                .andExpect(status().isConflict());

        verify(stripeService, times(1)).refund("pi_456");
    }

    @Test
    void confirmRejectsAMalformedRequestBody() throws Exception {
        User resident = createUser("resident");
        mockMvc.perform(post("/api/parking/confirm")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    private void savePass(String flatNumber) {
        ParkingPass pass = new ParkingPass();
        pass.setFlatNumber(flatNumber);
        pass.setPurchasedByResidentId(1L);
        pass.setPurchasedByName("Existing Owner");
        pass.setAmountPaidCents(500000);
        parkingPassRepository.save(pass);
    }
}
