package org.drvo.reggisapp.android.data;

import android.content.Context;
import android.database.sqlite.*;
import androidx.test.core.app.ApplicationProvider;
import org.drvo.reggisapp.domain.model.*;
import org.drvo.reggisapp.domain.service.*;
import org.drvo.reggisapp.domain.exception.ReglaNegocioException;
import org.drvo.reggisapp.report.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellType;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={26,35})
@SQLiteMode(SQLiteMode.Mode.NATIVE)
public class LedgerStoreTest {
    private Context context;
    private ClientStore store;
    private CobranzaService billing;
    private long client;
    private static BigDecimal n(String s){return new BigDecimal(s);}
    @Before public void setup(){
        context=ApplicationProvider.getApplicationContext();context.deleteDatabase("ledger-test.db");
        store=new ClientStore(context,"ledger-test.db");billing=store.cobranza();
        client=store.service().crear("=Cliente",Map.of("rif","J-123","email","ana@example.com")).getId();
    }
    @After public void cleanup(){store.close();context.deleteDatabase("ledger-test.db");context.deleteDatabase("migration-test.db");}
    private Pedido order(String amount){return billing.crearPedidoConMarcas(client,"Pedido",n(amount),Moneda.BS,n("10"),Set.of());}
    private void balance(long id,String expected){assertEquals(0,n(expected).compareTo(billing.obtenerSaldoPendiente(id)));}
    @Test public void partialAndFullPaymentsSurviveReopenWithExactDecimals(){
        Pedido p=order("0.30");
        billing.registrarPago(p.getId(),n("0.10"),Moneda.BS,n("10"));balance(p.getId(),"0.20");
        assertThrows(ReglaNegocioException.class,()->billing.registrarPago(p.getId(),n("0.21"),Moneda.BS,n("10")));
        billing.registrarPago(p.getId(),n("0.20"),Moneda.BS,n("10"));
        store.close();store=new ClientStore(context,"ledger-test.db");billing=store.cobranza();
        balance(p.getId(),"0");assertEquals(2,billing.listarPagos(p.getId()).size());
        assertEquals(EstadoPedido.COMPLETADO,billing.listarPedidos(client,null).get(0).getEstado());
        assertEquals(2,store.history().listarPorEntidad("PEDIDO",p.getId()).size());
    }
    @Test public void dollarConversionKeepsOriginalRateAndDates(){
        Clock day1=Clock.fixed(Instant.parse("2026-10-01T12:00:00Z"),ZoneOffset.UTC);
        Pedido p=store.cobranza(day1).crearPedido(client,"USD",n("10"),Moneda.USD,n("10"));
        var day5=store.cobranza(Clock.offset(day1,Duration.ofDays(4)));
        Pago payment=day5.registrarPago(p.getId(),n("5"),Moneda.USD,n("20"));
        assertEquals(0,n("100").compareTo(payment.getMontoBs()));
        assertEquals(0,n("20").compareTo(payment.getTasaCambio()));
        assertEquals(LocalDate.of(2026,10,5),payment.getFechaPago());
        assertEquals(Long.valueOf(4),new TiempoServiceImpl().calcularTiempo(p,day5.listarPagos(p.getId())).getDiasHastaCancelacion());
    }
    @Test public void concurrentPaymentsCannotOverpay() throws Exception {
        Pedido p=order("100");var pool=Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> pay=()->{try{store.cobranza().registrarPago(p.getId(),n("100"),Moneda.BS,n("1"));return true;}catch(ReglaNegocioException e){return false;}};
            var results=pool.invokeAll(List.of(pay,pay));
            assertNotEquals(results.get(0).get(),results.get(1).get());
            assertEquals(1,billing.listarPagos(p.getId()).size());balance(p.getId(),"0");
        } finally {pool.shutdownNow();}
    }
    @Test public void failedAuditRollsBackPaymentAndCompletion(){
        Pedido p=order("100");
        store.getWritableDatabase().execSQL("CREATE TRIGGER fail_close BEFORE INSERT ON historial WHEN NEW.tipo_evento='PEDIDO_COMPLETADO' BEGIN SELECT RAISE(ABORT,'forced'); END");
        assertThrows(RuntimeException.class,()->billing.registrarPago(p.getId(),n("100"),Moneda.BS,n("1")));
        assertTrue(billing.listarPagos(p.getId()).isEmpty());balance(p.getId(),"100");
        assertEquals(EstadoPedido.ACTIVO,billing.listarPedidos(client,null).get(0).getEstado());
        assertEquals(1,store.history().listarPorEntidad("PEDIDO",p.getId()).size());
    }
    @Test public void transfersAndForgivenessKeepAuditAndRejectOtherClients(){
        Pedido source=order("100"), target=order("50");
        billing.registrarPago(source.getId(),n("20"),Moneda.BS,n("1"));
        long other=store.service().crear("Otro",Map.of("rif","J2")).getId();
        Pedido wrong=billing.crearPedido(other,n("10"),Moneda.BS,n("1"));
        assertThrows(ReglaNegocioException.class,()->billing.anularPedido(source.getId(),"Cambio",ResolucionAnulacion.TRASLADAR_SALDO,wrong.getId()));
        balance(source.getId(),"80");
        billing.anularPedido(source.getId(),"Cambio",ResolucionAnulacion.TRASLADAR_SALDO,target.getId());
        balance(source.getId(),"0");balance(target.getId(),"130");
        assertTrue(store.history().listarPorEntidad("PEDIDO",source.getId()).get(0).getResolucion().contains(target.getId().toString()));
        billing.anularPedido(target.getId(),"Condonado",ResolucionAnulacion.CONDONAR_SALDO,null);balance(target.getId(),"0");
        assertThrows(ReglaNegocioException.class,()->billing.registrarPago(target.getId(),n("1"),Moneda.BS,n("1")));
    }
    @Test public void failedTransferRollsBackEveryTable(){
        Pedido source=order("100"), target=order("50");
        store.getWritableDatabase().execSQL("CREATE TRIGGER fail_cancel BEFORE INSERT ON historial WHEN NEW.tipo_evento='PEDIDO_ANULADO' BEGIN SELECT RAISE(ABORT,'forced'); END");
        assertThrows(RuntimeException.class,()->billing.anularPedido(source.getId(),"Cambio",ResolucionAnulacion.TRASLADAR_SALDO,target.getId()));
        balance(source.getId(),"100");balance(target.getId(),"50");
        try(var c=store.getReadableDatabase().rawQuery("SELECT count(*) FROM movimientos_deuda",null)){assertTrue(c.moveToFirst());assertEquals(0,c.getInt(0));}
    }
    @Test public void brandSnapshotKeepsSelectedAndUnselectedAfterRemoval(){
        billing.crearMarca("Marca A");billing.crearMarca("Marca B");
        long a=billing.listarMarcas().get(0).id();
        Pedido p=billing.crearPedidoConMarcas(client,"Marcas",n("10"),Moneda.BS,n("1"),Set.of(a));
        billing.retirarMarca(a);billing.crearMarca("marca a");
        assertEquals(a,billing.listarMarcas().get(0).id());
        assertEquals(2,billing.listarMarcasPedido(p.getId()).size());
        assertTrue(billing.listarMarcasPedido(p.getId()).get(0).seleccionada());
        assertFalse(billing.listarMarcasPedido(p.getId()).get(1).seleccionada());
        assertThrows(ReglaNegocioException.class,()->billing.crearMarca("MARCA A"));
        assertThrows(ReglaNegocioException.class,()->billing.crearPedidoConMarcas(client,"bad",n("1"),Moneda.BS,n("1"),Set.of(999L)));
        assertEquals(1,billing.listarPedidos(client,null).size());
    }
    @Test public void maliciousInputIsTextAndInactiveClientsCannotOrder(){
        String text="'); DROP TABLE pagos; --";billing.crearMarca(text);
        Pedido p=billing.crearPedido(client,text,n("1"),Moneda.BS,n("1"));
        billing.anularPedido(p.getId(),text,ResolucionAnulacion.CONDONAR_SALDO,null);
        assertEquals(text,store.history().listarPorEntidad("PEDIDO",p.getId()).get(0).getMotivo());
        assertEquals(text,billing.listarPedidos(client,null).get(0).getDescripcion());
        store.service().inactivar(client);
        assertThrows(ReglaNegocioException.class,()->order("10"));
        assertEquals(1,billing.listarPedidos(client,null).size());
    }
    @Test public void excelUsesTextCellsNotFormulasAndContainsRealPayment() throws Exception {
        Pedido p=order("100");billing.registrarPago(p.getId(),n("40"),Moneda.BS,n("1"));
        var payments=billing.listarPagos(p.getId());var report=new ReportePedido(p,payments,billing.obtenerSaldoPendiente(p.getId()),new TiempoServiceImpl().calcularTiempo(p,payments));
        var file=new java.io.File(context.getCacheDir(),"test.xls");
        new XlsReportExporter().exportarPedido(store.service().listarTodos().get(0),report,file.toPath());
        try(var input=new java.io.FileInputStream(file);var book=new HSSFWorkbook(input)) {
            var sheet=book.getSheet("Cobranza");
            assertEquals(CellType.STRING,sheet.getRow(1).getCell(1).getCellType());
            assertEquals("=Cliente",sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals(40,sheet.getRow(8).getCell(12).getNumericCellValue(),0);
            assertEquals(60,sheet.getRow(8).getCell(13).getNumericCellValue(),0);
        } finally {file.delete();}
    }
    @Test public void migrationPreservesStepThreeData(){
        context.deleteDatabase("migration-test.db");
        var old=new SQLiteOpenHelper(context,"migration-test.db",null,1){
            public void onCreate(SQLiteDatabase db){
                db.execSQL("CREATE TABLE clientes (id INTEGER PRIMARY KEY AUTOINCREMENT,nombre TEXT NOT NULL,estado TEXT NOT NULL)");
                db.execSQL("CREATE TABLE campos_cliente (id INTEGER PRIMARY KEY AUTOINCREMENT,clave TEXT NOT NULL UNIQUE,etiqueta TEXT NOT NULL,activo INTEGER NOT NULL)");
                db.execSQL("CREATE TABLE cliente_datos_adicionales (cliente_id INTEGER NOT NULL REFERENCES clientes(id),clave TEXT NOT NULL,valor TEXT NOT NULL,PRIMARY KEY(cliente_id,clave))");
                db.execSQL("CREATE TABLE historial (id INTEGER PRIMARY KEY AUTOINCREMENT,tipo_entidad TEXT NOT NULL,entidad_id INTEGER NOT NULL,tipo_evento TEXT NOT NULL,fecha_registro TEXT NOT NULL,motivo TEXT,monto_bs TEXT,resolucion TEXT)");
                db.execSQL("INSERT INTO clientes VALUES(7,'Cliente previo','ACTIVO')");
                db.execSQL("INSERT INTO cliente_datos_adicionales VALUES(7,'rif','J7')");
                db.execSQL("INSERT INTO historial VALUES(1,'CLIENTE',7,'CLIENTE_CREADO','2026-10-01T12:00',NULL,NULL,NULL)");
            }
            public void onUpgrade(SQLiteDatabase d,int a,int b){throw new AssertionError();}
        };
        old.getWritableDatabase();old.close();
        try(var migrated=new ClientStore(context,"migration-test.db")){
            assertEquals(2,migrated.getReadableDatabase().getVersion());
            assertEquals("J7",migrated.service().listarTodos().get(0).getDatosAdicionales().get("rif"));
            assertEquals(1,migrated.history().listarPorEntidad("CLIENTE",7L).size());
            assertNotNull(migrated.cobranza().crearPedido(7L,n("10"),Moneda.BS,n("1")).getId());
        }
    }
}
