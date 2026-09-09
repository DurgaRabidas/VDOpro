package com.vdopro.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TimelineStateTest {

    @Test
    fun testTotalDurationCalculation() {
        val clip1 = VideoClip(id = "1", filePath = "path1", name = "Clip 1", sourceDurationMs = 10000L, speed = 1.0f)
        val clip2 = VideoClip(id = "2", filePath = "path2", name = "Clip 2", sourceDurationMs = 6000L, speed = 2.0f)
        val project = VideoProject(name = "Test Project", videoClips = listOf(clip1, clip2))

        assertEquals(13000L, project.totalDurationMs)
    }

    @Test
    fun testSplitClipAt() {
        val clip = VideoClip(id = "c1", filePath = "path", name = "Clip", sourceDurationMs = 10000L)
        val project = VideoProject(name = "Test", videoClips = listOf(clip))
        val state = TimelineState(project = project, selectedClipId = "c1")

        val newState = state.splitClipAt("c1", splitOffsetInClipMs = 4000L)

        assertEquals(2, newState.project.videoClips.size)
        val firstClip = newState.project.videoClips[0]
        val secondClip = newState.project.videoClips[1]

        assertEquals(0L, firstClip.startInSourceMs)
        assertEquals(4000L, firstClip.endInSourceMs)

        assertEquals(4000L, secondClip.startInSourceMs)
        assertEquals(10000L, secondClip.endInSourceMs)
    }

    @Test
    fun testReorderClips() {
        val clip1 = VideoClip(id = "c1", filePath = "p1", name = "C1", sourceDurationMs = 5000L)
        val clip2 = VideoClip(id = "c2", filePath = "p2", name = "C2", sourceDurationMs = 5000L)
        val project = VideoProject(name = "Test", videoClips = listOf(clip1, clip2))
        val state = TimelineState(project = project)

        val newState = state.reorderClips(0, 1)

        assertEquals("c2", newState.project.videoClips[0].id)
        assertEquals("c1", newState.project.videoClips[1].id)
    }

    @Test
    fun testRemoveClip() {
        val clip1 = VideoClip(id = "c1", filePath = "p1", name = "C1", sourceDurationMs = 5000L)
        val clip2 = VideoClip(id = "c2", filePath = "p2", name = "C2", sourceDurationMs = 5000L)
        val project = VideoProject(name = "Test", videoClips = listOf(clip1, clip2))
        val state = TimelineState(project = project, selectedClipId = "c1")

        val newState = state.removeClip("c1")

        assertEquals(1, newState.project.videoClips.size)
        assertEquals("c2", newState.project.videoClips[0].id)
        assertNull(newState.selectedClipId)
    }

    @Test
    fun testGetClipAtTimestamp() {
        val clip1 = VideoClip(id = "c1", filePath = "p1", name = "C1", sourceDurationMs = 5000L)
        val clip2 = VideoClip(id = "c2", filePath = "p2", name = "C2", sourceDurationMs = 5000L)
        val project = VideoProject(name = "Test", videoClips = listOf(clip1, clip2))

        val result1 = project.getClipAtTimestamp(2500L)
        assertNotNull(result1)
        assertEquals("c1", result1?.first?.id)
        assertEquals(2500L, result1?.second)

        val result2 = project.getClipAtTimestamp(7000L)
        assertNotNull(result2)
        assertEquals("c2", result2?.first?.id)
        assertEquals(2000L, result2?.second)
    }
}
