@file:JvmName("Drawer")

package com.lanzdev

import javafx.animation.FadeTransition
import javafx.event.EventHandler
import javafx.scene.canvas.Canvas
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight
import javafx.scene.text.Text

class Drawer(private val canvas: Canvas) {

    fun drawCell(x: Double, y: Double, cellSize: Double, color: Color, strokeColor: Color, isRect: Boolean) {
        val gc = canvas.graphicsContext2D
        gc.fill = color
        if (isRect) {
            gc.fillRect(x - x % cellSize, y - y % cellSize, cellSize, cellSize)
        } else {
            gc.fillOval(x - x % cellSize, y - y % cellSize, cellSize, cellSize)
        }
        gc.stroke = strokeColor
        gc.strokeRect(x - x % cellSize, y - y % cellSize,
                cellSize, cellSize)
    }

    fun drawGrid(gridStep: Int, strokeColor: Color) {
        val gc = canvas.graphicsContext2D
        gc.stroke = strokeColor
        gc.lineWidth = .1
        (0..canvas.width.toInt() step gridStep).forEach { gc.strokeLine(it.toDouble(), 0.0, it.toDouble(), canvas.height) }
        (0..canvas.height.toInt() step gridStep).forEach { gc.strokeLine(0.0, it.toDouble(), canvas.width, it.toDouble()) }
    }

    fun clearCanvas(backgroundColor: Color) {
        val gc = canvas.graphicsContext2D
        gc.fill = backgroundColor
        gc.fillRect(0.0, 0.0, canvas.width, canvas.height)
    }

    fun updateTextElement(textElement: Text, text: String, textColor: Color) {
        textElement.text = text
        textElement.fill = textColor
        textElement.font = Font.font("verdana", FontWeight.BOLD, FontPosture.REGULAR, 40.0)
    }

    fun updateFadeTransition(ft: FadeTransition, cycles: Int, infinite: Boolean) {
        ft.fromValue = 0.0
        ft.toValue = 1.0
        ft.isAutoReverse = true
        ft.cycleCount = cycles
        ft.play()
        ft.onFinished = if (infinite) EventHandler { ft.play() } else null
    }
}