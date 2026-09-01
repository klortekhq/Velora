package com.klortek.velora.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.klortek.velora.jellyseerr.JellyseerrApiService
import com.klortek.velora.jellyseerr.JellyseerrImageUrl
import com.klortek.velora.jellyseerr.JellyseerrMovie
import com.klortek.velora.jellyseerr.JellyseerrTvShow
import com.klortek.velora.R
import kotlinx.coroutines.launch

import com.klortek.velora.ui.DeviceUtils

@Composable
fun JellyseerrDetailsScreen(
    tmdbId: Int,
    mediaType: String,
    apiService: JellyseerrApiService?,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isTv = remember(context) { DeviceUtils.isTvDevice(context) }
    
    var movieDetails by remember { mutableStateOf<JellyseerrMovie?>(null) }
    var tvDetails by remember { mutableStateOf<JellyseerrTvShow?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isRequesting by remember { mutableStateOf(false) }
    
    // Fetch details
    LaunchedEffect(tmdbId, mediaType, apiService) {
        if (apiService == null) {
            error = context.getString(com.klortek.velora.R.string.jellyseerr_service_unavailable)
            isLoading = false
            return@LaunchedEffect
        }
        
        isLoading = true
        error = null
        
        try {
            if (mediaType == "movie") {
                val details = apiService.getMovieDetails(tmdbId)
                if (details != null) {
                    movieDetails = details
                } else {
                    error = context.getString(com.klortek.velora.R.string.jellyseerr_movie_details_failed)
                }
            } else {
                val details = apiService.getTvShowDetails(tmdbId)
                if (details != null) {
                    tvDetails = details
                } else {
                    error = context.getString(com.klortek.velora.R.string.jellyseerr_tv_details_failed)
                }
            }
        } catch (e: Exception) {
            error = "Error: ${e.message}"
        } finally {
            isLoading = false
        }
    }
    
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(com.klortek.velora.R.string.jellyseerr_loading_details), color = Color.White)
        }
        return
    }
    
    if (error != null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = error ?: "Unknown error", color = Color.Red)
                Spacer(modifier = Modifier.height(16.dp))
                if (isTv) {
                    Button(onClick = onBackPressed) {
                        Text(stringResource(com.klortek.velora.R.string.jellyseerr_go_back))
                    }
                } else {
                    androidx.compose.material3.Button(onClick = onBackPressed) {
                        androidx.compose.material3.Text(stringResource(com.klortek.velora.R.string.jellyseerr_go_back))
                    }
                }
            }
        }
        return
    }
    
    // Extract display data
    val title = movieDetails?.title ?: tvDetails?.name ?: stringResource(com.klortek.velora.R.string.jellyseerr_unknown_title)
    val overview = movieDetails?.overview ?: tvDetails?.overview ?: stringResource(com.klortek.velora.R.string.jellyseerr_no_overview)
    val year = (movieDetails?.releaseDate ?: tvDetails?.firstAirDate)?.take(4) ?: ""
    val backdropPath = movieDetails?.backdropPath ?: tvDetails?.backdropPath
    val posterPath = movieDetails?.posterPath ?: tvDetails?.posterPath
    val mediaInfo = movieDetails?.mediaInfo ?: tvDetails?.mediaInfo
    
    // Status logic
    // 1=Pending, 2=Approved, 3=Declined, 4=Partially Available, 5=Available
    val status = mediaInfo?.status ?: 0 // 0 = Unknown/Not Requested
    val isAvailable = status == 5 || status == 4
    val isPending = status == 1 || status == 2
    
    val backdropUrl = JellyseerrImageUrl.backdrop(backdropPath)
    val posterUrl = JellyseerrImageUrl.poster(posterPath)
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Background Image
        if (backdropUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(backdropUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.4f }
            )
            
            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.9f),
                                Color.Black.copy(alpha = 0.7f),
                                Color.Transparent
                            )
                        )
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.DarkGray)
            )
        }
        
        // Content
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isTv) 48.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Poster
            if (posterUrl != null && isTv) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(posterUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(300.dp)
                        .height(450.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                )
                
                Spacer(modifier = Modifier.width(32.dp))
            }
            
            // Text Details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = if (isTv) MaterialTheme.typography.displayMedium else androidx.compose.material3.MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                if (year.isNotEmpty()) {
                    Text(
                        text = year,
                        style = if (isTv) MaterialTheme.typography.titleLarge else androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = overview,
                    style = if (isTv) MaterialTheme.typography.bodyLarge else androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                // Action Button
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (isAvailable) {
                        JellyseerrStatusBadge(
                            isTv = isTv,
                            icon = Icons.Default.Check,
                            label = stringResource(com.klortek.velora.R.string.jellyseerr_available),
                            color = Color(0xFF4CAF50)
                        )
                    } else if (isPending) {
                        JellyseerrStatusBadge(
                            isTv = isTv,
                            icon = Icons.Default.HourglassEmpty,
                            label = stringResource(com.klortek.velora.R.string.jellyseerr_request_pending),
                            color = Color(0xFFFFC107)
                        )
                    } else {
                        val requestAction = {
                            if (!isRequesting && apiService != null) {
                                isRequesting = true
                                scope.launch {
                                    val result = if (mediaType == "movie") {
                                        apiService.requestMovie(tmdbId)
                                    } else {
                                        apiService.requestTvShow(tmdbId)
                                    }
                                    
                                    if (result.isSuccess) {
                                        Toast.makeText(context, R.string.jellyseerr_request_sent, Toast.LENGTH_SHORT).show()
                                        if (mediaType == "movie") {
                                            movieDetails = apiService.getMovieDetails(tmdbId)
                                        } else {
                                            tvDetails = apiService.getTvShowDetails(tmdbId)
                                        }
                                    } else {
                                        Toast.makeText(context, R.string.jellyseerr_request_failed, Toast.LENGTH_SHORT).show()
                                    }
                                    isRequesting = false
                                }
                            }
                        }

                        if (isTv) {
                            Button(
                                onClick = requestAction,
                                enabled = !isRequesting,
                                colors = ButtonDefaults.colors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                if (isRequesting) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.jellyseerr_requesting))
                                } else {
                                    Icon(Icons.Default.Download, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.jellyseerr_request_action))
                                }
                            }
                        } else {
                            androidx.compose.material3.Button(
                                onClick = requestAction,
                                enabled = !isRequesting,
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
                                )
                            ) {
                                if (isRequesting) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    androidx.compose.material3.Text(stringResource(R.string.jellyseerr_requesting))
                                } else {
                                    androidx.compose.material3.Icon(Icons.Default.Download, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    androidx.compose.material3.Text(stringResource(R.string.jellyseerr_request_action))
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Back Button overlay
        Box(
            modifier = Modifier
                .padding(if (isTv) 32.dp else 16.dp)
                .align(Alignment.TopStart)
        ) {
            if (isTv) {
                IconButton(onClick = onBackPressed) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Atrás",
                        tint = Color.White
                    )
                }
            } else {
                androidx.compose.material3.IconButton(onClick = onBackPressed) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Atrás",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

/** A non-interactive status indicator; only request actions should be buttons. */
@Composable
private fun JellyseerrStatusBadge(
    isTv: Boolean,
    icon: ImageVector,
    label: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isTv) {
            Icon(icon, contentDescription = null, tint = color)
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = Color.White)
        } else {
            androidx.compose.material3.Icon(icon, contentDescription = null, tint = color)
            Spacer(modifier = Modifier.width(8.dp))
            androidx.compose.material3.Text(label, color = Color.White)
        }
    }
}
