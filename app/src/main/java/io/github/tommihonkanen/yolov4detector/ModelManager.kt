package io.github.tommihonkanen.yolov4detector

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream

data class ModelInfo(
    val id: String,
    val name: String,
    val weightsPath: String,
    val configPath: String,
    val namesPath: String,
    val isDefault: Boolean = false,
    val inputSize: Int = 416,
    val numClasses: Int = 80
)

class ModelManager(private val context: Context) {
    private val gson = Gson()
    private val prefsName = "model_prefs"
    private val modelsKey = "models_list"
    private val selectedModelKey = "selected_model"
    
    companion object {
        private const val TAG = "ModelManager"
        const val DEFAULT_MODEL_ID = "yolov4-tiny-coco"
        const val DEFAULT_MODEL_NAME = "YOLOv4-Tiny (COCO)"
    }
    
    init {
        ensureDefaultModel()
    }
    
    private fun ensureDefaultModel() {
        val models = getAllModels()
        if (models.none { it.id == DEFAULT_MODEL_ID }) {
            // Copy default model from assets to internal storage
            copyDefaultModelFromAssets()
        }
    }
    
    private fun copyDefaultModelFromAssets() {
        try {
            val modelDir = File(context.filesDir, "models/$DEFAULT_MODEL_ID")
            modelDir.mkdirs()
            
            // Copy each file from assets
            val assetFiles = mapOf(
                "models/yolov4-tiny-coco/yolov4-tiny.weights" to "yolov4-tiny.weights",
                "models/yolov4-tiny-coco/yolov4-tiny.cfg" to "yolov4-tiny.cfg",
                "models/yolov4-tiny-coco/coco.names" to "coco.names"
            )
            
            assetFiles.forEach { (assetPath, fileName) ->
                val outputFile = File(modelDir, fileName)
                context.assets.open(assetPath).use { input ->
                    FileOutputStream(outputFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            
            // Add to models list
            val defaultModel = ModelInfo(
                id = DEFAULT_MODEL_ID,
                name = DEFAULT_MODEL_NAME,
                weightsPath = File(modelDir, "yolov4-tiny.weights").absolutePath,
                configPath = File(modelDir, "yolov4-tiny.cfg").absolutePath,
                namesPath = File(modelDir, "coco.names").absolutePath,
                isDefault = true,
                inputSize = 416,
                numClasses = 80
            )
            
            addModel(defaultModel)
            setSelectedModel(DEFAULT_MODEL_ID)
            
            Log.d(TAG, "Default model copied successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error copying default model", e)
        }
    }
    
    fun getAllModels(): List<ModelInfo> {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val json = prefs.getString(modelsKey, "[]")
        val type = object : TypeToken<List<ModelInfo>>() {}.type
        return gson.fromJson(json, type)
    }
    
    fun addModel(model: ModelInfo) {
        val models = getAllModels().toMutableList()
        models.removeAll { it.id == model.id } // Remove if exists
        models.add(model)
        saveModels(models)
    }
    
    fun removeModel(modelId: String): Boolean {
        val models = getAllModels().toMutableList()
        val model = models.find { it.id == modelId }
        
        if (model?.isDefault == true) {
            return false // Can't remove default model
        }
        
        val removed = models.removeAll { it.id == modelId }
        if (removed) {
            saveModels(models)
            
            // Delete model files
            val modelDir = File(context.filesDir, "models/$modelId")
            modelDir.deleteRecursively()
            
            // If this was selected, switch to default
            if (getSelectedModelId() == modelId) {
                setSelectedModel(DEFAULT_MODEL_ID)
            }
        }
        
        return removed
    }
    
    fun getSelectedModel(): ModelInfo? {
        val selectedId = getSelectedModelId()
        return getAllModels().find { it.id == selectedId }
    }
    
    fun getSelectedModelId(): String {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        return prefs.getString(selectedModelKey, DEFAULT_MODEL_ID) ?: DEFAULT_MODEL_ID
    }
    
    fun setSelectedModel(modelId: String) {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefs.edit().putString(selectedModelKey, modelId).apply()
    }
    
    private fun saveModels(models: List<ModelInfo>) {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val json = gson.toJson(models)
        prefs.edit().putString(modelsKey, json).apply()
    }
    
    // This method is deprecated and no longer used
    // Model import is now handled in ModelManagerActivity
    @Deprecated("Use ModelManagerActivity for model import", level = DeprecationLevel.ERROR)
    fun importModel(name: String, weightsUri: String, configUri: String, namesUri: String): String {
        throw UnsupportedOperationException("Model import is now handled in ModelManagerActivity")
    }
}