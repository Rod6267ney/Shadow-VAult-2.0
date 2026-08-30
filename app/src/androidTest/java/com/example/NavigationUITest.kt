package com.example

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.example.MainActivity

@RunWith(AndroidJUnit4::class)
class NavigationUITest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testBottomNavigation_canNavigateToAllTabs() {
        // Inicialmente deve estar na tela de Clones ou Início (depende do estado inicial)
        
        // 1. Clicar em "Início"
        composeTestRule.onNodeWithText("Início").performClick()
        composeTestRule.onNodeWithText("Painel de Controle").assertExists()

        // 2. Clicar em "Clones"
        composeTestRule.onNodeWithText("Clones").performClick()
        composeTestRule.onNodeWithText("Gerenciador de Clones").assertExists()

        // 3. Clicar em "Chaos OS"
        composeTestRule.onNodeWithText("Chaos OS").performClick()
        composeTestRule.onNodeWithText("Controle do Chaos OS").assertExists()
        
        // 4. Clicar em "Perfis"
        composeTestRule.onNodeWithText("Perfis").performClick()
        composeTestRule.onNodeWithText("Identidades Sintéticas").assertExists()
        
        // 5. Clicar em "Ajustes"
        composeTestRule.onNodeWithText("Ajustes").performClick()
        composeTestRule.onNodeWithText("Configurações").assertExists()
    }
}
