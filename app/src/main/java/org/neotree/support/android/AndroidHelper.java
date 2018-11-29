/*
 * The MIT License (MIT)
 * Copyright (c) 2016 Ubiqueworks Ltd and contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT
 * SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package org.neotree.support.android;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;

/**
 * Created by matteo on 25/05/2016.
 */
public class AndroidHelper {

    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    public static void startActivity(Activity activity, Class<?> activityClass, boolean finish) {
        startActivity(activity, activityClass, finish, null);
    }

    public static void startActivity(Activity activity, Class<?> activityClass, boolean finish, Bundle args) {
        Intent i = new Intent(activity, activityClass);
        if (args != null) {
            i.putExtras(args);
        }

        activity.startActivity(i);

        if (finish) {
            activity.finish();
        }
    }

    public static void startActivityForResult(Activity activity, Class<?> activityClass, int requestCode, boolean finish) {
        startActivityForResult(activity, activityClass, null, requestCode, finish);
    }

    public static void startActivityForResult(Activity activity, Class<?> activityClass, Bundle args, int requestCode, boolean finish) {
        Intent i = new Intent(activity, activityClass);
        if (args != null) {
            i.putExtras(args);
        }

        activity.startActivityForResult(i, requestCode);

        if (finish) {
            activity.finish();
        }
    }

    public static void replaceFragment(FragmentManager fragmentManager, int containerId, Fragment fragment) {
        replaceFragment(fragmentManager, containerId, fragment, false);
    }

    public static void replaceFragment(FragmentManager fragmentManager, int containerId, Fragment fragment, boolean addToBackStack) {
        android.app.FragmentTransaction transaction = fragmentManager.beginTransaction();

        if (addToBackStack) {
            transaction.addToBackStack(null);
        }

        transaction.replace(containerId, fragment)
                .commit();
    }

    public static void replaceFragment(FragmentManager fragmentManager, int containerId, Fragment fragment, int enterAnimation, int exitAnimation) {
        fragmentManager.beginTransaction()
                .setCustomAnimations(enterAnimation, exitAnimation)
                .replace(containerId, fragment)
                .commit();
    }


    public static void restartApplication(Context context, Bundle bundle) {
        // Restart activity to fully clean the old stack (a little hack...)
        Context applicationContext = context.getApplicationContext();
        Intent i = applicationContext.getPackageManager()
                .getLaunchIntentForPackage(applicationContext.getPackageName());
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        if (bundle != null) {
            i.putExtras(bundle);
        }
        applicationContext.startActivity(i);
    }

    public static String bytesToHex(byte[] in) {
        if (in == null) {
            return null;
        }
        final StringBuilder builder = new StringBuilder();
        for(byte b : in) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

//    public static int getStatusBarHeight(Context context) {
//        int result = 0;
//        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
//        if (resourceId > 0) {
//            result = context.getResources().getDimensionPixelSize(resourceId);
//        }
//        return result;
//    }
//
//    public static float dipToPixels(Context context, float dipValue) {
//        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
//        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
//    }
//
//    public static void dismissSoftKeyboard(Context context, IBinder windowToken) {
//        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
//        imm.hideSoftInputFromWindow(windowToken, 0);
//    }
//
//    public static void dismissSoftKeyboard(Activity activity) {
//        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
//        imm.hideSoftInputFromWindow(activity.findViewById(android.R.id.content).getWindowToken(), 0);
//    }

}
