package org.drvo.reggisapp.config;

import org.drvo.reggisapp.domain.service.CobranzaService;
import org.drvo.reggisapp.domain.service.CobranzaServiceImpl;
import org.drvo.reggisapp.domain.service.ClienteService;
import org.drvo.reggisapp.domain.service.ClienteServiceImpl;
import org.drvo.reggisapp.domain.service.ReporteService;
import org.drvo.reggisapp.domain.service.ReporteServiceImpl;
import org.drvo.reggisapp.domain.service.TasaCambioManualService;
import org.drvo.reggisapp.domain.service.TiempoService;
import org.drvo.reggisapp.domain.service.TiempoServiceImpl;
import org.drvo.reggisapp.repository.CampoClienteRepository;
import org.drvo.reggisapp.repository.ClienteRepository;
import org.drvo.reggisapp.repository.HistorialRepository;
import org.drvo.reggisapp.repository.MovimientoDeudaRepository;
import org.drvo.reggisapp.repository.PagoRepository;
import org.drvo.reggisapp.repository.PedidoRepository;
import org.drvo.reggisapp.repository.sqlite.SqliteCampoClienteRepository;
import org.drvo.reggisapp.repository.sqlite.SqliteClienteRepository;
import org.drvo.reggisapp.repository.sqlite.SqliteHistorialRepository;
import org.drvo.reggisapp.repository.sqlite.SqliteMovimientoDeudaRepository;
import org.drvo.reggisapp.repository.sqlite.SqlitePagoRepository;
import org.drvo.reggisapp.repository.sqlite.SqlitePedidoRepository;

import java.time.Clock;

/** Composition root: configura SQLite, repositorios y servicios para la aplicación. */
public final class AppServices {
    private final ClienteService clienteService;
    private final CobranzaService cobranzaService;
    private final TiempoService tiempoService;
    private final ReporteService reporteService;
    private final HistorialRepository historialRepository;

    public AppServices() {
        this(new DatabaseConfig());
    }

    public AppServices(DatabaseConfig databaseConfig) {
        SqliteConnectionFactory conexiones = databaseConfig.crearConnectionFactory();
        databaseConfig.inicializar(conexiones);

        ClienteRepository clientes = new SqliteClienteRepository(conexiones);
        PedidoRepository pedidos = new SqlitePedidoRepository(conexiones);
        PagoRepository pagos = new SqlitePagoRepository(conexiones);
        historialRepository = new SqliteHistorialRepository(conexiones);
        MovimientoDeudaRepository movimientos = new SqliteMovimientoDeudaRepository(conexiones);
        CampoClienteRepository campos = new SqliteCampoClienteRepository(conexiones);
        Clock clock = Clock.systemDefaultZone();

        clienteService = new ClienteServiceImpl(clientes, campos, historialRepository, clock, conexiones);
        cobranzaService = new CobranzaServiceImpl(clientes, pedidos, pagos, historialRepository, movimientos,
                new TasaCambioManualService(), clock, conexiones);
        tiempoService = new TiempoServiceImpl();
        reporteService = new ReporteServiceImpl(clientes);
    }

    public ClienteService clienteService() { return clienteService; }
    public CobranzaService cobranzaService() { return cobranzaService; }
    public TiempoService tiempoService() { return tiempoService; }
    public ReporteService reporteService() { return reporteService; }
    public HistorialRepository historialRepository() { return historialRepository; }
}
