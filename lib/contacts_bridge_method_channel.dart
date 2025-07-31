import 'dart:async';
import 'package:flutter/services.dart';
import 'contacts_bridge_platform_interface.dart';

class MethodChannelContactsBridge extends ContactsBridgePlatform {
  static const MethodChannel _channel = MethodChannel('contacts_bridge');

  @override
  Future<List<Map<String, dynamic>>> getContacts() async {
    final List<dynamic> contacts = await _channel.invokeMethod('getContacts');
    return contacts.map((e) => Map<String, dynamic>.from(e)).toList();
  }

  @override
  Future<void> addContact(String name, String phone) async {
    await _channel.invokeMethod('addContact', {'name': name, 'phone': phone});
  }

  @override
  Future<void> updateContact(String id, String name, String phone) async {
    await _channel.invokeMethod('updateContact', {
      'id': id,
      'name': name,
      'phone': phone,
    });
  }

  @override
  Future<void> deleteContact(String id) async {
    await _channel.invokeMethod('deleteContact', {'id': id});
  }
}
