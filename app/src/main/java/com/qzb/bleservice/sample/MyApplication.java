package com.qzb.bleservice.sample;

import android.app.Application;

import com.qzb.ble.OkBleHelper;
import com.qzb.ble.listener.IMergePackageImpl;

/**
 * 生活白科技-居家小确幸
 *
 * @ClassName: MyApplication
 * @Author: Leon.Qin
 * @Date: 2022/4/11 9:13
 * @Description:
 */
public class MyApplication extends Application {

    public static final String SERVICE_UUID = "0000e0ff-3c17-d293-8e48-14fe2e4da212";
    public static final String NOTIFY_UUID = "0000ffe2-0000-1000-8000-00805f9b34fb";
    public static final String WRITE_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";

    private OkBleHelper chiereOneBleHelper;

    @Override
    public void onCreate() {
        super.onCreate();

        chiereOneBleHelper = new OkBleHelper.Builder(this)
                .serviceUUID(SERVICE_UUID)
                .writeUUID(WRITE_UUID)
                .notifyUUID(NOTIFY_UUID)
                .writeTimeout(1000)
                .writeLongTimeout(3000)
                .writeRetryTime(3)
                .writeLongRetryTimes(3)
                .mergePackage(new IMergePackageImpl())
                .build();
    }


    public OkBleHelper getChiereOneBleHelper() {
        return chiereOneBleHelper;
    }
}