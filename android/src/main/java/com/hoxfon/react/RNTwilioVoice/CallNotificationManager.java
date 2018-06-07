package com.hoxfon.react.RNTwilioVoice;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.WindowManager;

import com.facebook.react.bridge.ReactApplicationContext;
import com.twilio.voice.CallInvite;

import java.util.List;

import static android.content.Context.ACTIVITY_SERVICE;

import static com.hoxfon.react.RNTwilioVoice.TwilioVoiceModule.TAG;
import static com.hoxfon.react.RNTwilioVoice.TwilioVoiceModule.ACTION_ANSWER_CALL;
import static com.hoxfon.react.RNTwilioVoice.TwilioVoiceModule.ACTION_REJECT_CALL;
import static com.hoxfon.react.RNTwilioVoice.TwilioVoiceModule.ACTION_HANGUP_CALL;
import static com.hoxfon.react.RNTwilioVoice.TwilioVoiceModule.ACTION_INCOMING_CALL;
import static com.hoxfon.react.RNTwilioVoice.TwilioVoiceModule.ACTION_MISSED_CALL;
import static com.hoxfon.react.RNTwilioVoice.TwilioVoiceModule.INCOMING_CALL_INVITE;
import static com.hoxfon.react.RNTwilioVoice.TwilioVoiceModule.INCOMING_CALL_NOTIFICATION_ID;
import static com.hoxfon.react.RNTwilioVoice.TwilioVoiceModule.NOTIFICATION_TYPE;
import static com.hoxfon.react.RNTwilioVoice.TwilioVoiceModule.CALL_SID_KEY;
import static com.hoxfon.react.RNTwilioVoice.TwilioVoiceModule.INCOMING_NOTIFICATION_PREFIX;
import static com.hoxfon.react.RNTwilioVoice.TwilioVoiceModule.MISSED_CALLS_GROUP;
import static com.hoxfon.react.RNTwilioVoice.TwilioVoiceModule.MISSED_CALLS_NOTIFICATION_ID;
import static com.hoxfon.react.RNTwilioVoice.TwilioVoiceModule.HANGUP_NOTIFICATION_ID;
import static com.hoxfon.react.RNTwilioVoice.TwilioVoiceModule.PREFERENCE_KEY;
import static com.hoxfon.react.RNTwilioVoice.TwilioVoiceModule.ACTION_CLEAR_MISSED_CALLS_COUNT;
import static com.hoxfon.react.RNTwilioVoice.TwilioVoiceModule.CLEAR_MISSED_CALLS_NOTIFICATION_ID;


public class CallNotificationManager {

  private static final String VOICE_CHANNEL = "default";

  private NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

  public CallNotificationManager() {}

  public int getApplicationImportance(ReactApplicationContext context) {
    ActivityManager activityManager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
    if (activityManager == null) {
      return 0;
    }
    List<ActivityManager.RunningAppProcessInfo> processInfos = activityManager.getRunningAppProcesses();
    if (processInfos == null) {
      return 0;
    }

    for (ActivityManager.RunningAppProcessInfo processInfo : processInfos) {
      if (processInfo.processName.equals(context.getApplicationInfo().packageName)) {
        return processInfo.importance;
      }
    }
    return 0;
  }

  public Class getMainActivityClass(ReactApplicationContext context) {
    String packageName = context.getPackageName();
    Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
    String className = launchIntent.getComponent().getClassName();
    try {
      return Class.forName(className);
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      return null;
    }
  }

  public Intent getLaunchIntent(ReactApplicationContext context,
      int notificationId,
      CallInvite callInvite,
      Boolean shouldStartNewTask,
      int appImportance
  ) {
    Intent launchIntent = new Intent(context, getMainActivityClass(context));

    int launchFlag = Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP;
    if (shouldStartNewTask || appImportance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
      launchFlag = Intent.FLAG_ACTIVITY_NEW_TASK;
    }

    launchIntent.setAction(ACTION_INCOMING_CALL)
        .putExtra(INCOMING_CALL_NOTIFICATION_ID, notificationId);

    if (callInvite != null) {
      launchIntent.putExtra(INCOMING_CALL_INVITE, callInvite);
    }
    return launchIntent;
  }

  public void initCallNotificationsChannel(NotificationManager notificationManager) {
    if (Build.VERSION.SDK_INT < 26) {
      return;
    }
    NotificationChannel channel = new NotificationChannel(VOICE_CHANNEL,
        "Primary Voice Channel", NotificationManager.IMPORTANCE_DEFAULT);
    channel.setLightColor(Color.GREEN);
    channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
    notificationManager.createNotificationChannel(channel);
  }

  public void createMissedCallNotification(ReactApplicationContext context, CallInvite callInvite) {
    SharedPreferences sharedPref = context.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE);
    SharedPreferences.Editor sharedPrefEditor = sharedPref.edit();

        /*
         * Create a PendingIntent to specify the action when the notification is
         * selected in the notification drawer
         */
    Intent intent = new Intent(context, getMainActivityClass(context));
    intent.setAction(ACTION_MISSED_CALL)
        .putExtra(INCOMING_CALL_NOTIFICATION_ID, MISSED_CALLS_NOTIFICATION_ID)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

    Intent clearMissedCallsCountIntent = new Intent(ACTION_CLEAR_MISSED_CALLS_COUNT)
        .putExtra(INCOMING_CALL_NOTIFICATION_ID, CLEAR_MISSED_CALLS_NOTIFICATION_ID);
    PendingIntent clearMissedCallsCountPendingIntent = PendingIntent.getBroadcast(context, 0, clearMissedCallsCountIntent, 0);
        /*
         * Pass the notification id and call sid to use as an identifier to open the notification
         */
    Bundle extras = new Bundle();
    extras.putInt(INCOMING_CALL_NOTIFICATION_ID, MISSED_CALLS_NOTIFICATION_ID);
    extras.putString(CALL_SID_KEY, callInvite.getCallSid());
    extras.putString(NOTIFICATION_TYPE, ACTION_MISSED_CALL);

        /*
         * Create the notification shown in the notification drawer
         */
    NotificationCompat.Builder notification =
        new NotificationCompat.Builder(context, VOICE_CHANNEL)
            .setGroup(MISSED_CALLS_GROUP)
            .setGroupSummary(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setSmallIcon(R.drawable.ic_call_missed_white_24dp)
            .setContentTitle("Llamada perdida")
            .setContentText("Keenvil")
            .setAutoCancel(true)
            .setShowWhen(true)
            .setExtras(extras)
            .setDeleteIntent(clearMissedCallsCountPendingIntent)
            .setContentIntent(pendingIntent);

    int missedCalls = sharedPref.getInt(MISSED_CALLS_GROUP, 0);
    missedCalls++;
    if (missedCalls == 1) {
      inboxStyle = new NotificationCompat.InboxStyle();
      inboxStyle.setBigContentTitle("Llamada perdida");
    } else {
      inboxStyle.setBigContentTitle(String.valueOf(missedCalls) + " llamadas perdidas");
    }
    inboxStyle.addLine("de Keenvil");
    sharedPrefEditor.putInt(MISSED_CALLS_GROUP, missedCalls);
    sharedPrefEditor.commit();

    notification.setStyle(inboxStyle);

    // build notification large icon
    Resources res = context.getResources();
    int largeIconResId = res.getIdentifier("app_icon", "mipmap", context.getPackageName());
    Bitmap largeIconBitmap = BitmapFactory.decodeResource(res, largeIconResId);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && largeIconResId != 0) {
      notification.setLargeIcon(largeIconBitmap);
    }

    NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    notificationManager.notify(MISSED_CALLS_NOTIFICATION_ID, notification.build());
  }

  public void removeHangupNotification(ReactApplicationContext context) {
    NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    notificationManager.cancel(HANGUP_NOTIFICATION_ID);
  }
}