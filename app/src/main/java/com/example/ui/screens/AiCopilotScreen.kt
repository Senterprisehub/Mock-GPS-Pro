package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RouteItem
import com.example.service.ChatMessage
import com.example.service.CopilotAction
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BlueDark
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.BlueSubtle
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoseDanger
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.MockGpsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun AiCopilotScreen(
    viewModel: MockGpsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val chatMessages by viewModel.copilotMessages.collectAsState()
    val isGenerating by viewModel.isCopilotGenerating.collectAsState()
    val isMocking by viewModel.isMocking.collectAsState()
    val coordinates by viewModel.coordinates.collectAsState()

    var textInput by remember { mutableStateOf("") }

    // Auto-scroll to bottom on new message
    LaunchedEffect(chatMessages.size, isGenerating) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    val quickPrompts = listOf(
        "🗺️ Route from current location to Tokyo Tower",
        "🗺️ Route from Central Park to Times Square",
        "📍 Teleport to Shibuya Crossing",
        "🚗 Set speed to 45 km/h",
        "▶️ Start mock location",
        "🛡️ How to make mock location untraceable?",
        "🛰️ Explain NMEA satellite sentences"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
            .imePadding()
            .testTag("ai_copilot_screen")
    ) {
        // TOP APP HEADER
        Surface(
            color = PureWhite,
            shadowElevation = 2.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(BlueSubtle)
                            .border(1.dp, BluePrimary.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Copilot",
                            tint = BluePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Gemini AI Copilot",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(EmeraldSuccess.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "100% FREE FOREVER",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldSuccess
                                )
                            }
                        }

                        Text(
                            text = "Zero API Keys Required • Instant App Automation",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Slate600,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                // Clear Chat History Button
                IconButton(
                    onClick = {
                        viewModel.clearCopilotMessages()
                        Toast.makeText(context, "Chat history cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Slate600)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear History",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // CHAT MESSAGE LIST
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Introductory Greeting Card if conversation is empty
            if (chatMessages.isEmpty()) {
                item {
                    WelcomeCopilotCard(
                        onSelectPrompt = { prompt ->
                            viewModel.sendCopilotMessage(prompt)
                        }
                    )
                }
            }

            items(chatMessages, key = { it.id }) { message ->
                ChatMessageBubble(
                    message = message,
                    onExecuteAction = { action ->
                        when (action) {
                            is CopilotAction.SetLocation -> {
                                viewModel.updateCoordinates(action.lat, action.lng, "AI_COPILOT_TELEPORT")
                                viewModel.setTab(AppTab.MAP)
                                Toast.makeText(context, "📍 Teleported to ${action.name}!", Toast.LENGTH_SHORT).show()
                            }
                            is CopilotAction.SetSpeed -> {
                                viewModel.updateSpeed(action.speedKmh)
                                Toast.makeText(context, "🚗 Speed set to ${action.speedKmh.toInt()} km/h", Toast.LENGTH_SHORT).show()
                            }
                            is CopilotAction.LoadRoute -> {
                                viewModel.loadAiGeneratedRoute(action.title, action.waypoints)
                                viewModel.setTab(AppTab.MAP)
                                Toast.makeText(context, "🗺️ Loaded & simulated route '${action.title}'!", Toast.LENGTH_SHORT).show()
                            }
                            is CopilotAction.StartMock -> {
                                if (!isMocking) viewModel.toggleMocking()
                                Toast.makeText(context, "▶️ Mock Location Started", Toast.LENGTH_SHORT).show()
                            }
                            is CopilotAction.StopMock -> {
                                if (isMocking) viewModel.toggleMocking()
                                Toast.makeText(context, "⏹️ Mock Location Stopped", Toast.LENGTH_SHORT).show()
                            }
                            is CopilotAction.OpenSettings -> {
                                viewModel.setTab(AppTab.SETTINGS)
                            }
                            is CopilotAction.OpenRoutes -> {
                                viewModel.setTab(AppTab.ROUTES)
                            }
                        }
                    },
                    onSaveToRoutes = { routeAction ->
                        viewModel.saveAiRouteAndOpenRoutesTab(routeAction.title, routeAction.waypoints)
                        Toast.makeText(context, "📁 Route '${routeAction.title}' saved to Routes Tab!", Toast.LENGTH_SHORT).show()
                    },
                    onCopyText = { text ->
                        clipboardManager.setText(AnnotatedString(text))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Typing Indicator when waiting for Gemini
            if (isGenerating) {
                item {
                    AiTypingIndicator()
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // QUICK PROMPTS CHIPS ROW
        Surface(
            color = PureWhite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(color = Slate200)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickPrompts.forEach { prompt ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Slate100)
                                .border(1.dp, Slate200, RoundedCornerShape(16.dp))
                                .clickable(enabled = !isGenerating) {
                                    viewModel.sendCopilotMessage(prompt)
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = prompt,
                                fontSize = 11.sp,
                                color = Slate800,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // BOTTOM TEXT INPUT BAR
        Surface(
            color = PureWhite,
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = {
                        Text("Ask AI Copilot (e.g. Plan route, Teleport)...", fontSize = 13.sp, color = Slate400)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_copilot_input"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BluePrimary,
                        unfocusedBorderColor = Slate300,
                        focusedContainerColor = PureWhite,
                        unfocusedContainerColor = Slate50
                    ),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (textInput.isNotBlank() && !isGenerating) {
                                val msg = textInput.trim()
                                textInput = ""
                                viewModel.sendCopilotMessage(msg)
                            }
                        }
                    ),
                    trailingIcon = {
                        if (textInput.isNotBlank()) {
                            IconButton(onClick = { textInput = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Slate400, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                )

                // Send Button
                IconButton(
                    onClick = {
                        if (textInput.isNotBlank() && !isGenerating) {
                            val msg = textInput.trim()
                            textInput = ""
                            viewModel.sendCopilotMessage(msg)
                        }
                    },
                    enabled = textInput.isNotBlank() && !isGenerating,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (textInput.isNotBlank() && !isGenerating) BluePrimary else Slate200,
                        contentColor = PureWhite,
                        disabledContainerColor = Slate100,
                        disabledContentColor = Slate400
                    ),
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .testTag("ai_copilot_send_button")
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = BluePrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeCopilotCard(
    onSelectPrompt: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(BlueSubtle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = BluePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = "Hello! I am your AI Copilot",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                    )
                    Text(
                        text = "Ready to control, navigate, and simulate realistic GPS paths.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                    )
                }
            }

            HorizontalDivider(color = Slate100)

            Text(
                text = "💡 Tap any action to try it immediately:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Slate800
            )

            val samples = listOf(
                Pair("🗺️ Route to Tokyo Tower", "Make a waypoint route from current location to Tokyo Tower"),
                Pair("🗺️ Central Park to Times Square", "Generate a waypoint route from Central Park to Times Square"),
                Pair("📍 Teleport to Tokyo Shibuya", "Teleport to Tokyo Shibuya Crossing and set walking speed"),
                Pair("🚗 Set Speed to 45 km/h", "Set simulation speed to 45 km/h for city driving"),
                Pair("🛡️ Check Mock Detection Safety", "How does this app simulate realistic NMEA and GPS satellites?")
            )

            samples.forEach { (label, query) ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Slate50)
                        .border(1.dp, Slate200, RoundedCornerShape(10.dp))
                        .clickable { onSelectPrompt(query) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = BlueDark
                        )
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = null,
                            tint = BluePrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    onExecuteAction: (CopilotAction) -> Unit,
    onSaveToRoutes: (CopilotAction.LoadRoute) -> Unit,
    onCopyText: (String) -> Unit
) {
    val isUser = message.role == "user"

    val timeStr = remember(message.timestamp) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(Date(message.timestamp))
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (isUser) "You" else "Gemini Copilot",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isUser) BluePrimary else Slate700
            )
            Text(
                text = "• $timeStr",
                fontSize = 10.sp,
                color = Slate400
            )
        }

        Surface(
            color = if (isUser) BluePrimary else PureWhite,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            border = if (isUser) null else androidx.compose.foundation.BorderStroke(1.dp, Slate200),
            shadowElevation = if (isUser) 1.dp else 2.dp,
            modifier = Modifier.fillMaxWidth(if (isUser) 0.88f else 0.95f)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Formatted Message Text
                FormattedMessageText(
                    rawText = message.content,
                    isUser = isUser
                )

                // Render Action Cards if generated by AI
                if (message.actions.isNotEmpty()) {
                    HorizontalDivider(color = Slate200)

                    Text(
                        text = "⚡ QUICK APP ACTIONS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = BluePrimary
                    )

                    message.actions.forEach { action ->
                        ActionCard(
                            action = action,
                            onExecute = { onExecuteAction(action) },
                            onSaveToRoutes = if (action is CopilotAction.LoadRoute) {
                                { onSaveToRoutes(action) }
                            } else null
                        )
                    }
                }

                // Copy Action Footer for Assistant messages
                if (!isUser) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = { onCopyText(message.content) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = Slate400,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormattedMessageText(
    rawText: String,
    isUser: Boolean
) {
    if (isUser) {
        Text(
            text = rawText,
            color = PureWhite,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
        return
    }

    // Render formatted markdown-like text
    val formatted = remember(rawText) {
        buildAnnotatedString {
            val lines = rawText.lines()
            for ((index, line) in lines.withIndex()) {
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("### ") -> {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Slate900)) {
                            append(trimmed.removePrefix("### "))
                        }
                    }
                    trimmed.startsWith("## ") -> {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Slate900)) {
                            append(trimmed.removePrefix("## "))
                        }
                    }
                    trimmed.startsWith("# ") -> {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BlueDark)) {
                            append(trimmed.removePrefix("# "))
                        }
                    }
                    trimmed.startsWith("• ") || trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                        withStyle(SpanStyle(color = BluePrimary, fontWeight = FontWeight.Bold)) {
                            append(" • ")
                        }
                        appendInlineBold(trimmed.substring(2))
                    }
                    else -> {
                        appendInlineBold(line)
                    }
                }
                if (index < lines.size - 1) {
                    append("\n")
                }
            }
        }
    }

    Text(
        text = formatted,
        color = Slate800,
        fontSize = 13.sp,
        lineHeight = 19.sp
    )
}

private fun AnnotatedString.Builder.appendInlineBold(text: String) {
    var cursor = 0
    while (cursor < text.length) {
        val startBold = text.indexOf("**", cursor)
        if (startBold == -1) {
            append(text.substring(cursor))
            break
        }
        append(text.substring(cursor, startBold))
        val endBold = text.indexOf("**", startBold + 2)
        if (endBold == -1) {
            append(text.substring(startBold))
            break
        }
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Slate900)) {
            append(text.substring(startBold + 2, endBold))
        }
        cursor = endBold + 2
    }
}

@Composable
private fun ActionCard(
    action: CopilotAction,
    onExecute: () -> Unit,
    onSaveToRoutes: (() -> Unit)? = null
) {
    when (action) {
        is CopilotAction.SetLocation -> {
            Surface(
                color = BlueSubtle,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BluePrimary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(20.dp))
                        Column {
                            Text(
                                text = action.name,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = BlueDark)
                            )
                            Text(
                                text = String.format(Locale.US, "%.5f, %.5f", action.lat, action.lng),
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = Slate600, fontSize = 10.sp)
                            )
                        }
                    }

                    Button(
                        onClick = onExecute,
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary, contentColor = PureWhite),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Teleport", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        is CopilotAction.SetSpeed -> {
            Surface(
                color = Slate100,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate300),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = Slate800, modifier = Modifier.size(20.dp))
                        Column {
                            Text(
                                text = "Simulation Speed: ${action.label}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                            )
                            Text(
                                text = "${action.speedKmh.toInt()} km/h (${String.format(Locale.US, "%.1f", action.speedKmh / 3.6f)} m/s)",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = Slate600, fontSize = 10.sp)
                            )
                        }
                    }

                    Button(
                        onClick = onExecute,
                        colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = PureWhite),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Apply", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        is CopilotAction.LoadRoute -> {
            Surface(
                color = BlueSubtle,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BluePrimary.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(BluePrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AltRoute, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = action.title,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = BlueDark, fontSize = 13.sp)
                            )
                            Text(
                                text = "${action.waypoints.size} checkpoints • Auto-saved to Routes Tab",
                                style = MaterialTheme.typography.labelSmall.copy(color = Slate600, fontSize = 10.sp)
                            )
                        }
                    }

                    // Action buttons: 1) Simulate on Map, 2) View in Routes Tab
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onExecute,
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary, contentColor = PureWhite),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Simulate on Map", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (onSaveToRoutes != null) {
                            OutlinedButton(
                                onClick = onSaveToRoutes,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = BlueDark),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BluePrimary.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(16.dp), tint = BlueDark)
                                    Text("View in Routes", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
        is CopilotAction.StartMock -> {
            Surface(
                color = EmeraldSuccess.copy(alpha = 0.1f),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Start GPS Simulation",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                        )
                    }
                    Button(
                        onClick = onExecute,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = PureWhite),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Start", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        is CopilotAction.StopMock -> {
            Surface(
                color = RoseDanger.copy(alpha = 0.1f),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, RoseDanger.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, tint = RoseDanger, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Stop GPS Simulation",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = RoseDanger)
                        )
                    }
                    Button(
                        onClick = onExecute,
                        colors = ButtonDefaults.buttonColors(containerColor = RoseDanger, contentColor = PureWhite),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Stop", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        is CopilotAction.OpenSettings -> {
            Surface(
                color = Slate100,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate300),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = Slate800, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Open Developer Settings",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                        )
                    }
                    Button(
                        onClick = onExecute,
                        colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = PureWhite),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Settings", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        is CopilotAction.OpenRoutes -> {
            Surface(
                color = BlueSubtle,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BluePrimary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AltRoute, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Open Routes Library",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = BlueDark)
                        )
                    }
                    Button(
                        onClick = onExecute,
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary, contentColor = PureWhite),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Routes", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AiTypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "TypingTransition")
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 0), repeatMode = RepeatMode.Reverse),
        label = "Dot1"
    )
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 200), repeatMode = RepeatMode.Reverse),
        label = "Dot2"
    )
    val alpha3 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 400), repeatMode = RepeatMode.Reverse),
        label = "Dot3"
    )

    Surface(
        color = PureWhite,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = BluePrimary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Gemini is thinking",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Slate600
            )
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(BluePrimary.copy(alpha = alpha1)))
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(BluePrimary.copy(alpha = alpha2)))
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(BluePrimary.copy(alpha = alpha3)))
        }
    }
}
