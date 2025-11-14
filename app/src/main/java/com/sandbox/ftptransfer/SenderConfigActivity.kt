package com.sandbox.ftptransfer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sandbox.ftptransfer.model.FolderMonitorConfig
import com.sandbox.ftptransfer.model.FileAction
import com.sandbox.ftptransfer.model.SenderSettings
import com.google.gson.Gson
import java.io.File

class SenderConfigActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnAddFolder: Button
    private lateinit var btnSave: Button
    private lateinit var btnBack: Button
    private lateinit var switchBackgroundService: Switch
    
    private val adapter = FolderConfigAdapter()
    private val configs = mutableListOf<FolderMonitorConfig>()
    private var selectedConfigIndex = -1
    
    private val settingsFile = "sender_settings.json"
    private val FOLDER_PICKER_REQUEST = 1002
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sender_config)
        
        initViews()
        loadSettings()
        setupClickListeners()
    }
    
    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewSender)
        btnAddFolder = findViewById(R.id.btnAddFolder)
        btnSave = findViewById(R.id.btnSaveSender)
        btnBack = findViewById(R.id.btnBackSender)
        switchBackgroundService = findViewById(R.id.switchBackgroundService)
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        
        adapter.onFolderSelectListener = { index ->
            selectedConfigIndex = index
            openFolderPicker()
        }
        
        adapter.onPortChangeListener = { index, newPort ->
            configs[index] = configs[index].copy(targetPort = newPort)
        }
        
        adapter.onActionChangeListener = { index, newAction ->
            configs[index] = configs[index].copy(fileAction = newAction)
        }
    }
    
    private fun loadSettings() {
        try {
            val file = File(filesDir, settingsFile)
            if (file.exists()) {
                val json = file.readText()
                val settings = Gson().fromJson(json, SenderSettings::class.java)
                
                configs.clear()
                configs.addAll(settings.monitoredFolders)
                switchBackgroundService.isChecked = settings.backgroundServiceEnabled
            } else {
                // Default configs
                configs.clear()
                configs.addAll(SenderSettings.defaultFolders())
                switchBackgroundService.isChecked = false
            }
            adapter.submitList(configs.toList())
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading settings", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupClickListeners() {
        btnAddFolder.setOnClickListener {
            val newConfig = FolderMonitorConfig(
                folderPath = "/NewFolder/",
                folderName = "NewFolder",
                targetPort = (configs.maxByOrNull { it.targetPort }?.targetPort ?: 5151) + 1,
                fileAction = FileAction.COPY
            )
            configs.add(newConfig)
            adapter.submitList(configs.toList())
        }
        
        btnSave.setOnClickListener {
            saveSettings()
        }
        
        btnBack.setOnClickListener {
            finish()
        }
    }
    
    private fun openFolderPicker() {
        if (selectedConfigIndex == -1) return
        
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        startActivityForResult(intent, FOLDER_PICKER_REQUEST)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == FOLDER_PICKER_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                updateSelectedFolder(uri)
            }
        }
    }
    
    private fun updateSelectedFolder(uri: Uri) {
        if (selectedConfigIndex == -1) return
        
        try {
            val folderName = getFolderNameFromUri(uri)
            val folderPath = uri.toString()
            
            configs[selectedConfigIndex] = configs[selectedConfigIndex].copy(
                folderPath = folderPath,
                folderName = folderName
            )
            
            adapter.submitList(configs.toList())
            Toast.makeText(this, "Folder selected: $folderName", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error selecting folder", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun getFolderNameFromUri(uri: Uri): String {
        return uri.lastPathSegment ?: "Unknown Folder"
    }
    
    private fun saveSettings() {
        try {
            val settings = SenderSettings(
                monitoredFolders = configs,
                backgroundServiceEnabled = switchBackgroundService.isChecked,
                adaptiveScanning = true
            )
            
            val json = Gson().toJson(settings)
            File(filesDir, settingsFile).writeText(json)
            
            Toast.makeText(this, "Sender settings saved!", Toast.LENGTH_SHORT).show()
            
            // Start/stop background service based on setting
            if (settings.backgroundServiceEnabled) {
                // TODO: Start background service
                Toast.makeText(this, "Background service will be started", Toast.LENGTH_SHORT).show()
            } else {
                // TODO: Stop background service  
                Toast.makeText(this, "Background service will be stopped", Toast.LENGTH_SHORT).show()
            }
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error saving settings", Toast.LENGTH_SHORT).show()
        }
    }
}

class FolderConfigAdapter : RecyclerView.Adapter<FolderConfigAdapter.ViewHolder>() {
    
    private var configs: List<FolderMonitorConfig> = emptyList()
    var onFolderSelectListener: ((Int) -> Unit)? = null
    var onPortChangeListener: ((Int, Int) -> Unit)? = null
    var onActionChangeListener: ((Int, FileAction) -> Unit)? = null
    
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFolder: TextView = itemView.findViewById(R.id.tvFolderSender)
        val btnSelectFolder: Button = itemView.findViewById(R.id.btnSelectFolderSender)
        val etPort: EditText = itemView.findViewById(R.id.etPortSender)
        val spinnerAction: Spinner = itemView.findViewById(R.id.spinnerAction)
        val switchEnabled: Switch = itemView.findViewById(R.id.switchEnabledSender)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_folder_config, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val config = configs[position]
        
        holder.tvFolder.text = "Folder: ${config.folderName}"
        holder.etPort.setText(config.targetPort.toString())
        holder.switchEnabled.isChecked = config.enabled
        
        // Setup action spinner
        val actions = FileAction.values().map { it.name }
        val adapter = ArrayAdapter(holder.itemView.context, android.R.layout.simple_spinner_item, actions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        holder.spinnerAction.adapter = adapter
        holder.spinnerAction.setSelection(config.fileAction.ordinal)
        
        holder.etPort.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val newPort = holder.etPort.text.toString().toIntOrNull() ?: config.targetPort
                onPortChangeListener?.invoke(position, newPort)
            }
        }
        
        holder.spinnerAction.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                val selectedAction = FileAction.values()[pos]
                onActionChangeListener?.invoke(position, selectedAction)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        
        holder.btnSelectFolder.setOnClickListener {
            onFolderSelectListener?.invoke(position)
        }
    }
    
    override fun getItemCount(): Int = configs.size
    
    fun submitList(newConfigs: List<FolderMonitorConfig>) {
        configs = newConfigs
        notifyDataSetChanged()
    }
}
