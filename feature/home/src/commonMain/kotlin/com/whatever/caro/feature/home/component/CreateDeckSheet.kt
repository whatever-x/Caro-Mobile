package com.whatever.caro.feature.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.home_floating_button
import caromobile.core.designsystem.generated.resources.ic_edit_24
import caromobile.core.designsystem.generated.resources.ic_file_text_24
import caromobile.core.designsystem.generated.resources.import_sheet_import_body
import caromobile.core.designsystem.generated.resources.import_sheet_import_title
import caromobile.core.designsystem.generated.resources.import_sheet_manual_body
import caromobile.core.designsystem.generated.resources.import_sheet_manual_title
import com.whatever.caro.core.designsystem.themes.CaroTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreateDeckSheet(
    onCreateDeck: () -> Unit,
    onImportDeck: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetTitle = stringResource(Res.string.home_floating_button)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        // 반만 열리면 큰 글자에서 두 번째 선택지가 잘리므로 항상 끝까지 펼친다.
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape =
            RoundedCornerShape(
                topStart = CaroTheme.spacing.xl2,
                topEnd = CaroTheme.spacing.xl2,
            ),
        containerColor = CaroTheme.color.surface.primary,
        dragHandle = {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(color = CaroTheme.color.surface.primary)
                        .padding(vertical = CaroTheme.spacing.s),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .background(
                                color = CaroTheme.color.divider.primary,
                                shape = CaroTheme.shape.xxs,
                            ),
                )
            }
        },
    ) {
        // 시트에 보이는 제목이 없으므로 열릴 때 보조 기술이 읽을 이름을 pane 제목으로 둔다.
        Column(
            modifier =
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .semantics { paneTitle = sheetTitle }
                    .fillMaxWidth()
                    .background(color = CaroTheme.color.surface.primary)
                    .padding(
                        start = CaroTheme.spacing.l,
                        top = CaroTheme.spacing.l,
                        end = CaroTheme.spacing.l,
                        bottom = CaroTheme.spacing.xl3,
                    ),
            verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xs),
        ) {
            CreateDeckSheetItem(
                title = stringResource(Res.string.import_sheet_manual_title),
                body = stringResource(Res.string.import_sheet_manual_body),
                icon = Res.drawable.ic_edit_24,
                onClick = onCreateDeck,
            )

            CreateDeckSheetItem(
                title = stringResource(Res.string.import_sheet_import_title),
                body = stringResource(Res.string.import_sheet_import_body),
                icon = Res.drawable.ic_file_text_24,
                onClick = onImportDeck,
            )
        }
    }
}

@Composable
private fun CreateDeckSheetItem(
    title: String,
    body: String,
    icon: DrawableResource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(vertical = CaroTheme.spacing.s),
        horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = CaroTheme.color.icon.primary,
        )

        Column(verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xxs)) {
            Text(
                text = title,
                style = CaroTheme.typography.heading3,
                color = CaroTheme.color.text.primary,
            )

            Text(
                text = body,
                style = CaroTheme.typography.caption1.regular,
                color = CaroTheme.color.text.secondary,
            )
        }
    }
}
