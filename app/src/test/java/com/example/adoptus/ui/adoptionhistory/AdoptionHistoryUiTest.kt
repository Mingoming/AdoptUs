package com.example.adoptus.ui.adoptionhistory

import org.junit.Assert.assertEquals
import org.junit.Test

class AdoptionHistoryUiTest {

    @Test
    fun statusMessage_describesPendingApplication() {
        assertEquals(
            "Menunggu keputusan pemilik hewan",
            AdoptionHistoryUi.statusMessage("pending")
        )
    }

    @Test
    fun statusMessage_describesApprovedApplication() {
        assertEquals(
            "Pengajuan disetujui",
            AdoptionHistoryUi.statusMessage("approved")
        )
    }

    @Test
    fun statusMessage_describesRejectedApplication() {
        assertEquals(
            "Pengajuan ditolak",
            AdoptionHistoryUi.statusMessage("rejected")
        )
    }

    @Test
    fun statusLabel_formatsUnknownStatus() {
        assertEquals("CANCELLED", AdoptionHistoryUi.statusLabel("cancelled"))
    }
}
