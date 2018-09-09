package com.lanzdev

import javafx.scene.canvas.Canvas

class ResizableCanvas(private val cellSize: Int, width: Double, height: Double) : Canvas(width, height) {

    override fun isResizable(): Boolean {
        return true
    }

    override fun prefWidth(height: Double): Double {
        return width
    }

    override fun prefHeight(width: Double): Double {
        return height
    }
}