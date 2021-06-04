import io.ktor.client.*
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.websocket.*
import io.ktor.client.request.*
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json

val client = HttpClient {
    install(JsonFeature) {
        serializer = KotlinxSerializer(Json { ignoreUnknownKeys = true })
    }
    install(WebSockets)
}

suspend fun fetchNotes(): List<Note> = coroutineScope {
    client.get("http://localhost:8000/notes.json")
}
