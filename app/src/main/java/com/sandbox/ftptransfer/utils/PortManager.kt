package com.sandbox.ftptransfer.utils

import com.sandbox.ftptransfer.model.TransferConfig
import java.net.ServerSocket
import java.net.Socket

object PortManager {
    private val config = TransferConfig()
    private val serverSockets = mutableMapOf<Int, ServerSocket>()
    private val clientSockets = mutableMapOf<Int, Socket>()
    
    fun initializeServerSockets(): Boolean {
        try {
            config.channelPorts.forEach { port ->
                val serverSocket = ServerSocket(port)
                serverSockets[port] = serverSocket
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            closeAllSockets()
            return false
        }
    }
    
    fun getServerSocket(port: Int): ServerSocket? {
        return serverSockets[port]
    }
    
    fun getServerSocketForChannel(channel: Int): ServerSocket? {
        val port = config.getPortForChannel(channel)
        return serverSockets[port]
    }
    
    fun connectToServer(port: Int): Socket? {
        return try {
            val socket = Socket("127.0.0.1", port)
            clientSockets[port] = socket
            socket
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun connectToServerForChannel(channel: Int): Socket? {
        val port = config.getPortForChannel(channel)
        return connectToServer(port)
    }
    
    fun closeAllSockets() {
        serverSockets.values.forEach { socket ->
            try {
                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        serverSockets.clear()
        
        clientSockets.values.forEach { socket ->
            try {
                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        clientSockets.clear()
    }
    
    fun getAvailablePorts(): List<Int> {
        return config.channelPorts
    }
    
    fun getChannelForFolder(folderName: String): Int {
        // Simple hash-based channel assignment
        val hash = folderName.hashCode()
        return Math.abs(hash) % config.totalChannels
    }
}
