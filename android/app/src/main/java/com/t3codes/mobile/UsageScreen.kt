package com.t3codes.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.t3codes.core.UsageBucket
import com.t3codes.core.UsageSummary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class DailyUsage(val date: LocalDate, val cost: Double)
private data class UsageGroup(
    val provider: String,
    val model: String,
    val cost: Double,
    val tokens: Long,
    val sessions: Long,
)

@Composable
fun UsageScreen(state: UiState, viewModel: MainViewModel) {
    LaunchedEffect(Unit) {
        if (state.usage == null) viewModel.loadUsage(30)
    }
    val usage = state.usage
    val summary = usage?.summary
    val days = usage?.days ?: 30
    val groups = remember(summary?.buckets) {
        summary?.let { summarizeGroups(it.buckets) }.orEmpty()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ScreenHeader(
                title = "Usage",
                subtitle = summary?.let { "${it.sinceDay} → ${it.untilDay}" } ?: "Token and cost accounting",
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(7, 30, 90).forEach { range ->
                    FilterChip(
                        selected = days == range,
                        onClick = { viewModel.loadUsage(range) },
                        label = { Text("${range}d") },
                        shape = CircleShape,
                    )
                }
            }
        }

        when {
            usage?.error != null -> item {
                ErrorBanner(
                    message = usage.error,
                    onRetry = { viewModel.loadUsage(days) },
                )
            }

            summary == null && usage?.loading == true -> item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            summary == null || summary.buckets.isEmpty() -> item {
                EmptyState(
                    icon = Icons.Rounded.BarChart,
                    title = "No usage yet",
                    message = "Run an agent, then come back to see token totals and spend here.",
                )
            }

            else -> {
                item { UsageHero(summary) }
                item { SectionLabel("Daily spend") }
                item { DailyChart(summary.buckets, days) }
                if (groups.isNotEmpty()) {
                    item { SectionLabel("By model · ${groups.size}") }
                    item {
                        GroupedCard {
                            groups.forEachIndexed { index, group ->
                                UsageGroupRow(
                                    group = group,
                                    maxCost = groups.maxOf { it.cost },
                                )
                                if (index < groups.lastIndex) InsetDivider(start = 16.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UsageHero(summary: UsageSummary) {
    val tokens = summary.buckets.fold(0L) { total, bucket ->
        total + bucket.totals.uncachedInputTokens + bucket.totals.cachedInputTokens +
            bucket.totals.cacheCreationTokens + bucket.totals.outputTokens
    }
    val cost = summary.buckets.sumOf { it.costUsd }
    val savings = summary.buckets.sumOf { it.cacheSavingsUsd }
    val sessions = summary.buckets.sumOf { it.sessions }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary,
                    ),
                ),
            ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Payments,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = "TOTAL SPEND",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = formatCost(cost),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                HeroStat("Tokens", formatTokens(tokens))
                HeroStat("Sessions", sessions.toString())
                HeroStat("Cache saved", formatCost(savings))
            }
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
        )
    }
}

@Composable
private fun DailyChart(buckets: List<UsageBucket>, days: Int) {
    val daily = remember(buckets, days) { dailyUsage(buckets, days) }
    val maxCost = daily.maxOfOrNull { it.cost } ?: 0.0
    val formatter = remember { DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (maxCost <= 0.0) {
                Text(
                    text = "No spend recorded in this range.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().height(118.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    daily.forEach { day ->
                        Column(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            verticalArrangement = Arrangement.Bottom,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            val ratio = (day.cost / maxCost).toFloat().coerceIn(0.02f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.72f)
                                    .fillMaxHeight(ratio)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        if (day.cost > 0) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceContainerHighest,
                                    ),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = daily.firstOrNull()?.date?.format(formatter) ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = daily.getOrNull(daily.size / 2)?.date?.format(formatter) ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun UsageGroupRow(group: UsageGroup, maxCost: Double) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.model.ifBlank { "Unknown model" },
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOf(
                        group.provider.ifBlank { "provider" },
                        "${formatTokens(group.tokens)} tokens",
                        "${group.sessions} sessions",
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = formatCost(group.cost),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(
                        if (maxCost > 0) (group.cost / maxCost).toFloat().coerceIn(0f, 1f) else 0f,
                    )
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

private fun summarizeGroups(buckets: List<UsageBucket>): List<UsageGroup> {
    val grouped = buckets.groupBy { it.provider to it.model }
    return grouped.map { (key, items) ->
        UsageGroup(
            provider = key.first,
            model = key.second,
            cost = items.sumOf { it.costUsd },
            tokens = items.sumOf {
                it.totals.uncachedInputTokens + it.totals.cachedInputTokens +
                    it.totals.cacheCreationTokens + it.totals.outputTokens
            },
            sessions = items.sumOf { it.sessions },
        )
    }.sortedByDescending { it.cost }
}

private fun dailyUsage(buckets: List<UsageBucket>, days: Int): List<DailyUsage> {
    val byDay = buckets.groupBy { it.day }.mapValues { (_, items) -> items.sumOf { it.costUsd } }
    val end = LocalDate.now()
    return (days - 1 downTo 0).map { offset ->
        val date = end.minusDays(offset.toLong())
        DailyUsage(date, byDay[date.toString()] ?: 0.0)
    }
}
