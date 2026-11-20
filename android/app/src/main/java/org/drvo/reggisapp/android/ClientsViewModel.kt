package org.drvo.reggisapp.android

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.drvo.reggisapp.android.data.ClientStore
import org.drvo.reggisapp.domain.exception.ReglaNegocioException
import org.drvo.reggisapp.domain.model.*
import org.drvo.reggisapp.domain.service.*
import org.drvo.reggisapp.report.*
import java.io.File
import java.math.BigDecimal

internal object Stores {
    @Volatile private var instance: ClientStore? = null
    fun get(app: Application): ClientStore = instance ?: synchronized(this) {
        instance ?: ClientStore(app).also { instance = it }
    }
}
data class OrderData(val order: Pedido, val balance: BigDecimal, val payments: List<Pago>,
                     val brands: List<MarcaPedido>, val events: List<RegistroHistorial>, val time: ResumenTiempo)
private data class Snapshot(val clients: List<Cliente>, val orders: List<OrderData>, val brands: List<MarcaPedido>,
                            val fields: List<CampoCliente>, val events: List<RegistroHistorial>)
internal fun decimalInput(text: String): BigDecimal {
    val value = text.trim()
    if (value.length > 80 || !value.matches(Regex("[+-]?[0-9]+([.,][0-9]+)?")))
        throw ReglaNegocioException("Escribe un número válido, sin separadores de miles. Ejemplo: 1250,50.")
    return BigDecimal(value.replace(',', '.'))
}

class ClientsViewModel(app: Application, private val saved: SavedStateHandle) : AndroidViewModel(app) {
    private val store = Stores.get(app)
    private val service = store.service()
    private val billing = store.cobranza()
    private val timing = TiempoServiceImpl()
    var clients by mutableStateOf<List<Cliente>>(emptyList()); private set
    var orders by mutableStateOf<List<OrderData>>(emptyList()); private set
    var brands by mutableStateOf<List<MarcaPedido>>(emptyList()); private set
    var fields by mutableStateOf<List<CampoCliente>>(emptyList()); private set
    var clientEvents by mutableStateOf<List<RegistroHistorial>>(emptyList()); private set
    var loading by mutableStateOf(false); private set
    var saving by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var route by mutableStateOf(saved.get<String>("route") ?: "list"); private set
    var selectedId by mutableStateOf(saved.get<Long>("id") ?: -1L); private set
    var orderId by mutableStateOf(saved.get<Long>("orderId") ?: -1L); private set
    var draft by mutableStateOf(saved.get<HashMap<String, String>>("draft")?.toMap() ?: emptyMap()); private set
    var savedMessage by mutableStateOf<String?>(null); private set
    var exportFile by mutableStateOf(saved.get<String>("exportFile")); private set
    var pickerLaunched by mutableStateOf(saved.get<Boolean>("pickerLaunched") ?: false); private set
    val busy get() = saving || loading || exportFile != null
    val selected get() = clients.firstOrNull { it.id == selectedId }
    val selectedOrder get() = orders.firstOrNull { it.order.id == orderId }
    val form get() = route in setOf("form", "newOrder", "payment", "cancel")
    init { reload() }
    private fun navigate(to: String) { route = to; saved["route"] = to; error = null }
    private fun select(id: Long) { selectedId = id; saved["id"] = id }
    private fun selectOrder(id: Long) { orderId = id; saved["orderId"] = id }
    private fun replaceDraft(values: Map<String, String>) { draft = values; saved["draft"] = HashMap(values) }
    fun change(key: String, value: String) { if (!saving) replaceDraft(draft + (key to value)) }
    private fun snapshot(id: Long): Snapshot = store.inTransaction {
        val all = service.listarTodos()
        val data = if (all.any { it.id == id }) billing.listarPedidos(id, null).map { p ->
            val payments = billing.listarPagos(p.id)
            val events = store.history().listarPorEntidad("PEDIDO", p.id) + payments.flatMap { store.history().listarPorEntidad("PAGO", it.id) }
            OrderData(p, billing.obtenerSaldoPendiente(p.id), payments, billing.listarMarcasPedido(p.id),
                events.sortedWith(compareByDescending<RegistroHistorial> { it.fechaRegistro }.thenByDescending { it.id }), timing.calcularTiempo(p, payments))
        } else emptyList()
        Snapshot(all, data, billing.listarMarcas(), service.listarCamposActivos(),
            if (id > 0) store.history().listarPorEntidad("CLIENTE", id) else emptyList())
    }
    fun reload() {
        if (loading || saving) return
        loading = true
        val id = selectedId
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { snapshot(id) }
                clients = result.clients; brands = result.brands; fields = result.fields
                if (selectedId == id) { orders = result.orders; clientEvents = result.events }
                error = null
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = "No se pudieron cargar los datos. Intenta de nuevo." }
            finally { loading = false }
        }
    }
    fun open(client: Cliente) { if (busy) return; select(client.id); selectOrder(-1); savedMessage = null; navigate("detail"); reload() }
    fun openOrder(id: Long) { if (!busy) { selectOrder(id); savedMessage = null; navigate("order") } }
    fun edit(client: Cliente?) {
        if (busy) return
        select(client?.id ?: -1L)
        replaceDraft((client?.datosAdicionales ?: emptyMap()) + ("nombre" to (client?.nombre ?: "")))
        savedMessage = null; navigate("form")
    }
    fun begin(to: String) {
        if (busy) return
        replaceDraft(mapOf("moneda" to "BS", "resolucion" to "CONDONAR_SALDO"))
        savedMessage = null; navigate(to)
    }
    fun back() {
        if (busy) return
        val next = when (route) {
            "form" -> if (selectedId > 0) "detail" else "list"
            "payment", "cancel" -> "order"
            "newOrder", "order", "clientHistory" -> "detail"
            else -> "list"
        }
        replaceDraft(emptyMap()); navigate(next); savedMessage = null
    }
    private fun message(e: Exception) = if (e is ReglaNegocioException || e is IllegalArgumentException)
        e.message ?: "Revisa los datos ingresados." else "No se pudo guardar la operación. No vuelvas a enviarla hasta comprobar los datos."
    private fun <T> mutate(action: () -> T, success: (T) -> Unit) {
        if (busy) return
        saving = true; error = null; savedMessage = null
        viewModelScope.launch {
            var committed = false
            try {
                val result = withContext(Dispatchers.IO) { action() }
                committed = true
                success(result)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = message(e) }
            finally { saving = false }
            // A failed read after a commit must not leave a retryable write form.
            if (committed) reload()
        }
    }
    private fun done(to: String, text: String) { replaceDraft(emptyMap()); navigate(to); savedMessage = text }
    fun save() {
        val values = draft.toMutableMap(); val name = values.remove("nombre") ?: ""; val id = selectedId
        mutate({ if (id < 0) service.crear(name, values) else service.actualizar(id, name, values) }) { c ->
            clients = (clients.filterNot { it.id == c.id } + c).sortedBy { it.nombre.lowercase() }
            select(c.id); done("detail", "Cliente guardado")
        }
    }
    fun saveOrder() {
        val d = draft.toMap(); val id = selectedId
        mutate({ billing.crearPedidoConMarcas(id, d["descripcion"] ?: "", decimalInput(d["monto"] ?: ""),
            Moneda.valueOf(d["moneda"] ?: "BS"), decimalInput(d["tasa"] ?: ""), selectedBrands(d)) }) {
            selectOrder(it.id); done("order", "Pedido creado")
        }
    }
    fun pay() {
        val d = draft.toMap(); val id = orderId
        mutate({ billing.registrarPago(id, decimalInput(d["monto"] ?: ""), Moneda.valueOf(d["moneda"] ?: "BS"), decimalInput(d["tasa"] ?: "")) }) {
            done("order", "Pago registrado")
        }
    }
    fun cancelOrder() {
        val d = draft.toMap(); val id = orderId
        mutate({ billing.anularPedido(id, d["motivo"] ?: "", ResolucionAnulacion.valueOf(d["resolucion"] ?: "CONDONAR_SALDO"),
            if (d["resolucion"] == "TRASLADAR_SALDO") d["destino"]?.toLongOrNull() else null) }) {
            done("order", "Pedido anulado; historial conservado")
        }
    }
    fun inactivate() { val id = selectedId; mutate({ service.inactivar(id) }) { done("detail", "Cliente inactivado; pedidos e historial conservados") } }
    fun addBrand() { val name = draft["marca"] ?: ""; mutate({ billing.crearMarca(name) }) { replaceDraft(draft + ("marca" to "")); savedMessage = "Marca agregada" } }
    fun removeBrand(id: Long) { mutate({ billing.retirarMarca(id) }) {
        replaceDraft(draft + ("marcas" to (selectedBrands(draft) - id).joinToString(","))); savedMessage = "Marca retirada del catálogo"
    } }
    fun toggleBrand(id: Long) { val values = selectedBrands(draft).toMutableSet(); if (!values.add(id)) values.remove(id); change("marcas", values.joinToString(",")) }
    fun selectedBrands(d: Map<String,String> = draft): Set<Long> = d["marcas"]?.split(',')?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()
    fun addField() {
        val key = draft["clave"] ?: ""; val label = draft["etiqueta"] ?: ""
        mutate({
            if (key.trim().lowercase() in setOf("nombre", "rif", "email", "telefono", "direccion")) throw ReglaNegocioException("Ese campo ya está incluido en el formulario.")
            service.crearCampo(key,label)
        }) { done("fields", "Campo agregado") }
    }
    fun showHistory() { if (!busy) navigate("clientHistory") }
    fun conversion(): ConversionMonetaria? = try {
        TasaCambioManualService().convertir(decimalInput(draft["monto"] ?: ""), Moneda.valueOf(draft["moneda"] ?: "BS"), decimalInput(draft["tasa"] ?: ""))
    } catch (_: Exception) { null }
    fun prepareExport(singleOrder: Boolean) {
        if (busy) return
        val client = selected ?: return
        val id = orderId
        saving = true; error = null
        viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    val reports = store.inTransaction {
                        billing.listarPedidos(client.id,null).filter { !singleOrder || it.id == id }.map {
                            val payments = billing.listarPagos(it.id)
                            ReportePedido(it,payments,billing.obtenerSaldoPendiente(it.id),timing.calcularTiempo(it,payments))
                        }
                    }
                    val f = File.createTempFile("reggis-", ".xls", getApplication<Application>().cacheDir)
                    try {
                        val exporter = XlsReportExporter()
                        if (singleOrder) exporter.exportarPedido(client,reports.single(),f.toPath())
                        else exporter.exportarCliente(client,reports,f.toPath())
                        f
                    } catch (e: Exception) { f.delete(); throw e }
                }
                exportFile = file.absolutePath; saved["exportFile"] = exportFile
                pickerLaunched = false; saved["pickerLaunched"] = false
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = "No se pudo generar el reporte: ${e.message ?: "intenta de nuevo"}" }
            finally { saving = false }
        }
    }
    fun pickerOpened() { pickerLaunched = true; saved["pickerLaunched"] = true }
    fun exportUnavailable() {
        exportFile?.let { clearExport(File(it)) }
        error = "Este teléfono no tiene un selector de documentos disponible para guardar el reporte."
    }
    fun writeExport(uri: Uri?) {
        val file = exportFile?.let(::File) ?: return
        if (uri == null) { clearExport(file); return }
        saving = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use { output -> file.inputStream().use { it.copyTo(output) } }
                        ?: throw java.io.IOException("No se pudo abrir el archivo elegido.")
                }
                savedMessage = "Reporte Excel guardado"
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = "No se pudo guardar el reporte. El archivo de destino podría estar incompleto; vuelve a exportarlo." }
            finally { clearExport(file); saving = false }
        }
    }
    private fun clearExport(file: File) {
        file.delete(); exportFile = null; saved.remove<String>("exportFile")
        pickerLaunched = false; saved["pickerLaunched"] = false
    }
}
