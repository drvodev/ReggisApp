package org.drvo.reggisapp.report;

import org.drvo.reggisapp.domain.model.Pago;
import org.drvo.reggisapp.domain.model.Pedido;
import org.drvo.reggisapp.domain.model.ResumenTiempo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/** Datos ya reunidos para exportar un pedido y su historial de pagos. */
public record ReportePedido(Pedido pedido, List<Pago> pagos, BigDecimal saldoPendiente,
                            ResumenTiempo resumenTiempo) {
    public ReportePedido {
        Objects.requireNonNull(pedido, "El pedido es obligatorio.");
        pagos = List.copyOf(pagos == null ? List.of() : pagos);
        saldoPendiente = Objects.requireNonNull(saldoPendiente, "El saldo pendiente es obligatorio.");
        Objects.requireNonNull(resumenTiempo, "El resumen de tiempo es obligatorio.");
    }
}
