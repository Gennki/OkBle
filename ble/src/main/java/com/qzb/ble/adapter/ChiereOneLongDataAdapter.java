package com.qzb.ble.adapter;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.qzb.ble.utils.BleUtil;
import com.qzb.ble.utils.CRC8Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 生活白科技-居家小确幸
 *
 * @ClassName: ChiereOneLongDataAdapter
 * @Author: Leon.Qin
 * @Date: 2022/3/30 17:14
 * @Description:
 */
public class ChiereOneLongDataAdapter extends BaseLongDataAdapter {
    private final static String TAG = ChiereOneLongDataAdapter.class.getSimpleName();

    @Override
    public ArrayList<byte[]> splitDataList(byte[] bytes) {

        // 1k对齐
        byte[] alimentBytes = BleUtil.bytesTo1KAlignment(bytes);
        // 计算一共可以分成多少片
        int splitCount = alimentBytes.length / 1024;
        ArrayList<byte[]> splitList = new ArrayList<>();
        // 每一片添加片头和校验和等信息
        for (int i = 0; i < splitCount; i++) {
            byte[] dataLength = BleUtil.toLHTwo(1026); //数据长度
            byte[] headerBytes = new byte[5];
            headerBytes[0] = BleUtil.intToByte(0xF1);
            headerBytes[1] = dataLength[0];
            headerBytes[2] = dataLength[1];
            headerBytes[3] = BleUtil.toLHOne(i + 1);
            headerBytes[4] = "b".getBytes()[0];// 数据类型为byte数组
            byte[] splitIndexBytes = BleUtil.toLHTwo(i); // 第几片数据
            // 合并数据
            byte[] contentBytes = Arrays.copyOfRange(alimentBytes, i * 1024, (i + 1) * 1024);
            contentBytes = BleUtil.addBytes(splitIndexBytes, contentBytes);
            byte[] splitBytes = BleUtil.addBytes(headerBytes, contentBytes);
            // 计算CRC8校验
            byte[] crc8Bytes = new byte[1];
            crc8Bytes[0] = CRC8Util.calcCrc8(splitBytes);
            // 将校验和也合并到数据中
            splitBytes = BleUtil.addBytes(splitBytes, crc8Bytes);
            splitList.add(splitBytes);
        }
        return splitList;
    }
    @Override
    public boolean splitFilter(byte[] bytes) {
        return BleUtil.byteToHex(bytes).toUpperCase().startsWith("F2") &&
                BleUtil.byteToHex(bytes).substring(8, 10).equalsIgnoreCase("62");
    }

}