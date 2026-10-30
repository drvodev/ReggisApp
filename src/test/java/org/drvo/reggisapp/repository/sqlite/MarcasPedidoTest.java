package org.drvo.reggisapp.repository.sqlite;

import org.drvo.reggisapp.config.AppServices;
import org.drvo.reggisapp.config.DatabaseConfig;
import org.drvo.reggisapp.domain.model.Moneda;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class MarcasPedidoTest {
    @TempDir Path temporal;
    @Test void conservaSeleccionYReviertePedidoSiLaMarcaNoExiste() {
        var config = new DatabaseConfig(temporal.resolve("datos.db"));
        var servicios = new AppServices(config);
        var cliente = servicios.clienteService().crear("Cliente", Map.of("rif", "J-123"));
        var cobranza = servicios.cobranzaService();
        cobranza.crearMarca("Once-once");
        cobranza.crearMarca("Kronk");
        assertThrows(RuntimeException.class, () -> cobranza.crearMarca(" kronk "));
        var marcas = cobranza.listarMarcas();
        long id = marcas.stream().filter(m -> m.nombre().equals("Once-once")).findFirst().orElseThrow().id();
        var pedido = cobranza.crearPedidoConMarcas(cliente.getId(), "Compra", BigDecimal.TEN, Moneda.BS, BigDecimal.TEN, Set.of(id));
        cobranza.crearMarca("Gomitas");
        var recargado = new AppServices(config).cobranzaService().listarMarcasPedido(pedido.getId());
        assertEquals(2, recargado.size());
        assertTrue(recargado.stream().anyMatch(m -> m.nombre().equals("Once-once") && m.seleccionada()));
        assertTrue(recargado.stream().anyMatch(m -> m.nombre().equals("Kronk") && !m.seleccionada()));
        assertThrows(RuntimeException.class, () -> cobranza.crearPedidoConMarcas(cliente.getId(), "Inválido", BigDecimal.TEN, Moneda.BS, BigDecimal.TEN, Set.of(99999L)));
        assertEquals(1, cobranza.listarPedidos(cliente.getId(), null).size());
    }

    @Test void validaEntradaYGuardaTextoSinEjecutarSql() {
        var config = new DatabaseConfig(temporal.resolve("seguridad.db"));
        config.inicializar(config.crearConnectionFactory());
        var repo = new SqliteMarcaRepository(config.crearConnectionFactory());
        assertThrows(RuntimeException.class, () -> repo.crear("  "));
        assertThrows(RuntimeException.class, () -> repo.crear("x".repeat(101)));
        assertThrows(RuntimeException.class, () -> repo.crear("marca\nmaliciosa"));
        String texto = "Marca'); DROP TABLE clientes; --";
        repo.crear(texto);
        assertEquals(texto, repo.listar().getFirst().nombre());
        assertEquals(0, new SqliteClienteRepository(config.crearConnectionFactory()).listarTodos().size());
    }

    @Test void migraBaseVersionDosSinAlterarPedidosExistentes() {
        var config = new DatabaseConfig(temporal.resolve("anterior.db"));
        var conexiones = config.crearConnectionFactory();
        config.inicializar(conexiones);
        var clientes = new SqliteClienteRepository(conexiones);
        var cliente = clientes.guardar(org.drvo.reggisapp.domain.model.Cliente.nuevo("Existente"));
        var pedidos = new SqlitePedidoRepository(conexiones);
        var pedido = pedidos.guardar(org.drvo.reggisapp.domain.model.Pedido.nuevo(cliente.getId(), "Anterior", BigDecimal.TEN, BigDecimal.TEN, Moneda.BS, BigDecimal.TEN, java.time.LocalDate.now()));
        conexiones.withConnection(c -> { try (var s = c.createStatement()) {
            s.execute("DROP TABLE pedido_marcas"); s.execute("DROP TABLE marcas"); s.execute("PRAGMA user_version = 2");
        } return null; });
        config.inicializar(conexiones);
        config.inicializar(conexiones);
        assertEquals("Anterior", pedidos.buscarPorId(pedido.getId()).orElseThrow().getDescripcion());
        assertTrue(new SqliteMarcaRepository(conexiones).listarPorPedido(pedido.getId()).isEmpty());
        conexiones.withConnection(c -> { try (var s = c.createStatement(); var r = s.executeQuery("PRAGMA user_version")) { assertEquals(4, r.getInt(1)); } return null; });
    }

    @Test void retirarYReactivarConservaLosPedidosYNoApareceEnNuevos() {
        var config = new DatabaseConfig(temporal.resolve("retiro.db"));
        var servicios = new AppServices(config);
        var cliente = servicios.clienteService().crear("Cliente", Map.of("rif", "J-123"));
        var cobranza = servicios.cobranzaService();
        cobranza.crearMarca("Kronk");
        long id = cobranza.listarMarcas().getFirst().id();
        var anterior = cobranza.crearPedidoConMarcas(cliente.getId(), "Antes", BigDecimal.TEN, Moneda.BS, BigDecimal.TEN, Set.of(id));
        cobranza.retirarMarca(id);
        assertTrue(cobranza.listarMarcas().isEmpty());
        assertTrue(new AppServices(config).cobranzaService().listarMarcasPedido(anterior.getId()).getFirst().seleccionada());
        assertThrows(RuntimeException.class, () -> cobranza.crearPedidoConMarcas(cliente.getId(), "Inválido", BigDecimal.TEN, Moneda.BS, BigDecimal.TEN, Set.of(id)));
        var posterior = cobranza.crearPedidoConMarcas(cliente.getId(), "Después", BigDecimal.TEN, Moneda.BS, BigDecimal.TEN, Set.of());
        assertTrue(cobranza.listarMarcasPedido(posterior.getId()).isEmpty());
        cobranza.crearMarca("kronk");
        assertEquals(id, cobranza.listarMarcas().getFirst().id());
        assertTrue(cobranza.listarMarcasPedido(anterior.getId()).getFirst().seleccionada());
        assertTrue(cobranza.listarMarcasPedido(posterior.getId()).isEmpty());
    }

    @Test void migraMarcasExistentesDeVersionTresComoActivas() {
        var config = new DatabaseConfig(temporal.resolve("v3.db"));
        var conexiones = config.crearConnectionFactory();
        config.inicializar(conexiones);
        var repo = new SqliteMarcaRepository(conexiones);
        repo.crear("Once-once");
        conexiones.withConnection(c -> { try (var s = c.createStatement()) {
            s.execute("ALTER TABLE marcas DROP COLUMN activa");
            s.execute("PRAGMA user_version = 3");
        } return null; });
        config.inicializar(conexiones);
        assertEquals("Once-once", repo.listar().getFirst().nombre());
    }
}
