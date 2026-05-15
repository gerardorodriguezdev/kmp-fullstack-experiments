#!/usr/bin/env kotlin

import java.io.File

fun selectEnvironment(directoryPath: String, environmentName: String) {
    val directory = File(directoryPath)

    if (!directory.exists()) {
        directory.mkdir()
    }

    val propertiesFile = File(directory, "properties.chamaleon.json")
    if (!propertiesFile.exists()) {
        propertiesFile.createNewFile()
        propertiesFile.writeText(
            //language=json
            """
            |{
            |    "selectedEnvironmentName": "$environmentName"
            |}
            """.trimMargin()
        )
    }
}

selectEnvironment("client/apps/home/environments", "local")
selectEnvironment("client/apps/user/core/environments", "local")
selectEnvironment("server/services/app/environments", "localMemoryDataSources")
selectEnvironment("server/services/mock/environments", "local")