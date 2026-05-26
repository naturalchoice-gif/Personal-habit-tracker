package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.HabitRpgViewModel
import com.example.ui.theme.MyApplicationTheme
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F1113) // Dark Cyber Midnight background
                ) {
                    HabitRpgApp()
                }
            }
        }
    }
}

@Composable
fun HabitRpgApp() {
    val context = LocalContext.current
    val gameViewModel: HabitRpgViewModel = viewModel()
    
    val characterState by gameViewModel.character.collectAsStateWithLifecycle()
    val questsList by gameViewModel.quests.collectAsStateWithLifecycle()
    val badgesList by gameViewModel.badges.collectAsStateWithLifecycle()
    val logsList by gameViewModel.gameLogs.collectAsStateWithLifecycle()

    var showCharacterCreation by remember { mutableStateOf(false) }
    var showAddQuestDialog by remember { mutableStateOf(false) }
    var showClassInfoDialog by remember { mutableStateOf(false) }

    // Navigation sub-pages of the single layout (using a segmented control state)
    var selectedTab by remember { mutableStateOf("quests") } // quests, shop, badges

    // If character exists and has default unconfigured name, force design screen overlays
    val character = characterState
    val isDefaultNewHero = character != null && character.name == "Scholar Dev" && character.characterClass == "Scholar" && character.totalQuestsCompleted == 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        if (character == null) {
            // Screen preloader
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFFFB300))
            }
        } else if (isDefaultNewHero || showCharacterCreation) {
            // Immersive RPG Character Creation Sheet
            CharacterCreationSheet(
                character = character,
                onSave = { name, chosenClass ->
                    gameViewModel.setupCharacter(name, chosenClass)
                    showCharacterCreation = false
                },
                onCancel = {
                    if (!isDefaultNewHero) {
                        showCharacterCreation = false
                    }
                },
                allowCancel = !isDefaultNewHero
            )
        } else {
            // Main App Container
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // 1. Top Character Stats Summary Panel (Always visible)
                CharacterHeaderPanel(
                    character = character,
                    onCustomizeClick = { showCharacterCreation = true },
                    onInfoClick = { showClassInfoDialog = true },
                    onRestTavernClick = { gameViewModel.restAtInn() }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Segmented Custom Tab Layout (Inside Single Screen View)
                RpgSegmentControl(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Main dynamic workspace depending on selected custom state tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (selectedTab) {
                        "quests" -> {
                            QuestsWorkspace(
                                quests = questsList,
                                onCheckQuest = { gameViewModel.completeQuest(it) },
                                onFailQuest = { gameViewModel.triggerNegativeHabit(it) },
                                onDeleteQuest = { id, title -> gameViewModel.deleteQuest(id, title) },
                                onAddClick = { showAddQuestDialog = true }
                            )
                        }
                        "shop" -> {
                            ShopWorkspace(
                                gold = character.gold,
                                onPurchase = { name, cost -> gameViewModel.buyShopItem(name, cost) }
                            )
                        }
                        "badges" -> {
                            BadgesWorkspace(
                                badges = badgesList,
                                logs = logsList,
                                onClearLogs = { gameViewModel.clearLogHistory() }
                            )
                        }
                    }
                }
            }

            // Anchored Floating Action Button for Adding Quests on the Quests Workspace tab
            if (selectedTab == "quests") {
                FloatingActionButton(
                    onClick = { showAddQuestDialog = true },
                    containerColor = Color(0xFFD0BCFF),
                    contentColor = Color(0xFF381E72),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp)
                        .testTag("add_quest_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Enlist New Quest",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // Add Quest Overlay Dialog
        if (showAddQuestDialog) {
            AddQuestDialog(
                onDismiss = { showAddQuestDialog = false },
                onSave = { title, desc, type, difficulty, statType ->
                    gameViewModel.addQuest(title, desc, type, difficulty, statType)
                    showAddQuestDialog = false
                }
            )
        }

        // Class Specialization Info Banner Modal Dialog
        if (showClassInfoDialog) {
            RpgInfoDialog(
                character = character!!,
                onDismiss = { showClassInfoDialog = false }
            )
        }
    }
}

// Global Custom Segment Controller
@Composable
fun RpgSegmentControl(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1C1E))
            .border(1.dp, Color(0xFF333537), RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val tabs = listOf(
            Triple("quests", "Quests & Habits", Icons.Default.FitnessCenter),
            Triple("shop", "Tavern & Armory", Icons.Default.Storefront),
            Triple("badges", "Achievements & Logs", Icons.Default.EmojiEvents)
        )

        tabs.forEach { (tabId, label, icon) ->
            val isSelected = selectedTab == tabId
            val bgCol = if (isSelected) Color(0xFF381E72) else Color.Transparent
            val textCol = if (isSelected) Color(0xFFD0BCFF) else Color(0xFF909094)
            val iconCol = if (isSelected) Color(0xFFD0BCFF) else Color(0xFF909094)

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgCol)
                    .clickable { onTabSelected(tabId) }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconCol,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (tabId == "badges") "Achievements" else label.substringBefore(" &"),
                    color = textCol,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// IMMERSIVE HEADER: Stats Panel (Level, HP, XP, Gold, Streaks, Debuffs)
@Composable
fun CharacterHeaderPanel(
    character: UserCharacter,
    onCustomizeClick: () -> Unit,
    onInfoClick: () -> Unit,
    onRestTavernClick: () -> Unit
) {
    // Determine Class Specialization Colors and Icons
    val classDetails = when (character.characterClass) {
        "Warrior" -> Triple("⚔️", Color(0xFFD32F2F), Brush.horizontalGradient(listOf(Color(0xFFD32F2F), Color(0xFFFF5252))))
        "Mage" -> Triple("🔮", Color(0xFF9C27B0), Brush.horizontalGradient(listOf(Color(0xFF9C27B0), Color(0xFFE040FB))))
        "Rogue" -> Triple("🗡️", Color(0xFF2E7D32), Brush.horizontalGradient(listOf(Color(0xFF2E7D32), Color(0xFF66BB6A))))
        "Scholar" -> Triple("📚", Color(0xFF0288D1), Brush.horizontalGradient(listOf(Color(0xFF0288D1), Color(0xFF29B6F6))))
        else -> Triple("🎭", Color(0xFFFFB300), Brush.horizontalGradient(listOf(Color(0xFFFFB300), Color(0xFFFFD54F))))
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C1E)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF333537), RoundedCornerShape(24.dp))
            .testTag("character_header")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // First Row: Avatar and basic title details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Floating Avatar Square Box with Badge overlay
                    Box(
                        modifier = Modifier.padding(bottom = 4.dp, end = 4.dp /* decorative outer space */)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFFD0BCFF), Color(0xFF381E72))))
                                .padding(2.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF1A1C1E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = classDetails.first,
                                fontSize = 30.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        
                        // Overlapping LVL badge in bottom right corner
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 4.dp, y = 4.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFD0BCFF))
                                .border(1.dp, Color(0xFF1A1C1E), RoundedCornerShape(6.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "LVL ${character.level}",
                                color = Color(0xFF381E72),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = character.name,
                                color = Color(0xFFE2E2E6),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = onCustomizeClick,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Character Sheet",
                                    tint = Color(0xFFD0BCFF),
                                    modifier = Modifier.size(14.dp)
                               )
                            }
                        }
                        
                        Text(
                            text = "${character.characterClass} Class".uppercase(),
                            color = classDetails.second,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.5.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Tavern Rest quick shortcut
                    Button(
                        onClick = onRestTavernClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2D2F31),
                            contentColor = Color(0xFFD0BCFF)
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Filled.Bed, contentDescription = "Rest", modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rest 5g", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(onClick = onInfoClick, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Stats Spec Info",
                            tint = Color(0xFF909094),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dynamic EXP Bar and HP Bar Columns in parallel
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // HP Health Bar indicator
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Health Points",
                            color = Color(0xFFFFB4AB),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "${character.hp} / ${character.maxHp} HP",
                            color = Color(0xFFFFB4AB),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { character.hp.toFloat() / character.maxHp },
                        color = Color(0xFFFF5449),
                        trackColor = Color(0xFF333537),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }

                // EXP Bar indicator
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Experience",
                            color = Color(0xFFD0BCFF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "${character.exp} / ${character.maxExp} XP",
                            color = Color(0xFFD0BCFF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { character.exp.toFloat() / character.maxExp },
                        color = Color(0xFFD0BCFF),
                        trackColor = Color(0xFF333537),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF333537), thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

            // Bottom row: Gold, Streak, Attributes list
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gold stash index
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = "Gold Wallet",
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${character.gold}g",
                        color = Color(0xFFFFB300),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Unbroken Activity Streak",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${character.streakDays} Day Streak",
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            // Stat Attributes Layout Details
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2D2F31))
                    .border(1.dp, Color(0xFF333537), RoundedCornerShape(12.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                AttributeItem("STR 🏋️", character.strength, character.isGlassSwordActive, Color(0xFFFFB4AB))
                AttributeItem("INT 🧠", character.intellect, character.isBrainFogActive, Color(0xFFB1EBFF))
                AttributeItem("AGI 🏃", character.agility, character.isSlothActive, Color(0xFFD0BCFF))
                AttributeItem("CON 🛡️", character.constitution, character.isSnackSackerActive, Color(0xFFE2E2E6))
            }

            // Active Debuffs section
            val activeDebuffs = mutableListOf<String>()
            if (character.isGlassSwordActive) activeDebuffs.add("Weakened (-2 STR)")
            if (character.isBrainFogActive) activeDebuffs.add("Brain Fog (-2 INT)")
            if (character.isSlothActive) activeDebuffs.add("Sluggish (-2 AGI)")
            if (character.isSnackSackerActive) activeDebuffs.add("Decay (-2 CON)")

            if (activeDebuffs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Active Curses",
                            tint = Color(0xFFFFB4AB),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ACTIVE CURSES",
                            color = Color(0xFFE2E2E6),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF2D1616))
                            .border(1.dp, Color(0xFF410002), RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF410002)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Curse Impact",
                                tint = Color(0xFFFFB4AB),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Habit Penalties Incurred",
                                color = Color(0xFFFFB4AB),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = activeDebuffs.joinToString(", "),
                                color = Color(0xFFFFDAD6),
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AttributeItem(label: String, value: Int, isDebuffed: Boolean, normalColor: Color = Color.White) {
    val finalVal = if (isDebuffed) kotlin.math.max(1, value - 2) else value
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, color = Color(0xFF909094), fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$finalVal",
                color = if (isDebuffed) Color(0xFFFF5449) else normalColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            if (isDebuffed) {
                Text(
                    text = "↓",
                    color = Color(0xFFFF5449),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(start = 1.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------
// WORKSPACE 1: QUESTS PANEL
// ---------------------------------------------------------
@Composable
fun QuestsWorkspace(
    quests: List<Quest>,
    onCheckQuest: (Int) -> Unit,
    onFailQuest: (Int) -> Unit,
    onDeleteQuest: (Int, String) -> Unit,
    onAddClick: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, DAILY, POSITIVE, NEGATIVE

    val filteredQuests = when (selectedFilter) {
        "ALL" -> quests
        "DAILY" -> quests.filter { it.type == "DAILY" }
        "POSITIVE" -> quests.filter { it.type == "POSITIVE" }
        "NEGATIVE" -> quests.filter { it.type == "NEGATIVE" }
        else -> quests
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter Scrollable Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf(
                Pair("ALL", "All Quests"),
                Pair("DAILY", "⚔️ Dailies"),
                Pair("POSITIVE", "✨ Habits (+)"),
                Pair("NEGATIVE", "💀 Hazards (-)")
            )

            filters.forEach { (filterId, label) ->
                val active = selectedFilter == filterId
                FilterChip(
                    selected = active,
                    onClick = { selectedFilter = filterId },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF381E72),
                        selectedLabelColor = Color(0xFFD0BCFF),
                        containerColor = Color(0xFF1A1C1E),
                        labelColor = Color(0xFF909094)
                    ),
                    label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredQuests.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.HistoryEdu,
                        contentDescription = "Empty Log Book",
                        tint = Color(0xFF333537),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No quests in your current ledger!",
                        color = Color(0xFFE2E2E6),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Enlist a daily physical quest or track standard routine habits to begin leveling.",
                        color = Color(0xFF909094),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = onAddClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF), contentColor = Color(0xFF381E72))
                    ) {
                        Text("Draft First Quest")
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredQuests, key = { it.id }) { quest ->
                    QuestListItem(
                        quest = quest,
                        onComplete = { onCheckQuest(quest.id) },
                        onFlinched = { onFailQuest(quest.id) },
                        onDelete = { onDeleteQuest(quest.id, quest.title) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp)) // Space for overlapping FAB
                }
            }
        }
    }
}

@Composable
fun QuestListItem(
    quest: Quest,
    onComplete: () -> Unit,
    onFlinched: () -> Unit,
    onDelete: () -> Unit
) {
    // Stat Associated icon
    val statSymbol = when (quest.statType) {
        "STR" -> Pair(Icons.Default.FitnessCenter, Color(0xFFE57373))
        "INT" -> Pair(Icons.Default.MenuBook, Color(0xFF64B5F6))
        "AGI" -> Pair(Icons.Default.DirectionsRun, Color(0xFF81C784))
        "CON" -> Pair(Icons.Default.Shield, Color(0xFFFFD54F))
        else -> Pair(Icons.Default.Star, Color(0xFFBA68C8))
    }

    val typeLabel = when (quest.type) {
        "DAILY" -> "Daily Quest"
        "POSITIVE" -> "Positive Habit"
        "NEGATIVE" -> "Hazard"
        else -> "Quest"
    }

    val difficultyBorder = when (quest.difficulty) {
        "EASY" -> Color(0xFF81C784)
        "MEDIUM" -> Color(0xFFFFD54F)
        "HARD" -> Color(0xFFE57373)
        else -> Color(0xFFA0A0AC)
    }

    val isTickedToday = quest.type == "DAILY" && quest.isCompletedToday()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C1E)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF333537), RoundedCornerShape(16.dp))
            .testTag("quest_item_card_${quest.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Associated Attribute Sigil
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF2D2F31)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = statSymbol.first,
                    contentDescription = statSymbol.second.toString(),
                    tint = statSymbol.second,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Body
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = quest.title,
                        color = if (isTickedToday) Color(0xFF909094) else Color(0xFFE2E2E6),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Text(
                    text = quest.description,
                    color = Color(0xFF909094),
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Stats footer tags
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Type Tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF2D2F31))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(typeLabel, color = Color(0xFFECEFF1), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }

                    // Difficulty Tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(difficultyBorder.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(quest.difficulty, color = difficultyBorder, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Completion / Trigger Action buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (quest.type == "NEGATIVE") {
                    // Skull Trigger Button to indicate negative habit hit
                    IconButton(
                        onClick = onFlinched,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2D1616))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Trigger Hazard Damage",
                            tint = Color(0xFFFFB4AB),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    // Check button for positive or daily quests
                    if (isTickedToday) {
                        Button(
                            onClick = {},
                            enabled = false,
                            colors = ButtonDefaults.buttonColors(
                                disabledContainerColor = Color(0xFF1E2F21),
                                disabledContentColor = Color(0xFF81C784)
                            ),
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Completed Today", modifier = Modifier.size(24.dp))
                        }
                    } else {
                        IconButton(
                            onClick = onComplete,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF203222))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Complete Quest",
                                tint = Color(0xFF81C784),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Discard Quest",
                        tint = Color(0xFF909094),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------
// WORKSPACE 2: SHOP SERVICES
// ---------------------------------------------------------
@Composable
fun ShopWorkspace(
    gold: Int,
    onPurchase: (String, Int) -> Unit
) {
    val items = listOf(
        Triple("Health Potion", "Cure your flesh of direct combat scars. Instantly restores +30 HP.", 15),
        Triple("Golden Elixir", "Cures all active debuffs (Sloth, Brain Fog, Decay, Weakened) instantly.", 25),
        Triple("Ring of Power", "A mystical ruby ring which permanently increases your base Strength by +2.", 50),
        Triple("Amulet of Scholar", "A scroll insignia which permanently increases your base Intellect by +2.", 50),
        Triple("Boots of Swiftness", "Light wind-runner boots which permanently increases your base Agility by +2.", 50),
        Triple("Belt of Vitality", "A heavy steel girdle which permanently increases Constitution by +2 (Adds +10 Max HP).", 50)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "The Adventurer's Tavern & Armory",
            color = Color(0xFFE2E2E6),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            text = "Convert your accumulated Daily Quest gold into life potions and permanent stats equipment boost.",
            color = Color(0xFF909094),
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items) { item ->
                ShopItemCard(
                    title = item.first,
                    description = item.second,
                    cost = item.third,
                    userGold = gold,
                    onBuy = { onPurchase(item.first, item.third) }
                )
            }
            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

@Composable
fun ShopItemCard(
    title: String,
    description: String,
    cost: Int,
    userGold: Int,
    onBuy: () -> Unit
) {
    val canAfford = userGold >= cost
    val itemIcon = when (title) {
        "Health Potion" -> Pair(Icons.Default.Favorite, Color(0xFFE57373))
        "Golden Elixir" -> Pair(Icons.Default.Healing, Color(0xFFFFB300))
        "Ring of Power" -> Pair(Icons.Default.RadioButtonChecked, Color(0xFF9C27B0))
        "Amulet of Scholar" -> Pair(Icons.Default.AutoAwesome, Color(0xFF0288D1))
        "Boots of Swiftness" -> Pair(Icons.Default.DirectionsRun, Color(0xFF81C784))
        "Belt of Vitality" -> Pair(Icons.Default.Shield, Color(0xFF4DB6AC))
        else -> Pair(Icons.Default.ShoppingCart, Color.White)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C1E)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF333537), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle Logo
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2D2F31)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = itemIcon.first,
                    contentDescription = title,
                    tint = itemIcon.second,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Describe
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color(0xFFE2E2E6), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(description, color = Color(0xFF909094), fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Purchase Button
            Button(
                onClick = onBuy,
                enabled = canAfford,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD0BCFF),
                    contentColor = Color(0xFF381E72),
                    disabledContainerColor = Color(0xFF2D2F31),
                    disabledContentColor = Color(0xFF909094)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Payments, contentDescription = "Cost", modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${cost}g", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ---------------------------------------------------------
// WORKSPACE 3: BADGES & LOGS SERVICES
// ---------------------------------------------------------
@Composable
fun BadgesWorkspace(
    badges: List<Badge>,
    logs: List<GameLog>,
    onClearLogs: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Upper section: Unlocked Badge Badges list (Horizontal layout / Carousel or clean micro cards map)
        Text(
            text = "Legendary Accomplishments",
            color = Color(0xFFE2E2E6),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            badges.forEach { badge ->
                BadgeMicroCard(badge = badge)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Lower half section: Logging events
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Chronicles / Battle Feed",
                color = Color(0xFFE2E2E6),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                text = "Clear Chronicle",
                color = Color(0xFF909094),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onClearLogs() }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1A1C1E))
                    .border(1.dp, Color(0xFF333537), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No Chronicle logs has been recorded yet.",
                    color = Color(0xFF909094),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1A1C1E))
                    .border(1.dp, Color(0xFF333537), RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs) { log ->
                    LogItemRow(log = log)
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
fun BadgeMicroCard(badge: Badge) {
    val bIcon = when (badge.iconName) {
        "star" -> Icons.Default.Star
        "diamond" -> Icons.Default.AutoAwesome
        "assignment" -> Icons.Default.Assignment
        "emoji_events" -> Icons.Default.EmojiEvents
        "payments" -> Icons.Default.Payments
        "fitness_center" -> Icons.Default.FitnessCenter
        "bolt" -> Icons.Default.Bolt
        else -> Icons.Default.HistoryEdu
    }

    val iconColor = if (badge.isUnlocked) Color(0xFFD0BCFF) else Color(0xFF909094)
    val nameColor = if (badge.isUnlocked) Color(0xFFE2E2E6) else Color(0xFF909094)
    val bodyBg = if (badge.isUnlocked) Color(0xFF381E72) else Color(0xFF1A1C1E)
    val borderCol = if (badge.isUnlocked) Color(0xFFD0BCFF) else Color(0xFF333537)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bodyBg),
        modifier = Modifier
            .width(130.dp)
            .height(115.dp)
            .border(1.dp, borderCol, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2D2F31)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = bIcon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = badge.title,
                color = nameColor,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = badge.description,
                color = Color(0xFF909094),
                fontSize = 9.sp,
                textAlign = TextAlign.Center,
                lineHeight = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun LogItemRow(log: GameLog) {
    // Decipher style categories of logging feed
    val meta = when (log.logType) {
        "EXP" -> Pair("🌟", Color(0xFF00BCD4))
        "GOLD" -> Pair("🪙", Color(0xFFFFB300))
        "DAMAGE" -> Pair("💥", Color(0xFFFF5252))
        "LEVEL_UP" -> Pair("🎉", Color(0xFFE040FB))
        "BADGE" -> Pair("🏆", Color(0xFFFF9800))
        "DEBUFF_ON" -> Pair("⚠️", Color(0xFFFF5252))
        "DEBUFF_OFF" -> Pair("💖", Color(0xFF4CAF50))
        "HEAL" -> Pair("🧪", Color(0xFF4CAF50))
        else -> Pair("📜", Color(0xFFECEFF1))
    }

    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(meta.first, fontSize = 12.sp, modifier = Modifier.padding(end = 6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = log.message,
                color = Color(0xFFE2E2E6),
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
            Text(
                text = formatLogTime(log.timestamp),
                color = Color(0xFF909094),
                fontSize = 8.sp,
                modifier = Modifier.padding(top = 1.dp)
            )
        }
    }
}

fun formatLogTime(timestamp: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val min = cal.get(Calendar.MINUTE)
    return String.format("%02d:%02d", hour, min)
}

// ---------------------------------------------------------
// CHARACTER DESIGNER OVERLAY AND CLASS INFO DIALOGS
// ---------------------------------------------------------
@Composable
fun CharacterCreationSheet(
    character: UserCharacter,
    onSave: (String, String) -> Unit,
    onCancel: () -> Unit,
    allowCancel: Boolean
) {
    var heroName by remember { mutableStateOf(character.name) }
    var selectedClass by remember { mutableStateOf(character.characterClass) }

    val classes = listOf(
        Pair("Warrior", "⚔️ High STR/CON. SPECIALTY: Fitness, physical workout tasks, chores."),
        Pair("Mage", "🔮 High INT/AGI. SPECIALTY: Academic learning, rapid problem solving."),
        Pair("Rogue", "🗡️ High AGI/STR. SPECIALTY: Creative skills, sports practice, agility routines."),
        Pair("Scholar", "📚 High INT/CON. SPECIALTY: Book reading, quiet study, code development.")
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1113))
            .padding(24.dp)
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFFD0BCFF),
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Character Specialization Creator",
                color = Color(0xFFE2E2E6),
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Draft your RPG self avatar. Choose specialist classes to receive dynamic attribute bonuses when completing compatible quests.",
                color = Color(0xFF909094),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // Name edit text
            OutlinedTextField(
                value = heroName,
                onValueChange = { if (it.length <= 15) heroName = it },
                label = { Text("Hero Name", color = Color(0xFFD0BCFF)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFFE2E2E6),
                    unfocusedTextColor = Color(0xFFE2E2E6),
                    focusedBorderColor = Color(0xFFD0BCFF),
                    unfocusedBorderColor = Color(0xFF333537),
                    focusedContainerColor = Color(0xFF1A1C1E),
                    unfocusedContainerColor = Color(0xFF1A1C1E)
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hero_name_input")
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Select Character Class Guild",
                color = Color(0xFFE2E2E6),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Class selectable stack
            classes.forEach { (classType, desc) ->
                val active = selectedClass == classType
                val borderCol = if (active) Color(0xFFD0BCFF) else Color(0xFF333537)
                val bgCol = if (active) Color(0xFF381E72) else Color(0xFF1A1C1E)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(bgCol)
                        .border(1.dp, borderCol, RoundedCornerShape(14.dp))
                        .clickable { selectedClass = classType }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (classType == "Warrior") "⚔️" else if (classType == "Mage") "🔮" else if (classType == "Rogue") "🗡️" else "📚",
                        fontSize = 28.sp
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = classType,
                            color = Color(0xFFE2E2E6),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = desc,
                            color = Color(0xFF909094),
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Control Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (allowCancel) {
                    OutlinedButton(
                        onClick = onCancel,
                        border = BorderStroke(1.dp, Color(0xFF333537)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF909094)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Retain Guild")
                    }
                }

                Button(
                    onClick = { if (heroName.isNotBlank()) onSave(heroName, selectedClass) },
                    enabled = heroName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF), contentColor = Color(0xFF381E72)),
                    modifier = Modifier.weight(1f).testTag("save_hero_button")
                ) {
                    Text("Apply Attributes", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// RPG CLASS DETAILS INFO SHEET DIALOG
@Composable
fun RpgInfoDialog(
    character: UserCharacter,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C1E)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF333537), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFFD0BCFF),
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Character Specialization Rules",
                    color = Color(0xFFE2E2E6),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Completing compatible daily quests or habits gives dynamic bonuses based on specialization classes of choice:\n\n" +
                            "• Warrior: 3x strength, 2x constitution buffs during levelUp.\n" +
                            "• Mage: 3x intellect, 2x agility buffs during levelUp.\n" +
                            "• Rogue: 3x agility, 1x strength/intellect/con buffs.\n" +
                            "• Scholar: 3x intellect, 2x constitution buffs.\n\n" +
                            "Completing any positive quest has a 50% probability to instantly increase its designated attribute by +1!",
                    color = Color(0xFF909094),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381E72), contentColor = Color(0xFFD0BCFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Dismiss", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ADD QUEST FORM BUILDER DIALOG
@Composable
fun AddQuestDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var questType by remember { mutableStateOf("POSITIVE") } // DAILY, POSITIVE, NEGATIVE
    var difficulty by remember { mutableStateOf("EASY") } // EASY, MEDIUM, HARD
    var statType by remember { mutableStateOf("STR") } // STR, INT, AGI, CON

    var hasAttemptedSubmit by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C1E)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF333537), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Draft Blueprint Ledger",
                    color = Color(0xFFE2E2E6),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Title Input
                Column {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Quest Title (e.g., Read 10 Pages)", color = Color(0xFFD0BCFF)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFFE2E2E6),
                            unfocusedTextColor = Color(0xFFE2E2E6),
                            focusedBorderColor = Color(0xFFD0BCFF),
                            unfocusedBorderColor = Color(0xFF333537),
                            focusedContainerColor = Color(0xFF2D2F31),
                            unfocusedContainerColor = Color(0xFF2D2F31)
                        ),
                        singleLine = true,
                        isError = hasAttemptedSubmit && title.isBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_quest_title")
                    )
                    if (hasAttemptedSubmit && title.isBlank()) {
                        Text("Title is mandatory", color = Color(0xFFFF5449), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
                    }
                }

                // Description Input
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Quest Narrative / Notes", color = Color(0xFFD0BCFF)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFE2E2E6),
                        unfocusedTextColor = Color(0xFFE2E2E6),
                        focusedBorderColor = Color(0xFFD0BCFF),
                        unfocusedBorderColor = Color(0xFF333537),
                        focusedContainerColor = Color(0xFF2D2F31),
                        unfocusedContainerColor = Color(0xFF2D2F31)
                    ),
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                // Habit/Quest Type Select Stack
                Column {
                    Text("Quest Classification Block", color = Color(0xFFE2E2E6), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val types = listOf(
                            Triple("DAILY", "Daily", Color(0xFF00BCD4)),
                            Triple("POSITIVE", "Habit (+)", Color(0xFF4CAF50)),
                            Triple("NEGATIVE", "Hazard (-)", Color(0xFFFF5449))
                        )
                        types.forEach { (typeId, label, activeColor) ->
                            val isSelected = questType == typeId
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) activeColor else Color(0xFF2D2F31))
                                    .clickable { questType = typeId }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color(0xFF121214) else Color(0xFF909094),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Difficulty selectors
                Column {
                    Text("Loot / Danger Difficulty Grade", color = Color(0xFFE2E2E6), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val levels = listOf("EASY", "MEDIUM", "HARD")
                        levels.forEach { level ->
                            val isSelected = difficulty == level
                            val activeBg = when (level) {
                                "EASY" -> Color(0xFF4CAF50)
                                "MEDIUM" -> Color(0xFFFFB300)
                                "HARD" -> Color(0xFFFF5449)
                                else -> Color.White
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) activeBg else Color(0xFF2D2F31))
                                    .clickable { difficulty = level }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = level,
                                    color = if (isSelected) Color(0xFF121214) else Color(0xFF909094),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Associated Attribute
                Column {
                    Text("Core Attribute Associated Skill", color = Color(0xFFE2E2E6), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val stats = listOf(
                            Pair("STR", "STR 🏋️"),
                            Pair("INT", "INT 🧠"),
                            Pair("AGI", "AGI 🏃"),
                            Pair("CON", "CON 🛡️")
                        )
                        stats.forEach { (statId, label) ->
                            val isSelected = statType == statId
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF381E72) else Color(0xFF2D2F31))
                                    .border(1.dp, if (isSelected) Color(0xFFD0BCFF) else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { statType = statId }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color(0xFFD0BCFF) else Color(0xFF909094),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Dialog Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, Color(0xFF333537)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF909094)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Discard Block")
                    }

                    Button(
                        onClick = {
                            hasAttemptedSubmit = true
                            if (title.isNotBlank()) {
                                onSave(title, desc.ifBlank { "A generic routine habit quest." }, questType, difficulty, statType)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF), contentColor = Color(0xFF381E72)),
                        modifier = Modifier.weight(1f).testTag("save_quest_button")
                    ) {
                        Text("Draft Quest", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
