package com.vincentkammerer.android_alarm_manager;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.AlarmManagerCompat;
import com.vincentkammerer.flutter_service.FlutterJobIntentServiceBroadcastReceiver;
import com.vincentkammerer.flutter_service.FlutterForegroundServiceBroadcastReceiver;
import com.vincentkammerer.flutter_service.PluginRegistrantException;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.JSONMethodCodec;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * AndroidAlarmManagerPlugin
 */
public class AndroidAlarmManagerPlugin implements FlutterPlugin, MethodCallHandler {

  public static final String TAG = "AndroidAlarmManagerP";
  public static final String CHANNEL_NAME = "plugins.flutter.io/android_alarm_manager";
  public static final String ONE_SHOT_AT_METHOD = "onShotAt";
  public static final String PERIODIC_METHOD = "periodic";
  public static final String CANCEL_METHOD = "cancel";
  private Context context;
  private Object initializationLock = new Object();
  private MethodChannel channel;


  private static final String PERSISTENT_ALARMS_SET_KEY = "persistent_alarm_ids";
  protected static final String SHARED_PREFERENCES_KEY = "io.flutter.android_alarm_manager_plugin";
  private static final Object persistentAlarmsLock = new Object();

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    initialize(flutterPluginBinding.getApplicationContext(),
        flutterPluginBinding.getBinaryMessenger());
  }

  public static void registerWith(Registrar registrar) {
    AndroidAlarmManagerPlugin instance = new AndroidAlarmManagerPlugin();
    instance.initialize(registrar.context(), registrar.messenger());
  }

  private void initialize(Context context, BinaryMessenger messenger) {
    synchronized (initializationLock) {
      this.context = context;
      channel = new MethodChannel(messenger, CHANNEL_NAME, JSONMethodCodec.INSTANCE);
      channel.setMethodCallHandler(this);
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    String method = call.method;
    Object arguments = call.arguments;
    try {
      if (method.equals(PERIODIC_METHOD)) {
        // This message indicates that the Flutter app would like to schedule a periodic
        // task.
        PeriodicRequest periodicRequest = PeriodicRequest.fromJson((JSONArray) arguments);
        setPeriodic(context, periodicRequest);
        result.success(true);
      } else if (method.equals(ONE_SHOT_AT_METHOD)) {
        // This message indicates that the Flutter app would like to schedule a one-time
        // task.
        OneShotRequest oneShotRequest = OneShotRequest.fromJson((JSONArray) arguments);
        setOneShot(context, oneShotRequest);
        result.success(true);
      } else if (method.equals(CANCEL_METHOD)) {
        // This message indicates that the Flutter app would like to cancel a previously
        // scheduled task.
        int requestCode = ((JSONArray) arguments).getInt(0);
        String serviceType = ((JSONArray) arguments).getString(1);
        cancel(context, requestCode, serviceType);
        result.success(true);
      } else {
        result.notImplemented();
      }
    } catch (JSONException e) {
      result.error("error", "JSON error: " + e.getMessage(), null);
    } catch (PluginRegistrantException e) {
      result.error("error", "AlarmManager error: " + e.getMessage(), null);
    }
  }

  /**
   * A request to schedule a one-shot Dart task.
   */
  static final class OneShotRequest {

    static OneShotRequest fromJson(JSONArray json) throws JSONException {
      int requestCode = json.getInt(0);
      boolean alarmClock = json.getBoolean(1);
      boolean allowWhileIdle = json.getBoolean(2);
      boolean exact = json.getBoolean(3);
      boolean wakeup = json.getBoolean(4);
      long startMillis = json.getLong(5);
      boolean rescheduleOnReboot = json.getBoolean(6);
      long callbackHandle = json.getLong(7);
      String serviceType = json.getString(8);

      return new OneShotRequest(
          requestCode,
          alarmClock,
          allowWhileIdle,
          exact,
          wakeup,
          startMillis,
          rescheduleOnReboot,
          callbackHandle,
          serviceType);
    }

    final int requestCode;
    final boolean alarmClock;
    final boolean allowWhileIdle;
    final boolean exact;
    final boolean wakeup;
    final long startMillis;
    final boolean rescheduleOnReboot;
    final long callbackHandle;
    final String serviceType;

    OneShotRequest(
        int requestCode,
        boolean alarmClock,
        boolean allowWhileIdle,
        boolean exact,
        boolean wakeup,
        long startMillis,
        boolean rescheduleOnReboot,
        long callbackHandle,
        String serviceType) {
      this.requestCode = requestCode;
      this.alarmClock = alarmClock;
      this.allowWhileIdle = allowWhileIdle;
      this.exact = exact;
      this.wakeup = wakeup;
      this.startMillis = startMillis;
      this.rescheduleOnReboot = rescheduleOnReboot;
      this.callbackHandle = callbackHandle;
      this.serviceType = serviceType;
    }
  }

  /**
   * A request to schedule a periodic Dart task.
   */
  static final class PeriodicRequest {

    static PeriodicRequest fromJson(JSONArray json) throws JSONException {
      int requestCode = json.getInt(0);
      boolean exact = json.getBoolean(1);
      boolean wakeup = json.getBoolean(2);
      long startMillis = json.getLong(3);
      long intervalMillis = json.getLong(4);
      boolean rescheduleOnReboot = json.getBoolean(5);
      long callbackHandle = json.getLong(6);
      String serviceType = json.getString(7);

      return new PeriodicRequest(
          requestCode,
          exact,
          wakeup,
          startMillis,
          intervalMillis,
          rescheduleOnReboot,
          callbackHandle,
          serviceType);
    }

    final int requestCode;
    final boolean exact;
    final boolean wakeup;
    final long startMillis;
    final long intervalMillis;
    final boolean rescheduleOnReboot;
    final long callbackHandle;
    final String serviceType;

    PeriodicRequest(
        int requestCode,
        boolean exact,
        boolean wakeup,
        long startMillis,
        long intervalMillis,
        boolean rescheduleOnReboot,
        long callbackHandle,
        String serviceType) {
      this.requestCode = requestCode;
      this.exact = exact;
      this.wakeup = wakeup;
      this.startMillis = startMillis;
      this.intervalMillis = intervalMillis;
      this.rescheduleOnReboot = rescheduleOnReboot;
      this.callbackHandle = callbackHandle;
      this.serviceType = serviceType;
    }
  }

  private static void scheduleAlarm(
      Context context,
      int requestCode,
      boolean alarmClock,
      boolean allowWhileIdle,
      boolean repeating,
      boolean exact,
      boolean wakeup,
      long startMillis,
      long intervalMillis,
      boolean rescheduleOnReboot,
      long callbackHandle,
      String serviceType) {
    if (rescheduleOnReboot) {
      addPersistentAlarm(
          context,
          requestCode,
          alarmClock,
          allowWhileIdle,
          repeating,
          exact,
          wakeup,
          startMillis,
          intervalMillis,
          callbackHandle,
          serviceType);
    }

    // Create an Intent for the alarm and set the desired Dart callback handle.
    Intent alarm;
    if (serviceType.equals("JobIntentService")) {
      alarm = new Intent(context, FlutterJobIntentServiceBroadcastReceiver.class);
    }
    else if (serviceType.equals("ForegroundService")) {
      alarm = new Intent(context, FlutterForegroundServiceBroadcastReceiver.class);
    }
    else {
      Log.e(TAG, "At least one serviceType must be provided");
      return;
    }

    alarm.putExtra("id", requestCode);
    alarm.putExtra("callbackHandle", callbackHandle);
    PendingIntent pendingIntent =
        PendingIntent.getBroadcast(context, requestCode, alarm, PendingIntent.FLAG_UPDATE_CURRENT);

    // Use the appropriate clock.
    int clock = AlarmManager.RTC;
    if (wakeup) {
      clock = AlarmManager.RTC_WAKEUP;
    }

    // Schedule the alarm.
    AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

    if (alarmClock) {
      AlarmManagerCompat.setAlarmClock(manager, startMillis, pendingIntent, pendingIntent);
      return;
    }

    if (exact) {
      if (repeating) {
        manager.setRepeating(clock, startMillis, intervalMillis, pendingIntent);
      } else {
        if (allowWhileIdle) {
          AlarmManagerCompat.setExactAndAllowWhileIdle(manager, clock, startMillis, pendingIntent);
        } else {
          AlarmManagerCompat.setExact(manager, clock, startMillis, pendingIntent);
        }
      }
    } else {
      if (repeating) {
        manager.setInexactRepeating(clock, startMillis, intervalMillis, pendingIntent);
      } else {
        if (allowWhileIdle) {
          AlarmManagerCompat.setAndAllowWhileIdle(manager, clock, startMillis, pendingIntent);
        } else {
          manager.set(clock, startMillis, pendingIntent);
        }
      }
    }
  }

  /**
   * Schedules a one-shot alarm to be executed once in the future.
   */
  public static void setOneShot(Context context, AndroidAlarmManagerPlugin.OneShotRequest request) {
    final boolean repeating = false;
    scheduleAlarm(
        context,
        request.requestCode,
        request.alarmClock,
        request.allowWhileIdle,
        repeating,
        request.exact,
        request.wakeup,
        request.startMillis,
        0,
        request.rescheduleOnReboot,
        request.callbackHandle,
        request.serviceType);
  }

  /**
   * Schedules a periodic alarm to be executed repeatedly in the future.
   */
  public static void setPeriodic(
      Context context, AndroidAlarmManagerPlugin.PeriodicRequest request) {
    final boolean repeating = true;
    final boolean allowWhileIdle = false;
    final boolean alarmClock = false;
    scheduleAlarm(
        context,
        request.requestCode,
        alarmClock,
        allowWhileIdle,
        repeating,
        request.exact,
        request.wakeup,
        request.startMillis,
        request.intervalMillis,
        request.rescheduleOnReboot,
        request.callbackHandle,
        request.serviceType);
  }

  /**
   * Cancels an alarm with ID {@code requestCode}.
   */
  public static void cancel(Context context, int requestCode, String serviceType) {
    // Clear the alarm if it was set to be rescheduled after reboots.
    clearPersistentAlarm(context, requestCode);

    Intent alarm;
    // Cancel the alarm with the system alarm service.
    if (serviceType.equals("JobIntentService")) {
      alarm = new Intent(context, FlutterJobIntentServiceBroadcastReceiver.class);
    }
    else if (serviceType.equals("ForegroundService")) {
      alarm = new Intent(context, FlutterForegroundServiceBroadcastReceiver.class);
    }
    else {
      Log.e(TAG, "At least one valid service must be provided");
      return;
    }
    PendingIntent existingIntent =
        PendingIntent.getBroadcast(context, requestCode, alarm, PendingIntent.FLAG_NO_CREATE);
    if (existingIntent == null) {
      Log.i(TAG, "cancel: broadcast receiver not found");
      return;
    }
    AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    manager.cancel(existingIntent);
  }

  private static String getPersistentAlarmKey(int requestCode) {
    return "android_alarm_manager/persistent_alarm_" + Integer.toString(requestCode);
  }

  private static void addPersistentAlarm(
      Context context,
      int requestCode,
      boolean alarmClock,
      boolean allowWhileIdle,
      boolean repeating,
      boolean exact,
      boolean wakeup,
      long startMillis,
      long intervalMillis,
      long callbackHandle,
      String serviceType) {
    HashMap<String, Object> alarmSettings = new HashMap<>();
    alarmSettings.put("alarmClock", alarmClock);
    alarmSettings.put("allowWhileIdle", allowWhileIdle);
    alarmSettings.put("repeating", repeating);
    alarmSettings.put("exact", exact);
    alarmSettings.put("wakeup", wakeup);
    alarmSettings.put("startMillis", startMillis);
    alarmSettings.put("intervalMillis", intervalMillis);
    alarmSettings.put("callbackHandle", callbackHandle);
    alarmSettings.put("serviceType", serviceType);
    JSONObject obj = new JSONObject(alarmSettings);
    String key = getPersistentAlarmKey(requestCode);
    SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES_KEY, 0);

    synchronized (persistentAlarmsLock) {
      Set<String> persistentAlarms = prefs.getStringSet(PERSISTENT_ALARMS_SET_KEY, null);
      if (persistentAlarms == null) {
        persistentAlarms = new HashSet<>();
      }
      if (persistentAlarms.isEmpty()) {
        AndroidAlarmManagerRebootBroadcastReceiver.enableRescheduleOnReboot(context);
      }
      persistentAlarms.add(Integer.toString(requestCode));
      prefs
          .edit()
          .putString(key, obj.toString())
          .putStringSet(PERSISTENT_ALARMS_SET_KEY, persistentAlarms)
          .apply();
    }
  }

  private static void clearPersistentAlarm(Context context, int requestCode) {
    SharedPreferences p = context.getSharedPreferences(SHARED_PREFERENCES_KEY, 0);
    synchronized (persistentAlarmsLock) {
      Set<String> persistentAlarms = p.getStringSet(PERSISTENT_ALARMS_SET_KEY, null);
      if ((persistentAlarms == null) || !persistentAlarms.contains(requestCode)) {
        return;
      }
      persistentAlarms.remove(requestCode);
      String key = getPersistentAlarmKey(requestCode);
      p.edit().remove(key).putStringSet(PERSISTENT_ALARMS_SET_KEY, persistentAlarms).apply();

      if (persistentAlarms.isEmpty()) {
        AndroidAlarmManagerRebootBroadcastReceiver.disableRescheduleOnReboot(context);
      }
    }
  }

  public static void reschedulePersistentAlarms(Context context) {
    synchronized (persistentAlarmsLock) {
      SharedPreferences p = context.getSharedPreferences(SHARED_PREFERENCES_KEY, 0);
      Set<String> persistentAlarms = p.getStringSet(PERSISTENT_ALARMS_SET_KEY, null);
      // No alarms to reschedule.
      if (persistentAlarms == null) {
        return;
      }

      Iterator<String> it = persistentAlarms.iterator();
      while (it.hasNext()) {
        int requestCode = Integer.parseInt(it.next());
        String key = getPersistentAlarmKey(requestCode);
        String json = p.getString(key, null);
        if (json == null) {
          Log.e(
              TAG, "Data for alarm request code " + Integer.toString(requestCode) + " is invalid.");
          continue;
        }
        try {
          JSONObject alarm = new JSONObject(json);
          boolean alarmClock = alarm.getBoolean("alarmClock");
          boolean allowWhileIdle = alarm.getBoolean("allowWhileIdle");
          boolean repeating = alarm.getBoolean("repeating");
          boolean exact = alarm.getBoolean("exact");
          boolean wakeup = alarm.getBoolean("wakeup");
          long startMillis = alarm.getLong("startMillis");
          long intervalMillis = alarm.getLong("intervalMillis");
          long callbackHandle = alarm.getLong("callbackHandle");
          String serviceType = alarm.getString("serviceType");
          scheduleAlarm(
              context,
              requestCode,
              alarmClock,
              allowWhileIdle,
              repeating,
              exact,
              wakeup,
              startMillis,
              intervalMillis,
              false,
              callbackHandle,
              serviceType);
        } catch (JSONException e) {
          Log.e(TAG, "Data for alarm request code " + requestCode + " is invalid: " + json);
        }
      }
    }
  }
}
