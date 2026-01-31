package mattstarkey.dev.cli_tool_gui.utils

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker

class FilePicker {

    var currentOutputFolder: String? = null
    var inputUrl: String? = null

    fun setPickerInputUrl(url: String) {
        inputUrl = url
    }

    suspend fun pickFolder(): String {
        val selection = FileKit.openDirectoryPicker(
            title = "Select a directory",
            directory = null,
            dialogSettings = FileKitDialogSettings(parentWindow = null)
        )

        if (selection == null) return ""

        // Try common path-extraction methods at runtime to avoid compile-time type-check issues
        val runtimePath = runCatching {
            val cls = selection.javaClass
            // Try methods that libraries commonly expose
            val candidates = listOf("getPath", "getAbsolutePath", "toPath")
            for (name in candidates) {
                val method = runCatching { cls.getMethod(name) }.getOrNull() ?: continue
                val result = method.invoke(selection) ?: continue
                // Normalize every branch to a Kotlin String
                if (result is java.io.File) return@runCatching result.path
                if (result is java.nio.file.Path) return@runCatching result.toAbsolutePath().toString()
                return@runCatching result.toString()
            }
            // Fallback to toString if none of the methods exist
            selection.toString()
        }.getOrNull()

        currentOutputFolder = runtimePath
        return runtimePath ?: selection.toString()
    }
}