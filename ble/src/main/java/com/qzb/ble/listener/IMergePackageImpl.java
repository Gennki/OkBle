package com.qzb.ble.listener;

import android.bluetooth.BluetoothGatt;
import android.util.Log;

import com.qzb.ble.utils.CRC8Util;

import java.util.ArrayList;
import java.util.List;

/**
 * 生活白科技-居家小确幸
 *
 * @ClassName: MergePackage
 * @Author: Leon.Qin
 * @Date: 2022/4/15 13:42
 * @Description:
 */
public class IMergePackageImpl implements IMergePackage {

    private static final int STATE_FRAME_HEAD = 1;           // 请求帧头
    private static final int STATE_FRAME_LENGTH = 2;         // 请求长度
    private static final int STATE_FRAME_SEND_NUM = 3;       // 数据是第几帧
    private static final int STATE_FRAME_DATA_TYPE = 4;      // 数据类型
    private static final int STATE_FRAME_DATA = 5;           // 数据内容
    private static final int STATE_FRAME_CHECKSUM = 6;       // 校验和

    private static final int DATA_TYPE_INDEX = 4;


    /**
     * 当前检查状态
     */
    private int currentState = STATE_FRAME_HEAD;
    private int byteIndex = 0;
    private int dataLength = 0;
    private final byte[] crc8Bytes = new byte[1024 * 10];
    private final StringBuilder sb = new StringBuilder();


    @Override
    public List<String> getHexList(BluetoothGatt gatt, byte[] bytes) {
        List<String> hexList = new ArrayList<>();
        for (int i = 0; i < bytes.length; i++, byteIndex++) {
            byte currentByte = bytes[i];
            crc8Bytes[byteIndex] = currentByte;
            switch (currentState) {
                case STATE_FRAME_HEAD:
                    reset();// 重置
                    if (currentByte == (byte) 0xF2) {
                        String hex = byte2Hex(currentByte);
                        sb.append(hex);
                        currentState = STATE_FRAME_LENGTH;
                    }
                    break;

                case STATE_FRAME_LENGTH:
                    byte[] lengthBytes = new byte[2];
                    byte nextByte = bytes[i + 1];
                    lengthBytes[1] = currentByte;
                    lengthBytes[0] = nextByte;
                    dataLength = bytes2Int(lengthBytes);
                    sb.append(byte2Hex(currentByte))
                            .append(byte2Hex(nextByte));
                    i++;// 数据长度占2字节，这里组装数据已经把下一字节一起组装掉了，因此i++跳过下一次循环
                    byteIndex++;
                    crc8Bytes[byteIndex] = nextByte;
                    currentState = STATE_FRAME_SEND_NUM;
                    break;

                case STATE_FRAME_SEND_NUM:
                    String hex = byte2Hex(currentByte);
                    sb.append(hex);
                    currentState = STATE_FRAME_DATA_TYPE;
                    break;

                case STATE_FRAME_DATA_TYPE:
                    hex = byte2Hex(currentByte);
                    sb.append(hex);
                    currentState = STATE_FRAME_DATA;
                    break;

                case STATE_FRAME_DATA:
                    if (byteIndex < DATA_TYPE_INDEX + dataLength) {
                        hex = byte2Hex(currentByte);
                        sb.append(hex);
                    } else if (byteIndex == DATA_TYPE_INDEX + dataLength) {
                        hex = byte2Hex(currentByte);
                        sb.append(hex);
                        currentState = STATE_FRAME_CHECKSUM;
                    } else {
                        reset();
                    }
                    break;
                case STATE_FRAME_CHECKSUM:
                    if (crc8Check(currentByte)) {
                        hexList.add(sb.toString());
                        // 如果需要收到的
//                        hexList.add(sb.substring(10, sb.length()));
                    }
                    reset();
                    break;
            }
        }

        return hexList;
    }

    private boolean crc8Check(byte currentByte) {
        byte[] crc8 = new byte[byteIndex];
        System.arraycopy(crc8Bytes, 0, crc8, 0, crc8.length);
        return CRC8Util.calcCrc8(crc8) == currentByte;
    }


    private void reset() {
        currentState = STATE_FRAME_HEAD;
        byteIndex = 0;
        dataLength = 0;
        sb.delete(0, sb.length());
    }


    private String byte2Hex(byte b) {
        String hex = Integer.toHexString(b & 0xFF);
        if (hex.length() < 2) {
            return "0" + Integer.toHexString(b & 0xFF);
        } else {
            return hex;
        }
    }

    private int bytes2Int(byte[] bytes) {
        if (bytes.length == 2) {
            int int1 = bytes[0] & 0xff;
            int int2 = (bytes[1] & 0xff) << 8;
            return int1 | int2;
        } else if (bytes.length == 4) {
            int int1 = bytes[0] & 0xff;
            int int2 = (bytes[1] & 0xff) << 8;
            int int3 = (bytes[2] & 0xff) << 16;
            int int4 = (bytes[3] & 0xff) << 24;
            return int1 | int2 | int3 | int4;
        } else {
            return 0;
        }
    }
}