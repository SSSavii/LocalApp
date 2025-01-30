class LocalDataManager(private val context: Context) {
    private val hieroglyphsMap = mutableMapOf<String, List<String>>()

    init {
        loadData()
    }

    private fun loadData() {
        try {
            context.assets.open("hieroglyphs.txt").bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val (unicode, parts) = line.split(":")
                    hieroglyphsMap[unicode] = parts.split(",")
                }
            }
        } catch (e: IOException) {
            Log.e("LocalDataManager", "Error loading data", e)
        }
    }

    fun getAvailableGraphemes(selectedGraphemes: List<String>): List<String> {
        if (selectedGraphemes.isEmpty()) {
            return getAllInitialGraphemes()
        }

        val availableGraphemes = mutableSetOf<String>()

        hieroglyphsMap.forEach { (_, parts) ->
            if (selectedGraphemes.all { it in parts }) {
                parts.forEach { part ->
                    if (part !in selectedGraphemes) {
                        availableGraphemes.add(part)
                    }
                }
            }
        }

        return availableGraphemes.toList()
    }

    fun getHieroglyph(graphemes: List<String>): String? {
        return hieroglyphsMap.entries.find { (_, parts) ->
            parts.size == graphemes.size && parts.toSet() == graphemes.toSet()
        }?.key?.let { unicode ->
            String(Character.toChars(unicode.toInt(16)))
        }
    }

    private fun getAllInitialGraphemes(): List<String> {
        return hieroglyphsMap.values.flatten().distinct()
    }
}