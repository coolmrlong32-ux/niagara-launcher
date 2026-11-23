package com.niagara.launcher

import com.niagara.launcher.core.data.AppRepository
import com.niagara.launcher.core.model.AppItem
import org.junit.Assert.assertEquals
import org.junit.Test

class AppRepositoryDeltaTest {

    private fun app(
        pkg: String,
        activity: String = ".MainActivity",
        label: String,
        firstLetter: Char
    ) = AppItem(
        packageName = pkg,
        activityName = activity,
        label = label,
        firstLetter = firstLetter
    )

    @Test
    fun testPackageAddedInsertsInSortedOrder() {
        val initial = listOf(
            app("com.alpha", label = "Alpha", firstLetter = 'A'),
            app("com.zeta", label = "Zeta", firstLetter = 'Z')
        )
        val added = listOf(
            app("com.mid", label = "MidApp", firstLetter = 'M')
        )

        val result = AppRepository.applyPackageDelta(initial, "com.mid", added)

        assertEquals(listOf("Alpha", "MidApp", "Zeta"), result.map { it.label })
    }

    @Test
    fun testPackageRemovedEliminatesAllActivitiesOfPackage() {
        val initial = listOf(
            app("com.alpha", ".Main", label = "Alpha", firstLetter = 'A'),
            app("com.multi", ".Act1", label = "Multi One", firstLetter = 'M'),
            app("com.multi", ".Act2", label = "Multi Two", firstLetter = 'M'),
            app("com.zeta", ".Main", label = "Zeta", firstLetter = 'Z')
        )

        val result = AppRepository.applyPackageDelta(initial, "com.multi", emptyList())

        assertEquals(listOf("Alpha", "Zeta"), result.map { it.label })
    }

    @Test
    fun testPackageUpdatedReplacesOldEntriesAndResorts() {
        val initial = listOf(
            app("com.alpha", ".Main", label = "Alpha", firstLetter = 'A'),
            app("com.target", ".OldAct", label = "Beta", firstLetter = 'B'),
            app("com.zeta", ".Main", label = "Zeta", firstLetter = 'Z')
        )
        val updated = listOf(
            app("com.target", ".NewAct", label = "Omega", firstLetter = 'O')
        )

        val result = AppRepository.applyPackageDelta(initial, "com.target", updated)

        assertEquals(listOf("Alpha", "Omega", "Zeta"), result.map { it.label })
        assertEquals(".NewAct", result[1].activityName)
    }

    @Test
    fun testRemovingUnknownPackageKeepsListUnchanged() {
        val initial = listOf(
            app("com.alpha", label = "Alpha", firstLetter = 'A'),
            app("com.zeta", label = "Zeta", firstLetter = 'Z')
        )

        val result = AppRepository.applyPackageDelta(initial, "com.nonexistent", emptyList())

        assertEquals(initial, result)
    }
}
