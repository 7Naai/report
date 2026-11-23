package com.pedidos360.report.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderEventDto(
        String id,
        String userId,
        String customerId,
        String status,
        BigDecimal totalAmount,
        LocalDateTime createdAt
) {}