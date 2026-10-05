package org.drvo.reggisapp.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public class Pago {
    private final Long id;
    private final Long pedidoId;
    private final LocalDate fechaPago;
    private final BigDecimal montoBs;
    private final BigDecimal montoOriginal;
    private final Moneda monedaOriginal;
    private final BigDecimal tasaCambio;

    public Pago(Long id, Long pedidoId, LocalDate fechaPago, BigDecimal montoBs, BigDecimal montoOriginal,
                Moneda monedaOriginal, BigDecimal tasaCambio) {
        this.id = id;
        this.pedidoId = Objects.requireNonNull(pedidoId, "El pago debe estar asociado a un pedido.");
        this.fechaPago = Objects.requireNonNull(fechaPago, "La fecha del pago es obligatoria.");
        this.montoBs = requirePositive(montoBs, "El monto en BS debe ser mayor que cero.");
        this.montoOriginal = requirePositive(montoOriginal, "El monto ingresado debe ser mayor que cero.");
        this.monedaOriginal = Objects.requireNonNull(monedaOriginal, "La moneda original es obligatoria.");
        this.tasaCambio = requirePositive(tasaCambio, "La tasa de cambio debe ser mayor que cero.");
    }

    public Long getId() { return id; }
    public Long getPedidoId() { return pedidoId; }
    public LocalDate getFechaPago() { return fechaPago; }
    public BigDecimal getMontoBs() { return montoBs; }
    public BigDecimal getMontoOriginal() { return montoOriginal; }
    public Moneda getMonedaOriginal() { return monedaOriginal; }
    public BigDecimal getTasaCambio() { return tasaCambio; }
    public Pago conId(Long nuevoId) { return new Pago(nuevoId, pedidoId, fechaPago, montoBs, montoOriginal,
            monedaOriginal, tasaCambio); }

    private static BigDecimal requirePositive(BigDecimal value, String message) {
        if (value == null || value.signum() <= 0) throw new IllegalArgumentException(message);
        return value;
    }
}
