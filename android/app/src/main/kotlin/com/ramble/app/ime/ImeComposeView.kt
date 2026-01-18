package com.ramble.app.ime

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.compositionContext
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Custom ComposeView for InputMethodService that properly handles lifecycle.
 *
 * The standard ComposeView looks up the view tree for a LifecycleOwner, but InputMethodService's
 * parent views don't have one. This class sets up its own composition context before being attached
 * to the window.
 */
class ImeComposeView(context: Context, private val service: RambleKeyboardService) :
  AbstractComposeView(context) {

  private var content: @Composable () -> Unit = {}
  private var pendingContent = false

  private val coroutineScope = CoroutineScope(AndroidUiDispatcher.CurrentThread)
  private val recomposer = Recomposer(coroutineScope.coroutineContext)

  init {
    // Set view tree owners before any attachment happens
    setViewTreeLifecycleOwner(service)
    setViewTreeSavedStateRegistryOwner(service)
    setViewTreeViewModelStoreOwner(service)

    // Set our own composition context to avoid looking up the tree
    this.compositionContext = recomposer

    // Start the recomposer
    coroutineScope.launch { recomposer.runRecomposeAndApplyChanges() }
  }

  fun setContent(content: @Composable () -> Unit) {
    this.content = content
    pendingContent = true
    // Don't call createComposition here - wait until attached
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    if (pendingContent) {
      createComposition()
      pendingContent = false
    }
  }

  @Composable
  override fun Content() {
    content()
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    recomposer.cancel()
  }
}
