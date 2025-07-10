package io.github.tommihonkanen.yolov4detector.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.tommihonkanen.yolov4detector.ModelInfo
import io.github.tommihonkanen.yolov4detector.R
import io.github.tommihonkanen.yolov4detector.ui.theme.*

@Composable
fun ModelCard(
    model: ModelInfo,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(ModelCardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) md_theme_primary else md_theme_surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSelected) md_theme_onPrimary else md_theme_onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    if (model.isDefault) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = md_theme_tertiary,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = "DEFAULT",
                                style = MaterialTheme.typography.labelSmall,
                                color = md_theme_onTertiary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "${model.inputSize}x${model.inputSize} • ${model.numClasses} classes",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) md_theme_onPrimary else md_theme_onSurfaceVariant
                )
            }
            
            if (!model.isDefault) {
                IconButton(
                    onClick = onRename
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_edit),
                        contentDescription = "Rename",
                        tint = if (isSelected) md_theme_onPrimary else md_theme_onSurfaceVariant
                    )
                }
                
                IconButton(
                    onClick = onDelete
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_delete),
                        contentDescription = "Delete",
                        tint = md_theme_error
                    )
                }
            }
        }
    }
}