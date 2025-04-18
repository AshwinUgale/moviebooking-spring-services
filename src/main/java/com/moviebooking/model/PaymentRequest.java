package com.moviebooking.model;

import lombok.Data;

@Data
public class PaymentRequest {
    private String currency;
    private String description;
    private Double amount;
    private String returnUrl;
    private String cancelUrl;
    private String bookingId;
} 