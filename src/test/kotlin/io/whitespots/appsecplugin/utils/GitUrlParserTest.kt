package io.whitespots.appsecplugin.utils

import org.junit.Assert.*
import org.junit.Test

class GitUtilsTest {

    @Test
    fun `test parse HTTPS GitHub URL`() {
        val url = "https://github.com/user/repository.git"
        val result = GitUtils.parse(url)

        assertNotNull(result)
        assertEquals("github.com", result!!.domain)
        assertEquals("user/repository", result.path)
    }

    @Test
    fun `test parse SSH GitHub URL`() {
        val url = "git@github.com:user/repository.git"
        val result = GitUtils.parse(url)

        assertNotNull(result)
        assertEquals("github.com", result!!.domain)
        assertEquals("user/repository", result.path)
    }

    @Test
    fun `test parse HTTPS GitLab URL`() {
        val url = "https://gitlab.com/group/subgroup/project.git"
        val result = GitUtils.parse(url)

        assertNotNull(result)
        assertEquals("gitlab.com", result!!.domain)
        assertEquals("group/subgroup/project", result.path)
    }

    @Test
    fun `test parse SSH GitLab URL`() {
        val url = "git@gitlab.com:group/project.git"
        val result = GitUtils.parse(url)

        assertNotNull(result)
        assertEquals("gitlab.com", result!!.domain)
        assertEquals("group/project", result.path)
    }

    @Test
    fun `test parse custom domain HTTPS URL`() {
        val url = "https://git.company.com/team/project.git"
        val result = GitUtils.parse(url)

        assertNotNull(result)
        assertEquals("git.company.com", result!!.domain)
        assertEquals("team/project", result.path)
    }

    @Test
    fun `test parse custom domain SSH URL`() {
        val url = "git@git.company.com:team/project.git"
        val result = GitUtils.parse(url)

        assertNotNull(result)
        assertEquals("git.company.com", result!!.domain)
        assertEquals("team/project", result.path)
    }

    @Test
    fun `test parse URL without git extension`() {
        val url = "https://github.com/user/repository"
        val result = GitUtils.parse(url)

        assertNotNull(result)
        assertEquals("github.com", result!!.domain)
        assertEquals("user/repository", result.path)
    }

    @Test
    fun `test parse SSH URL without git extension`() {
        val url = "git@github.com:user/repository"
        val result = GitUtils.parse(url)

        assertNotNull(result)
        assertEquals("github.com", result!!.domain)
        assertEquals("user/repository", result.path)
    }

    @Test
    fun `test parse HTTPS URL with port`() {
        val url = "https://git.company.com:8080/team/project.git"
        val result = GitUtils.parse(url)

        assertNotNull(result)
        assertEquals("git.company.com", result!!.domain)
        assertEquals("team/project", result.path)
    }

    @Test
    fun `test parse SSH URL with port`() {
        val url = "ssh://git@git.company.com:2222/team/project.git"
        val result = GitUtils.parse(url)

        assertNotNull(result)
        assertEquals("git.company.com", result!!.domain)
        assertEquals("team/project", result.path)
    }

    @Test
    fun `test parse returns null for invalid URL`() {
        val url = "invalid-url"
        val result = GitUtils.parse(url)

        assertNull(result)
    }

    @Test
    fun `test parse returns null for empty URL`() {
        val url = ""
        val result = GitUtils.parse(url)

        assertNull(result)
    }

    @Test
    fun `test parse returns null for null URL`() {
        val result = GitUtils.parse(null)

        assertNull(result)
    }

    @Test
    fun `test parse handles URL with username in HTTPS`() {
        val url = "https://username@github.com/user/repository.git"
        val result = GitUtils.parse(url)

        assertNotNull(result)
        assertEquals("github.com", result!!.domain)
        assertEquals("user/repository", result.path)
    }

    @Test
    fun `test parse handles nested group paths`() {
        val url = "https://gitlab.com/group/subgroup/sub-subgroup/project.git"
        val result = GitUtils.parse(url)

        assertNotNull(result)
        assertEquals("gitlab.com", result!!.domain)
        assertEquals("group/subgroup/sub-subgroup/project", result.path)
    }

    @Test
    fun `test parse handles URL with query parameters`() {
        val url = "https://github.com/user/repository.git?ref=main"
        val result = GitUtils.parse(url)

        assertNotNull(result)
        assertEquals("github.com", result!!.domain)
        assertEquals("user/repository", result.path)
    }

    @Test
    fun `test parse handles URL with fragment`() {
        val url = "https://github.com/user/repository.git#readme"
        val result = GitUtils.parse(url)

        assertNotNull(result)
        assertEquals("github.com", result!!.domain)
        assertEquals("user/repository", result.path)
    }

    @Test
    fun `test ParsedGitUrl equality`() {
        val url1 = GitUtils.ParsedGitUrl("github.com", "user/repo")
        val url2 = GitUtils.ParsedGitUrl("github.com", "user/repo")
        val url3 = GitUtils.ParsedGitUrl("gitlab.com", "user/repo")

        assertEquals(url1, url2)
        assertNotEquals(url1, url3)
    }

    @Test
    fun `test ParsedGitUrl toString`() {
        val url = GitUtils.ParsedGitUrl("github.com", "user/repo")
        val expected = "ParsedGitUrl(domain=github.com, path=user/repo)"

        assertEquals(expected, url.toString())
    }
}
