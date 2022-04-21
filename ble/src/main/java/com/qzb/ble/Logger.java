package com.qzb.ble;

import android.util.Log;

/**
 * 生活白科技-居家小确幸
 *
 * @ClassName: Logger
 * @Author: Leon.Qin
 * @Date: 2022/3/4 17:46
 * @Description:
 */
public class Logger {
    private final static String TAG = "OkBle";
    private static boolean logEnable = true;

    public static void i(String log) {
        if (logEnable) {
            Log.i(TAG, log);
        }
    }

    public static void e(String log) {
        if (logEnable) {
            Log.e(TAG, log);
        }
    }
}