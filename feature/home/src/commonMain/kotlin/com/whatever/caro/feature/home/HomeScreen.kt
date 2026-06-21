package com.whatever.caro.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.whatever.caro.feature.home.mvi.HomeIntent
import com.whatever.caro.feature.home.mvi.HomeState

@Composable
internal fun HomeScreen(
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(onClick = { onIntent(HomeIntent.ClickLogout) }) {
            Text("logOut")
        }

        Button(onClick = { onIntent(HomeIntent.ClickSignUp) }) {
            Text("SignUp")
        }

        Button(onClick = { onIntent(HomeIntent.ClickProfile) }) {
            Text("Profile")
        }

        Button(onClick = { onIntent(HomeIntent.ClickCreateDeck) }) {
            Text("덱 만들기")
        }

        Button(onClick = { onIntent(HomeIntent.ClickCreateCard) }) {
            Text("카드 생성")
        }
    }
}
