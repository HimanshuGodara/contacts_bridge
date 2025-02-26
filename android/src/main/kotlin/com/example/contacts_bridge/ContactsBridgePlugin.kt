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

    resolver?.let {
      val cursor: Cursor? =
              it.query(
                      ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                      arrayOf(
                              ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                              ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                              ContactsContract.CommonDataKinds.Phone.NUMBER
                      ),
                      null,
                      null,
                      null
              )

      cursor?.use { cur ->
        while (cur.moveToNext()) {
          val id =
                  cur.getString(
                          cur.getColumnIndexOrThrow(
                                  ContactsContract.CommonDataKinds.Phone.CONTACT_ID
                          )
                  )
          val name =
                  cur.getString(
                          cur.getColumnIndexOrThrow(
                                  ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                          )
                  )
          val phone =
                  cur.getString(
                          cur.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                  )
          contactsList.add(mapOf("id" to id, "name" to name, "phone" to phone))
        }
      }
    }

    result.success(contactsList)
  }

  private fun addContact(call: MethodCall, result: MethodChannel.Result) {
    val name = call.argument<String>("name") ?: return
    val phone = call.argument<String>("phone") ?: return
    val resolver: ContentResolver? = activity?.contentResolver

    resolver?.let {
      val values =
              ContentValues().apply {
                put(ContactsContract.RawContacts.ACCOUNT_TYPE, null as String?)
                put(ContactsContract.RawContacts.ACCOUNT_NAME, null as String?)
              }

      val rawContactUri = it.insert(ContactsContract.RawContacts.CONTENT_URI, values)
      val rawContactId = rawContactUri?.lastPathSegment?.toLong() ?: return

      // Insert name
      val nameValues =
              ContentValues().apply {
                put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                put(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                )
                put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
              }
      it.insert(ContactsContract.Data.CONTENT_URI, nameValues)

      // Insert phone number
      val phoneValues =
              ContentValues().apply {
                put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                put(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                )
                put(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                put(
                        ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                )
              }
      it.insert(ContactsContract.Data.CONTENT_URI, phoneValues)

      result.success(true)
    }
            ?: result.error("ERROR", "Content Resolver is null", null)
  }

  private fun deleteContact(call: MethodCall, result: MethodChannel.Result) {
    val id = call.argument<String>("id") ?: return
    val resolver: ContentResolver? = activity?.contentResolver

    resolver?.let {
      val rowsDeleted =
              it.delete(
                      ContactsContract.RawContacts.CONTENT_URI,
                      ContactsContract.RawContacts.CONTACT_ID + " = ?",
                      arrayOf(id)
              )
      result.success(rowsDeleted > 0)
    }
            ?: result.error("ERROR", "Content Resolver is null", null)
  }

  private fun updateContact(call: MethodCall, result: MethodChannel.Result) {
    val id = call.argument<String>("id") ?: return
    val newName = call.argument<String>("name")
    val newPhone = call.argument<String>("phone")
    val resolver: ContentResolver? = activity?.contentResolver

    resolver?.let {
      if (!newName.isNullOrBlank()) {
        val nameValues =
                ContentValues().apply {
                  put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, newName)
                }
        it.update(
                ContactsContract.Data.CONTENT_URI,
                nameValues,
                "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf(id, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
        )
      }

      if (!newPhone.isNullOrBlank()) {
        val phoneValues =
                ContentValues().apply {
                  put(ContactsContract.CommonDataKinds.Phone.NUMBER, newPhone)
                }
        it.update(
                ContactsContract.Data.CONTENT_URI,
                phoneValues,
                "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf(id, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
        )
      }

      result.success(true)
    }
            ?: result.error("ERROR", "Content Resolver is null", null)
  }
}
