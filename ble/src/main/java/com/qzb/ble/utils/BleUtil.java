package com.qzb.ble.utils;

import android.bluetooth.BluetoothDevice;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

/**
 * 生活白科技-居家小确幸
 *
 * @ClassName: DataChangeUtil
 * @Author: Kevin.Xu
 * @Date: 2021/8/12 11:21
 * @Description:
 */
public class BleUtil {
    //byte[]转十六进制字符串
    public static String byteToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        String strHex = "";
        StringBuilder sb = new StringBuilder("");
        for (int n = 0; n < bytes.length; n++) {
            strHex = Integer.toHexString(bytes[n] & 0xFF);
            sb.append((strHex.length() == 1) ? "0" + strHex : strHex); // 每个字节由两个字符表示，位数不够，高位补0
        }
        return sb.toString().toUpperCase().trim();
    }

    //byte转十六进制字符串
    public static String byteToHex(byte bytes) {
        StringBuilder sb = new StringBuilder("");
        String strHex = Integer.toHexString(bytes & 0xFF);
        if (strHex.length() < 2) {
            strHex = "0" + strHex;
        }
        return strHex.toUpperCase();
    }

    //十六进制字符串转byte[]
    public static byte[] hexStringToByteArray(String hexString) {
        hexString = hexString.replaceAll(" ", "");
        int len = hexString.length();
        byte[] bytes = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            // 两位一组，表示一个字节,把这样表示的16进制字符串，还原成一个字节
            bytes[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4) + Character
                    .digit(hexString.charAt(i + 1), 16));
        }
        return bytes;
    }

    //char 转byte
    public static byte charToByte(char c) {
        byte[] b = new byte[2];
        b[0] = (byte) ((c & 0xFF00) >> 8);
        b[1] = (byte) (c & 0xFF);
        return b[1];
    }

    public static char byteToChar(byte b0, byte b1) {
        char c = (char) (((b0 & 0xFF) << 8) | (b1 & 0xFF));
        return c;
    }

    //int 转byte
    public static byte intToByte(int i) {
        return (byte) (i & 0xFF);
    }

    //int 转byte数组
    public static byte[] toLH(long n) {
        byte[] b = new byte[4];
        b[0] = (byte) (n & 0xff);
        b[1] = (byte) (n >> 8 & 0xff);
        b[2] = (byte) (n >> 16 & 0xff);
        b[3] = (byte) (n >> 24 & 0xff);
        return b;
    }

    //int 转byte数组  取一位
    public static byte toLHOne(int n) {
        byte[] b = new byte[2];
        b[0] = (byte) (n & 0xff);
       /* b[1] = (byte) (n >> 8 & 0xff);
        b[2] = (byte) (n >> 16 & 0xff);
        b[3] = (byte) (n >> 24 & 0xff);*/
        return b[0];
    }


    //int 转byte数组  取两位 高位在前，低位在后
    public static byte[] toLHTwo(int n) {
        byte[] b = new byte[2];
        b[0] = (byte) (n >> 8 & 0xff);
        b[1] = (byte) (n & 0xff);
        return b;
    }

    //int 转byte数组  取两位  高位在前，低位在后
    public static byte[] toLHTwoRevert(int n) {
        byte[] b = new byte[2];
        b[1] = (byte) (n & 0xff);
        b[0] = (byte) (n >> 8 & 0xff);
        /* b[2] = (byte) (n >> 16 & 0xff);
        b[3] = (byte) (n >> 24 & 0xff);*/
        return b;
    }

    //int 转byte数组 由高位到低位
    public static byte[] toHL(int n) {
        byte[] b = new byte[4];
        b[0] = (byte) (n >> 24 & 0xff);
        b[1] = (byte) (n >> 16 & 0xff);
        b[2] = (byte) (n >> 8 & 0xff);
        b[3] = (byte) (n & 0xff);
        return b;
    }

    public static byte[] addBytes(byte[] data1, byte[] data2) {
        byte[] data3 = new byte[data1.length + data2.length];
        System.arraycopy(data1, 0, data3, 0, data1.length);
        System.arraycopy(data2, 0, data3, data1.length, data2.length);
        return data3;

    }

    //byte数组转int
    public static int bytes2Int(byte[] bytes) {
        int int1 = bytes[0] & 0xff;
        int int2 = (bytes[1] & 0xff) << 8;
        int int3 = (bytes[2] & 0xff) << 16;
        int int4 = (bytes[3] & 0xff) << 24;
        return int1 | int2 | int3 | int4;
    }

    //2字节byte数组转int
    public static int bytes2IntTwo(byte[] bytes) {
        int int1 = bytes[0] & 0xff;
        int int2 = (bytes[1] & 0xff) << 8;
        return int1 | int2;
    }

    /**
     * 取两个字节的byte数组所代表的short值
     *
     * @return short
     */
    public static short bytes2short(Byte byte0, Byte byte1) {
        byte[] bytes = new byte[2];
        bytes[0] = byte0;
        bytes[1] = byte1;
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getShort();
    }

    //byte[]倒序
    public static String byteInverted(byte[] bytes) {
        byte[] newbytes = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            newbytes[i] = bytes[bytes.length - 1 - i];
        }
        String byteString = byteToHex(newbytes);

        return byteString;
    }

    //int转十六进制字符串
    public static String intToHexString(int inData) {
        String outData = Integer.toHexString(inData);
        if (outData.length() < 4) {
            for (int j = outData.length(); j < 4; j++) {
                outData = outData + "0";
            }
        }
        return outData;
    }

    //字符串转十六进制字符串
    public static String stringToHexString(String inData) {
        String outData = toChineseHex(inData);
        if (outData.length() < 10) {
            for (int j = outData.length(); j < 10; j++) {
                outData = outData + "0";
            }
        }
        return outData;
    }


    //byte[] 数组转为数组 char[]
    public static char[] getChars(byte[] bytes) {
        Charset cs = Charset.forName("gb2312");
        ByteBuffer bb = ByteBuffer.allocate(bytes.length);
        bb.put(bytes);
        bb.flip();
        CharBuffer cb = cs.decode(bb);
        return cb.array();
    }

    //char[] 数组转为byte[] 数组
    public static byte[] getBytes(char[] chars) {
        Charset cs = Charset.forName("gb2312");
        CharBuffer cb = CharBuffer.allocate(chars.length);
        cb.put(chars);
        cb.flip();
        ByteBuffer bb = cs.encode(cb);
        return bb.array();
    }

    //bin文件内容转为byte数组
    public static byte[] getContent(String filePath) throws IOException {
        File file = new File(filePath);
        long fileSize = file.length();
        if (fileSize > Integer.MAX_VALUE) {
            System.out.println("file too big...");
            return null;
        }
        FileInputStream fi = new FileInputStream(file);
        byte[] buffer = new byte[(int) fileSize];
        int offset = 0;
        int numRead = 0;
        while (offset < buffer.length
                && (numRead = fi.read(buffer, offset, buffer.length - offset)) >= 0) {
            offset += numRead;
        }
        // 确保所有数据均被读取
        if (offset != buffer.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }
        fi.close();
        return buffer;
    }

    public static String toChineseHex(String s) {
        String ss = s;
        byte[] bt = new byte[0];

        try {
            bt = ss.getBytes("GBK");
        } catch (Exception e) {
            e.printStackTrace();
        }
        String s1 = "";
        for (int i = 0; i < bt.length; i++) {
            String tempStr = Integer.toHexString(bt[i]);
            if (tempStr.length() > 2)
                tempStr = tempStr.substring(tempStr.length() - 2);
            s1 = s1 + tempStr + "";
        }
        return s1.toUpperCase();
    }

    //十六进制的字符串转字符
    public static String hexStr2Str(String hexString) {
        hexString = hexString.toUpperCase();
        String str = "0123456789ABCDEF";
        char[] hexs = hexString.toCharArray();
        byte[] hexBytes = new byte[hexString.length() / 2];
        int n;
        for (int i = 0; i < hexBytes.length; i++) {
            n = str.indexOf(hexs[2 * i]) * 16;
            n += str.indexOf(hexs[2 * i + 1]);
            hexBytes[i] = (byte) (n & 0xff);
        }
        try {
            return new String(hexBytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return new String(hexBytes);
        }
    }


    public static long bytes2Long(byte[] bytes) {
        long int1 = bytes[0] & 0xff;
        long int2 = (bytes[1] & 0xff) << 8;
        long int3 = (bytes[2] & 0xff) << 16;
        long int4 = (bytes[3] & 0xff) << 24;
        return int1 | int2 | int3 | int4;
    }


    public static byte[] setDataByteArray(String sendJson, int sendNum) {
        char modelData0 = 0xF1;
        byte[] dataStyle = "j".getBytes();
        byte dataLength[] = BleUtil.toLHTwo(sendJson.length());//数据长度

        byte[] sendData = sendJson.getBytes();

        byte[] preData = new byte[5];
        preData[0] = BleUtil.charToByte(modelData0);
        preData[1] = dataLength[0];
        preData[2] = dataLength[1];
        preData[3] = BleUtil.toLHOne(sendNum);
        preData[4] = dataStyle[0];
        byte[] totalSendData = BleUtil.addBytes(preData, sendData);

        byte[] crc8Bytes = new byte[1];
        crc8Bytes[0] = CRC8Util.calcCrc8(totalSendData);
        totalSendData = BleUtil.addBytes(totalSendData, crc8Bytes);
        return totalSendData;
    }


    /**
     * 1k对齐
     * 例如发送一个bin文件，判断bin文件byte数组是否正好是1024的倍数，如果不是，需要补0
     */
    public static byte[] bytesTo1KAlignment(byte[] bytes) {
        int remainder = bytes.length % 1024;
        byte[] totalBytes;
        if (remainder != 0) {
            totalBytes = new byte[(bytes.length / 1024 + 1) * 1024];
            for (int i = 0; i < totalBytes.length - 1; i++) {
                if (i < bytes.length) {
                    totalBytes[i] = bytes[i];
                } else {
                    totalBytes[i] = 0x00;
                }
            }
        } else {
            totalBytes = bytes;
        }
        return totalBytes;
    }


    /**
     * 当前蓝牙设备是否连接
     */
    public static boolean isDeviceConnected(BluetoothDevice device) {
        try {
            Class bluetoothDeviceClass = device.getClass();
            Method method = bluetoothDeviceClass.getDeclaredMethod("isConnected");
            method.setAccessible(true);
            return (boolean) method.invoke(device);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }
}
