import 'package:plugin_platform_interface/plugin_platform_interface.dart';
import 'contacts_bridge_method_channel.dart';

abstract class ContactsBridgePlatform extends PlatformInterface {
  ContactsBridgePlatform() : super(token: _token);

  static final Object _token = Object();

  static ContactsBridgePlatform _instance = MethodChannelContactsBridge();

  static ContactsBridgePlatform get instance => _instance;

  static set instance(ContactsBridgePlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<List<Map<String, String>>> getContacts();

  Future<void> addContact(String name, String phone);

  Future<void> updateContact(String id, String name, String phone);

  Future<void> deleteContact(String id);
}
