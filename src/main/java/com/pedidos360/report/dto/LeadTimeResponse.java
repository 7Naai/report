package com.pedidos360.report.dto;

public class LeadTimeResponse {

    private String orderId;
    private Long leadTimeMinutes;

    public LeadTimeResponse() {
    }

    public LeadTimeResponse(String orderId, Long leadTimeMinutes) {
        this.orderId = orderId;
        this.leadTimeMinutes = leadTimeMinutes;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public Long getLeadTimeMinutes() {
        return leadTimeMinutes;
    }

    public void setLeadTimeMinutes(Long leadTimeMinutes) {
        this.leadTimeMinutes = leadTimeMinutes;
    }
}