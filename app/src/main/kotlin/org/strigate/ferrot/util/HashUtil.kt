package org.strigate.ferrot.util

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.Locale

fun sha256(filePath: String): String? {
    val file = File(filePath)
    if (!file.exists() || !file.isFile) {
        return null
    }
    val messageDigest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    FileInputStream(file).use { input ->
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) {
                break
            }
            messageDigest.update(buffer, 0, read)
        }
    }
    return messageDigest.digest().joinToString("") { "%02x".format(Locale.ROOT, it) }
}
