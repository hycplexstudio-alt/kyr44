package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ChatMessage
import com.example.data.ChatRepository
import com.example.data.ChatThread
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(private val repository: ChatRepository) : ViewModel() {

    // List of all conversations (threads) saved in database
    val threads: StateFlow<List<ChatThread>> = repository.allThreads
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _activeThreadId = MutableStateFlow<Long?>(null)
    val activeThreadId: StateFlow<Long?> = _activeThreadId.asStateFlow()

    // Flag for active generation call
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    // Temporary user input
    private val _userInput = MutableStateFlow("")
    val userInput: StateFlow<String> = _userInput.asStateFlow()

    // Reactive flow of messages for whichever conversation is active
    @OptIn(ExperimentalCoroutinesApi::class)
    val activeMessages: StateFlow<List<ChatMessage>> = _activeThreadId
        .flatMapLatest { threadId ->
            if (threadId != null) {
                repository.getMessagesForThread(threadId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Automatically select the most recent thread if available, or create a default one
        viewModelScope.launch {
            threads.collect { list ->
                if (_activeThreadId.value == null && list.isNotEmpty()) {
                    _activeThreadId.value = list.first().id
                }
            }
        }
    }

    fun updateUserInput(text: String) {
        _userInput.value = text
    }

    fun selectThread(threadId: Long) {
        _activeThreadId.value = threadId
    }

    fun createNewThread(initialTitle: String = "Nueva conversacion") {
        viewModelScope.launch {
            val title = if (initialTitle.length > 20) {
                initialTitle.take(17) + "..."
            } else {
                initialTitle
            }
            val newId = repository.createThread(title)
            _activeThreadId.value = newId
        }
    }

    fun deleteThread(id: Long) {
        viewModelScope.launch {
            repository.deleteThread(id)
            if (_activeThreadId.value == id) {
                // Determine next selection or set null
                val remaining = threads.value.filter { it.id != id }
                if (remaining.isNotEmpty()) {
                    _activeThreadId.value = remaining.first().id
                } else {
                    _activeThreadId.value = null
                }
            }
        }
    }

    fun renameThread(id: Long, newTitle: String) {
        viewModelScope.launch {
            if (newTitle.isNotBlank()) {
                repository.renameThread(id, newTitle)
            }
        }
    }

    fun sendMessage() {
        val textToSend = _userInput.value.trim()
        if (textToSend.isEmpty() || _isGenerating.value) return

        _userInput.value = ""

        viewModelScope.launch {
            var currentThreadId = _activeThreadId.value
            if (currentThreadId == null) {
                // If there's no active thread, create one with the user's message as title
                val title = if (textToSend.length > 25) {
                    textToSend.take(22) + "..."
                } else {
                    textToSend
                }
                currentThreadId = repository.createThread(title)
                _activeThreadId.value = currentThreadId
            } else {
                // If thread exists but has default name, maybe update it for nice experience
                val currentThread = threads.value.find { it.id == currentThreadId }
                if (currentThread?.title == "Nueva conversacion") {
                    val title = if (textToSend.length > 25) {
                        textToSend.take(22) + "..."
                    } else {
                        textToSend
                    }
                    repository.renameThread(currentThreadId, title)
                }
            }

            // Save user message
            repository.saveMessage(currentThreadId, "user", textToSend)

            // Trigger generating state
            _isGenerating.value = true

            // Prompt Kyr44
            val aiResponse = repository.getAiResponse(currentThreadId, textToSend)

            // Save model message
            repository.saveMessage(currentThreadId, "model", aiResponse)

            _isGenerating.value = false
        }
    }
}

class ChatViewModelFactory(private val repository: ChatRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
