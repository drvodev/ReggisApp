package org.drvo.reggisapp.domain.service;

import org.drvo.reggisapp.domain.exception.ReglaNegocioException;
import org.drvo.reggisapp.domain.model.Cliente;
import org.drvo.reggisapp.domain.model.EstadoCliente;
import org.drvo.reggisapp.domain.model.EstadoPedido;
import org.drvo.reggisapp.domain.model.Moneda;
import org.drvo.reggisapp.domain.model.Pago;
import org.drvo.reggisapp.domain.model.Pedido;
import org.drvo.reggisapp.domain.model.ResolucionAnulacion;
import org.drvo.reggisapp.repository.ClienteRepository;
import org.drvo.reggisapp.repository.HistorialRepository;
import org.drvo.reggisapp.repository.MovimientoDeudaRepository;
import org.drvo.reggisapp.repository.PagoRepository;
import org.drvo.reggisapp.repository.PedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CobranzaServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-04T12:00:00Z"), ZoneOffset.UTC);
    private static final BigDecimal RATE = new BigDecimal("840.00");

    @Mock private ClienteRepository clienteRepository;
    @Mock private PedidoRepository pedidoRepository;
    @Mock private PagoRepository pagoRepository;
    @Mock private HistorialRepository historialRepository;
    @Mock private MovimientoDeudaRepository movimientoDeudaRepository;

    private CobranzaService service;

    @BeforeEach
    void setUp() {
        service = new CobranzaServiceImpl(
                clienteRepository,
                pedidoRepository,
                pagoRepository,
                historialRepository,
                movimientoDeudaRepository,
                new TasaCambioManualService(),
                CLOCK);
    }

    @Test
    void crearPedidoConvierteDolaresABolivaresYRegistraHistorial() {
        when(clienteRepository.buscarPorId(7L)).thenReturn(Optional.of(new Cliente(7L, "Ana")));
        when(pedidoRepository.guardar(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido pedido = invocation.getArgument(0);
            return pedido.conId(11L);
        });

        Pedido pedido = service.crearPedido(7L, new BigDecimal("100.00"), Moneda.USD, RATE);

        assertEquals(new BigDecimal("84000.00"), pedido.getMontoPedidoBs());
        assertEquals(new BigDecimal("100.00"), pedido.getMontoOriginal());
        assertEquals(EstadoPedido.ACTIVO, pedido.getEstado());
        assertEquals(LocalDate.of(2026, 10, 4), pedido.getFechaCreacion());
        verify(historialRepository).guardar(any());
    }

    @Test
    void crearPedidoRechazaMontoNuloOCero() {
        assertThrows(ReglaNegocioException.class,
                () -> service.crearPedido(7L, BigDecimal.ZERO, Moneda.BS, RATE));
        assertThrows(ReglaNegocioException.class,
                () -> service.crearPedido(7L, null, Moneda.BS, RATE));
        verifyNoInteractions(pedidoRepository);
    }

    @Test
    void pagoParcialEnDolaresSeGuardaEnAmbasMonedasYMantienePedidoActivo() {
        Pedido pedido = pedidoActivo(11L, "1000000.00");
        when(pedidoRepository.buscarPorId(11L)).thenReturn(Optional.of(pedido));
        when(pagoRepository.sumarPagosBs(11L)).thenReturn(new BigDecimal("0.00"));
        when(movimientoDeudaRepository.sumarEntradasBs(11L)).thenReturn(BigDecimal.ZERO);
        when(movimientoDeudaRepository.sumarSalidasBs(11L)).thenReturn(BigDecimal.ZERO);
        when(pagoRepository.guardar(any(Pago.class))).thenAnswer(invocation ->
                ((Pago) invocation.getArgument(0)).conId(21L));

        Pago pago = service.registrarPago(11L, new BigDecimal("500.00"), Moneda.USD, RATE);

        assertEquals(new BigDecimal("420000.00"), pago.getMontoBs());
        assertEquals(new BigDecimal("500.00"), pago.getMontoOriginal());
        assertEquals(Moneda.USD, pago.getMonedaOriginal());
        assertEquals(new BigDecimal("840.00"), pago.getTasaCambio());
        verify(pedidoRepository, never()).actualizar(any());
        verify(historialRepository).guardar(any());
    }

    @Test
    void pagoIgualAlSaldoCompletaElPedido() {
        Pedido pedido = pedidoActivo(11L, "100.00");
        when(pedidoRepository.buscarPorId(11L)).thenReturn(Optional.of(pedido));
        when(pagoRepository.sumarPagosBs(11L)).thenReturn(new BigDecimal("60.00"));
        when(movimientoDeudaRepository.sumarEntradasBs(11L)).thenReturn(BigDecimal.ZERO);
        when(movimientoDeudaRepository.sumarSalidasBs(11L)).thenReturn(BigDecimal.ZERO);
        when(pagoRepository.guardar(any(Pago.class))).thenAnswer(invocation ->
                ((Pago) invocation.getArgument(0)).conId(22L));

        service.registrarPago(11L, new BigDecimal("40.00"), Moneda.BS, RATE);

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).actualizar(captor.capture());
        assertEquals(EstadoPedido.COMPLETADO, captor.getValue().getEstado());
        verify(historialRepository, atLeast(2)).guardar(any());
    }

    @Test
    void pagoMayorAlSaldoEsRechazadoSinGuardarMovimiento() {
        when(pedidoRepository.buscarPorId(11L)).thenReturn(Optional.of(pedidoActivo(11L, "100.00")));
        when(pagoRepository.sumarPagosBs(11L)).thenReturn(new BigDecimal("70.00"));
        when(movimientoDeudaRepository.sumarEntradasBs(11L)).thenReturn(BigDecimal.ZERO);
        when(movimientoDeudaRepository.sumarSalidasBs(11L)).thenReturn(BigDecimal.ZERO);

        assertThrows(ReglaNegocioException.class,
                () -> service.registrarPago(11L, new BigDecimal("31.00"), Moneda.BS, RATE));

        verify(pagoRepository, never()).guardar(any());
    }

    @Test
    void pagoNuloOCeroEsRechazado() {
        assertThrows(ReglaNegocioException.class,
                () -> service.registrarPago(11L, BigDecimal.ZERO, Moneda.BS, RATE));
        assertThrows(ReglaNegocioException.class,
                () -> service.registrarPago(11L, null, Moneda.BS, RATE));
        verifyNoInteractions(pagoRepository);
    }

    @Test
    void anularPedidoConSaldoRequiereMotivoYRegistraSaldoYResolucion() {
        when(pedidoRepository.buscarPorId(11L)).thenReturn(Optional.of(pedidoActivo(11L, "100.00")));
        when(pagoRepository.sumarPagosBs(11L)).thenReturn(new BigDecimal("40.00"));
        when(movimientoDeudaRepository.sumarEntradasBs(11L)).thenReturn(BigDecimal.ZERO);
        when(movimientoDeudaRepository.sumarSalidasBs(11L)).thenReturn(BigDecimal.ZERO);

        assertThrows(ReglaNegocioException.class,
                () -> service.anularPedido(11L, "  ", ResolucionAnulacion.CONDONAR_SALDO, null));

        service.anularPedido(11L, "Cliente canceló el pedido", ResolucionAnulacion.CONDONAR_SALDO, null);

        ArgumentCaptor<Pedido> pedidoCaptor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).actualizar(pedidoCaptor.capture());
        assertEquals(EstadoPedido.ANULADO, pedidoCaptor.getValue().getEstado());
        verify(historialRepository, atLeastOnce()).guardar(argThat(evento ->
                evento.getMotivo().equals("Cliente canceló el pedido")
                        && evento.getMontoBs().compareTo(new BigDecimal("60.00")) == 0));
    }

    @Test
    void anulacionPorTrasladoCreaRegistroDeTransferencia() {
        when(pedidoRepository.buscarPorId(11L)).thenReturn(Optional.of(pedidoActivo(11L, "100.00")));
        when(pedidoRepository.buscarPorId(12L)).thenReturn(Optional.of(pedidoActivo(12L, "200.00")));
        when(pagoRepository.sumarPagosBs(11L)).thenReturn(new BigDecimal("40.00"));
        when(movimientoDeudaRepository.sumarEntradasBs(anyLong())).thenReturn(BigDecimal.ZERO);
        when(movimientoDeudaRepository.sumarSalidasBs(anyLong())).thenReturn(BigDecimal.ZERO);

        service.anularPedido(11L, "Cambio de pedido", ResolucionAnulacion.TRASLADAR_SALDO, 12L);

        verify(movimientoDeudaRepository).guardar(argThat(movimiento ->
                movimiento.getPedidoOrigenId().equals(11L)
                        && movimiento.getPedidoDestinoId().equals(12L)
                        && movimiento.getMontoBs().compareTo(new BigDecimal("60.00")) == 0));
        verify(pedidoRepository).actualizar(argThat(actualizado ->
                actualizado.getId().equals(11L) && actualizado.getEstado() == EstadoPedido.ANULADO));
    }

    @Test
    void pedidoCompletadoNoSePuedeAnular() {
        Pedido pedido = pedidoActivo(11L, "100.00").marcarCompletado();
        when(pedidoRepository.buscarPorId(11L)).thenReturn(Optional.of(pedido));

        assertThrows(ReglaNegocioException.class,
                () -> service.anularPedido(11L, "Cambio", ResolucionAnulacion.CONDONAR_SALDO, null));
        verify(historialRepository, never()).guardar(any());
    }

    private Pedido pedidoActivo(long id, String montoBs) {
        return new Pedido(id, 7L, new BigDecimal(montoBs), new BigDecimal(montoBs), Moneda.BS,
                RATE, LocalDate.of(2026, 10, 1), EstadoPedido.ACTIVO);
    }
}
