package dev.seeight

import org.joml.Vector2i

class Snake(var body: MutableList<Vector2i>, var direction: Direction)

enum class Direction(val xOffset: Int, val yOffset: Int) {
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0);

    fun getOpposite(): Direction {
        return when (this) {
            UP -> DOWN
            DOWN -> UP
            LEFT -> RIGHT
            RIGHT -> LEFT
        }
    }
}