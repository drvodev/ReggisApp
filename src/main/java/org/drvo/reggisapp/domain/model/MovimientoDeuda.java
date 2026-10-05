package org.drvo.reggisapp.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class MovimientoDeuda {
    private final Long id;
    private final Long pedidoOrigenId;
    private final Long pedidoDestinoId;
    private final BigDecimal montoBs;
    private final LocalDate fecha;
    private final String motivo;

    public MovimientoDeuda(Long id, Long pedidoOrigenId, Long pedidoDestinoId,
                           BigDecimal montoBs, LocalDate fecha, String motivo) {
        this.id = id;
        this.pedidoOrigenId = pedidoOrigenId;
        this.pedidoDestinoId = pedidoDestinoId;
        this.montoBs = montoBs;
        this.fecha = fecha;
        this.motivo = motivo;
    }

    public Long getId() { return id; }
    public Long getPedidoOrigenId() { return pedidoOrigenId; }
    public Long getPedidoDestinoId() { return pedidoDestinoId; }
    public BigDecimal getMontoBs() { return montoBs; }
    public LocalDate getFecha() { return fecha; }
    public String getMotivo() { return motivo; }
}
