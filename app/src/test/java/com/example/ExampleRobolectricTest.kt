package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.DiaryItem
import com.example.ui.viewmodel.EntryDraftState
import com.example.util.GujaratiDateUtils
import com.example.util.GujaratiVoiceParser
import com.example.util.IndianNumberFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("રોજની ડાયરી", appName)
  }

  @Test
  fun `test indian number formatting`() {
    assertEquals("2,51,904", IndianNumberFormatter.formatIndian(251904))
    assertEquals("1,00,000", IndianNumberFormatter.formatIndian(100000))
    assertEquals("1,400", IndianNumberFormatter.formatIndian(1400))
    assertEquals("500", IndianNumberFormatter.formatIndian(500))
    assertEquals("0", IndianNumberFormatter.formatIndian(0))
  }

  @Test
  fun `test india timezone date utils`() {
    assertEquals("Asia/Kolkata", GujaratiDateUtils.INDIA_TIME_ZONE.id)
    val todayIso = GujaratiDateUtils.getTodayIsoDate()
    assertTrue(todayIso.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
    val gujaratiFormatted = GujaratiDateUtils.formatGujaratiDate("2026-09-28")
    assertEquals("સોમવાર, 28 સપ્ટેમ્બર 2026", gujaratiFormatted)
  }

  @Test
  fun `test gujarati voice amount parsing`() {
    assertEquals(50000L, GujaratiVoiceParser.parseSpokenAmount("50000"))
    assertEquals(50000L, GujaratiVoiceParser.parseSpokenAmount("પચાસ હજાર"))
    assertEquals(200000L, GujaratiVoiceParser.parseSpokenAmount("2 લાખ"))
    assertEquals(12000L, GujaratiVoiceParser.parseSpokenAmount("બાર હજાર રૂપિયા"))
    assertEquals(500L, GujaratiVoiceParser.parseSpokenAmount("૫૦૦"))
  }

  @Test
  fun `test float total and drop alert`() {
    val draft = EntryDraftState(
        items = listOf(
            DiaryItem(name = "ID માં", amount = 45000L),
            DiaryItem(name = "રોકડ", amount = 15000L),
            DiaryItem(name = "અન્ય", amount = 10000L)
        ),
        total = 70000L,
        previousSavedDayTotal = 90000L
    )

    assertEquals(60000L, draft.floatTotal)
    assertTrue(draft.isDropAlertActive) // 90000 - 70000 = 20000 > 10000
    assertEquals(20000L, draft.dropDifference)
  }
}
