package com.vanethos.notification_permissions;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.core.app.NotificationManagerCompat;

import java.util.List;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import static android.provider.Settings.EXTRA_APP_PACKAGE;
import static android.provider.Settings.EXTRA_CHANNEL_ID;

public class NotificationPermissionsPlugin implements MethodChannel.MethodCallHandler {
  public static void registerWith(Registrar registrar) {
    final MethodChannel channel =
        new MethodChannel(registrar.messenger(), "notification_permissions");
    channel.setMethodCallHandler(new NotificationPermissionsPlugin(registrar));
  }

  private static final String PERMISSION_GRANTED = "granted";
  private static final String PERMISSION_DENIED = "denied";
  private static final String CHANNEL_AVAILABLE = "available";
  private static final String CHANNEL_UNAVAILABLE = "unavailable";
  private static final String CHANNEL_NONE = "none";

  private final Context context;

  private NotificationPermissionsPlugin(Registrar registrar) {
    this.context = registrar.activity();
  }

  @Override
  public void onMethodCall(MethodCall call, MethodChannel.Result result) {
    if ("getNotificationPermissionStatus".equalsIgnoreCase(call.method)) {
      result.success(getNotificationPermissionStatus());
    } else if ("requestNotificationPermissions".equalsIgnoreCase(call.method)) {
      if (PERMISSION_DENIED.equalsIgnoreCase(getNotificationPermissionStatus())) {
        if (context instanceof Activity) {
          navigateToSettings();
          result.success(null);
        } else {
          result.error(call.method, "context is not instance of Activity", null);
        }
      } else {
        result.success(null);
      }
    } else if ("navigateToNotificationSettings".equalsIgnoreCase(call.method)) {
      if (context instanceof Activity) {
        navigateToSettings();
        result.success(null);
      } else {
        result.error(call.method, "context is not instance of Activity", null);
      }
    } else if ("checkChannelsStatus".equalsIgnoreCase(call.method)) {
      final List<String> channels = call.arguments();
      result.success(checkChannelsStatus(channels));
    } else {
      result.notImplemented();
    }
  }

  private String getNotificationPermissionStatus() {
    return (NotificationManagerCompat.from(context).areNotificationsEnabled())
        ? PERMISSION_GRANTED
        : PERMISSION_DENIED;
  }

  private void navigateToSettings() {
    try {
      if (navigateToNotificationSettings()) return;
      navigateToAppSettings();
    } catch (Exception e) {
      navigateToAppSettings();
    }
  }

  private boolean navigateToNotificationSettings() {
    // 21-25 may also support this
    Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
    intent.putExtra(EXTRA_APP_PACKAGE, context.getPackageName());
    intent.putExtra(EXTRA_CHANNEL_ID, context.getApplicationInfo().uid);
    // 21-25, 5.0-7.1
    intent.putExtra("app_package", context.getPackageName());
    intent.putExtra("app_uid", context.getApplicationInfo().uid);
    if (intent.resolveActivity(context.getPackageManager()) == null) return false;

    context.startActivity(intent);
    return true;
  }

  private void navigateToAppSettings() {
    final Uri uri = Uri.fromParts("package", context.getPackageName(), null);

    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
    intent.setData(uri);

    context.startActivity(intent);
  }

  private String checkChannelsStatus(List<String> channels) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      return (NotificationManagerCompat.from(context).areNotificationsEnabled())
          ? CHANNEL_AVAILABLE
          : CHANNEL_UNAVAILABLE;
    }
    NotificationManagerCompat nm = NotificationManagerCompat.from(context);
    boolean notificationEnable = nm.areNotificationsEnabled();
    boolean channelsCreated = false;
    boolean channelsAvailable = false;
    for (NotificationChannel ch : nm.getNotificationChannels()) {
      if (!channels.isEmpty() && !channels.contains(ch.getId())) continue;
      channelsCreated = true;
      if (!notificationEnable) break;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        String groupId = ch.getGroup();
        if (groupId != null) {
          NotificationChannelGroup group = nm.getNotificationChannelGroup(groupId);
          if (group != null && group.isBlocked()) continue;
        }
      }
      if (ch.getImportance() > NotificationManager.IMPORTANCE_NONE) channelsAvailable = true;
    }
    if (!channelsCreated) return CHANNEL_NONE;
    if (channelsAvailable) return CHANNEL_AVAILABLE;
    return CHANNEL_UNAVAILABLE;
  }
}
