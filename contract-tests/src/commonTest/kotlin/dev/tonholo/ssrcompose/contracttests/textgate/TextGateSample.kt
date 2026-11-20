package dev.tonholo.ssrcompose.contracttests.textgate

enum class TextGateSample(val id: String, val script: TextGateScript, val text: String) {
    LatinShort(id = "latin-short", script = TextGateScript.Latin, text = "Hello world"),
    LatinWrapping(
        id = "latin-wrapping",
        script = TextGateScript.Latin,
        text = "Pack my box with five dozen liquor jugs",
    ),
    LatinLong(
        id = "latin-long",
        script = TextGateScript.Latin,
        text = "The quick brown fox jumps over the lazy dog while the sun sets slowly behind the hills, " +
            "and the light spreads over the quiet water of the bay where small boats rest for the night.",
    ),
    GreekShort(id = "greek-short", script = TextGateScript.Greek, text = "Καλημέρα κόσμε"),
    GreekLong(
        id = "greek-long",
        script = TextGateScript.Greek,
        text = "Η γρήγορη καφέ αλεπού πηδά πάνω από το τεμπέλικο σκυλί, ενώ ο ήλιος δύει αργά πίσω από τα βουνά " +
            "και το φως απλώνεται στη θάλασσα.",
    ),
}
