package com.qzb.ble.listener;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

/**
 * 生活白科技-居家小确幸
 *
 * @ClassName: IResponseIml
 * @Author: Leon.Qin
 * @Date: 2022/3/23 10:19
 * @Description:
 */
public class IResponseImpl implements IResponse {
    @Override
    public void onWriteSuccess(BluetoothGatt gatt, byte[] value) {

    }

    @Override
    public void onWriteFailed(BluetoothGatt gatt, byte[] value) {

    }

    @Override
    public void onNotify(BluetoothGatt gatt, byte[] value) {

    }
}