package com.creativeblisslabs.peqlearn.app.data.repository

import com.creativeblisslabs.peqlearn.app.data.GroovePattern
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int

class GrooveRepository(private val loader: PatternLoader) {
    private val repoScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val _patterns = MutableStateFlow<List<GroovePattern>>(emptyList())
    val patterns: StateFlow<List<GroovePattern>> = _patterns.asStateFlow()

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    init {
        loadPatterns()
    }

    private fun loadPatterns() {
        repoScope.launch {
            try {
                val jsonStr = loader.loadGrooveJson().trim()
                if (jsonStr.isNotEmpty() && jsonStr != "{}") {
                    val element = jsonParser.parseToJsonElement(jsonStr)
                    val patternsList: List<GroovePattern> = when {
                        element is kotlinx.serialization.json.JsonObject && element.containsKey("patterns") -> {
                            val arr = element["patterns"] as? kotlinx.serialization.json.JsonArray
                            arr?.mapNotNull { item ->
                                runCatching {
                                    val obj = item.jsonObject
                                    val name = obj["name"]?.jsonPrimitive?.content ?: "Pattern"
                                    val kit = obj["kit"]?.jsonPrimitive?.content ?: "DRUM"
                                    val grid = obj["grid"]?.jsonArray?.map { it.jsonPrimitive.int } ?: emptyList()
                                    val events = grid.mapIndexedNotNull { idx, stepVal ->
                                        if (stepVal > 0) com.creativeblisslabs.peqlearn.app.data.BeatEvent(
                                            padIndex = 0,
                                            step = idx,
                                            velocity = if (stepVal > 1) 1.0f else 0.8f
                                        ) else null
                                    }
                                    GroovePattern(
                                        id = name,
                                        name = name,
                                        genre = kit,
                                        bpm = 120,
                                        stepsPerBar = 16,
                                        barsCount = 1,
                                        events = events,
                                        description = kit
                                    )
                                }.getOrNull()
                            } ?: emptyList()
                        }
                        element is kotlinx.serialization.json.JsonArray -> {
                            jsonParser.decodeFromString<List<GroovePattern>>(jsonStr)
                        }
                        else -> emptyList()
                    }
                    if (patternsList.isNotEmpty()) {
                        _patterns.value = patternsList
                    }
                }
            } catch (e: Exception) {
                // Keep default or fallback patterns
            }
        }
    }

    fun release() {
        repoScope.cancel()
    }
}
