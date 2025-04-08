@file:Suppress("SpellCheckingInspection")

package dev.seeight

import dev.seeight.common.lwjgl.Window
import dev.seeight.common.lwjgl.font.IFont
import dev.seeight.common.lwjgl.font.TTFFont
import dev.seeight.common.lwjgl.font.json.CharacterData
import dev.seeight.common.lwjgl.fontrenderer.BufferedFontRenderer
import dev.seeight.renderer.renderer.gl.OpenGLRenderer2
import dev.seeight.util.MathUtil
import org.joml.Vector2i
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30
import java.text.DecimalFormat
import java.util.concurrent.LinkedTransferQueue


object Main {
    private var prevTime: Double = 0.0
    private var deltaTime: Double = 0.0
    private var tickTime: Double = 0.0

    @JvmStatic
    fun main(args: Array<String>) {
        if (!GLFW.glfwInit()) throw RuntimeException("GLFW no fué creado correctamente.")

        val window = Window("Snake", 720, 720)
        window.setVisible(false)
        window.createWindow()
        window.setWindowAspectRatio(1, 1)

        GL.createCapabilities()

        GL11.glClearColor(0f, 0f, 0f, 1f)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

        val renderer = OpenGLRenderer2(true)
        renderer.ortho(0f, window.width.toFloat(), window.height.toFloat(), 0f, 0f, 10f)

        val font = object : TTFFont(Main.javaClass.getResourceAsStream("/font.ttf"), 1024, 1024, 60, 1f) {
            override fun applyTextureParameters() {
                super.applyTextureParameters()
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR_MIPMAP_NEAREST)
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_NEAREST)
                GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D)
            }
        }
        val fontRenderer = object : BufferedFontRenderer(renderer) {
            override fun getCharacterWidth(font: IFont?, data: CharacterData?, codepoint: Int): Float {
                return super.getCharacterWidth(font, data, codepoint) - 0.75f
            }
        }

        window.setFramebufferSizeCallback { width, height ->
            renderer.ortho(0f, width.toFloat(), height.toFloat(), 0f, 0f, 10f)
            GL11.glViewport(0, 0, width, height)
        }

        window.setVisible(true)

        val gridSizeX = 20
        val gridSizeY = 20

        val body = mutableListOf<Vector2i>()
        val snake = Snake(body, Direction.entries[MathUtil.getRandomInt(0, Direction.entries.size - 1)])
        body.add(Vector2i(MathUtil.getRandomInt(0, gridSizeX - 1), MathUtil.getRandomInt(0, gridSizeY - 1)))

        var fruitPos = Vector2i(MathUtil.getRandomInt(0, gridSizeX - 1), MathUtil.getRandomInt(0, gridSizeY - 1))
        var score = 0
        var maxScore = 0
        var gameOver = false

        var speed = 1.0
        val buffer = LinkedTransferQueue<Direction>()
        var prev = snake.direction

        window.setKeyCallback { key, scancode, action, mods ->
            if (gameOver) {
                if (key == GLFW.GLFW_KEY_R) {
                    speed = 1.0
                    gameOver = false
                    score = 0
                    buffer.clear()
                    snake.direction = Direction.entries[MathUtil.getRandomInt(0, Direction.entries.size - 1)]
                    snake.body.clear()
                    body.add(Vector2i(MathUtil.getRandomInt(0, gridSizeX - 1), MathUtil.getRandomInt(0, gridSizeY - 1)))
                    fruitPos = Vector2i(MathUtil.getRandomInt(0, gridSizeX - 1), MathUtil.getRandomInt(0, gridSizeY - 1))
                }
                return@setKeyCallback
            }

            if (action == GLFW.GLFW_PRESS) {
                val nextDirection = when (key) {
                    GLFW.GLFW_KEY_UP -> Direction.UP
                    GLFW.GLFW_KEY_DOWN -> Direction.DOWN
                    GLFW.GLFW_KEY_LEFT -> Direction.LEFT
                    GLFW.GLFW_KEY_RIGHT -> Direction.RIGHT
                    else -> null
                }

                if (nextDirection != null && nextDirection != prev.getOpposite() && nextDirection != prev) {
                    buffer.put(nextDirection)
                    prev = nextDirection
                }
            }
        }

        val df = DecimalFormat("#.####")
        while (!window.shouldClose()) {
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT)

            renderer.frameStart()

            fontRenderer.setScale((window.width / gridSizeX / 80.0).toFloat())

            renderer.color(1.0, 1.0, 1.0, 1.0)
            fontRenderer.drawString(font, "Puntaje: $score", 0f, 0f)
            fontRenderer.drawString(font, "Maximo Puntaje: $maxScore", 0f, fontRenderer.getNewLineHeight(font))
            fontRenderer.drawString(font, "Velocidad: ${df.format(speed)}", 0f, fontRenderer.getNewLineHeight(font) * 2)

            val renderWidth = window.width / gridSizeX
            val renderHeight = window.height / gridSizeY

            for (v in snake.body) {
                val x = v.x * renderWidth
                val y = v.y * renderHeight

                renderer.color(1.0, 1.0, 1.0, 1.0)
                renderer.rect2d(x + 2.0, y + 2.0, x + renderWidth - 2.0, y + renderHeight - 2.0)
            }

            val x = fruitPos.x * renderWidth
            val y = fruitPos.y * renderHeight
            renderer.color(0.0, 0.0, 1.0, 1.0)
            renderer.rect2d(x + 2.0, y + 2.0, x + renderWidth - 2.0, y + renderHeight - 2.0)

            if (tickTime >= 1f/(speed * 4) && !gameOver) {
                val _b = buffer.poll()
                if (_b != null) snake.direction = _b

                val head = snake.body[0]
                val newX = MathUtil.wrap(head.x + snake.direction.xOffset, 0, gridSizeX - 1)
                val newY = MathUtil.wrap(head.y + snake.direction.yOffset, 0, gridSizeY - 1)

                for (i in 1 until snake.body.size) {
                    if (snake.body[i].x == newX && snake.body[i].y == newY) {
                        gameOver = true
                    }
                }

                for (i in snake.body.size - 1 downTo 1) {
                    snake.body[i].set(snake.body[i - 1])
                }

                head.x = newX
                head.y = newY

                if (head.x == fruitPos.x && head.y == fruitPos.y) {
                    fruitPos = Vector2i(MathUtil.getRandomInt(0, gridSizeX - 1), MathUtil.getRandomInt(0, gridSizeY - 1))
                    snake.body.add(Vector2i(-1, -1)) // offscreen, to prevent it appearing before time.
                    score++
                    if (maxScore < score) {
                        maxScore = score
                    }
                    speed += 0.2
                }

                tickTime = 0.0
            }

            if (gameOver) {
                renderer.color(0.0, 0.0, 0.0, 0.25)
                renderer.rect2d(0.0, 0.0, window.width.toDouble(), window.height.toDouble())

                val s = "Fin del Juego!".toCharArray()
                val s1 = "Presiona 'R' para volver a intentarlo.".toCharArray()
                var fx = (window.width.toFloat() - fontRenderer.getWidthFloat(font, s)) / 2f
                var fy = window.height.toFloat() / 2 - 100

                renderer.color(1.0, 1.0, 1.0, 1.0)
                fontRenderer.drawString(font, s, fx, fy)

                fx = (window.width.toFloat() - fontRenderer.getWidthFloat(font, s1)) / 2f
                fy += fontRenderer.getHeightFloat(font, s)

                fontRenderer.drawString(font, s1, fx, fy)
            }


            tickTime += deltaTime

            renderer.frameEnd()

            window.swapBuffers()
            GLFW.glfwPollEvents()

            val time = GLFW.glfwGetTime()
            deltaTime = time - prevTime
            prevTime = time
        }

        renderer.destroy()
        GL.destroy()
        window.destroy()
        GLFW.glfwTerminate()
    }
}