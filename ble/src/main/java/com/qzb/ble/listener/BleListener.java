package com.qzb.ble.listener;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;

/**
 * 生活白科技-居家小确幸
 *
 * @ClassName: BleListener
 * @Author: Leon.Qin
 * @Date: 2022/3/7 18:19
 * @Description:
 */
public abstract class BleListener {

    /**
     * 连接蓝牙失败
     *
     * @param gatt
     * @param status
     * @param newState
     */
    public void onConnectFail(BluetoothGatt gatt, int status, int newState) {

    }

    /**
     * 蓝牙状态改变
     *
     * @param gatt
     * @param status
     * @param newState
     */
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

    }


    /**
     * 蓝牙收到通知
     *
     * @param gatt
     * @param value
     */
    public void onGlobalNotify(BluetoothGatt gatt, byte[] value) {

    }
}