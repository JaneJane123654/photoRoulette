package com.example.photoroulette.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IntentHelperTest {

    @Test
    fun `buildRelativePathSegments returns nested segments when media is inside granted tree`() {
        val result = IntentHelper.buildRelativePathSegments(
            treeDocumentId = "primary:DCIM",
            mediaRelativePath = "DCIM/Camera/",
        )

        assertEquals(listOf("Camera"), result)
    }

    @Test
    fun `buildRelativePathSegments resolves screenshots inside DCIM root`() {
        val result = IntentHelper.buildRelativePathSegments(
            treeDocumentId = "primary:DCIM",
            mediaRelativePath = "DCIM/Screenshots/",
        )

        assertEquals(listOf("Screenshots"), result)
    }

    @Test
    fun `buildRelativePathSegments resolves screenshots inside Pictures root`() {
        val result = IntentHelper.buildRelativePathSegments(
            treeDocumentId = "primary:Pictures",
            mediaRelativePath = "Pictures/Screenshots/",
        )

        assertEquals(listOf("Screenshots"), result)
    }

    @Test
    fun `buildRelativePathSegments returns empty list when media is in granted root`() {
        val result = IntentHelper.buildRelativePathSegments(
            treeDocumentId = "primary:DCIM/Camera",
            mediaRelativePath = "DCIM/Camera/",
        )

        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `buildRelativePathSegments returns null when media is outside granted tree`() {
        val result = IntentHelper.buildRelativePathSegments(
            treeDocumentId = "primary:Pictures",
            mediaRelativePath = "DCIM/Camera/",
        )

        assertNull(result)
    }

    @Test
    fun `normalizeDirectoryPath trims separators and blanks`() {
        assertEquals("DCIM/Camera", IntentHelper.normalizeDirectoryPath(" /DCIM/Camera/ "))
        assertNull(IntentHelper.normalizeDirectoryPath("   "))
        assertNull(IntentHelper.normalizeDirectoryPath(null))
    }
}
