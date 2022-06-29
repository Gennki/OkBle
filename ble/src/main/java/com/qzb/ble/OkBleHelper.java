package com.qzb.ble;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.qzb.ble.adapter.BaseLongDataAdapter;
import com.qzb.ble.listener.BleListener;
import com.qzb.ble.listener.IFilter;
import com.qzb.ble.listener.IMergePackage;
import com.qzb.ble.listener.IMergePackageImpl;
import com.qzb.ble.listener.IResponse;
import com.qzb.ble.utils.BleUtil;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class OkBleHelper {

    private final String serviceUUID;
    private final String writeUUID;
    private final String notifyUUID;
    private final int writeRetryTimes;
    private final int writeLongRetryTimes;
    private final long writeTimeout;
    private final long writeLongTimeout;
    private final IMergePackage mergePackage;

    private static Context appContext;
    private final BluetoothAdapter bluetoothAdapter;


    private static final int HANDLER_WRITE_FAIL_CALLBACK = 1;
    private static final int HANDLER_WRITE_SUCCESS_CALLBACK = 2;


    // 蓝牙设备的全局监听
    private final HashMap<Context, BleListener> bleListenerMap = new HashMap<>();
    // 蓝牙设备的写入监听
    private final List<WriteBean> writeCallbackList = new CopyOnWriteArrayList<>();


    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case HANDLER_WRITE_FAIL_CALLBACK:
                    WriteBean writeBean = (WriteBean) msg.obj;
                    writeBean.getResponse().onWriteFailed(writeBean.getBluetoothGatt(), writeBean.getValue());
                    break;
                case HANDLER_WRITE_SUCCESS_CALLBACK:
                    writeBean = (WriteBean) msg.obj;
                    writeBean.getResponse().onNotify(writeBean.getBluetoothGatt(), writeBean.getValue());
                    break;

            }
        }
    };

    public OkBleHelper(Context context, String serviceUUID, String writeUUID, String notifyUUID, int writeRetryTimes, int writeLongRetryTimes, long writeTimeout, long writeLongTimeout, IMergePackage mergePackage) {
        appContext = context.getApplicationContext();
        this.serviceUUID = serviceUUID;
        this.writeUUID = writeUUID;
        this.notifyUUID = notifyUUID;
        this.writeRetryTimes = writeRetryTimes;
        this.writeLongRetryTimes = writeLongRetryTimes;
        this.writeTimeout = writeTimeout;
        this.writeLongTimeout = writeLongTimeout;
        this.mergePackage = mergePackage;

        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // 开启一个1秒执行1次的定时器，判断写入的数据在规定时间内是否响应，做超时判断
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (writeCallbackList) {
                    Iterator<WriteBean> iterator = writeCallbackList.iterator();
                    while (iterator.hasNext()) {
                        WriteBean writeBean = iterator.next();
                        writeBean.setTimeout(writeBean.getTimeout() - 1000);
                        if (writeBean.getTimeout() <= 0) {
                            if (writeBean.getRetryTimes() <= 0) {
                                // 如果倒计时到了0，并且重试次数也到了0，就判断为发送失败了
                                Message msg = Message.obtain();
                                msg.what = HANDLER_WRITE_FAIL_CALLBACK;
                                msg.obj = writeBean;
                                handler.sendMessage(msg);
                                writeCallbackList.remove(writeBean);
                            } else {
                                // 如果倒计时到了0，但重试次数未到0，就重新发送
                                writeCallbackList.remove(writeBean);
                                write(writeBean.getBluetoothGatt(), writeBean.getValue(), writeBean.getOriginTimeout(), writeBean.getRetryTimes() - 1, writeBean.getFilter(), writeBean.getResponse());
                            }
                        }
                    }
                }
            }
        }, 1000, 1000);
    }


    /**
     * 获取蓝牙适配器
     *
     * @return 返回蓝牙适配器，可能为null
     */
    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    /**
     * 判断当前设备是否支持BLE
     *
     * @return 如果设备支持Ble，则返回true
     */
    public static boolean isDeviceSupportBle() {
        return appContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    /**
     * 启用蓝牙
     * <p>
     * 如果设备支持 BLE 但已停用此功能，则您可以请求用户在不离开应用的同时启用蓝牙调用此方法前请先判断是否拥有
     * Manifest.permission.BLUETOOTH_SCAN(蓝牙扫描)，
     * Manifest.permission.BLUETOOTH_CONNECT(蓝牙匹配后通信)，
     * Manifest.permission.BLUETOOTH_ADVERTISE（当前设备蓝牙是否可以被其他设备发现）
     * 以上三个权限,通过ActivityCompat.requestPermissions动态请求权限，
     * 并重载Activity的onRequestPermissionsResult方法
     * <p/>
     *
     * @param activity    当前activity
     * @param requestCode 开启成功后，在onActivityResult回调中对应的请求码
     */
    public void openBle(Activity activity, int requestCode) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Logger.i("缺少Manifest.permission.BLUETOOTH_CONNECT权限");
                return;
            }
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, requestCode);
        }
    }


    /**
     * 直接打开蓝牙，无法自定义弹窗提示用户（不同系统可能处理方式不同，小米手机默认会有一个弹窗）
     * 建议使用 {@link #openBle(Activity, int)}
     */
    public void openBle() {
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Logger.i("缺少Manifest.permission.BLUETOOTH_CONNECT权限");
                return;
            }
            bluetoothAdapter.enable();
        }
    }


    /**
     * 打开扫描
     *
     * @param callback 扫描回调
     */
    public void startScanDevice(ScanCallback callback) {
        startScanDevice(null, new ScanSettings.Builder().build(), callback);
    }


    /**
     * 打开扫描
     *
     * @param filters  过滤条件
     * @param settings 扫描设置
     * @param callback 扫描回调
     */
    public void startScanDevice(List<ScanFilter> filters, ScanSettings settings, ScanCallback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Logger.i("缺少Manifest.permission.BLUETOOTH_SCAN权限");
            return;
        }
        if (bluetoothAdapter == null) {
            Logger.i("设备未开启蓝牙");
            return;
        }
        BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bluetoothLeScanner == null) {
            Logger.i("设备未开启蓝牙或正在开启中");
            return;
        }
        bluetoothLeScanner.startScan(filters, settings, callback);
    }


    /**
     * 关闭扫描
     *
     * @param scanCallback 停止扫描回调
     */
    public void stopScanDevice(ScanCallback scanCallback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Logger.i("缺少Manifest.permission.BLUETOOTH_SCAN权限");
            return;
        }
        if (bluetoothAdapter == null) {
            Logger.i("设备未开启蓝牙");
            return;
        }
        BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        bluetoothLeScanner.stopScan(scanCallback);
    }


    /**
     * 根据mac地址链接蓝牙
     *
     * @param macAddress 要连接设备的mac地址
     * @return 返回gatt
     */
    public BluetoothGatt connectDevice(@NonNull String macAddress) {
        if (bluetoothAdapter == null) {
            Logger.i("设备未开启蓝牙");
            return null;
        }
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(macAddress);
        return connectDevice(device);
    }

    /**
     * 连接指定设备
     *
     * @param device 要连接的设备
     * @return 返回gatt
     */
    public BluetoothGatt connectDevice(@NonNull BluetoothDevice device) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Logger.i("缺少Manifest.permission.BLUETOOTH_CONNECT权限");
            return null;
        }
        return device.connectGatt(appContext, false, new BluetoothGattCallback() {
            @SuppressLint("MissingPermission")
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                switch (newState) {
                    case BluetoothProfile.STATE_CONNECTED:
                        Logger.i("服务连接成功");
                        gatt.discoverServices();
                        break;
                    case BluetoothProfile.STATE_DISCONNECTED:
                        // 断开蓝牙后，释放资源
                        gatt.close();
                        break;
                }
                for (BleListener listener : bleListenerMap.values()) {
                    listener.onConnectionStateChange(gatt, status, newState);

                    // 连接失败
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        listener.onConnectFail(gatt, status, newState);
                    }
                }

            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {  // 发现服务
                super.onServicesDiscovered(gatt, status);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Logger.i("缺少Manifest.permission.BLUETOOTH_CONNECT权限");
                    return;
                }
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    BluetoothGattService service = gatt.getService(UUID.fromString(serviceUUID));
                    BluetoothGattCharacteristic notifyCharacteristic = service.getCharacteristic(UUID.fromString(notifyUUID));
                    gatt.setCharacteristicNotification(notifyCharacteristic, true);
                    for (BluetoothGattDescriptor descriptor : notifyCharacteristic.getDescriptors()) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);
                    }
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                synchronized (writeCallbackList) {
                    // 防止数据粘连
                    List<String> hexList;
                    if (mergePackage != null) {
                        hexList = mergePackage.getHexList(characteristic.getValue());
                    } else {
                        hexList = Collections.singletonList(BleUtil.byteToHex(characteristic.getValue()));
                    }
                    for (String hex : hexList) {
                        // 收到通知
                        Logger.e("收到数据：" + hex);
                        byte[] hexBytes = BleUtil.hexStringToByteArray(hex);
                        characteristic.setValue(hexBytes);

                        // 通知全局
                        for (BleListener listener : bleListenerMap.values()) {
                            listener.onGlobalNotify(gatt, characteristic.getValue());
                        }

                        // 通知订阅者
                        Iterator<WriteBean> iterator = writeCallbackList.iterator();
                        while (iterator.hasNext()) {
                            WriteBean writeBean = iterator.next();
                            if (writeBean.getFilter().filter(hexBytes)) {
                                Message msg = Message.obtain();
                                msg.what = HANDLER_WRITE_SUCCESS_CALLBACK;
                                writeBean.setValue(characteristic.getValue());
                                msg.obj = writeBean;
                                handler.sendMessage(msg);
                                writeCallbackList.remove(writeBean);
                                break;
                            }
                        }
                    }
                }
            }
        });
    }


    /**
     * 只发送一次消息，不考虑超时和延迟
     */
    public void writeOneTime(BluetoothGatt gatt, byte[] bytes) {
        write(gatt, bytes, null, null);
    }

    /**
     * 发送信息
     */
    public void write(BluetoothGatt gatt, byte[] bytes) {
        write(gatt, bytes, new IFilter() {
            @Override
            public boolean filter(byte[] bytes) {
                return true;
            }
        }, new IResponse() {

            @Override
            public void onWriteSuccess(BluetoothGatt gatt, byte[] value) {

            }

            @Override
            public void onWriteFailed(BluetoothGatt gatt, byte[] value) {

            }

            @Override
            public void onNotify(BluetoothGatt gatt, byte[] value) {

            }
        });
    }


    /**
     * 发送信息
     */
    public void write(BluetoothGatt gatt, byte[] bytes, @Nullable IFilter iFilter, @Nullable IResponse iResponse) {
        write(gatt, bytes, writeTimeout, writeRetryTimes, iFilter, iResponse);
    }

    public void write(BluetoothGatt gatt, byte[] bytes, long timeout, int retryTimes, @Nullable IFilter iFilter, @Nullable IResponse iResponse) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Logger.i("缺少Manifest.permission.BLUETOOTH_CONNECT权限");
            return;
        }
        if (gatt == null) {
            Logger.i("gatt服务未连接");
            return;
        }
        BluetoothGattService service = gatt.getService(UUID.fromString(serviceUUID));
        BluetoothGattCharacteristic writeCharacteristic = service.getCharacteristic(UUID.fromString(writeUUID));
        writeCharacteristic.setValue(bytes);

        if (gatt.writeCharacteristic(writeCharacteristic)) {
            Logger.e("发送数据成功：" + BleUtil.byteToHex(writeCharacteristic.getValue()));
            // 加入回调列表，定时器会定期处理超时任务
            if (iFilter != null && iResponse != null) {
                iResponse.onWriteSuccess(gatt, writeCharacteristic.getValue());
                writeCallbackList.add(new WriteBean(timeout, retryTimes, iFilter, iResponse, gatt, writeCharacteristic.getValue()));
            }
        } else {
            Logger.e("发送数据失败：" + BleUtil.byteToHex(writeCharacteristic.getValue()));
            if (iResponse != null) {
                iResponse.onWriteFailed(gatt, writeCharacteristic.getValue());
            }
        }
    }


    /**
     * 写长数据
     */
    public Thread writeLong(BluetoothGatt gatt, byte[] bytes, @NonNull BaseLongDataAdapter baseLongDataAdapter) {
        // 蓝牙每次传输有最大长度上限，因此需要将数据分片发送
        ArrayList<byte[]> splitList = baseLongDataAdapter.splitDataList(bytes);
        // 发送切好片的数据
        Thread thread = new Thread(() -> {
            try {
                writeSplit(gatt, splitList, baseLongDataAdapter::splitFilter, baseLongDataAdapter);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        thread.start();
        return thread;
    }


    /**
     * 1k的数据分成5个包
     * 1k指的是真正数据内容的1k，不包含和嵌入式约定的请求头和校验和
     * 实际情况，每片数据会大于1024字节,但不会大于太多
     *
     * @param splitList           所有需要发送的数据片
     * @param splitFilter         每发完一片数据后，判断是否成功的回调
     * @param baseLongDataAdapter 发送数据的适配器，里面有些必须要实现的规则，根据规则来判断如何去发送数据
     */
    private void writeSplit(BluetoothGatt gatt, List<byte[]> splitList, IFilter splitFilter, @NonNull BaseLongDataAdapter baseLongDataAdapter) throws InterruptedException {
        // 当前片是否发送成功
        final boolean[] isCurrentSplitWriteFinished = {true};
        // 当前处于第几片
        final int[] currentSplitIndex = {0};
        // 重试次数
        final int[] retryCount = {0};
        while (currentSplitIndex[0] < splitList.size()) {
            // 并发处理，防止资源抢占，这里需要上锁
            synchronized (currentSplitIndex) {
                if (currentSplitIndex[0] >= splitList.size()) {
                    return;
                }
                if (!isCurrentSplitWriteFinished[0]) {
                    continue;
                } else {
                    isCurrentSplitWriteFinished[0] = false;
                }

                byte[] currentSplitBytes = splitList.get(currentSplitIndex[0]);
                int packageLength;
                if (currentSplitBytes.length % 5 == 0) {
                    // 正好分成5个包
                    packageLength = currentSplitBytes.length / 5;
                } else {
                    // 如果没有正好分成5个包,计算每个包的大小，最后一个包长度不够也没关系
                    packageLength = currentSplitBytes.length / 5 + 1;
                }


                // 分成5个包，每次发送1个包
                for (int i = 0; i < 5; i++) {
                    Thread.sleep(200);
                    if (i < 4) {
                        byte[] packageArr = Arrays.copyOfRange(currentSplitBytes, i * packageLength, (i + 1) * packageLength);
                        write(gatt, packageArr, writeLongTimeout, writeLongRetryTimes, null, null);
                    } else {
                        byte[] packageArr = Arrays.copyOfRange(currentSplitBytes, i * packageLength, currentSplitBytes.length);
                        write(gatt, packageArr, writeLongTimeout, writeLongRetryTimes, splitFilter, new IResponse() {
                            @Override
                            public void onWriteSuccess(BluetoothGatt gatt, byte[] value) {

                            }

                            @Override
                            public void onWriteFailed(BluetoothGatt gatt, byte[] value) {
                                synchronized (currentSplitIndex) {
                                    if (retryCount[0] >= 3) {
                                        BaseLongDataAdapter.OnWriteLongListener onWriteLongListener = baseLongDataAdapter.getOnWriteLongCallback();
                                        if (onWriteLongListener != null) {
                                            onWriteLongListener.onWriteFailed(gatt, value);
                                        }
                                        // 让当前的splitIndex直接移到最后，以此来跳出循环，结束本次发送长数据
                                        currentSplitIndex[0] = splitList.size();
                                    } else {
                                        // 如果重试次数还没有大于3次，则不修改当前片数，但是循环次数+1，继续循环
                                        retryCount[0] += 1;
                                    }
                                    isCurrentSplitWriteFinished[0] = true;
                                }
                            }

                            @Override
                            public void onNotify(BluetoothGatt gatt, byte[] value) {
                                // 发送数据片成功
                                synchronized (currentSplitIndex) {
                                    BaseLongDataAdapter.OnWriteLongListener onWriteLongListener = baseLongDataAdapter.getOnWriteLongCallback();
                                    if (onWriteLongListener != null) {
                                        onWriteLongListener.onProgress(splitList.size() - 1 - currentSplitIndex[0]);// 更新进度
                                    }
                                    if (currentSplitIndex[0] == splitList.size() - 1) {
                                        // 所有数据片发送成功
                                        if (onWriteLongListener != null) {
                                            onWriteLongListener.onWriteSuccess(gatt, value);
                                        }
                                    }
                                    // 继续循环，如果还有数据片没法送，就发送下一片数据，否则就会推出循环
                                    currentSplitIndex[0]++;
                                    isCurrentSplitWriteFinished[0] = true;
                                }
                            }
                        });
                    }
                }
            }
        }
    }


    /**
     * 断开蓝牙连接
     */
    public void disConnect(BluetoothGatt bluetoothGatt) {
        if (bluetoothGatt == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Logger.i("缺少Manifest.permission.BLUETOOTH_CONNECT权限");
            return;
        }
        bluetoothGatt.disconnect();
    }


    /**
     * 添加蓝牙状态和读写的回调
     */
    public void addBleListener(Context context, BleListener listener) {
        if (bleListenerMap.containsKey(context)) {
            return;
        }
        bleListenerMap.put(context, listener);
    }

    /**
     * 移除蓝牙状态和读写的回调
     */
    public void removeBleListener(Context context) {
        bleListenerMap.remove(context);
    }


    public String getServiceUUID() {
        return serviceUUID;
    }

    public String getWriteUUID() {
        return writeUUID;
    }

    public String getNotifyUUID() {
        return notifyUUID;
    }

    public int getWriteRetryTimes() {
        return writeRetryTimes;
    }

    public int getWriteLongRetryTimes() {
        return writeLongRetryTimes;
    }

    public long getWriteTimeout() {
        return writeTimeout;
    }

    public long getWriteLongTimeout() {
        return writeLongTimeout;
    }

    public static class Builder {
        private final Context context;
        private String serviceUUID = "";
        private String writeUUID = "";
        private String notifyUUID = "";
        private int writeRetryTimes = 0;
        private int writeLongRetryTimes = 0;
        private long writeTimeout = 3000;
        private long writeLongTimeout = 3000;
        private IMergePackage mergePackage;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder serviceUUID(String serviceUUID) {
            this.serviceUUID = serviceUUID;
            return this;
        }

        public Builder writeUUID(String writeUUID) {
            this.writeUUID = writeUUID;
            return this;
        }

        public Builder notifyUUID(String notifyUUID) {
            this.notifyUUID = notifyUUID;
            return this;
        }

        public Builder writeRetryTime(int writeRetryTimes) {
            this.writeRetryTimes = writeRetryTimes;
            return this;
        }

        public Builder writeLongRetryTimes(int writeLongRetryTimes) {
            this.writeLongRetryTimes = writeLongRetryTimes;
            return this;
        }

        public Builder writeTimeout(int writeTimeout) {
            this.writeTimeout = writeTimeout;
            return this;
        }


        public Builder writeLongTimeout(int writeLongTimeout) {
            this.writeLongTimeout = writeLongTimeout;
            return this;
        }

        public Builder mergePackage(IMergePackage mergePackage) {
            this.mergePackage = mergePackage;
            return this;
        }


        public OkBleHelper build() {
            return new OkBleHelper(context, serviceUUID, writeUUID, notifyUUID, writeRetryTimes, writeLongRetryTimes, writeTimeout, writeLongTimeout, mergePackage);
        }
    }

}