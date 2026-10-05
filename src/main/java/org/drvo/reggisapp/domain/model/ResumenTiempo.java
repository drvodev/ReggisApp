package org.drvo.reggisapp.domain.model;

public class ResumenTiempo {
    private final long promedioDiasEntrePagos;
    private final long diasHastaPrimerPago;
    private final Long diasHastaCancelacion;

    public ResumenTiempo(long promedioDiasEntrePagos, long diasHastaPrimerPago, Long diasHastaCancelacion) {
        this.promedioDiasEntrePagos = promedioDiasEntrePagos;
        this.diasHastaPrimerPago = diasHastaPrimerPago;
        this.diasHastaCancelacion = diasHastaCancelacion;
    }

    public long getPromedioDiasEntrePagos() { return promedioDiasEntrePagos; }
    public long getDiasHastaPrimerPago() { return diasHastaPrimerPago; }
    public Long getDiasHastaCancelacion() { return diasHastaCancelacion; }
}
