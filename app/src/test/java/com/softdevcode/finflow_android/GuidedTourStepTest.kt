package com.softdevcode.finflow_android

import com.softdevcode.finflow_android.ui.tutorial.GuidedTourStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GuidedTourStepTest {
    @Test fun guidesRealCreationActions() {
        assertEquals(GuidedTourStep.TYPE, GuidedTourStep.MOVEMENT.next())
        assertEquals("add_transaction", GuidedTourStep.SAVE.destination)
        assertEquals("goals", GuidedTourStep.NEW_GOAL.destination)
        assertEquals("create_goal", GuidedTourStep.GOAL_AMOUNT.destination)
        assertEquals("module/accounts", GuidedTourStep.NEW_ACCOUNT.destination)
        assertEquals("module/categories", GuidedTourStep.NEW_CATEGORY.destination)
        assertNull(GuidedTourStep.NEW_CATEGORY.next())
        assertEquals(GuidedTourStep.values().size, GuidedTourStep.total)
    }
}
