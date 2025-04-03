package com.moviebooking.controller;

import com.moviebooking.model.PaymentEntity;
import com.moviebooking.model.PaymentRequest;
import com.moviebooking.service.PaymentService;
import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

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

    @GetMapping("/execute")
    public RedirectView executePayment(@RequestParam("paymentId") String paymentId,
                                     @RequestParam("PayerID") String payerId) {
        try {
            Payment payment = paymentService.executePayment(paymentId, payerId);
            // Redirect to success page
            return new RedirectView("/payment-success.html?paymentId=" + paymentId);
        } catch (PayPalRESTException e) {
            // Redirect to error page
            return new RedirectView("/payment-error.html?error=" + e.getMessage());
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