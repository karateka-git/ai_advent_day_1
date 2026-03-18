import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.Properties
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.serialization.json.Json
import models.ChatCompletionRequest
import models.ChatCompletionResponse
import models.ChatMessage

private const val CONFIG_FILE = "config/app.properties"
private const val OUTPUT_FILE = "output.txt"
private const val MODEL = "DeepSeek V3.2"
private const val API_URL_TEMPLATE =
    "https://agent.timeweb.cloud/api/v1/cloud-ai/agents/%s/v1/chat/completions"
private const val USER_ROLE = "user"
private const val ASSISTANT_ROLE = "assistant"
private val json = Json { ignoreUnknownKeys = true }
private val consoleReader = BufferedReader(
    InputStreamReader(System.`in`, detectConsoleCharset())
)
private val systemConsole = System.console()

fun main() {
    val config = loadConfig()
    val agentId = config.getRequired("AGENT_ID")
    val userToken = config.getRequired("USER_TOKEN")
    val conversation = mutableListOf<ChatMessage>()
    val httpClient = HttpClient.newHttpClient()

    println("\u0427\u0430\u0442 \u0437\u0430\u043f\u0443\u0449\u0435\u043d. \u0412\u0432\u0435\u0434\u0438\u0442\u0435 \u0441\u043e\u043e\u0431\u0449\u0435\u043d\u0438\u0435 \u0438 \u043d\u0430\u0436\u043c\u0438\u0442\u0435 Enter.")
    println("\u0412\u0432\u0435\u0434\u0438\u0442\u0435 'exit' \u0438\u043b\u0438 'quit' \u0434\u043b\u044f \u0432\u044b\u0445\u043e\u0434\u0430.")

    while (true) {
        print("\u0412\u044b: ")
        val prompt = readConsoleLine()?.trim()
            ?: break

        if (prompt.isEmpty()) {
            continue
        }

        if (prompt.equals("exit", ignoreCase = true) || prompt.equals("quit", ignoreCase = true)) {
            println("\u0427\u0430\u0442 \u0437\u0430\u0432\u0435\u0440\u0448\u0451\u043d.")
            break
        }

        conversation += ChatMessage(role = USER_ROLE, content = prompt)

        val request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL_TEMPLATE.format(agentId)))
            .header("Content-Type", "application/json; charset=UTF-8")
            .header("Authorization", "Bearer $userToken")
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    json.encodeToString(
                        ChatCompletionRequest(
                            model = MODEL,
                            messages = conversation,
                            temperature = 1,
                            maxTokens = 100
                        )
                    ),
                    StandardCharsets.UTF_8
                )
            )
            .build()

        val loading = LoadingIndicator()
        loading.start()

        val response = try {
            httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
        } finally {
            loading.stop()
        }

        if (response.statusCode() !in 200..299) {
            error("\u041e\u0448\u0438\u0431\u043a\u0430 \u0437\u0430\u043f\u0440\u043e\u0441\u0430 \u043a API. \u0421\u0442\u0430\u0442\u0443\u0441 ${response.statusCode()}: ${response.body()}")
        }

        val completion = json.decodeFromString<ChatCompletionResponse>(response.body())
        val content = completion.choices.firstOrNull()?.message?.content
            ?: error("\u041e\u0442\u0432\u0435\u0442 API \u043d\u0435 \u0441\u043e\u0434\u0435\u0440\u0436\u0438\u0442 choices[0].message.content")

        conversation += ChatMessage(role = ASSISTANT_ROLE, content = content)
        writeOutputFile(conversation)
        println("\u0410\u0441\u0441\u0438\u0441\u0442\u0435\u043d\u0442: $content")
    }
}

private fun detectConsoleCharset(): Charset {
    val nativeEncoding = System.getProperty("native.encoding")
    return if (nativeEncoding.isNullOrBlank()) {
        Charset.defaultCharset()
    } else {
        Charset.forName(nativeEncoding)
    }
}

private fun readConsoleLine(): String? = systemConsole?.readLine() ?: consoleReader.readLine()

private fun loadConfig(): Properties {
    val configPath = Path.of(CONFIG_FILE)
    require(Files.exists(configPath)) {
        "\u0424\u0430\u0439\u043b \u043a\u043e\u043d\u0444\u0438\u0433\u0443\u0440\u0430\u0446\u0438\u0438 $CONFIG_FILE \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d. \u0421\u043e\u0437\u0434\u0430\u0439\u0442\u0435 \u0435\u0433\u043e \u043d\u0430 \u043e\u0441\u043d\u043e\u0432\u0435 config/app.properties.example."
    }

    return Properties().apply {
        Files.newInputStream(configPath).use(::load)
    }
}

private fun Properties.getRequired(key: String): String =
    getProperty(key)?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("\u0412 $CONFIG_FILE \u043e\u0442\u0441\u0443\u0442\u0441\u0442\u0432\u0443\u0435\u0442 \u043e\u0431\u044f\u0437\u0430\u0442\u0435\u043b\u044c\u043d\u043e\u0435 \u0441\u0432\u043e\u0439\u0441\u0442\u0432\u043e '$key'.")

private fun writeOutputFile(conversation: List<ChatMessage>) {
    val transcript = conversation.joinToString(
        separator = System.lineSeparator() + System.lineSeparator()
    ) { message ->
        "${message.role.toRussianRole()}: ${message.content}"
    }

    Files.writeString(
        Path.of(OUTPUT_FILE),
        transcript,
        StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.WRITE
    )
}

private fun String.toRussianRole(): String = when (this) {
    USER_ROLE -> "\u041f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u0435\u043b\u044c"
    ASSISTANT_ROLE -> "\u0410\u0441\u0441\u0438\u0441\u0442\u0435\u043d\u0442"
    else -> replaceFirstChar(Char::uppercase)
}

private class LoadingIndicator {
    private val running = AtomicBoolean(false)
    private var thread: Thread? = null

    fun start() {
        running.set(true)
        thread = Thread {
            var step = 0
            while (running.get()) {
                val dots = ".".repeat(step % 4)
                val padding = " ".repeat(3 - dots.length)
                print("\r\u0410\u0441\u0441\u0438\u0441\u0442\u0435\u043d\u0442 \u0434\u0443\u043c\u0430\u0435\u0442$dots$padding")
                Thread.sleep(350)
                step++
            }
        }.apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        running.set(false)
        thread?.join(500)
        print("\r${" ".repeat(40)}\r")
    }
}
