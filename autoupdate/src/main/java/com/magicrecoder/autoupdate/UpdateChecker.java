package com.magicrecoder.autoupdate;

import android.content.Context;
import android.util.Log;

public class UpdateChecker {


    public static void checkForDialog(Context context) {
        if (context != null) {
            Log.d(Constants.TAG, "检测是否有新版本");
            new CheckUpdateTask(context, Constants.TYPE_DIALOG, true).execute();
        } else {
            Log.d(Constants.TAG, "The arg context is null");
        }
    }


    public static void checkForNotification(Context context) {
        if (context != null) {
            new CheckUpdateTask(context, Constants.TYPE_NOTIFICATION, false).execute();
        } else {
            Log.d(Constants.TAG, "The arg context is null");
        }

    }


}
