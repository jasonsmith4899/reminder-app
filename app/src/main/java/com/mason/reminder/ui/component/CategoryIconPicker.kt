package com.mason.reminder.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

val CATEGORY_ICONS = listOf(
    "star", "favorite", "work", "home", "school",
    "fitness_center", "restaurant", "directions_car", "flight",
    "pets", "book", "music_note", "movie",
    "shopping_cart", "attach_money", "credit_card",
    "phone", "camera_alt", "palette", "alarm",
    "schedule", "event", "notification_important",
    "folder", "label", "tag", "person", "group",
    "location_on", "local_hospital", "build", "science",
    "description", "edit", "check", "add", "delete",
    "search", "settings", "gift", "cake", "celebration",
    "computer", "local_grocery_store"
)

@Composable
fun CategoryIconPicker(
    currentIcon: String,
    onIconSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = currentIcon,
            onValueChange = {},
            readOnly = true,
            label = { Text("图标") },
            leadingIcon = {
                Icon(iconFromName(currentIcon), contentDescription = currentIcon)
            },
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "选择图标")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (expanded) {
            Spacer(Modifier.height(8.dp))
            Surface(
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(48.dp),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.heightIn(max = 240.dp)
                ) {
                    items(CATEGORY_ICONS) { iconName ->
                        IconButton(
                            onClick = {
                                onIconSelected(iconName)
                                expanded = false
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                iconFromName(iconName),
                                contentDescription = iconName,
                                modifier = Modifier.size(24.dp),
                                tint = if (iconName == currentIcon)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}