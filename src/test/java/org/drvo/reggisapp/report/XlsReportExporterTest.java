package org.drvo.reggisapp.report;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.drvo.reggisapp.domain.model.Cliente;
import org.drvo.reggisapp.domain.model.EstadoCliente;
import org.drvo.reggisapp.domain.model.EstadoPedido;
import org.drvo.reggisapp.domain.model.Moneda;
import org.drvo.reggisapp.domain.model.Pago;
import org.drvo.reggisapp.domain.model.Pedido;
import org.drvo.reggisapp.domain.model.ResumenTiempo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XlsReportExporterTest {
    @TempDir Path directorioTemporal;
    private final XlsReportExporter exporter = new XlsReportExporter();

    @Test
    void reporteDeClienteIncluyeDatosContactoPedidosPagosYSaldos() throws Exception {
        Cliente cliente = new Cliente(9L, "=Cliente de prueba", EstadoCliente.ACTIVO,
                Map.of("rif", "J-123", "email", "ana@example.com", "telefono", "0412-1234567", "direccion", "Calle 1"));
        ReportePedido reporte = crearReportePedido();
        Path destino = directorioTemporal.resolve("cliente.xls");

        exporter.exportarCliente(cliente, List.of(reporte), destino);

        assertTrue(Files.exists(destino));
        try (InputStream entrada = Files.newInputStream(destino); HSSFWorkbook libro = new HSSFWorkbook(entrada)) {
            var hoja = libro.getSheet("Cobranza");
            assertEquals("=Cliente de prueba", hoja.getRow(1).getCell(1).getStringCellValue());
            assertEquals("J-123", hoja.getRow(2).getCell(1).getStringCellValue());
            assertEquals("ana@example.com", hoja.getRow(3).getCell(1).getStringCellValue());
            assertEquals(9, hoja.getLastRowNum());
            assertEquals("Pago", hoja.getRow(8).getCell(0).getStringCellValue());
            assertEquals("Pedido de prueba", hoja.getRow(8).getCell(2).getStringCellValue());
            assertEquals(40.00, hoja.getRow(8).getCell(12).getNumericCellValue());
            assertEquals(60.00, hoja.getRow(9).getCell(12).getNumericCellValue());
            assertEquals("4", hoja.getRow(9).getCell(16).getStringCellValue());
        }
    }

    @Test
    void reporteEspecificoIncluyeSoloElPedidoSeleccionado() throws Exception {
        Cliente cliente = new Cliente(9L, "Ana", EstadoCliente.ACTIVO, Map.of("rif", "J-123"));
        Path destino = directorioTemporal.resolve("pedido.xls");

        exporter.exportarPedido(cliente, crearReportePedido(), destino);

        try (InputStream entrada = Files.newInputStream(destino); HSSFWorkbook libro = new HSSFWorkbook(entrada)) {
            var hoja = libro.getSheet("Cobranza");
            assertEquals("Reporte de pedido · Reggis", hoja.getRow(0).getCell(0).getStringCellValue());
            assertEquals("Pedido de prueba", hoja.getRow(8).getCell(2).getStringCellValue());
        }
    }

    private ReportePedido crearReportePedido() {
        LocalDate fechaPedido = LocalDate.of(2026, 10, 1);
        Pedido pedido = new Pedido(77L, 9L, "Pedido de prueba", new BigDecimal("100.00"),
                new BigDecimal("10.00"), Moneda.USD, new BigDecimal("10.00"), fechaPedido, EstadoPedido.COMPLETADO);
        List<Pago> pagos = List.of(
                new Pago(301L, 77L, fechaPedido.plusDays(1), new BigDecimal("40.00"),
                        new BigDecimal("4.00"), Moneda.USD, new BigDecimal("10.00")),
                new Pago(302L, 77L, fechaPedido.plusDays(4), new BigDecimal("60.00"),
                        new BigDecimal("6.00"), Moneda.USD, new BigDecimal("10.00")));
        return new ReportePedido(pedido, pagos, BigDecimal.ZERO.setScale(2), new ResumenTiempo(2, 1, 4L));
    }
}
