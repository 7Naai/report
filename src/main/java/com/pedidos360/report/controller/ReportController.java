package com.pedidos360.report.controller;

import com.pedidos360.report.dto.ActiveStatusResponse;
import com.pedidos360.report.dto.LeadTimeResponse;
import com.pedidos360.report.dto.SalesByHourResponse;
import com.pedidos360.report.dto.TopProductsResponse;
import com.pedidos360.report.service.ReportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/report")
@Tag(
        name = "Reportería",
        description = "KPIs y analítica de Pedidos360"
)
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/sales-by-hour")
    @Operation(
            summary = "Ventas por hora",
            description = "Obtiene las ventas agrupadas por hora"
    )
    public ResponseEntity<List<SalesByHourResponse>> getSalesByHour() {

        return ResponseEntity.ok(
                reportService.getSalesByHour()
        );
    }

    @GetMapping("/lead-time")
    @Operation(
            summary = "Lead time de pedidos",
            description = "Obtiene el tiempo entre creación y entrega"
    )
    public ResponseEntity<List<LeadTimeResponse>> getLeadTime() {

        return ResponseEntity.ok(
                reportService.getLeadTime()
        );
    }

    @GetMapping("/active-status")
    @Operation(
            summary = "Estados activos",
            description = "Obtiene la cantidad de pedidos en estados activos"
    )
    public ResponseEntity<List<ActiveStatusResponse>> getActiveStatuses() {

        return ResponseEntity.ok(
                reportService.getActiveStatuses()
        );
    }

    @GetMapping("/top-products")
    @Operation(
            summary = "Productos más vendidos",
            description = "Obtiene los productos ordenados por cantidad vendida"
    )
    public ResponseEntity<List<TopProductsResponse>> getTopProducts() {

        return ResponseEntity.ok(
                reportService.getTopProducts()
        );
    }
}