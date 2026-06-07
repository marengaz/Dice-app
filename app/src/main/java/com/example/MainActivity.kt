package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sound.SoundEngine
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class DiceMode {
    DOTS, ANIMALS, NUMBERS
}

data class Particle(
    val id: Long,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val size: Float,
    val color: Color,
    val emoji: String,
    val alpha: Float,
    val rotation: Float,
    val rotationSpeed: Float
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SoundEngine.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("app_scaffold")
                ) { innerPadding ->
                    DiceGameScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

// Map each number (1-6) to a vibrant color setting
val DiceColors = mapOf(
    1 to Color(0xFFFF2E93),  // Vibrant Pink Magic
    2 to Color(0xFFFF6200),  // Bright Comic Orange
    3 to Color(0xFFFFD600),  // Brilliant Sunshine Yellow
    4 to Color(0xFF00E676),  // Neon Shamrock Green
    5 to Color(0xFF00B0FF),  // Crystal Spray Cyan
    6 to Color(0xFF9D4EDD)   // Cosmic Violet Purple
)

val ColorDescriptions = mapOf(
    1 to "Vibrant Pink",
    2 to "Bright Orange",
    3 to "Sunshine Yellow",
    4 to "Shamrock Green",
    5 to "Vibrant Blue",
    6 to "Cosmic Violet"
)

val DiceNames = mapOf(
    1 to "One",
    2 to "Two",
    3 to "Three",
    4 to "Four",
    5 to "Five",
    6 to "Six"
)

val DiceEmojis = mapOf(
    1 to "🌸",
    2 to "🦊",
    3 to "🌟",
    4 to "🌱",
    5 to "🐬",
    6 to "🦄"
)

val ParticleEmojis = listOf("✨", "⭐", "🎉", "🎈", "💫", "🍿", "🍬", "🌈", "🔥")

@Composable
fun PulsingDotsRow() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    
    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 150, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )

    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White.copy(alpha = dot1Alpha)))
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White.copy(alpha = dot2Alpha)))
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White.copy(alpha = dot3Alpha)))
    }
}

@Composable
fun DiceGameScreen(modifier: Modifier = Modifier) {
    var currentNumber by remember { mutableStateOf(5) }
    var isRolling by remember { mutableStateOf(false) }
    var activeMode by remember { mutableStateOf(DiceMode.DOTS) }
    var soundMuted by remember { mutableStateOf(false) }

    // Dimensions
    var screenWidth by remember { mutableStateOf(0f) }
    var screenHeight by remember { mutableStateOf(0f) }

    // Sound configuration synchronization
    LaunchedEffect(soundMuted) {
        SoundEngine.setMute(soundMuted)
    }

    // Interactive Physics Particles
    var activeParticles by remember { mutableStateOf(emptyList<Particle>()) }

    // Trigger Particle Exploding Burst
    fun triggerBurst(cx: Float, cy: Float) {
        val numberColor = DiceColors[currentNumber] ?: Color.White
        val matchingEmoji = DiceEmojis[currentNumber] ?: "⭐"
        
        val list = ArrayList<Particle>()
        for (i in 0 until 24) {
            val angle = Math.toRadians((0..359).random().toDouble())
            val magnitude = (8..22).random().toFloat()
            val vx = (kotlin.math.cos(angle) * magnitude).toFloat()
            val vy = (kotlin.math.sin(angle) * magnitude - 6).toFloat()
            
            val emojiStr = if (i % 3 == 0) matchingEmoji else ParticleEmojis.random()
            val randomSize = (14..32).random().toFloat()

            list.add(
                Particle(
                    id = System.nanoTime() + i,
                    x = cx,
                    y = cy,
                    vx = vx,
                    vy = vy,
                    size = randomSize,
                    color = numberColor,
                    emoji = emojiStr,
                    alpha = 1.0f,
                    rotation = (0..359).random().toFloat(),
                    rotationSpeed = (-12..12).random().toFloat()
                )
            )
        }
        activeParticles = activeParticles + list
    }

    // Tick Loop handling custom gravitational particles
    LaunchedEffect(activeParticles) {
        if (activeParticles.isNotEmpty()) {
            delay(16)
            activeParticles = activeParticles.map { p ->
                p.copy(
                    x = p.x + p.vx,
                    y = p.y + p.vy,
                    vy = p.vy + 0.5f,
                    alpha = p.alpha - 0.02f,
                    rotation = p.rotation + p.rotationSpeed
                )
            }.filter { it.alpha > 0f }
        }
    }

    // Animated backdrops: Soft dynamic gradient based on color reference
    val targetColor = DiceColors[currentNumber] ?: Color(0xFF00B0FF)
    val animatedBgColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 400),
        label = "BackgroundFade"
    )

    // Animated variables for 3D card movement and tumble
    val diceRotationZ = remember { Animatable(0f) }
    val diceRotationX = remember { Animatable(0f) }
    val diceRotationY = remember { Animatable(0f) }
    val diceScale = remember { Animatable(1f) }

    val coroutineScope = rememberCoroutineScope()

    // Trigger the kid friendly spinning roll
    fun startRolling() {
        if (isRolling) return
        isRolling = true
        coroutineScope.launch {
            launch {
                diceRotationZ.snapTo(0f)
                diceRotationZ.animateTo(
                    targetValue = 1080f,
                    animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
                )
            }
            launch {
                diceRotationX.animateTo(
                    targetValue = 35f,
                    animationSpec = tween(durationMillis = 400, easing = LinearEasing)
                )
                diceRotationX.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 80f)
                )
            }
            launch {
                diceRotationY.animateTo(
                    targetValue = -35f,
                    animationSpec = tween(durationMillis = 400, easing = LinearEasing)
                )
                diceRotationY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 80f)
                )
            }
            launch {
                diceScale.animateTo(
                    targetValue = 1.35f,
                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                )
                diceScale.animateTo(
                    targetValue = 1.00f,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = 120f)
                )
            }

            val steps = 8
            var previousVal = currentNumber
            for (step in 1..steps) {
                val delayTime = 50L + step * step * 3L
                delay(delayTime)
                
                var randomRoll = (1..6).random()
                while (randomRoll == previousVal) {
                    randomRoll = (1..6).random()
                }
                previousVal = randomRoll
                currentNumber = randomRoll
                SoundEngine.playTick()
            }

            isRolling = false
            SoundEngine.playDing()
            triggerBurst(screenWidth / 2f, screenHeight / 2f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                screenWidth = size.width.toFloat()
                screenHeight = size.height.toFloat()
            }
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        animatedBgColor.copy(alpha = 0.85f),
                        animatedBgColor
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { startRolling() }
            )
            .testTag("roll_trigger")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            
            Spacer(modifier = Modifier.height(12.dp))

            // Kids-friendly mode pills with smooth glassmorphic aesthetic
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(32.dp))
                    .padding(4.dp)
                    .fillMaxWidth(0.95f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    DiceMode.DOTS to "🎲 Dots",
                    DiceMode.ANIMALS to "🦊 Mascot",
                    DiceMode.NUMBERS to "🔢 Num"
                ).forEach { (mode, label) ->
                    val isSelected = activeMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(if (isSelected) Color.White else Color.Transparent)
                            .clickable {
                                activeMode = mode
                                SoundEngine.playTick()
                            }
                            .padding(vertical = 10.dp)
                            .testTag("mode_${mode.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color(0xFF0F172A) else Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Dice Block: Rounded 64dp design with deep minimalistic shadow and absolute bottom-right volume button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(310.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                rotationZ = diceRotationZ.value
                                rotationX = diceRotationX.value
                                rotationY = diceRotationY.value
                                scaleX = diceScale.value
                                scaleY = diceScale.value
                                cameraDistance = 16f
                            }
                    ) {
                        // Soft large shadow representing pure Clean Minimalism
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .shadow(32.dp, shape = RoundedCornerShape(64.dp), clip = false)
                                .background(Color.White, shape = RoundedCornerShape(64.dp))
                        )

                        Card(
                            shape = RoundedCornerShape(64.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("dice_card")
                        ) {
                            DiceFace(
                                number = currentNumber,
                                mode = activeMode,
                                dotColor = targetColor
                            )
                        }
                    }

                    // Floating Absolute Volume button overlaid on the dice block corner as requested by the design
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 12.dp, y = 12.dp)
                            .shadow(12.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color(0xFFF43F5E))
                            .clickable { soundMuted = !soundMuted }
                            .testTag("volume_toggle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (soundMuted) "🔇" else "🔊",
                            fontSize = 22.sp,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Text results section: tracked uppercase minimalistic typography
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val nameStr = DiceNames[currentNumber] ?: ""
                val descStr = ColorDescriptions[currentNumber] ?: ""
                
                Text(
                    text = if (isRolling) "ROLLING..." else "$nameStr!",
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 4.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isRolling) "🎈 🎲 🎈" else descStr.uppercase(),
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom tap guidance box with clean white gradient pulsing indicators
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(32.dp))
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "TAP SCREEN TO ROLL",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    
                    PulsingDotsRow()
                }
            }
        }

        // Beautiful layered particle explode canvas overlay
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            activeParticles.forEach { p ->
                drawContext.canvas.save()
                drawContext.canvas.translate(p.x, p.y)
                drawContext.canvas.rotate(p.rotation)
                
                val textPaint = android.graphics.Paint().apply {
                    textSize = p.size.dp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                    alpha = (p.alpha * 255).toInt().coerceIn(0, 255)
                }

                drawContext.canvas.nativeCanvas.drawText(
                    p.emoji,
                    0f,
                    p.size.dp.toPx() / 3f,
                    textPaint
                )
                
                drawContext.canvas.restore()
            }
        }
    }
}

@Composable
fun DiceFace(
    number: Int,
    mode: DiceMode,
    dotColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        when (mode) {
            DiceMode.DOTS -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    val isOdd = number % 2 != 0
                    
                    if (isOdd) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(if (number == 1) 56.dp else 28.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }

                    if (number in 2..6) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }

                    if (number in 4..6) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }

                    if (number == 6) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }
            }
            DiceMode.ANIMALS -> {
                val emoji = DiceEmojis[number] ?: "🦊"
                Text(
                    text = emoji,
                    fontSize = 100.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            DiceMode.NUMBERS -> {
                Text(
                    text = number.toString(),
                    fontSize = 116.sp,
                    fontWeight = FontWeight.Black,
                    color = dotColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
