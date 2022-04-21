package com.qzb.ble.listener;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

/**
 * 生活白科技-居家小确幸
 *
 * @ClassName: IWriteCallback
 * @Author: Leon.Qin
 * @Date: 2022/3/23 10:15
 * @Description:
 */
public interface IResponse {
    /**
     * 蓝牙发送数据成功
     *
     * @param gatt
     * @param characteristic
     * @param status
     */
    public void onWriteSuccess(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);

    /**
     * 蓝牙发送数据失败
     *
     * @param gatt
     * @param characteristic
     * @param status
     */
    public void onWriteFailed(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);


    /**
     * 蓝牙收到通知
     *
     * @param gatt
     * @param characteristic
     */
    public void onNotify(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);
}