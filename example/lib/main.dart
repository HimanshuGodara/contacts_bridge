import 'package:equatable/equatable.dart';
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
  List<ContactModel> _contacts = [];
  String _error = '';

  @override
  void initState() {
    super.initState();
    requestPermissionAndFetchContacts(context);
  }

  Future<void> requestPermissionAndFetchContacts(BuildContext context) async {
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
      List<Map<String, dynamic>> contacts = await ContactsBridge.getContacts();
      final t2 = DateTime.now();
      debugPrint(
        'Total time in fetching ${contacts.length} contacts: ${t2.difference(t1).inMilliseconds} ms',
      );

      if (!mounted) return;

      final Map<String, ContactModel> uniqueContacts = {};

      for (var contact in contacts) {
        String name = contact['name'] as String? ?? 'Unknown';
        String id = contact['id'] as String? ?? '';
        List<dynamic> phones = contact['phones'] as List<dynamic>? ?? [];

        for (var phone in phones) {
          String normalizedPhone = _normalizePhone(phone.toString());
          if (normalizedPhone.length == 10) {
            uniqueContacts[normalizedPhone] = ContactModel(
              id: id,
              name: name,
              phone: normalizedPhone,
            );
          }
        }
      }

      List<ContactModel> processedContacts =
          uniqueContacts.values.toList()
            ..sort((a, b) => a.name.compareTo(b.name));

      debugPrint('Final contacts length: ${processedContacts.length}');

      if (mounted) {
        setState(() {
          _contacts = processedContacts;
          debugPrint('SetState called, contacts length: ${_contacts.length}');
        });
      }
    } catch (e, stackTrace) {
      debugPrint("Error fetching contacts: $e\n$stackTrace");
      setState(() {
        _error = 'Error fetching contacts: $e\n$stackTrace';
      });
    }
  }

  // **Normalize phone numbers (keep last 10 digits)**
  String _normalizePhone(String phone) {
    String digitsOnly = phone.replaceAll(RegExp(r'\D'), '');
    return digitsOnly.length >= 10
        ? digitsOnly.substring(digitsOnly.length - 10)
        : '';
  }

  Future<void> addContact() async {
    try {
      await ContactsBridge.addContact("Alice Doe", "9876543210");
      fetchContacts();
    } catch (e, st) {
      setState(() {
        _error = 'Error adding contact: $e, stack: $st';
      });
    }
  }

  Future<void> deleteContact(String id) async {
    try {
      await ContactsBridge.deleteContact(id);
      fetchContacts(); // Refresh contacts after deleting
    } catch (e, st) {
      setState(() {
        _error = 'Error deleting contact: $e \n $st';
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
            Builder(
              builder: (context) {
                return IconButton(
                  icon: const Icon(Icons.refresh),
                  onPressed: () {
                    try {
                      requestPermissionAndFetchContacts(context);
                    } catch (e, st) {
                      ScaffoldMessenger.of(context).showMaterialBanner(
                        MaterialBanner(
                          content: Text('error: $e, stack: $st'),
                          actions: [],
                        ),
                      );
                    }
                  },
                );
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
                  key: ValueKey(_contacts.length), // Ensures proper rebuilds
                  itemCount: _contacts.length,
                  itemBuilder: (context, index) {
                    final contact = _contacts[index];
                    return ListTile(
                      title: Text(contact.name as String? ?? 'Unknown'),
                      subtitle: Text(contact.phone as String? ?? 'No number'),
                      trailing: IconButton(
                        icon: const Icon(Icons.delete, color: Colors.red),
                        onPressed:
                            () => deleteContact(contact.id as String? ?? ''),
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

final class ContactModel extends Equatable {
  final String id;
  final String name;
  final String phone;

  const ContactModel({
    required this.id,
    required this.name,
    required this.phone,
  });

  @override
  List<Object?> get props => [id, name, phone];
}
