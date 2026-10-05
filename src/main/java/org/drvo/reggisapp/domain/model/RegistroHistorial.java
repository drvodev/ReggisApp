package org.drvo.reggisapp.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RegistroHistorial {
    private final Long id;
    private final String tipoEntidad;
    private final Long entidadId;
    private final String tipoEvento;
    private final LocalDateTime fechaRegistro;
    private final String motivo;
    private final BigDecimal montoBs;
    private final String resolucion;

    public RegistroHistorial(Long id, String tipoEntidad, Long entidadId, String tipoEvento,
                             LocalDateTime fechaRegistro, String motivo, BigDecimal montoBs, String resolucion) {
        this.id = id;
        this.tipoEntidad = tipoEntidad;
        this.entidadId = entidadId;
        this.tipoEvento = tipoEvento;
        this.fechaRegistro = fechaRegistro;
        this.motivo = motivo;
        this.montoBs = montoBs;
        this.resolucion = resolucion;
    }

    public Long getId() { return id; }
    public String getTipoEntidad() { return tipoEntidad; }
    public Long getEntidadId() { return entidadId; }
    public String getTipoEvento() { return tipoEvento; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public String getMotivo() { return motivo; }
    public BigDecimal getMontoBs() { return montoBs; }
    public String getResolucion() { return resolucion; }
}
