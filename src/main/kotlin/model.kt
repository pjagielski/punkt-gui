import kotlinx.serialization.Serializable

@Serializable
data class Note(
    val beat: Double,
    val duration: Double,
    val midinote: Int?,
    val amp: Double,
    val name: String
)

@Serializable
data class TickData(
    val bar: Long,
    val beat: Double,
    val millisPerBeat: Long
) {
    fun tickLength(targetStep: Double) = (millisPerBeat * targetStep).toInt()
}

fun isSynth(note: Note) = note.midinote != null

typealias NoteMap = Map<Double, Map<Int, Note>>

fun List<Note>.toSynthNames(): Set<String> =
    this.filter(::isSynth).map(Note::name).toSet()

data class NoteState(
    val notes: List<Note>,
    val synths: Set<String>,
) {

    fun noteMapFor(synth: String): NoteMap {
        val result = mutableMapOf<Double, MutableMap<Int, Note>>()
        this.notes.filter { it.name == synth }.forEach loop@ { note ->
            if (note.midinote == null) return@loop
            val m = result.getOrPut(note.beat) { mutableMapOf() }
            m[note.midinote] = note
        }
        return result
    }

    companion object {
        fun from(notes: List<Note>): NoteState {
            val synths = notes.toSynthNames()
            return NoteState(notes, synths)
        }
    }
}
