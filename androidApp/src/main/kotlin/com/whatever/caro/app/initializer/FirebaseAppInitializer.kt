package com.whatever.caro.app.initializer

import android.content.Context
import androidx.startup.Initializer
import com.google.firebase.FirebaseApp
import io.github.aakira.napier.Napier

class FirebaseAppInitializer : Initializer<FirebaseApp> {

    override fun create(context: Context): FirebaseApp {
        val firebaseApp =
            requireNotNull(FirebaseApp.initializeApp(context)) {
                "FirebaseApp 초기화 실패. google-services.json 파일 설정을 확인해주세요."
            }

        Napier.d("FirebaseApp 초기화")
        return firebaseApp
    }

    override fun dependencies(): List<Class<out Initializer<*>>> =
        listOf(NapierInitializer::class.java)

}
