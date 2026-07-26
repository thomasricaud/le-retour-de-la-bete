package fr.leretourdelabete.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubReleaseUpdateCheckerTest {
    @Test
    fun `detects a newer semantic version`() {
        assertTrue(isVersionNewer("v0.3.0", "0.2.0"))
        assertTrue(isVersionNewer("v1.0.0", "0.99.99"))
        assertTrue(isVersionNewer("v2.4.7", "0.2.0"))
        assertTrue(isVersionNewer("0.10.0", "0.9.9"))
    }

    @Test
    fun `does not offer the installed or an older version`() {
        assertFalse(isVersionNewer("v0.2.0", "0.2.0"))
        assertFalse(isVersionNewer("v0.1.9", "0.2.0"))
        assertFalse(isVersionNewer("1.0", "1.0.0"))
    }

    @Test
    fun `ignores malformed release tags`() {
        assertFalse(isVersionNewer("latest", "0.2.0"))
        assertFalse(isVersionNewer("", "0.2.0"))
    }
}
