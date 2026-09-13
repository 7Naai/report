package com.pedidos360.report.dto;

public class SalesByHourResponse {

    private Integer hour;
    private Double totalSales;
    private Long totalOrders;

    public SalesByHourResponse() {
    }

    public SalesByHourResponse(Integer hour,
                               Double totalSales,
                               Long totalOrders) {
        this.hour = hour;
        this.totalSales = totalSales;
        this.totalOrders = totalOrders;
    }

    public Integer getHour() {
        return hour;
    }

    public void setHour(Integer hour) {
        this.hour = hour;
    }

    public Double getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(Double totalSales) {
        this.totalSales = totalSales;
    }

    public Long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(Long totalOrders) {
        this.totalOrders = totalOrders;
    }
}