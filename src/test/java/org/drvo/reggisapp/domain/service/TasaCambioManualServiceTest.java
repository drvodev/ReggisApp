package org.drvo.reggisapp.domain.service;

import org.drvo.reggisapp.domain.exception.ReglaNegocioException;
import org.drvo.reggisapp.domain.model.ConversionMonetaria;
import org.drvo.reggisapp.domain.model.Moneda;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class TasaCambioManualServiceTest {
    private final TasaCambioManualService service = new TasaCambioManualService();

    @Test
    void convierteDolaresABolivaresConLaTasaIndicada() {
        ConversionMonetaria conversion = service.convertir(
                new BigDecimal("500.00"), Moneda.USD, new BigDecimal("840.00"));

        assertEquals(new BigDecimal("420000.00"), conversion.getMontoBs());
        assertEquals(new BigDecimal("500.00"), conversion.getMontoUsd());
    }

    @Test
    void convierteBolivaresADolaresRedondeandoAcentavos() {
        ConversionMonetaria conversion = service.convertir(
                new BigDecimal("1232000.00"), Moneda.BS, new BigDecimal("840.00"));

        assertEquals(new BigDecimal("1232000.00"), conversion.getMontoBs());
        assertEquals(new BigDecimal("1466.67"), conversion.getMontoUsd());
    }

    @Test
    void conservaLaTasaIngresadaParaElHistorial() {
        BigDecimal tasa = new BigDecimal("840.4056");

        ConversionMonetaria conversion = service.convertir(new BigDecimal("10.00"), Moneda.USD, tasa);

        assertEquals(new BigDecimal("8404.06"), conversion.getMontoBs());
        assertEquals(tasa, conversion.getTasaCambio());
    }

    @Test
    void rechazaMontoNuloCeroYTasaNoPositiva() {
        assertThrows(ReglaNegocioException.class, () -> service.convertir(null, Moneda.BS, BigDecimal.ONE));
        assertThrows(ReglaNegocioException.class, () -> service.convertir(BigDecimal.ZERO, Moneda.BS, BigDecimal.ONE));
        assertThrows(ReglaNegocioException.class, () -> service.convertir(BigDecimal.ONE, Moneda.USD, BigDecimal.ZERO));
        assertThrows(ReglaNegocioException.class, () -> service.convertir(BigDecimal.ONE, null, BigDecimal.ONE));
    }
}
