package io.github.tommihonkanen.yolov4detector

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

class FilePickerHelper(private val activity: ComponentActivity) {
    private var filePickerLauncher: ActivityResultLauncher<String>? = null
    private var currentCallback: ((Uri) -> Unit)? = null
    
    init {
        filePickerLauncher = activity.registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                currentCallback?.invoke(it)
            }
        }
    }
    
    fun pickFile(mimeType: String, onFilePicked: (Uri) -> Unit) {
        currentCallback = onFilePicked
        filePickerLauncher?.launch(mimeType)
    }
}