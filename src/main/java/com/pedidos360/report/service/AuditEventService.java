package com.pedidos360.report.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.report.model.AuditEvent;
import com.pedidos360.report.repository.AuditEventRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/** Procesa el evento de auditoría y persiste su representación en PostgreSQL. */
@Service
public class AuditEventService {

    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;

    public AuditEventService(
            AuditEventRepository auditEventRepository,
            ObjectMapper objectMapper) {
        this.auditEventRepository = auditEventRepository;
        this.objectMapper = objectMapper;
    }

    public void saveFromJson(String payload) {
        final JsonNode json;
        try {
            json = objectMapper.readTree(payload);
        } catch (JsonProcessingException ex) {
            throw new InvalidAuditEventException("El mensaje no contiene JSON válido", ex);
        }

        if (json == null || !json.isObject()) {
            throw new InvalidAuditEventException("El evento debe ser un objeto JSON");
        }

        AuditEvent event = new AuditEvent();
        event.setEventId(getText(json, "eventId"));
        event.setEventType(getText(json, "type"));
        event.setOrderId(getText(json, "orderId"));
        event.setActor(getText(json, "actor"));
        event.setDetails(payload);

        String timestamp = getText(json, "timestamp");
        if (timestamp == null || timestamp.isBlank()) {
            event.setTimestamp(LocalDateTime.now());
        } else {
            try {
                event.setTimestamp(parseTimestamp(timestamp));
            } catch (DateTimeParseException ex) {
                throw new InvalidAuditEventException(
                        "El campo timestamp no tiene un formato ISO válido", ex);
            }
        }

        auditEventRepository.save(event);
    }

    private LocalDateTime parseTimestamp(String value) {
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException localDateTimeException) {
            // También acepta instantes ISO con zona horaria, por ejemplo 2026-10-17T15:30:00Z.
            return OffsetDateTime.parse(value).toLocalDateTime();
        }
    }

    private String getText(JsonNode json, String field) {
        JsonNode node = json.get(field);
        return node != null && !node.isNull() ? node.asText() : null;
    }
}
