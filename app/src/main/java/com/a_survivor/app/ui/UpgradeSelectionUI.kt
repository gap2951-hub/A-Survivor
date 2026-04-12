package com.a_survivor.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a_survivor.app.data.Upgrade

@Composable
fun UpgradeSelectionUI(
    choices: List<Upgrade>,
    onSelect: (Upgrade) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text       = "레벨 업!",
                color      = Color(0xFFFFEB3B),
                fontSize   = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text     = "업그레이드를 선택하세요",
                color    = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            choices.forEach { upgrade ->
                UpgradeCard(upgrade = upgrade, onClick = { onSelect(upgrade) })
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun UpgradeCard(upgrade: Upgrade, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E2E), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF7C4DFF), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column {
            Text(
                text       = upgrade.name,
                color      = Color(0xFFCE93D8),
                fontSize   = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text     = upgrade.description,
                color    = Color(0xFFBDBDBD),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
