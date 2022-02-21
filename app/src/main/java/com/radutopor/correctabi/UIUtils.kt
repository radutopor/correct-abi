package com.radutopor.correctabi

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveDataScope
import androidx.lifecycle.liveData
import kotlinx.coroutines.Dispatchers

object UIUtils {

    fun AppCompatActivity.async(block: suspend LiveDataScope<Unit>.() -> Unit) =
        liveData(Dispatchers.IO, block = block).observe(this) {}

    fun View.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(windowToken, 0)
    }
}
