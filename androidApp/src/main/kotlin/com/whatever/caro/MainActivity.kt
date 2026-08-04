package com.whatever.caro

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.whatever.caro.composeApp.CaroApp
import com.whatever.caro.core.messaging.IntentMessagingPublisher

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        applyOrientationPolicy()

        enableEdgeToEdge(
            statusBarStyle =
                SystemBarStyle.light(
                    scrim = Color.TRANSPARENT,
                    darkScrim = Color.TRANSPARENT,
                ),
            navigationBarStyle =
                SystemBarStyle.light(
                    scrim = Color.TRANSPARENT,
                    darkScrim = Color.TRANSPARENT,
                ),
        )

        IntentMessagingPublisher.publishFromIntent(intent)

        setContent {
            CaroApp()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        IntentMessagingPublisher.publishFromIntent(intent)
    }

    /**
     * 폰(접힌 폴더블 포함)은 세로 고정, 태블릿·펼친 폴더블은 회전 허용.
     * 폴더블 접기/펼치기는 smallestScreenSize 설정 변경이라 Activity 가 재생성되며 여기서 다시 계산된다.
     */
    private fun applyOrientationPolicy() {
        val orientation =
            if (resources.configuration.smallestScreenWidthDp < LARGE_SCREEN_MIN_WIDTH_DP) {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        // setRequestedOrientation 은 값이 같아도 재구성을 유발할 수 있어 달라질 때만 대입한다.
        if (requestedOrientation != orientation) {
            requestedOrientation = orientation
        }
    }
}

private const val LARGE_SCREEN_MIN_WIDTH_DP = 600
