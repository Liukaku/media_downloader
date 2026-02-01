package mattstarkey.dev.cli_tool_gui.utils

class DependencyValidator {

       fun startUpChecks(): List<StartupErrors> {
           println("Starting dependency checks...")
           val errors = mutableListOf<StartupErrors>()
           try {
               val galleryCheck = Cli.runCommand(
                   command = listOf("gallery-dl", "--version"),
               )
               println(galleryCheck)
               val invalidCmd = extractInvalidCommand(galleryCheck)
                if (invalidCmd != null) {
                     println("how did we get here $invalidCmd")
                    errors.add(StartupErrors.MISSING_GALLERY_DL)
                }
           } catch (e: Exception) {
               println(e.message)
               errors.add(StartupErrors.MISSING_GALLERY_DL)
           }

           try {
               val ytdlpCheck = Cli.runCommand(
                   command = listOf("C:\\yt-dlp.exe", "--version"),
               )
               val invalidYtCmd = extractInvalidCommand(ytdlpCheck)

               if (invalidYtCmd != null) {
                    println("how did we get here $invalidYtCmd")
                   errors.add(StartupErrors.MISSING_YT_DLP)
               }
           } catch (e: Exception) {
               errors.add(StartupErrors.MISSING_YT_DLP)
           }
            return errors
        }

        private val errorPatterns = mapOf(
            // Matches: The term 'testaaa' is not recognized...
            "powershell" to Regex("The term '(.+?)' is not recognized"),

            // Matches: 'teasss' is not recognized as an internal or external command
            "cmd" to Regex("'(.+?)' is not recognized as an internal or external command"),

            // Matches: bash: testaaa: command not found
            "bash" to Regex("bash: (.+?): command not found")
        )

        fun extractInvalidCommand(result: ProcessResult): String? {
            // If the exit code is 0, it probably succeeded
            if (result.exitCode == 0) return null

            val errorText = result.stderr.trim()

            // Check patterns based on OS/Shell
            for ((_, regex) in errorPatterns) {
                val match = regex.find(errorText)
                if (match != null) {
                    // Group 1 is the text inside the quotes/capture group
                    return match.groups[1]?.value
                }
            }

            return "Unknown error format ${result.exitCode}"
        }

}

enum class StartupErrors {
    MISSING_GALLERY_DL,
    MISSING_YT_DLP,
}

val requirements = mapOf(
    StartupErrors.MISSING_GALLERY_DL to "GALLERY_DL",
    StartupErrors.MISSING_YT_DLP to "YT_DLP",
)