package org.drvo.reggisapp.android

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@LooperMode(LooperMode.Mode.PAUSED)
class ClientsScreenTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun ready() { compose.waitUntil(20000) { compose.onAllNodes(SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsProperties.ProgressBarRangeInfo)).fetchSemanticsNodes().isEmpty() } }

    @Test fun createsEditsAndRestoresDraftAfterRecreation() {
        compose.waitUntil(15000) {
            compose.onAllNodesWithText("Registrar cliente").fetchSemanticsNodes().isNotEmpty()
        }
        ready()
        compose.onNodeWithText("Registrar cliente").performClick()
        compose.onNodeWithText("Nombre *").performTextInput("Cliente Android")
        compose.activityRule.scenario.recreate()
        ready()
        compose.onNodeWithText("Cliente Android").assertExists()
        compose.onNodeWithText("RIF *").performTextInput("J-123")
        compose.onNodeWithText("Guardar cliente").performScrollTo().performClick()
        compose.waitUntil(15000) {
            compose.onAllNodesWithText("Cliente guardado").fetchSemanticsNodes().isNotEmpty()
        }
        ready()
        compose.onNodeWithText("RIF: J-123").assertExists()
        compose.onNodeWithText("Editar cliente").performScrollTo().performClick()
        compose.onNodeWithText("Nombre *").performTextReplacement("Cliente actualizado")
        compose.onNodeWithText("Guardar cliente").performScrollTo().performClick()
        compose.waitUntil(15000) {
            compose.onAllNodesWithText("Cliente guardado").fetchSemanticsNodes().isNotEmpty()
        }
        ready()
        compose.onNodeWithText("Volver").performClick()
        compose.onNodeWithText("Cliente actualizado").assertExists()
        compose.onNodeWithText("1 clientes activos").assertExists()
    }
}
