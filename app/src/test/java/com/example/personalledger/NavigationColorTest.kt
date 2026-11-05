package com.example.personalledger

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationColorTest {
    @Test fun selectedAndUnselectedColors() {
        assertEquals(Color(0xFF292929), navigationColor(true))
        assertEquals(Color(0xFF888888), navigationColor(false))
    }
}
