package com.fuse9.persistence

import com.fuse9.game.GameState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

val FuseJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
    allowStructuredMapKeys = true
}

/**
 * Offline-first save of the board in progress. Writes go to a temp file and are renamed into
 * place, so a kill mid-write never leaves a corrupt save behind.
 */
class SaveRepository(private val dir: File) {
    private val file get() = File(dir, "game.json")

    suspend fun save(state: GameState) = withContext(Dispatchers.IO) { writeAtomically(file, FuseJson.encodeToString(GameState.serializer(), state)) }

    fun saveBlocking(state: GameState) = writeAtomically(file, FuseJson.encodeToString(GameState.serializer(), state))

    suspend fun load(): GameState? = withContext(Dispatchers.IO) { loadBlocking() }

    fun loadBlocking(): GameState? = runCatching {
        if (!file.exists()) null else FuseJson.decodeFromString(GameState.serializer(), file.readText())
    }.getOrNull()

    fun hasSave(): Boolean = file.exists()

    suspend fun clear() = withContext(Dispatchers.IO) { file.delete() }
}

internal fun writeAtomically(target: File, text: String) {
    target.parentFile?.mkdirs()
    val tmp = File(target.parentFile, target.name + ".tmp")
    tmp.writeText(text)
    if (!tmp.renameTo(target)) {
        target.delete()
        tmp.renameTo(target)
    }
}
