package com.logitech.ble;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "charishma";
    private BluetoothAdapter bluetoothAdapter;
    private final static int REQUEST_ENABLE_BT = 1;
    private BluetoothLeScanner bleScanner;
    private ScanSettings scanSettings;
    private static final int SCAN_PERIOD = 30000;
    private Handler mHandler = new Handler();
    private Button mScnBtn;
    int ACTION_REQUEST_MULTIPLE_PERMISSION = 1;
    private BluetoothGatt mBluetoothGatt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mScnBtn = findViewById(R.id.scn_btn);

        if (!getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            // BLE NOT SUPPORTED!
            Log.v(TAG, "Ble is not supported");
        }

        Intent intent = new Intent();
        intent.setAction("HELLO");
        sendBroadcast(intent);

        BluetoothManager bluetoothManager = (BluetoothManager)
                getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null)
            bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBluetooth =
                    new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth,
                    REQUEST_ENABLE_BT);
        }

        if ((ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) || (
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED)) {
            // Location permission not granted!
            Log.v(TAG, "Access coarse permission is not granted");
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN},
                    ACTION_REQUEST_MULTIPLE_PERMISSION);
        }

        mScnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bleScanner == null)
                    Log.v(TAG, "Scanner is null");
                else {
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.v(TAG, "Scan period is over so going to stop scan");
                            bleScanner.stopScan(bleScanCallback);
                        }

                    }, SCAN_PERIOD);
                    bleScanner.startScan(bleScanCallback);
                    Log.v(TAG, "Ble started scanning...");
                }
            }
        });

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(receiver, filter);
    }

    private ScanCallback bleScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            BluetoothDevice oriDevice = bluetoothAdapter.getRemoteDevice("FC:58:FA:D7:1E:DC");
            if (device == oriDevice)
                Log.v(TAG, "Both the devices are equal");
            Log.v(TAG, "On scan result");
            Log.v(TAG, "Device " + device);
            if (device.getName() != null && device.getName().contains("Boat")) {
                Log.v(TAG, "Before pair device");
                pairDevice(device);
                bleScanner.stopScan(bleScanCallback);
            }
            Log.v(TAG, "Device address " + result.getDevice().getAddress());
            //stopScan();
        }

        @Override
        public void onScanFailed(int errorCode) {
            // OOPS, there is something wrong.
            Log.v(TAG, "On Scan failed" + errorCode);
        }
    };

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        break;
                    case BluetoothAdapter.STATE_ON:
                        //to check if BluetoothAdapter is enable by your code
                        Log.v(TAG, "On state on");
                        if (bluetoothAdapter != null) {
                            bleScanner = bluetoothAdapter.getBluetoothLeScanner();
                        }
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        break;
                }
            }
        }
    };

    private void pairDevice(BluetoothDevice device) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mBluetoothGatt = device.connectGatt(this,
                    false, bluetoothGattCallback,
                    BluetoothDevice.TRANSPORT_LE);
        } else {
            mBluetoothGatt = device.connectGatt(this,
                    false, bluetoothGattCallback);
        }
    }

    private BluetoothGattCallback bluetoothGattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt,
                                                    int status, int newState) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        // Device connected!
                        Log.v(TAG, "Device connected");
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        // Device disconnected!
                        Log.v(TAG, "Device disconnected");
                    }
                }

            };
}
