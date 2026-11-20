package org.drvo.reggisapp.android.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import org.drvo.reggisapp.domain.exception.PersistenciaException;
import org.drvo.reggisapp.domain.model.*;
import org.drvo.reggisapp.domain.service.*;
import org.drvo.reggisapp.repository.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;

/** Android-only storage. All calls must run on a worker thread. */
public final class ClientStore extends SQLiteOpenHelper implements TransactionBoundary {
    public ClientStore(Context context) { this(context, "reggis.db"); }
    public ClientStore(Context context, String name) {
        super(context.getApplicationContext(), name, null, 2);
        setWriteAheadLoggingEnabled(true);
    }
    @Override public void onConfigure(SQLiteDatabase db) { db.setForeignKeyConstraintsEnabled(true); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE clientes (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL CHECK(length(trim(nombre))>0), estado TEXT NOT NULL CHECK(estado IN ('ACTIVO','INACTIVO')))");
        db.execSQL("CREATE TABLE campos_cliente (id INTEGER PRIMARY KEY AUTOINCREMENT, clave TEXT NOT NULL UNIQUE, etiqueta TEXT NOT NULL, activo INTEGER NOT NULL CHECK(activo IN (0,1)))");
        db.execSQL("CREATE TABLE cliente_datos_adicionales (cliente_id INTEGER NOT NULL REFERENCES clientes(id) ON DELETE RESTRICT, clave TEXT NOT NULL, valor TEXT NOT NULL, PRIMARY KEY(cliente_id,clave))");
        db.execSQL("CREATE TABLE historial (id INTEGER PRIMARY KEY AUTOINCREMENT, tipo_entidad TEXT NOT NULL, entidad_id INTEGER NOT NULL, tipo_evento TEXT NOT NULL, fecha_registro TEXT NOT NULL, motivo TEXT, monto_bs TEXT, resolucion TEXT)");
        db.execSQL("CREATE INDEX idx_historial_entidad ON historial(tipo_entidad,entidad_id)");
        db.execSQL("CREATE TRIGGER historial_no_update BEFORE UPDATE ON historial BEGIN SELECT RAISE(ABORT,'El historial es inmutable'); END");
        db.execSQL("CREATE TRIGGER historial_no_delete BEFORE DELETE ON historial BEGIN SELECT RAISE(ABORT,'El historial es inmutable'); END");
        LedgerRepositories.createSchema(db);
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1 && newVersion == 2) LedgerRepositories.createSchema(db);
        else throw new SQLiteException("No hay una migración disponible para esta base de datos.");
    }
    @Override public <T> T inTransaction(Supplier<T> action) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            T result = action.get();
            db.setTransactionSuccessful();
            return result;
        } catch (SQLiteException error) {
            throw new PersistenciaException("No se pudieron guardar los datos.", error);
        } finally { db.endTransaction(); }
    }
    public ClienteService service() {
        return new ClienteServiceImpl(clients, fields, history, Clock.systemDefaultZone(), this);
    }
    public HistorialRepository history() { return history; }
    public CobranzaService cobranza() { return cobranza(Clock.systemDefaultZone()); }
    public CobranzaService cobranza(Clock clock) {
        LedgerRepositories ledger = new LedgerRepositories(this);
        CobranzaServiceImpl service = new CobranzaServiceImpl(clients, ledger.orders, ledger.payments,
                history, ledger.movements, new TasaCambioManualService(), clock, this);
        service.configurarMarcas(ledger.brands);
        return service;
    }

    private Map<String,String> contact(long id) {
        Map<String,String> result = new LinkedHashMap<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT clave,valor FROM cliente_datos_adicionales WHERE cliente_id=? ORDER BY clave", new String[]{Long.toString(id)})) {
            while(c.moveToNext()) result.put(c.getString(0), c.getString(1));
        }
        return result;
    }
    private Cliente readClient(Cursor c) {
        long id = c.getLong(0);
        return new Cliente(id, c.getString(1), EstadoCliente.valueOf(c.getString(2)), contact(id));
    }
    private void writeContact(long id, Map<String,String> data) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("cliente_datos_adicionales", "cliente_id=?", new String[]{Long.toString(id)});
        data.forEach((key,value) -> {
            ContentValues row = new ContentValues();
            row.put("cliente_id", id); row.put("clave", key); row.put("valor", value);
            db.insertOrThrow("cliente_datos_adicionales", null, row);
        });
    }
    private List<Cliente> list(boolean active) {
        List<Cliente> result = new ArrayList<>();
        String sql = "SELECT id,nombre,estado FROM clientes" + (active ? " WHERE estado='ACTIVO'" : "") + " ORDER BY nombre COLLATE NOCASE,id";
        try (Cursor c = getReadableDatabase().rawQuery(sql, null)) {
            while(c.moveToNext()) result.add(readClient(c));
        }
        return Collections.unmodifiableList(result);
    }
    private final ClienteRepository clients = new ClienteRepository() {
        public Cliente guardar(Cliente client) { return inTransaction(() -> {
            ContentValues row = new ContentValues();
            row.put("nombre",client.getNombre()); row.put("estado",client.getEstado().name());
            long id = getWritableDatabase().insertOrThrow("clientes", null, row);
            writeContact(id, client.getDatosAdicionales());
            return client.conId(id);
        }); }
        public Cliente actualizar(Cliente client) { return inTransaction(() -> {
            ContentValues row = new ContentValues();
            row.put("nombre",client.getNombre()); row.put("estado",client.getEstado().name());
            if (getWritableDatabase().update("clientes", row, "id=?", new String[]{String.valueOf(client.getId())}) != 1)
                throw new PersistenciaException("No se encontró el cliente.", null);
            writeContact(client.getId(), client.getDatosAdicionales());
            return client;
        }); }
        public Optional<Cliente> buscarPorId(Long id) {
            try(Cursor c = getReadableDatabase().rawQuery("SELECT id,nombre,estado FROM clientes WHERE id=?", new String[]{String.valueOf(id)})) {
                return c.moveToFirst() ? Optional.of(readClient(c)) : Optional.empty();
            }
        }
        public List<Cliente> listarActivos() { return list(true); }
        public List<Cliente> listarTodos() { return list(false); }
    };
    private final CampoClienteRepository fields = new CampoClienteRepository() {
        public CampoCliente guardar(CampoCliente field) {
            ContentValues row = new ContentValues();
            row.put("clave",field.getClave()); row.put("etiqueta",field.getEtiqueta()); row.put("activo",field.isActivo()?1:0);
            long id = getWritableDatabase().insertOrThrow("campos_cliente",null,row);
            return new CampoCliente(id,field.getClave(),field.getEtiqueta(),field.isActivo());
        }
        public List<CampoCliente> listarActivos() {
            List<CampoCliente> result = new ArrayList<>();
            try(Cursor c = getReadableDatabase().rawQuery("SELECT id,clave,etiqueta FROM campos_cliente WHERE activo=1 ORDER BY etiqueta COLLATE NOCASE", null)) {
                while(c.moveToNext()) result.add(new CampoCliente(c.getLong(0),c.getString(1),c.getString(2),true));
            }
            return Collections.unmodifiableList(result);
        }
    };
    private final HistorialRepository history = new HistorialRepository() {
        public RegistroHistorial guardar(RegistroHistorial event) {
            ContentValues row = new ContentValues();
            row.put("tipo_entidad",event.getTipoEntidad()); row.put("entidad_id",event.getEntidadId());
            row.put("tipo_evento",event.getTipoEvento()); row.put("fecha_registro",event.getFechaRegistro().toString());
            row.put("motivo",event.getMotivo()); row.put("monto_bs",event.getMontoBs()==null?null:event.getMontoBs().toPlainString()); row.put("resolucion",event.getResolucion());
            long id = getWritableDatabase().insertOrThrow("historial",null,row);
            return new RegistroHistorial(id,event.getTipoEntidad(),event.getEntidadId(),event.getTipoEvento(),event.getFechaRegistro(),event.getMotivo(),event.getMontoBs(),event.getResolucion());
        }
        public List<RegistroHistorial> listarPorEntidad(String type, Long id) {
            List<RegistroHistorial> result = new ArrayList<>();
            try(Cursor c = getReadableDatabase().rawQuery("SELECT id,tipo_entidad,entidad_id,tipo_evento,fecha_registro,motivo,monto_bs,resolucion FROM historial WHERE tipo_entidad=? AND entidad_id=? ORDER BY fecha_registro DESC,id DESC",new String[]{type,String.valueOf(id)})) {
                while(c.moveToNext()) result.add(new RegistroHistorial(c.getLong(0),c.getString(1),c.getLong(2),c.getString(3),LocalDateTime.parse(c.getString(4)),c.getString(5),c.isNull(6)?null:new BigDecimal(c.getString(6)),c.getString(7)));
            }
            return Collections.unmodifiableList(result);
        }
    };
}
