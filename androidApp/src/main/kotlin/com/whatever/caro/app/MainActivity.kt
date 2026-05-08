package com.whatever.caro.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.whatever.caro.composeApp.CaroApp
import com.whatever.caro.core.messaging.IntentMessagingPublisher

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        IntentMessagingPublisher.publishFromIntent(intent)

        setContent {
            CaroApp()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        IntentMessagingPublisher.publishFromIntent(intent)
    }
}
