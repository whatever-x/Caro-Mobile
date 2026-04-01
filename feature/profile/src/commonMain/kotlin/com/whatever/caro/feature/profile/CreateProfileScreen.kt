package com.whatever.caro.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatever.caro.feature.profile.mvi.CreateProfileIntent
import com.whatever.caro.feature.profile.mvi.CreateProfileState

private val BackgroundPrimary = Color(0xFFF8FBFD)
private val TextPrimary = Color(0xFF4A5563)
private val TextDisabled = Color(0xFFD2DAE2)
private val TextTertiary = Color(0xFFBCC6D1)
private val SurfacePrimary = Color.White
private val DividerPrimary = Color(0xFFE3EAF0)
private val SurfaceInverse = Color(0xFF4A5563)
private val TextInverse = Color.White

@Composable
internal fun CreateProfileScreen(
    state: CreateProfileState,
    onIntent: (CreateProfileIntent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(BackgroundPrimary),
    ) {
        TopBar(
            onBackClick = { onIntent(CreateProfileIntent.ClickBack) },
        )

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NicknameLabel(
                onRefreshClick = { onIntent(CreateProfileIntent.ClickRefresh) },
            )

            NicknameTextField(
                nickname = state.nickname,
                placeholder = state.placeholder,
                onNicknameChange = { onIntent(CreateProfileIntent.UpdateNickname(it)) },
            )

            NicknameHelperText(
                characterCount = state.characterCount,
            )
        }

        ConfirmButton(
            enabled = state.isValid,
            onClick = { onIntent(CreateProfileIntent.ClickConfirm) },
        )
    }
}

@Composable
private fun TopBar(onBackClick: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(BackgroundPrimary)
                .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "<",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier =
                Modifier
                    .size(24.dp)
                    .clickable(onClick = onBackClick),
        )
        Text(
            text = "프로필 생성",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )
    }
}

@Composable
private fun NicknameLabel(onRefreshClick: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "닉네임",
            fontSize = 14.sp,
            color = TextPrimary,
        )
        Text(
            text = "\u21BB",
            fontSize = 14.sp,
            color = TextPrimary,
            modifier =
                Modifier
                    .size(16.dp)
                    .clickable(onClick = onRefreshClick),
        )
    }
}

@Composable
private fun NicknameTextField(
    nickname: String,
    placeholder: String,
    onNicknameChange: (String) -> Unit,
) {
    BasicTextField(
        value = nickname,
        onValueChange = onNicknameChange,
        textStyle =
            TextStyle(
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = TextPrimary,
            ),
        singleLine = true,
        cursorBrush = SolidColor(TextPrimary),
        decorationBox = { innerTextField ->
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfacePrimary)
                        .border(
                            width = 1.dp,
                            color = DividerPrimary,
                            shape = RoundedCornerShape(12.dp),
                        ).padding(horizontal = 17.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (nickname.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = TextDisabled,
                    )
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun NicknameHelperText(characterCount: String) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "2~10자 한글, 영문, 숫자 사용 가능",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextTertiary,
        )
        Text(
            text = characterCount,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextTertiary,
        )
    }
}

@Composable
private fun ConfirmButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(CircleShape)
                    .background(if (enabled) SurfaceInverse else SurfaceInverse.copy(alpha = 0.4f))
                    .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "확인",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextInverse,
            )
        }
    }
}
