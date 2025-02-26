package com.example.contacts_bridge

import android.app.Activity
import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import android.provider.ContactsContract
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler

class ContactsBridgePlugin : FlutterPlugin, MethodCallHandler, ActivityAware {
  private lateinit var channel: MethodChannel
  private var activity: Activity? = null

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

  override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
    when (call.method) {
      "getContacts" -> getContacts(result)
      "addContact" -> addContact(call, result)
      "updateContact" -> updateContact(call, result)
      "deleteContact" -> deleteContact(call, result)
      else -> result.notImplemented()
    }
  }

  private fun getContacts(result: MethodChannel.Result) {
    val contactsList = mutableListOf<Map<String, String>>()
    val resolver: ContentResolver? = activity?.contentResolver
    val cursor: Cursor? =
            resolver?.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                    ),
                    null,
                    null,
                    null
            )

    cursor?.use {
      while (it.moveToNext()) {
        val name =
                it.getString(
                        it.getColumnIndexOrThrow(
                                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                        )
                )
        val phone =
                it.getString(
                        it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                )
        contactsList.add(mapOf("name" to name, "phone" to phone))
      }
    }

    result.success(contactsList)
  }

  private fun addContact(call: MethodCall, result: MethodChannel.Result) {
    val name = call.argument<String>("name") ?: return
    val phone = call.argument<String>("phone") ?: return
    val resolver: ContentResolver? = activity?.contentResolver

    val values =
            ContentValues().apply {
              put(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
              put(ContactsContract.RawContacts.ACCOUNT_NAME, null)
            }

    val rawContactUri = resolver?.insert(ContactsContract.RawContacts.CONTENT_URI, values)
    val rawContactId = rawContactUri?.lastPathSegment?.toLong() ?: return

    values.clear()
    values.apply {
      put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
      put(
              ContactsContract.Data.MIMETYPE,
              ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
      )
      put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
    }
    resolver.insert(ContactsContract.Data.CONTENT_URI, values)

    values.clear()
    values.apply {
      put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
      put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
      put(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
      put(
              ContactsContract.CommonDataKinds.Phone.TYPE,
              ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
      )
    }
    resolver.insert(ContactsContract.Data.CONTENT_URI, values)

    result.success(null)
  }

  private fun deleteContact(call: MethodCall, result: MethodChannel.Result) {
    val id = call.argument<String>("id") ?: return
    val resolver: ContentResolver? = activity?.contentResolver
    val rowsDeleted =
            resolver?.delete(
                    ContactsContract.RawContacts.CONTENT_URI,
                    ContactsContract.RawContacts.CONTACT_ID + " = ?",
                    arrayOf(id)
            )

    result.success(rowsDeleted ?: 0)
  }

  private fun updateContact(call: MethodCall, result: MethodChannel.Result) {
    // Implement update logic similar to deleteContact and addContact
    result.notImplemented()
  }
}
