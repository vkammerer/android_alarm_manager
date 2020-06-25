import 'dart:async';
import 'dart:ui';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

const String _pluginChannelName = "plugins.flutter.io/android_alarm_manager";
const String _oneShotAtMethod = "onShotAt";
const String _periodicMethod = "periodic";
const String _cancelMethod = "cancel";
const String _defaultServiceType = "JobIntentService";

typedef DateTime _Now();
typedef CallbackHandle _GetCallbackHandle(Function callback);

class AndroidAlarmManager {
  static MethodChannel _channel =
      const MethodChannel(_pluginChannelName, JSONMethodCodec());

  static _Now _now = () => DateTime.now();
  static _GetCallbackHandle _getCallbackHandle =
      (Function callback) => PluginUtilities.getCallbackHandle(callback);

  /// This is exposed for the unit tests. It should not be accessed by users of
  /// the plugin.
  @visibleForTesting
  static void setTestOverides(
      {_Now now, _GetCallbackHandle getCallbackHandle}) {
    _now = (now ?? _now);
    _getCallbackHandle = (getCallbackHandle ?? _getCallbackHandle);
  }

  static Future<bool> oneShot(
    Duration delay,
    int id,
    Function callback, {
    bool alarmClock = false,
    bool allowWhileIdle = false,
    bool exact = false,
    bool wakeup = false,
    bool rescheduleOnReboot = false,
    String serviceType = _defaultServiceType,
  }) =>
      oneShotAt(
        _now().add(delay),
        id,
        callback,
        alarmClock: alarmClock,
        allowWhileIdle: allowWhileIdle,
        exact: exact,
        wakeup: wakeup,
        rescheduleOnReboot: rescheduleOnReboot,
        serviceType: serviceType,
      );

  static Future<bool> oneShotAt(
    DateTime time,
    int id,
    Function callback, {
    bool alarmClock = false,
    bool allowWhileIdle = false,
    bool exact = false,
    bool wakeup = false,
    bool rescheduleOnReboot = false,
    String serviceType = _defaultServiceType,
  }) async {
    // ignore: inference_failure_on_function_return_type
    assert(callback is Function() || callback is Function(int));
    assert(id.bitLength < 32);
    final int startMillis = time.millisecondsSinceEpoch;
    final CallbackHandle handle = _getCallbackHandle(callback);
    if (handle == null) {
      return false;
    }
    final bool r =
        await _channel.invokeMethod<bool>(_oneShotAtMethod, <dynamic>[
      id,
      alarmClock,
      allowWhileIdle,
      exact,
      wakeup,
      startMillis,
      rescheduleOnReboot,
      handle.toRawHandle(),
      serviceType
    ]);
    return (r == null) ? false : r;
  }

  static Future<bool> periodic(
    Duration duration,
    int id,
    Function callback, {
    DateTime startAt,
    bool exact = false,
    bool wakeup = false,
    bool rescheduleOnReboot = false,
    String serviceType = _defaultServiceType,
  }) async {
    // ignore: inference_failure_on_function_return_type
    assert(callback is Function() || callback is Function(int));
    assert(id.bitLength < 32);
    final int now = _now().millisecondsSinceEpoch;
    final int period = duration.inMilliseconds;
    final int first =
        startAt != null ? startAt.millisecondsSinceEpoch : now + period;
    final CallbackHandle handle = _getCallbackHandle(callback);
    if (handle == null) {
      return false;
    }
    final bool r = await _channel.invokeMethod<bool>(_periodicMethod, <dynamic>[
      id,
      exact,
      wakeup,
      first,
      period,
      rescheduleOnReboot,
      handle.toRawHandle(),
      serviceType
    ]);
    return (r == null) ? false : r;
  }

  static Future<bool> cancel(int id,
      {String serviceType = _defaultServiceType}) async {
    final bool r = await _channel
        .invokeMethod<bool>(_cancelMethod, <dynamic>[id, serviceType]);
    return (r == null) ? false : r;
  }
}
