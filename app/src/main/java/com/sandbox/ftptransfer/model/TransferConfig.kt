package com.sandbox.ftptransfer.model

data class TransferConfig(
    val basePort: Int = 5152,
    val totalChannels: Int = 10,
    val fileMonitorDelay: Long = 2000, // 2 seconds
    val bufferSize: Int = 8192
) {
    val channelPorts: List<Int>
        get() = (0 until totalChannels).map { basePort + it }
    
    fun getPortForChannel(channel: Int): Int {
        require(channel in 0 until totalChannels) { "Channel must be between 0 and ${totalChannels - 1}" }
        return basePort + channel
    }
    
    fun getChannelForPort(port: Int): Int {
        require(port in basePort until basePort + totalChannels) { "Port $port is not in configured range" }
        return port - basePort
    }
}

data class FileTransferRequest(
    val fileName: String,
    val fileSize: Long,
    val channel: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class FileTransferResponse(
    val success: Boolean,
    val message: String,
    val fileName: String,
    val timestamp: Long = System.currentTimeMillis()
)
