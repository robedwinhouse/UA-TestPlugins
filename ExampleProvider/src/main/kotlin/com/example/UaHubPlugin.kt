package com.example

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class UaHubPlugin : Plugin() {
    override fun load(context: Context) {
        // Реєструємо наш новий парсер
        registerMainAPI(EneyidaProvider())
    }
}
