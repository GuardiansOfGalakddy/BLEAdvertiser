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
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Switch;
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
    private TextView textView;
    private Button advertiseButton;
    private Button discoverButton;
    private Button connectButton;
    private Button addButton;
    private Button uuidButton;

    private Switch switchDevice;
    private Boolean boolDevice = false;
    private Switch switchSTS;
    private Boolean boolSTS = false;
    private EditText editID;
    private EditText editTime;

    private RadioGroup radioGroupAID;
    private RadioGroup radioGroupSEV;
    private RadioGroup radioGroupALM;
    //Layout components

    //common components
    UuidDbManager dbManager = null;
    BluetoothLeAdvertiser advertiser = null;
    BluetoothManager bluetoothManager = null;
    BluetoothAdapter adapter = null;

    BluetoothGatt bluetoothGatt = null;
    BluetoothGattServer gattServer = null;

    BluetoothDevice device = null;
    UUID exUuid = null;

    AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i("BLE", "Advertising onStartSuccess");
            textView.setText("Advertising.. -> " + exUuid.toString());
            super.onStartSuccess(settingsInEffect);
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.e("BLE", "Advertising onStartFailure : " + errorCode);
            textView.setText("Advertise error.. -> " + errorCode);
            super.onStartFailure(errorCode);
        }
    };
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
                    for (BluetoothGattCharacteristic characteristic : gattService.getCharacteristics()) {
                        Log.d("characteristic", characteristic.getUuid().toString());
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

        dbManager = UuidDbManager.getInstance(this);
        mText = findViewById(R.id.text);
        textView = findViewById(R.id.textView);
        discoverButton = findViewById(R.id.discover_btn);
        advertiseButton = findViewById(R.id.advertise_btn);
        connectButton = findViewById(R.id.connect_btn);
        addButton = findViewById(R.id.add_btn);
        uuidButton = findViewById(R.id.uuid_btn);

        switchDevice = findViewById(R.id.switchDevice);
        switchSTS = findViewById(R.id.switchSTS);
        editID = findViewById(R.id.editID);
        editTime = findViewById(R.id.editTime);
        radioGroupAID = findViewById(R.id.radioGroupAID);
        radioGroupSEV = findViewById(R.id.radioGroupSEV);
        radioGroupALM = findViewById(R.id.radioGroupALM);
        radioGroupAID.check(R.id.radioAID0);
        radioGroupSEV.check(R.id.radioSEV0);
        radioGroupALM.check(R.id.radioALM0);

        switchDevice.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                boolDevice = isChecked;
            }
        });
        switchSTS.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                boolSTS = isChecked;
            }
        });

        discoverButton.setOnClickListener(this);
        advertiseButton.setOnClickListener(this);
        connectButton.setOnClickListener(this);
        addButton.setOnClickListener(this);
        uuidButton.setOnClickListener(this);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "this device does not support BLE", Toast.LENGTH_SHORT).show();
            finish();
        }

        bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        adapter = bluetoothManager.getAdapter();

        if (!adapter.isMultipleAdvertisementSupported()) {
            Toast.makeText(this, "Multiple advertisement not supported", Toast.LENGTH_LONG).show();
            advertiseButton.setEnabled(false);
            discoverButton.setEnabled(false);
        }

        mBluetoothLeScanner = adapter.getBluetoothLeScanner();
        Set<BluetoothDevice> devices = adapter.getBondedDevices();
        Iterator iterator = devices.iterator();
        while (iterator.hasNext()) {
            BluetoothDevice device = (BluetoothDevice) iterator.next();
            Log.d("bonded", device.getName() + "/" + device.getAddress());
        }

        AutoPermissions.Companion.loadAllPermissions(this, 101);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.discover_btn)
            discover();
        else if (v.getId() == R.id.advertise_btn) {
            if (advertiseButton.getText().toString().equals("START ADVERTISE"))
                advertise();
            else
                stopAdvertise();
        } else if (v.getId() == R.id.connect_btn)
            connect();
        else if (v.getId() == R.id.add_btn)
            add();
        else if (v.getId() == R.id.uuid_btn) {
            Intent intent = new Intent(getApplicationContext(), HistoryActivity.class);
            startActivity(intent);
        }
    }

    private void connect() {
        bluetoothGatt = device.connectGatt(getApplicationContext(), false, gattCallback, 2);

    }

    private void add() {
        UUID uuid = makeUuid();
        if (uuid == null) {
            Toast.makeText(getApplicationContext(), "UUID를 정확히 설정하세요.", Toast.LENGTH_LONG).show();
            return;
        }
        ContentValues addRowValue = new ContentValues();
        addRowValue.put("uuid", uuid.toString());
        dbManager.insert(addRowValue);
        Toast.makeText(getApplicationContext(), "Success -> " + uuid.toString(), Toast.LENGTH_LONG).show();
        editID.setText("");
        editTime.setText("");
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
        advertiser = adapter.getBluetoothLeAdvertiser();
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                .build();

        long Uuid1 = 0x0052532D00020101L;//수집기
        long Uuid2 = 0x0200130B050C1E2DL;
        UUID uuid = new UUID(Uuid1, Uuid2);
        exUuid = makeUuid();
        if (exUuid == null)
            exUuid = uuid;

        ParcelUuid pUuid = new ParcelUuid(exUuid);
        adapter.setName("test");

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .addServiceUuid(pUuid)
                .build();

        advertiser.startAdvertising(settings, data, advertiseCallback);
        advertiseButton.setText("Stop advertise");

        gattServer = bluetoothManager.openGattServer(getApplicationContext(), gattServerCallback);
        gattServer.clearServices();
        BluetoothGattService uuidService = new BluetoothGattService(UUID.fromString("453d1ed7-9c6a-47e6-b774-4a736be00baa"),
                BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattService subService = new BluetoothGattService(UUID.fromString("453d1ed7-9c6a-47e6-b774-4a736be00bab"),
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        Cursor uuidCursor = dbManager.getUuid();
        while (uuidCursor.moveToNext()) {
            uuidService.addCharacteristic(new BluetoothGattCharacteristic(UUID.fromString(uuidCursor.getString(0)),
                    BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ));
            Log.d("addCharacteristic", uuidCursor.getString(0));
        }

        gattServer.addService(uuidService);
        gattServer.addService(subService);
    }

    private UUID makeUuid() {
        if (editID.getText().length() != 4) {
            Toast.makeText(getApplicationContext(), "ID를 4글자로 설정해주세요.", Toast.LENGTH_LONG).show();
            return null;
        }
        StringBuilder builder = new StringBuilder("00");
        builder.append(boolDevice ? "42532D" : "52532D")
                .append(editID.getText().toString());
        switch (radioGroupAID.getCheckedRadioButtonId()) {
            case R.id.radioAID0:
                builder.append("00");
                break;
            case R.id.radioAID1:
                builder.append("01");
                break;
            case R.id.radioAID2:
                builder.append("02");
                break;
            case R.id.radioAID3:
                builder.append("03");
                break;
        }
        switch (radioGroupSEV.getCheckedRadioButtonId()) {
            case R.id.radioSEV0:
                builder.append("00");
                break;
            case R.id.radioSEV1:
                builder.append("01");
                break;
            case R.id.radioSEV2:
                builder.append("02");
                break;
        }
        switch (radioGroupALM.getCheckedRadioButtonId()) {
            case R.id.radioALM0:
                builder.append("00");
                break;
            case R.id.radioALM1:
                builder.append("01");
                break;
            case R.id.radioALM2:
                builder.append("02");
                break;
        }
        builder.append(boolSTS ? "01" : "00");
        if (editTime.getText().length() == 0) {
            builder.append("130B050C1E2D");
        } else if (editTime.getText().length() == 12) {
            if (Integer.toHexString(Integer.parseInt(editTime.getText().toString().substring(0, 2))).length() < 2)
                builder.append("0");
            builder.append(Integer.toHexString(Integer.parseInt(editTime.getText().toString().substring(0, 2))));
            if (Integer.toHexString(Integer.parseInt(editTime.getText().toString().substring(2, 4))).length() < 2)
                builder.append("0");
            builder.append(Integer.toHexString(Integer.parseInt(editTime.getText().toString().substring(2, 4))));
            if (Integer.toHexString(Integer.parseInt(editTime.getText().toString().substring(4, 6))).length() < 2)
                builder.append("0");
            builder.append(Integer.toHexString(Integer.parseInt(editTime.getText().toString().substring(4, 6))));
            if (Integer.toHexString(Integer.parseInt(editTime.getText().toString().substring(6, 8))).length() < 2)
                builder.append("0");
            builder.append(Integer.toHexString(Integer.parseInt(editTime.getText().toString().substring(6, 8))));
            if (Integer.toHexString(Integer.parseInt(editTime.getText().toString().substring(8, 10))).length() < 2)
                builder.append("0");
            builder.append(Integer.toHexString(Integer.parseInt(editTime.getText().toString().substring(8, 10))));
            if (Integer.toHexString(Integer.parseInt(editTime.getText().toString().substring(10))).length() < 2)
                builder.append("0");
            builder.append(Integer.toHexString(Integer.parseInt(editTime.getText().toString().substring(10))));
        } else {
            Toast.makeText(getApplicationContext(), "시간을 12자로 입력하거나 비워두세요", Toast.LENGTH_LONG).show();
            return null;
        }
        long uuid1 = Long.parseLong(builder.substring(0, 16), 16);
        long uuid2 = Long.parseLong(builder.substring(16), 16);
        UUID uuid = new UUID(uuid1, uuid2);
        return uuid;
    }

    private void stopAdvertise() {
        advertiser.stopAdvertising(advertiseCallback);
        advertiseButton.setText("START ADVERTISE");
        textView.setText("Advertise stopped");
    }//0052532d-0002-0301-0000-0101e110010b

    @Override
    public void onDenied(int i, String[] strings) {
        finish();
    }

    @Override
    public void onGranted(int i, String[] strings) {

    }
}
