package com.vincentkammerer.android_alarm_manager;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

public class AndroidAlarmManagerRebootBroadcastReceiver extends BroadcastReceiver {
  public static final String TAG = "AndroidAlarmManagerRBR";

  @Override
  public void onReceive(Context context, Intent intent) {
    if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
      Log.i(TAG, "Rescheduling after boot!");
      AndroidAlarmManagerPlugin.reschedulePersistentAlarms(context);
    }
  }

  public static void enableRescheduleOnReboot(Context context) {
    scheduleOnReboot(context, PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
  }

  public static void disableRescheduleOnReboot(Context context) {
    scheduleOnReboot(context, PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
  }

  private static void scheduleOnReboot(Context context, int state) {
    ComponentName receiver = new ComponentName(context, AndroidAlarmManagerRebootBroadcastReceiver.class);
    PackageManager pm = context.getPackageManager();
    pm.setComponentEnabledSetting(receiver, state, PackageManager.DONT_KILL_APP);
  }
}