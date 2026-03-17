import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
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

fun main() {
    val config = loadConfig()
    val agentId = config.getRequired("AGENT_ID")
    val userToken = config.getRequired("USER_TOKEN")
    val conversation = mutableListOf<ChatMessage>()
    val httpClient = HttpClient.newHttpClient()

    println("Чат запущен. Введите сообщение и нажмите Enter.")
    println("Введите 'exit' или 'quit' для выхода.")

    while (true) {
        print("Вы: ")
        val prompt = readlnOrNull()?.trim()
            ?: break

        if (prompt.isEmpty()) {
            continue
        }

        if (prompt.equals("exit", ignoreCase = true) || prompt.equals("quit", ignoreCase = true)) {
            println("Чат завершён.")
            break
        }

        conversation += ChatMessage(role = USER_ROLE, content = prompt)

        val request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL_TEMPLATE.format(agentId)))
            .header("Content-Type", "application/json")
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
                    )
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
            error("Ошибка запроса к API. Статус ${response.statusCode()}: ${response.body()}")
        }

        val completion = json.decodeFromString<ChatCompletionResponse>(response.body())
        val content = completion.choices.firstOrNull()?.message?.content
            ?: error("Ответ API не содержит choices[0].message.content")

        conversation += ChatMessage(role = ASSISTANT_ROLE, content = content)
        writeOutputFile(conversation)
        println("Ассистент: $content")
    }
}

private fun loadConfig(): Properties {
    val configPath = Path.of(CONFIG_FILE)
    require(Files.exists(configPath)) {
        "Файл конфигурации $CONFIG_FILE не найден. Создайте его на основе config/app.properties.example."
    }

    return Properties().apply {
        Files.newInputStream(configPath).use(::load)
    }
}

private fun Properties.getRequired(key: String): String =
    getProperty(key)?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("В $CONFIG_FILE отсутствует обязательное свойство '$key'.")

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
    USER_ROLE -> "Пользователь"
    ASSISTANT_ROLE -> "Ассистент"
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
                print("\rАссистент думает$dots$padding")
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
