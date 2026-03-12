package com.ramble.app.overlay

data class InsertionResult(
    val text: String,
    val cursorPosition: Int
)

data class ProvisionalState(
    val text: String,
    val position: Int,
    val length: Int
)

object TextInsertion {

    fun insertTextAtCursor(
        currentText: String,
        cursorStart: Int,
        cursorEnd: Int,
        newText: String
    ): InsertionResult {
        val safeStart = cursorStart.coerceIn(0, currentText.length)
        val safeEnd = cursorEnd.coerceIn(safeStart, currentText.length)
        val result = currentText.substring(0, safeStart) + newText + currentText.substring(safeEnd)
        return InsertionResult(result, safeStart + newText.length)
    }

    fun removeProvisional(
        currentText: String,
        provisional: ProvisionalState?
    ): InsertionResult {
        if (provisional == null) {
            return InsertionResult(currentText, currentText.length)
        }

        val start = provisional.position
        val end = start + provisional.length

        if (start > currentText.length || end > currentText.length) {
            return InsertionResult(currentText, currentText.length)
        }

        val actualText = currentText.substring(start, end)
        if (actualText != provisional.text) {
            return InsertionResult(currentText, currentText.length)
        }

        val result = currentText.substring(0, start) + currentText.substring(end)
        return InsertionResult(result, start)
    }

    fun applyFinalText(
        currentText: String,
        cursorPosition: Int,
        provisional: ProvisionalState?,
        finalText: String
    ): Pair<InsertionResult, ProvisionalState?> {
        val (textAfterRemoval, posAfterRemoval) = removeProvisional(currentText, provisional)
        val insertPos = if (provisional != null) posAfterRemoval else cursorPosition.coerceIn(0, textAfterRemoval.length)
        val result = insertTextAtCursor(textAfterRemoval, insertPos, insertPos, finalText)
        return Pair(result, null)
    }

    fun applyProvisionalText(
        currentText: String,
        cursorPosition: Int,
        provisional: ProvisionalState?,
        provisionalText: String
    ): Pair<InsertionResult, ProvisionalState?> {
        val (textAfterRemoval, posAfterRemoval) = removeProvisional(currentText, provisional)
        val insertPos = if (provisional != null) posAfterRemoval else cursorPosition.coerceIn(0, textAfterRemoval.length)
        val result = insertTextAtCursor(textAfterRemoval, insertPos, insertPos, provisionalText)
        val newProvisional = if (provisionalText.isNotEmpty()) {
            ProvisionalState(provisionalText, insertPos, provisionalText.length)
        } else {
            null
        }
        return Pair(result, newProvisional)
    }
}
