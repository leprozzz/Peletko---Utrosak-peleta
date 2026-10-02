package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.PelletCalculator

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickNumberStepper(
    bags: Int,
    maxBags: Int,
    boilerPowerKw: Double,
    onBagsChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Brzo biranje: 1, 2, 3, 4, 5 (opcije preko limita peći su onemogućene)
    val quickOptions = listOf(1, 2, 3, 4, 5)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Glavni prikaz sa cijelim brojevima i tipkama +/-
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Smanji za 1 cijelu vreću (minimalno 1)
            IconButton(
                onClick = {
                    val newVal = (bags - 1).coerceAtLeast(1)
                    onBagsChanged(newVal)
                },
                enabled = bags > 1,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (bags > 1) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .testTag("stepper_decrease_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Smanji",
                    tint = if (bags > 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Veliki cijeli broj
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$bags",
                        fontSize = 50.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = PelletCalculator.getBagsLabel(bags),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }

                Text(
                    text = "= ${PelletCalculator.formatNumber(PelletCalculator.computeKgFromBags(bags.toDouble()), 0)} kg",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Povećaj za 1 cijelu vreću (maksimalno maxBags)
            val canIncrease = bags < maxBags
            IconButton(
                onClick = {
                    if (canIncrease) {
                        onBagsChanged(bags + 1)
                    }
                },
                enabled = canIncrease,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (canIncrease) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    .testTag("stepper_increase_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Povećaj",
                    tint = if (canIncrease) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Brzo biranje cijelih vreća: 1, 2, 3, 4, 5
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickOptions.forEach { option ->
                val isSelected = (bags == option)
                val isAllowed = (option <= maxBags)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                        !isAllowed -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            !isAllowed -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = isAllowed) { onBagsChanged(option) }
                ) {
                    Text(
                        text = PelletCalculator.formatBagsWithUnit(option),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                            !isAllowed -> MaterialTheme.colorScheme.outline
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
    }
}
