package com.lanzdev

import java.util.*

class Life(var width: Int, var height: Int) {

    private val random = Random()
    private var field: MutableList<MutableList<CellState>> = MutableList(width, { _ -> MutableList(height, { CellState.EMPTY }) })

    fun place(x: Int, y: Int, state: CellState) {
        try {
            field[x][y] = state
        } catch (e: Exception) {
            println("${field.indices.last}:${field[0].indices.last} --- $x:$y")
        }
    }

    fun getCell(x: Int, y: Int) =
            if (x < 0 || x >= width) CellState.EMPTY
            else if (y < 0 || y >= height) CellState.EMPTY
            else field[x][y]

    fun resize(width: Int, height: Int) {
        val fieldWidth = field.indices.last
        val fieldHeight = field[0].indices.last
        if (width > fieldWidth && height > fieldHeight) {
            increaseList(width, height)
        } else if (width > fieldWidth && height <= fieldHeight) {
            increaseList(width, fieldHeight)
        } else if (width <= fieldWidth && height > fieldHeight) {
            increaseList(fieldWidth, height)
        }
    }

    private fun increaseList(width: Int, height: Int) {
        field.indices.forEach { x ->
            (field[x].size..height).forEach { field[x].add(CellState.EMPTY) }
        }
        (field.size..width).forEach { field.add(MutableList(height, { CellState.EMPTY })) }
        this.width = width
        this.height = height
    }

    fun getField() = field.toMutableList()

    fun populateRandomly() {
        val randomCellsAmount = width * height / 2
        (0..randomCellsAmount).forEach {
            var count = 0
            do {
                val width = random.nextInt(this.width)
                val height = random.nextInt(this.height)
                if (field[width][height] == CellState.ALIVE) {
                    count++
                    continue
                }
                field[width][height] = CellState.ALIVE
                break
            } while (count++ < 10)
        }
    }

    fun nextGeneration() {
        markHatchedAndDead()
        setHatchedRemoveDead()
    }

    private fun markHatchedAndDead() {
        field.indices.forEach { x ->
            field[x].indices.forEach { y ->
                val countAround = countAround(x, y)
                defineNextGenerationForCell(x, y, countAround)
            }
        }
    }

    private fun countAround(x: Int, y: Int): Int {
        return IntRange(-1, 1).flatMap { dx -> IntRange(-1, 1).map { dy -> dx to dy } }
                .filter { (it.first != 0 || it.second != 0) }
                .filter { getCell(x + it.first, y + it.second).visible }
                .count()
    }

    private fun defineNextGenerationForCell(x: Int, y: Int, countAround: Int) {
        if (field[x][y] == CellState.ALIVE) {
            if (countAround <= 1 || countAround >= 4) {
                field[x][y] = CellState.DEAD
            }
        } else if (countAround == 3) {
            field[x][y] = CellState.HATCHED
        }
    }

    private fun setHatchedRemoveDead() {
        field.indices.forEach { x ->
            field[x].indices.forEach { y ->
                if (field[x][y] == CellState.HATCHED) field[x][y] = CellState.ALIVE
                else if (field[x][y] == CellState.DEAD) field[x][y] = CellState.EMPTY
            }
        }
    }
}

