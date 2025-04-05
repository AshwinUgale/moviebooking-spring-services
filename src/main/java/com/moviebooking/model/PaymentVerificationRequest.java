package com.moviebooking.model;

import lombok.Data;

@Data
public class PaymentVerificationRequest {
    private String paymentId;
    private String payerID;
} 