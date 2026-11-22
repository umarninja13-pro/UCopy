package com.umkt.ucopy.ui.markdown

import android.content.Context
import android.graphics.Rect
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.umkt.ucopy.R

internal interface ReaderSelectionHost {
    fun hasSelection(): Boolean
    fun selectedText(): CharSequence
    fun selectAll()
    fun selectParagraph()
    fun clearSelection()
    fun fillSelectionRect(outRect: Rect)
    fun onToolbarDestroyed()
}

internal class ReaderToolbarCallback(
    private val host: ReaderSelectionHost,
    private val context: Context
) : ActionMode.Callback2() {

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        menu.add(0, MENU_COPY, 0, R.string.copy)
        menu.add(0, MENU_SELECT_ALL, 1, R.string.select_all)
        menu.add(0, MENU_PARAGRAPH, 2, R.string.select_paragraph)
        menu.add(0, MENU_SHARE, 3, R.string.share)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        if (!host.hasSelection()) return false
        when (item.itemId) {
            MENU_COPY -> {
                ReaderTextActions.copy(context, host.selectedText())
                host.clearSelection()
            }
            MENU_SELECT_ALL -> host.selectAll()
            MENU_PARAGRAPH -> host.selectParagraph()
            MENU_SHARE -> {
                ReaderTextActions.share(context, host.selectedText().toString())
                host.clearSelection()
            }
            else -> return false
        }
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        host.onToolbarDestroyed()
    }

    override fun onGetContentRect(mode: ActionMode, view: View, outRect: Rect) {
        host.fillSelectionRect(outRect)
    }

    private companion object {
        const val MENU_COPY = 1
        const val MENU_SELECT_ALL = 2
        const val MENU_PARAGRAPH = 3
        const val MENU_SHARE = 4
    }
}
