package com.softdevcode.finflow_android

import com.softdevcode.finflow_android.data.updates.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class AppUpdateDownloadTest {
    private val update = AppUpdate(9, "1.2.6", "https://github.com/LuisJose713/finflow_android/releases/download/v1.2.6/app.apk",
        "Mejoras", sha256 = "a".repeat(64), sizeBytes = 1024)

    @Test fun completeDownloadMustMatchPublishedSizeAndChecksum() {
        verifyDownloadedUpdate(update, 1024, "a".repeat(64), 1024)
        assertThrows(IOException::class.java) { verifyDownloadedUpdate(update, 1024, "b".repeat(64), 1024) }
        assertThrows(IOException::class.java) { verifyDownloadedUpdate(update, 1000, "a".repeat(64), 1024) }
        assertThrows(IOException::class.java) { verifyDownloadedUpdate(update, 0, "a".repeat(64), 0) }
    }

    @Test fun chunkedDownloadsAreCheckedAgainstTheManifest() {
        verifyDownloadedUpdate(update, 1024, "a".repeat(64), -1)
        assertThrows(IOException::class.java) { verifyDownloadedUpdate(update, 1000, "a".repeat(64), -1) }
    }

    @Test fun legacyManifestsStillRequireCompleteAndBoundedFiles() {
        val legacy = update.copy(sizeBytes = null, sha256 = null)
        verifyDownloadedUpdate(legacy, 1024, "b".repeat(64), 1024)
        assertThrows(IOException::class.java) { verifyDownloadedUpdate(legacy, MAX_UPDATE_APK_BYTES + 1, "", -1) }
        assertThrows(IOException::class.java) { verifyDownloadedUpdate(legacy, 1000, "", 1024) }
    }
}
