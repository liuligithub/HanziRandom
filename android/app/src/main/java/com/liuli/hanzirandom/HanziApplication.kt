package com.liuli.hanzirandom

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.liuli.hanzirandom.data.CharacterRepository
import com.liuli.hanzirandom.data.SettingsRepository
import com.liuli.hanzirandom.data.SharedPrefsKeyValueStore
import com.liuli.hanzirandom.domain.SpeechService

class HanziApplication : Application() {
    private val keyValueStore by lazy { SharedPrefsKeyValueStore(this) }

    val characterRepository by lazy { CharacterRepository(keyValueStore) }
    val settingsRepository by lazy { SettingsRepository(keyValueStore) }
    val speechService by lazy { SpeechService(this) }

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStop(owner: LifecycleOwner) {
                    characterRepository.persistRound()
                    speechService.shutdown()
                }
            }
        )
    }
}
