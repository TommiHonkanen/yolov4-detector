package io.github.tommihonkanen.yolov4detector

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.*
import io.github.tommihonkanen.yolov4detector.ui.screens.ImportModelDialog
import io.github.tommihonkanen.yolov4detector.ui.screens.ModelManagerScreen
import io.github.tommihonkanen.yolov4detector.ui.theme.YoloDetectorTheme
import java.io.File
import java.io.FileOutputStream

class ModelManagerActivity : ComponentActivity() {
    private lateinit var modelManager: ModelManager
    private lateinit var filePickerHelper: FilePickerHelper
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    private var models by mutableStateOf(listOf<ModelInfo>())
    private var selectedId by mutableStateOf("")
    private var showImportDialog by mutableStateOf(false)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        modelManager = ModelManager(this)
        filePickerHelper = FilePickerHelper(this)
        
        loadModels()
        
        // Handle back press with animation
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
                overridePendingTransition(R.anim.fade_in, R.anim.slide_out_down)
            }
        })
        
        setContent {
            YoloDetectorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ModelManagerScreen(
                        models = models,
                        selectedModelId = selectedId,
                        onBackClick = {
                            finish()
                            overridePendingTransition(R.anim.fade_in, R.anim.slide_out_down)
                        },
                        onModelSelect = { model ->
                            modelManager.setSelectedModel(model.id)
                            setResult(RESULT_OK)
                            loadModels()
                        },
                        onModelRename = { model ->
                            if (!model.isDefault) {
                                showRenameDialog(model)
                            }
                        },
                        onModelDelete = { model ->
                            if (model.isDefault) {
                                showMessage("Cannot delete default model")
                            } else {
                                confirmDelete(model)
                            }
                        },
                        onImportClick = {
                            showImportDialog = true
                        }
                    )
                    
                    if (showImportDialog) {
                        ImportModelDialog(
                            onDismiss = { showImportDialog = false },
                            onStartImport = {
                                showImportDialog = false
                                startImportProcess()
                            }
                        )
                    }
                }
            }
        }
    }
    
    private fun loadModels() {
        models = modelManager.getAllModels()
        selectedId = modelManager.getSelectedModelId()
    }
    
    private fun confirmDelete(model: ModelInfo) {
        AlertDialog.Builder(this)
            .setTitle("Delete Model")
            .setMessage("Are you sure you want to delete ${model.name}?")
            .setPositiveButton("Delete") { _, _ ->
                if (modelManager.removeModel(model.id)) {
                    loadModels()
                    showMessage("Model deleted")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showRenameDialog(model: ModelInfo) {
        val input = EditText(this)
        input.setText(model.name)
        
        AlertDialog.Builder(this)
            .setTitle("Rename Model")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    val updatedModel = model.copy(name = newName)
                    modelManager.addModel(updatedModel)
                    loadModels()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showMessage(message: String) {
        com.google.android.material.snackbar.Snackbar
            .make(findViewById(android.R.id.content), message, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
            .show()
    }
    
    private var importWeightsUri: Uri? = null
    private var importConfigUri: Uri? = null
    private var importNamesUri: Uri? = null
    private var filesSelected = 0
    
    private fun startImportProcess() {
        // Reset import state
        importWeightsUri = null
        importConfigUri = null
        importNamesUri = null
        filesSelected = 0
        
        selectNextFile()
    }
    
    private fun selectNextFile() {
        val remainingFiles = mutableListOf<String>()
        if (importWeightsUri == null) remainingFiles.add(".weights")
        if (importConfigUri == null) remainingFiles.add(".cfg")
        if (importNamesUri == null) remainingFiles.add(".names")
        
        if (remainingFiles.isEmpty()) {
            // All files selected, start import
            importNewModel(importWeightsUri!!, importConfigUri!!, importNamesUri!!)
            return
        }
        
        val fileTypes = remainingFiles.joinToString(" or ")
        showMessage("Select file ${filesSelected + 1} of 3 ($fileTypes)")
        
        filePickerHelper.pickFile("*/*") { uri ->
            val fileName = getFileName(uri)
            
            when {
                fileName.endsWith(".weights") && importWeightsUri == null -> {
                    importWeightsUri = uri
                    filesSelected++
                    showMessage("Weights file selected: $fileName")
                    selectNextFile()
                }
                fileName.endsWith(".cfg") && importConfigUri == null -> {
                    importConfigUri = uri
                    filesSelected++
                    showMessage("Config file selected: $fileName")
                    selectNextFile()
                }
                fileName.endsWith(".names") && importNamesUri == null -> {
                    importNamesUri = uri
                    filesSelected++
                    showMessage("Names file selected: $fileName")
                    selectNextFile()
                }
                else -> {
                    val alreadySelected = when {
                        fileName.endsWith(".weights") && importWeightsUri != null -> "weights"
                        fileName.endsWith(".cfg") && importConfigUri != null -> "config"
                        fileName.endsWith(".names") && importNamesUri != null -> "names"
                        else -> null
                    }
                    
                    if (alreadySelected != null) {
                        showMessage("$alreadySelected file already selected. Select a different file type.")
                    } else {
                        showMessage("Invalid file. Please select a $fileTypes file")
                    }
                    selectNextFile()
                }
            }
        }
    }
    
    private fun getFileName(uri: Uri): String {
        return contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(nameIndex)
        } ?: uri.lastPathSegment ?: "unknown"
    }
    
    private fun importNewModel(weightsUri: Uri, configUri: Uri, namesUri: Uri) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val modelName = "Custom Model ${modelManager.getAllModels().size}"
                val modelId = "model_${System.currentTimeMillis()}"
                val modelDir = File(filesDir, "models/$modelId")
                modelDir.mkdirs()
                
                // Copy files
                val weightsFile = File(modelDir, "model.weights")
                val configFile = File(modelDir, "model.cfg")
                val namesFile = File(modelDir, "model.names")
                
                // Copy and validate weights file
                contentResolver.openInputStream(weightsUri)?.use { input ->
                    FileOutputStream(weightsFile).use { output ->
                        input.copyTo(output)
                    }
                }
                
                // Validate weights file size (should be > 1MB for a real model)
                if (weightsFile.length() < 1024 * 1024) {
                    weightsFile.parentFile?.deleteRecursively()
                    throw Exception("Weights file is too small. Please select a valid YOLO weights file.")
                }
                
                // Copy and validate config file
                contentResolver.openInputStream(configUri)?.use { input ->
                    FileOutputStream(configFile).use { output ->
                        input.copyTo(output)
                    }
                }
                
                // Validate config file contains YOLO configuration
                val cfgContent = configFile.readText()
                if (!cfgContent.contains("[net]") || !cfgContent.contains("[yolo]")) {
                    weightsFile.parentFile?.deleteRecursively()
                    throw Exception("Invalid config file. Please select a valid YOLO .cfg file.")
                }
                
                // Copy and validate names file
                contentResolver.openInputStream(namesUri)?.use { input ->
                    FileOutputStream(namesFile).use { output ->
                        input.copyTo(output)
                    }
                }
                
                // Validate names file contains class names
                val namesLines = namesFile.readLines().filter { it.isNotBlank() }
                if (namesLines.isEmpty()) {
                    weightsFile.parentFile?.deleteRecursively()
                    throw Exception("Names file is empty. Please select a valid .names file with class labels.")
                }
                
                // Parse config for input size - fail if not found
                val inputSize = try {
                    val cfgContent = configFile.readText()
                    val widthMatch = Regex("width=(\\d+)").find(cfgContent)
                    val heightMatch = Regex("height=(\\d+)").find(cfgContent)
                    
                    val width = widthMatch?.groupValues?.get(1)?.toInt()
                    val height = heightMatch?.groupValues?.get(1)?.toInt()
                    
                    if (width == null || height == null) {
                        throw Exception("Network size not found in config file. The .cfg file must contain width and height parameters.")
                    }
                    
                    if (width != height) {
                        throw Exception("Network width ($width) and height ($height) must be equal.")
                    }
                    
                    width
                } catch (e: Exception) {
                    weightsFile.parentFile?.deleteRecursively()
                    throw e
                }
                
                val numClasses = try {
                    namesFile.readLines().size
                } catch (e: Exception) {
                    80
                }
                
                val model = ModelInfo(
                    id = modelId,
                    name = modelName,
                    weightsPath = weightsFile.absolutePath,
                    configPath = configFile.absolutePath,
                    namesPath = namesFile.absolutePath,
                    isDefault = false,
                    inputSize = inputSize,
                    numClasses = numClasses
                )
                
                modelManager.addModel(model)
                
                withContext(Dispatchers.Main) {
                    loadModels()
                    showMessage("Model imported successfully")
                    
                    // Rename the model immediately
                    showRenameDialog(model)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showMessage("Import failed: ${e.message}")
                }
            }
        }
    }
    
    
    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}