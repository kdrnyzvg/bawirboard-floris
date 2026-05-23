package com.snakegame

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.random.Random

enum class Direction { UP, DOWN, LEFT, RIGHT }
enum class GameState { IDLE, PLAYING, GAME_OVER }

data class Point(val x: Int, val y: Int)

private const val GRID_SIZE = 20

@Composable
fun SnakeGameScreen() {
    var snake by remember { mutableStateOf(listOf(Point(10, 10), Point(9, 10), Point(8, 10))) }
    var food by remember { mutableStateOf(Point(15, 15)) }
    var direction by remember { mutableStateOf(Direction.RIGHT) }
    var nextDirection by remember { mutableStateOf(Direction.RIGHT) }
    var score by remember { mutableIntStateOf(0) }
    var highScore by remember { mutableIntStateOf(0) }
    var gameState by remember { mutableStateOf(GameState.IDLE) }

    fun spawnFood(currentSnake: List<Point>): Point {
        var p: Point
        do {
            p = Point(Random.nextInt(GRID_SIZE), Random.nextInt(GRID_SIZE))
        } while (currentSnake.contains(p))
        return p
    }

    fun resetGame() {
        val initialSnake = listOf(Point(10, 10), Point(9, 10), Point(8, 10))
        snake = initialSnake
        food = spawnFood(initialSnake)
        direction = Direction.RIGHT
        nextDirection = Direction.RIGHT
        score = 0
        gameState = GameState.PLAYING
    }

    LaunchedEffect(gameState) {
        if (gameState != GameState.PLAYING) return@LaunchedEffect
        while (gameState == GameState.PLAYING) {
            delay(150L)
            direction = nextDirection
            val head = snake.first()
            val newHead = when (direction) {
                Direction.UP -> Point(head.x, head.y - 1)
                Direction.DOWN -> Point(head.x, head.y + 1)
                Direction.LEFT -> Point(head.x - 1, head.y)
                Direction.RIGHT -> Point(head.x + 1, head.y)
            }
            if (newHead.x < 0 || newHead.x >= GRID_SIZE || newHead.y < 0 || newHead.y >= GRID_SIZE || snake.contains(newHead)) {
                if (score > highScore) highScore = score
                gameState = GameState.GAME_OVER
                break
            }
            val ateFood = newHead == food
            snake = if (ateFood) listOf(newHead) + snake else listOf(newHead) + snake.dropLast(1)
            if (ateFood) {
                score += 10
                food = spawnFood(snake)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Score: $score",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Best: $highScore",
                color = Color(0xFF4CAF50),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(8.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(gameState) {
                        var totalX = 0f
                        var totalY = 0f
                        detectDragGestures(
                            onDragStart = { _ ->
                                totalX = 0f
                                totalY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                totalX += dragAmount.x
                                totalY += dragAmount.y
                                if (abs(totalX) > 50f || abs(totalY) > 50f) {
                                    if (abs(totalX) > abs(totalY)) {
                                        if (totalX > 0 && direction != Direction.LEFT) {
                                            nextDirection = Direction.RIGHT
                                        } else if (totalX < 0 && direction != Direction.RIGHT) {
                                            nextDirection = Direction.LEFT
                                        }
                                    } else {
                                        if (totalY > 0 && direction != Direction.UP) {
                                            nextDirection = Direction.DOWN
                                        } else if (totalY < 0 && direction != Direction.DOWN) {
                                            nextDirection = Direction.UP
                                        }
                                    }
                                    totalX = 0f
                                    totalY = 0f
                                }
                            }
                        )
                    }
            ) {
                val cellSize = size.width / GRID_SIZE

                drawRect(Color(0xFF1E1E1E))

                for (i in 0..GRID_SIZE) {
                    val pos = i * cellSize
                    drawLine(Color(0xFF2A2A2A), Offset(pos, 0f), Offset(pos, size.height), strokeWidth = 1f)
                    drawLine(Color(0xFF2A2A2A), Offset(0f, pos), Offset(size.width, pos), strokeWidth = 1f)
                }

                drawRect(
                    Color(0xFFE53935),
                    topLeft = Offset(food.x * cellSize + 2f, food.y * cellSize + 2f),
                    size = Size(cellSize - 4f, cellSize - 4f)
                )

                snake.forEachIndexed { index, point ->
                    val color = if (index == 0) Color(0xFF76FF03) else Color(0xFF4CAF50)
                    drawRect(
                        color,
                        topLeft = Offset(point.x * cellSize + 1f, point.y * cellSize + 1f),
                        size = Size(cellSize - 2f, cellSize - 2f)
                    )
                }
            }

            if (gameState == GameState.IDLE) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "SNAKE",
                        color = Color(0xFF76FF03),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Swipe board or use buttons to play",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { resetGame() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("START GAME", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (gameState == GameState.GAME_OVER) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xCC000000)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "GAME OVER",
                        color = Color(0xFFE53935),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Score: $score",
                        color = Color.White,
                        fontSize = 22.sp
                    )
                    if (score > 0 && score == highScore) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "New High Score!",
                            color = Color(0xFFFFD700),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { resetGame() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("PLAY AGAIN", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            DpadButton("^") {
                if (direction != Direction.DOWN) nextDirection = Direction.UP
            }
            Row {
                DpadButton("<") {
                    if (direction != Direction.RIGHT) nextDirection = Direction.LEFT
                }
                Spacer(Modifier.size(56.dp))
                DpadButton(">") {
                    if (direction != Direction.LEFT) nextDirection = Direction.RIGHT
                }
            }
            DpadButton("v") {
                if (direction != Direction.UP) nextDirection = Direction.DOWN
            }
        }
    }
}

@Composable
private fun DpadButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(56.dp),
        contentPadding = PaddingValues(0.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
    ) {
        Text(
            text = label,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
