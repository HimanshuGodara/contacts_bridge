package com.example.contacts_bridge

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.ContentValues
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.ContactsContract
import android.util.Log
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import java.util.ArrayList
import java.util.HashMap

class ContactsBridgePlugin : FlutterPlugin, MethodCallHandler, ActivityAware {
        private lateinit var channel: MethodChannel
        private var activity: Activity? = null
        private val CONTACTS_PERMISSION_CODE = 123

        override fun onAttachedToEngine(
                @NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding
        ) {
                channel = MethodChannel(flutterPluginBinding.binaryMessenger, "contacts_bridge")
                channel.setMethodCallHandler(this)
        }

        override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
                channel.setMethodCallHandler(null)
        }

        override fun onAttachedToActivity(binding: ActivityPluginBinding) {
                activity = binding.activity
        }

        override fun onDetachedFromActivityForConfigChanges() {
                activity = null
        }

        override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
                activity = binding.activity
        }

        override fun onDetachedFromActivity() {
                activity = null
        }

        override fun onMethodCall(call: MethodCall, result: Result) {
                when (call.method) {
                        "getContacts" -> getContacts(result)
                        "addContact" -> addContact(call, result)
                        "updateContact" -> updateContact(call, result)
                        "deleteContact" -> deleteContact(call, result)
                        else -> result.notImplemented()
                }
        }

        private fun getContacts(result: Result) {
                Log.d("ContactsBridgePlugin", "getContacts: activity = $activity")
                val contactsMap = HashMap<String, MutableMap<String, Any>>()
                val resolver: ContentResolver? = activity?.contentResolver

                if (activity == null) {
                        result.error("ERROR", "Activity is null", null)
                        return
                }

                if (ContextCompat.checkSelfPermission(
                                activity!!,
                                Manifest.permission.READ_CONTACTS
                        ) != PackageManager.PERMISSION_GRANTED
                ) {
                        ActivityCompat.requestPermissions(
                                activity!!,
                                arrayOf(Manifest.permission.READ_CONTACTS),
                                CONTACTS_PERMISSION_CODE
                        )
                        result.error(
                                "PERMISSION_DENIED",
                                "Read contacts permission not granted",
                                null
                        )
                        return
                }

                resolver?.let {
                        var cursor: Cursor? = null
                        try {
                                cursor =
                                        it.query(
                                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                                arrayOf(
                                                        ContactsContract.CommonDataKinds.Phone
                                                                .CONTACT_ID,
                                                        ContactsContract.CommonDataKinds.Phone
                                                                .DISPLAY_NAME,
                                                        ContactsContract.CommonDataKinds.Phone
                                                                .NUMBER
                                                ),
                                                null,
                                                null,
                                                null
                                        )

                                if (cursor == null) {
                                        Log.e(
                                                "ContactsBridgePlugin",
                                                "getContacts: Cursor is null after query"
                                        )
                                        result.error("ERROR", "Failed to query contacts", null)
                                        return
                                }

                                while (cursor.moveToNext()) {
                                        try {
                                                val id =
                                                        cursor.getString(
                                                                cursor.getColumnIndexOrThrow(
                                                                        ContactsContract
                                                                                .CommonDataKinds
                                                                                .Phone.CONTACT_ID
                                                                )
                                                        )
                                                val name =
                                                        cursor.getString(
                                                                cursor.getColumnIndexOrThrow(
                                                                        ContactsContract
                                                                                .CommonDataKinds
                                                                                .Phone.DISPLAY_NAME
                                                                )
                                                        )
                                                val phone =
                                                        cursor.getString(
                                                                cursor.getColumnIndexOrThrow(
                                                                        ContactsContract
                                                                                .CommonDataKinds
                                                                                .Phone.NUMBER
                                                                )
                                                        )

                                                if (!contactsMap.containsKey(id)) {
                                                        contactsMap[id] =
                                                                HashMap<String, Any>().apply {
                                                                        put("id", id)
                                                                        put("name", name)
                                                                        put(
                                                                                "phones",
                                                                                ArrayList<String>()
                                                                        )
                                                                }
                                                }
                                                (contactsMap[id]?.get("phones") as
                                                                ArrayList<String>)
                                                        .add(phone)
                                        } catch (e: Exception) {
                                                Log.e(
                                                        "ContactsBridgePlugin",
                                                        "Error processing contact: ${e.localizedMessage}",
                                                        e
                                                )
                                                // Skip this contact and continue to the next
                                        }
                                }
                        } catch (e: Exception) {
                                Log.e(
                                        "ContactsBridgePlugin",
                                        "Error querying contacts: ${e.localizedMessage}",
                                        e
                                )
                                result.error(
                                        "ERROR",
                                        "Failed to fetch contacts from device: ${e.localizedMessage}",
                                        null
                                )
                        } finally {
                                cursor?.close()
                        }

                        val contactsList = ArrayList(contactsMap.values)
                        result.success(contactsList)
                }
                        ?: run { result.error("ERROR", "Content Resolver is null", null) }
        }

        private fun addContact(call: MethodCall, result: Result) {
                val name = call.argument<String>("name")
                val phone = call.argument<String>("phone")
                val resolver: ContentResolver? = activity?.contentResolver

                if (activity == null) {
                        result.error("ERROR", "Activity is null", null)
                        return
                }

                if (name == null) {
                        result.error("ERROR", "Missing argument: name", null)
                        return
                }

                if (phone == null) {
                        result.error("ERROR", "Missing argument: phone", null)
                        return
                }

                resolver?.let {
                        try {
                                val values =
                                        ContentValues().apply {
                                                put(
                                                        ContactsContract.RawContacts.ACCOUNT_TYPE,
                                                        null as String?
                                                )
                                                put(
                                                        ContactsContract.RawContacts.ACCOUNT_NAME,
                                                        null as String?
                                                )
                                        }

                                val rawContactUri =
                                        it.insert(ContactsContract.RawContacts.CONTENT_URI, values)
                                val rawContactId =
                                        rawContactUri?.lastPathSegment?.toLong()
                                                ?: run {
                                                        result.error(
                                                                "ERROR",
                                                                "Failed to get raw contact ID",
                                                                null
                                                        )
                                                        return
                                                }

                                // Insert name
                                val nameValues =
                                        ContentValues().apply {
                                                put(
                                                        ContactsContract.Data.RAW_CONTACT_ID,
                                                        rawContactId
                                                )
                                                put(
                                                        ContactsContract.Data.MIMETYPE,
                                                        ContactsContract.CommonDataKinds
                                                                .StructuredName.CONTENT_ITEM_TYPE
                                                )
                                                put(
                                                        ContactsContract.CommonDataKinds
                                                                .StructuredName.DISPLAY_NAME,
                                                        name
                                                )
                                        }
                                it.insert(ContactsContract.Data.CONTENT_URI, nameValues)

                                // Insert phone number
                                val phoneValues =
                                        ContentValues().apply {
                                                put(
                                                        ContactsContract.Data.RAW_CONTACT_ID,
                                                        rawContactId
                                                )
                                                put(
                                                        ContactsContract.Data.MIMETYPE,
                                                        ContactsContract.CommonDataKinds.Phone
                                                                .CONTENT_ITEM_TYPE
                                                )
                                                put(
                                                        ContactsContract.CommonDataKinds.Phone
                                                                .NUMBER,
                                                        phone
                                                )
                                                put(
                                                        ContactsContract.CommonDataKinds.Phone.TYPE,
                                                        ContactsContract.CommonDataKinds.Phone
                                                                .TYPE_MOBILE
                                                )
                                        }
                                it.insert(ContactsContract.Data.CONTENT_URI, phoneValues)

                                result.success(true)
                        } catch (e: Exception) {
                                Log.e(
                                        "ContactsBridgePlugin",
                                        "Error adding contact: ${e.localizedMessage}",
                                        e
                                )
                                result.error(
                                        "ERROR",
                                        "Failed to add contact: ${e.localizedMessage}",
                                        null
                                )
                        }
                }
                        ?: run { result.error("ERROR", "Content Resolver is null", null) }
        }

        private fun deleteContact(call: MethodCall, result: Result) {
                val id = call.argument<String>("id")
                val resolver: ContentResolver? = activity?.contentResolver

                if (activity == null) {
                        result.error("ERROR", "Activity is null", null)
                        return
                }

                if (id == null) {
                        result.error("ERROR", "Missing argument: id", null)
                        return
                }

                resolver?.let {
                        try {
                                val rowsDeleted =
                                        it.delete(
                                                ContactsContract.RawContacts.CONTENT_URI,
                                                ContactsContract.RawContacts.CONTACT_ID + " = ?",
                                                arrayOf(id)
                                        )
                                result.success(rowsDeleted > 0)
                        } catch (e: Exception) {
                                Log.e(
                                        "ContactsBridgePlugin",
                                        "Error deleting contact: ${e.localizedMessage}",
                                        e
                                )
                                result.error(
                                        "ERROR",
                                        "Failed to delete contact: ${e.localizedMessage}",
                                        null
                                )
                        }
                }
                        ?: run { result.error("ERROR", "Content Resolver is null", null) }
        }

        private fun updateContact(call: MethodCall, result: Result) {
                val id = call.argument<String>("id")
                val newName = call.argument<String>("name")
                val newPhone = call.argument<String>("phone")
                val resolver: ContentResolver? = activity?.contentResolver

                if (activity == null) {
                        result.error("ERROR", "Activity is null", null)
                        return
                }

                if (id == null) {
                        result.error("ERROR", "Missing argument: id", null)
                        return
                }

                resolver?.let {
                        try {
                                var rowsUpdated = 0

                                if (!newName.isNullOrBlank()) {
                                        val nameValues =
                                                ContentValues().apply {
                                                        put(
                                                                ContactsContract.CommonDataKinds
                                                                        .StructuredName
                                                                        .DISPLAY_NAME,
                                                                newName
                                                        )
                                                }
                                        val updatedNameRows =
                                                it.update(
                                                        ContactsContract.Data.CONTENT_URI,
                                                        nameValues,
                                                        "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                                                        arrayOf(
                                                                id,
                                                                ContactsContract.CommonDataKinds
                                                                        .StructuredName
                                                                        .CONTENT_ITEM_TYPE
                                                        )
                                                )
                                        rowsUpdated += updatedNameRows
                                }

                                if (!newPhone.isNullOrBlank()) {
                                        val phoneValues =
                                                ContentValues().apply {
                                                        put(
                                                                ContactsContract.CommonDataKinds
                                                                        .Phone.NUMBER,
                                                                newPhone
                                                        )
                                                }
                                        val updatedPhoneRows =
                                                it.update(
                                                        ContactsContract.Data.CONTENT_URI,
                                                        phoneValues,
                                                        "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                                                        arrayOf(
                                                                id,
                                                                ContactsContract.CommonDataKinds
                                                                        .Phone.CONTENT_ITEM_TYPE
                                                        )
                                                )
                                        rowsUpdated += updatedPhoneRows
                                }

                                result.success(rowsUpdated > 0)
                        } catch (e: Exception) {
                                Log.e(
                                        "ContactsBridgePlugin",
                                        "Error updating contact: ${e.localizedMessage}",
                                        e
                                )
                                result.error(
                                        "ERROR",
                                        "Failed to update contact: ${e.localizedMessage}",
                                        null
                                )
                        }
                }
                        ?: run { result.error("ERROR", "Content Resolver is null", null) }
        }
}
