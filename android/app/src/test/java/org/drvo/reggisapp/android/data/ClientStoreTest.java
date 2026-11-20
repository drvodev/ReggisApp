package org.drvo.reggisapp.android.data;

import android.content.Context;
import android.database.sqlite.SQLiteException;
import androidx.test.core.app.ApplicationProvider;
import org.drvo.reggisapp.domain.exception.ReglaNegocioException;
import org.drvo.reggisapp.domain.model.Cliente;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.SQLiteMode;
import java.util.Map;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
public class ClientStoreTest {
    private Context context;
    private ClientStore store;
    @Before public void setup() {
        context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase("clients-test.db");
        store = new ClientStore(context, "clients-test.db");
    }
    @After public void cleanup() { store.close(); context.deleteDatabase("clients-test.db"); }

    @Test public void persistsContactsAndEditsAcrossReopen() {
        Cliente client = store.service().crear("  Ana  ", Map.of("rif"," J-123 ","email","ana@example.com","zona","Norte"));
        store.close(); store = new ClientStore(context,"clients-test.db");
        Cliente loaded = store.service().listarTodos().get(0);
        assertEquals(client.getId(),loaded.getId());
        assertEquals("Ana", loaded.getNombre());
        assertEquals("J-123", loaded.getDatosAdicionales().get("rif"));
        store.service().actualizar(client.getId(),"Ana María",loaded.getDatosAdicionales());
        store.close(); store = new ClientStore(context,"clients-test.db");
        assertEquals("Ana María", store.service().listarTodos().get(0).getNombre());
        assertEquals("Norte", store.service().listarTodos().get(0).getDatosAdicionales().get("zona"));
        assertEquals(2,store.history().listarPorEntidad("CLIENTE",client.getId()).size());
    }
    @Test public void requiredFieldsRejectWithoutSaving() {
        assertThrows(ReglaNegocioException.class, () -> store.service().crear(" ",Map.of("rif","J1")));
        assertThrows(ReglaNegocioException.class, () -> store.service().crear("Ana",Map.of()));
        assertTrue(store.service().listarTodos().isEmpty());
    }
    @Test public void sqlInjectionRemainsLiteralData() {
        String text = "'); DROP TABLE clientes; --";
        Cliente client = store.service().crear(text,Map.of("rif",text,"email",text));
        assertEquals(text,store.service().listarTodos().get(0).getNombre());
        assertEquals(text,store.service().listarTodos().get(0).getDatosAdicionales().get("email"));
        assertTrue(store.history().listarPorEntidad("CLIENTE' OR 1=1 --",client.getId()).isEmpty());
        assertEquals(1,store.history().listarPorEntidad("CLIENTE",client.getId()).size());
    }
    @Test public void failedHistoryRollsBackClientAndContact() {
        store.getWritableDatabase().execSQL("CREATE TRIGGER fail_history BEFORE INSERT ON historial BEGIN SELECT RAISE(ABORT,'test failure'); END");
        assertThrows(RuntimeException.class, () -> store.service().crear("Ana",Map.of("rif","J1")));
        assertTrue(store.service().listarTodos().isEmpty());
        try(var c = store.getReadableDatabase().rawQuery("SELECT count(*) FROM cliente_datos_adicionales",null)) {
            assertTrue(c.moveToFirst()); assertEquals(0,c.getInt(0));
        }
    }
    @Test public void auditCannotBeChangedOrDeletedAndForeignKeysAreEnforced() {
        store.service().crear("Ana",Map.of("rif","J1"));
        assertThrows(SQLiteException.class, () -> store.getWritableDatabase().execSQL("DELETE FROM historial"));
        assertThrows(SQLiteException.class, () -> store.getWritableDatabase().execSQL("UPDATE historial SET motivo='changed'"));
        assertThrows(SQLiteException.class, () -> store.getWritableDatabase().execSQL("INSERT INTO cliente_datos_adicionales VALUES(999,'rif','J2')"));
    }
    @Test public void inactivationPreservesClientAndHistory() {
        Cliente client = store.service().crear("Ana",Map.of("rif","J1"));
        store.service().inactivar(client.getId());
        assertTrue(store.service().listarActivos().isEmpty());
        assertEquals(1,store.service().listarTodos().size());
        assertEquals(2,store.history().listarPorEntidad("CLIENTE",client.getId()).size());
    }
}
