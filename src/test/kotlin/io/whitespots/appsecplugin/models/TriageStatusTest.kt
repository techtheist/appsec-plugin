package io.whitespots.appsecplugin.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class TriageStatusTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `test TriageStatus enum values and integer mapping`() {
        assertEquals(0, TriageStatus.RESOLVED.intValue)
        assertEquals(1, TriageStatus.UNVERIFIED.intValue)
        assertEquals(2, TriageStatus.VERIFIED.intValue)
        assertEquals(3, TriageStatus.ASSIGNED.intValue)
        assertEquals(4, TriageStatus.REJECTED.intValue)
        assertEquals(5, TriageStatus.TEMPORARILY.intValue)
        assertEquals(6, TriageStatus.PERMANENTLY.intValue)
    }

    @Test
    fun `test TriageStatus fromInt with valid values`() {
        assertEquals(TriageStatus.RESOLVED, TriageStatus.fromInt(0))
        assertEquals(TriageStatus.UNVERIFIED, TriageStatus.fromInt(1))
        assertEquals(TriageStatus.VERIFIED, TriageStatus.fromInt(2))
        assertEquals(TriageStatus.ASSIGNED, TriageStatus.fromInt(3))
        assertEquals(TriageStatus.REJECTED, TriageStatus.fromInt(4))
        assertEquals(TriageStatus.TEMPORARILY, TriageStatus.fromInt(5))
        assertEquals(TriageStatus.PERMANENTLY, TriageStatus.fromInt(6))
    }

    @Test
    fun `test TriageStatus fromInt with invalid value returns UNVERIFIED`() {
        assertEquals(TriageStatus.UNVERIFIED, TriageStatus.fromInt(-1))
        assertEquals(TriageStatus.UNVERIFIED, TriageStatus.fromInt(7))
        assertEquals(TriageStatus.UNVERIFIED, TriageStatus.fromInt(999))
    }

    @Test
    fun `test TriageStatus serialization`() {
        assertEquals("0", json.encodeToString(TriageStatus.RESOLVED))
        assertEquals("1", json.encodeToString(TriageStatus.UNVERIFIED))
        assertEquals("2", json.encodeToString(TriageStatus.VERIFIED))
        assertEquals("3", json.encodeToString(TriageStatus.ASSIGNED))
        assertEquals("4", json.encodeToString(TriageStatus.REJECTED))
        assertEquals("5", json.encodeToString(TriageStatus.TEMPORARILY))
        assertEquals("6", json.encodeToString(TriageStatus.PERMANENTLY))
    }

    @Test
    fun `test TriageStatus deserialization`() {
        assertEquals(TriageStatus.RESOLVED, json.decodeFromString<TriageStatus>("0"))
        assertEquals(TriageStatus.UNVERIFIED, json.decodeFromString<TriageStatus>("1"))
        assertEquals(TriageStatus.VERIFIED, json.decodeFromString<TriageStatus>("2"))
        assertEquals(TriageStatus.ASSIGNED, json.decodeFromString<TriageStatus>("3"))
        assertEquals(TriageStatus.REJECTED, json.decodeFromString<TriageStatus>("4"))
        assertEquals(TriageStatus.TEMPORARILY, json.decodeFromString<TriageStatus>("5"))
        assertEquals(TriageStatus.PERMANENTLY, json.decodeFromString<TriageStatus>("6"))
    }

    @Test
    fun `test TriageStatus deserialization with invalid value`() {
        assertEquals(TriageStatus.UNVERIFIED, json.decodeFromString<TriageStatus>("-1"))
        assertEquals(TriageStatus.UNVERIFIED, json.decodeFromString<TriageStatus>("999"))
    }

    @Test
    fun `test TriageStatus in JSON object`() {
        @Serializable
        data class TestObject(val status: TriageStatus)

        val testObj = TestObject(TriageStatus.VERIFIED)
        val jsonString = json.encodeToString(testObj)
        val deserializedObj = json.decodeFromString<TestObject>(jsonString)

        assertEquals(TriageStatus.VERIFIED, deserializedObj.status)
        assertTrue(jsonString.contains("\"status\":2"))
    }

    @Test
    fun `test all TriageStatus enum entries`() {
        val expectedStatuses = setOf(
            TriageStatus.RESOLVED,
            TriageStatus.UNVERIFIED,
            TriageStatus.VERIFIED,
            TriageStatus.ASSIGNED,
            TriageStatus.REJECTED,
            TriageStatus.TEMPORARILY,
            TriageStatus.PERMANENTLY
        )

        assertEquals(expectedStatuses, TriageStatus.entries.toSet())
        assertEquals(7, TriageStatus.entries.size)
    }

    @Test
    fun `test TriageStatus unique integer values`() {
        val intValues = TriageStatus.entries.map { it.intValue }
        val uniqueIntValues = intValues.toSet()

        assertEquals(intValues.size, uniqueIntValues.size)
        assertEquals(setOf(0, 1, 2, 3, 4, 5, 6), uniqueIntValues)
    }
}
