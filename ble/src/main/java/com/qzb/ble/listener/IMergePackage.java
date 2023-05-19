package com.qzb.ble.listener;

import android.bluetooth.BluetoothGatt;

import java.util.List;

/**
 * 生活白科技-居家小确幸
 *
 * @ClassName: CheckBytesImp
 * @Author: Leon.Qin
 * @Date: 2022/4/15 13:39
 * @Description:
 */
public interface IMergePackage {
    List<String> getHexList(BluetoothGatt gatt, byte[] bytes);
}