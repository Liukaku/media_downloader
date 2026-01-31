package mattstarkey.dev.cli_tool_gui.utils

import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class Cli (
    val osName: String = System.getProperty("os.name") ?: ""
) {

    fun printOsInfo() {
        println("Detected OS: $osName")
    }
    companion object {
        // Runs the given command (program + args) and returns stdout/stderr/exit code.
        // Streams output incrementally via optional callbacks while still returning the full output.
        // Uses small thread-pool readers so large outputs won't block.
        fun runCommand(
            command: List<String>,
            onStdout: ((String) -> Unit)? = null,
            onStderr: ((String) -> Unit)? = null
        ): ProcessResult {
            val pb = ProcessBuilder(command).redirectErrorStream(false)

            return try {
                val process = pb.start()

                // Thread to drain STDOUT and send to the UI callback
                val outThread = Thread {
                    process.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            onStdout?.invoke(line + "\n")
                        }
                    }
                }.apply { start() }

                // Thread to drain STDERR and send to the UI callback
                val errThread = Thread {
                    process.errorStream.bufferedReader(Charsets.UTF_8).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            onStderr?.invoke(line + "\n")
                        }
                    }
                }.apply { start() }

                // Wait for the process to exit naturally (No more fixed timeouts!)
                val exitCode = process.waitFor()

                // Ensure threads finish reading the last bit of text before returning
                outThread.join()
                errThread.join()

                ProcessResult(exitCode, "Output streamed to console", "")
            } catch (e: Exception) {
                ProcessResult(-1, "", e.message ?: "Execution failed")
            }
        }


    }


}

data class ProcessResult(val exitCode: Int, val stdout: String, val stderr: String)

