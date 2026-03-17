package com.example.rgm

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast

object SafetyUtils {

    /**
     * Simulates a content safety scan.
     * In a real app, this would use an AI model (like TensorFlow Lite) 
     * or a Cloud API to detect NSFW content.
     */
    fun isContentSafe(context: Context, uri: Uri): Boolean {
        val fileName = getFileName(context, uri).lowercase()
        
        // Basic keyword filter for simulation
        val prohibitedKeywords = listOf("adult", "nsfw", "porn", "xxx", "sex", "naked")
        
        for (keyword in prohibitedKeywords) {
            if (fileName.contains(keyword)) {
                Toast.makeText(context, "Adult content detected and blocked.", Toast.LENGTH_LONG).show()
                return false
            }
        }

        // Simulate scanning delay/process
        // Toast.makeText(context, "Scanning content for safety...", Toast.LENGTH_SHORT).show()
        
        return true
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: ""
    }
}
