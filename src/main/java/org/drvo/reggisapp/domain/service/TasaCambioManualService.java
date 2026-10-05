package org.drvo.reggisapp.domain.service;

import org.drvo.reggisapp.domain.exception.ReglaNegocioException;
import org.drvo.reggisapp.domain.model.ConversionMonetaria;
import org.drvo.reggisapp.domain.model.Moneda;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class TasaCambioManualService implements TasaCambioService {
    private static final int ESCALA_MONETARIA = 2;

    @Override
    public ConversionMonetaria convertir(BigDecimal monto, Moneda moneda, BigDecimal tasaCambio) {
        if (monto == null || monto.signum() <= 0) {
            throw new ReglaNegocioException("El monto debe ser mayor que cero.");
        }
        if (tasaCambio == null || tasaCambio.signum() <= 0) {
            throw new ReglaNegocioException("La tasa de cambio debe ser mayor que cero.");
        }
        if (moneda == null) throw new ReglaNegocioException("La moneda es obligatoria.");

        BigDecimal bolivares;
        BigDecimal dolares;
        if (moneda == Moneda.USD) {
            dolares = monto.setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
            bolivares = monto.multiply(tasaCambio).setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
        } else {
            bolivares = monto.setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
            dolares = bolivares.divide(tasaCambio, ESCALA_MONETARIA, RoundingMode.HALF_UP);
        }
        return new ConversionMonetaria(bolivares, dolares, monto.setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP),
                moneda, tasaCambio);
    }
}
