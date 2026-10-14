package com.hardlinecoding.checkthebox.data

import android.accounts.Account
import android.content.Context
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.File as JavaFile

class GoogleDriveSyncService(private val drive: Drive) {

    companion object {
        fun createDriveService(context: Context, accountEmail: String): Drive {
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                setOf(DriveScopes.DRIVE_APPDATA)
            ).setSelectedAccount(Account(accountEmail, "com.google"))

            @Suppress("DEPRECATION")
            return Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                GsonFactory(),
                credential
            ).setApplicationName("Mnemosyne").build()
        }
    }

    suspend fun uploadFile(localFile: JavaFile, fileName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val existingFile = findFileByName(fileName)
            val mediaContent = FileContent("application/octet-stream", localFile)
            
            if (existingFile != null) {
                val fileMetadata = File().apply {
                    modifiedTime = com.google.api.client.util.DateTime(localFile.lastModified())
                }
                drive.files().update(existingFile.id, fileMetadata, mediaContent).execute()
            } else {
                val fileMetadata = File().apply {
                    name = fileName
                    parents = listOf("appDataFolder")
                    modifiedTime = com.google.api.client.util.DateTime(localFile.lastModified())
                }
                drive.files().create(fileMetadata, mediaContent).execute()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun downloadFile(localFile: JavaFile, fileName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val existingFile = findFileByName(fileName)
            if (existingFile != null) {
                FileOutputStream(localFile).use { outputStream ->
                    drive.files().get(existingFile.id).executeMediaAndDownloadTo(outputStream)
                }
                localFile.setLastModified(existingFile.modifiedTime.value)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun findFileByName(fileName: String): File? {
        val result = drive.files().list()
            .setSpaces("appDataFolder")
            .setQ("name = '$fileName'")
            .setFields("files(id, name, modifiedTime)")
            .execute()
        return result.files?.firstOrNull()
    }
}
