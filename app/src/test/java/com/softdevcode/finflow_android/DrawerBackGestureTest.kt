package com.softdevcode.finflow_android

import androidx.activity.BackEventCompat
import com.softdevcode.finflow_android.ui.screens.main.opensDrawerFromBackEdge
import org.junit.Assert.*
import org.junit.Test

class DrawerBackGestureTest {
    @Test fun leftEdgeOpensMenuWithoutPoppingTheRoute() {
        assertTrue(opensDrawerFromBackEdge(BackEventCompat.EDGE_LEFT))
    }
    @Test fun rightEdgeAndPhysicalBackRemainNavigationActions() {
        assertFalse(opensDrawerFromBackEdge(BackEventCompat.EDGE_RIGHT))
        assertFalse(opensDrawerFromBackEdge(null))
    }
}
