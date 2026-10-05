package org.drvo.reggisapp.domain.service;

import org.drvo.reggisapp.domain.exception.ReglaNegocioException;
import org.drvo.reggisapp.domain.model.Cliente;
import org.drvo.reggisapp.domain.model.ConversionMonetaria;
import org.drvo.reggisapp.domain.model.EstadoCliente;
import org.drvo.reggisapp.domain.model.EstadoPedido;
import org.drvo.reggisapp.domain.model.Moneda;
import org.drvo.reggisapp.domain.model.MovimientoDeuda;
import org.drvo.reggisapp.domain.model.Pago;
import org.drvo.reggisapp.domain.model.Pedido;
import org.drvo.reggisapp.domain.model.RegistroHistorial;
import org.drvo.reggisapp.domain.model.ResolucionAnulacion;
import org.drvo.reggisapp.repository.ClienteRepository;
import org.drvo.reggisapp.repository.HistorialRepository;
import org.drvo.reggisapp.repository.MovimientoDeudaRepository;
import org.drvo.reggisapp.repository.PagoRepository;
import org.drvo.reggisapp.repository.PedidoRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class CobranzaServiceImpl implements CobranzaService {
    private final ClienteRepository clienteRepository;
    private final PedidoRepository pedidoRepository;
    private final PagoRepository pagoRepository;
    private final HistorialRepository historialRepository;
    private final MovimientoDeudaRepository movimientoDeudaRepository;
    private final TasaCambioService tasaCambioService;
    private final Clock clock;
    private final TransactionBoundary transactionBoundary;

    public CobranzaServiceImpl(ClienteRepository clienteRepository,
                               PedidoRepository pedidoRepository,
                               PagoRepository pagoRepository,
                               HistorialRepository historialRepository,
                               MovimientoDeudaRepository movimientoDeudaRepository,
                               TasaCambioService tasaCambioService,
                               Clock clock) {
        this(clienteRepository, pedidoRepository, pagoRepository, historialRepository,
                movimientoDeudaRepository, tasaCambioService, clock, TransactionBoundary.directa());
    }

    public CobranzaServiceImpl(ClienteRepository clienteRepository,
                               PedidoRepository pedidoRepository,
                               PagoRepository pagoRepository,
                               HistorialRepository historialRepository,
                               MovimientoDeudaRepository movimientoDeudaRepository,
                               TasaCambioService tasaCambioService,
                               Clock clock,
                               TransactionBoundary transactionBoundary) {
        this.clienteRepository = Objects.requireNonNull(clienteRepository);
        this.pedidoRepository = Objects.requireNonNull(pedidoRepository);
        this.pagoRepository = Objects.requireNonNull(pagoRepository);
        this.historialRepository = Objects.requireNonNull(historialRepository);
        this.movimientoDeudaRepository = Objects.requireNonNull(movimientoDeudaRepository);
        this.tasaCambioService = Objects.requireNonNull(tasaCambioService);
        this.clock = Objects.requireNonNull(clock);
        this.transactionBoundary = Objects.requireNonNull(transactionBoundary);
    }

    @Override
    public Pedido crearPedido(Long clienteId, BigDecimal monto, Moneda moneda, BigDecimal tasaCambio) {
        return crearPedido(clienteId, "Pedido", monto, moneda, tasaCambio);
    }

    @Override
    public Pedido crearPedido(Long clienteId, String descripcion, BigDecimal monto, Moneda moneda, BigDecimal tasaCambio) {
        return transactionBoundary.inTransaction(() -> crearPedidoEnTransaccion(
                clienteId, descripcion, monto, moneda, tasaCambio));
    }

    private Pedido crearPedidoEnTransaccion(Long clienteId, String descripcion, BigDecimal monto,
                                              Moneda moneda, BigDecimal tasaCambio) {
        if (clienteId == null) throw new ReglaNegocioException("Debe seleccionar un cliente.");
        if (descripcion == null || descripcion.isBlank()) throw new ReglaNegocioException("La descripción del pedido es obligatoria.");
        validarMonto(monto);
        Cliente cliente = clienteRepository.buscarPorId(clienteId)
                .orElseThrow(() -> new ReglaNegocioException("No se encontró el cliente."));
        if (cliente.getEstado() != EstadoCliente.ACTIVO) {
            throw new ReglaNegocioException("No se puede crear un pedido para un cliente inactivo.");
        }

        ConversionMonetaria conversion = tasaCambioService.convertir(monto, moneda, tasaCambio);
        Pedido nuevo = Pedido.nuevo(clienteId, descripcion.trim(), conversion.getMontoBs(), conversion.getMontoOriginal(),
                conversion.getMonedaOriginal(), conversion.getTasaCambio(), LocalDate.now(clock));
        Pedido guardado = pedidoRepository.guardar(nuevo);
        if (guardado == null || guardado.getId() == null) {
            throw new ReglaNegocioException("No se pudo guardar el pedido.");
        }
        guardarHistorial("PEDIDO", guardado.getId(), "PEDIDO_CREADO", null,
                guardado.getMontoPedidoBs(), guardado.getMonedaOriginal().name());
        return guardado;
    }

    @Override
    public Pago registrarPago(Long pedidoId, BigDecimal monto, Moneda moneda, BigDecimal tasaCambio) {
        return transactionBoundary.inTransaction(() -> registrarPagoEnTransaccion(pedidoId, monto, moneda, tasaCambio));
    }

    private Pago registrarPagoEnTransaccion(Long pedidoId, BigDecimal monto, Moneda moneda, BigDecimal tasaCambio) {
        validarId(pedidoId, "pedido");
        validarMonto(monto);
        Pedido pedido = buscarPedido(pedidoId);
        if (pedido.getEstado() != EstadoPedido.ACTIVO) {
            throw new ReglaNegocioException("Solo se pueden registrar pagos en pedidos activos.");
        }

        ConversionMonetaria conversion = tasaCambioService.convertir(monto, moneda, tasaCambio);
        BigDecimal saldo = obtenerSaldoPendiente(pedidoId);
        if (conversion.getMontoBs().compareTo(saldo) > 0) {
            throw new ReglaNegocioException("El pago no puede superar el saldo pendiente.");
        }

        Pago pago = new Pago(null, pedidoId, LocalDate.now(clock), conversion.getMontoBs(),
                conversion.getMontoOriginal(), conversion.getMonedaOriginal(), conversion.getTasaCambio());
        Pago guardado = pagoRepository.guardar(pago);
        if (guardado == null || guardado.getId() == null) {
            throw new ReglaNegocioException("No se pudo guardar el pago con un identificador válido.");
        }
        guardarHistorial("PAGO", guardado.getId(), "PAGO_REGISTRADO", null,
                guardado.getMontoBs(), guardado.getMonedaOriginal().name());

        BigDecimal saldoRestante = saldo.subtract(guardado.getMontoBs());
        if (saldoRestante.signum() == 0) {
            Pedido completado = pedido.marcarCompletado();
            pedidoRepository.actualizar(completado);
            guardarHistorial("PEDIDO", pedidoId, "PEDIDO_COMPLETADO", null,
                    pedido.getMontoPedidoBs(), null);
        }
        return guardado;
    }

    @Override
    public Pedido anularPedido(Long pedidoId, String motivo, ResolucionAnulacion resolucion,
                               Long pedidoDestinoId) {
        return transactionBoundary.inTransaction(() -> anularPedidoEnTransaccion(pedidoId, motivo, resolucion, pedidoDestinoId));
    }

    private Pedido anularPedidoEnTransaccion(Long pedidoId, String motivo, ResolucionAnulacion resolucion,
                                              Long pedidoDestinoId) {
        validarId(pedidoId, "pedido");
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaNegocioException("Debe indicar el motivo de la anulación.");
        }
        if (resolucion == null) throw new ReglaNegocioException("Debe seleccionar qué hacer con el saldo.");

        Pedido origen = buscarPedido(pedidoId);
        if (origen.getEstado() != EstadoPedido.ACTIVO) {
            throw new ReglaNegocioException("Solo se pueden anular pedidos activos.");
        }
        BigDecimal saldo = obtenerSaldoPendiente(pedidoId);
        if (saldo.signum() <= 0) throw new ReglaNegocioException("El pedido no tiene deuda activa para anular.");

        if (resolucion == ResolucionAnulacion.TRASLADAR_SALDO) {
            if (pedidoDestinoId == null) {
                throw new ReglaNegocioException("Debe seleccionar el pedido de reemplazo.");
            }
            Pedido destino = buscarPedido(pedidoDestinoId);
            if (Objects.equals(origen.getId(), destino.getId())) {
                throw new ReglaNegocioException("El pedido de reemplazo debe ser distinto al pedido anulado.");
            }
            if (destino.getEstado() != EstadoPedido.ACTIVO || !origen.getClienteId().equals(destino.getClienteId())) {
                throw new ReglaNegocioException("El reemplazo debe ser un pedido activo del mismo cliente.");
            }
            movimientoDeudaRepository.guardar(new MovimientoDeuda(null, origen.getId(), destino.getId(),
                    saldo, LocalDate.now(clock), motivo.trim()));
        } else if (pedidoDestinoId != null) {
            throw new ReglaNegocioException("No se debe seleccionar un pedido destino al condonar el saldo.");
        }

        Pedido anulado = origen.anular();
        pedidoRepository.actualizar(anulado);
        guardarHistorial("PEDIDO", origen.getId(), "PEDIDO_ANULADO", motivo.trim(), saldo,
                resolucion.name() + (pedidoDestinoId == null ? "" : ":" + pedidoDestinoId));
        return anulado;
    }

    @Override
    public BigDecimal obtenerSaldoPendiente(Long pedidoId) {
        Pedido pedido = buscarPedido(pedidoId);
        if (pedido.getEstado() != EstadoPedido.ACTIVO) return BigDecimal.ZERO.setScale(2);
        BigDecimal pagos = ceroSiNulo(pagoRepository.sumarPagosBs(pedidoId));
        BigDecimal entradas = ceroSiNulo(movimientoDeudaRepository.sumarEntradasBs(pedidoId));
        BigDecimal salidas = ceroSiNulo(movimientoDeudaRepository.sumarSalidasBs(pedidoId));
        BigDecimal saldo = pedido.getMontoPedidoBs().add(entradas).subtract(pagos).subtract(salidas);
        return saldo.signum() < 0 ? BigDecimal.ZERO.setScale(2) : saldo;
    }

    @Override
    public List<Pedido> listarPedidos(Long clienteId, EstadoPedido estado) {
        validarId(clienteId, "cliente");
        List<Pedido> pedidos = pedidoRepository.listarPorCliente(clienteId);
        return estado == null ? pedidos : pedidos.stream().filter(p -> p.getEstado() == estado).toList();
    }

    @Override
    public List<Pago> listarPagos(Long pedidoId) {
        validarId(pedidoId, "pedido");
        buscarPedido(pedidoId);
        return pagoRepository.listarPorPedido(pedidoId);
    }

    private Pedido buscarPedido(Long id) {
        return pedidoRepository.buscarPorId(id)
                .orElseThrow(() -> new ReglaNegocioException("No se encontró el pedido."));
    }

    private void guardarHistorial(String tipo, Long id, String evento, String motivo,
                                  BigDecimal monto, String resolucion) {
        historialRepository.guardar(new RegistroHistorial(null, tipo, id, evento,
                LocalDateTime.now(clock), motivo, monto, resolucion));
    }

    private void validarId(Long id, String nombre) {
        if (id == null || id <= 0) throw new ReglaNegocioException("El identificador de " + nombre + " no es válido.");
    }

    private void validarMonto(BigDecimal monto) {
        if (monto == null || monto.signum() <= 0) {
            throw new ReglaNegocioException("El monto debe ser mayor que cero.");
        }
    }

    private BigDecimal ceroSiNulo(BigDecimal monto) {
        return monto == null ? BigDecimal.ZERO : monto;
    }
}
