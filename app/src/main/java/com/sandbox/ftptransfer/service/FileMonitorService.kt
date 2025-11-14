package com.sandbox.ftptransfer.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.sandbox.ftptransfer.utils.PortManager
import kotlinx.coroutines.*
import java.io.*
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class FileMonitorService : Service() {
    
    private val TAG = "FileMonitorService"
    private val isRunning = AtomicBoolean(false)
    private var monitorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val processedFiles = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "FileMonitorService created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning.getAndSet(true)) {
            startFileMonitoring()
        }
        return START_STICKY
    }
    
    private fun startFileMonitoring() {
        monitorJob = scope.launch {
            Log.d(TAG, "Starting file monitoring service...")
            
            // Monitor multiple source directories
            val sourceDirs = listOf(
                File(getExternalFilesDir(null), "screenshots"),
                File(getExternalFilesDir(null), "documents"),
                File(getExternalFilesDir(null), "downloads"),
                File(getExternalFilesDir(null), "camera")
            )
            
            // Create source directories if they don't exist
            sourceDirs.forEach { dir ->
                if (!dir.exists()) {
                    dir.mkdirs()
                    Log.d(TAG, "Created directory: ${dir.absolutePath}")
                }
            }
            
            while (isRunning.get()) {
                try {
                    sourceDirs.forEach { sourceDir ->
                        monitorDirectory(sourceDir)
                    }
                    
                    // Wait 2 seconds before next scan
                    delay(2000)
                    
                } catch (e: Exception) {
                    if (isRunning.get()) {
                        Log.e(TAG, "Error in file monitoring: ${e.message}")
                        delay(5000) // Wait longer if there's an error
                    }
                }
            }
        }
    }
    
    private suspend fun monitorDirectory(directory: File) {
        if (!directory.exists() || !directory.isDirectory) return
        
        val files = directory.listFiles() ?: return
        
        for (file in files) {
            if (file.isFile && !processedFiles.contains(file.absolutePath)) {
                // Wait for file to be completely written (2 second delay)
                delay(2000)
                
                if (isFileReady(file)) {
                    processedFiles.add(file.absolutePath)
                    scope.launch {
                        sendFileToReceiver(file, directory.name)
                    }
                }
            }
        }
        
        // Clean up processed files set to prevent memory leak
        if (processedFiles.size > 1000) {
            processedFiles.clear()
        }
    }
    
    private suspend fun isFileReady(file: File): Boolean {
        return try {
            val initialSize = file.length()
            delay(1000)
            val finalSize = file.length()
            initialSize == finalSize && initialSize > 0
        } catch (e: Exception) {
            false
        }
    }
    
    private suspend fun sendFileToReceiver(file: File, folderName: String) {
        Log.d(TAG, "Attempting to send file: ${file.name} from folder: $folderName")
        
        // Determine channel based on folder name
        val channel = PortManager.getChannelForFolder(folderName)
        val socket = PortManager.connectToServerForChannel(channel)
        
        if (socket == null) {
            Log.e(TAG, "Failed to connect to server on channel $channel")
            processedFiles.remove(file.absolutePath)
            return
        }
        
        try {
            val outputStream = socket.getOutputStream()
            val inputStream = socket.getInputStream()
            val dataOutputStream = DataOutputStream(outputStream)
            val dataInputStream = DataInputStream(inputStream)
            
            // Send file metadata
            dataOutputStream.writeUTF(file.name)
            dataOutputStream.writeLong(file.length())
            dataOutputStream.writeInt(channel)
            dataOutputStream.flush()
            
            // Send file data
            val fileInputStream = FileInputStream(file)
            val buffer = ByteArray(8192)
            var read: Int
            
            while (fileInputStream.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
            }
            
            outputStream.flush()
            fileInputStream.close()
            
            // Wait for response
            val success = dataInputStream.readBoolean()
            val message = dataInputStream.readUTF()
            
            if (success) {
                Log.d(TAG, "File sent successfully: ${file.name} - $message")
                
                // Optionally delete the file after successful transfer
                // file.delete()
                // Log.d(TAG, "Source file deleted: ${file.name}")
                
            } else {
                Log.e(TAG, "File transfer failed: ${file.name} - $message")
                processedFiles.remove(file.absolutePath) // Retry later
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending file ${file.name}: ${e.message}")
            processedFiles.remove(file.absolutePath) // Retry later
        } finally {
            try {
                socket.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing socket: ${e.message}")
            }
        }
    }
    
    override fun onDestroy() {
        Log.d(TAG, "Stopping file monitoring service...")
        isRunning.set(false)
        monitorJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }
}