# OkBle

## 简介

`OkBle`是一个BLE蓝牙框架，支持以下功能

- 自定义超时时长
- 发送数据超时重发
- 自定义重发次数



## 基本使用

### 添加依赖

在项目根目录的`gradle`文件中添加

```groovy
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```

在`app`模块下的`gradle`中添加依赖

```groovy
dependencies {
	implementation 'com.github.Gennki:OkBle:v1.0.8'
}
```



### 初始化

在`Application`中进行初始化

```java
public class MyApplication extends Application {

    public static final String SERVICE_UUID = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx";
    public static final String NOTIFY_UUID = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx";
    public static final String WRITE_UUID = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx";

    private OkBleHelper okBleHelper;

    @Override
    public void onCreate() {
        super.onCreate();

        okBleHelper = new OkBleHelper.Builder(this)
        	// 设置UUID
                .serviceUUID(SERVICE_UUID)
                .writeUUID(WRITE_UUID)
                .notifyUUID(NOTIFY_UUID)
              	.mtu(512)// 设置mtu
                .writeTimeout(1000)// 设置短数据读写超时1000毫秒
                .writeLongTimeout(3000)// 设置长数据读写超时3000毫秒
                .writeRetryTime(3)// 设置短数据超时后重发次数
                .writeLongRetryTimes(3)// 设置长数据超时后重发次数
                .build();
    }


    public OkBleHelper getOkBleHelper() {
        return okBleHelper;
    }
}
```

我们需要自己维护`OkBleHelper`，**每一个蓝牙设备都会对应一个`OkBleHelper`实例**



### 扫描蓝牙

1. 普通蓝牙扫描

   ```java
   okBleHelper.startScanDevice(ScanCallback callback)
   ```

2. 蓝牙扫描设置过滤条件

   ```java
   okBleHelper.startScanDevice(List<ScanFilter> filters, ScanSettings settings, ScanCallback callback)
   ```

3. 停止蓝牙扫描

   ```java
   okBleHelper.stopScanDevice(ScanCallback scanCallback)
   ```

   



### 蓝牙连接

```java
BluetoothGatt gatt = okBleHelper.connectDevice(@NonNull String macAddress)
// 或者
BluetoothGatt gatt = okBleHelper.connectDevice(@NonNull BluetoothDevice device)
```

连接蓝牙会返回一个`BluetoothGatt`对象，需要自己维护好，读写和断开蓝牙连接时需要用到



### 断开蓝牙连接

```java
okBleHelper.disConnect(BluetoothGatt bluetoothGatt)
```



### 蓝牙数据读写

1. 最简单的发送方法

   ```java
   okBleHelper.write(BluetoothGatt gatt, byte[] bytes)
   ```

   

2. 只发送一次数据，不管底层是否收到。哪怕`Application`中设置了超时和重发，也不会生效

   ```java
   okBleHelper.writeOneTime(BluetoothGatt gatt, byte[] bytes)
   ```



3. 发送数据，并且根据`filter`条件获取响应

   ```java
   okBleHelper.write(BluetoothGatt gatt, byte[] bytes, @Nullable IFilter iFilter, @Nullable IResponse iResponse)
   ```

   例如：

   ```java
   try {
       JSONObject jsonObject = new JSONObject();
       jsonObject.put("version", 1);
       chiereOneBleHelper.write(bluetoothGatt, BleUtil.setDataByteArray(jsonObject.toString(), 1), new IFilter() {
           @Override
           public boolean filter(byte[] bytes) {
               return true;
           }
       }, new IResponse() {
           @Override
           public void onWriteSuccess(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
               stateTV.setText("发送数据成功:" + BleUtil.byteToHex(characteristic.getValue()));
           }
           @Override
           public void onWriteFailed(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
               stateTV.setText("发送数据失败:" + BleUtil.byteToHex(characteristic.getValue()));
           }
           @Override
           public void onNotify(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
               stateTV.setText("获取数据:" + BleUtil.byteToHex(characteristic.getValue()));
           }
       });
   } catch (Exception e) {
       e.printStackTrace();
   }
   ```

   其中`filter`方法直接返回`true`，代表不作任何过滤，底层的任何消息发送到`Android`端，都会进`onNotify`回调

   也可以通过`filter`对底层回调到`onNotify`的数据作出限制，例如必须返回以`0xF100`开头的`byte`数组，才会回调到`onNotify`,和底层商定好后，可以通过`filter`方法，来使发送数据和收到的数据一一对应

   

4. 发送数据，根据`filter`条件来获取响应数据，并自定义超时时长以及重发次数，如果`Application`中也设置了超时时长和重试次数，则优先使用方法中指定的参数

   ```java
   okBleHelper.write(BluetoothGatt gatt, byte[] bytes, long timeout, int retryTimes, @Nullable IFilter iFilter, @Nullable IResponse iResponse)
   ```

   

### 状态监听

```java
okBleHelper.addBleListener(Context context, BleListener listener)
```

我们可以通过此`api`来监听蓝牙的各庄状态以及数据回调，例如：

```java
okBleHelper.addBleListener(this, new BleListener() {
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        switch (newState) {
            case BluetoothProfile.STATE_CONNECTING:
                Log.e(TAG, "连接GATT服务器中");
                break;
            case BluetoothProfile.STATE_CONNECTED:
                Log.e(TAG, "GATT服务器连接成功");
                break;
            case BluetoothProfile.STATE_DISCONNECTED:
                Log.e(TAG, "GATT服务器已断开");
                break;
            case BluetoothProfile.STATE_DISCONNECTING:
                Log.e(TAG, "GATT服务器断开中");
                break;
        }
    }
    @Override
    public void onConnectFail(BluetoothGatt gatt, int status, int newState) {
        super.onConnectFail(gatt, status, newState);
        Log.e(TAG, "连接GATT服务器失败");
    }
    @Override
    public void onGlobalNotify(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onGlobalNotify(gatt, characteristic);
        Log.e(TAG, "全局获取数据:" + BleUtil.byteToHex(characteristic.getValue()));
    }
});
```

如果是在`Activity`或者`Fragment`中使用此`api`，需要记得在`onDestroy`时解除绑定

```java
okBleHelper.removeBleListener(Context context)
```



## 进阶使用

### 发送长数据

```java
Thread thread = okBleHelper.writeLong(BluetoothGatt gatt, byte[] bytes, @NonNull BaseLongDataAdapter baseLongDataAdapter)
```

此api会开启一个线程，并将此线程返回，我们可以随时终止这个线程来停止数据发送。

由于`Android`对发送长数据有限制，因此必须通过将数据分段来进行发送。我们需要自己实现`BaseLongDataAdapter`来自定义分段规则。`BaseLongDataAdapter`提供了两个回调,分别是`splitDataList(byte[] bytes)`和`splitFilter(byte[] bytes)`

在`splitDataList(byte[] bytes)`中,自定义分段规则，每发给底层一段数据，底层需要响应，响应数据要满足`splitFilter(byte[] bytes)`方法中定义的规则，才会继续发送下一段数据



此外，`BaseLongDataAdapter`还提供了进度监听,可以通过以下方法实现：

```java
longDataAdapter.setOnWriteLongCallback(new BaseLongDataAdapter.OnWriteLongListener() {
    @Override
    public void onProgress(long leftTimeInSecond) {
        stateTV.setText("剩余时间" + leftTimeInSecond + "秒");
    }
    @Override
    public void onWriteFailed(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        stateTV.setText("写入失败");
    }
    @Override
    public void onWriteSuccess(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        stateTV.setText("写入成功");
    }
});
```



### 数据粘包

当底层并发发送两条数据时，例如`{"version":"1.0.0"}`和`{"power":"OK"}`，`Android`端收到的可能不是两条单独的数据，而是`{"version":"1.0.0"}{"power":"OK"}`一条粘连在一起的数据，对于这种情况，强烈建议底层做处理，每次发送消息时添加延时，不要多条数据并发发送。如果底层一定要并发发送，框架也提供了解决方案。

如果要解决数据粘连问题，我们需要自定义一个类，实现`IMergePackage`接口,接口提供了一个`getHexList(byte[] bytes)`方法，我们需要实现它，`bytes`是一个可能粘在一起的数据，我们需要和底层协商一套规则，来将这个数组拆分开，例如定死一个长度，或者CRC8校验等。

定义好后，我们需要在`Application`中，通过`mergePackage(IMergePackage mergePackage)`指定对应的解包方式

```java
public class MyApplication extends Application {

    public static final String SERVICE_UUID = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx";
    public static final String NOTIFY_UUID = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx";
    public static final String WRITE_UUID = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx";

    private OkBleHelper okBleHelper;

    @Override
    public void onCreate() {
        super.onCreate();

        okBleHelper = new OkBleHelper.Builder(this)
        	// 设置UUID
                .serviceUUID(SERVICE_UUID)
                .writeUUID(WRITE_UUID)
                .notifyUUID(NOTIFY_UUID)
              	.mtu(512)// 设置mtu
                .writeTimeout(1000)// 设置短数据读写超时1000毫秒
                .writeLongTimeout(3000)// 设置长数据读写超时3000毫秒
                .writeRetryTime(3)// 设置短数据超时后重发次数
                .writeLongRetryTimes(3)// 设置长数据超时后重发次数
                .mergePackage(new IMergePackageImpl())// 防止粘包
                .build();
    }


    public OkBleHelper getOkBleHelper() {
        return okBleHelper;
    }
}
```












