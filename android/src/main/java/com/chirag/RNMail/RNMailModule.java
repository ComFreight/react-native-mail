package com.chirag.RNMail;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import androidx.core.content.FileProvider;
import android.text.Html;
import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.Callback;

import java.util.List;
import java.io.File;

/**
 * NativeModule that allows JS to open emails sending apps chooser.
 */
public class RNMailModule extends ReactContextBaseJavaModule {

  ReactApplicationContext reactContext;
  Uri uri;

  public RNMailModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  @Override
  public String getName() {
    return "RNMail";
  }

  /**
   * Converts a ReadableArray to a String array
   *
   * @param r the ReadableArray instance to convert
   *
   * @return array of strings
   */
  private String[] readableArrayToStringArray(ReadableArray r) {
    int length = r.size();
    String[] strArray = new String[length];

    for (int keyIndex = 0; keyIndex < length; keyIndex++) {
      strArray[keyIndex] = r.getString(keyIndex);
    }

    return strArray;
  }

  @ReactMethod
  public void mail(ReadableMap options, Callback callback) {
    Intent i = new Intent(Intent.ACTION_SENDTO);
    i.setData(Uri.parse("mailto:"));

    if (options.hasKey("subject") && !options.isNull("subject")) {
      i.putExtra(Intent.EXTRA_SUBJECT, options.getString("subject"));
    }

    if (options.hasKey("body") && !options.isNull("body")) {
      String body = options.getString("body");
      if (options.hasKey("isHTML") && options.getBoolean("isHTML")) {
        i.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(body));
      } else {
        i.putExtra(Intent.EXTRA_TEXT, body);
      }
    }

    if (options.hasKey("recipients") && !options.isNull("recipients")) {
      ReadableArray recipients = options.getArray("recipients");
      i.putExtra(Intent.EXTRA_EMAIL, readableArrayToStringArray(recipients));
    }

    if (options.hasKey("ccRecipients") && !options.isNull("ccRecipients")) {
      ReadableArray ccRecipients = options.getArray("ccRecipients");
      i.putExtra(Intent.EXTRA_CC, readableArrayToStringArray(ccRecipients));
    }

    if (options.hasKey("bccRecipients") && !options.isNull("bccRecipients")) {
      ReadableArray bccRecipients = options.getArray("bccRecipients");
      i.putExtra(Intent.EXTRA_BCC, readableArrayToStringArray(bccRecipients));
    }

    if (options.hasKey("attachment") && !options.isNull("attachment")) {
      ReadableMap attachment = options.getMap("attachment");
      if (attachment.hasKey("path") && !attachment.isNull("path")) {
        String path = attachment.getString("path");
        File file = new File(path);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
          uri = Uri.fromFile(file);
          Log.i("RNMail uri", uri.toString());
        } else {
          try {
            final String packageName = reactContext.getApplicationContext().getPackageName();
            final String authority =  new StringBuilder(packageName).append(".provider").toString();
            uri = FileProvider.getUriForFile(
                    reactContext,
                    authority,
                    file);
            Log.i("RNMail file Provider", uri.toString());
          } catch (Exception e) {
            String message = "There was a problem sharing the file " + file.getName();
            Log.e("RNMail", message);
            callback.invoke("error", message + "\n" + e.getMessage());
          }
        }
        //uri = Uri.parse(path);
        String message = "the file uri " + uri.toString();
        Log.i("RNMail", message);
        i.putExtra(Intent.EXTRA_STREAM, uri);
      }
    }
    try{
      PackageManager manager = reactContext.getPackageManager();
      List<ResolveInfo> list = manager.queryIntentActivities(i, 0);
      for (ResolveInfo resolvedIntentInfo : list) {
        final String packageName = resolvedIntentInfo.activityInfo.packageName;
        reactContext.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
      }
      if (list == null || list.size() == 0) {
        callback.invoke("not_available");
        return;
      }

      if (list.size() == 1) {
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
          reactContext.startActivity(i);
        } catch (Exception ex) {
          callback.invoke("error");
        }
      } else {
        Intent chooser = Intent.createChooser(i, "Send Mail");
        chooser.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
          reactContext.startActivity(chooser);
        } catch (Exception ex) {
          callback.invoke("error");
        }
      }
    } catch(Exception e) {
      String message = "There was a problem sharing the file";
      callback.invoke("error", message + "\n" + e.getMessage());
    }
  }
}
