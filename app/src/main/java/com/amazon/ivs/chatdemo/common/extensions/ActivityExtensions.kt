package com.amazon.ivs.chatdemo.common.extensions

import android.content.DialogInterface
import android.content.res.ColorStateList
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import com.amazon.ivs.chatdemo.R
import com.amazon.ivs.chatdemo.common.IVS_PLAYBACK_URL_BASE
import com.amazon.ivs.chatdemo.databinding.DialogInputBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

fun AppCompatActivity.showInputDialog(
    inputText: String?,
    onSave: (value: String?) -> Unit,
    onCancel: () -> Unit
) {
    val binding = DialogInputBinding.inflate(layoutInflater)
    val dialog = MaterialAlertDialogBuilder(this, R.style.AlertDialog)
        .setView(binding.root)
        .setTitle(title)
        .setCancelable(false)
        .setPositiveButton(resources.getString(R.string.save)) { dialog, _ ->
            dialog.dismiss()
            onSave(binding.input.text.toString().takeIf { it.isNotBlank() })
        }
        .setNegativeButton(resources.getString(R.string.cancel)) { dialog, _ ->
            dialog.dismiss()
            onCancel()
        }.show()

    fun showError(text: String?) {
        if (text?.isNotBlank() == true && !text.contains(IVS_PLAYBACK_URL_BASE)) {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = false
            binding.input.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.red))
            binding.error.visibility = View.VISIBLE
        } else {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = true
            binding.input.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.yellow))
            binding.error.visibility = View.INVISIBLE
        }
    }

    binding.input.setText(inputText)
    binding.input.requestFocusFromTouch()
    binding.input.doOnTextChanged { text, _, _, _ ->
        showError(text?.toString())
    }
    showError(inputText)

    dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
}
