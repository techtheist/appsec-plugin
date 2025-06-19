package io.whitespots.appsecplugin.models

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class FindingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `test Finding serialization and deserialization`() {
        val finding = Finding(
            id = 123L,
            name = "SQL Injection",
            description = "Potential SQL injection vulnerability",
            filePath = "src/main/java/App.java",
            line = 42,
            severity = Severity.HIGH,
            triageStatus = TriageStatus.UNVERIFIED,
            product = 1L,
            dateCreated = "2024-01-01T10:00:00Z",
            findingUrl = "https://example.com/finding/123",
            tags = listOf("security", "sql"),
            lineText = "String query = \"SELECT * FROM users WHERE id = \" + userId;",
            language = "java"
        )

        val jsonString = json.encodeToString(finding)
        val deserializedFinding = json.decodeFromString<Finding>(jsonString)

        assertEquals(finding, deserializedFinding)
    }

    @Test
    fun `test Finding with null optional fields`() {
        val finding = Finding(
            id = 123L,
            name = "SQL Injection",
            description = null,
            filePath = null,
            line = null,
            severity = Severity.HIGH,
            triageStatus = TriageStatus.UNVERIFIED,
            product = 1L,
            dateCreated = null,
            findingUrl = null
        )

        assertNull(finding.description)
        assertNull(finding.filePath)
        assertNull(finding.line)
        assertNull(finding.dateCreated)
        assertNull(finding.findingUrl)
        assertTrue(finding.tags.isEmpty())
        assertNull(finding.lineText)
        assertNull(finding.language)
    }

    @Test
    fun `test Finding JSON deserialization with missing optional fields`() {
        val jsonString = """
            {
                "id": 123,
                "name": "Test Finding",
                "severity": 3,
                "current_sla_level": 1,
                "product": 1
            }
        """.trimIndent()

        val finding = json.decodeFromString<Finding>(jsonString)

        assertEquals(123L, finding.id)
        assertEquals("Test Finding", finding.name)
        assertNull(finding.description)
        assertNull(finding.filePath)
        assertNull(finding.line)
        assertEquals(Severity.HIGH, finding.severity)
        assertEquals(TriageStatus.UNVERIFIED, finding.triageStatus)
        assertEquals(1L, finding.product)
        assertNull(finding.dateCreated)
        assertNull(finding.findingUrl)
        assertTrue(finding.tags.isEmpty())
        assertNull(finding.lineText)
        assertNull(finding.language)
    }

    @Test
    fun `test Finding JSON deserialization with snake_case field names`() {
        val jsonString = """
            {
                "id": 123,
                "name": "Test Finding",
                "description": "Test description",
                "file_path": "src/main/App.java",
                "line": 42,
                "severity": 4,
                "current_sla_level": 2,
                "product": 1,
                "date_created": "2024-01-01T10:00:00Z",
                "dojo_finding_url": "https://example.com/finding/123",
                "tags": ["tag1", "tag2"],
                "line_text": "var x = input;",
                "language": "javascript"
            }
        """.trimIndent()

        val finding = json.decodeFromString<Finding>(jsonString)

        assertEquals(123L, finding.id)
        assertEquals("Test Finding", finding.name)
        assertEquals("Test description", finding.description)
        assertEquals("src/main/App.java", finding.filePath)
        assertEquals(42, finding.line)
        assertEquals(Severity.CRITICAL, finding.severity)
        assertEquals(TriageStatus.VERIFIED, finding.triageStatus)
        assertEquals(1L, finding.product)
        assertEquals("2024-01-01T10:00:00Z", finding.dateCreated)
        assertEquals("https://example.com/finding/123", finding.findingUrl)
        assertEquals(listOf("tag1", "tag2"), finding.tags)
        assertEquals("var x = input;", finding.lineText)
        assertEquals("javascript", finding.language)
    }

    @Test
    fun `test Finding toString contains basic information`() {
        val finding = Finding(
            id = 123L,
            name = "SQL Injection",
            description = "Test description",
            filePath = "src/main/App.java",
            line = 42,
            severity = Severity.HIGH,
            triageStatus = TriageStatus.UNVERIFIED,
            product = 1L,
            dateCreated = "2024-01-01T10:00:00Z",
            findingUrl = "https://example.com/finding/123"
        )

        val stringRepresentation = finding.toString()

        assertTrue(stringRepresentation.contains("123"))
        assertTrue(stringRepresentation.contains("SQL Injection"))
        assertTrue(stringRepresentation.contains("HIGH"))
    }

    @Test
    fun `test Finding equality`() {
        val finding1 = Finding(
            id = 123L,
            name = "SQL Injection",
            description = "Test description",
            filePath = "src/main/App.java",
            line = 42,
            severity = Severity.HIGH,
            triageStatus = TriageStatus.UNVERIFIED,
            product = 1L,
            dateCreated = "2024-01-01T10:00:00Z",
            findingUrl = "https://example.com/finding/123"
        )

        val finding2 = Finding(
            id = 123L,
            name = "SQL Injection",
            description = "Test description",
            filePath = "src/main/App.java",
            line = 42,
            severity = Severity.HIGH,
            triageStatus = TriageStatus.UNVERIFIED,
            product = 1L,
            dateCreated = "2024-01-01T10:00:00Z",
            findingUrl = "https://example.com/finding/123"
        )

        val finding3 = finding1.copy(id = 456L)

        assertEquals(finding1, finding2)
        assertNotEquals(finding1, finding3)
        assertEquals(finding1.hashCode(), finding2.hashCode())
        assertNotEquals(finding1.hashCode(), finding3.hashCode())
    }
}
