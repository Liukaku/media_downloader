package mattstarkey.dev.cli_tool_gui

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "cli_tool_gui",
    ) {
        App()
    }
}