import Flutter
import UIKit
import Contacts

public class ContactsBridgePlugin: NSObject, FlutterPlugin {
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "contacts_bridge", binaryMessenger: registrar.messenger())
        let instance = ContactsBridgePlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
    }
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        switch call.method {
        case "getContacts":
            getContacts(result: result)
        case "addContact":
            addContact(call: call, result: result)
        case "updateContact":
            updateContact(call: call, result: result)
        case "deleteContact":
            deleteContact(call: call, result: result)
        default:
            result(FlutterMethodNotImplemented)
        }
    }
    
    private func getContacts(result: @escaping FlutterResult) {
    let store = CNContactStore()
    let keys = [CNContactGivenNameKey, CNContactFamilyNameKey, CNContactPhoneNumbersKey]
    let request = CNContactFetchRequest(keysToFetch: keys as [CNKeyDescriptor])
    var contactsList: [[String: Any]] = []

    do {
        try store.enumerateContacts(with: request) { (contact, _) in
            let phoneNumbers = contact.phoneNumbers.map { $0.value.stringValue } 
            
            let contactData: [String: Any] = [
                "id": contact.identifier,
                "name": "\(contact.givenName) \(contact.familyName)",
                "phones": phoneNumbers 
            ]
            contactsList.append(contactData)
        }
        result(contactsList)
    } catch {
        result(FlutterError(code: "CONTACT_FETCH_ERROR", message: "Failed to fetch contacts", details: nil))
    }
}

    
    private func addContact(call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard let args = call.arguments as? [String: Any],
              let name = args["name"] as? String,
              let phone = args["phone"] as? String else {
            result(FlutterError(code: "INVALID_ARGUMENTS", message: "Missing name or phone", details: nil))
            return
        }
        
        let store = CNContactStore()
        let contact = CNMutableContact()
        
        contact.givenName = name
        let phoneNumber = CNLabeledValue(label: CNLabelPhoneNumberMobile, value: CNPhoneNumber(stringValue: phone))
        contact.phoneNumbers = [phoneNumber]
        
        let saveRequest = CNSaveRequest()
        saveRequest.add(contact, toContainerWithIdentifier: nil)
        
        do {
            try store.execute(saveRequest)
            result(nil)
        } catch {
            result(FlutterError(code: "CONTACT_SAVE_ERROR", message: "Failed to add contact", details: nil))
        }
    }
    
    private func updateContact(call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard let args = call.arguments as? [String: Any],
              let id = args["id"] as? String,
              let name = args["name"] as? String,
              let phone = args["phone"] as? String else {
            result(FlutterError(code: "INVALID_ARGUMENTS", message: "Missing id, name, or phone", details: nil))
            return
        }
        
        let store = CNContactStore()
        let predicate = CNContact.predicateForContacts(withIdentifiers: [id])
        let keys = [CNContactGivenNameKey, CNContactFamilyNameKey, CNContactPhoneNumbersKey]
        
        do {
            let contacts = try store.unifiedContacts(matching: predicate, keysToFetch: keys as [CNKeyDescriptor])
            guard let existingContact = contacts.first else {
                result(FlutterError(code: "CONTACT_NOT_FOUND", message: "No contact found with given ID", details: nil))
                return
            }
            
            let mutableContact = existingContact.mutableCopy() as! CNMutableContact
            mutableContact.givenName = name
            mutableContact.phoneNumbers = [CNLabeledValue(label: CNLabelPhoneNumberMobile, value: CNPhoneNumber(stringValue: phone))]
            
            let saveRequest = CNSaveRequest()
            saveRequest.update(mutableContact)
            try store.execute(saveRequest)
            result(nil)
            
        } catch {
            result(FlutterError(code: "CONTACT_UPDATE_ERROR", message: "Failed to update contact", details: nil))
        }
    }
    
    private func deleteContact(call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard let args = call.arguments as? [String: Any],
              let id = args["id"] as? String else {
            result(FlutterError(code: "INVALID_ARGUMENTS", message: "Missing id", details: nil))
            return
        }
        
        let store = CNContactStore()
        let predicate = CNContact.predicateForContacts(withIdentifiers: [id])
        let keys = [CNContactGivenNameKey, CNContactFamilyNameKey, CNContactPhoneNumbersKey]
        
        do {
            let contacts = try store.unifiedContacts(matching: predicate, keysToFetch: keys as [CNKeyDescriptor])
            guard let existingContact = contacts.first else {
                result(FlutterError(code: "CONTACT_NOT_FOUND", message: "No contact found with given ID", details: nil))
                return
            }
            
            let mutableContact = existingContact.mutableCopy() as! CNMutableContact
            let saveRequest = CNSaveRequest()
            saveRequest.delete(mutableContact)
            try store.execute(saveRequest)
            result(nil)
            
        } catch {
            result(FlutterError(code: "CONTACT_DELETE_ERROR", message: "Failed to delete contact", details: nil))
        }
    }
}
