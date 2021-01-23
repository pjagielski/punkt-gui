import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.css.*
import kotlinx.html.js.onClickFunction
import react.RProps
import react.dom.tr
import react.functionalComponent
import react.useEffect
import react.useState
import styled.css
import styled.styledDiv
import styled.styledTable
import styled.styledTd

external interface NoteProps : RProps {
    var synth: String?
}

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

    useEffect(dependencies = listOf()) {
        reloadNotes()
        window.setInterval(::reloadNotes, 1500)
    }

    styledDiv {
        css {
            margin = "auto"
        }

        styledTable {
            css {
                marginBottom = 10.px
                marginTop = 10.px
                borderSpacing = 5.px
            }

            tr {
                styledTd {
                    css {
                        width = 50.px
                    }
                    +" "
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

        styledDiv {
            css {
                maxHeight = 800.px
                overflow = Overflow.auto
            }

            styledTable {
                css {
                    borderSpacing = 0.px
                }
                (96 downTo 24).forEach { midinote ->
                    tr {
                        styledTd {
                            css {
                                textAlign = TextAlign.center
                                fontSize = 14.px
                                width = 50.px
                            }
                            if (isC(midinote)) {
                                +"${findName(midinote)}${findOctave(midinote)}"
                            }
                        }
                        var beat = 0.0
                        while (beat < 8.0) {
                            val note = selectedSynth?.let { notes.noteMapFor(it)[beat]?.get(midinote) }
                            val span = note?.duration?.let { (it * 4).toInt() } ?: 1
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
