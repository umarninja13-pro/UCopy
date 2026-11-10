package com.umkt.ucopy.utils

import android.content.Context

data class AISuggestions(
    val suggestedCategories: List<String>,
    val suggestedTitle: String?
)

class AITagAnalyzer(private val context: Context) {

    suspend fun analyzeText(text: String): AISuggestions {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val contentAnalysis = analyzeContent(text)
            val categories = detectDynamicCategories(text, contentAnalysis)
            val title = generateTitle(text)

            AISuggestions(
                suggestedCategories = categories.take(3),
                suggestedTitle = title
            )
        }
    }

    private fun analyzeContent(text: String): ContentAnalysis {
        val lower = text.lowercase()
        val hasCodeBlock = text.contains("```") || text.contains("`")
        val hasStoryElements = listOf("once upon", "character", "story", "plot").any { lower.contains(it) }
        val hasTechnicalDocs = listOf("documentation", "api", "reference", "function").any { lower.contains(it) }
        val hasBusinessContent = listOf("project", "meeting", "client", "deadline").any { lower.contains(it) }
        
        return ContentAnalysis(
            isCode = hasCodeBlock,
            isStory = hasStoryElements,
            isTechnical = hasTechnicalDocs,
            isBusiness = hasBusinessContent
        )
    }

    private fun detectDynamicCategories(text: String, analysis: ContentAnalysis): List<String> {
        val combined = text.lowercase()
        val categoryKeywordMap = mapOf(
            "Story" to listOf("story", "tale", "character", "plot", "narrative"),
            "Code" to listOf("function", "class", "object", "val", "var", "fun", "import", "android", "kotlin"),
            "Business" to listOf("project", "meeting", "client", "deadline", "budget", "strategy"),
            "Personal" to listOf("my", "i", "me", "myself", "personal", "diary", "journal"),
            "Ideas" to listOf("idea", "think", "plan", "goal", "dream", "future")
        )
        val scores = categoryKeywordMap.mapValues { (_, keywords) -> keywords.sumOf { if (combined.contains(it)) 1 else 0 } }.toMutableMap()
        
        if (analysis.isStory) scores["Story"] = (scores["Story"] ?: 0) + 5
        if (analysis.isCode) scores["Code"] = (scores["Code"] ?: 0) + 5
        if (analysis.isBusiness) scores["Business"] = (scores["Business"] ?: 0) + 5

        return scores.filter { it.value > 0 }.entries.sortedByDescending { it.value }.map { it.key }.take(3).ifEmpty { listOf("Notes") }
    }

    private fun generateTitle(text: String): String? {
        val lines = text.split("\n").filter { it.isNotBlank() }
        val firstTwoLines = lines.take(2)
        for (line in firstTwoLines) {
            val trimmed = line.trim()
            if (trimmed.length in 3..80 && !trimmed.startsWith("#") && !trimmed.endsWith(".")) return trimmed
        }
        return text.take(60).replace("\n", " ").trim()
    }
    
    private data class ContentAnalysis(
        val isCode: Boolean, val isStory: Boolean, val isTechnical: Boolean, val isBusiness: Boolean
    )
}
