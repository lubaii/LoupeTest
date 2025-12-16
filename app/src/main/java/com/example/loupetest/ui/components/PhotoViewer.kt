package com.example.loupetest.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.min

@Composable
fun PhotoViewerWithLoupe(
    photoUri: String?,
    onTakePhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    var targetPoint by remember { mutableStateOf<Offset?>(null) }
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val context = LocalContext.current
    val density = LocalDensity.current

    LaunchedEffect(photoUri) {
        if (photoUri != null) {
            try {
                val bitmap = BitmapFactory.decodeFile(photoUri)
                imageBitmap = bitmap.asImageBitmap()
                imageSize = IntSize(bitmap.width, bitmap.height)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                containerSize = coordinates.size
            }
    ) {
        if (imageBitmap != null) {
            // Основное изображение
            Image(
                bitmap = imageBitmap!!,
                contentDescription = "Captured photo",
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { tapOffset ->
                            targetPoint = tapOffset
                        }
                    },
                contentScale = ContentScale.Fit
            )

            // Прицел (перекрестие) и лупа
            targetPoint?.let { point ->
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    drawCrosshair(point)
                }

                // Вычисляем позицию лупы (размещаем справа-сверху от точки прицеливания)
                val loupeSizePx = with(density) { 150.dp.toPx() }
                val spacing = with(density) { 20.dp.toPx() }
                
                // Позиция справа-сверху от точки
                var offsetX = point.x + spacing
                var offsetY = point.y - loupeSizePx - spacing
                
                // Если не помещается справа, размещаем слева
                if (offsetX + loupeSizePx > containerSize.width) {
                    offsetX = point.x - loupeSizePx - spacing
                }
                
                // Если не помещается сверху, размещаем снизу
                if (offsetY < 0) {
                    offsetY = point.y + spacing
                }
                
                // Ограничиваем границами экрана
                offsetX = offsetX.coerceIn(0f, containerSize.width.toFloat() - loupeSizePx)
                offsetY = offsetY.coerceIn(0f, containerSize.height.toFloat() - loupeSizePx)

                // Лупа рядом с точкой прицеливания
                LoupeView(
                    imageBitmap = imageBitmap!!,
                    targetPoint = point,
                    imageSize = imageSize,
                    containerSize = containerSize,
                    modifier = Modifier
                        .size(150.dp)
                        .offset(
                            x = with(density) { offsetX.toDp() },
                            y = with(density) { offsetY.toDp() }
                        )
                )
            }
        } else {
            // Пустой экран
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Нажмите кнопку для съемки фото",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun LoupeView(
    imageBitmap: ImageBitmap,
    targetPoint: Offset,
    imageSize: IntSize,
    containerSize: IntSize,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White)
            .padding(4.dp)
    ) {
        val zoomFactor = 3f
        
        // Вычисляем масштаб изображения (ContentScale.Fit)
        val imageWidth = imageSize.width.toFloat()
        val imageHeight = imageSize.height.toFloat()
        val containerWidth = containerSize.width.toFloat()
        val containerHeight = containerSize.height.toFloat()
        
        val scaleX = containerWidth / imageWidth
        val scaleY = containerHeight / imageHeight
        val scale = min(scaleX, scaleY)
        
        // Вычисляем размер отображаемого изображения
        val displayedWidth = imageWidth * scale
        val displayedHeight = imageHeight * scale
        
        // Вычисляем смещение для центрирования
        val offsetX = (containerWidth - displayedWidth) / 2
        val offsetY = (containerHeight - displayedHeight) / 2
        
        // Преобразуем координаты касания в координаты изображения
        val imageX = ((targetPoint.x - offsetX) / scale).coerceIn(0f, imageWidth)
        val imageY = ((targetPoint.y - offsetY) / scale).coerceIn(0f, imageHeight)
        
        // Размер области для увеличения
        val cropSize = min(imageWidth, imageHeight) / zoomFactor
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Рисуем увеличенную область
            val left = (imageX - cropSize / 2).coerceIn(0f, imageWidth - cropSize)
            val top = (imageY - cropSize / 2).coerceIn(0f, imageHeight - cropSize)
            
            drawImage(
                image = imageBitmap,
                srcOffset = IntOffset(left.toInt(), top.toInt()),
                srcSize = IntSize(cropSize.toInt(), cropSize.toInt()),
                dstSize = IntSize(size.width.toInt(), size.height.toInt())
            )
        }
        
        // Обводка лупы
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.Black,
                radius = size.minDimension / 2,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
        }
    }
}

fun DrawScope.drawCrosshair(center: Offset) {
    val crosshairSize = 30.dp.toPx()
    val lineWidth = 2.dp.toPx()
    
    // Горизонтальная линия
    drawLine(
        color = Color.Red,
        start = Offset(center.x - crosshairSize / 2, center.y),
        end = Offset(center.x + crosshairSize / 2, center.y),
        strokeWidth = lineWidth
    )
    
    // Вертикальная линия
    drawLine(
        color = Color.Red,
        start = Offset(center.x, center.y - crosshairSize / 2),
        end = Offset(center.x, center.y + crosshairSize / 2),
        strokeWidth = lineWidth
    )
    
    // Центральная точка
    drawCircle(
        color = Color.Red,
        radius = 3.dp.toPx(),
        center = center
    )
}

