package com.liuli.hanzirandom.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NavigationStackCodecTest {

    private val sep = "\u001F"

    @Test
    fun roundTrip_main() {
        assertEquals(AppScreen.Main, decode(encode(AppScreen.Main)))
    }

    @Test
    fun roundTrip_detail() {
        assertEquals(AppScreen.Detail("马"), decode(encode(AppScreen.Detail("马"))))
    }

    @Test
    fun roundTrip_reviewWithThreeCharacters() {
        val review = AppScreen.Review(listOf("山", "水", "火"), 42L)
        assertEquals(review, decode(encode(review)))
    }

    @Test
    fun decode_main() {
        assertEquals(AppScreen.Main, decode("main"))
    }

    @Test
    fun decode_detail() {
        assertEquals(AppScreen.Detail("马"), decode("detail" + sep + "马"))
    }

    @Test
    fun decode_review() {
        assertEquals(
            AppScreen.Review(listOf("山", "水"), 42L),
            decode("review" + sep + "42" + sep + "山" + sep + "水")
        )
    }

    @Test
    fun decode_empty_returnsNull() {
        assertNull(decode(""))
    }

    @Test
    fun decode_detailWithoutCharacter_returnsNull() {
        assertNull(decode("detail"))
    }

    @Test
    fun decode_detailWithEmptyCharacter_returnsNull() {
        assertNull(decode("detail" + sep))
    }

    @Test
    fun decode_detailWithExtraParts_returnsNull() {
        assertNull(decode("detail" + sep + "马" + sep + "extra"))
    }

    @Test
    fun decode_reviewWithoutId_returnsNull() {
        assertNull(decode("review"))
    }

    @Test
    fun decode_reviewWithoutCharacters_returnsNull() {
        assertNull(decode("review" + sep + "1"))
    }

    @Test
    fun decode_reviewWithEmptyCharacters_returnsNull() {
        assertNull(decode("review" + sep + "1" + sep))
    }

    @Test
    fun decode_reviewWithNonNumericId_returnsNull() {
        assertNull(decode("review" + sep + "notanumber" + sep + "山"))
    }

    @Test
    fun decode_unknownRoute_returnsNull() {
        assertNull(decode("bogus"))
    }

    @Test
    fun encode_typicalHanziInput_neverCrashes() {
        val inputs = listOf(
            AppScreen.Detail("龘"),
            AppScreen.Review(listOf("一", "丁", "七", "万", "丈"), Long.MAX_VALUE),
            AppScreen.Review(listOf("山", "水", "火"), 0L)
        )
        inputs.forEach { screen ->
            assertEquals(screen, decode(encode(screen)))
        }
    }

    private fun encode(screen: AppScreen): String = NavigationStackCodec.encode(screen)

    private fun decode(encoded: String): AppScreen? = NavigationStackCodec.decode(encoded)
}
