# 随机汉字 Android 版 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在同一仓库 `android/` 目录下实现与现有 iOS App 功能对齐的原生 Android 版「随机汉字」。

**Architecture:** 单 Activity + Jetpack Compose；`KeyValueStore` 抽象持久化，`CharacterRepository` 管理字库/错题/学习轮次，`DrawSession` 是可单测的抽卡状态机，`SettingsRepository` 管理偏好，`SpeechService` 封装 Android TTS。UI 用底部两个 Tab（学习/错题本）+ 栈式页面（设置/详情/复习）。

**Tech Stack:** Kotlin 2.0.21、Jetpack Compose（BOM 2024.09.03）、Material3、AGP 8.6.1、Gradle 8.7、JDK 17、JUnit4、Compose UI Test。

---

## 前置环境说明

- 本机已有：Android SDK（`/Users/liuli/Library/Android/sdk`，platform 35、build-tools 35.0.0）、模拟器 AVD `Pixel_Fold_API_35`、Android Studio、Gradle 8.7 发行版缓存（`~/.gradle/wrapper/dists/gradle-8.7-bin`）。
- 本机 JDK 为 11/25/26，AGP 8.6 需要 JDK 17：Task 1 先安装 Temurin 17。
- 所有 Gradle 命令都用 `JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew …` 前缀，避免误用 JDK 26。
- 如果 `brew install` 需要输入密码，请用户自己在会话里执行 `! brew install --cask temurin@17`。

---

### Task 1: 工程骨架 + 可构建的空 App

**Files:**
- Create: `android/settings.gradle.kts`
- Create: `android/build.gradle.kts`
- Create: `android/gradle.properties`
- Create: `android/.gitignore`
- Create: `android/app/build.gradle.kts`
- Create: `android/app/src/main/AndroidManifest.xml`
- Create: `android/app/src/main/res/values/strings.xml`
- Create: `android/app/src/main/res/values/themes.xml`
- Create: `android/app/src/main/java/com/liuli/hanzirandom/MainActivity.kt`
- Create (gitignored): `android/local.properties`
- Modify: `.gitignore`（根目录，追加 Android 忽略）

- [ ] **Step 1: 安装 JDK 17 并确认**

Run:
```bash
brew install --cask temurin@17
/usr/libexec/java_home -v 17
```
Expected: 输出 `/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home`。

- [ ] **Step 2: 写 Gradle 配置**

`android/settings.gradle.kts`:
```kotlin
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "HanziRandom"
include(":app")
```

`android/build.gradle.kts`:
```kotlin
plugins {
    id("com.android.application") version "8.6.1" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}
```

`android/gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
```

`android/app/build.gradle.kts`:
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.liuli.hanzirandom"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.liuli.hanzirandom"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-process:2.8.6")
    implementation("androidx.core:core-splashscreen:1.0.1")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")

    androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.03"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

- [ ] **Step 3: 写最小 Manifest、资源与 MainActivity**

`android/app/src/main/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:allowBackup="true"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.HanziRandom">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.HanziRandom">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

`android/app/src/main/res/values/strings.xml`:
```xml
<resources>
    <string name="app_name">随机汉字</string>
</resources>
```

`android/app/src/main/res/values/themes.xml`:
```xml
<resources>
    <style name="Theme.HanziRandom" parent="android:Theme.Material.Light.NoActionBar">
        <item name="android:forceDarkAllowed">false</item>
        <item name="android:windowLightStatusBar">true</item>
        <item name="android:windowLightNavigationBar">true</item>
    </style>
</resources>
```

`android/app/src/main/java/com/liuli/hanzirandom/MainActivity.kt`:
```kotlin
package com.liuli.hanzirandom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface { Text("随机汉字") }
            }
        }
    }
}
```

- [ ] **Step 4: 配置本地 SDK 与忽略文件**

`android/local.properties`（不提交）:
```properties
sdk.dir=/Users/liuli/Library/Android/sdk
```

根目录 `.gitignore` 追加：
```gitignore

# Android
android/.gradle/
android/.idea/
android/local.properties
android/**/build/
android/captures/
*.iml
```

`android/.gitignore`:
```gitignore
.gradle/
build/
local.properties
.idea/
captures/
*.iml
```

- [ ] **Step 5: 生成 Gradle wrapper**

Run:
```bash
cd android
JAVA_HOME=$(/usr/libexec/java_home -v 17) ~/.gradle/wrapper/dists/gradle-8.7-bin/*/gradle-8.7/bin/gradle wrapper --gradle-version 8.7
```
Expected: 生成 `gradlew`、`gradlew.bat`、`gradle/wrapper/`。

- [ ] **Step 6: 构建空 App**

Run:
```bash
cd android
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew :app:assembleDebug
```
Expected: `BUILD SUCCESSFUL`，产物在 `app/build/outputs/apk/debug/app-debug.apk`。

- [ ] **Step 7: 提交**

```bash
git add .gitignore android
git commit -m "chore(android): scaffold Compose app and Gradle build"
```

---

### Task 2: DrawSession 抽卡状态机（TDD）

**Files:**
- Test: `android/app/src/test/java/com/liuli/hanzirandom/domain/DrawSessionTest.kt`
- Create: `android/app/src/main/java/com/liuli/hanzirandom/domain/DrawSession.kt`

- [ ] **Step 1: 写失败测试**

`android/app/src/test/java/com/liuli/hanzirandom/domain/DrawSessionTest.kt`:
```kotlin
package com.liuli.hanzirandom.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawSessionTest {

    @Test
    fun afterFinished_previousThenNext_works() {
        val session = DrawSession(listOf("甲", "乙"))
        session.goNext()
        assertTrue(session.state.value.isFinished)

        val last = session.state.value.current
        session.goPrevious()
        assertNotEquals(last, session.state.value.current)
        assertTrue(session.state.value.canGoNext)

        session.goNext()
        assertEquals(last, session.state.value.current)
    }

    @Test
    fun canGoNext_falseOnlyAtHistoryEndWhenFinished() {
        val session = DrawSession(listOf("甲"))
        assertTrue(session.state.value.isFinished)
        assertFalse(session.state.value.canGoNext)
    }

    @Test
    fun removeCurrent_lastCharacter_finishesWithNoCurrent() {
        val session = DrawSession(listOf("甲"))
        session.removeCurrent()
        assertNull(session.state.value.current)
        assertTrue(session.state.value.isFinished)
        assertEquals(0, session.state.value.total)
    }

    @Test
    fun removeCurrent_midRound_showsAnotherCharacter() {
        val session = DrawSession(listOf("甲", "乙", "丙"))
        val removed = session.state.value.current
        session.removeCurrent()
        assertNotEquals(removed, session.state.value.current)
        assertNotNull(session.state.value.current)
        assertEquals(2, session.state.value.total)
    }

    @Test
    fun removeCharacter_currentlyShown_dropsItFromHistoryAndPool() {
        val session = DrawSession(listOf("甲", "乙", "丙"))
        val removed = session.state.value.current!!
        session.removeCharacter(removed)
        assertNotEquals(removed, session.state.value.current)
        assertFalse(session.state.value.history.contains(removed))
        assertFalse(session.state.value.pool.contains(removed))
        assertEquals(2, session.state.value.total)
    }

    @Test
    fun removeCharacter_stillInPool_dropsItAndKeepsCurrent() {
        val session = DrawSession(listOf("甲", "乙"))
        val shown = session.state.value.current!!
        val queued = session.state.value.pool[0]
        session.removeCharacter(queued)
        assertEquals(shown, session.state.value.current)
        assertFalse(session.state.value.pool.contains(queued))
        assertEquals(1, session.state.value.total)
        assertTrue(session.state.value.isFinished)
    }

    @Test
    fun removeCharacter_notInRound_isNoOp() {
        val session = DrawSession(listOf("甲", "乙"))
        val before = session.state.value
        session.removeCharacter("丙")
        assertEquals(before, session.state.value)
    }

    @Test
    fun drawNext_noRepeatsWithinRound() {
        val session = DrawSession(listOf("甲", "乙", "丙", "丁"))
        val seen = mutableListOf(session.state.value.current!!)
        while (!session.state.value.isFinished) {
            session.drawNext()
            session.state.value.current?.let(seen::add)
        }
        assertEquals(4, seen.size)
        assertEquals(4, seen.toSet().size)
    }

    @Test
    fun restore_reproducesState() {
        val session = DrawSession(emptyList())
        session.restore(
            RoundSnapshot(
                pool = listOf("丙"),
                history = listOf("甲", "乙"),
                cursor = 1,
                total = 3,
                isFinished = false
            )
        )
        assertEquals("乙", session.state.value.current)
        assertEquals(1, session.state.value.cursor)
        assertEquals(2, session.state.value.drawnCount)
        assertEquals(3, session.state.value.total)
        assertTrue(session.state.value.canGoPrevious)
        assertTrue(session.state.value.canGoNext)
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run:
```bash
cd android
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew :app:testDebugUnitTest --tests 'com.liuli.hanzirandom.domain.DrawSessionTest'
```
Expected: 编译失败，`Unresolved reference: DrawSession`。

- [ ] **Step 3: 实现 DrawSession**

`android/app/src/main/java/com/liuli/hanzirandom/domain/DrawSession.kt`:
```kotlin
package com.liuli.hanzirandom.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RoundSnapshot(
    val pool: List<String>,
    val history: List<String>,
    val cursor: Int,
    val total: Int,
    val isFinished: Boolean
)

data class SessionState(
    val pool: List<String> = emptyList(),
    val current: String? = null,
    val total: Int = 0,
    val drawnCount: Int = 0,
    val isFinished: Boolean = false,
    val history: List<String> = emptyList(),
    val cursor: Int = -1
) {
    val canGoPrevious: Boolean get() = cursor > 0
    val canGoNext: Boolean get() = cursor < history.size - 1 || !isFinished
    val position: Int get() = cursor + 1
}

/**
 * A single shuffled drawing round. Mirrors the iOS DrawSession state machine.
 */
class DrawSession(characters: List<String> = emptyList()) {
    private var characters: List<String> = characters
    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state.asStateFlow()

    init {
        restart()
    }

    fun restart(characters: List<String>? = null) {
        if (characters != null) this.characters = characters
        val pool = this.characters.shuffled()
        _state.value = SessionState(pool = pool, total = this.characters.size)
        if (pool.isEmpty()) {
            _state.value = _state.value.copy(isFinished = true)
            return
        }
        drawNext()
    }

    fun drawNext() {
        val s = _state.value
        if (s.isFinished || s.pool.isEmpty()) {
            _state.value = s.copy(isFinished = true)
            return
        }
        val index = s.pool.indices.random()
        val drawn = s.pool[index]
        val pool = s.pool.toMutableList().apply { removeAt(index) }
        val history = s.history + drawn
        _state.value = s.copy(
            pool = pool,
            current = drawn,
            history = history,
            cursor = history.lastIndex,
            drawnCount = s.drawnCount + 1,
            isFinished = pool.isEmpty()
        )
    }

    fun appendToPool(newCharacters: List<String>) {
        val s = _state.value
        if (s.isFinished || newCharacters.isEmpty()) return
        _state.value = s.copy(
            pool = s.pool + newCharacters,
            total = s.total + newCharacters.size
        )
    }

    fun removeCharacter(character: String) {
        val s = _state.value
        val index = s.history.indexOf(character)
        if (index >= 0) {
            if (index == s.cursor) {
                removeCurrent()
                return
            }
            val history = s.history.toMutableList().apply { removeAt(index) }
            val cursor = if (index < s.cursor) s.cursor - 1 else s.cursor
            _state.value = s.copy(
                history = history,
                cursor = cursor,
                current = if (cursor in history.indices) history[cursor] else null,
                total = maxOf(0, s.total - 1),
                drawnCount = history.size,
                isFinished = s.pool.isEmpty()
            )
            return
        }
        if (character !in s.pool) return
        val pool = s.pool.filter { it != character }
        _state.value = s.copy(
            pool = pool,
            total = maxOf(0, s.total - 1),
            isFinished = pool.isEmpty()
        )
    }

    fun removeCurrent() {
        val s = _state.value
        val shown = s.current ?: return
        if (s.cursor < 0) return

        val pool = s.pool.filter { it != shown }
        val history = s.history.toMutableList().apply { removeAt(s.cursor) }
        val total = maxOf(0, s.total - 1)
        val drawnCount = maxOf(0, s.drawnCount - 1)

        when {
            s.cursor < history.size -> {
                _state.value = s.copy(
                    pool = pool,
                    history = history,
                    current = history[s.cursor],
                    total = total,
                    drawnCount = drawnCount
                )
            }
            pool.isNotEmpty() -> {
                _state.value = s.copy(
                    pool = pool,
                    history = history,
                    total = total,
                    drawnCount = drawnCount
                )
                drawNext()
            }
            else -> {
                _state.value = s.copy(
                    pool = pool,
                    history = history,
                    current = null,
                    cursor = history.lastIndex,
                    total = total,
                    drawnCount = drawnCount,
                    isFinished = true
                )
            }
        }
    }

    fun restore(snapshot: RoundSnapshot) {
        _state.value = SessionState(
            pool = snapshot.pool,
            history = snapshot.history,
            current = if (snapshot.cursor in snapshot.history.indices) {
                snapshot.history[snapshot.cursor]
            } else {
                null
            },
            total = snapshot.total,
            drawnCount = snapshot.history.size,
            cursor = snapshot.cursor,
            isFinished = snapshot.isFinished
        )
    }

    fun goPrevious() {
        val s = _state.value
        if (s.cursor <= 0) return
        val cursor = s.cursor - 1
        _state.value = s.copy(cursor = cursor, current = s.history[cursor])
    }

    fun goNext() {
        val s = _state.value
        if (s.cursor < s.history.lastIndex) {
            val cursor = s.cursor + 1
            _state.value = s.copy(cursor = cursor, current = s.history[cursor])
        } else {
            drawNext()
        }
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run:
```bash
cd android
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew :app:testDebugUnitTest --tests 'com.liuli.hanzirandom.domain.DrawSessionTest'
```
Expected: `BUILD SUCCESSFUL`，9 个测试通过。

- [ ] **Step 5: 提交**

```bash
git add android/app/src
git commit -m "feat(android): port DrawSession round state machine"
```

---

### Task 3: KeyValueStore + SettingsRepository（TDD）

**Files:**
- Test: `android/app/src/test/java/com/liuli/hanzirandom/data/SettingsRepositoryTest.kt`
- Create: `android/app/src/main/java/com/liuli/hanzirandom/data/KeyValueStore.kt`
- Create: `android/app/src/main/java/com/liuli/hanzirandom/data/SettingsRepository.kt`

- [ ] **Step 1: 写失败测试**

`android/app/src/test/java/com/liuli/hanzirandom/data/SettingsRepositoryTest.kt`:
```kotlin
package com.liuli.hanzirandom.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRepositoryTest {

    @Test
    fun defaults_matchIos() {
        val repo = SettingsRepository(InMemoryKeyValueStore())
        assertEquals(150f, repo.fontSize.value, 0.001f)
        assertTrue(repo.swipeEnabled.value)
    }

    @Test
    fun values_persistAcrossInstances() {
        val store = InMemoryKeyValueStore()
        val first = SettingsRepository(store)
        first.setFontSize(200f)
        first.setSwipeEnabled(false)

        val second = SettingsRepository(store)
        assertEquals(200f, second.fontSize.value, 0.001f)
        assertFalse(second.swipeEnabled.value)
    }

    @Test
    fun fontSize_isClampedToRange() {
        val repo = SettingsRepository(InMemoryKeyValueStore())
        repo.setFontSize(10f)
        assertEquals(60f, repo.fontSize.value, 0.001f)
        repo.setFontSize(999f)
        assertEquals(260f, repo.fontSize.value, 0.001f)
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run:
```bash
cd android
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew :app:testDebugUnitTest --tests 'com.liuli.hanzirandom.data.SettingsRepositoryTest'
```
Expected: 编译失败，`Unresolved reference: SettingsRepository`。

- [ ] **Step 3: 实现**

`android/app/src/main/java/com/liuli/hanzirandom/data/KeyValueStore.kt`:
```kotlin
package com.liuli.hanzirandom.data

import android.content.Context

interface KeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun remove(key: String)
    fun getFloat(key: String, defaultValue: Float): Float
    fun putFloat(key: String, value: Float)
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
}

class SharedPrefsKeyValueStore(context: Context) : KeyValueStore {
    private val prefs = context.getSharedPreferences("hanzi_random", Context.MODE_PRIVATE)

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    override fun getFloat(key: String, defaultValue: Float): Float =
        prefs.getFloat(key, defaultValue)

    override fun putFloat(key: String, value: Float) {
        prefs.edit().putFloat(key, value).apply()
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        prefs.getBoolean(key, defaultValue)

    override fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }
}

class InMemoryKeyValueStore : KeyValueStore {
    private val strings = mutableMapOf<String, String>()
    private val floats = mutableMapOf<String, Float>()
    private val booleans = mutableMapOf<String, Boolean>()

    override fun getString(key: String): String? = strings[key]

    override fun putString(key: String, value: String) {
        strings[key] = value
    }

    override fun remove(key: String) {
        strings.remove(key)
        floats.remove(key)
        booleans.remove(key)
    }

    override fun getFloat(key: String, defaultValue: Float): Float =
        floats[key] ?: defaultValue

    override fun putFloat(key: String, value: Float) {
        floats[key] = value
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        booleans[key] ?: defaultValue

    override fun putBoolean(key: String, value: Boolean) {
        booleans[key] = value
    }
}
```

`android/app/src/main/java/com/liuli/hanzirandom/data/SettingsRepository.kt`:
```kotlin
package com.liuli.hanzirandom.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(private val store: KeyValueStore) {
    private val _fontSize = MutableStateFlow(store.getFloat(KEY_FONT_SIZE, 150f))
    val fontSize: StateFlow<Float> = _fontSize.asStateFlow()

    private val _swipeEnabled = MutableStateFlow(store.getBoolean(KEY_SWIPE_ENABLED, true))
    val swipeEnabled: StateFlow<Boolean> = _swipeEnabled.asStateFlow()

    fun setFontSize(value: Float) {
        val clamped = value.coerceIn(60f, 260f)
        _fontSize.value = clamped
        store.putFloat(KEY_FONT_SIZE, clamped)
    }

    fun setSwipeEnabled(enabled: Boolean) {
        _swipeEnabled.value = enabled
        store.putBoolean(KEY_SWIPE_ENABLED, enabled)
    }

    companion object {
        const val KEY_FONT_SIZE = "fontSize"
        const val KEY_SWIPE_ENABLED = "swipeEnabled"
    }
}
```

- [ ] **Step 4: 运行确认通过**

Run:
```bash
cd android
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew :app:testDebugUnitTest --tests 'com.liuli.hanzirandom.data.SettingsRepositoryTest'
```
Expected: `BUILD SUCCESSFUL`，3 个测试通过。

- [ ] **Step 5: 提交**

```bash
git add android/app/src
git commit -m "feat(android): add key-value store and settings repository"
```

---

### Task 4: CharacterRepository 字库/错题/轮次持久化（TDD）

**Files:**
- Test: `android/app/src/test/java/com/liuli/hanzirandom/data/CharacterRepositoryTest.kt`
- Create: `android/app/src/main/java/com/liuli/hanzirandom/data/CharacterRepository.kt`

- [ ] **Step 1: 写失败测试**

`android/app/src/test/java/com/liuli/hanzirandom/data/CharacterRepositoryTest.kt`:
```kotlin
package com.liuli.hanzirandom.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterRepositoryTest {

    private fun repositoryWith(library: List<String>): CharacterRepository {
        val store = InMemoryKeyValueStore()
        store.putString("allCharacters", library.joinToString("\n"))
        return CharacterRepository(store)
    }

    @Test
    fun firstRun_initializesWithPreset() {
        val repo = CharacterRepository(InMemoryKeyValueStore())
        assertEquals(CharacterRepository.PRESET, repo.library.value)
        assertEquals("一", repo.library.value.first())
        assertTrue(repo.library.value.contains("舞"))
    }

    @Test
    fun preset_hasNoDuplicates() {
        assertEquals(
            CharacterRepository.PRESET.size,
            CharacterRepository.PRESET.toSet().size
        )
    }

    @Test
    fun preset_allHanzi() {
        for (char in CharacterRepository.PRESET) {
            assertTrue(
                "non-hanzi in preset: $char",
                CharacterRepository.isHanzi(char.codePointAt(0))
            )
        }
    }

    @Test
    fun startRound_setsTotalAndDrawsFirst() {
        val repo = repositoryWith(listOf("甲", "乙", "丙"))
        repo.startRound()
        val state = repo.session.state.value
        assertEquals(3, state.total)
        assertEquals(1, state.drawnCount)
        assertNotNull(state.current)
        assertFalse(state.isFinished)
    }

    @Test
    fun drawNext_noRepeatsWithinRound() {
        val repo = repositoryWith(listOf("甲", "乙", "丙", "丁"))
        val seen = mutableListOf(repo.session.state.value.current!!)
        while (!repo.session.state.value.isFinished) {
            repo.session.drawNext()
            repo.session.state.value.current?.let(seen::add)
        }
        assertEquals(4, seen.size)
        assertEquals(4, seen.toSet().size)
    }

    @Test
    fun emptyLibrary_isFinishedWithNoCurrent() {
        val repo = repositoryWith(emptyList())
        val state = repo.session.state.value
        assertNull(state.current)
        assertTrue(state.isFinished)
        assertEquals(0, state.total)
        assertEquals(0, state.drawnCount)
    }

    @Test
    fun addCharacters_appendsNewUniqueInOrder() {
        val repo = repositoryWith(listOf("欢", "给"))
        repo.addCharacters("山水田")
        assertEquals(listOf("欢", "给", "山", "水", "田"), repo.library.value)
    }

    @Test
    fun addCharacters_skipsExisting() {
        val repo = repositoryWith(listOf("欢", "给"))
        repo.addCharacters("给山欢水")
        assertEquals(listOf("欢", "给", "山", "水"), repo.library.value)
    }

    @Test
    fun addCharacters_dedupsWithinInput() {
        val repo = repositoryWith(emptyList())
        repo.addCharacters("山山水山田水")
        assertEquals(listOf("山", "水", "田"), repo.library.value)
    }

    @Test
    fun addCharacters_filtersWhitespaceAndNonHanzi() {
        val repo = repositoryWith(emptyList())
        repo.addCharacters(" 山a水\n1，田! 水 ")
        assertEquals(listOf("山", "水", "田"), repo.library.value)
    }

    @Test
    fun addCharacters_acceptsExtensionB() {
        val repo = repositoryWith(emptyList())
        repo.addCharacters("𠀀")
        assertEquals(listOf("𠀀"), repo.library.value)
    }

    @Test
    fun addCharacters_allDuplicatesDoesNotMutate() {
        val repo = repositoryWith(listOf("山", "水"))
        repo.addCharacters("山水山水")
        assertEquals(listOf("山", "水"), repo.library.value)
    }

    @Test
    fun addCharacters_persists() {
        val store = InMemoryKeyValueStore()
        val first = repositoryWith(listOf("欢", "给"))
        // 复用同一份存储验证持久化
        store.putString("allCharacters", first.library.value.joinToString("\n"))
        val second = CharacterRepository(store)
        second.addCharacters("山水")
        val third = CharacterRepository(store)
        assertTrue(third.library.value.contains("山"))
        assertTrue(third.library.value.contains("水"))
    }

    @Test
    fun addCharacters_joinsCurrentRoundImmediately() {
        val repo = repositoryWith(listOf("山", "水"))
        assertEquals(2, repo.session.state.value.total)

        repo.addCharacters("田")
        assertEquals(3, repo.session.state.value.total)
        assertTrue(repo.session.state.value.pool.contains("田"))

        while (!repo.session.state.value.isFinished) {
            repo.session.goNext()
        }
        assertTrue(repo.session.state.value.history.contains("田"))
    }

    @Test
    fun addCharacters_afterFinished_doesNotCorruptFinishedRound() {
        val repo = repositoryWith(listOf("山"))
        assertTrue(repo.session.state.value.isFinished)

        repo.addCharacters("水")
        assertEquals(listOf("山", "水"), repo.library.value)
        assertTrue(repo.session.state.value.isFinished)

        repo.reset()
        assertEquals(2, repo.session.state.value.total)
        assertFalse(repo.session.state.value.isFinished)
    }

    @Test
    fun remove_deletesAndPersists() {
        val store = InMemoryKeyValueStore()
        val repo = CharacterRepository(store)
        repo.addCharacters("山")
        repo.remove("山")
        assertFalse(repo.library.value.contains("山"))

        val reloaded = CharacterRepository(store)
        assertFalse(reloaded.library.value.contains("山"))
    }

    @Test
    fun remove_nonexistent_isNoOp() {
        val repo = repositoryWith(listOf("山", "水"))
        repo.remove("田")
        assertEquals(listOf("山", "水"), repo.library.value)
    }

    @Test
    fun remove_alsoDropsShownCharacterFromRoundAndMistakes() {
        val repo = repositoryWith(listOf("甲", "乙", "丙"))
        val shown = repo.session.state.value.current!!
        repo.addMistake(shown)

        repo.remove(shown)

        assertNull(repo.mistakes.value[shown])
        assertEquals(2, repo.session.state.value.total)
        assertNotEquals(shown, repo.session.state.value.current)
        assertFalse(repo.session.state.value.history.contains(shown))
        assertFalse(repo.session.state.value.pool.contains(shown))
    }

    @Test
    fun exportString_spaceJoinedInOrder() {
        val repo = repositoryWith(listOf("欢", "给", "哈"))
        assertEquals("欢 给 哈", repo.exportString())
    }

    @Test
    fun exportString_empty_returnsEmptyString() {
        val repo = repositoryWith(emptyList())
        assertEquals("", repo.exportString())
    }

    @Test
    fun addMistake_accumulatesCount() {
        val repo = repositoryWith(listOf("山", "水"))
        repo.addMistake("山")
        repo.addMistake("山")
        repo.addMistake("水")
        assertEquals(2, repo.mistakes.value["山"])
        assertEquals(1, repo.mistakes.value["水"])
    }

    @Test
    fun removeMistake_deletesOnlyThatCharacter() {
        val repo = repositoryWith(listOf("山", "水"))
        repo.addMistake("山")
        repo.addMistake("水")
        repo.removeMistake("山")
        assertNull(repo.mistakes.value["山"])
        assertEquals(1, repo.mistakes.value["水"])
    }

    @Test
    fun clearMistakes_emptiesBook() {
        val repo = repositoryWith(listOf("山"))
        repo.addMistake("山")
        repo.clearMistakes()
        assertTrue(repo.mistakes.value.isEmpty())
    }

    @Test
    fun mistakesSorted_ordersByCountDescending() {
        val repo = repositoryWith(listOf("水", "山", "田"))
        repo.addMistake("水")
        repo.addMistake("山")
        repo.addMistake("山")
        repo.addMistake("田")
        repo.addMistake("田")
        repo.addMistake("田")
        assertEquals(listOf("田", "山", "水"), repo.mistakesSorted().map { it.char })
        assertEquals(listOf(3, 2, 1), repo.mistakesSorted().map { it.count })
    }

    @Test
    fun mistakesSorted_tiesFollowLibraryOrder() {
        val repo = repositoryWith(listOf("山", "水", "田"))
        repo.addMistake("田")
        repo.addMistake("山")
        repo.addMistake("水")
        assertEquals(listOf("山", "水", "田"), repo.mistakesSorted().map { it.char })
    }

    @Test
    fun mistakes_persistAcrossInstances() {
        val store = InMemoryKeyValueStore()
        val first = CharacterRepository(store)
        first.addMistake("山")
        first.addMistake("山")
        first.addMistake("水")

        val second = CharacterRepository(store)
        assertEquals(2, second.mistakes.value["山"])
        assertEquals(1, second.mistakes.value["水"])
    }

    @Test
    fun roundPersistsAcrossInstances() {
        val store = InMemoryKeyValueStore()
        val first = CharacterRepository(store)
        first.session.goNext()
        first.session.goPrevious()
        first.persistRound()

        val second = CharacterRepository(store)
        assertEquals(first.session.state.value.pool, second.session.state.value.pool)
        assertEquals(first.session.state.value.history, second.session.state.value.history)
        assertEquals(first.session.state.value.cursor, second.session.state.value.cursor)
        assertEquals(first.session.state.value.current, second.session.state.value.current)
        assertEquals(first.session.state.value.total, second.session.state.value.total)
        assertEquals(first.session.state.value.isFinished, second.session.state.value.isFinished)
    }

    @Test
    fun finishedRoundRestoresAsFinished() {
        val store = InMemoryKeyValueStore()
        store.putString("allCharacters", "甲")
        val first = CharacterRepository(store)
        assertTrue(first.session.state.value.isFinished)
        first.persistRound()

        val second = CharacterRepository(store)
        assertTrue(second.session.state.value.isFinished)
        assertEquals("甲", second.session.state.value.current)
        assertEquals(1, second.session.state.value.total)
    }

    @Test
    fun invalidRoundSnapshot_isDiscarded() {
        val store = InMemoryKeyValueStore()
        store.putString("allCharacters", "甲乙")
        store.putString("roundSnapshot", "garbage")
        val repo = CharacterRepository(store)
        assertEquals(2, repo.session.state.value.total)
        assertNotNull(repo.session.state.value.current)
    }

    @Test
    fun roundPersists_extendedBCharacter() {
        val store = InMemoryKeyValueStore()
        store.putString("allCharacters", "𠀀")
        val first = CharacterRepository(store)
        first.persistRound()

        val second = CharacterRepository(store)
        assertEquals("𠀀", second.session.state.value.current)
        assertEquals(1, second.session.state.value.total)
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run:
```bash
cd android
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew :app:testDebugUnitTest --tests 'com.liuli.hanzirandom.data.CharacterRepositoryTest'
```
Expected: 编译失败，`Unresolved reference: CharacterRepository`。

- [ ] **Step 3: 实现 CharacterRepository**

`android/app/src/main/java/com/liuli/hanzirandom/data/CharacterRepository.kt`:
```kotlin
package com.liuli.hanzirandom.data

import com.liuli.hanzirandom.domain.DrawSession
import com.liuli.hanzirandom.domain.RoundSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MistakeEntry(val char: String, val count: Int)

class CharacterRepository(private val store: KeyValueStore) {
    private val _library = MutableStateFlow<List<String>>(emptyList())
    val library: StateFlow<List<String>> = _library.asStateFlow()

    private val _mistakes = MutableStateFlow<Map<String, Int>>(emptyMap())
    val mistakes: StateFlow<Map<String, Int>> = _mistakes.asStateFlow()

    /** Learning round. Review rounds own separate DrawSession instances. */
    val session = DrawSession()

    init {
        val savedLibrary = store.getString(KEY_LIBRARY)
        if (savedLibrary != null) {
            _library.value = decodeList(savedLibrary)
        } else {
            _library.value = PRESET
            store.putString(KEY_LIBRARY, encodeList(PRESET))
        }
        _mistakes.value = decodeMistakes(store.getString(KEY_MISTAKES))

        val snapshot = restoreSnapshot()
        if (snapshot != null) {
            session.restore(snapshot)
        } else {
            session.restart(_library.value)
        }
    }

    // MARK: - Round

    fun startRound() {
        session.restart(_library.value)
        persistRound()
    }

    fun reset() = startRound()

    fun persistRound() {
        val state = session.state.value
        if (state.total <= 0) {
            store.remove(KEY_ROUND)
            return
        }
        store.putString(
            KEY_ROUND,
            RoundCodec.encode(
                RoundSnapshot(
                    pool = state.pool,
                    history = state.history,
                    cursor = state.cursor,
                    total = state.total,
                    isFinished = state.isFinished
                )
            )
        )
    }

    private fun restoreSnapshot(): RoundSnapshot? {
        val raw = store.getString(KEY_ROUND) ?: return null
        val snapshot = RoundCodec.decode(raw) ?: return null
        if (snapshot.total != snapshot.pool.size + snapshot.history.size) return null
        val cursorValid =
            (snapshot.history.isEmpty() && snapshot.cursor == -1) ||
                (snapshot.history.isNotEmpty() && snapshot.cursor in snapshot.history.indices)
        return if (cursorValid) snapshot else null
    }

    // MARK: - Mistake book

    fun addMistake(character: String) {
        _mistakes.value = _mistakes.value.toMutableMap().apply {
            this[character] = (this[character] ?: 0) + 1
        }
        persistMistakes()
    }

    fun removeMistake(character: String) {
        if (!_mistakes.value.containsKey(character)) return
        _mistakes.value = _mistakes.value - character
        persistMistakes()
    }

    fun clearMistakes() {
        if (_mistakes.value.isEmpty()) return
        _mistakes.value = emptyMap()
        persistMistakes()
    }

    fun mistakesSorted(): List<MistakeEntry> {
        val order = _library.value.withIndex().associate { (index, char) -> char to index }
        return _mistakes.value.entries
            .sortedWith(
                compareByDescending<Map.Entry<String, Int>> { it.value }
                    .thenBy { order[it.key] ?: Int.MAX_VALUE }
            )
            .map { MistakeEntry(it.key, it.value) }
    }

    private fun persistMistakes() {
        val raw = _mistakes.value.entries.joinToString("\n") { "${it.key}\t${it.value}" }
        store.putString(KEY_MISTAKES, raw)
    }

    // MARK: - Library

    fun addCharacters(from text: String): Int {
        val existing = _library.value.toHashSet()
        val seen = HashSet<String>()
        val toAppend = mutableListOf<String>()

        var i = 0
        while (i < text.length) {
            val codePoint = text.codePointAt(i)
            i += Character.charCount(codePoint)
            if (!isHanzi(codePoint)) continue
            val char = String(Character.toChars(codePoint))
            if (char in existing || !seen.add(char)) continue
            toAppend.add(char)
        }

        if (toAppend.isEmpty()) return 0
        _library.value = _library.value + toAppend
        store.putString(KEY_LIBRARY, encodeList(_library.value))
        session.appendToPool(toAppend)
        persistRound()
        return toAppend.size
    }

    fun remove(character: String) {
        if (character !in _library.value) return
        _library.value = _library.value - character
        store.putString(KEY_LIBRARY, encodeList(_library.value))
        session.removeCharacter(character)
        if (_mistakes.value.containsKey(character)) {
            _mistakes.value = _mistakes.value - character
            persistMistakes()
        }
        persistRound()
    }

    fun exportString(): String = _library.value.joinToString(" ")

    private fun encodeList(values: List<String>): String = values.joinToString("\n")

    private fun decodeList(raw: String): List<String> =
        if (raw.isEmpty()) emptyList() else raw.split("\n").filter { it.isNotEmpty() }

    private fun decodeMistakes(raw: String?): Map<String, Int> {
        if (raw.isNullOrEmpty()) return emptyMap()
        return raw.lineSequence().mapNotNull { line ->
            val parts = line.split('\t')
            val char = parts.getOrNull(0)?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val count = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
            char to count
        }.toMap()
    }

    companion object {
        private const val KEY_LIBRARY = "allCharacters"
        private const val KEY_MISTAKES = "mistakes"
        private const val KEY_ROUND = "roundSnapshot"

        private const val PRESET_SOURCE =
            "一二三人个大山上下小雨天车开火门口月羊只有牛水中风头田土里在日白马手出" +
                "王子儿女刀木鸟伞鱼瓜气飞虫米云电石了来玉去回多少问早巴父工力不干几男坐打" +
                "四五六闪的花朵果比午面包高文分平我连元丁厂目见兔禾是把笑着看会自己画好爱心" +
                "也可家爸妈这爷奶们书串球圆形长方正角灯块冰那弓买哪乐谢想抱竹要立耳红色吃香" +
                "穿身它发衣他她毛朋友哥弟兄眉鼻光星阳亮今明走兴空两哭牙肚尾你脸黑半呀吗" +
                "七八九十又很太宝贝和关园生公苗叶林草先岛嘴眼睛脚腿翅膀羽体没认识颜黄蓝绿" +
                "粉青紫彩食物尝闻味鲜苦甜什么起运动踢足赛快慢跑步唱歌跳舞吹号讲故事怎帮找欢" +
                "担闷急害怕惊哈老师同学校桌椅教室课做给操伸拉举抓拍拥扔寒冷温暖热凉湿雾净" +
                "到都办渴喝饿饭睡觉累醒房间帘窗户台视床被处让对姐妹阿姨叔伯姑亲餐盆碗筷盖袋" +
                "巾杯成用像原菜萝卜豆苹桃杏笋扫地拖洒洗服晒收全呢还谁"

        val PRESET: List<String> = PRESET_SOURCE.map { it.toString() }.distinct()

        fun isHanzi(codePoint: Int): Boolean =
            codePoint in 0x4E00..0x9FFF ||
                codePoint in 0x3400..0x4DBF ||
                codePoint in 0xF900..0xFAFF ||
                codePoint in 0x20000..0x2A6DF
    }
}

private object RoundCodec {
    fun encode(snapshot: RoundSnapshot): String = buildString {
        append(snapshot.cursor).append('|')
        append(snapshot.total).append('|')
        append(snapshot.isFinished).append('|')
        append(snapshot.pool.size).append('|')
        append(snapshot.history.size).append('\n')
        for (char in snapshot.pool) append(char).append('\n')
        for (char in snapshot.history) append(char).append('\n')
    }

    fun decode(raw: String): RoundSnapshot? {
        val lines = raw.split('\n')
        val meta = lines.firstOrNull()?.split('|') ?: return null
        if (meta.size != 5) return null
        val cursor = meta[0].toIntOrNull() ?: return null
        val total = meta[1].toIntOrNull() ?: return null
        val finished = meta[2].toBooleanStrictOrNull() ?: return null
        val poolSize = meta[3].toIntOrNull() ?: return null
        val historySize = meta[4].toIntOrNull() ?: return null
        if (poolSize < 0 || historySize < 0) return null

        val items = lines.drop(1)
        if (items.size < poolSize + historySize) return null
        val pool = items.subList(0, poolSize)
        val history = items.subList(poolSize, poolSize + historySize)
        if (pool.any { it.isEmpty() } || history.any { it.isEmpty() }) return null

        return RoundSnapshot(
            pool = pool,
            history = history,
            cursor = cursor,
            total = total,
            isFinished = finished
        )
    }
}
```

- [ ] **Step 4: 运行确认通过**

Run:
```bash
cd android
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew :app:testDebugUnitTest
```
Expected: `BUILD SUCCESSFUL`，全部单测通过。

- [ ] **Step 5: 提交**

```bash
git add android/app/src
git commit -m "feat(android): add character repository with local persistence"
```

---

### Task 5: 主题、字体、田字格与胶囊按钮

**Files:**
- Create: `android/app/src/main/res/font/lxgw_wenkai_gb_lite_regular.ttf`（复制自 iOS 资源）
- Create: `android/app/src/main/res/values/colors.xml`
- Create: `android/app/src/main/java/com/liuli/hanzirandom/ui/theme/Theme.kt`
- Create: `android/app/src/main/java/com/liuli/hanzirandom/ui/components/TianZiGe.kt`
- Create: `android/app/src/main/java/com/liuli/hanzirandom/ui/components/PillButtons.kt`
- Create: `android/app/src/main/java/com/liuli/hanzirandom/ui/Clipboard.kt`

- [ ] **Step 1: 复制正楷字体**

Run:
```bash
mkdir -p android/app/src/main/res/font
cp Sources/Resources/Fonts/LXGWWenKaiGBLite-Regular.ttf \
   android/app/src/main/res/font/lxgw_wenkai_gb_lite_regular.ttf
ls -lh android/app/src/main/res/font/
```
Expected: 约 14MB 的 ttf 文件。

- [ ] **Step 2: 写颜色与主题**

`android/app/src/main/res/values/colors.xml`:
```xml
<resources>
    <color name="splash_background">#FF5B49</color>
</resources>
```

`android/app/src/main/java/com/liuli/hanzirandom/ui/theme/Theme.kt`:
```kotlin
package com.liuli.hanzirandom.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.liuli.hanzirandom.R

object HanziColors {
    val Accent = Color(0xFFFF9E40)
    val AccentDeep = Color(0xFFF27326)
    val CardBackground = Color(0xFFFFFAF0)
    val SoftYellow = Color(0xFFFFEDC7)
    val Danger = Color(0xFFD13D2E)
    val DangerSoft = Color(0xFFFFEDE6)
    val TextPrimary = Color(0xFF3B2A1A)
}

val HanziCardCornerRadius = 28.dp

val CardGradient = Brush.linearGradient(listOf(Color.White, HanziColors.SoftYellow))
val ButtonGradient = Brush.horizontalGradient(
    listOf(HanziColors.Accent, HanziColors.AccentDeep)
)

val HanziFontFamily = FontFamily(Font(R.font.lxgw_wenkai_gb_lite_regular))

@Composable
fun HanziRandomTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = HanziColors.AccentDeep,
            onPrimary = Color.White,
            secondary = HanziColors.Accent,
            background = Color(0xFFFFFBF5),
            onBackground = HanziColors.TextPrimary,
            surface = HanziColors.CardBackground,
            onSurface = HanziColors.TextPrimary,
            error = HanziColors.Danger
        ),
        typography = Typography(),
        content = content
    )
}

fun Modifier.cardBackground(cornerRadius: Dp = HanziCardCornerRadius): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    return this
        .shadow(
            elevation = 10.dp,
            shape = shape,
            spotColor = HanziColors.Accent.copy(alpha = 0.35f),
            ambientColor = HanziColors.Accent.copy(alpha = 0.2f)
        )
        .clip(shape)
        .background(CardGradient)
        .border(2.dp, HanziColors.Accent.copy(alpha = 0.35f), shape)
}
```
注意：`cardBackground` 不是 `@Composable`，`CardGradient` 是顶层常量，可以直接用。

- [ ] **Step 3: 田字格与按钮**

`android/app/src/main/java/com/liuli/hanzirandom/ui/components/TianZiGe.kt`:
```kotlin
package com.liuli.hanzirandom.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.liuli.hanzirandom.ui.theme.HanziColors

@Composable
fun TianZiGe(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val inset = 24.dp.toPx()
        val side = minOf(size.width, size.height) - inset * 2
        if (side <= 0f) return@Canvas

        val left = (size.width - side) / 2f
        val top = (size.height - side) / 2f
        val color = HanziColors.Accent.copy(alpha = 0.18f)
        val stroke = 1.5.dp.toPx()
        val dash = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 7.dp.toPx()))

        drawRect(
            color = color,
            topLeft = Offset(left, top),
            size = Size(side, side),
            style = Stroke(width = stroke)
        )
        drawLine(
            color = color,
            start = Offset(left + side / 2, top),
            end = Offset(left + side / 2, top + side),
            strokeWidth = stroke,
            pathEffect = dash
        )
        drawLine(
            color = color,
            start = Offset(left, top + side / 2),
            end = Offset(left + side, top + side / 2),
            strokeWidth = stroke,
            pathEffect = dash
        )
    }
}
```

`android/app/src/main/java/com/liuli/hanzirandom/ui/components/PillButtons.kt`:
```kotlin
package com.liuli.hanzirandom.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liuli.hanzirandom.ui.theme.ButtonGradient
import com.liuli.hanzirandom.ui.theme.HanziColors

enum class PillStyle { Primary, Secondary, Danger }

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PillStyle = PillStyle.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val background: Brush = when (style) {
        PillStyle.Primary -> ButtonGradient
        PillStyle.Secondary -> SolidColor(HanziColors.SoftYellow)
        PillStyle.Danger -> SolidColor(HanziColors.DangerSoft)
    }
    val contentColor = when (style) {
        PillStyle.Primary -> Color.White
        PillStyle.Secondary -> HanziColors.AccentDeep
        PillStyle.Danger -> HanziColors.Danger
    }
    val borderColor = when (style) {
        PillStyle.Primary -> Color.Transparent
        PillStyle.Secondary -> HanziColors.Accent.copy(alpha = 0.5f)
        PillStyle.Danger -> HanziColors.Danger.copy(alpha = 0.4f)
    }
    val horizontalPadding = if (style == PillStyle.Primary) 28.dp else 24.dp
    val verticalPadding = if (style == PillStyle.Primary) 14.dp else 12.dp
    val textSize = if (style == PillStyle.Primary) 22.sp else 18.sp

    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .background(background)
            .border(1.5.dp, borderColor, CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = text,
                color = contentColor,
                fontSize = textSize,
                fontWeight = if (style == PillStyle.Primary) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
```

`android/app/src/main/java/com/liuli/hanzirandom/ui/Clipboard.kt`:
```kotlin
package com.liuli.hanzirandom.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent

fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("hanzi", text))
}

fun shareText(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, "分享字库"))
}
```

- [ ] **Step 4: 构建确认**

Run:
```bash
cd android
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew :app:assembleDebug
```
Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 5: 提交**

```bash
git add android/app/src
git commit -m "feat(android): add theme, bundled kaiti font, and shared components"
```

---

### Task 6: 学习页 + Application + 朗读

**Files:**
- Create: `android/app/src/main/java/com/liuli/hanzirandom/HanziApplication.kt`
- Create: `android/app/src/main/java/com/liuli/hanzirandom/domain/SpeechService.kt`
- Create: `android/app/src/main/java/com/liuli/hanzirandom/ui/StudyViewModel.kt`
- Create: `android/app/src/main/java/com/liuli/hanzirandom/ui/StudyScreen.kt`
- Modify: `android/app/src/main/AndroidManifest.xml`（`android:name=".HanziApplication"`）
- Modify: `android/app/src/main/java/com/liuli/hanzirandom/MainActivity.kt`

- [ ] **Step 1: 写 Application 与朗读服务**

`android/app/src/main/java/com/liuli/hanzirandom/HanziApplication.kt`:
```kotlin
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
                }
            }
        )
    }
}
```

`android/app/src/main/java/com/liuli/hanzirandom/domain/SpeechService.kt`:
```kotlin
package com.liuli.hanzirandom.domain

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Shared TextToSpeech wrapper. Chinese voice missing/init failure degrades
 * silently to no audio, matching the iOS spec.
 */
class SpeechService(context: Context) {
    private var tts: TextToSpeech? = null
    private var ready = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.SIMPLIFIED_CHINESE)
                ready = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
    }

    fun speak(text: String) {
        if (!ready || text.isEmpty()) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "hanzi-${System.nanoTime()}")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
```

- [ ] **Step 2: 写 StudyViewModel**

`android/app/src/main/java/com/liuli/hanzirandom/ui/StudyViewModel.kt`:
```kotlin
package com.liuli.hanzirandom.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.liuli.hanzirandom.HanziApplication
import com.liuli.hanzirandom.data.CharacterRepository
import com.liuli.hanzirandom.data.SettingsRepository
import com.liuli.hanzirandom.domain.SpeechService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StudyViewModel(
    private val repository: CharacterRepository,
    settings: SettingsRepository,
    private val speech: SpeechService
) : ViewModel() {
    val state = repository.session.state
    val fontSize = settings.fontSize
    val swipeEnabled = settings.swipeEnabled

    private val _justAddedMistake = MutableStateFlow(false)
    val justAddedMistake: StateFlow<Boolean> = _justAddedMistake.asStateFlow()
    private var flashJob: Job? = null

    fun next() = repository.session.goNext()

    fun previous() = repository.session.goPrevious()

    fun reset() = repository.reset()

    fun speak(character: String) = speech.speak(character)

    fun addMistake(character: String) {
        repository.addMistake(character)
        flashJob?.cancel()
        _justAddedMistake.value = true
        flashJob = viewModelScope.launch {
            delay(1000)
            _justAddedMistake.value = false
        }
    }

    companion object {
        fun factory(app: HanziApplication): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    StudyViewModel(
                        app.characterRepository,
                        app.settingsRepository,
                        app.speechService
                    ) as T
            }
    }
}
```

- [ ] **Step 3: 写 StudyScreen**

`android/app/src/main/java/com/liuli/hanzirandom/ui/StudyScreen.kt`:
```kotlin
package com.liuli.hanzirandom.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liuli.hanzirandom.domain.SessionState
import com.liuli.hanzirandom.ui.components.PillButton
import com.liuli.hanzirandom.ui.components.PillStyle
import com.liuli.hanzirandom.ui.components.TianZiGe
import com.liuli.hanzirandom.ui.theme.HanziColors
import com.liuli.hanzirandom.ui.theme.HanziFontFamily
import com.liuli.hanzirandom.ui.theme.cardBackground

enum class StudyMode { Learning, Review }

@Composable
fun StudyScreen(
    state: SessionState,
    fontSize: Float,
    swipeEnabled: Boolean,
    justAddedMistake: Boolean,
    mode: StudyMode,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onReset: () -> Unit,
    onSpeak: (String) -> Unit,
    onAddMistake: (String) -> Unit,
    onRemoveMistake: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val haptic = { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HanziColors.SoftYellow.copy(alpha = 0.25f)),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (mode == StudyMode.Learning) "随机汉字" else "错题复习",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = HanziColors.AccentDeep
                )
                Spacer(Modifier.weight(1f))
                if (mode == StudyMode.Learning) {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "设置",
                            tint = HanziColors.AccentDeep
                        )
                    }
                } else {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "关闭",
                            tint = HanziColors.AccentDeep
                        )
                    }
                }
            }

            if (state.total > 0) {
                Text(
                    text = "第 ${state.position} / 共 ${state.total}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = HanziColors.AccentDeep.copy(alpha = 0.8f),
                    modifier = Modifier.testTag("progressLabel")
                )
                LinearProgressIndicator(
                    progress = {
                        state.drawnCount.toFloat() / maxOf(state.total, 1).toFloat()
                    },
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .width(240.dp),
                    color = HanziColors.AccentDeep,
                    trackColor = HanziColors.SoftYellow
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                CharacterArea(
                    state = state,
                    fontSize = fontSize,
                    swipeEnabled = swipeEnabled,
                    mode = mode,
                    onNext = { haptic(); onNext() },
                    onPrevious = { haptic(); onPrevious() },
                    onOpenSettings = onOpenSettings,
                    onCopy = { current -> copyToClipboard(context, current) }
                )
            }

            if (state.current != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val current = state.current
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        PillButton(
                            text = "读一读",
                            onClick = { onSpeak(current) },
                            style = PillStyle.Secondary,
                            icon = Icons.Filled.VolumeUp
                        )
                        PillButton(
                            text = if (justAddedMistake) "已加入 ✓" else "加入错题本",
                            onClick = { haptic(); onAddMistake(current) },
                            style = PillStyle.Secondary,
                            icon = if (justAddedMistake) {
                                Icons.Filled.CheckCircle
                            } else {
                                Icons.Filled.Star
                            }
                        )
                    }
                    if (mode == StudyMode.Review) {
                        PillButton(
                            text = "删除错题",
                            onClick = { haptic(); onRemoveMistake(current) },
                            style = PillStyle.Danger,
                            icon = Icons.Filled.Delete
                        )
                    }
                }
            }

            if (state.total > 0) {
                Spacer(Modifier.height(12.dp))
                BottomControls(
                    state = state,
                    mode = mode,
                    onPrevious = { haptic(); onPrevious() },
                    onNext = { haptic(); onNext() },
                    onReset = { haptic(); onReset() }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CharacterArea(
    state: SessionState,
    fontSize: Float,
    swipeEnabled: Boolean,
    mode: StudyMode,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onOpenSettings: () -> Unit,
    onCopy: (String) -> Unit
) {
    val current = state.current
    if (current == null && state.isFinished) {
        EmptyOrFinishedCard(mode, onOpenSettings)
        return
    }
    if (current == null) return

    var showMenu by remember { mutableStateOf(false) }
    var drag by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .cardBackground()
            .pointerInput(swipeEnabled, current) {
                if (!swipeEnabled) return@pointerInput
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        drag += amount
                    },
                    onDragEnd = {
                        val threshold = 40.dp.toPx()
                        if (drag < -threshold) onNext() else if (drag > threshold) onPrevious()
                        drag = 0f
                    },
                    onDragCancel = { drag = 0f }
                )
            }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onNext,
                onLongClick = { showMenu = true }
            )
    ) {
        TianZiGe(Modifier.fillMaxSize())

        val cardSide = minOf(maxWidth.value, maxHeight.value)
        val effectiveSize = minOf(fontSize, cardSide * 0.72f)
        Text(
            text = current,
            modifier = Modifier
                .align(Alignment.Center)
                .testTag("characterLabel"),
            fontFamily = HanziFontFamily,
            fontSize = effectiveSize.sp,
            color = HanziColors.TextPrimary
        )
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text("复制") },
                onClick = {
                    showMenu = false
                    onCopy(current)
                }
            )
        }
    }
}

@Composable
private fun EmptyOrFinishedCard(mode: StudyMode, onOpenSettings: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .cardBackground()
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        if (mode == StudyMode.Review) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("🎉", fontSize = 48.sp)
                Text(
                    text = "全部复习完啦",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = HanziColors.AccentDeep
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "请先在设置里添加汉字",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Gray
                )
                PillButton(text = "去添加汉字", onClick = onOpenSettings)
            }
        }
    }
}

@Composable
private fun BottomControls(
    state: SessionState,
    mode: StudyMode,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onReset: () -> Unit
) {
    val showReset = state.isFinished && mode == StudyMode.Learning && !state.canGoNext
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (showReset) {
            Text(
                text = "全部抽完",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = HanziColors.AccentDeep
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PillButton(
                text = "上一个",
                onClick = onPrevious,
                style = PillStyle.Secondary,
                enabled = state.canGoPrevious
            )
            if (showReset) {
                PillButton(text = "重置", onClick = onReset, style = PillStyle.Primary)
            } else {
                PillButton(
                    text = "下一个",
                    onClick = onNext,
                    style = PillStyle.Primary,
                    enabled = state.canGoNext
                )
            }
        }
    }
}
```

- [ ] **Step 4: 接入 Application 与 MainActivity**

Manifest 的 `<application>` 增加属性：
```xml
android:name=".HanziApplication"
```

`MainActivity.kt` 替换为：
```kotlin
package com.liuli.hanzirandom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liuli.hanzirandom.ui.StudyMode
import com.liuli.hanzirandom.ui.StudyScreen
import com.liuli.hanzirandom.ui.StudyViewModel
import com.liuli.hanzirandom.ui.theme.HanziRandomTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as HanziApplication
        setContent {
            HanziRandomTheme {
                val vm: StudyViewModel = viewModel(factory = StudyViewModel.factory(app))
                val state by vm.state.collectAsStateWithLifecycle()
                val fontSize by vm.fontSize.collectAsStateWithLifecycle()
                val swipeEnabled by vm.swipeEnabled.collectAsStateWithLifecycle()
                val justAdded by vm.justAddedMistake.collectAsStateWithLifecycle()

                Scaffold { padding ->
                    Box(
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize()
                    ) {
                        StudyScreen(
                            state = state,
                            fontSize = fontSize,
                            swipeEnabled = swipeEnabled,
                            justAddedMistake = justAdded,
                            mode = StudyMode.Learning,
                            onNext = vm::next,
                            onPrevious = vm::previous,
                            onReset = vm::reset,
                            onSpeak = vm::speak,
                            onAddMistake = vm::addMistake,
                            onRemoveMistake = {},
                            onOpenSettings = {},
                            onClose = {}
                        )
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 5: 构建、安装、截图验证学习页**

Run:
```bash
cd android
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew :app:installDebug
adb shell am start -n com.liuli.hanzirandom/.MainActivity
sleep 2
mkdir -p ../docs/screenshots/android
adb exec-out screencap -p > ../docs/screenshots/android/study.png
```
Expected: 模拟器打开 App，截图能看到标题「随机汉字」、田字格、正楷大字、进度、朗读/错题按钮、上一个/下一个。

如果模拟器未启动：
```bash
~/Library/Android/sdk/emulator/emulator -avd Pixel_Fold_API_35 -no-window -no-audio -no-boot-anim &
adb wait-for-device
```

- [ ] **Step 6: 提交**

```bash
git add android/app/src docs/screenshots/android/study.png
git commit -m "feat(android): add study screen, application wiring, and TTS"
```

---

### Task 7: 错题本 + 复习 + 详情 + App 导航壳

**Files:**
- Create: `android/app/src/main/java/com/liuli/hanzirandom/ui/MistakeBookViewModel.kt`
- Create: `android/app/src/main/java/com/liuli/hanzirandom/ui/ReviewViewModel.kt`
- Create: `android/app/src/main/java/com/liuli/hanzirandom/ui/MistakeBookScreen.kt`
- Create: `android/app/src/main/java/com/liuli/hanzirandom/ui/CharacterDetailScreen.kt`
- Create: `android/app/src/main/java/com/liuli/hanzirandom/ui/App.kt`
- Modify: `android/app/src/main/java/com/liuli/hanzirandom/MainActivity.kt`

- [ ] **Step 1: 写两个 ViewModel**

`android/app/src/main/java/com/liuli/hanzirandom/ui/MistakeBookViewModel.kt`:
```kotlin
package com.liuli.hanzirandom.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.liuli.hanzirandom.HanziApplication
import com.liuli.hanzirandom.data.CharacterRepository
import com.liuli.hanzirandom.data.MistakeEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class MistakeBookViewModel(
    private val repository: CharacterRepository
) : ViewModel() {
    val mistakes: StateFlow<List<MistakeEntry>> =
        combine(repository.mistakes, repository.library) { _, _ ->
            repository.mistakesSorted()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = repository.mistakesSorted()
        )

    fun remove(character: String) = repository.removeMistake(character)

    fun clear() = repository.clearMistakes()

    companion object {
        fun factory(app: HanziApplication): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    MistakeBookViewModel(app.characterRepository) as T
            }
    }
}
```

`android/app/src/main/java/com/liuli/hanzirandom/ui/ReviewViewModel.kt`:
```kotlin
package com.liuli.hanzirandom.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.liuli.hanzirandom.HanziApplication
import com.liuli.hanzirandom.data.CharacterRepository
import com.liuli.hanzirandom.domain.DrawSession
import com.liuli.hanzirandom.domain.SpeechService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReviewViewModel(
    private val repository: CharacterRepository,
    private val speech: SpeechService,
    characters: List<String>
) : ViewModel() {
    private val session = DrawSession(characters)
    val state = session.state

    private val _justAddedMistake = MutableStateFlow(false)
    val justAddedMistake: StateFlow<Boolean> = _justAddedMistake.asStateFlow()
    private var flashJob: Job? = null

    fun next() = session.goNext()

    fun previous() = session.goPrevious()

    fun speak(character: String) = speech.speak(character)

    fun addMistake(character: String) {
        repository.addMistake(character)
        flashJob?.cancel()
        _justAddedMistake.value = true
        flashJob = viewModelScope.launch {
            delay(1000)
            _justAddedMistake.value = false
        }
    }

    fun removeMistake(character: String) {
        repository.removeMistake(character)
        session.removeCurrent()
    }

    companion object {
        fun factory(
            app: HanziApplication,
            characters: List<String>
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ReviewViewModel(
                    app.characterRepository,
                    app.speechService,
                    characters
                ) as T
        }
    }
}
```

- [ ] **Step 2: 写错题本界面**

`android/app/src/main/java/com/liuli/hanzirandom/ui/MistakeBookScreen.kt`:
```kotlin
package com.liuli.hanzirandom.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liuli.hanzirandom.data.MistakeEntry
import com.liuli.hanzirandom.ui.theme.HanziColors
import com.liuli.hanzirandom.ui.theme.HanziFontFamily
import com.liuli.hanzirandom.ui.theme.cardBackground

@Composable
fun MistakeBookScreen(
    entries: List<MistakeEntry>,
    onReview: (List<String>) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    onDetail: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HanziColors.SoftYellow.copy(alpha = 0.25f))
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "错题本",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = HanziColors.AccentDeep,
                modifier = Modifier.testTag("mistakeBookHeader")
            )
            Spacer(Modifier.weight(1f))
            if (entries.isNotEmpty()) {
                IconButton(onClick = { showClearDialog = true }) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "清空错题本",
                        tint = HanziColors.AccentDeep
                    )
                }
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = "设置",
                    tint = HanziColors.AccentDeep
                )
            }
        }

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Filled.StarBorder,
                        contentDescription = null,
                        tint = HanziColors.Accent.copy(alpha = 0.6f),
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = "还没有错题",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = HanziColors.AccentDeep
                    )
                    Text(
                        text = "学习时点「加入错题本」\n错得多的会排在前面",
                        style = MaterialTheme.typography.bodyMedium,
                        color = HanziColors.TextPrimary.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 560.dp)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onReview(entries.map { it.char }) }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = HanziColors.AccentDeep
                        )
                        Text(
                            text = "开始复习（${entries.size} 个）",
                            color = HanziColors.AccentDeep,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                items(entries, key = { it.char }) { entry ->
                    MistakeRow(entry, onRemove, onDetail)
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空错题本？") },
            text = { Text("所有错题记录将被删除，且无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClear()
                    }
                ) {
                    Text("清空", color = HanziColors.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun MistakeRow(
    entry: MistakeEntry,
    onRemove: (String) -> Unit,
    onDetail: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDetail(entry.char) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .cardBackground(cornerRadius = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = entry.char,
                fontFamily = HanziFontFamily,
                fontSize = 48.sp
            )
        }
        Spacer(Modifier.padding(horizontal = 8.dp))
        Text(
            text = "错 ${entry.count} 次",
            color = HanziColors.AccentDeep,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier
                .background(HanziColors.SoftYellow, CircleShape)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = { onRemove(entry.char) }) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "删除 ${entry.char}",
                tint = HanziColors.AccentDeep
            )
        }
    }
}
```

- [ ] **Step 3: 写详情页**

`android/app/src/main/java/com/liuli/hanzirandom/ui/CharacterDetailScreen.kt`:
```kotlin
package com.liuli.hanzirandom.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liuli.hanzirandom.ui.components.PillButton
import com.liuli.hanzirandom.ui.components.PillStyle
import com.liuli.hanzirandom.ui.components.TianZiGe
import com.liuli.hanzirandom.ui.theme.HanziColors
import com.liuli.hanzirandom.ui.theme.HanziFontFamily
import com.liuli.hanzirandom.ui.theme.cardBackground
import kotlinx.coroutines.delay

@Composable
fun CharacterDetailScreen(
    character: String,
    onSpeak: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1500)
            copied = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HanziColors.SoftYellow.copy(alpha = 0.25f))
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onClose) {
                    Text("关闭")
                }
            }
            Spacer(Modifier.weight(1f))
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(3f)
                    .cardBackground()
                    .clickable { onSpeak(character) }
            ) {
                TianZiGe(Modifier.fillMaxSize())
                val side = minOf(maxWidth.value, maxHeight.value) * 0.8f
                Text(
                    text = character,
                    modifier = Modifier.align(Alignment.Center),
                    fontFamily = HanziFontFamily,
                    fontSize = minOf(200f, side).sp,
                    color = HanziColors.TextPrimary
                )
            }
            Spacer(Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PillButton(
                    text = "读一读",
                    onClick = { onSpeak(character) },
                    style = PillStyle.Secondary,
                    icon = Icons.Filled.VolumeUp
                )
                PillButton(
                    text = if (copied) "已复制" else "复制",
                    onClick = {
                        copyToClipboard(context, character)
                        copied = true
                    },
                    style = PillStyle.Primary
                )
            }
            Spacer(Modifier.weight(1f))
        }
    }
}
```

- [ ] **Step 4: 写 App 导航壳并接入 MainActivity**

`android/app/src/main/java/com/liuli/hanzirandom/ui/App.kt`:
```kotlin
package com.liuli.hanzirandom.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liuli.hanzirandom.HanziApplication
import com.liuli.hanzirandom.ui.theme.HanziColors
import com.liuli.hanzirandom.ui.theme.HanziRandomTheme

sealed interface AppScreen {
    data object Main : AppScreen
    data class Detail(val character: String) : AppScreen
    data class Review(val characters: List<String>) : AppScreen
}

@Composable
fun HanziRandomApp(app: HanziApplication) {
    HanziRandomTheme {
        val stack = remember { mutableStateListOf<AppScreen>(AppScreen.Main) }
        BackHandler(enabled = stack.size > 1) {
            stack.removeAt(stack.lastIndex)
        }

        when (val screen = stack.last()) {
            AppScreen.Main -> MainTabs(
                app = app,
                onDetail = { stack.add(AppScreen.Detail(it)) },
                onReview = { stack.add(AppScreen.Review(it)) }
            )

            is AppScreen.Detail -> CharacterDetailScreen(
                character = screen.character,
                onSpeak = { app.speechService.speak(it) },
                onClose = { stack.removeAt(stack.lastIndex) }
            )

            is AppScreen.Review -> {
                val vm: ReviewViewModel = viewModel(
                    key = "review-${screen.characters.joinToString("")}",
                    factory = ReviewViewModel.factory(app, screen.characters)
                )
                val state by vm.state.collectAsStateWithLifecycle()
                val justAdded by vm.justAddedMistake.collectAsStateWithLifecycle()
                val fontSize by app.settingsRepository.fontSize.collectAsStateWithLifecycle()
                val swipeEnabled by app.settingsRepository.swipeEnabled.collectAsStateWithLifecycle()
                StudyScreen(
                    state = state,
                    fontSize = fontSize,
                    swipeEnabled = swipeEnabled,
                    justAddedMistake = justAdded,
                    mode = StudyMode.Review,
                    onNext = vm::next,
                    onPrevious = vm::previous,
                    onReset = {},
                    onSpeak = vm::speak,
                    onAddMistake = vm::addMistake,
                    onRemoveMistake = vm::removeMistake,
                    onOpenSettings = {},
                    onClose = { stack.removeAt(stack.lastIndex) }
                )
            }
        }
    }
}

@Composable
private fun MainTabs(
    app: HanziApplication,
    onDetail: (String) -> Unit,
    onReview: (List<String>) -> Unit
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = HanziColors.CardBackground) {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.School, contentDescription = null) },
                    label = { Text("学习") },
                    colors = navigationItemColors()
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.Star, contentDescription = null) },
                    label = { Text("错题本") },
                    colors = navigationItemColors()
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (tab == 0) {
                val vm: StudyViewModel = viewModel(factory = StudyViewModel.factory(app))
                val state by vm.state.collectAsStateWithLifecycle()
                val fontSize by vm.fontSize.collectAsStateWithLifecycle()
                val swipeEnabled by vm.swipeEnabled.collectAsStateWithLifecycle()
                val justAdded by vm.justAddedMistake.collectAsStateWithLifecycle()
                StudyScreen(
                    state = state,
                    fontSize = fontSize,
                    swipeEnabled = swipeEnabled,
                    justAddedMistake = justAdded,
                    mode = StudyMode.Learning,
                    onNext = vm::next,
                    onPrevious = vm::previous,
                    onReset = vm::reset,
                    onSpeak = vm::speak,
                    onAddMistake = vm::addMistake,
                    onRemoveMistake = {},
                    onOpenSettings = {},
                    onClose = {}
                )
            } else {
                val vm: MistakeBookViewModel = viewModel(factory = MistakeBookViewModel.factory(app))
                val entries by vm.mistakes.collectAsStateWithLifecycle()
                MistakeBookScreen(
                    entries = entries,
                    onReview = onReview,
                    onRemove = vm::remove,
                    onClear = vm::clear,
                    onDetail = onDetail,
                    onOpenSettings = {}
                )
            }
        }
    }
}

@Composable
private fun navigationItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = HanziColors.AccentDeep,
    selectedTextColor = HanziColors.AccentDeep,
    indicatorColor = HanziColors.SoftYellow
)
```

`MainActivity.kt` 替换为：
```kotlin
package com.liuli.hanzirandom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.liuli.hanzirandom.ui.HanziRandomApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as HanziApplication
        setContent {
            HanziRandomApp(app)
        }
    }
}
```

- [ ] **Step 5: 构建、安装、截图验证**

Run:
```bash
cd android
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew :app:installDebug
adb shell am start -n com.liuli.hanzirandom/.MainActivity
sleep 2
adb exec-out screencap -p > ../docs/screenshots/android/study.png
adb shell input tap 900 1700
sleep 1
adb exec-out screencap -p > ../docs/screenshots/android/mistake-book.png
```
（坐标以实际模拟器分辨率为准；用 `adb shell wm size` 与 `adb shell uiautomator dump` 辅助定位「错题本」Tab。）

Expected: 学习页正常；错题本 Tab 显示「还没有错题」空状态。

- [ ] **Step 6: 提交**

```bash
git add android/app/src docs/screenshots/android
git commit -m "feat(android): add mistake book, review flow, detail screen, and app navigation"
```

---

### Task 8: 设置页与字库管理

**Files:**
- Create: `android/app/src/main/java/com/liuli/hanzirandom/ui/SettingsViewModel.kt`
- Create: `android/app/src/main/java/com/liuli/hanzirandom/ui/SettingsScreen.kt`
- Modify: `android/app/src/main/java/com/liuli/hanzirandom/ui/App.kt`（增加 Settings 路由，接上所有设置入口）
- Modify: `android/app/src/main/java/com/liuli/hanzirandom/ui/MainTabs`（传入 `onSettings`）

- [ ] **Step 1: 写 SettingsViewModel**

`android/app/src/main/java/com/liuli/hanzirandom/ui/SettingsViewModel.kt`:
```kotlin
package com.liuli.hanzirandom.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.liuli.hanzirandom.HanziApplication
import com.liuli.hanzirandom.data.CharacterRepository
import com.liuli.hanzirandom.data.SettingsRepository

class SettingsViewModel(
    private val repository: CharacterRepository,
    private val settings: SettingsRepository
) : ViewModel() {
    val library = repository.library
    val fontSize = settings.fontSize
    val swipeEnabled = settings.swipeEnabled

    fun setFontSize(value: Float) = settings.setFontSize(value)

    fun setSwipeEnabled(enabled: Boolean) = settings.setSwipeEnabled(enabled)

    fun resetRound() = repository.reset()

    fun addCharacters(text: String): Int = repository.addCharacters(text)

    fun remove(character: String) = repository.remove(character)

    fun exportString(): String = repository.exportString()

    companion object {
        fun factory(app: HanziApplication): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SettingsViewModel(app.characterRepository, app.settingsRepository) as T
            }
    }
}
```

- [ ] **Step 2: 写设置界面**

`android/app/src/main/java/com/liuli/hanzirandom/ui/SettingsScreen.kt`:
```kotlin
package com.liuli.hanzirandom.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liuli.hanzirandom.ui.components.PillButton
import com.liuli.hanzirandom.ui.components.PillStyle
import com.liuli.hanzirandom.ui.theme.HanziColors
import com.liuli.hanzirandom.ui.theme.HanziFontFamily
import com.liuli.hanzirandom.ui.theme.cardBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    library: List<String>,
    fontSize: Float,
    swipeEnabled: Boolean,
    onFontSizeChange: (Float) -> Unit,
    onSwipeEnabledChange: (Boolean) -> Unit,
    onResetRound: () -> Unit,
    onAddCharacters: (String) -> Int,
    onRemove: (String) -> Unit,
    onExportString: () -> String,
    onDetail: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var input by rememberSaveable { mutableStateOf("") }
    var copiedAll by remember { mutableStateOf(false) }

    LaunchedEffect(copiedAll) {
        if (copiedAll) {
            delay(1500)
            copiedAll = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HanziColors.SoftYellow.copy(alpha = 0.25f))
            .safeDrawingPadding()
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "设置",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = HanziColors.AccentDeep
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onClose) {
                    Text("完成", fontSize = 17.sp)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .cardBackground()
                            .padding(16.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("字号", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = fontSize.toInt().toString(),
                                fontWeight = FontWeight.SemiBold,
                                color = HanziColors.AccentDeep
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(84.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "永",
                                fontFamily = HanziFontFamily,
                                fontSize = minOf(fontSize, 72f).sp,
                                color = HanziColors.TextPrimary
                            )
                        }
                        Slider(
                            value = fontSize,
                            onValueChange = onFontSizeChange,
                            valueRange = 60f..260f
                        )
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .cardBackground()
                            .padding(16.dp)
                    ) {
                        Text("手势", fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("左右滑动切换")
                            Spacer(Modifier.weight(1f))
                            Switch(
                                checked = swipeEnabled,
                                onCheckedChange = onSwipeEnabledChange
                            )
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .cardBackground()
                            .padding(16.dp)
                    ) {
                        Text("抽卡", fontWeight = FontWeight.Bold)
                        TextButton(onClick = onResetRound) {
                            Icon(
                                Icons.Filled.Shuffle,
                                contentDescription = null,
                                tint = HanziColors.AccentDeep
                            )
                            Text(
                                text = "重新开始抽卡",
                                color = HanziColors.AccentDeep,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        Text(
                            text = "用最新的字库重新洗牌开始一轮。新加的字会立即加入还没抽完的这一轮；" +
                                "如果本轮已经抽完，重新开始后生效。",
                            fontSize = 12.sp,
                            color = HanziColors.TextPrimary.copy(alpha = 0.6f)
                        )
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .cardBackground()
                            .padding(16.dp)
                    ) {
                        Text("添加汉字（可粘贴整段）", fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp)
                                .padding(top = 8.dp),
                            placeholder = { Text("输入或粘贴汉字…") }
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = {
                                    val added = onAddCharacters(input)
                                    input = ""
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (added > 0) "已添加 $added 个汉字" else "没有新增汉字"
                                        )
                                    }
                                },
                                enabled = input.isNotEmpty()
                            ) {
                                Text("添加")
                            }
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .cardBackground()
                            .padding(16.dp)
                    ) {
                        Text("导出全部", fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    copyToClipboard(context, onExportString())
                                    copiedAll = true
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (copiedAll) "已复制" else "复制到剪贴板", fontSize = 13.sp)
                            }
                            OutlinedButton(
                                onClick = { shareText(context, onExportString()) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("分享/存到备忘录", fontSize = 13.sp)
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "字库（${library.size}）",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                items(library, key = { it }) { character ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDetail(character) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = character,
                            fontFamily = HanziFontFamily,
                            fontSize = 22.sp
                        )
                        Spacer(Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                copyToClipboard(context, character)
                                scope.launch {
                                    snackbarHostState.showSnackbar("已复制 $character")
                                }
                            }
                        ) {
                            Icon(
                                Icons.Filled.ContentCopy,
                                contentDescription = "复制 $character",
                                tint = HanziColors.AccentDeep
                            )
                        }
                        IconButton(onClick = { onRemove(character) }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "删除 $character",
                                tint = HanziColors.AccentDeep
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}
```
注意：`PillButton`/`PillStyle` 在本文件未使用，可以删掉这两个 import；若保留会产生未使用 import 警告（不影响构建）。

- [ ] **Step 3: 在 App.kt 中接上设置路由**

`App.kt` 顶部 `AppScreen` 增加：
```kotlin
data object Settings : AppScreen
```

`HanziRandomApp` 的 `when` 增加分支（放在 `AppScreen.Main` 之后）：
```kotlin
AppScreen.Settings -> {
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(app))
    val library by vm.library.collectAsStateWithLifecycle()
    val fontSize by vm.fontSize.collectAsStateWithLifecycle()
    val swipeEnabled by vm.swipeEnabled.collectAsStateWithLifecycle()
    SettingsScreen(
        library = library,
        fontSize = fontSize,
        swipeEnabled = swipeEnabled,
        onFontSizeChange = vm::setFontSize,
        onSwipeEnabledChange = vm::setSwipeEnabled,
        onResetRound = vm::resetRound,
        onAddCharacters = vm::addCharacters,
        onRemove = vm::remove,
        onExportString = vm::exportString,
        onDetail = { stack.add(AppScreen.Detail(it)) },
        onClose = { stack.removeAt(stack.lastIndex) }
    )
}
```

`AppScreen.Main` 分支改为传入设置回调：
```kotlin
AppScreen.Main -> MainTabs(
    app = app,
    onSettings = { stack.add(AppScreen.Settings) },
    onDetail = { stack.add(AppScreen.Detail(it)) },
    onReview = { stack.add(AppScreen.Review(it)) }
)
```

`MainTabs` 签名增加参数：
```kotlin
private fun MainTabs(
    app: HanziApplication,
    onSettings: () -> Unit,
    onDetail: (String) -> Unit,
    onReview: (List<String>) -> Unit
)
```
并把学习页与错题本页里的 `onOpenSettings = {}` 都改成 `onOpenSettings = onSettings`。

- [ ] **Step 4: 构建、安装、截图验证设置页**

Run:
```bash
cd android
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew :app:installDebug
adb shell am start -n com.liuli.hanzirandom/.MainActivity
sleep 2
# 用 uiautomator dump 找到「设置」图标的 bounds 后点击
adb shell uiautomator dump /sdcard/ui.xml
adb pull /sdcard/ui.xml /tmp/ui.xml
# 从 /tmp/ui.xml 的 content-desc="设置" 节点读取 bounds，计算中心点后：
# adb shell input tap <x> <y>
sleep 1
adb exec-out screencap -p > ../docs/screenshots/android/settings.png
```
Expected: 设置页显示字号滑块、手势开关、抽卡、添加、导出、字库列表；返回学习页后字号/手势生效。

- [ ] **Step 5: 提交**

```bash
git add android/app/src docs/screenshots/android
git commit -m "feat(android): add settings and library management"
```

---

### Task 9: 应用图标与启动页

**Files:**
- Create: `android/app/src/main/res/mipmap-mdpi/ic_launcher.png`（48×48）
- Create: `android/app/src/main/res/mipmap-hdpi/ic_launcher.png`（72×72）
- Create: `android/app/src/main/res/mipmap-xhdpi/ic_launcher.png`（96×96）
- Create: `android/app/src/main/res/mipmap-xxhdpi/ic_launcher.png`（144×144）
- Create: `android/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png`（192×192）
- Modify: `android/app/src/main/AndroidManifest.xml`（`android:icon`、启动主题）
- Modify: `android/app/src/main/res/values/themes.xml`（增加 `Theme.HanziRandom.Starting`）
- Modify: `android/app/src/main/java/com/liuli/hanzirandom/MainActivity.kt`（`installSplashScreen()`）

- [ ] **Step 1: 生成各密度图标**

Run:
```bash
cd /Users/liuli/HanziRandom
SRC=Sources/Assets.xcassets/AppIcon.appiconset/AppIcon-1024.png
for spec in "mdpi 48" "hdpi 72" "xhdpi 96" "xxhdpi 144" "xxxhdpi 192"; do
  set -- $spec
  mkdir -p android/app/src/main/res/mipmap-$1
  sips -z $2 $2 "$SRC" --out android/app/src/main/res/mipmap-$1/ic_launcher.png >/dev/null
done
ls -l android/app/src/main/res/mipmap-*/ic_launcher.png
```
Expected: 5 个尺寸正确的 PNG。

- [ ] **Step 2: 配置图标与启动主题**

`AndroidManifest.xml` 的 `<application>` 增加：
```xml
android:icon="@mipmap/ic_launcher"
```
并把 application 与 activity 的 `android:theme` 都改为 `@style/Theme.HanziRandom.Starting`。

`themes.xml` 增加（保留原 `Theme.HanziRandom`）：
```xml
<style name="Theme.HanziRandom.Starting" parent="Theme.SplashScreen">
    <item name="windowSplashScreenBackground">@color/splash_background</item>
    <item name="windowSplashScreenAnimatedIcon">@mipmap/ic_launcher</item>
    <item name="postSplashScreenTheme">@style/Theme.HanziRandom</item>
</style>
```

`MainActivity.onCreate` 改为：
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    val app = application as HanziApplication
    setContent {
        HanziRandomApp(app)
    }
}
```
并增加 import：
```kotlin
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
```

- [ ] **Step 3: 构建、安装、截图验证**

Run:
```bash
cd android
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew :app:installDebug
adb shell am force-stop com.liuli.hanzirandom
adb shell am start -n com.liuli.hanzirandom/.MainActivity
sleep 2
adb exec-out screencap -p > ../docs/screenshots/android/launch.png
```
Expected: APK 图标为橙色「字」图标；启动时出现暖橙/红橙色启动画面。

- [ ] **Step 4: 提交**

```bash
git add android/app/src docs/screenshots/android
git commit -m "feat(android): add launcher icon and splash screen"
```

---

### Task 10: Compose UI 冒烟测试

**Files:**
- Create: `android/app/src/androidTest/java/com/liuli/hanzirandom/AppSmokeTest.kt`

- [ ] **Step 1: 写 UI 测试**

`android/app/src/androidTest/java/com/liuli/hanzirandom/AppSmokeTest.kt`:
```kotlin
package com.liuli.hanzirandom

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppSmokeTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun studyShowsCharacterAndTabsSwitch() {
        rule.onNodeWithTag("characterLabel").assertExists()

        rule.onNodeWithText("错题本").performClick()
        rule.onNodeWithTag("mistakeBookHeader").assertExists()
    }
}
```

- [ ] **Step 2: 确保模拟器运行并执行测试**

Run:
```bash
adb devices
# 若没有设备：
~/Library/Android/sdk/emulator/emulator -avd Pixel_Fold_API_35 -no-window -no-audio -no-boot-anim &
adb wait-for-device
adb shell 'while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 1; done'
cd android
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew connectedDebugAndroidTest
```
Expected: `BUILD SUCCESSFUL`，`AppSmokeTest` 通过。

- [ ] **Step 3: 提交**

```bash
git add android/app/src/androidTest
git commit -m "test(android): add compose UI smoke test"
```

---

### Task 11: 最终验证、截图与 README

**Files:**
- Create: `docs/screenshots/android/study.png`
- Create: `docs/screenshots/android/mistake-book.png`
- Create: `docs/screenshots/android/settings.png`
- Create: `docs/screenshots/android/detail.png`
- Modify: `README.md`（增加 Android 构建与结构说明）

- [ ] **Step 1: 全量单测 + 构建**

Run:
```bash
cd android
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew testDebugUnitTest assembleDebug
```
Expected: 全部通过，APK 生成。

- [ ] **Step 2: 验证核心交互**

在模拟器上依次验证并记录结果：
1. 学习页点「下一个」换字；点「上一个」回看。
2. 右滑/左滑切换；到设置里关闭手势后滑动无效、按钮仍有效。
3. 点「加入错题本」后，错题本出现该字，次数 +1；再点一次次数 +1。
4. 错题本点「开始复习」进入全屏复习；「删除错题」后该字从列表消失。
5. 设置里字号滑块改变学习页大字大小；重启 App 后保持不变。
6. 粘贴一段含非汉字/重复字文本点「添加」，提示新增数量正确。
7. 复制/分享按钮可调起系统剪贴板与分享面板。
8. 学习到一半按 Home 退到后台，再启动 App，轮次与当前字恢复。
9. 详情页点大字/「读一读」触发朗读；模拟器无中文 TTS 时静默（属预期）。

- [ ] **Step 3: 截取最终截图**

按 Task 7/8 的方式导航并截图，保存为：
- `docs/screenshots/android/study.png`
- `docs/screenshots/android/mistake-book.png`
- `docs/screenshots/android/settings.png`
- `docs/screenshots/android/detail.png`

- [ ] **Step 4: 更新 README**

在 `README.md` 末尾追加：
```markdown
## Android 版

Android 版位于 `android/`，与 iOS 版功能对齐（Kotlin + Jetpack Compose）。

### 环境要求

- JDK 17（AGP 要求）：`brew install --cask temurin@17`
- Android SDK Platform 35
- 模拟器或真机（Android 8.0 / API 26+）

### 构建与安装

```bash
cd android
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew assembleDebug
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew installDebug
```

### 测试

```bash
cd android
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew testDebugUnitTest
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew connectedDebugAndroidTest
```

### 结构

```
android/app/src/main/java/com/liuli/hanzirandom/
  MainActivity.kt            单 Activity 入口
  HanziApplication.kt        仓库、朗读、后台轮次保存
  data/                      KeyValueStore、字库/错题/设置仓库
  domain/                    DrawSession 状态机、TextToSpeech 封装
  ui/                        Compose 页面、ViewModel、主题与组件
android/app/src/main/res/font/
  lxgw_wenkai_gb_lite_regular.ttf   内嵌正楷字体
```

设计文档见 [docs/superpowers/specs/2026-10-19-android-port-design.md](docs/superpowers/specs/2026-10-19-android-port-design.md)。
```

- [ ] **Step 5: 提交**

```bash
git add docs/screenshots/android README.md
git commit -m "docs: add Android build instructions and screenshots"
```

---

## 自查记录

- **Spec 覆盖**：学习页（Task 6）、错题本/复习（Task 7）、设置/字库（Task 8）、朗读（Task 6）、田字格/字体（Task 5）、图标/启动页（Task 9）、持久化（Task 3/4）、测试（Task 2/3/4/10）、截图验收（Task 6/7/8/11）均有对应任务。
- **类型一致性**：`SessionState`、`RoundSnapshot`、`MistakeEntry`、`DrawSession.state`、`CharacterRepository.session/library/mistakes`、`SettingsRepository.fontSize/swipeEnabled` 在所有任务中命名一致。
- **持久化格式**：字库换行分隔、错题 `字\t次数`、轮次首行元信息 + 条目行；Ext-B 代理对通过按 code point 处理与换行编码保证不拆散。
- **已知环境风险**：JDK 17 安装需要用户授权；首次 Gradle 构建会下载 AndroidX/Compose 依赖；模拟器无中文 TTS 时朗读静默属预期。
