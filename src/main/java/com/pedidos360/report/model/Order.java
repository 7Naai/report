package com.pedidos360.report.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @Column(name = "id", nullable = false, length = 255)
    private String id;

    @Column(name = "customer")
    private String customer;

    @Column(name = "description")
    private String description;

    @Column(name = "status")
    private String status;

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    public Order() {
    }

    public Order(String id, String customer, String description,
                 String status, Double totalAmount) {
        this.id = id;
        this.customer = customer;
        this.description = description;
        this.status = status;
        this.totalAmount = totalAmount;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomer() {
        return customer;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }
}