package com.klortek.velora

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.klortek.velora.jellyfin.JellyfinApiService
import com.klortek.velora.jellyfin.JellyfinConfig
import com.klortek.velora.jellyfin.JellyfinItem
import com.klortek.velora.jellyfin.Person
import com.klortek.velora.screens.CastInfoScreen

class CastInfoActivity : ComponentActivity() {
    companion object {
        private const val EXTRA_PERSON_ID = "person_id"
        private const val EXTRA_PERSON_NAME = "person_name"
        private const val EXTRA_PERSON_TYPE = "person_type"

        fun createIntent(
            context: Context,
            person: Person
        ): Intent {
            return Intent(context, CastInfoActivity::class.java).apply {
                putExtra(EXTRA_PERSON_ID, person.Id)
                putExtra(EXTRA_PERSON_NAME, person.Name)
                putExtra(EXTRA_PERSON_TYPE, person.Type) // Actor, Director, Writer, etc.
            }
        }

        fun createIntent(
            context: Context,
            personId: String,
            personName: String,
            personType: String? = null
        ): Intent {
            return Intent(context, CastInfoActivity::class.java).apply {
                putExtra(EXTRA_PERSON_ID, personId)
                putExtra(EXTRA_PERSON_NAME, personName)
                putExtra(EXTRA_PERSON_TYPE, personType)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val personId = intent.getStringExtra(EXTRA_PERSON_ID)?.trim()?.takeIf { it.isNotEmpty() }
        val personName = intent.getStringExtra(EXTRA_PERSON_NAME) ?: ""
        val personType = intent.getStringExtra(EXTRA_PERSON_TYPE) // Actor, Director, etc.

        // Get Jellyfin configuration and API service
        val config = JellyfinConfig(this)
        val apiService = if (config.isConfigured()) {
            JellyfinApiService(
                baseUrl = config.serverUrl,
                accessToken = config.accessToken,
                userId = config.userId,
                config = config
            )
        } else {
            finish()
            return
        }

        if (personId == null && personName.isBlank()) {
            finish()
            return
        }

        setContent {
            JellyfinAppTheme {
                var resolvedPersonId by remember { mutableStateOf(personId) }
                var resolving by remember { mutableStateOf(personId == null) }

                LaunchedEffect(personId, personName) {
                    if (resolvedPersonId == null) {
                        resolvedPersonId = apiService.findPersonIdByName(personName)
                        resolving = false
                    }
                }

                when {
                    resolvedPersonId != null -> CastInfoScreen(
                        personId = resolvedPersonId!!,
                        personName = personName,
                        personType = personType,
                        apiService = apiService,
                        onNavigateToItem = { item ->
                            when (item.Type) {
                                "Movie" -> startActivity(MovieDetailsActivity.createIntent(this@CastInfoActivity, item))
                                "Series" -> startActivity(SeriesDetailsActivity.createIntent(this@CastInfoActivity, item, autoFocusNextUp = true))
                            }
                        },
                        onBack = { finish() }
                    )
                    resolving -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.person_not_found), color = Color.White)
                    }
                }
            }
        }
    }
}

