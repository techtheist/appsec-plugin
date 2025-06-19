package io.whitespots.appsecplugin.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class SeverityTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `test Severity enum values and integer mapping`() {
        assertEquals(0, Severity.INFO.intValue)
        assertEquals(1, Severity.LOW.intValue)
        assertEquals(2, Severity.MEDIUM.intValue)
        assertEquals(3, Severity.HIGH.intValue)
        assertEquals(4, Severity.CRITICAL.intValue)
    }

    @Test
    fun `test Severity fromInt with valid values`() {
        assertEquals(Severity.INFO, Severity.fromInt(0))
        assertEquals(Severity.LOW, Severity.fromInt(1))
        assertEquals(Severity.MEDIUM, Severity.fromInt(2))
        assertEquals(Severity.HIGH, Severity.fromInt(3))
        assertEquals(Severity.CRITICAL, Severity.fromInt(4))
    }

    @Test
    fun `test Severity fromInt with invalid value returns INFO`() {
        assertEquals(Severity.INFO, Severity.fromInt(-1))
        assertEquals(Severity.INFO, Severity.fromInt(5))
        assertEquals(Severity.INFO, Severity.fromInt(999))
    }

    @Test
    fun `test Severity serialization`() {
        assertEquals("0", json.encodeToString(Severity.INFO))
        assertEquals("1", json.encodeToString(Severity.LOW))
        assertEquals("2", json.encodeToString(Severity.MEDIUM))
        assertEquals("3", json.encodeToString(Severity.HIGH))
        assertEquals("4", json.encodeToString(Severity.CRITICAL))
    }

    @Test
    fun `test Severity deserialization`() {
        assertEquals(Severity.INFO, json.decodeFromString<Severity>("0"))
        assertEquals(Severity.LOW, json.decodeFromString<Severity>("1"))
        assertEquals(Severity.MEDIUM, json.decodeFromString<Severity>("2"))
        assertEquals(Severity.HIGH, json.decodeFromString<Severity>("3"))
        assertEquals(Severity.CRITICAL, json.decodeFromString<Severity>("4"))
    }

    @Test
    fun `test Severity deserialization with invalid value`() {
        assertEquals(Severity.INFO, json.decodeFromString<Severity>("-1"))
        assertEquals(Severity.INFO, json.decodeFromString<Severity>("999"))
    }

    @Test
    fun `test Severity in JSON object`() {
        @Serializable
        data class TestObject(val severity: Severity)

        val testObj = TestObject(Severity.HIGH)
        val jsonString = json.encodeToString(testObj)
        val deserializedObj = json.decodeFromString<TestObject>(jsonString)

        assertEquals(Severity.HIGH, deserializedObj.severity)
        assertTrue(jsonString.contains("\"severity\":3"))
    }

    @Test
    fun `test Severity ordering by intValue`() {
        val severities = listOf(Severity.CRITICAL, Severity.INFO, Severity.HIGH, Severity.LOW, Severity.MEDIUM)
        val sortedBySeverity = severities.sortedBy { it.intValue }

        assertEquals(
            listOf(Severity.INFO, Severity.LOW, Severity.MEDIUM, Severity.HIGH, Severity.CRITICAL),
            sortedBySeverity
        )
    }

    @Test
    fun `test all Severity enum entries`() {
        val expectedSeverities = setOf(
            Severity.INFO,
            Severity.LOW,
            Severity.MEDIUM,
            Severity.HIGH,
            Severity.CRITICAL
        )

        assertEquals(expectedSeverities, Severity.entries.toSet())
        assertEquals(5, Severity.entries.size)
    }
}
