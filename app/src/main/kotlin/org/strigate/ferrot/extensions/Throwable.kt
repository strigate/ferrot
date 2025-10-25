package org.strigate.ferrot.extensions

fun Throwable.parseErrorMessage(): String? {
    val errorMessage = message ?: return null
    val pattern = Regex("""\[(\w+)]\s+([\w-]+):\s*([^\r\n]*)""")
    val match = pattern.find(errorMessage)
    val parsed = match?.groupValues?.get(3) ?: errorMessage
    val sentenceBoundary = Regex("""^.*?[.!?](?=\s|$)""")
    val primary = sentenceBoundary.find(parsed)?.value?.trim()
    return primary ?: parsed.trim()
}
