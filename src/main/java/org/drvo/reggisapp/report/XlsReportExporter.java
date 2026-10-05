package org.drvo.reggisapp.report;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.drvo.reggisapp.domain.model.Cliente;
import org.drvo.reggisapp.domain.model.Pago;
import org.drvo.reggisapp.domain.model.Pedido;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Exporta libros Excel binarios .xls compatibles con Excel 97-2003. */
public final class XlsReportExporter implements ExcelReportExporter {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final List<String> ENCABEZADOS = List.of(
            "Tipo de registro", "ID pedido", "Descripción", "Fecha creación", "Estado",
            "Total pedido BS", "Moneda original", "Monto original", "Tasa BS/USD",
            "Fecha de pago", "Monto pagado original", "Moneda de pago", "Pago BS", "Saldo actual BS",
            "Promedio días entre pagos", "Días hasta primer pago", "Días total de deuda");
    private static final int MAX_FILAS_HSSF = 65_536;

    @Override
    public void exportarCliente(Cliente cliente, List<ReportePedido> pedidos, Path destino) throws IOException {
        escribir("Reporte de cliente", cliente, pedidos, destino);
    }

    @Override
    public void exportarPedido(Cliente cliente, ReportePedido pedido, Path destino) throws IOException {
        escribir("Reporte de pedido", cliente, List.of(pedido), destino);
    }

    private void escribir(String titulo, Cliente cliente, List<ReportePedido> pedidos, Path destino) throws IOException {
        if (cliente == null || destino == null) throw new IllegalArgumentException("Cliente y destino son obligatorios.");
        if (pedidos == null) throw new IllegalArgumentException("La lista de pedidos es obligatoria.");
        long filasNecesarias = 8L + pedidos.stream().mapToLong(p -> Math.max(1, p.pagos().size() + 1L)).sum();
        if (filasNecesarias > MAX_FILAS_HSSF) {
            throw new IOException("El archivo .xls alcanzó su límite de 65.536 filas. Exporte menos pedidos por archivo.");
        }

        Path absoluta = destino.toAbsolutePath();
        Path directorio = absoluta.getParent();
        if (directorio != null) Files.createDirectories(directorio);

        try (Workbook libro = new HSSFWorkbook()) {
            Sheet hoja = libro.createSheet("Cobranza");
            CellStyle estiloTitulo = estiloTitulo(libro);
            CellStyle estiloEncabezado = estiloEncabezado(libro);
            CellStyle estiloMonto = estiloMonto(libro);
            crearTexto(hoja.createRow(0), 0, titulo + " · Reggis", estiloTitulo);
            crearTexto(hoja.createRow(1), 0, "Cliente", estiloEncabezado);
            crearTexto(hoja.getRow(1), 1, cliente.getNombre());
            crearTexto(hoja.createRow(2), 0, "RIF");
            crearTexto(hoja.getRow(2), 1, cliente.getDatosAdicionales().get("rif"));
            crearTexto(hoja.createRow(3), 0, "Correo");
            crearTexto(hoja.getRow(3), 1, cliente.getDatosAdicionales().get("email"));
            crearTexto(hoja.createRow(4), 0, "Teléfono");
            crearTexto(hoja.getRow(4), 1, cliente.getDatosAdicionales().get("telefono"));
            crearTexto(hoja.createRow(5), 0, "Dirección");
            crearTexto(hoja.getRow(5), 1, cliente.getDatosAdicionales().get("direccion"));

            Row encabezado = hoja.createRow(7);
            for (int i = 0; i < ENCABEZADOS.size(); i++) crearTexto(encabezado, i, ENCABEZADOS.get(i), estiloEncabezado);
            int numeroFila = 8;
            for (ReportePedido reporte : pedidos) {
                if (reporte.pagos().isEmpty()) {
                    escribirFilaPedido(hoja.createRow(numeroFila++), reporte, null, estiloMonto);
                } else {
                    for (Pago pago : reporte.pagos()) escribirFilaPedido(hoja.createRow(numeroFila++), reporte, pago, estiloMonto);
                }
            }
            hoja.createFreezePane(0, 8);
            hoja.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(7, Math.max(7, numeroFila - 1), 0,
                    ENCABEZADOS.size() - 1));
            int[] anchos = {18, 12, 32, 15, 14, 18, 15, 18, 15, 15, 20, 15, 18, 18, 22, 20, 19};
            for (int i = 0; i < anchos.length; i++) hoja.setColumnWidth(i, anchos[i] * 256);
            try (OutputStream salida = Files.newOutputStream(absoluta)) {
                libro.write(salida);
            }
        }
    }

    private void escribirFilaPedido(Row fila, ReportePedido reporte, Pago pago, CellStyle estiloMonto) {
        Pedido pedido = reporte.pedido();
        texto(fila, 0, pago == null ? "Pedido" : "Pago");
        texto(fila, 1, pedido.getId().toString());
        texto(fila, 2, pedido.getDescripcion());
        texto(fila, 3, FECHA.format(pedido.getFechaCreacion()));
        texto(fila, 4, pedido.getEstado().name());
        numero(fila, 5, pedido.getMontoPedidoBs(), estiloMonto);
        texto(fila, 6, pedido.getMonedaOriginal().name());
        numero(fila, 7, pedido.getMontoOriginal(), estiloMonto);
        numero(fila, 8, pedido.getTasaCambio(), estiloMonto);
        if (pago != null) {
            texto(fila, 9, FECHA.format(pago.getFechaPago()));
            numero(fila, 10, pago.getMontoOriginal(), estiloMonto);
            texto(fila, 11, pago.getMonedaOriginal().name());
            numero(fila, 12, pago.getMontoBs(), estiloMonto);
        }
        numero(fila, 13, reporte.saldoPendiente(), estiloMonto);
        texto(fila, 14, Long.toString(reporte.resumenTiempo().getPromedioDiasEntrePagos()));
        texto(fila, 15, Long.toString(reporte.resumenTiempo().getDiasHastaPrimerPago()));
        texto(fila, 16, reporte.resumenTiempo().getDiasHastaCancelacion() == null
                ? "Pendiente" : reporte.resumenTiempo().getDiasHastaCancelacion().toString());
    }

    private CellStyle estiloMonto(Workbook libro) {
        CellStyle estilo = libro.createCellStyle();
        DataFormat formato = libro.createDataFormat();
        estilo.setDataFormat(formato.getFormat("#,##0.00"));
        return estilo;
    }

    private void numero(Row fila, int columna, java.math.BigDecimal valor, CellStyle estilo) {
        Cell celda = fila.createCell(columna);
        celda.setCellValue(valor.doubleValue());
        celda.setCellStyle(estilo);
    }

    private CellStyle estiloTitulo(Workbook libro) {
        CellStyle estilo = libro.createCellStyle();
        Font fuente = libro.createFont();
        fuente.setBold(true);
        fuente.setFontHeightInPoints((short) 16);
        fuente.setColor(IndexedColors.DARK_BLUE.getIndex());
        estilo.setFont(fuente);
        return estilo;
    }

    private CellStyle estiloEncabezado(Workbook libro) {
        CellStyle estilo = libro.createCellStyle();
        Font fuente = libro.createFont();
        fuente.setBold(true);
        fuente.setColor(IndexedColors.WHITE.getIndex());
        estilo.setFont(fuente);
        estilo.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return estilo;
    }

    private void texto(Row fila, int columna, String valor) {
        crearTexto(fila, columna, valor);
    }

    private void crearTexto(Row fila, int columna, String valor) {
        Cell celda = fila.createCell(columna);
        celda.setCellValue(valor == null ? "" : valor);
    }

    private void crearTexto(Row fila, int columna, String valor, CellStyle estilo) {
        crearTexto(fila, columna, valor);
        fila.getCell(columna).setCellStyle(estilo);
    }
}
