import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.coroutineScope

suspend fun fetchNotes(): List<Note> = coroutineScope {
    window.fetch("http://localhost:8000/notes.json")
        .await()
        .json()
        .await()
        .unsafeCast<Array<Note>>()
        .toList()
}
