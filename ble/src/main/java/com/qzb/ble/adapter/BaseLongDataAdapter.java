package com.qzb.ble.adapter;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import java.util.ArrayList;

/**
 * 生活白科技-居家小确幸
 *
 * @ClassName: IPrepareLongData
 * @Author: Leon.Qin
 * @Date: 2022/3/24 8:45
 * @Description:
 */
public abstract class BaseLongDataAdapter {

    private OnWriteLongListener onWriteLongListener;

    public interface OnWriteLongListener {
        /**
         * 进度回调
         *
         * @param leftTimeInSecond 剩余多少秒
         */
        public void onProgress(long leftTimeInSecond);


        public void onWriteFailed(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);

        public void onWriteSuccess(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);
    }

    /**
     * 自定义分包规则
     *
     * @param bytes 发送的原始数据内容
     * @return 返回分好片的数据内容
     */
    public abstract ArrayList<byte[]> splitDataList(byte[] bytes);

    /**
     * 发送完一片数据后，自定义规则判断当前片是否发送成功
     */
    public abstract boolean splitFilter(byte[] bytes);

    public OnWriteLongListener getOnWriteLongCallback() {
        return onWriteLongListener;
    }

    public void setOnWriteLongCallback(OnWriteLongListener onWriteLongListener) {
        this.onWriteLongListener = onWriteLongListener;
    }
}