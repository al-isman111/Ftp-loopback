package com.sandbox.ftptransfer.model

data class PortFolderConfig(
    val port: Int,
    var folderPath: String,
    var folderName: String,
    val enabled: Boolean = true
) {
    fun getDisplayName(): String {
        return "Port $port - $folderName"
    }
}

data class ReceiverSettings(
    val portMappings: Map<Int, String> = defaultMappings(),
    val autoCreateFolders: Boolean = true
) {
    companion object {
        fun defaultMappings(): Map<Int, String> {
            return mapOf(
                5152 to "Screenshots",
                5153 to "Documents", 
                5154 to "Downloads",
                5155 to "Camera",
                5156 to "Music"
            )
        }
    }
    
    fun getPortConfigs(): List<PortFolderConfig> {
        return portMappings.map { (port, folderName) ->
            PortFolderConfig(
                port = port,
                folderPath = "/$folderName/",
                folderName = folderName
            )
        }
    }
}
