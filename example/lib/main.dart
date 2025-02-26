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
  List<Map<String, Object>> _contacts = [];
  String _error = '';

  @override
  void initState() {
    super.initState();
    requestPermissionAndFetchContacts();
  }

  Future<void> requestPermissionAndFetchContacts() async {
    PermissionStatus status = await Permission.contacts.request();
    debugPrint("Permission status: ${status.name}");
    if (status.isGranted) {
      fetchContacts();
    } else if (status.isPermanentlyDenied) {
      debugPrint("Permission permanently denied. Opening settings...");
      openAppSettings();
    } else {
      setState(() {
        _error = 'Contacts permission denied. Please enable it in settings.';
      });
    }
  }

  Future<void> fetchContacts() async {
    try {
      final t1 = DateTime.now();
      List<Map<String, Object>> contacts = await ContactsBridge.getContacts();
      final t2 = DateTime.now();
      debugPrint(
        'Total time in fetching ${contacts.length} contacts: ${t2.difference(t1).inMilliseconds} ms',
      );

      if (!mounted) return;

      List<Map<String, Object>> processedContacts = [];

      for (var contact in contacts) {
        String name = contact['name'] as String? ?? 'Unknown';
        String id = contact['id'] as String? ?? '';
        List<dynamic> phones = contact['phones'] as List<dynamic>? ?? [];

        for (var phone in phones) {
          processedContacts.add({
            'id': id,
            'name': name,
            'phone': phone.toString(),
          });
        }
      }

      debugPrint(
        'Processed contacts length before setState: ${processedContacts.length}',
      );
      processedContacts.sort(
        (a, b) => (a["name"] as String).compareTo(b["name"] as String),
      );

      if (mounted) {
        setState(() {
          _contacts = processedContacts;
          debugPrint('SetState called, contacts length: ${_contacts.length}');
        });
      }
    } catch (e, stackTrace) {
      debugPrint("Error fetching contacts: $e\n$stackTrace");
      setState(() {
        _error = 'Error fetching contacts: $e';
      });
    }
  }

  Future<void> addContact() async {
    try {
      await ContactsBridge.addContact("Alice Doe", "9876543210");
      fetchContacts();
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
              onPressed: requestPermissionAndFetchContacts,
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
                  key: ValueKey(_contacts.length), // Ensures proper rebuilds
                  itemCount: _contacts.length,
                  itemBuilder: (context, index) {
                    final contact = _contacts[index];
                    return ListTile(
                      title: Text(contact['name'] as String? ?? 'Unknown'),
                      subtitle: Text(
                        contact['phone'] as String? ?? 'No number',
                      ),
                      trailing: IconButton(
                        icon: const Icon(Icons.delete, color: Colors.red),
                        onPressed:
                            () => deleteContact(contact['id'] as String? ?? ''),
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
