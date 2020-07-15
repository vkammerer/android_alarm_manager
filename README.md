### Warning: This is an experiment   
This project is an adaptation of the [official Flutter Android Alarm Manager plugin](https://github.com/flutter/plugins/tree/master/packages/android_alarm_manager).

It has been refactored to rely on [flutter_service](https://github.com/vkammerer/flutter_service) for Dart callbacks execution.

Please refer to [flutter_service](https://github.com/vkammerer/flutter_service) for details about what it does or bug reports.

### Why?
The original implementation of `android_alarm_manager` executes Dart callbacks in an Android [`JobIntentService`](https://developer.android.com/reference/androidx/core/app/JobIntentService), which is a good default that will suit users most of the time.   

However, there may be cases when you want the Dart callback to execute in a different context, like a Foreground Service for example. This is how this plugin differs from its original counterpart: it exposes the `serviceType` you would like it to run the Dart callback in.

### API
The original public API of the plugin remains available, but:
- the initialization is different.
- a new parameter `serviceType` is available in all available public methods.

##### Initialization
Instead of calling
```dart
await AndroidAlarmManager.initialize();
```

You should now call
```dart
// at least one of the two should be set to true
await FlutterService.initialize(
  jobIntentService: true,
  foregroundService: true,
);
```

##### Specifying the service type
Most public methods available on the `AndroidAlarmManager` class now expose an optional parameter `serviceType`:
```dart
await AndroidAlarmManager.oneShot(
  const Duration(seconds: 5),
  1,
  callback,
  serviceType: 'JobIntentService' // by default set to 'JobIntentService' - currently also accepts 'ForegroundService'
);
```

If you have:
- initialized FlutterService with `foregroundService` set to true
- started the FlutterService' `FlutterForegroundService` by broadcasting an Intent containing a notification

Then you may now decide to execute the Dart callback within that Foreground Service:
```dart
await AndroidAlarmManager.oneShot(
  const Duration(seconds: 5),
  1,
  callback,
  serviceType: 'ForegroundService' // by default set to 'JobIntentService' - currently also accepts 'ForegroundService'
);
```
