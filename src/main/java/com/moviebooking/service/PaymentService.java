package com.moviebooking.service;

import com.moviebooking.model.PaymentEntity;
import com.moviebooking.model.PaymentRequest;
import com.moviebooking.repository.PaymentRepository;
import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final APIContext apiContext;
    private final PaymentRepository paymentRepository;

    public Payment createPayment(PaymentRequest paymentRequest) throws PayPalRESTException {
        Amount amount = new Amount();
        amount.setCurrency(paymentRequest.getCurrency());
        amount.setTotal(String.format("%.2f", paymentRequest.getAmount()));

        Transaction transaction = new Transaction();
        transaction.setDescription(paymentRequest.getDescription());
        transaction.setAmount(amount);

        List<Transaction> transactions = new ArrayList<>();
        transactions.add(transaction);

        Payer payer = new Payer();
        payer.setPaymentMethod("paypal");

        Payment payment = new Payment();
        payment.setIntent("sale");
        payment.setPayer(payer);
        payment.setTransactions(transactions);

        RedirectUrls redirectUrls = new RedirectUrls();
        redirectUrls.setCancelUrl(paymentRequest.getCancelUrl());
        redirectUrls.setReturnUrl(paymentRequest.getReturnUrl());
        payment.setRedirectUrls(redirectUrls);

        Payment createdPayment = payment.create(apiContext);
        
        // Save initial payment record
        PaymentEntity paymentEntity = new PaymentEntity();
        paymentEntity.setPaymentId(createdPayment.getId());
        paymentEntity.setAmount(paymentRequest.getAmount());
        paymentEntity.setCurrency(paymentRequest.getCurrency());
        paymentEntity.setDescription(paymentRequest.getDescription());
        paymentEntity.setStatus("CREATED");
        paymentEntity.setCreatedAt(LocalDateTime.now());
        paymentEntity.setBookingId(paymentRequest.getBookingId());
        
        paymentRepository.save(paymentEntity);

        return createdPayment;
    }

    public Payment executePayment(String paymentId, String payerId) throws PayPalRESTException {
        PaymentExecution paymentExecution = new PaymentExecution();
        paymentExecution.setPayerId(payerId);

        Payment payment = new Payment();
        payment.setId(paymentId);

        Payment executedPayment = payment.execute(apiContext, paymentExecution);
        
        // Update payment record
        PaymentEntity paymentEntity = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        paymentEntity.setStatus(executedPayment.getState().toUpperCase());
        paymentEntity.setPayerId(payerId);
        paymentRepository.save(paymentEntity);

        return executedPayment;
    }

    public Payment getPaymentDetails(String paymentId) throws PayPalRESTException {
        return Payment.get(apiContext, paymentId);
    }

    public List<PaymentEntity> getAllPayments() {
        return paymentRepository.findAll();
    }

    public PaymentEntity getLocalPaymentDetails(String paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }

    public Payment verifyPayment(String paymentId, String payerId) throws PayPalRESTException {
        // First check if payment exists in local database
        Optional<PaymentEntity> localPayment = paymentRepository.findById(paymentId);
        if (localPayment.isPresent() && "COMPLETED".equals(localPayment.get().getStatus())) {
            // Payment already verified, return the payment details
            return Payment.get(apiContext, paymentId);
        }
    
        // If payment not completed and payerId provided, execute it
        if (payerId != null) {
            PaymentExecution paymentExecution = new PaymentExecution();
            paymentExecution.setPayerId(payerId);
    
            Payment payment = new Payment();
            payment.setId(paymentId);
    
            Payment executedPayment = payment.execute(apiContext, paymentExecution);
    
            // Normalize PayPal state to match frontend expectations
            String state = executedPayment.getState();
            String normalizedStatus = "approved".equalsIgnoreCase(state) ? "paid" : state;
    
            // Update local payment record
            PaymentEntity paymentEntity = localPayment.orElseGet(PaymentEntity::new);
            paymentEntity.setPaymentId(paymentId);
            paymentEntity.setPayerId(payerId);
            paymentEntity.setStatus(normalizedStatus);
            paymentRepository.save(paymentEntity);
    
            return executedPayment;
        }
    
        // If no payerId, just get payment details
        return Payment.get(apiContext, paymentId);
    }
    

    
} 