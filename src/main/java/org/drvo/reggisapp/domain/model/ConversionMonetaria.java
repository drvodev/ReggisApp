package org.drvo.reggisapp.domain.model;

import java.math.BigDecimal;

public class ConversionMonetaria {
    private final BigDecimal montoBs;
    private final BigDecimal montoUsd;
    private final BigDecimal montoOriginal;
    private final Moneda monedaOriginal;
    private final BigDecimal tasaCambio;

    public ConversionMonetaria(BigDecimal montoBs, BigDecimal montoUsd, BigDecimal montoOriginal,
                               Moneda monedaOriginal, BigDecimal tasaCambio) {
        this.montoBs = montoBs;
        this.montoUsd = montoUsd;
        this.montoOriginal = montoOriginal;
        this.monedaOriginal = monedaOriginal;
        this.tasaCambio = tasaCambio;
    }

    public BigDecimal getMontoBs() { return montoBs; }
    public BigDecimal getMontoUsd() { return montoUsd; }
    public BigDecimal getMontoOriginal() { return montoOriginal; }
    public Moneda getMonedaOriginal() { return monedaOriginal; }
    public BigDecimal getTasaCambio() { return tasaCambio; }
}
