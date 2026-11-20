package org.drvo.reggisapp.android

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@LooperMode(LooperMode.Mode.PAUSED)
class BillingScreenTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun ready(){compose.waitUntil(25000){compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).fetchSemanticsNodes().isEmpty()}}
    private fun click(text:String){
        if(compose.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()) compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(text))
        compose.onNodeWithText(text).performScrollTo().performClick()
    }
    private fun fill(text:String,value:String){compose.onNodeWithText(text).performScrollTo().performTextReplacement(value)}
    private fun message(text:String){compose.waitUntil(25000){compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()};ready()}
    private fun newOrder(description:String,amount:String){
        click("Nuevo pedido");fill("Descripción *",description);fill("Monto *",amount);fill("Tasa BS por USD *","1")
        click("Crear pedido");message("Pedido creado")
    }
    @Test fun orderPaymentTransferAndInactivationAreReachable(){
        ready();compose.onNodeWithText("Registrar cliente").performClick()
        fill("Nombre *","Cliente flujo");fill("RIF *","J-999");click("Guardar cliente");message("Cliente guardado")
        newOrder("Origen","100")
        click("Registrar pago");fill("Monto *","40");fill("Tasa BS por USD *","1");click("Guardar pago");message("Pago registrado")
        compose.onNodeWithText("Activo · Saldo: 60.00 BS").assertExists()
        compose.onNodeWithText("Volver").performClick()
        newOrder("Destino","20")
        compose.onNodeWithText("Volver").performClick()
        click("#1 · Origen");click("Anular pedido")
        fill("Motivo *","Reorganización")
        click("Trasladar a otro pedido");click("#2 · Destino");click("Continuar con anulación")
        compose.onNodeWithText("Continuar").performClick()
        compose.onNodeWithText("Confirmar definitivamente").performClick();message("Pedido anulado; historial conservado")
        compose.onNodeWithText("Anulado · Saldo: 0.00 BS").assertExists()
        compose.onNodeWithText("Volver").performClick();click("#2 · Destino")
        compose.onNodeWithText("Activo · Saldo: 80.00 BS").assertExists()
        click("Registrar pago");fill("Monto *","80");fill("Tasa BS por USD *","1");click("Guardar pago");message("Pago registrado")
        compose.onNodeWithText("Completado · Saldo: 0.00 BS").assertExists()
        compose.onNodeWithText("Volver").performClick();click("Inactivar cliente")
        compose.onNodeWithText("Continuar").performClick()
        compose.onNodeWithText("Confirmar definitivamente").performClick();message("Cliente inactivado; pedidos e historial conservados")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Cliente inactivo"))
        compose.onNodeWithText("Cliente inactivo").assertExists()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Nuevo pedido"))
        compose.onNodeWithText("Nuevo pedido").assertIsNotEnabled()
    }
}
