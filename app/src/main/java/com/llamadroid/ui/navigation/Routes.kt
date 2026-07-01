package com.llamadroid.ui.navigation

object Routes {
    const val ONBOARDING = "onboarding"
    const val CONVERSATIONS = "conversations"
    const val CHAT = "chat/{conversationId}"
    const val CHAT_NEW = "chat/new"
    const val LIBRARY = "library"
    const val HUB = "hub"
    const val SETTINGS = "settings"
    const val SERVER = "server"
    const val BENCHMARK = "benchmark"
    const val SYSTEM_PROMPTS = "system_prompts"
    const val RAG = "rag"

    fun chat(id: String) = "chat/$id"
}
