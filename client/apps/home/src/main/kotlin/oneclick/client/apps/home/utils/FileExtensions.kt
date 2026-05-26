package oneclick.client.apps.home.utils

import java.io.File

internal fun File.createFileIfNotExists() {
    if (!exists()) {
        createNewFile()
    }
}

internal fun File.createDirectoryIfNotExists() {
    if (!exists()) {
        mkdir()
    }
}