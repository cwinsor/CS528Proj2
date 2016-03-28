package com.bignerdranch.android.criminalintent.database;

import android.util.Log;

/**
 * Created by Chris on 2/23/2016.
 */
public class MyStatic {
//    private static final String TAG = MyActivity.class.getSimpleName();
private static final String TAG = "ZONA ";

    private static MyStatic ourInstance = new MyStatic();

    public static MyStatic getInstance() {
        return ourInstance;
    }

    private MyStatic() {
    }

    public static void Fatal(String tag, String msg) {
        Log.e(TAG + tag , msg);
        // generate a stack
        int foo = 10/0;
    }

    public static void Log(String tag, String msg) {
        Log.v(TAG + tag , msg);
    }


}
