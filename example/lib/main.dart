import 'package:flutter/material.dart';
import 'package:contacts_bridge/contacts_bridge.dart';
import 'package:permission_handler/permission_handler.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final _contactsBridgePlugin = ContactsBridge();
  List<Map<String, String>> _contacts = [];
  String _error = '';

  @override
  void initState() {
    super.initState();
  }

  Future<void> fetchContacts() async {
    try {
      final t1 = DateTime.now();
      List<Map<String, String>> contacts = await ContactsBridge.getContacts();
      final t2 = DateTime.now();
      debugPrint(
        'total time in fetching ${contacts.length} contacts: ${t2.difference(t1).inMilliseconds}',
      );

      if (!mounted) return;
      setState(() {
        _contacts = contacts;
      });
    } catch (e) {
      setState(() {
        _error = 'Error fetching contacts: $e';
      });
    }
  }

  Future<void> addContact() async {
    try {
      await ContactsBridge.addContact("Alice Doe", "9876543210");
      fetchContacts(); // Refresh contacts after adding
    } catch (e) {
      setState(() {
        _error = 'Error adding contact: $e';
      });
    }
  }

  Future<void> deleteContact(String id) async {
    try {
      await ContactsBridge.deleteContact(id);
      fetchContacts(); // Refresh contacts after deleting
    } catch (e) {
      setState(() {
        _error = 'Error deleting contact: $e';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Contacts Plugin Example'),
          actions: [
            IconButton(
              icon: const Icon(Icons.refresh),
              onPressed: () async {
                // Check permission status
                PermissionStatus permissionStatus =
                    await Permission.contacts.status;

                if (permissionStatus.isDenied ||
                    permissionStatus.isPermanentlyDenied) {
                  // Request permission
                  permissionStatus = await Permission.contacts.request();
                }

                if (permissionStatus.isGranted) {
                  // If permission is granted, fetch contacts
                  fetchContacts();
                } else {
                  // Handle case where permission is denied
                  setState(() {
                    _error =
                        'Contacts permission denied. Please enable it in settings.';
                  });
                }
              },
            ),
          ],
        ),
        body:
            _error.isNotEmpty
                ? Center(
                  child: Text(
                    _error,
                    style: const TextStyle(color: Colors.red),
                  ),
                )
                : _contacts.isEmpty
                ? const Center(child: Text('No contacts found'))
                : ListView.builder(
                  itemCount: _contacts.length,
                  itemBuilder: (context, index) {
                    final contact = _contacts[index];
                    return ListTile(
                      title: Text(contact['name'] ?? 'Unknown'),
                      subtitle: Text(contact['phone'] ?? 'No phone'),
                      trailing: IconButton(
                        icon: const Icon(Icons.delete, color: Colors.red),
                        onPressed: () => deleteContact(contact['id'] ?? ''),
                      ),
                    );
                  },
                ),
        floatingActionButton: FloatingActionButton(
          onPressed: addContact,
          child: const Icon(Icons.add),
        ),
      ),
    );
  }
}
