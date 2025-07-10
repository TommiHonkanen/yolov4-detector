package io.github.tommihonkanen.yolov4detector.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.tommihonkanen.yolov4detector.ModelInfo
import io.github.tommihonkanen.yolov4detector.R
import io.github.tommihonkanen.yolov4detector.ui.components.ModelCard
import io.github.tommihonkanen.yolov4detector.ui.components.SimpleTopBar
import io.github.tommihonkanen.yolov4detector.ui.theme.*

@Composable
fun ModelManagerScreen(
    models: List<ModelInfo>,
    selectedModelId: String,
    onBackClick: () -> Unit,
    onModelSelect: (ModelInfo) -> Unit,
    onModelRename: (ModelInfo) -> Unit,
    onModelDelete: (ModelInfo) -> Unit,
    onImportClick: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var modelToDelete by remember { mutableStateOf<ModelInfo?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var modelToRename by remember { mutableStateOf<ModelInfo?>(null) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            SimpleTopBar(
                title = "Model Manager",
                onBackClick = onBackClick
            )
            
            if (models.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = models,
                        key = { it.id }
                    ) { model ->
                        ModelCard(
                            model = model,
                            isSelected = model.id == selectedModelId,
                            onSelect = { onModelSelect(model) },
                            onRename = {
                                modelToRename = model
                                showRenameDialog = true
                            },
                            onDelete = {
                                modelToDelete = model
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
        
        // FAB
        ExtendedFloatingActionButton(
            onClick = onImportClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp),
            containerColor = md_theme_primary,
            contentColor = md_theme_onPrimary
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_add),
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Import Model")
        }
    }
    
    // Delete Dialog
    if (showDeleteDialog && modelToDelete != null) {
        DeleteModelDialog(
            modelName = modelToDelete!!.name,
            onConfirm = {
                onModelDelete(modelToDelete!!)
                showDeleteDialog = false
                modelToDelete = null
            },
            onDismiss = {
                showDeleteDialog = false
                modelToDelete = null
            }
        )
    }
    
    // Rename Dialog
    if (showRenameDialog && modelToRename != null) {
        RenameModelDialog(
            currentName = modelToRename!!.name,
            onConfirm = { newName ->
                onModelRename(modelToRename!!.copy(name = newName))
                showRenameDialog = false
                modelToRename = null
            },
            onDismiss = {
                showRenameDialog = false
                modelToRename = null
            }
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.size(120.dp),
            shape = RoundedCornerShape(60.dp),
            colors = CardDefaults.cardColors(
                containerColor = md_theme_surfaceContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_model_empty),
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .alpha(0.6f),
                    tint = md_theme_onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "No custom models",
            style = MaterialTheme.typography.headlineMedium,
            color = md_theme_onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Import YOLO models to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = md_theme_onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DeleteModelDialog(
    modelName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Delete Model")
        },
        text = {
            Text(text = "Are you sure you want to delete $modelName?")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text("Delete", color = md_theme_error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun RenameModelDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Rename Model")
        },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newName.isNotBlank()) {
                        onConfirm(newName.trim())
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ImportModelDialog(
    onDismiss: () -> Unit,
    onStartImport: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Import Model")
        },
        text = {
            Text(
                text = "To import a model, you need:\n" +
                        "• .weights file (model weights)\n" +
                        "• .cfg file (configuration)\n" +
                        "• .names file (class names)\n\n" +
                        "You'll be prompted to select each file. They can be selected in any order."
            )
        },
        confirmButton = {
            TextButton(
                onClick = onStartImport
            ) {
                Text("Start Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}