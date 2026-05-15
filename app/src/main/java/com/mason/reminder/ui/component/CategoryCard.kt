package com.mason.reminder.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mason.reminder.data.model.Category
import com.mason.reminder.data.model.UrgencyState
import com.mason.reminder.data.model.color
import com.mason.reminder.data.model.label

/**
 * 分类卡片组件 — 展示分类图标、名称、待办数量、倒计时和紧急度指示。
 *
 * 左侧有紧急度色条作为视觉强调，右侧有 UrgencyBadge 色点 + 文字标签。
 * 支持单击进入任务列表、长按触发编辑/删除。
 * Card 背景色随 CRITICAL 状态微变，elevation 随紧急度递增。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryCard(
    category: Category,
    taskCount: Int,
    daysLeft: Long,
    categoryUrgency: UrgencyState,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconVector = iconFromName(category.iconName)
    val tint = parseColorHex(category.colorHex)
    val urgencyColor by animateColorAsState(
        targetValue = categoryUrgency.color(),
        label = "urgencyColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = when (categoryUrgency) {
                UrgencyState.CRITICAL -> 4.dp
                UrgencyState.URGENT   -> 3.dp
                UrgencyState.NOTICE   -> 2.dp
                UrgencyState.CALM     -> 1.dp
            }
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (categoryUrgency == UrgencyState.CRITICAL)
                urgencyColor.copy(alpha = 0.05f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 分类图标 — 圆形背景 + 图标
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = category.name,
                    modifier = Modifier.size(28.dp),
                    tint = tint
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // 中间：名称 + 统计
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$taskCount 个待办",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // 圆点分隔符
                    if (daysLeft != Long.MAX_VALUE) {
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                        )
                        CountdownText(
                            daysLeft = daysLeft,
                            urgency = categoryUrgency
                        )
                    }
                }
            }

            // 右侧：紧急度色点 + 标签
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                UrgencyBadge(urgency = categoryUrgency, size = 14.dp)
                Text(
                    text = categoryUrgency.label(),
                    style = MaterialTheme.typography.labelSmall,
                    color = urgencyColor,
                    fontWeight = if (categoryUrgency >= UrgencyState.URGENT)
                        FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

/** Map icon name strings to Material Icons ImageVectors. */
fun iconFromName(name: String): ImageVector = when (name.lowercase()) {
    // ── Old short-name aliases (backward compatibility) ──
    "work"      -> Icons.Default.Work
    "home"      -> Icons.Default.Home
    "school"    -> Icons.Default.School
    "health"    -> Icons.Default.HealthAndSafety
    "shopping"  -> Icons.Default.ShoppingCart
    "finance"   -> Icons.Default.AccountBalance
    "travel"    -> Icons.Default.Flight
    "food"      -> Icons.Default.Restaurant
    "sports"    -> Icons.Default.SportsSoccer
    "book"      -> Icons.Default.MenuBook
    "music"     -> Icons.Default.MusicNote
    "car"       -> Icons.Default.DirectionsCar
    "pets"      -> Icons.Default.Pets
    "people"    -> Icons.Default.People
    "star"      -> Icons.Default.Star
    "folder"    -> Icons.Default.Folder

    // ── New snake_case names ──
    "favorite"               -> Icons.Default.Favorite
    "fitness_center"         -> Icons.Default.FitnessCenter
    "restaurant"             -> Icons.Default.Restaurant
    "directions_car"         -> Icons.Default.DirectionsCar
    "flight"                 -> Icons.Default.Flight
    "movie"                  -> Icons.Default.Movie
    "shopping_cart"          -> Icons.Default.ShoppingCart
    "attach_money"           -> Icons.Default.AttachMoney
    "credit_card"            -> Icons.Default.CreditCard
    "phone"                  -> Icons.Default.Phone
    "camera_alt"             -> Icons.Default.CameraAlt
    "camera"                 -> Icons.Default.Camera
    "palette"                -> Icons.Default.Palette
    "alarm"                  -> Icons.Default.Alarm
    "schedule"               -> Icons.Default.Schedule
    "event"                  -> Icons.Default.Event
    "notification_important" -> Icons.Default.NotificationImportant
    "label"                  -> Icons.Default.Label
    "tag"                    -> Icons.Default.Tag
    "person"                 -> Icons.Default.Person
    "group"                  -> Icons.Default.Group
    "location_on"            -> Icons.Default.LocationOn
    "local_hospital"         -> Icons.Default.LocalHospital
    "local_grocery_store"    -> Icons.Default.LocalGroceryStore
    "build"                  -> Icons.Default.Build
    "science"                -> Icons.Default.Science
    "description"            -> Icons.Default.Description
    "edit"                   -> Icons.Default.Edit
    "check"                  -> Icons.Default.Check
    "add"                    -> Icons.Default.Add
    "delete"                 -> Icons.Default.Delete
    "search"                 -> Icons.Default.Search
    "settings"               -> Icons.Default.Settings
    "gift"                   -> Icons.Default.CardGiftcard
    "cake"                   -> Icons.Default.Cake
    "celebration"            -> Icons.Default.Celebration
    "computer"               -> Icons.Default.Computer
    "music_note"             -> Icons.Default.MusicNote

    else -> Icons.Default.Folder
}

/** Parse "#RRGGBB" or "RRGGBB" hex string to Compose Color. */
fun parseColorHex(hex: String): Color {
    return try {
        val clean = hex.removePrefix("#")
        Color(("FF$clean").toLong(16))
    } catch (_: Exception) {
        Color.Gray
    }
}

/** Predefined icon options for category creation dialog. */
val ICON_OPTIONS: List<String> = listOf(
    "work", "home", "school", "health", "shopping",
    "finance", "travel", "food", "sports", "book",
    "music", "car", "pets", "people", "star", "folder"
)

/** Predefined color options for category creation dialog. */
val COLOR_OPTIONS: List<String> = listOf(
    "#2E8B57", "#4CAF50", "#8BC34A",  // greens
    "#FFC107", "#FF9800", "#F44336",  // warm
    "#2196F3", "#3F51B5", "#9C27B0",  // cool
    "#795548", "#607D8B", "#000000"   // neutral
)