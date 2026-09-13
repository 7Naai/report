package com.pedidos360.report.dto;

public class TopProductsResponse {

    private String productId;
    private String productName;
    private Long quantitySold;
    private Double totalSales;

    public TopProductsResponse() {
    }

    public TopProductsResponse(String productId,
                               String productName,
                               Long quantitySold,
                               Double totalSales) {
        this.productId = productId;
        this.productName = productName;
        this.quantitySold = quantitySold;
        this.totalSales = totalSales;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Long getQuantitySold() {
        return quantitySold;
    }

    public void setQuantitySold(Long quantitySold) {
        this.quantitySold = quantitySold;
    }

    public Double getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(Double totalSales) {
        this.totalSales = totalSales;
    }
}