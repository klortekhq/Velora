package com.klortek.velora.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button as MobileButton
import androidx.compose.material3.Text as MobileText
import com.klortek.velora.components.TvTextField
import com.klortek.velora.ui.DeviceUtils
import com.klortek.velora.jellyfin.JellyfinAuthService
import com.klortek.velora.jellyfin.JellyfinConfig
import com.klortek.velora.jellyfin.ServerDiscovery
import com.klortek.velora.security.SensitiveDataRedactor

@Composable
fun ServerEntryScreen(
    onServerConnected: (String) -> Unit,
    prefillAddress: String? = null
) {
    val context = LocalContext.current
    val config = remember { JellyfinConfig(context) }
    
    var serverAddress by remember { mutableStateOf(prefillAddress ?: config.serverUrl.ifEmpty { "" }) }
    var isConnecting by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var discoveredServers by remember { mutableStateOf<List<ServerDiscovery.LocalServer>>(emptyList()) }
    
    val addressFocusRequester = remember { FocusRequester() }
    val connectButtonFocusRequester = remember { FocusRequester() }
    val autoDetectButtonFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var addressFocused by remember { mutableStateOf(false) }
    var connectButtonFocused by remember { mutableStateOf(false) }
    var autoDetectButtonFocused by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    
    // Auto-connect if address is prefilled
    LaunchedEffect(prefillAddress) {
        if (prefillAddress != null && prefillAddress.isNotBlank()) {
            isConnecting = true
            connectToServer(
                address = serverAddress,
                context = context,
                config = config,
                scope = coroutineScope,
                onStatusUpdate = { status -> statusMessage = status }
            ) { success, message ->
                isConnecting = false
                statusMessage = null
                if (success) {
                    onServerConnected(config.serverUrl) // Use discovered URL
                } else {
                    errorMessage = message
                }
            }
        } else {
            addressFocusRequester.requestFocus()
        }
    }
    
    fun connect() {
        if (serverAddress.isBlank()) {
            errorMessage = context.getString(com.klortek.velora.R.string.server_entry_empty_address)
            return
        }
        
        isConnecting = true
        errorMessage = null
        statusMessage = context.getString(com.klortek.velora.R.string.server_entry_discovering)
        
        connectToServer(
            address = serverAddress,
            context = context,
            config = config,
            scope = coroutineScope,
            onStatusUpdate = { status -> statusMessage = status }
        ) { success, message ->
            isConnecting = false
            statusMessage = null
            if (success) {
                onServerConnected(config.serverUrl) // Use discovered URL
            } else {
                errorMessage = message
            }
        }
    }
    
    fun autoDetect() {
        isScanning = true
        errorMessage = null
        statusMessage = context.getString(com.klortek.velora.R.string.server_entry_scanning)
        discoveredServers = emptyList()
        
        coroutineScope.launch {
            val servers = ServerDiscovery.discoverLocalServers { server ->
                // Real-time update as servers are found
                discoveredServers = discoveredServers + server
            }
            
            isScanning = false
            
            if (servers.isEmpty()) {
                statusMessage = null
                errorMessage = context.getString(com.klortek.velora.R.string.server_entry_no_servers)
            } else if (servers.size == 1) {
                // Auto-fill if only one server found
                serverAddress = servers.first().address
                statusMessage = context.getString(com.klortek.velora.R.string.server_entry_found_one, servers.first().name)
            } else {
                statusMessage = context.getString(com.klortek.velora.R.string.server_entry_found_many, servers.size)
            }
        }
    }
    
    val isTv = remember(context) { DeviceUtils.isTvDevice(context) }
    val widthFraction = if (isTv) 0.5f else 0.9f
    val horizontalPadding = if (isTv) 48.dp else 24.dp
    val verticalPadding = if (isTv) 27.dp else 16.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = if (isTv) Alignment.TopStart else Alignment.Center
    ) {
        // Left side content area (50% width on TV, centered on mobile)
        Column(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            Text(
                text = stringResource(com.klortek.velora.R.string.server_entry_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Label
            Text(
                text = stringResource(com.klortek.velora.R.string.server_entry_valid_address),
                style = MaterialTheme.typography.bodyMedium,
                color = if (addressFocused)
                    Color(0xFF9C27B0) // Purple label when focused for better visibility
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp)
            )
            
            // Address input field
            TvTextField(
                value = serverAddress,
                onValueChange = { serverAddress = it },
                label = stringResource(com.klortek.velora.R.string.server_entry_address),
                enabled = !isConnecting && prefillAddress == null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done,
                    autoCorrectEnabled = false
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        if (serverAddress.isNotBlank()) {
                            connectButtonFocusRequester.requestFocus()
                            connect()
                        }
                    }
                ),
                focusRequester = addressFocusRequester,
                onFocusChanged = { addressFocused = it },
                modifier = Modifier.fillMaxWidth()
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isTv) {
                    Button(
                        onClick = { connect() },
                        enabled = !isConnecting && !isScanning && prefillAddress == null,
                        modifier = Modifier
                            .focusRequester(connectButtonFocusRequester)
                            .onFocusChanged { connectButtonFocused = it.isFocused }
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyUp && keyEvent.key == Key.Enter && !isConnecting && !isScanning && prefillAddress == null) {
                                    connect()
                                    true
                                } else {
                                    false
                                }
                            },
                        colors = ButtonDefaults.colors(
                            containerColor = if (connectButtonFocused) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.primaryContainer,
                            contentColor = if (connectButtonFocused)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Text(
                            text = stringResource(com.klortek.velora.R.string.server_entry_connect),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    
                    // Auto Detect button
                    Button(
                        onClick = { autoDetect() },
                        enabled = !isConnecting && !isScanning && prefillAddress == null,
                        modifier = Modifier
                            .focusRequester(autoDetectButtonFocusRequester)
                            .onFocusChanged { autoDetectButtonFocused = it.isFocused }
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyUp && keyEvent.key == Key.Enter && !isConnecting && !isScanning && prefillAddress == null) {
                                    autoDetect()
                                    true
                                } else {
                                    false
                                }
                            },
                        colors = ButtonDefaults.colors(
                            containerColor = if (autoDetectButtonFocused) 
                                MaterialTheme.colorScheme.secondary
                            else 
                                MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = if (autoDetectButtonFocused)
                                MaterialTheme.colorScheme.onSecondary
                            else
                                MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text(
                        text = if (isScanning) stringResource(com.klortek.velora.R.string.server_entry_searching) else stringResource(com.klortek.velora.R.string.server_entry_detect),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                } else {
                    MobileButton(
                        onClick = { connect() },
                        enabled = !isConnecting && !isScanning && prefillAddress == null,
                        modifier = Modifier.weight(1f)
                    ) {
                        MobileText(
                            text = stringResource(com.klortek.velora.R.string.server_entry_connect),
                            style = androidx.compose.material3.MaterialTheme.typography.labelLarge
                        )
                    }
                    
                    MobileButton(
                        onClick = { autoDetect() },
                        enabled = !isConnecting && !isScanning && prefillAddress == null,
                        modifier = Modifier.weight(1f)
                    ) {
                        MobileText(
                            text = if (isScanning) stringResource(com.klortek.velora.R.string.server_entry_searching) else stringResource(com.klortek.velora.R.string.server_entry_detect),
                            style = androidx.compose.material3.MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
            
            // Status/Error message
            if (statusMessage != null) {
                Text(
                    text = statusMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            // Discovered servers list
            if (discoveredServers.isNotEmpty()) {
                Text(
                    text = stringResource(com.klortek.velora.R.string.server_entry_found_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    discoveredServers.forEach { server ->
                        var serverItemFocused by remember { mutableStateOf(false) }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (serverItemFocused) 
                                        MaterialTheme.colorScheme.primaryContainer
                                    else 
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    serverAddress = server.address
                                    discoveredServers = emptyList()
                                    statusMessage = context.getString(com.klortek.velora.R.string.server_entry_selected, server.name)
                                }
                                .onFocusChanged { serverItemFocused = it.isFocused }
                                .focusable()
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = server.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (serverItemFocused)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = server.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (serverItemFocused)
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Connect to a Jellyfin server using smart discovery.
 * Handles reverse proxies, subpaths, and various configurations automatically.
 * 
 * Supports:
 * ✅ Reverse proxies (Nginx, Caddy, Traefik, Cloudflare)
 * ✅ HTTPS-only servers
 * ✅ Subpath installs (/jellyfin)
 * ✅ Root installs (/)
 * ✅ Non-standard ports
 * ✅ Path-rewriting proxies
 * ✅ Custom domain hosting (orbyt.link, duckdns, etc.)
 */
private fun connectToServer(
    address: String,
    context: android.content.Context,
    config: JellyfinConfig,
    scope: kotlinx.coroutines.CoroutineScope,
    onStatusUpdate: ((String) -> Unit)? = null,
    onResult: (Boolean, String) -> Unit
) {
    scope.launch {
        try {
            if (address.isBlank()) {
                onResult(false, context.getString(com.klortek.velora.R.string.server_entry_empty_address))
                return@launch
            }
            
            onStatusUpdate?.invoke(context.getString(com.klortek.velora.R.string.server_entry_discovering))
            android.util.Log.d("ServerEntry", "Starting server discovery for: $address")
            
            // Use smart discovery to find the server
            val discoveredUrl = ServerDiscovery.discoverServer(address)
            
            if (discoveredUrl != null) {
                android.util.Log.i("ServerEntry", "✅ Found server at: $discoveredUrl")
                
                // Save the discovered URL (this is the working URL)
                config.serverUrl = discoveredUrl
                
                onStatusUpdate?.invoke(context.getString(com.klortek.velora.R.string.server_entry_connected))
                onResult(true, "")
            } else {
                android.util.Log.w("ServerEntry", "❌ Server discovery failed for: $address")
                
                onResult(false, context.getString(com.klortek.velora.R.string.server_entry_connect_error))
            }
        } catch (e: Exception) {
            android.util.Log.e("ServerEntry", "Error during server discovery: ${SensitiveDataRedactor.message(e)}")
            onResult(false, "Error: ${e::class.simpleName ?: e.javaClass.simpleName}")
        }
    }
}

