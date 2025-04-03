package com.moviebooking.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
public class PaymentEntity {
    @Id
    private String paymentId;
    private String status;
    private Double amount;
    private String currency;
    private String description;
    private LocalDateTime createdAt;
    private String payerId;
} 