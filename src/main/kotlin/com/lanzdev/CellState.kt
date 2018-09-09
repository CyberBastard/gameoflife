package com.lanzdev

enum class CellState(val visible: Boolean) {
    EMPTY(false),
    ALIVE(true),
    DEAD(true),
    HATCHED(false)
}