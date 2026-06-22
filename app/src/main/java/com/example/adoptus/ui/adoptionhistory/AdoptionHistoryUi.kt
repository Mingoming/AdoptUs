package com.example.adoptus.ui.adoptionhistory

object AdoptionHistoryUi {

    fun statusLabel(status: String): String {
        return status.ifBlank { "unknown" }.uppercase()
    }

    fun statusMessage(status: String): String {
        return when (status.lowercase()) {
            "pending" -> "Menunggu keputusan pemilik hewan"
            "approved" -> "Pengajuan disetujui"
            "rejected" -> "Pengajuan ditolak"
            "cancelled" -> "Pengajuan dibatalkan"
            else -> "Status pengajuan belum diketahui"
        }
    }
}
