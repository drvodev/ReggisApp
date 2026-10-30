package org.drvo.reggisapp.domain.service;

import org.drvo.reggisapp.domain.model.EstadoPedido;
import org.drvo.reggisapp.domain.model.Moneda;
import org.drvo.reggisapp.domain.model.Pago;
import org.drvo.reggisapp.domain.model.Pedido;
import org.drvo.reggisapp.domain.model.ResolucionAnulacion;

import java.math.BigDecimal;
import java.util.List;

public interface CobranzaService {
    Pedido crearPedido(Long clienteId, BigDecimal monto, Moneda moneda, BigDecimal tasaCambio);
    Pedido crearPedido(Long clienteId, String descripcion, BigDecimal monto, Moneda moneda, BigDecimal tasaCambio);
    Pedido crearPedidoConMarcas(Long clienteId, String descripcion, BigDecimal monto, Moneda moneda, BigDecimal tasaCambio, java.util.Set<Long> marcas);
    List<org.drvo.reggisapp.domain.model.MarcaPedido> listarMarcas();
    List<org.drvo.reggisapp.domain.model.MarcaPedido> listarMarcasPedido(Long pedidoId);
    void crearMarca(String nombre);
    void retirarMarca(long id);
    Pago registrarPago(Long pedidoId, BigDecimal monto, Moneda moneda, BigDecimal tasaCambio);
    Pedido anularPedido(Long pedidoId, String motivo, ResolucionAnulacion resolucion, Long pedidoDestinoId);
    BigDecimal obtenerSaldoPendiente(Long pedidoId);
    List<Pedido> listarPedidos(Long clienteId, EstadoPedido estado);
    List<Pago> listarPagos(Long pedidoId);
}
