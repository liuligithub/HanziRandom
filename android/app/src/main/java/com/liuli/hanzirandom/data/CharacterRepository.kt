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

    fun addCharacters(text: String): Int {
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
