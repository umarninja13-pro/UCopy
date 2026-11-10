package com.umkt.ucopy.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UndoRedoHelper(initialText: String) {
    private val _text = MutableStateFlow(initialText)
    val text: StateFlow<String> = _text.asStateFlow()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val history = mutableListOf<String>()
    private var currentIndex = -1
    private var isUndoRedoInProgress = false
    private val maxHistory = 50

    init {
        history.add(initialText)
        currentIndex = 0
        updateState()
    }

    fun setText(newText: String) {
        if (isUndoRedoInProgress) return
        if (newText == _text.value) return

        if (currentIndex < history.lastIndex) {
            history.subList(currentIndex + 1, history.size).clear()
        }
        history.add(newText)
        if (history.size > maxHistory) {
            history.removeAt(0)
            currentIndex--
        }
        currentIndex++
        _text.value = newText
        updateState()
    }

    fun undo(): Boolean {
        if (currentIndex <= 0) return false
        isUndoRedoInProgress = true
        currentIndex--
        _text.value = history[currentIndex]
        isUndoRedoInProgress = false
        updateState()
        return true
    }

    fun redo(): Boolean {
        if (currentIndex >= history.lastIndex) return false
        isUndoRedoInProgress = true
        currentIndex++
        _text.value = history[currentIndex]
        isUndoRedoInProgress = false
        updateState()
        return true
    }

    fun reset(newText: String) {
        history.clear()
        history.add(newText)
        currentIndex = 0
        _text.value = newText
        updateState()
    }

    private fun updateState() {
        _canUndo.value = currentIndex > 0
        _canRedo.value = currentIndex < history.lastIndex
    }
}