package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ChatMessage
import com.example.data.ChatRepository
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val threads by viewModel.threads.collectAsStateWithLifecycle()
    val activeThreadId by viewModel.activeThreadId.collectAsStateWithLifecycle()
    val messages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val userInput by viewModel.userInput.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Dialog state for renaming thread
    var threadToRenameId by remember { mutableStateOf<Long?>(null) }
    var renameInputText by remember { mutableStateOf("") }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (threadToRenameId != null) {
        Dialog(onDismissRequest = { threadToRenameId = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicDarkSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Renombrar conversación",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = renameInputText,
                        onValueChange = { renameInputText = it },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CosmicPurple,
                            unfocusedBorderColor = CosmicBlue,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("rename_input")
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { threadToRenameId = null }) {
                            Text("Cancelar", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                viewModel.renameThread(threadToRenameId!!, renameInputText)
                                threadToRenameId = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CosmicPurple),
                            modifier = Modifier.testTag("confirm_rename_button")
                        ) {
                            Text("Guardar", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = CosmicDarkSurface,
                drawerContentColor = Color.White,
                modifier = Modifier.width(300.dp)
            ) {
                Spacer(modifier = Modifier.height(36.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Conversaciones",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = CosmicPurple
                    )
                    IconButton(
                        onClick = {
                            viewModel.createNewThread()
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier
                            .background(
                                brush = Brush.linearGradient(listOf(CosmicPurple, CosmicPink)),
                                shape = CircleShape
                            )
                            .size(40.dp)
                            .testTag("new_chat_button"),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Nueva conversación",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Divider(color = CosmicDarkCard, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (threads.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Sin conversaciones históricas",
                                    color = TextSecondary,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(threads, key = { it.id }) { thread ->
                            val isActive = thread.id == activeThreadId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isActive) CosmicDarkCard else Color.Transparent)
                                    .border(
                                        width = if (isActive) 1.dp else 0.dp,
                                        color = if (isActive) CosmicPurple else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        viewModel.selectThread(thread.id)
                                        scope.launch { drawerState.close() }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isActive) Icons.Default.AutoAwesome else Icons.Default.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = if (isActive) CosmicPink else CosmicBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = thread.title,
                                    color = if (isActive) Color.White else TextSecondary,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                    fontSize = 14.sp
                                )
                                if (isActive) {
                                    IconButton(
                                        onClick = {
                                            renameInputText = thread.title
                                            threadToRenameId = thread.id
                                        },
                                        modifier = Modifier.size(32.dp).testTag("rename_thread_action")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Renombrar",
                                            tint = CosmicBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteThread(thread.id) },
                                        modifier = Modifier.size(32.dp).testTag("delete_thread_action")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Borrar",
                                            tint = CosmicPink,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Kyr44 AI - Versión 1.0",
                        color = CosmicPink,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Powered by Gemini 3.5 Flash",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = CosmicDarkBg,
            topBar = {
                LargeTopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Kyr44",
                                    style = TextStyle(
                                        brush = Brush.linearGradient(listOf(CosmicPurple, CosmicPink)),
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color(0xFF00E676), CircleShape)
                                        .border(1.dp, CosmicDarkBg, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Online",
                                    color = Color(0xFF00E676),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Inteligencia Artificial Conversacional",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("menu_drawer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu lateral",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.createNewThread() },
                            modifier = Modifier.testTag("top_add_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Nueva conversación",
                                tint = CosmicBlue
                            )
                        }
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = CosmicDarkBg,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
            ) {
                if (messages.isEmpty() && !isGenerating) {
                    // Show a lovely premium intro screen with quick suggestions
                    IntroScreen(
                        onSuggestionSelected = { suggestionText ->
                            viewModel.updateUserInput(suggestionText)
                            viewModel.sendMessage()
                        },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    // Conversation list
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            val isUser = msg.role == "user"
                            ChatBubbleContainer(isUser = isUser) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isUser) Icons.Default.Person else Icons.Default.SmartToy,
                                            contentDescription = null,
                                            tint = if (isUser) CosmicBlue else CosmicPink,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isUser) "Tú" else "Kyr44",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isUser) CosmicBlue else CosmicPink
                                        )
                                    }
                                    FormattedBubbleContent(text = msg.text)
                                }
                            }
                        }

                        if (isGenerating) {
                            item {
                                ChatBubbleContainer(isUser = false) {
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.SmartToy,
                                                contentDescription = null,
                                                tint = CosmicPink,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Kyr44 está pensando...",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CosmicPink
                                            )
                                        }
                                        PulseDotsIndicator()
                                    }
                                }
                            }
                        }
                    }
                }

                // Input bar area
                InputBar(
                    userInput = userInput,
                    onValueChange = { viewModel.updateUserInput(it) },
                    onSendClicked = { viewModel.sendMessage() },
                    isGenerating = isGenerating
                )
            }
        }
    }
}

@Composable
fun ChatBubbleContainer(
    isUser: Boolean,
    content: @Composable () -> Unit
) {
    val containerColor = if (isUser) CosmicDarkCard else CosmicDarkSurface
    val borderStroke = if (isUser) 1.dp else 0.dp
    val borderColor = if (isUser) CosmicBlue.copy(alpha = 0.5f) else Color.Transparent

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .background(containerColor, RoundedCornerShape(18.dp))
                .border(borderStroke, borderColor, RoundedCornerShape(18.dp))
                .padding(14.dp)
        ) {
            content()
        }
    }
}

@Composable
fun FormattedBubbleContent(text: String) {
    // If the response contains markdown code blocks, render elegantly
    if (text.contains("```")) {
        val segments = text.split("```")
        Column {
            segments.forEachIndexed { index, segment ->
                if (index % 2 == 1) {
                    // Code block
                    val lines = segment.trim().lines()
                    val language = lines.firstOrNull()?.trim() ?: ""
                    val codeContent = if (lines.size > 1) lines.drop(1).joinToString("\n") else segment

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .background(Color(0xFF07080F), RoundedCornerShape(8.dp))
                            .border(1.dp, CosmicPink.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        if (language.isNotEmpty()) {
                            Text(
                                text = language.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CosmicPink,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                        Text(
                            text = codeContent,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color(0xFFA5D6A7),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    // Plain text block
                    if (segment.isNotEmpty()) {
                        Text(
                            text = segment.trim(),
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    } else {
        Text(
            text = text,
            fontSize = 14.sp,
            color = Color.White,
            lineHeight = 20.sp
        )
    }
}

@Composable
fun IntroScreen(
    onSuggestionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = CosmicPurple,
            modifier = Modifier
                .size(64.dp)
                .background(CosmicDarkSurface, CircleShape)
                .padding(12.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Bienvenido a Kyr44",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tu compañera inteligente. Pregúntame sobre programación, literatura, ideas creativas o pídeme un consejo.",
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Prueba con estos temas:",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = CosmicPink,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(12.dp))

        val suggestions = listOf(
            "💡 Dame ideas creativas para crear una app móvil",
            "✍️ Escribe un poema futurista corto sobre la IA",
            "🚀 Explica de forma súper simple la mecánica cuántica",
            "🛡️ Dame 5 consejos prácticos de seguridad cibernética"
        )

        suggestions.forEach { suggestion ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { onSuggestionSelected(suggestion.substring(3)) },
                colors = CardDefaults.cardColors(containerColor = CosmicDarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(listOf(CosmicPurple.copy(alpha = 0.3f), CosmicBlue.copy(alpha = 0.3f)))
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = suggestion,
                        fontSize = 13.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun InputBar(
    userInput: String,
    onValueChange: (String) -> Unit,
    onSendClicked: () -> Unit,
    isGenerating: Boolean
) {
    val focusManager = LocalFocusManager.current

    Surface(
        color = CosmicDarkBg,
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = userInput,
                onValueChange = onValueChange,
                placeholder = { Text("Escribe un mensaje a Kyr44...", color = TextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CosmicPurple,
                    unfocusedBorderColor = CosmicDarkCard,
                    focusedContainerColor = CosmicDarkSurface,
                    unfocusedContainerColor = CosmicDarkSurface,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1.0f)
                    .testTag("chat_input_field"),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        onSendClicked()
                        focusManager.clearFocus()
                    }
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            IconButton(
                onClick = {
                    onSendClicked()
                    focusManager.clearFocus()
                },
                enabled = userInput.isNotBlank() && !isGenerating,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        brush = Brush.linearGradient(
                            if (userInput.isNotBlank() && !isGenerating) {
                                listOf(CosmicPurple, CosmicPink)
                            } else {
                                listOf(CosmicDarkCard, CosmicDarkCard)
                            }
                        ),
                        shape = CircleShape
                    )
                    .testTag("send_chat_button"),
                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Enviar mensaje",
                    modifier = Modifier.scale(0.9f)
                )
            }
        }
    }
}

@Composable
fun PulseDotsIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val dotCount = 3
    val itemRange = 0 until dotCount

    val scales = itemRange.map { index ->
        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(index * 200)
            ),
            label = "scale_$index"
        )
    }

    Row(
        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        scales.forEach { scaleState ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .scale(scaleState.value)
                    .background(CosmicPink, CircleShape)
            )
        }
    }
}
