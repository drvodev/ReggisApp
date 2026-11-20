package org.drvo.reggisapp.android.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.drvo.reggisapp.domain.exception.*;
import org.drvo.reggisapp.domain.model.*;
import org.drvo.reggisapp.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** Parameterized Android adapters; monetary totals are accumulated as BigDecimal, never SQL REAL. */
final class LedgerRepositories {
    private final ClientStore store;
    LedgerRepositories(ClientStore store) { this.store = store; }
    private SQLiteDatabase db() { return store.getWritableDatabase(); }
    static void createSchema(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE pedidos (id INTEGER PRIMARY KEY AUTOINCREMENT, cliente_id INTEGER NOT NULL REFERENCES clientes(id) ON DELETE RESTRICT, descripcion TEXT NOT NULL CHECK(length(trim(descripcion))>0), monto_pedido_bs TEXT NOT NULL, monto_original TEXT NOT NULL, moneda_original TEXT NOT NULL CHECK(moneda_original IN ('BS','USD')), tasa_cambio TEXT NOT NULL, fecha_creacion TEXT NOT NULL, estado TEXT NOT NULL CHECK(estado IN ('ACTIVO','COMPLETADO','ANULADO')))");
        db.execSQL("CREATE TABLE pagos (id INTEGER PRIMARY KEY AUTOINCREMENT, pedido_id INTEGER NOT NULL REFERENCES pedidos(id) ON DELETE RESTRICT, fecha_pago TEXT NOT NULL, monto_bs TEXT NOT NULL, monto_original TEXT NOT NULL, moneda_original TEXT NOT NULL CHECK(moneda_original IN ('BS','USD')), tasa_cambio TEXT NOT NULL)");
        db.execSQL("CREATE TABLE movimientos_deuda (id INTEGER PRIMARY KEY AUTOINCREMENT, pedido_origen_id INTEGER NOT NULL REFERENCES pedidos(id) ON DELETE RESTRICT, pedido_destino_id INTEGER NOT NULL REFERENCES pedidos(id) ON DELETE RESTRICT, monto_bs TEXT NOT NULL, fecha TEXT NOT NULL, motivo TEXT NOT NULL)");
        db.execSQL("CREATE TABLE marcas (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL COLLATE NOCASE UNIQUE CHECK(length(trim(nombre))>0), activa INTEGER NOT NULL DEFAULT 1 CHECK(activa IN (0,1)))");
        db.execSQL("CREATE TABLE pedido_marcas (pedido_id INTEGER NOT NULL REFERENCES pedidos(id), marca_id INTEGER NOT NULL REFERENCES marcas(id), seleccionada INTEGER NOT NULL CHECK(seleccionada IN (0,1)), PRIMARY KEY(pedido_id,marca_id))");
        db.execSQL("CREATE INDEX idx_pedidos_cliente ON pedidos(cliente_id,estado)");
        db.execSQL("CREATE INDEX idx_pagos_pedido ON pagos(pedido_id,fecha_pago)");
        db.execSQL("CREATE INDEX idx_movimientos_origen ON movimientos_deuda(pedido_origen_id)");
        db.execSQL("CREATE INDEX idx_movimientos_destino ON movimientos_deuda(pedido_destino_id)");
    }
    private static String[] args(long id) { return new String[]{Long.toString(id)}; }
    private static BigDecimal amount(Cursor c, int column) { return new BigDecimal(c.getString(column)); }
    private static final String ORDER_COLUMNS = "id,cliente_id,descripcion,monto_pedido_bs,monto_original,moneda_original,tasa_cambio,fecha_creacion,estado";
    private Pedido order(Cursor c) {
        return new Pedido(c.getLong(0),c.getLong(1),c.getString(2),amount(c,3),amount(c,4),Moneda.valueOf(c.getString(5)),amount(c,6),LocalDate.parse(c.getString(7)),EstadoPedido.valueOf(c.getString(8)));
    }
    final PedidoRepository orders = new PedidoRepository() {
        public Pedido guardar(Pedido p) {
            ContentValues row = new ContentValues();
            row.put("cliente_id",p.getClienteId()); row.put("descripcion",p.getDescripcion());
            row.put("monto_pedido_bs",p.getMontoPedidoBs().toPlainString()); row.put("monto_original",p.getMontoOriginal().toPlainString());
            row.put("moneda_original",p.getMonedaOriginal().name()); row.put("tasa_cambio",p.getTasaCambio().toPlainString());
            row.put("fecha_creacion",p.getFechaCreacion().toString()); row.put("estado",p.getEstado().name());
            return p.conId(db().insertOrThrow("pedidos",null,row));
        }
        public Optional<Pedido> buscarPorId(Long id) {
            try(Cursor c=db().rawQuery("SELECT "+ORDER_COLUMNS+" FROM pedidos WHERE id=?",args(id))) { return c.moveToFirst()?Optional.of(order(c)):Optional.empty(); }
        }
        public Pedido actualizar(Pedido p) {
            ContentValues row = new ContentValues(); row.put("estado",p.getEstado().name());
            if(db().update("pedidos",row,"id=?",args(p.getId()))!=1) throw new PersistenciaException("No se encontró el pedido.",null);
            return p;
        }
        public List<Pedido> listarPorCliente(Long id) {
            List<Pedido> result=new ArrayList<>();
            try(Cursor c=db().rawQuery("SELECT "+ORDER_COLUMNS+" FROM pedidos WHERE cliente_id=? ORDER BY fecha_creacion DESC,id DESC",args(id))) { while(c.moveToNext()) result.add(order(c)); }
            return List.copyOf(result);
        }
    };
    final PagoRepository payments = new PagoRepository() {
        public Pago guardar(Pago p) {
            ContentValues row=new ContentValues(); row.put("pedido_id",p.getPedidoId()); row.put("fecha_pago",p.getFechaPago().toString());
            row.put("monto_bs",p.getMontoBs().toPlainString()); row.put("monto_original",p.getMontoOriginal().toPlainString());
            row.put("moneda_original",p.getMonedaOriginal().name()); row.put("tasa_cambio",p.getTasaCambio().toPlainString());
            return p.conId(db().insertOrThrow("pagos",null,row));
        }
        public List<Pago> listarPorPedido(Long id) {
            List<Pago> result=new ArrayList<>();
            try(Cursor c=db().rawQuery("SELECT id,pedido_id,fecha_pago,monto_bs,monto_original,moneda_original,tasa_cambio FROM pagos WHERE pedido_id=? ORDER BY fecha_pago,id",args(id))) {
                while(c.moveToNext()) result.add(new Pago(c.getLong(0),c.getLong(1),LocalDate.parse(c.getString(2)),amount(c,3),amount(c,4),Moneda.valueOf(c.getString(5)),amount(c,6)));
            }
            return List.copyOf(result);
        }
        public BigDecimal sumarPagosBs(Long id) { return sum("SELECT monto_bs FROM pagos WHERE pedido_id=?",id); }
    };
    private BigDecimal sum(String sql,long id) {
        BigDecimal total=BigDecimal.ZERO;
        try(Cursor c=db().rawQuery(sql,args(id))) { while(c.moveToNext()) total=total.add(amount(c,0)); }
        return total;
    }
    final MovimientoDeudaRepository movements = new MovimientoDeudaRepository() {
        public MovimientoDeuda guardar(MovimientoDeuda m) {
            ContentValues row=new ContentValues(); row.put("pedido_origen_id",m.getPedidoOrigenId()); row.put("pedido_destino_id",m.getPedidoDestinoId());
            row.put("monto_bs",m.getMontoBs().toPlainString()); row.put("fecha",m.getFecha().toString()); row.put("motivo",m.getMotivo());
            long id=db().insertOrThrow("movimientos_deuda",null,row);
            return new MovimientoDeuda(id,m.getPedidoOrigenId(),m.getPedidoDestinoId(),m.getMontoBs(),m.getFecha(),m.getMotivo());
        }
        public BigDecimal sumarEntradasBs(Long id) { return sum("SELECT monto_bs FROM movimientos_deuda WHERE pedido_destino_id=?",id); }
        public BigDecimal sumarSalidasBs(Long id) { return sum("SELECT monto_bs FROM movimientos_deuda WHERE pedido_origen_id=?",id); }
    };
    private List<MarcaPedido> brands(String sql,String[] params) {
        List<MarcaPedido> result=new ArrayList<>();
        try(Cursor c=db().rawQuery(sql,params)) { while(c.moveToNext()) result.add(new MarcaPedido(c.getLong(0),c.getString(1),c.getInt(2)==1)); }
        return List.copyOf(result);
    }
    final MarcaRepository brands = new MarcaRepository() {
        public List<MarcaPedido> listar() { return brands("SELECT id,nombre,0 FROM marcas WHERE activa=1 ORDER BY nombre COLLATE NOCASE",null); }
        public List<MarcaPedido> listarPorPedido(long id) { return brands("SELECT m.id,m.nombre,pm.seleccionada FROM pedido_marcas pm JOIN marcas m ON m.id=pm.marca_id WHERE pm.pedido_id=? ORDER BY m.nombre COLLATE NOCASE",args(id)); }
        public void crear(String name) {
            if(name==null || name.isBlank()) throw new ReglaNegocioException("Escribe el nombre de la marca.");
            if(name.strip().length()>100 || name.codePoints().anyMatch(Character::isISOControl)) throw new ReglaNegocioException("El nombre de la marca debe tener hasta 100 caracteres y no contener caracteres de control.");
            store.inTransaction(() -> {
                // Android 8's SQLite predates UPSERT; an explicit transaction preserves the desktop semantics.
                try(Cursor c=db().rawQuery("SELECT id,activa FROM marcas WHERE nombre=? COLLATE NOCASE",new String[]{name.strip()})) {
                    if(c.moveToFirst()) {
                        if(c.getInt(1)==1) throw new ReglaNegocioException("Esta marca ya existe.");
                        ContentValues row=new ContentValues(); row.put("activa",1); db().update("marcas",row,"id=?",args(c.getLong(0))); return null;
                    }
                }
                ContentValues row=new ContentValues(); row.put("nombre",name.strip()); db().insertOrThrow("marcas",null,row); return null;
            });
        }
        public void retirar(long id) {
            store.inTransaction(() -> {
                ContentValues row=new ContentValues(); row.put("activa",0);
                if(db().update("marcas",row,"id=? AND activa=1",args(id))!=1) throw new ReglaNegocioException("La marca no está disponible.");
                return null;
            });
        }
        public void guardarSeleccion(long id,Set<Long> selected) {
            List<MarcaPedido> available=listar();
            if(!available.stream().map(MarcaPedido::id).collect(Collectors.toSet()).containsAll(selected)) throw new ReglaNegocioException("Una marca seleccionada ya no existe.");
            for(MarcaPedido brand:available) {
                ContentValues row=new ContentValues(); row.put("pedido_id",id); row.put("marca_id",brand.id()); row.put("seleccionada",selected.contains(brand.id())?1:0);
                db().insertOrThrow("pedido_marcas",null,row);
            }
        }
    };
}
