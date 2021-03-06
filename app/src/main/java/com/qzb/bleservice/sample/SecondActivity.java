package com.qzb.bleservice.sample;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.qzb.ble.OkBleHelper;
import com.qzb.ble.adapter.BaseLongDataAdapter;
import com.qzb.ble.adapter.ChiereOneLongDataAdapter;
import com.qzb.ble.listener.BleListener;
import com.qzb.ble.listener.IFilter;
import com.qzb.ble.listener.IResponse;
import com.qzb.ble.utils.BleUtil;

import org.json.JSONObject;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SecondActivity extends AppCompatActivity {

    private final String TAG = getClass().getSimpleName();

    private final List<ScanResult> data = new ArrayList<>();
    private BluetoothListAdapter adapter;
    private OkBleHelper chiereOneBleHelper;


    private final ScanFilter scanFilter = new ScanFilter.Builder()
//            .setServiceUuid(ParcelUuid.fromString(MyApplication.SERVICE_UUID))
            .build();
    private BluetoothGatt bluetoothGatt;
    private TextView stateTV;
    private ScanCallback scanCallback;
    private Thread writeLongThread;

    public static void launch(Context context) {
        Intent intent = new Intent(context, SecondActivity.class);
        context.startActivity(intent);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        chiereOneBleHelper = ((MyApplication) getApplication()).getChiereOneBleHelper();

        stateTV = findViewById(R.id.tv_state);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        adapter = new BluetoothListAdapter(this, data);
        recyclerView.setAdapter(adapter);

        chiereOneBleHelper.openBle();

        // ????????????????????????
        scanCallback = new BleScanCallback(this);

        // ??????????????????
        findViewById(R.id.btn_start_scan).setOnClickListener(v -> chiereOneBleHelper.startScanDevice(Collections.singletonList(scanFilter), new ScanSettings.Builder().build(), scanCallback));

        // ????????????
        findViewById(R.id.btn_stop_scan).setOnClickListener(v -> chiereOneBleHelper.stopScanDevice(scanCallback));

        // ????????????
        adapter.setOnItemClickListener((view, position) -> {
            BluetoothDevice device = data.get(position).getDevice();
            if (!BleUtil.isDeviceConnected(device)) {
                bluetoothGatt = chiereOneBleHelper.connectDevice(device);
            }
        });

        // ????????????
        findViewById(R.id.btn_send_data).setOnClickListener(v -> {
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
                    public void onWriteSuccess(BluetoothGatt gatt, byte[] value) {
                        stateTV.setText("??????????????????:" + BleUtil.byteToHex(value));
                    }

                    @Override
                    public void onWriteFailed(BluetoothGatt gatt, byte[] value) {
                        stateTV.setText("??????????????????:" + BleUtil.byteToHex(value));

                    }

                    @Override
                    public void onNotify(BluetoothGatt gatt, byte[] value) {
                        stateTV.setText("????????????:" + BleUtil.byteToHex(value));
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // ????????????
        findViewById(R.id.btn_disconnect).setOnClickListener(v -> chiereOneBleHelper.disConnect(bluetoothGatt));

        // ????????????
        findViewById(R.id.btn_clear).setOnClickListener(v -> {
            data.clear();
            adapter.notifyDataSetChanged();
        });

        // ????????????
        findViewById(R.id.btn_upgrade).setOnClickListener(v -> {
            try {
                InputStream fis = getAssets().open("KA2108_SenBox_HC32F460_6.0.0.4_0x5a408a.bin");
                byte[] fileBytes = new byte[fis.available()];
                fis.read(fileBytes);
                fis.close();

                ChiereOneLongDataAdapter chiereOneLongDataAdapter = new ChiereOneLongDataAdapter();
                chiereOneLongDataAdapter.setOnWriteLongCallback(new BaseLongDataAdapter.OnWriteLongListener() {
                    @Override
                    public void onProgress(long leftTimeInSecond) {
                        stateTV.setText("????????????" + leftTimeInSecond + "???");
                    }

                    @Override
                    public void onWriteFailed(BluetoothGatt gatt, byte[] value) {
                        stateTV.setText("????????????");
                    }

                    @Override
                    public void onWriteSuccess(BluetoothGatt gatt, byte[] value) {
                        stateTV.setText("????????????");
                    }
                });

                writeLongThread = chiereOneBleHelper.writeLong(bluetoothGatt, fileBytes, chiereOneLongDataAdapter);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // ????????????
        chiereOneBleHelper.addBleListener(this, new BleListener() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int state) {
                super.onConnectionStateChange(gatt, state);
                switch (state) {
                    case OkBleHelper.STATE_CONNECTING:
                        Log.e(TAG, "??????GATT????????????");
                        break;
                    case OkBleHelper.STATE_CONNECTED:
                        Log.e(TAG, "GATT?????????????????????");
                        break;
                    case OkBleHelper.STATE_DISCONNECTED:
                        Log.e(TAG, "GATT??????????????????");
                        break;
                    case OkBleHelper.STATE_DISCONNECTING:
                        Log.e(TAG, "GATT??????????????????");
                        break;
                }
            }

            @Override
            public void onConnectFail(BluetoothGatt gatt, int status) {
                super.onConnectFail(gatt, status);
                Log.e(TAG, "??????GATT???????????????");
            }

            @Override
            public void onGlobalNotify(BluetoothGatt gatt, byte[] value) {
                super.onGlobalNotify(gatt, value);
                Log.e(TAG, "??????????????????:" + BleUtil.byteToHex(value));
            }
        });
    }


    /**
     * ????????????
     */
    @SuppressLint("MissingPermission")
    private void onScanResult(int callbackType, ScanResult result) {
        if (data.contains(result) || TextUtils.isEmpty(result.getDevice().getName())) {
            return;
        }
        if (!result.getDevice().getName().startsWith("T_KA21081")) {
            return;
        }
        String address = result.getDevice().getAddress();
        for (ScanResult item : data) {
            if (item.getDevice().getAddress().equals(address)) {
                return;
            }
        }
        Log.e(TAG, result.toString());
        data.add(result);
        adapter.notifyItemInserted(data.size());
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        chiereOneBleHelper.disConnect(bluetoothGatt);
        chiereOneBleHelper.removeBleListener(this);
        chiereOneBleHelper.stopScanDevice(scanCallback);
        if (writeLongThread != null) {
            writeLongThread.interrupt();
        }
    }


    /**
     * ??????????????????????????????????????????????????????????????????
     */
    public static class BleScanCallback extends ScanCallback {
        private final WeakReference<SecondActivity> mActivity;

        public BleScanCallback(SecondActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            SecondActivity activity = mActivity.get();
            activity.onScanResult(callbackType, result);
        }
    }
}