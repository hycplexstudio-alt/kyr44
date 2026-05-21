package com.example.data

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class ChatRepository(private val chatDao: ChatDao) {

    val allThreads: Flow<List<ChatThread>> = chatDao.getAllThreads()

    fun getMessagesForThread(threadId: Long): Flow<List<ChatMessage>> {
        return chatDao.getMessagesForThread(threadId)
    }

    suspend fun createThread(title: String): Long = withContext(Dispatchers.IO) {
        chatDao.insertThread(ChatThread(title = title))
    }

    suspend fun renameThread(threadId: Long, newTitle: String) = withContext(Dispatchers.IO) {
        val existing = chatDao.getThreadById(threadId)
        if (existing != null) {
            chatDao.updateThread(existing.copy(title = newTitle))
        }
    }

    suspend fun deleteThread(threadId: Long) = withContext(Dispatchers.IO) {
        chatDao.deleteThreadById(threadId)
    }

    suspend fun saveMessage(threadId: Long, role: String, text: String): Long = withContext(Dispatchers.IO) {
        chatDao.insertMessage(
            ChatMessage(
                threadId = threadId,
                role = role,
                text = text
            )
        )
    }

    suspend fun getAiResponse(threadId: Long, userPrompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Error: API Key de Gemini no configurada. Por favor, agregue su clave GEMINI_API_KEY en el panel de Secrets de AI Studio."
        }

        // Get the historical messages for the thread
        val historyList = chatDao.getMessagesForThread(threadId).firstOrNull() ?: emptyList()

        // Map history to API model
        val apiContents = mutableListOf<Content>()
        for (msg in historyList) {
            val apiRole = if (msg.role == "user") "user" else "model"
            apiContents.add(
                Content(
                    role = apiRole,
                    parts = listOf(Part(text = msg.text))
                )
            )
        }

        // Also add the new userPrompt if not already in database status flow
        if (historyList.none { it.text == userPrompt }) {
            apiContents.add(
                Content(
                    role = "user",
                    parts = listOf(Part(text = userPrompt))
                )
            )
        }

        val systemInstruction = Content(
            parts = listOf(
                Part(
                    text = "Eres Kyr44, una inteligencia artificial conversacional avanzada, empática, ingeniosa, moderna e inteligente. " +
                           "Respondes de manera sumamente clara, amigable y constructiva en español (o el idioma que use el usuario). " +
                           "Eres capaz de ayudar en tareas complejas como programación, redacción creativa, resolución de problemas y debates interesantes. " +
                           "Preséntate siempre como Kyr44, con una personalidad única que mezcla un tono tecnológico, optimista y cercano. " +
                           "Si te piden código, muéstralo de forma limpia con formato legible."
                )
            )
        )

        val request = GenerateContentRequest(
            contents = apiContents,
            systemInstruction = systemInstruction,
            generationConfig = GenerationConfig(
                temperature = 0.7f
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "Kyr44 no pudo formular una respuesta en este momento. Inténtalo de nuevo."
        } catch (e: Exception) {
            "Ocurrió un error al contactar a Kyr44: ${e.localizedMessage ?: e.message}"
        }
    }
}
