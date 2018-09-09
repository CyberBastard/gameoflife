package com.lanzdev

import javafx.animation.FadeTransition
import javafx.application.Application
import javafx.application.Application.launch
import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleLongProperty
import javafx.beans.value.ChangeListener
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseEvent
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.text.Text
import javafx.stage.Stage
import javafx.util.Duration
import java.lang.reflect.Modifier
import java.util.*
import java.util.stream.Collectors

class GameOfLife : Application() {

    private val pressedQ: SimpleBooleanProperty = SimpleBooleanProperty(false).also {
        it.addListener(ChangeListener { _, _, newValue ->
            if (!newValue) return@ChangeListener
            if (pressedQAndShift.get()) return@ChangeListener
            if (currentColorIndex + 1 >= currentColorIndex) {
                colorsHistory.add(random.nextInt(colors.size))
                cellColor = colors[colorsHistory[++currentColorIndex]]
            } else if (currentColorIndex + 1 < colorsHistory.size) {
                cellColor = colors[colorsHistory[currentColorIndex++]]
            }
            Platform.runLater({ redraw() })
        })
    }
    private val pressedShift = SimpleBooleanProperty(false)
    private val pressedQAndShift = pressedQ.and(pressedShift).also {
        it.addListener(ChangeListener { _, _, newValue ->
            if (!newValue) return@ChangeListener
            if (currentColorIndex - 1 >= 0) {
                cellColor = colors[colorsHistory[--currentColorIndex]]
            }
            Platform.runLater({ redraw() })
        })
    }
    private val pressedCtrl = SimpleBooleanProperty(false)
    private val pressedL = SimpleBooleanProperty(false)
    private val pressedCtrlAndL = pressedCtrl.and(pressedL).also {
        it.addListener { _, _, newValue ->
            if (newValue) {
                killAll()
                Platform.runLater({ redraw() })
            }
        }
    }
    private val cellSize = SimpleIntegerProperty(10).also {
        it.addListener { _, oldValue, newValue ->
            run {
                if (xDelta + canvas.width > life.width * newValue.toInt())
                    xDelta = if (life.width * newValue.toInt() - canvas.width > 0) life.width * newValue.toInt() - canvas.width else 0.0
                if (yDelta + canvas.height > life.height * newValue.toInt())
                    yDelta = if (life.height * newValue.toInt() - canvas.height > 0) life.height * newValue.toInt() - canvas.height else 0.0
                if (newValue.toInt() > 30) it.set(oldValue.toInt())
                if (newValue.toInt() < 1) it.set(oldValue.toInt())
            }
        }
    }
    private var interval = SimpleLongProperty(20L).also {
        it.addListener { _, oldValue, newValue ->
            if (newValue.toLong() - intervalFactor < 10) it.set(oldValue.toLong())
        }
    }

    private val random = Random()
    private val randomizingRadius = 5
    private val randomizingFactor = 1 // percentage of cells to be filled
    private val intervalFactor = 10L
    private val cellFactor = 1

    private val textFadeDuration = 300.0
    private val notificationTextElement = Text()
    private val ftNotification = FadeTransition(Duration.millis(textFadeDuration), notificationTextElement)
    private val stackPane = StackPane()
    private val canvas: Canvas
    private val scene: Scene
    private val life: Life
    private val drawer: Drawer

    private val backgroundColor = Color.BLACK
    private val strokeColor = Color.WHITE
    private val textColor = Color.DEEPPINK
    private val colorsHistory = mutableListOf<Int>()
    private val colors: List<Color>

    private var timer: Timer? = null
    private var onPause = true
    private var rectangularCell = false
    private var lastClickedCell = CellState.EMPTY
    private var cellColor: Color
    private var currentColorIndex = -1
    private lateinit var mousePositionAfterClick: Pair<Double, Double>

    private var xDelta = 0.0 // values from 0.0 to life.width * cellSize - canvas.width
    private var yDelta = 0.0 // values from 0.0 to life.height * cellSize - canvas.height

    init {
        val width = 500.0
        val height = 500.0

        canvas = ResizableCanvas(cellSize.get(), width, height)
        scene = Scene(stackPane, width, height)
        life = Life((width / cellSize.get()).toInt(), (height / cellSize.get()).toInt())
        life.populateRandomly()
        drawer = Drawer(canvas)

        colors = Arrays.stream(Color::class.java.declaredFields)
                .filter { Modifier.isStatic(it.modifiers) }
                .filter { it.type == Color::class.java }
                .map { it.get(null) }
                .map { Color::class.java.cast(it) }
                .collect(Collectors.toList())

        colorsHistory.add(colors.indexOf(Color.AQUAMARINE))
        cellColor = colors[colorsHistory[++currentColorIndex]]
    }

    override fun start(primaryStage: Stage) {
        prepareAndStart(primaryStage)
    }

    private fun prepareAndStart(primaryStage: Stage) {
        initCanvas()
        initScene()
        initStackPane()
        initStage(primaryStage)
        Platform.runLater({
            redraw()
        })
        updateDrawingTask()
    }

    private fun initCanvas() {
        canvas.widthProperty().bind(stackPane.widthProperty())
        canvas.heightProperty().bind(stackPane.heightProperty())
        canvas.widthProperty().addListener({ _, _, newValue ->
            run {
                life.resize(newValue.toInt() / cellSize.get(), (canvas.height / cellSize.get()).toInt())
                Platform.runLater({ redraw() })
            }
        })
        canvas.heightProperty().addListener({ _, _, newValue ->
            run {
                life.resize((canvas.width / cellSize.get()).toInt(), newValue.toInt() / cellSize.get())
                Platform.runLater({ redraw() })
            }
        })
    }

    private fun initScene() {
        val onClickEventHandler: (MouseEvent) -> Unit = { event ->
            if (isMouseWithinWindow(event) && pressedCtrl.get()) {
                defineDeltaCoordinates(event)
                Platform.runLater({ redraw() })
            } else if (isMouseWithinWindow(event) && pressedShift.get()) {
                Platform.runLater({ onCellClickActionWithSplash(event.x, event.y) })
            } else if (isMouseWithinWindow(event)) {
                Platform.runLater({ onCellClickAction(event.x, event.y) })
            }
        }
        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, onClickEventHandler)
        scene.addEventFilter(MouseEvent.MOUSE_CLICKED, onClickEventHandler)
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, { event ->
            if (isMouseWithinWindow(event)) {
                lastClickedCell = life.getCell(((event.x + xDelta) / cellSize.get()).toInt(), ((event.y + yDelta) / cellSize.get()).toInt())
                mousePositionAfterClick = event.x + xDelta to event.y + yDelta
            }
        })
        scene.setOnKeyPressed {
            when {
                it.code == KeyCode.SPACE -> {
                    onPause = !onPause
                    if (onPause) actionNotification("Paused", 2, true)
                    else actionNotification("Resumed", 2, false)
                }
                it.code == KeyCode.RIGHT -> {
                    interval.set(interval.subtract(intervalFactor).get())
                    updateDrawingTask()
                    actionNotification("Interval reduced ${interval.get()}", 4, false)
                }
                it.code == KeyCode.LEFT -> {
                    interval.set(interval.add(intervalFactor).get())
                    updateDrawingTask()
                    actionNotification("Interval increased ${interval.get()}", 4, false)
                }
                it.code == KeyCode.UP -> {
                    cellSize.set(cellSize.add(cellFactor).get())
                    Platform.runLater({ redraw() })
                    actionNotification("Cell size increased ${cellSize.get()}", 4, false)
                }
                it.code == KeyCode.DOWN -> {
                    cellSize.set(cellSize.subtract(cellFactor).get())
                    life.resize((canvas.width / cellSize.get()).toInt(), (canvas.height / cellSize.get()).toInt())
                    Platform.runLater({ redraw() })
                    actionNotification("Cell size decreased ${cellSize.get()}", 4, false)
                }
                it.code == KeyCode.Q -> pressedQ.set(true)
                it.code == KeyCode.SHIFT -> pressedShift.set(true)
                it.code == KeyCode.W -> {
                    rectangularCell = !rectangularCell
                    Platform.runLater({ redraw() })
                }
                it.code == KeyCode.CONTROL -> pressedCtrl.set(true)
                it.code == KeyCode.L -> pressedL.set(true)
            }
        }
        scene.setOnKeyReleased {
            when {
                it.code == KeyCode.Q -> pressedQ.set(false)
                it.code == KeyCode.SHIFT -> pressedShift.set(false)
                it.code == KeyCode.CONTROL -> pressedCtrl.set(false)
                it.code == KeyCode.L -> pressedL.set(false)
            }
        }
    }

    private fun defineDeltaCoordinates(event: MouseEvent) {
        xDelta = mousePositionAfterClick.first - event.x
        yDelta = mousePositionAfterClick.second - event.y
        if (xDelta <= 0) xDelta = 0.0
        if (xDelta >= cellSize.multiply(life.width).get() - canvas.width) xDelta = cellSize.multiply(life.width).get() - canvas.width
        if (yDelta <= 0) yDelta = 0.0
        if (yDelta >= cellSize.multiply(life.height).get() - canvas.height) yDelta = cellSize.multiply(life.height).get() - canvas.height
    }

    private fun isMouseWithinWindow(event: MouseEvent) = event.x in 0 until scene.width.toInt()
            && event.y in 0 until scene.height.toInt()

    private fun onCellClickActionWithSplash(x: Double, y: Double) {
        val actualRadius = randomizingRadius * cellSize.get()
        val leftTopLimit = (if (x - actualRadius < 0) 0.0 else x - actualRadius) to if (y - actualRadius < 0) 0.0 else y - actualRadius
        val rightBottomLimit = (if (x + actualRadius > canvas.width) canvas.width else x + actualRadius) to if (y + actualRadius > canvas.height) canvas.height else y + actualRadius
        val randomWidth = rightBottomLimit.first - leftTopLimit.first
        val randomHeight = rightBottomLimit.second - leftTopLimit.second
        val randomAmount = (randomWidth / cellSize.get() * randomHeight / cellSize.get()) / 100 * randomizingFactor
        (0..randomAmount.toInt()).forEach {
            onCellClickAction(random.nextInt(randomWidth.toInt()) + leftTopLimit.first,
                    random.nextInt(randomHeight.toInt()) + leftTopLimit.second)
        }
    }

    private fun onCellClickAction(x: Double, y: Double) {
        life.place(((x + xDelta) / cellSize.get()).toInt(), ((y + yDelta) / cellSize.get()).toInt(),
                if (lastClickedCell == CellState.EMPTY) CellState.ALIVE else CellState.EMPTY)
        if (lastClickedCell == CellState.EMPTY) {
            drawer.drawCell(x, y, cellSize.get().toDouble(), cellColor, strokeColor, rectangularCell)
        } else {
            drawer.drawCell(x, y, cellSize.get().toDouble(), backgroundColor, strokeColor, rectangularCell)
        }
    }

    private fun initStackPane() {
        stackPane.children.add(canvas)
        stackPane.children.add(notificationTextElement)
    }

    private fun initStage(primaryStage: Stage) {
        primaryStage.scene = scene
        primaryStage.title = "Game Of Life"
        primaryStage.show()
        primaryStage.setOnCloseRequest {
            Platform.exit()
            System.exit(0)
        }
    }

    private fun updateDrawingTask() {
        if (timer != null) {
            timer!!.cancel()
            timer = null
        }
        timer = Timer()
        timer!!.schedule(DrawingTask(), interval.get(), interval.get())
    }

    private fun redraw() {
        drawer.clearCanvas(backgroundColor)
        drawer.drawGrid(cellSize.get(), strokeColor)
        updateCanvas()
        System.gc()
    }

    private fun updateCanvas() {
        val field = life.getField()
        val xDeltaCells = (xDelta / cellSize.get()).toInt()
        val yDeltaCells = (yDelta / cellSize.get()).toInt()
        val doubleCellSize = cellSize.get().toDouble()
        (0 until (canvas.width / cellSize.get()).toInt()).forEach { x ->
            (0 until (canvas.height / cellSize.get()).toInt()).forEach { y ->
                if (field[x + xDeltaCells][y + yDeltaCells] == CellState.ALIVE) {
                    drawer.drawCell(x * doubleCellSize, y * doubleCellSize, doubleCellSize, cellColor, strokeColor, rectangularCell)
                }
            }
        }
    }

    private fun killAll() {
        val field = life.getField()
        field.indices.forEach { x ->
            field[x].indices.forEach { y ->
                field[x][y] = CellState.EMPTY
            }
        }
    }

    private fun actionNotification(text: String, cycles: Int, infinite: Boolean) {
        drawer.updateTextElement(notificationTextElement, text, textColor)
        drawer.updateFadeTransition(ftNotification, cycles, infinite)
    }

    inner class DrawingTask : TimerTask() {
        override fun run() {
            if (!onPause) {
                life.nextGeneration()
                Platform.runLater({ redraw() })
            }
        }
    }
}

fun main(args: Array<String>) {
    launch(GameOfLife::class.java)
}