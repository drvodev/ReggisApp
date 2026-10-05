package org.drvo.reggisapp.domain.service;

import org.drvo.reggisapp.domain.model.ConversionMonetaria;
import org.drvo.reggisapp.domain.model.Moneda;

import java.math.BigDecimal;

public interface TasaCambioService {
    ConversionMonetaria convertir(BigDecimal monto, Moneda moneda, BigDecimal tasaCambio);
}
