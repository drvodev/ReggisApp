package org.drvo.reggisapp.report;

import org.drvo.reggisapp.domain.model.Cliente;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface ExcelReportExporter {
    void exportarCliente(Cliente cliente, List<ReportePedido> pedidos, Path destino) throws IOException;
    void exportarPedido(Cliente cliente, ReportePedido pedido, Path destino) throws IOException;
}
