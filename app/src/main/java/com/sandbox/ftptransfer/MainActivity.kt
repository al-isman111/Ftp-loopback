package com.sandbox.ftptransfer

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import com.sandbox.ftptransfer.service.FileMonitorService
import com.sandbox.ftptransfer.service.LoopbackServer

class MainActivity : AppCompatActivity() {
    
    private lateinit var switchMode: Switch
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnReceiverConfig: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvLog: TextView
    
    private var isReceiverMode = true
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        setupClickListeners()
        updateModeDisplay()
    }
    
    private fun initViews() {
        switchMode = findViewById(R.id.switchMode)
        btnStart = findViewById(R.id.btnStartService)
        btnStop = findViewById(R.id.btnStopService)
        btnReceiverConfig = findViewById(R.id.btnReceiverConfig)
        tvStatus = findViewById(R.id.tvStatus)
        tvLog = findViewById(R.id.tvLog)
    }
    
    private fun setupClickListeners() {
        switchMode.setOnCheckedChangeListener { _, isChecked ->
            isReceiverMode = !isChecked
            updateModeDisplay()
        }
        
        btnStart.setOnClickListener {
            startServices()
        }
        
        btnStop.setOnClickListener {
            stopServices()
        }
        
        btnReceiverConfig.setOnClickListener {
            if (isReceiverMode) {
                val intent = Intent(this, ReceiverConfigActivity::class.java)
                startActivity(intent)
            } else {
                // TODO: Add sender port configuration
                logMessage("Sender port configuration coming soon")
            }
        }
    }
    
    private fun updateModeDisplay() {
        val modeText = if (isReceiverMode) "Receiver" else "Sender"
        switchMode.text = "$modeText Mode"
        tvStatus.text = "Status: Stopped - $modeText Mode"
        
        // Show/hide receiver config button
        btnReceiverConfig.text = if (isReceiverMode) "Configure Receiver" else "Configure Sender"
    }
    
    private fun startServices() {
        if (isReceiverMode) {
            // Start receiver (server)
            val intent = Intent(this, LoopbackServer::class.java)
            startService(intent)
            logMessage("Receiver service started")
        } else {
            // Start sender (file monitor)
            val intent = Intent(this, FileMonitorService::class.java)
            startService(intent)
            logMessage("Sender service started")
        }
        tvStatus.text = "Status: Running - ${if (isReceiverMode) "Receiver" else "Sender"} Mode"
    }
    
    private fun stopServices() {
        if (isReceiverMode) {
            val intent = Intent(this, LoopbackServer::class.java)
            stopService(intent)
            logMessage("Receiver service stopped")
        } else {
            val intent = Intent(this, FileMonitorService::class.java)
            stopService(intent)
            logMessage("Sender service stopped")
        }
        tvStatus.text = "Status: Stopped - ${if (isReceiverMode) "Receiver" else "Sender"} Mode"
    }
    
    private fun logMessage(message: String) {
        runOnUiThread {
            val currentText = tvLog.text.toString()
            tvLog.text = "$currentText\n$message"
        }
    }
}
