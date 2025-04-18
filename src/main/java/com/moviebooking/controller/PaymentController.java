package com.moviebooking.controller;

import com.moviebooking.model.PaymentEntity;
import com.moviebooking.model.PaymentRequest;
import com.moviebooking.model.PaymentVerificationRequest;
import com.moviebooking.service.PaymentService;
import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.beans.factory.annotation.Value;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    @Value("${CLIENT_URL}")
    private String clientUrl;

    @PostMapping("/create")
    public ResponseEntity<?> createPayment(@RequestBody PaymentRequest paymentRequest) {
        try {
            Payment payment = paymentService.createPayment(paymentRequest);
            String approvalUrl = payment.getLinks().stream()
                    .filter(link -> "approval_url".equals(link.getRel()))
                    .findFirst()
                    .orElseThrow(() -> new PayPalRESTException("No approval URL found"))
                    .getHref();

            Map<String, String> response = new HashMap<>();
            response.put("approvalUrl", approvalUrl);
            return ResponseEntity.ok(response);
        } catch (PayPalRESTException e) {
            return ResponseEntity.badRequest().body("Error creating payment: " + e.getMessage());
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody PaymentVerificationRequest request) {
        try {
            Payment payment = paymentService.verifyPayment(request.getPaymentId(), request.getPayerID());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("status", payment.getState());
            response.put("id", payment.getId());
            response.put("state", payment.getState());
            return ResponseEntity.ok(response);
        } catch (PayPalRESTException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @GetMapping("/execute")
public RedirectView executePayment(@RequestParam("paymentId") String paymentId,
                                   @RequestParam("PayerID") String payerId) {
    try {
        paymentService.executePayment(paymentId, payerId); // Cleaned line
        PaymentEntity entity = paymentService.getLocalPaymentDetails(paymentId);
        String bookingId = entity.getBookingId();

        return new RedirectView(clientUrl + "/payment/success?paymentId=" + paymentId + "&bookingId=" + bookingId);
    } catch (PayPalRESTException e) {
        return new RedirectView(clientUrl + "/payment-error.html?error=" + e.getMessage());
    }
}


    @GetMapping("/{paymentId}")
    public ResponseEntity<?> getPaymentDetails(@PathVariable String paymentId) {
        try {
            Payment payment = paymentService.getPaymentDetails(paymentId);
            return ResponseEntity.ok(payment);
        } catch (PayPalRESTException e) {
            return ResponseEntity.badRequest().body("Error getting payment details: " + e.getMessage());
        }
    }

    @GetMapping("/local/{paymentId}")
    public ResponseEntity<PaymentEntity> getLocalPaymentDetails(@PathVariable String paymentId) {
        try {
            PaymentEntity payment = paymentService.getLocalPaymentDetails(paymentId);
            return ResponseEntity.ok(payment);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/local")
    public ResponseEntity<List<PaymentEntity>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }
} 