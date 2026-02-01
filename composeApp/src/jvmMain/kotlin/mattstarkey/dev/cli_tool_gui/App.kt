package mattstarkey.dev.cli_tool_gui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mattstarkey.dev.cli_tool_gui.utils.Cli
import mattstarkey.dev.cli_tool_gui.utils.DependencyValidator
import mattstarkey.dev.cli_tool_gui.utils.FilePicker
import mattstarkey.dev.cli_tool_gui.utils.StartupErrors
import mattstarkey.dev.cli_tool_gui.utils.requirements
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// --- EVA COLOR PALETTE ---
val EvaBlack = Color(0xFF050505)
val EvaOrange = Color(0xFFFF9800)
val EvaDarkOrange = Color(0xFF8B4500)
val EvaRed = Color(0xFFD32F2F)
val EvaGreen = Color(0xFF00FF41)

// --- CUSTOM HEXAGON SHAPE ---
val HexagonShape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            val radius = min(size.width, size.height) / 2f
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            for (i in 0..5) {
                val angle = i * (2 * PI / 6) - (PI / 2)
                val x = centerX + radius * cos(angle).toFloat()
                val y = centerY + radius * sin(angle).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        return Outline.Generic(path)
    }
}

@Preview
@Composable
fun App() {
    val cli = Cli()
    val picker = FilePicker()
    val coroutineScope = rememberCoroutineScope()
    val validator = DependencyValidator()

    var consoleOutput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    var startupErrors by remember { mutableStateOf<List<StartupErrors>>(emptyList()) }
    var isChecking by remember { mutableStateOf(true) }
    var inputTextState by remember { mutableStateOf("") }
    var outputFolderState by remember { mutableStateOf("") }

    LaunchedEffect(consoleOutput) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    LaunchedEffect(Unit) {
        startupErrors = validator.startUpChecks()
        isChecking = false
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = EvaOrange,
            surface = EvaBlack,
            onSurface = EvaOrange,
        )
    ) {
        // Main outer container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(EvaBlack)
                .padding(12.dp) // Outer margin from window edge
                .border(2.dp, EvaOrange, CutCornerShape(12.dp))
                .padding(20.dp) // INTERNAL SPACE: This stops the content from touching the orange border
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- MAGI HEADER ---
                MagiHeader(startupErrors, isChecking)

                Spacer(Modifier.height(24.dp))

                // --- FOLDER SELECTION ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(48.dp).background(EvaOrange, HexagonShape))
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "STORAGE_PATH: ${outputFolderState.ifEmpty { "UNDEFINED" }}",
                            color = EvaOrange,
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                        )
                        Spacer(Modifier.height(4.dp))
                        EvaButton(
                            text = "BROWSE_DIRECTORIES",
                            onClick = { coroutineScope.launch { outputFolderState = picker.pickFolder() } }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // --- INPUT SEQUENCE ---
                TextField(
                    value = inputTextState,
                    onValueChange = {
                        picker.setPickerInputUrl(it)
                        inputTextState = it
                    },
                    label = { Text("TARGET_URL_SEQUENCE", color = EvaOrange, fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth().border(1.dp, EvaDarkOrange, CutCornerShape(4.dp)),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = EvaOrange),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF121212),
                        unfocusedContainerColor = Color.Black,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                Spacer(Modifier.height(16.dp))

                // --- ACTION BUTTONS ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EvaButton(
                        modifier = Modifier.weight(1f),
                        enabled = (startupErrors.isEmpty() && outputFolderState.isNotEmpty() && inputTextState.contains("youtube.com")),
                        text = "EXECUTE: YT_DLP",
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                consoleOutput = "INITIATING_YOUTUBE_STREAM_CAPTURE...\n"
                                val result = Cli.runCommand(
                                    command = listOf("C:\\yt-dlp.exe", "-P", outputFolderState, inputTextState),
                                    onStdout = { consoleOutput += it },
                                    onStderr = { consoleOutput += it }
                                )
                                consoleOutput += "\nSEQUENCE_COMPLETE: CODE ${result.exitCode}\n"
                            }
                        }
                    )
                    EvaButton(
                        modifier = Modifier.weight(1f),
                        enabled = (startupErrors.isEmpty() && outputFolderState.isNotEmpty() && inputTextState.isNotEmpty() && !inputTextState.contains("youtube.com")),
                        text = "EXECUTE: GALLERY_DL",
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                consoleOutput = "INITIATING_GALLERY_EXTRACTION_SEQUENCE...\n"
                                val result = Cli.runCommand(
                                    command = listOf("gallery-dl", "-d", outputFolderState, inputTextState),
                                    onStdout = { consoleOutput += it },
                                    onStderr = { consoleOutput += it }
                                )
                                consoleOutput += "\nSEQUENCE_COMPLETE: CODE ${result.exitCode}\n"
                            }
                        }
                    )
                }

                // --- CONSOLE (THE HUD) ---
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                        .border(1.dp, EvaDarkOrange)
                        .background(Color.Black)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(12.dp)
                    ) {
                        Text(
                            text = consoleOutput,
                            color = EvaGreen,
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 16.sp)
                        )
                    }
                    VerticalScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(2.dp),
                        adapter = rememberScrollbarAdapter(scrollState)
                    )
                }

                // --- FOOTER ACTION ---
                EvaButton(
                    modifier = Modifier.fillMaxWidth(0.5f),
                    enabled = outputFolderState.isNotEmpty(),
                    text = "OPEN_TARGET_DIRECTORY",
                    onClick = {
                        val command = if (System.getProperty("os.name").startsWith("Windows")) {
                            listOf("explorer.exe", outputFolderState)
                        } else listOf("open", outputFolderState)
                        Cli.runCommand(command)
                    }
                )
            }
        }
    }
}
@Composable
fun MagiHeader(errors: List<StartupErrors>, isChecking: Boolean) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse)
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("DOWNLOADER_SYSTEM_STATUS", color = EvaOrange, fontWeight = FontWeight.Bold)
            Text(if (isChecking) "SYNCING..." else "LINKED", color = if (isChecking) EvaOrange else EvaGreen)
        }

        if (errors.isNotEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().background(EvaRed.copy(alpha = alpha)).padding(4.dp)
            ) {
                Text("EMERGENCY: $errors", color = Color.White, modifier = Modifier.align(Alignment.Center))
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StartupErrors.entries.forEach { magi ->
                    Text("${requirements[magi]}: [ ${if (errors.contains(magi)) magi else "STARTUP_SUCCESS"} ]", color = if (errors.contains(magi)) EvaRed else EvaGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp), thickness = 1.dp, color = EvaOrange)
    }
}

@Composable
fun EvaButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp),
        modifier = modifier
            .height(44.dp)
            .border(1.dp, EvaDarkOrange, CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = EvaOrange,
            disabledContainerColor = Color(0xFF2E1A00),
            contentColor = Color.Black,
            disabledContentColor = EvaDarkOrange
        ),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        Text(
            text.uppercase(),
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        )
    }
}