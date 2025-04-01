package com.example.coursessupermarche.utils

import android.view.View
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Extension pour afficher facilement un Snackbar
 */
fun View.showSnackbar(
    message: String,
    duration: Int = Snackbar.LENGTH_SHORT,
    actionText: String? = null,
    action: (() -> Unit)? = null
) {
    val snackbar = Snackbar.make(this, message, duration)

    if (actionText != null && action != null) {
        snackbar.setAction(actionText) { action() }
    }

    snackbar.show()
}

/**
 * Extension pour collecter un Flow dans un Fragment
 */
fun <T> Fragment.collectFlow(flow: Flow<T>, collect: suspend (T) -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect { collect(it) }
        }
    }
}

/**
 * Extension permettant de lancer une coroutine liée au cycle de vie d'un Fragment
 */
fun Fragment.launchWhenStarted(block: suspend CoroutineScope.() -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            block()
        }
    }
}

/**
 * Extension pour vérifier si un EditText est vide
 */
fun EditText.isEmpty(): Boolean {
    return text.toString().trim().isEmpty()
}

/**
 * Extension pour obtenir la valeur d'un EditText
 */
fun EditText.textValue(): String {
    return text.toString().trim()
}

/**
 * Extension pour obtenir la valeur numérique d'un EditText
 */
fun EditText.intValue(defaultValue: Int = 0): Int {
    val value = text.toString().trim()
    return if (value.isEmpty()) defaultValue else value.toIntOrNull() ?: defaultValue
}