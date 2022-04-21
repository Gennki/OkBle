package com.qzb.ble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.qzb.ble.listener.IFilter;
import com.qzb.ble.listener.IResponse;

/**
 * 生活白科技-居家小确幸
 *
 * @ClassName: WriteBean
 * @Author: Leon.Qin
 * @Date: 2022/3/23 10:21
 * @Description:
 */
public class WriteBean {
    private IFilter filter;
    private IResponse response;
    public long originTimeout;
    private long timeout; // 默认三秒超时
    private int retryTimes;// 发送失败后的重试次数
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic characteristic;

    public WriteBean(long timeout, int retryTimes, IFilter filter, IResponse response, BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic characteristic) {
        this.originTimeout = timeout;
        this.timeout = timeout;
        this.retryTimes = retryTimes;
        this.filter = filter;
        this.response = response;
        this.bluetoothGatt = bluetoothGatt;
        this.characteristic = characteristic;
    }


    public IFilter getFilter() {
        return filter;
    }

    public void setFilter(IFilter filter) {
        this.filter = filter;
    }

    public IResponse getResponse() {
        return response;
    }

    public void setResponse(IResponse response) {
        this.response = response;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public BluetoothGatt getBluetoothGatt() {
        return bluetoothGatt;
    }

    public void setBluetoothGatt(BluetoothGatt bluetoothGatt) {
        this.bluetoothGatt = bluetoothGatt;
    }

    public BluetoothGattCharacteristic getCharacteristic() {
        return characteristic;
    }

    public void setCharacteristic(BluetoothGattCharacteristic characteristic) {
        this.characteristic = characteristic;
    }

    public int getRetryTimes() {
        return retryTimes;
    }

    public void setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
    }

    public long getOriginTimeout() {
        return originTimeout;
    }

    public void setOriginTimeout(long originTimeout) {
        this.originTimeout = originTimeout;
    }
}