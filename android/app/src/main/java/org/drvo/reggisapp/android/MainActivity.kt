package org.drvo.reggisapp.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.drvo.reggisapp.domain.model.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF1769D2), background = Color(0xFFF5F7FB))) {
                ReggisScreen(viewModel())
            }
        }
    }
}
internal fun money(value: BigDecimal) = value.setScale(2,RoundingMode.HALF_UP).toPlainString()
private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private fun stateLabel(state: EstadoPedido) = when(state) { EstadoPedido.ACTIVO -> "Activo"; EstadoPedido.COMPLETADO -> "Completado"; EstadoPedido.ANULADO -> "Anulado" }
private val contactFields = listOf("rif" to "RIF", "email" to "Correo", "telefono" to "Teléfono", "direccion" to "Dirección")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReggisScreen(vm: ClientsViewModel) {
    var dialog by rememberSaveable { mutableStateOf("") }
    var removedBrand by rememberSaveable { mutableLongStateOf(-1L) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.ms-excel"), vm::writeExport)
    LaunchedEffect(vm.exportFile,vm.pickerLaunched) {
        if(vm.exportFile != null && !vm.pickerLaunched) {
            vm.pickerOpened()
            try { picker.launch(if(vm.route=="order") "Reggis-pedido-${vm.orderId}.xls" else "Reggis-cliente-${vm.selectedId}.xls") }
            catch (_: android.content.ActivityNotFoundException) { vm.exportUnavailable() }
        }
    }
    val back: () -> Unit = { if (!vm.busy) { if(vm.form) dialog="discard" else vm.back() } }
    BackHandler(vm.route!="list") { back() }
    if(dialog.isNotEmpty()) {
        val final = dialog.endsWith("2")
        val title = when { dialog=="discard" -> "¿Salir del formulario?"; dialog=="brand" -> "Retirar marca"; final -> "Confirmación final"; dialog=="client1" -> "Inactivar cliente"; else -> "Confirmar anulación" }
        val text = when {
            dialog=="discard" -> "Los cambios que no guardaste se descartarán."
            dialog=="brand" -> "Se retirará del catálogo. Los pedidos anteriores conservarán esta marca."
            dialog.startsWith("client") -> "El cliente dejará de estar activo. Sus pedidos, pagos e historial se conservarán."
            else -> "Se anulará el pedido #${vm.orderId}. " + (if(vm.draft["resolucion"]=="TRASLADAR_SALDO") "El saldo se trasladará al pedido #${vm.draft["destino"] ?: "sin seleccionar"}." else "Se condonará el saldo pendiente.") + " Esta decisión quedará registrada y no se puede editar."
        }
        AlertDialog(onDismissRequest={dialog=""}, title={Text(title)}, text={Text(text)},
            confirmButton={TextButton(onClick={
                when(dialog) {
                    "discard" -> {dialog="";vm.back()}
                    "brand" -> {dialog="";vm.removeBrand(removedBrand)}
                    "client1" -> dialog="client2"
                    "client2" -> {dialog="";vm.inactivate()}
                    "cancel1" -> dialog="cancel2"
                    "cancel2" -> {dialog="";vm.cancelOrder()}
                }
            }){Text(if(dialog=="discard") "Salir" else if(final) "Confirmar definitivamente" else "Continuar")}},
            dismissButton={TextButton(onClick={dialog=""}){Text(if(dialog=="discard") "Seguir editando" else "Cancelar")}})
    }
    Scaffold(topBar={TopAppBar(title={Text("Reggis")},navigationIcon={
        if(vm.route!="list") TextButton(onClick=back,enabled=!vm.busy){Text("Volver")}
    })}) { padding ->
        Box(Modifier.padding(padding).fillMaxSize(),contentAlignment=Alignment.TopCenter) {
            Column(Modifier.widthIn(max=840.dp).fillMaxSize().padding(horizontal=20.dp)) {
                if(vm.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                vm.error?.let { Text(it,color=MaterialTheme.colorScheme.error,modifier=Modifier.padding(vertical=8.dp))
                    if(!vm.form) TextButton(onClick=vm::reload,enabled=!vm.busy){Text("Reintentar")}
                }
                vm.savedMessage?.let { Text(it,color=MaterialTheme.colorScheme.primary,modifier=Modifier.padding(vertical=8.dp)) }
                when(vm.route) {
                    "list" -> ClientsList(vm)
                    "form" -> ClientForm(vm)
                    "detail" -> ClientDetail(vm){dialog="client1"}
                    "order" -> OrderDetail(vm)
                    "newOrder" -> ScrollForm {
                        Heading("Nuevo pedido")
                        Text(vm.selected?.nombre ?: "")
                        DraftField(vm,"descripcion","Descripción *", multiline=true)
                        MoneyForm(vm)
                        Heading("Marcas compradas",small=true)
                        if(vm.brands.isEmpty()) Text("No hay marcas en el catálogo. Puedes agregar una o continuar sin marcas.")
                        vm.brands.forEach { brand ->
                            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                                Checkbox(checked=brand.id() in vm.selectedBrands(),onCheckedChange={vm.toggleBrand(brand.id())},enabled=!vm.busy)
                                Text(brand.nombre(),Modifier.weight(1f))
                                TextButton(onClick={removedBrand=brand.id();dialog="brand"},enabled=!vm.busy){Text("Retirar")}
                            }
                        }
                        DraftField(vm,"marca","Nueva marca")
                        OutlinedButton(onClick=vm::addBrand,enabled=!vm.busy){Text("Agregar marca")}
                        Action("Crear pedido",vm,vm::saveOrder)
                    }
                    "payment" -> ScrollForm {
                        Heading("Registrar pago")
                        Text("Pedido #${vm.orderId} · ${vm.selectedOrder?.order?.descripcion ?: ""}")
                        Text("Saldo pendiente: ${vm.selectedOrder?.balance?.let(::money) ?: "—"} BS")
                        MoneyForm(vm)
                        Text("La fecha del pago será la fecha actual del teléfono.")
                        Action("Guardar pago",vm,vm::pay)
                    }
                    "cancel" -> ScrollForm {
                        Heading("Anular pedido #${vm.orderId}")
                        Text("Saldo: ${vm.selectedOrder?.balance?.let(::money) ?: "—"} BS")
                        DraftField(vm,"motivo","Motivo *",multiline=true)
                        Choice("Condonar saldo",vm.draft["resolucion"]!="TRASLADAR_SALDO",!vm.busy){vm.change("resolucion","CONDONAR_SALDO")}
                        Choice("Trasladar a otro pedido",vm.draft["resolucion"]=="TRASLADAR_SALDO",!vm.busy){vm.change("resolucion","TRASLADAR_SALDO")}
                        if(vm.draft["resolucion"]=="TRASLADAR_SALDO") {
                            val available=vm.orders.filter{it.order.id!=vm.orderId && it.order.estado==EstadoPedido.ACTIVO}
                            if(available.isEmpty()) Text("No hay otro pedido activo para recibir el saldo. Crea uno antes de continuar.")
                            available.forEach { Choice("#${it.order.id} · ${it.order.descripcion}", vm.draft["destino"]==it.order.id.toString(),!vm.busy){vm.change("destino",it.order.id.toString())} }
                        }
                        Action("Continuar con anulación",vm,{dialog="cancel1"},
                            !vm.draft["motivo"].isNullOrBlank() && (vm.draft["resolucion"]!="TRASLADAR_SALDO" || vm.draft["destino"]!=null))
                    }
                    "calculator" -> ScrollForm { Heading("Conversión BS / USD"); MoneyForm(vm); Text("Tasa manual. Esta consulta no registra un pedido ni un pago.") }
                    "fields" -> ScrollForm {
                        Heading("Campos adicionales")
                        Text("Agrega datos opcionales a los formularios de clientes.")
                        vm.fields.forEach { Text("${it.etiqueta} (${it.clave})") }
                        DraftField(vm,"clave","Clave del campo")
                        DraftField(vm,"etiqueta","Nombre visible")
                        Action("Agregar campo",vm,vm::addField)
                    }
                    "clientHistory" -> EventList(vm.clientEvents)
                }
            }
        }
    }
}

@Composable private fun Heading(text:String, small:Boolean=false) { Text(text,style=if(small) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium) }
@Composable private fun ScrollForm(content:@Composable ColumnScope.()->Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(vertical=12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        content(); Spacer(Modifier.height(24.dp))
    }
}
@Composable private fun Action(text:String,vm:ClientsViewModel,onClick:()->Unit,enabled:Boolean=true) {
    Button(onClick=onClick,enabled=!vm.busy && enabled,modifier=Modifier.fillMaxWidth()){Text(text)}
}
@Composable private fun DraftField(vm:ClientsViewModel,key:String,label:String,multiline:Boolean=false,numeric:Boolean=false) {
    OutlinedTextField(value=vm.draft[key] ?: "",onValueChange={vm.change(key,it)},label={Text(label)},enabled=!vm.busy,
        singleLine=!multiline,minLines=if(multiline)2 else 1,modifier=Modifier.fillMaxWidth(),
        keyboardOptions=KeyboardOptions(keyboardType=when {numeric -> KeyboardType.Decimal; key=="email" -> KeyboardType.Email; key=="telefono" -> KeyboardType.Phone; else -> KeyboardType.Text}))
}
@Composable private fun Choice(text:String,selected:Boolean,enabled:Boolean,onClick:()->Unit) {
    Row(Modifier.selectable(selected=selected,enabled=enabled,role=Role.RadioButton,onClick=onClick).padding(end=12.dp),verticalAlignment=Alignment.CenterVertically) {
        RadioButton(selected,onClick=null,enabled=enabled); Text(text)
    }
}
@Composable private fun MoneyForm(vm:ClientsViewModel) {
    DraftField(vm,"monto","Monto *",numeric=true)
    Row { Choice("BS",vm.draft["moneda"]!="USD",!vm.busy){vm.change("moneda","BS")}; Choice("USD",vm.draft["moneda"]=="USD",!vm.busy){vm.change("moneda","USD")} }
    DraftField(vm,"tasa","Tasa BS por USD *",numeric=true)
    Text("Usa coma o punto decimal, sin separadores de miles.",style=MaterialTheme.typography.bodySmall)
    vm.conversion()?.let { Text("Equivalente: ${money(it.montoBs)} BS · ${money(it.montoUsd)} USD",color=MaterialTheme.colorScheme.primary) }
}
@Composable private fun ClientsList(vm:ClientsViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    Heading("Clientes")
    Text("${vm.clients.count{it.estado==EstadoCliente.ACTIVO}} clientes activos",modifier=Modifier.padding(vertical=8.dp))
    Action("Registrar cliente",vm,{vm.edit(null)})
    Row(Modifier.horizontalScroll(rememberScrollState())) {
        TextButton(onClick={vm.begin("calculator")},enabled=!vm.busy){Text("Conversión")}
        TextButton(onClick={vm.begin("fields")},enabled=!vm.busy){Text("Campos adicionales")}
    }
    OutlinedTextField(query,{query=it},label={Text("Buscar por nombre o RIF")},singleLine=true,modifier=Modifier.fillMaxWidth())
    val matches=vm.clients.filter{it.nombre.contains(query,true) || (it.datosAdicionales["rif"] ?: "").contains(query,true)}
    LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(vertical=16.dp)) {
        if(matches.isEmpty() && !vm.loading) item {Text(if(query.isEmpty()) "Todavía no hay clientes. Registra el primero para comenzar." else "No hay clientes que coincidan.")}
        items(matches,key={it.id}) { client ->
            OutlinedCard(onClick={vm.open(client)},enabled=!vm.busy,modifier=Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(client.nombre,style=MaterialTheme.typography.titleMedium)
                    Text("RIF: ${client.datosAdicionales["rif"] ?: "—"}")
                    Text(if(client.estado==EstadoCliente.ACTIVO) "Activo" else "Inactivo",style=MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
@Composable private fun ClientForm(vm:ClientsViewModel) { ScrollForm {
    Heading(if(vm.selectedId<0) "Registrar cliente" else "Editar cliente")
    Text("Los campos con * son obligatorios.")
    DraftField(vm,"nombre","Nombre *")
    contactFields.forEach{(key,label)->DraftField(vm,key,label+if(key=="rif") " *" else "",multiline=key=="direccion")}
    vm.fields.filter{it.clave !in setOf("nombre","rif","email","telefono","direccion")}.forEach{DraftField(vm,it.clave,it.etiqueta)}
    Action("Guardar cliente",vm,vm::save)
} }
@Composable private fun ClientDetail(vm:ClientsViewModel,onInactivate:()->Unit) {
    val client=vm.selected
    if(client==null) {if(!vm.loading) Text("No se encontró el cliente.");return}
    var filter by rememberSaveable(vm.selectedId){mutableStateOf("Todos")}
    LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(vertical=12.dp)) {
        item { Heading(client.nombre); Text(if(client.estado==EstadoCliente.ACTIVO) "Cliente activo" else "Cliente inactivo") }
        item { Action("Editar cliente",vm,{vm.edit(client)}) }
        item { OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            contactFields.forEach{(key,label)->Text("$label: ${client.datosAdicionales[key] ?: "Sin registrar"}")}
            client.datosAdicionales.filterKeys{key->contactFields.none{it.first==key}}.forEach{(key,value)->Text("${vm.fields.firstOrNull{it.clave==key}?.etiqueta ?: key}: $value")}
        } } }
        item { Text("Saldo de pedidos activos: ${money(vm.orders.fold(BigDecimal.ZERO){total,p->total+p.balance})} BS",style=MaterialTheme.typography.titleMedium) }
        item { Action("Nuevo pedido",vm,{vm.begin("newOrder")},client.estado==EstadoCliente.ACTIVO) }
        item { OutlinedButton(onClick={vm.prepareExport(false)},enabled=!vm.busy,modifier=Modifier.fillMaxWidth()){Text("Exportar cliente (.xls)")} }
        item { TextButton(onClick=vm::showHistory,enabled=!vm.busy){Text("Historial del cliente")} }
        item { Heading("Pedidos",true); Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("Todos","Activo","Completado","Anulado").forEach { value -> FilterChip(selected=filter==value,onClick={filter=value},label={Text(value)}) }
        } }
        if(vm.orders.isEmpty() && !vm.loading) item{Text("Este cliente aún no tiene pedidos.")}
        items(vm.orders.filter{filter=="Todos" || stateLabel(it.order.estado)==filter},key={it.order.id}) { p ->
            OutlinedCard(onClick={vm.openOrder(p.order.id)},enabled=!vm.busy,modifier=Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                    Text("#${p.order.id} · ${p.order.descripcion}",style=MaterialTheme.typography.titleMedium)
                    Text("${stateLabel(p.order.estado)} · ${dateFormat.format(p.order.fechaCreacion)}")
                    Text("Total: ${money(p.order.montoPedidoBs)} BS · Saldo: ${money(p.balance)} BS")
                }
            }
        }
        if(client.estado==EstadoCliente.ACTIVO) item { TextButton(onClick=onInactivate,enabled=!vm.busy){Text("Inactivar cliente",color=MaterialTheme.colorScheme.error)} }
    }
}
@Composable private fun OrderDetail(vm:ClientsViewModel) {
    val p=vm.selectedOrder
    if(p==null){if(!vm.loading)Text("No se encontró el pedido.");return}
    var tab by rememberSaveable(vm.orderId){mutableStateOf("Resumen")}
    Column {
        Text("Pedido #${p.order.id}",style=MaterialTheme.typography.headlineSmall)
        Text(p.order.descripcion)
        Text("${stateLabel(p.order.estado)} · Saldo: ${money(p.balance)} BS",color=MaterialTheme.colorScheme.primary)
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("Resumen","Pagos","Marcas","Historial").forEach { t -> FilterChip(tab==t,{tab=t},label={Text(t)}) }
        }
        when(tab) {
            "Resumen" -> ScrollForm {
                Text("Creado: ${dateFormat.format(p.order.fechaCreacion)}")
                Text("Importe original: ${money(p.order.montoOriginal)} ${p.order.monedaOriginal}")
                Text("Tasa aplicada: ${p.order.tasaCambio.toPlainString()} BS por USD")
                Text("Total del pedido: ${money(p.order.montoPedidoBs)} BS")
                Text("Pagado: ${money(p.payments.fold(BigDecimal.ZERO){sum,payment->sum+payment.montoBs})} BS")
                Heading("Seguimiento",true)
                Text("Promedio entre pagos: ${p.time.promedioDiasEntrePagos} días")
                Text("Hasta el primer pago: ${p.time.diasHastaPrimerPago} días")
                Text("Duración hasta saldar: ${p.time.diasHastaCancelacion?.let{"$it días"} ?: "Pendiente"}")
                if(p.payments.isEmpty()) Text("Sin pagos registrados; los intervalos todavía no están disponibles.")
                Action("Registrar pago",vm,{vm.begin("payment")},p.order.estado==EstadoPedido.ACTIVO)
                OutlinedButton(onClick={vm.prepareExport(true)},enabled=!vm.busy,modifier=Modifier.fillMaxWidth()){Text("Exportar pedido (.xls)")}
                if(p.order.estado==EstadoPedido.ACTIVO) TextButton(onClick={vm.begin("cancel")},enabled=!vm.busy){Text("Anular pedido",color=MaterialTheme.colorScheme.error)}
            }
            "Pagos" -> LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(vertical=12.dp)) {
                if(p.payments.isEmpty()) item{Text("Sin pagos registrados.")}
                items(p.payments,key={it.id}) { payment -> OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                    Text("Pago #${payment.id} · ${dateFormat.format(payment.fechaPago)}")
                    Text("${money(payment.montoOriginal)} ${payment.monedaOriginal} = ${money(payment.montoBs)} BS")
                    Text("Tasa: ${payment.tasaCambio.toPlainString()} BS por USD")
                } } }
            }
            "Marcas" -> LazyColumn(contentPadding=PaddingValues(vertical=12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                if(p.brands.isEmpty()) item{Text("Sin marcas registradas para este pedido.")}
                items(p.brands,key={it.id()}) { Text("${if(it.seleccionada()) "Comprada" else "No comprada"} · ${it.nombre()}") }
            }
            "Historial" -> EventList(p.events)
        }
    }
}
@Composable private fun EventList(events:List<RegistroHistorial>) {
    LazyColumn(contentPadding=PaddingValues(vertical=12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item{Heading("Historial",true)}
        if(events.isEmpty()) item{Text("No hay eventos registrados.")}
        items(events,key={it.id}) { e -> OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
            Text(e.tipoEvento.replace('_',' '),style=MaterialTheme.typography.titleSmall)
            Text(e.fechaRegistro.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
            Text("${e.tipoEntidad} #${e.entidadId}")
            e.montoBs?.let{Text("${money(it)} BS")}
            e.motivo?.let{Text("Motivo: $it")}
            e.resolucion?.let{Text("Resolución: ${it.replace('_',' ')}")}
        } } }
    }
}
