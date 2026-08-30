package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.UiDevice
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.example.MainActivity

@RunWith(AndroidJUnit4::class)
class ChaosOsUITest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        // Initialize UiDevice instance
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @Test
    fun testCreateChaosOsInstance() {
        // 1. Navegar até a aba Chaos OS
        composeTestRule.onNodeWithText("Chaos OS").performClick()
        composeTestRule.onNodeWithText("Controle do Chaos OS").assertIsDisplayed()

        // 2. Clicar no botão para criar nova VM
        composeTestRule.onNodeWithText("Novo Celular Virtual").performClick()
        
        // 3. Preencher Wizard - Passo 1 (Identificação)
        composeTestRule.onNodeWithText("Nome da Instância").performTextInput("Teste Automático VM")
        composeTestRule.onNodeWithText("Avançar").performClick()
        
        // 4. Preencher Wizard - Passo 2 (Sistema Operacional)
        composeTestRule.onNodeWithText("Android 10 (Q)").performClick()
        composeTestRule.onNodeWithText("Avançar").performClick()
        
        // 5. Preencher Wizard - Passo 3 (Memória e Armazenamento)
        composeTestRule.onNodeWithText("Criar VM").performClick()
        
        // 6. Verificar se a tela volta para o controle com o item criado
        // Pode demorar um pouco devido a transições e criação no banco
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                composeTestRule.onNodeWithText("Teste Automático VM").assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        
        composeTestRule.onNodeWithText("Teste Automático VM").assertIsDisplayed()
    }
}
