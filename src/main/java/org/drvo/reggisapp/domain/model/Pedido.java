package org.drvo.reggisapp.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public class Pedido {
    private final Long id;
    private final Long clienteId;
    private final BigDecimal montoPedidoBs;
    private final BigDecimal montoOriginal;
    private final Moneda monedaOriginal;
    private final BigDecimal tasaCambio;
    private final LocalDate fechaCreacion;
    private final EstadoPedido estado;

    public Pedido(Long id, Long clienteId, BigDecimal montoPedidoBs, BigDecimal montoOriginal,
                  Moneda monedaOriginal, BigDecimal tasaCambio, LocalDate fechaCreacion, EstadoPedido estado) {
        this.id = id;
        this.clienteId = Objects.requireNonNull(clienteId, "El pedido debe pertenecer a un cliente.");
        this.montoPedidoBs = requirePositive(montoPedidoBs, "El monto del pedido debe ser mayor que cero.");
        this.montoOriginal = requirePositive(montoOriginal, "El monto original debe ser mayor que cero.");
        this.monedaOriginal = Objects.requireNonNull(monedaOriginal, "La moneda original es obligatoria.");
        this.tasaCambio = requirePositive(tasaCambio, "La tasa de cambio debe ser mayor que cero.");
        this.fechaCreacion = Objects.requireNonNull(fechaCreacion, "La fecha de creación es obligatoria.");
        this.estado = Objects.requireNonNull(estado, "El estado del pedido es obligatorio.");
    }

    public static Pedido nuevo(Long clienteId, BigDecimal montoPedidoBs, BigDecimal montoOriginal,
                               Moneda monedaOriginal, BigDecimal tasaCambio, LocalDate fechaCreacion) {
        return new Pedido(null, clienteId, montoPedidoBs, montoOriginal, monedaOriginal,
                tasaCambio, fechaCreacion, EstadoPedido.ACTIVO);
    }

    public Long getId() { return id; }
    public Long getClienteId() { return clienteId; }
    public BigDecimal getMontoPedidoBs() { return montoPedidoBs; }
    public BigDecimal getMontoOriginal() { return montoOriginal; }
    public Moneda getMonedaOriginal() { return monedaOriginal; }
    public BigDecimal getTasaCambio() { return tasaCambio; }
    public LocalDate getFechaCreacion() { return fechaCreacion; }
    public EstadoPedido getEstado() { return estado; }
    public Pedido conId(Long nuevoId) { return new Pedido(nuevoId, clienteId, montoPedidoBs, montoOriginal,
            monedaOriginal, tasaCambio, fechaCreacion, estado); }
    public Pedido marcarCompletado() { return cambiarEstado(EstadoPedido.COMPLETADO); }
    public Pedido anular() { return cambiarEstado(EstadoPedido.ANULADO); }

    private Pedido cambiarEstado(EstadoPedido nuevoEstado) {
        return new Pedido(id, clienteId, montoPedidoBs, montoOriginal, monedaOriginal,
                tasaCambio, fechaCreacion, nuevoEstado);
    }

    private static BigDecimal requirePositive(BigDecimal value, String message) {
        if (value == null || value.signum() <= 0) throw new IllegalArgumentException(message);
        return value;
    }
}
