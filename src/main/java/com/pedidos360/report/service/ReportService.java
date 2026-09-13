package com.pedidos360.report.service;

import com.pedidos360.report.dto.ActiveStatusResponse;
import com.pedidos360.report.dto.LeadTimeResponse;
import com.pedidos360.report.dto.SalesByHourResponse;
import com.pedidos360.report.dto.TopProductsResponse;
import com.pedidos360.report.model.AuditEvent;
import com.pedidos360.report.model.Order;
import com.pedidos360.report.model.OrderItem;
import com.pedidos360.report.repository.AuditEventRepository;
import com.pedidos360.report.repository.OrderItemRepository;
import com.pedidos360.report.repository.OrderRepository;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final AuditEventRepository auditEventRepository;

    public ReportService(OrderRepository orderRepository,
                         OrderItemRepository orderItemRepository,
                         AuditEventRepository auditEventRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.auditEventRepository = auditEventRepository;
    }

    public List<SalesByHourResponse> getSalesByHour() {

        List<AuditEvent> deliveredEvents =
                auditEventRepository.findAll()
                        .stream()
                        .filter(event -> event.getTimestamp() != null)
                        .filter(event ->
                                "ORDER_DELIVERED".equalsIgnoreCase(event.getEventType())
                                        || isStatus(event, "ENTREGADO")
                        )
                        .toList();

        Map<Integer, List<AuditEvent>> eventsByHour =
                deliveredEvents.stream()
                        .collect(Collectors.groupingBy(
                                event -> event.getTimestamp().getHour()
                        ));

        return eventsByHour.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {

                        double totalSales = entry.getValue()
                                .stream()
                                .mapToDouble(this::getOrderTotalFromEvent)
                                .sum();

                        long totalOrders = entry.getValue()
                                .stream()
                                .map(AuditEvent::getOrderId)
                                .filter(Objects::nonNull)
                                .distinct()
                                .count();

                        return new SalesByHourResponse(
                                entry.getKey(),
                                totalSales,
                                totalOrders
                        );
                })
                .toList();
        }


    public List<LeadTimeResponse> getLeadTime() {

        List<AuditEvent> events =
                auditEventRepository.findAll();

        Map<String, List<AuditEvent>> eventsByOrder =
                events.stream()
                        .filter(event -> event.getOrderId() != null)
                        .collect(Collectors.groupingBy(
                                AuditEvent::getOrderId
                        ));

        List<LeadTimeResponse> results = new ArrayList<>();

        for (Map.Entry<String, List<AuditEvent>> entry :
                eventsByOrder.entrySet()) {

                Optional<AuditEvent> created =
                        entry.getValue()
                                .stream()
                                .filter(event -> event.getTimestamp() != null)
                                .filter(event ->
                                        "ORDER_CREATED".equalsIgnoreCase(event.getEventType())
                                                || isStatus(event, "CREADO")
                                )
                                .min(Comparator.comparing(
                                        AuditEvent::getTimestamp
                                ));

                Optional<AuditEvent> delivered =
                        entry.getValue()
                                .stream()
                                .filter(event -> event.getTimestamp() != null)
                                .filter(event ->
                                        "ORDER_DELIVERED".equalsIgnoreCase(event.getEventType())
                                                || isStatus(event, "ENTREGADO")
                                )
                                .max(Comparator.comparing(
                                        AuditEvent::getTimestamp
                                ));

                if (created.isPresent() && delivered.isPresent()) {

                long minutes = Duration.between(
                        created.get().getTimestamp(),
                        delivered.get().getTimestamp()
                ).toMinutes();

                results.add(
                        new LeadTimeResponse(
                                entry.getKey(),
                                minutes
                        )
                );
                }
        }

        return results;
        }

    public List<ActiveStatusResponse> getActiveStatuses() {

        List<Order> orders = orderRepository.findAll();

        Map<String, Long> grouped =
                orders.stream()
                        .filter(order -> order.getStatus() != null)
                        .filter(order ->
                                !"ENTREGADO".equalsIgnoreCase(order.getStatus()))
                        .filter(order ->
                                !"CANCELADO".equalsIgnoreCase(order.getStatus()))
                        .collect(Collectors.groupingBy(
                                Order::getStatus,
                                Collectors.counting()
                        ));

        return grouped.entrySet()
                .stream()
                .map(entry ->
                        new ActiveStatusResponse(
                                entry.getKey(),
                                entry.getValue()
                        )
                )
                .sorted(Comparator.comparing(
                        ActiveStatusResponse::getStatus
                ))
                .toList();
    }

    public List<TopProductsResponse> getTopProducts() {

                List<OrderItem> items =
                        orderItemRepository.findAll();

                Map<String, List<OrderItem>> grouped =
                        items.stream()
                                .collect(Collectors.groupingBy(
                                        item -> item.getProductId() != null
                                                ? item.getProductId()
                                                : "SIN-ID"
                                ));

                return grouped.values()
                        .stream()
                        .map(productItems -> {

                        OrderItem first = productItems.get(0);

                        long quantity = productItems.stream()
                                .mapToLong(item ->
                                        item.getQuantity() != null
                                                ? item.getQuantity()
                                                : 0
                                )
                                .sum();

                        double totalSales = productItems.stream()
                                .mapToDouble(item -> {

                                        double price = item.getUnitPrice() != null
                                                ? item.getUnitPrice()
                                                : 0.0;

                                        int quantityItem = item.getQuantity() != null
                                                ? item.getQuantity()
                                                : 0;

                                        return price * quantityItem;
                                })
                                .sum();

                        return new TopProductsResponse(
                                first.getProductId(),
                                first.getProductName(),
                                quantity,
                                totalSales
                        );
                        })
                        .sorted(Comparator.comparing(
                                TopProductsResponse::getQuantitySold
                        ).reversed())
                        .toList();
        }
        private boolean isStatus(AuditEvent event, String status) {

        if (event.getDetails() == null) {
                return false;
        }

        return event.getDetails().contains("\"status\":\"" + status + "\"");
        }


        private double getOrderTotalFromEvent(AuditEvent event) {

        if (event.getDetails() == null) {
                return 0.0;
        }

        try {
                String details = event.getDetails();

                String search = "\"totalAmount\":";
                int start = details.indexOf(search);

                if (start == -1) {
                return 0.0;
                }

                start += search.length();

                int end = details.indexOf(",", start);

                if (end == -1) {
                end = details.indexOf("}", start);
                }

                if (end == -1) {
                return 0.0;
                }

                String value = details.substring(start, end).trim();

                return Double.parseDouble(value);

        } catch (Exception e) {
                return 0.0;
        }
        }
}