package mattstarkey.dev.cli_tool_gui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.painterResource

import cli_tool_gui.composeapp.generated.resources.Res
import cli_tool_gui.composeapp.generated.resources.compose_multiplatform
import mattstarkey.dev.cli_tool_gui.utils.Cli
import mattstarkey.dev.cli_tool_gui.utils.FilePicker
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import mattstarkey.dev.cli_tool_gui.utils.DependencyValidator
import mattstarkey.dev.cli_tool_gui.utils.StartupErrors
import java.beans.Visibility

@Composable
@Preview
fun App() {

    val cli = Cli()
    val picker = FilePicker()
    val coroutineScope = rememberCoroutineScope()

    val validator = DependencyValidator()

    var consoleOutput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState() // Tracks scroll position

    // Auto-scroll logic: whenever consoleOutput changes, scroll to the bottom
    LaunchedEffect(consoleOutput) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    fun resetConsoleOutput(input: String) {
        consoleOutput = input
    }

    var startupErrors by remember { mutableStateOf<List<StartupErrors>>(emptyList()) }
    var isChecking by remember { mutableStateOf(true) }

    // This runs ONCE when the app starts, in the background
    LaunchedEffect(Unit) {
        startupErrors = validator.startUpChecks()
        isChecking = false
    }

    MaterialTheme {
        var showContent by remember { mutableStateOf(false) }
        var inputTextState by remember { mutableStateOf("") }
        var outputFolderState by remember { mutableStateOf("") }
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedVisibility(visible = startupErrors.isNotEmpty()){
                Column {
                    Text( "ERRORS DETECTED: $startupErrors" )
                }

            }

            Text("Selected folder: $outputFolderState")
            Button(onClick = {
                coroutineScope.launch {
                    val folder = picker.pickFolder()
                    outputFolderState = folder
                    println("Selected folder: $outputFolderState")
                }
            }) {
                Text("Open Directory Picker")
            }

            TextField(
                value = inputTextState,
                onValueChange = {
                    picker.setPickerInputUrl(it)
                    inputTextState = it
                },
                label = { Text("Which URL?") },
            )

            Button(
                enabled = (startupErrors.isEmpty() && outputFolderState != "" && inputTextState.contains("youtube.com")),
                onClick = {
                    coroutineScope.launch {
                        val outputFolder = picker.currentOutputFolder
                        val inputUrl = picker.inputUrl

                        if (outputFolder.isNullOrEmpty() || inputUrl.isNullOrEmpty()) {
                            println("Output folder or input URL is empty!")
                            return@launch
                        }

                        val commands = listOf(
                            "C:\\yt-dlp.exe",
                            "-P", outputFolder,
                            inputUrl
                        )

                        resetConsoleOutput("Starting download...\n") // Reset or append

                        val result = Cli.runCommand(
                            command = commands,
                            onStdout = { consoleOutput += it }, // Minimal change: append directly
                            onStderr = { consoleOutput += it }
                        )

                        println("Process exited with code: ${result.exitCode}")
                    }
                }
            )
            {
                Text("Download YouTube Video" )
            }

            Button(
                enabled = (startupErrors.isEmpty() && outputFolderState.isNotEmpty() && inputTextState !== "" && !inputTextState.contains("youtube.com")),
                onClick = {
                    coroutineScope.launch {
                        val outputFolder = picker.currentOutputFolder
                        val inputUrl = picker.inputUrl

                        if (outputFolder.isNullOrEmpty() || inputUrl.isNullOrEmpty()) {
                            println("Output folder or input URL is empty!")
                            return@launch
                        }

                        val commands = listOf(
                            "gallery-dl",
                            "-d", outputFolder,
                            inputUrl
                        )

                        resetConsoleOutput("Starting download...\n") // Reset or append

//                        val result = Cli.runCommand(
//                            command = commands,
//                            onStdout = { consoleOutput += it }, // Minimal change: append directly
//                            onStderr = { consoleOutput += it }
//                        )

                        coroutineScope.launch(Dispatchers.IO) {
                            val result = Cli.runCommand(
                                command = commands,
                                onStdout = { consoleOutput += it }, // Minimal change: append directly
                                onStderr = { consoleOutput += it }
                            )
                            consoleOutput += ("\n Process exited with code: ${result.exitCode} \n")
                        }

                    }
                }
            ){
                Text("Download Gallery-DL Media" )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Takes up remaining space
                    .padding(8.dp)
                    .background(androidx.compose.ui.graphics.Color.Black)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState) // Makes it scrollable
                        .padding(8.dp)
                ) {
                    Text(
                        text = consoleOutput,
                        color = androidx.compose.ui.graphics.Color.Green, // Console green
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }

                // Add a Windows-style scrollbar to the right side
                VerticalScrollbar(
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                    adapter = androidx.compose.foundation.rememberScrollbarAdapter(scrollState)
                )
            }

//            TextField(
//                value = consoleOutput,
//                onValueChange = {}, // Read-only
//                label = { Text("Console Output") },
//                modifier = Modifier.fillMaxWidth().weight(1f), // weight(1f) helps it take up space
//                readOnly = true
//            )

            Button(
                enabled = outputFolderState.isNotEmpty(),
                onClick = {
                    val outputFolder = picker.currentOutputFolder
                    if (!outputFolder.isNullOrEmpty()) {
                        // Open the folder in the system file explorer
                        val command = when {
                            System.getProperty("os.name").startsWith("Windows") -> listOf("explorer.exe", outputFolder)
                            System.getProperty("os.name").startsWith("Mac") -> listOf("open", outputFolder)
                            else -> listOf("xdg-open", outputFolder) // Linux and others
                        }
                        Cli.runCommand(command)
                    }
                }
            ){
                Text("Open Selected Folder")
            }
        }
    }
}