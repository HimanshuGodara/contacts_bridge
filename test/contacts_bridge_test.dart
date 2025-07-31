import 'package:flutter_test/flutter_test.dart';
import 'package:contacts_bridge/contacts_bridge.dart';
import 'package:contacts_bridge/contacts_bridge_platform_interface.dart';
import 'package:contacts_bridge/contacts_bridge_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockContactsBridgePlatform
    with MockPlatformInterfaceMixin
    implements ContactsBridgePlatform {
  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final ContactsBridgePlatform initialPlatform =
      ContactsBridgePlatform.instance;

  test('$MethodChannelContactsBridge is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelContactsBridge>());
  });

  test('getPlatformVersion', () async {
    ContactsBridge contactsBridgePlugin = ContactsBridge();
    MockContactsBridgePlatform fakePlatform = MockContactsBridgePlatform();
    ContactsBridgePlatform.instance = fakePlatform;

    expect(await contactsBridgePlugin.getPlatformVersion(), '42');
  });
}
