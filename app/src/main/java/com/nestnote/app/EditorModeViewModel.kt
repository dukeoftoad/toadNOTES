package com.nestnote.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/** Editing is the default to preserve the existing editor entry behavior. */
enum class EditorMode {
    Locked,
    Editing
}

/** Holds lock/edit state for the lifetime of the editor destination. */
class EditorModeViewModel : ViewModel() {
    var mode by mutableStateOf(EditorMode.Editing)
        private set

    fun lock() {
        mode = EditorMode.Locked
    }

    fun edit() {
        mode = EditorMode.Editing
    }

    fun toggle() {
        mode = if (mode == EditorMode.Editing) EditorMode.Locked else EditorMode.Editing
    }
}
