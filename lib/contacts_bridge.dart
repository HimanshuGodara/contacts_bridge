import 'contacts_bridge_platform_interface.dart';

class ContactsBridge {
  static Future<List<Map<String, Object>>> getContacts() {
    return ContactsBridgePlatform.instance.getContacts();
  }

  static Future<void> addContact(String name, String phone) {
    return ContactsBridgePlatform.instance.addContact(name, phone);
  }

  static Future<void> updateContact(String id, String name, String phone) {
    return ContactsBridgePlatform.instance.updateContact(id, name, phone);
  }

  static Future<void> deleteContact(String id) {
    return ContactsBridgePlatform.instance.deleteContact(id);
  }
}
