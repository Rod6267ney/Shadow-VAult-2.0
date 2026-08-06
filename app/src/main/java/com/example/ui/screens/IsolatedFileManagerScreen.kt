package com.example.ui.screens

import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.utils.ShizukuUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RemoteFile(val name: String, val isDirectory: Boolean, val size: String, val path: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IsolatedFileManagerScreen(userId: String, packageName: String, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var currentPath by remember { mutableStateOf("/data/user/$userId/$packageName") }
    var files by remember { mutableStateOf<List<RemoteFile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    fun loadFiles(path: String) {
        isLoading = true
        coroutineScope.launch {
            try {
                val output = ShizukuUtils.executeCommand("ls -l $path")
                val newFiles = mutableListOf<RemoteFile>()
                
                if (!output.contains("No such file or directory") && !output.contains("Permission denied")) {
                    output.lines().forEach { line ->
                        if (line.isNotEmpty() && !line.startsWith("total")) {
                            val parts = line.split(Regex("\\s+"))
                            if (parts.size >= 8) {
                                val isDir = parts[0].startsWith("d")
                                val size = parts[4]
                                val name = parts.subList(7, parts.size).joinToString(" ")
                                newFiles.add(RemoteFile(name, isDir, size, "$path/$name"))
                            }
                        }
                    }
                }
                files = newFiles
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_SHORT).show() }
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(currentPath) {
        loadFiles(currentPath)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Arquivos Isolados", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { loadFiles(currentPath) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            
            Text("Caminho atual:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(currentPath, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 16.dp))
            
            if (currentPath != "/data/user/$userId/$packageName") {
                Button(onClick = { 
                    val parentPath = currentPath.substringBeforeLast("/")
                    if (parentPath.isNotEmpty()) currentPath = parentPath 
                }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Text("Subir Diretório (..)")
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (files.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Diretório vazio ou sem permissão", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(files) { file ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            onClick = {
                                if (file.isDirectory) {
                                    currentPath = file.path
                                } else {
                                    // TODO: Implementar download/exportação do arquivo para o Host (Downloads)
                                    coroutineScope.launch {
                                        val destDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
                                        val destPath = "$destDir/${file.name}"
                                        val cmd = "cp '${file.path}' '$destPath'"
                                        val res = ShizukuUtils.executeCommand(cmd)
                                        withContext(Dispatchers.Main) {
                                            if (res.contains("Error") || res.contains("Exception")) {
                                                 Toast.makeText(context, "Erro ao copiar: $res", Toast.LENGTH_SHORT).show()
                                            } else {
                                                 Toast.makeText(context, "Arquivo copiado para Downloads!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            }
                        ) {
                            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (file.isDirectory) Icons.Filled.Folder else Icons.Filled.Description,
                                    contentDescription = null,
                                    tint = if (file.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(file.name, style = MaterialTheme.typography.bodyLarge)
                                    if (!file.isDirectory) {
                                        Text("${file.size} bytes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                if (!file.isDirectory) {
                                     Icon(Icons.Filled.Upload, contentDescription = "Exportar", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
