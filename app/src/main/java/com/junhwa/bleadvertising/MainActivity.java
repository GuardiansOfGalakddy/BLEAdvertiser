package com.junhwa.bleadvertising;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.pedro.library.AutoPermissions;
import com.pedro.library.AutoPermissionsListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AutoPermissionsListener {
    //Layout components
    private TextView mText;
    private Button mAdvertiseButton;
    private Button mDiscoverButton;
    private Button connectButton;
    //Layout components

    //common components
    BluetoothManager bluetoothManager = null;
    BluetoothAdapter adapter = null;



    BluetoothGatt bluetoothGatt = null;
    BluetoothGattServer gattServer = null;

    BluetoothDevice device = null;
    UUID exUuid = null;
    //common components

    //discover() components
    private BluetoothLeScanner mBluetoothLeScanner;
    private Handler mHandler = new Handler();
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (result == null || result.getScanRecord().getServiceUuids() == null)
                return;
            exUuid = result.getScanRecord().getServiceUuids().get(0).getUuid();
            StringBuilder builder = new StringBuilder(result.getScanRecord().getServiceUuids().get(0).toString());
            mText.setText(builder.toString());
            device = result.getDevice();
            Log.d("BLE", result.getDevice().getName() + result.getDevice().toString());
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("gattCallback", "new State = Connected");
                Log.i("gattCallback", "Attempting to start service discovery:" + bluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i("gattCallback", "new State = Disconnected");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("onServicesDiscovered", "onServicesDiscovered received: " + gatt.getServices().size());
                for (BluetoothGattService gattService : gatt.getServices()) {
                    Log.d("onServicesDiscovered", gattService.getUuid().toString());
                    if (gattService.getUuid().equals(exUuid)) {
                        for (BluetoothGattCharacteristic characteristic : gattService.getCharacteristics()) {
                            Log.d("characteristic", characteristic.getUuid().toString());
                        }
                    }
                }
            } else {
                Log.w("onServicesDiscovered", "onServicesDiscovered received: " + status);
            }
        }

        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("onCharacteristicRead", characteristic.toString());
            }
        }
    };
    //discover() components

    BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            //super.onConnectionStateChange(device, status, newState);
            switch (newState) {
                case STATE_CONNECTED:
                    final String str = device.toString();
                    if (str != null)
                        mText.post(new Runnable() {
                            @Override
                            public void run() {
                                mText.append("\n" + str);
                            }
                        });
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        long Uuid1 = 0x0042532D00020101L;//수집기
        long Uuid2 = 0x0201130B050C1E2DL;
        UUID uuid = new UUID(Uuid1, Uuid2);
        exUuid = uuid;

        mText = findViewById(R.id.text);
        mDiscoverButton = findViewById(R.id.discover_btn);
        mAdvertiseButton = findViewById(R.id.advertise_btn);
        connectButton = findViewById(R.id.connect_btn);

        mDiscoverButton.setOnClickListener(this);
        mAdvertiseButton.setOnClickListener(this);
        connectButton.setOnClickListener(this);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "this device does not support BLE", Toast.LENGTH_SHORT).show();
            finish();
        }

        bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        adapter = bluetoothManager.getAdapter();

        if (!adapter.isMultipleAdvertisementSupported()) {
            Toast.makeText(this, "Multiple advertisement not supported", Toast.LENGTH_LONG).show();
            mAdvertiseButton.setEnabled(false);
            mDiscoverButton.setEnabled(false);
        }

        mBluetoothLeScanner = adapter.getBluetoothLeScanner();
        Set<BluetoothDevice> devices =  adapter.getBondedDevices();
        Iterator iterator = devices.iterator();
        while (iterator.hasNext()){
            BluetoothDevice device = (BluetoothDevice) iterator.next();
            Log.d("bonded", device.getName() + "/" + device.getAddress());
        }

        AutoPermissions.Companion.loadAllPermissions(this, 101);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.discover_btn)
            discover();
        else if (v.getId() == R.id.advertise_btn)
            advertise();
        else if (v.getId() == R.id.connect_btn)
            connect();
    }

    private void connect() {
        bluetoothGatt = device.connectGatt(getApplicationContext(), false, gattCallback, 2);

    }

    private void discover() {
        ScanFilter filter = new ScanFilter.Builder()
                .build();
        List<ScanFilter> filters = new ArrayList<>();
        filters.add(filter);

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        mBluetoothLeScanner.startScan(filters, settings, mScanCallback);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBluetoothLeScanner.stopScan(mScanCallback);
            }
        }, 3000);
    }

    private void advertise() {
        BluetoothLeAdvertiser advertiser = adapter.getBluetoothLeAdvertiser();
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                .build();

        long Uuid1 = 0x0042532D00020101L;//수집기
        long Uuid2 = 0x0201130B050C1E2DL;
        UUID uuid = new UUID(Uuid1, Uuid2);
        exUuid = uuid;

        ParcelUuid pUuid = new ParcelUuid(uuid);
        adapter.setName("test");

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .addServiceUuid(pUuid)
                .build();

        AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                Log.i("BLE", "Advertising onStartSuccess");
                super.onStartSuccess(settingsInEffect);
            }

            @Override
            public void onStartFailure(int errorCode) {
                Log.e("BLE", "Advertising onStartFailure : " + errorCode);
                super.onStartFailure(errorCode);
            }
        };

        advertiser.startAdvertising(settings, data, advertiseCallback);

        gattServer = bluetoothManager.openGattServer(getApplicationContext(), gattServerCallback);
        BluetoothGattService service = new BluetoothGattService(exUuid, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattCharacteristic characteristic
                = new BluetoothGattCharacteristic(UUID.fromString("453d1ed7-9c6a-47e6-b774-4a736be00baa"),
                BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ);
        BluetoothGattCharacteristic characteristic2
                = new BluetoothGattCharacteristic(UUID.fromString("453d1ed7-9c6a-47e6-b774-4a736be00bab"),
                BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ);
        service.addCharacteristic(characteristic);
        service.addCharacteristic(characteristic2);
        gattServer.addService(service);
    }

    @Override
    public void onDenied(int i, String[] strings) {
        finish();
    }

    @Override
    public void onGranted(int i, String[] strings) {

    }
}
