package com.example.personalledger

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
internal class SheetExit(private val state: SheetState, private val scope: CoroutineScope) {
    var closing by mutableStateOf(false)
        private set

    fun close(after: () -> Unit) {
        if (closing) return
        closing = true
        scope.launch {
            try {
                state.hide()
                if (!state.isVisible) after()
            } finally {
                closing = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun rememberSheetExit(state: SheetState): SheetExit {
    val scope = rememberCoroutineScope()
    return remember(state, scope) { SheetExit(state, scope) }
}
