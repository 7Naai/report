package com.pedidos360.report.repository;

import com.pedidos360.report.model.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    List<AuditEvent> findByEventType(String eventType);

    List<AuditEvent> findByOrderIdOrderByTimestampAsc(String orderId);

    List<AuditEvent> findByTimestampBetween(
            LocalDateTime from,
            LocalDateTime to
    );
}