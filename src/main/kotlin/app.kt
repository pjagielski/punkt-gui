import io.ktor.client.features.websocket.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.css.*
import kotlinx.html.TBODY
import kotlinx.html.TD
import kotlinx.html.TR
import kotlinx.html.js.onClickFunction
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import react.*
import react.dom.RDOMBuilder
import react.dom.tbody
import react.dom.tr
import styled.*

val NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

val NOTE_COLORS = arrayOf(
    Color("#FA0B0C"), // red
    Color("#F44712"), // red-orange
    Color("#F88010"), // orange
    Color("#F5D23B"), // orange-yellow
    Color("#F5F43C"), // yellow
    Color("#149033"), // green
    Color("#1B9081"), // green-blue
    Color("#1C0D82"), // blue
    Color("#4B0E7D"), // blue-purple
    Color("#7F087C"), // purple
    Color("#A61586"), // purple-violet
    Color("#D71285")  // violet
)

fun findColor(note: Int) = NOTE_COLORS[note % 12]

fun findOctave(note: Int) = note / 12

fun findName(note: Int) = NOTE_NAMES[note % 12]

fun isSharp(note: Int) = findName(note).contains("#")

fun isC(note: Int) = note % 12 == 0

private val scope = MainScope()

object Colors {
    val lightGray = Color("#cccccc")
    val gray = Color("#a7a7a7")
    val darkGray = Color("#878787")
}


private fun RDOMBuilder<TBODY>.gridRow(header: String = "", block: RDOMBuilder<TR>.() -> Unit) {
    tr {
        styledTd {
            css {
                textAlign = TextAlign.center
                fontSize = 14.px
                width = 50.px
            }
            + header
        }
        this.block()
    }
}

val Ticker = functionalComponent<RProps> {
    val (tickData, setTickData) = useState(TickData(0, 0.0, 1000))

    useEffect(dependencies = emptyList()) {
        scope.launch {
            client.ws(method = HttpMethod.Get, host = "127.0.0.1", port = 8000, path = "/tick") {
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()
                            val newTickData = Json.decodeFromString<TickData>(text)
                            setTickData(newTickData)
                            // we get tick every beat, so need to fill remaining gaps manually
                            (1..3).forEach { i ->
                                scope.launch {
                                    val delayToNextStep = newTickData.tickLength(0.25) * i
                                    delay(delayToNextStep.toLong())
                                    setTickData(newTickData.copy(beat = newTickData.beat + (0.25 * i)))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    styledTable {
        css {
            marginBottom = 10.px
            marginTop = 10.px
            borderSpacing = 5.px
        }

        tbody {
            tr {
                styledTd {
                    css {
                        width = 35.px
                    }
                    + ""
                }
                styledTd {
                    css {
                        width = 100.px
                        fontSize = 20.px
                    }
                    + "${tickData.bar} / ${tickData.beat}"
                }
            }
        }
    }

    styledTable {
        css {
            borderSpacing = 0.px
            marginBottom = 10.px
        }

        tbody {
            gridRow {
                var beat = 0.0
                val currentBeat = tickData.beat
                val step = 0.25
                while (beat < 8.0) {
                    val rowBackground = when {
                        beat == currentBeat -> Colors.gray
                        else -> Colors.lightGray
                    }
                    styledTd {
                        css {
                            width = 35.px
                            height = 15.px
                            backgroundColor = rowBackground
                            border = "1px solid ${Colors.darkGray.value}"
                            if (beat % 1.0 == 0.0) {
                                borderLeft = "2px solid ${Colors.darkGray.value}"
                            }
                        }
                        attrs {
                            colSpan = "$span"
                        }
                        +" "
                    }
                    beat += step
                }
            }
        }
    }
}

val App = functionalComponent<RProps> { _ ->
    val (notes, setNotes) = useState(NoteState.from(emptyList()))
    val (synth, setSynth) = useState<String?>(null)
    val selectedSynth = when {
        synth != null && notes.synths.contains(synth) -> synth
        else -> notes.synths.firstOrNull()
    }

    fun reloadNotes() {
        scope.launch {
            val currentState = NoteState.from(fetchNotes())
            setNotes(currentState)
        }
    }

    fun schedulePing(ws: WebSocketSession) {
        scope.launch {
            delay(10000)
            ws.send(Frame.Text("ping"))
            schedulePing(ws)
        }
    }

    useEffect(dependencies = emptyList()) {
        reloadNotes()
        scope.launch {
            client.ws(method = HttpMethod.Get, host = "127.0.0.1", port = 8000, path = "/notes") {
                schedulePing(this)
                try {
                    for (frame in incoming) {
                        println("Got frame!!")
                        when (frame) {
                            is Frame.Text -> {
                                println("Got notes!!")
                                val text = frame.readText()
                                val format = Json { ignoreUnknownKeys = true }
                                val newNotes = format.decodeFromString<List<Note>>(text)
                                setNotes(NoteState.from(newNotes))
                            }
                        }
                    }
                } catch (e: ClosedReceiveChannelException) {
                    println("onClose ${closeReason.await()}")
                } catch (e: Throwable) {
                    println("onError ${closeReason.await()}")
                    e.printStackTrace()
                }
            }
        }
    }

    styledDiv {
        css {
            margin = "auto"
            fontFamily = "Roboto"
        }

        styledTable {
            css {
                marginBottom = 10.px
                marginTop = 10.px
                borderSpacing = 5.px
            }

            tbody {
                tr {
                    styledTd {
                        css {
                            width = 35.px
                        }
                        + " "
                    }

                    styledTd {
                        css {
                            width = 125.px
                            fontSize = 24.px
                        }
                        + "● punkt"
                    }

                    notes.synths.forEach { synthName ->
                        styledTd {
                            css {
                                textAlign = TextAlign.center
                                cursor = Cursor.pointer
                                padding = "5px"
                                marginLeft = 15.px
                                width = 75.px
                                backgroundColor = when {
                                    selectedSynth == synthName -> Colors.gray
                                    else -> Colors.lightGray
                                }
                            }
                            attrs {
                                onClickFunction = {
                                    setSynth(synthName)
                                }
                            }
                            +synthName
                        }
                    }
                }
            }
        }

        child(Ticker)

        styledDiv {
            css {
                maxHeight = 800.px
                overflow = Overflow.auto
            }

            styledTable {
                css {
                    borderSpacing = 0.px
                }

                tbody {
                    (84 downTo 24).forEach { midinote ->
                        val header = if (isC(midinote)) "${findName(midinote)}${findOctave(midinote)}" else ""
                        gridRow(header) {
                            var beat = 0.0
                            while (beat < 8.0) {
                                val note = selectedSynth?.let { notes.noteMapFor(it)[beat]?.get(midinote) }
                                val span = note?.duration?.let { (it * (1/0.25)).toInt() } ?: 1
                                val step = note?.duration ?: 0.25
                                val rowBackground = when {
                                    isSharp(midinote) -> Colors.gray
                                    else -> Colors.lightGray
                                }
                                styledTd {
                                    css {
                                        width = 35.px
                                        height = 15.px
                                        backgroundColor = note?.midinote?.let(::findColor) ?: rowBackground
                                        border = "1px solid ${Colors.darkGray.value}"
                                        if (isC(midinote)) {
                                            borderBottom = "2px solid ${Colors.darkGray.value}"
                                        }
                                        if (beat % 1.0 == 0.0) {
                                            borderLeft = "2px solid ${Colors.darkGray.value}"
                                        }
                                    }
                                    attrs {
                                        colSpan = "$span"
                                    }
                                    +" "
                                }
                                beat += step
                            }
                        }
                    }
                }
            }
        }
    }
}
