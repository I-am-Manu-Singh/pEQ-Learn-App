package com.creativeblisslabs.peqlearn.app.data.repository

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import com.creativeblisslabs.peqlearn.app.data.Raaga

class RagaRepository(private val httpClient: HttpClient) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true // Gracefully switches null fields to declared data class defaults
    }

    // Host this custom matrix endpoint where your data class structures are mirrored safely
    private val TARGET_RAGA_API = "https://raw.githubusercontent.com/I-am-Manu-Singh/peq-learn-data/main/ragas_expanded_matrix.json"

    suspend fun fetchExpandedRaagas(): List<Raaga> = withContext(Dispatchers.IO) {
        try {
            val response: String = httpClient.get(TARGET_RAGA_API).bodyAsText()
            
            // Decodes individual values cleanly using serialization arrays
            json.decodeFromString<List<Raaga>>(response)
        } catch (e: Exception) {
            println("pEQLearn: API pipeline down, dropping to runtime generator: ${e.message}")
            generateHardwareFallbackMatrix()
        }
    }

    /**
     * Completely self-contained local matrix generator matching the 
     * expanded Raaga signature perfectly for fail-safe fallback operation.
     */
    private fun generateHardwareFallbackMatrix(): List<Raaga> {
        return listOf(
            Raaga(
                name = "Yaman",
                thaat = "Kalyan",
                aroha = "Ni Re Ga Ma Pa Dha Ni Sa",
                avaroha = "Sa Ni Dha Pa Ma Ga Re Sa",
                vadi = "Ga",
                samvadi = "Ni",
                time = "Evening (6 PM - 9 PM)",
                mood = "Devotional",
                semitones = listOf(0, 2, 4, 6, 7, 9, 11, 12),
                description = "A serene evening raga using Teevra Madhyam.",
                pakad = "Ni Re Ga Re Sa, Pa Ma(t) Ga Re Sa",
                chalan = "Ni Re Ga, Ma Pa Dha Ni Sa",
                jati = "Sampoorna",
                alaap = listOf("Ni Re Ga... Re Sa"),
                bandish = "Eri Aali Piya Bina",
                sthai = "Eri aali piya bina kal na parat mohe",
                antara = "Jab se piya pardesh sidhare...",
                taal = "Teental",
                bolTaal = "Dha Dhin Dhin Dha",
                taansPaltas = listOf("Ni Re Ga Ma Pa Dha Ni Sa"),
                jhala = "Da Ra Da Ra"
            )
        )
    }
}