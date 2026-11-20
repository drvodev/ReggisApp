package org.drvo.reggisapp.android;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.drvo.reggisapp.android.data.ClientStore;
import org.drvo.reggisapp.domain.model.*;
import org.drvo.reggisapp.domain.service.TiempoServiceImpl;
import org.drvo.reggisapp.report.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import java.math.BigDecimal;
import java.io.*;
import java.util.*;
import static org.junit.Assert.*;

/** Exercises DEX/desugaring and POI on Android itself, not the host JVM. */
@RunWith(AndroidJUnit4.class)
public class DeviceSmokeTest {
    @Test public void paymentsAndRealXlsRunOnAndroid() throws Exception {
        var context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase("device-smoke.db");
        File report=new File(context.getCacheDir(),"device-smoke.xls");
        try(var db=new ClientStore(context,"device-smoke.db")) {
            var client=db.service().crear("=Prueba Android",Map.of("rif","J-1"));
            var billing=db.cobranza();billing.crearMarca("Marca Android");
            var order=billing.crearPedidoConMarcas(client.getId(),"Pedido Android",new BigDecimal("100"),Moneda.BS,BigDecimal.ONE,Set.of(billing.listarMarcas().get(0).id()));
            billing.registrarPago(order.getId(),new BigDecimal("40"),Moneda.BS,BigDecimal.ONE);
            var payments=billing.listarPagos(order.getId());
            var data=new ReportePedido(order,payments,billing.obtenerSaldoPendiente(order.getId()),new TiempoServiceImpl().calcularTiempo(order,payments));
            new XlsReportExporter().exportarPedido(client,data,report.toPath());
            try(var input=new FileInputStream(report);var book=new HSSFWorkbook(input)) {
                assertEquals("=Prueba Android",book.getSheetAt(0).getRow(1).getCell(1).getStringCellValue());
                assertEquals(60,book.getSheetAt(0).getRow(8).getCell(13).getNumericCellValue(),0);
            }
        } finally {context.deleteDatabase("device-smoke.db");report.delete();}
    }
}
