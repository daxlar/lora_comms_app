package com.example.lora_comms_app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;


import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private final int REQUEST_ENABLE_BT = 1;
    private final int REQUEST_CHECK_SETTINGS = 2;
    private final int SCAN_PERIOD = 10000;
    private BluetoothAdapter bluetoothAdapter = null;
    private ArrayAdapter<String> devicesListViewAdapter = null;
    private HashMap<String, BluetoothDevice> bleDeviceLookup = new HashMap<>();
    private int duplicateDeviceCounter = 0;

    public static final String BLUETOOTH_DEVICE = "BLUETOOTH DEVICE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart(){
        super.onStart();
        setScanButton();
        setDevicesListView();
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        getLocationPermission();
        getLocationSettings();
        getBluetoothLeSettings();
    }

    @Override
    protected void onActivityResult (int requestCode,
                                     int resultCode,
                                     Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_ENABLE_BT) {
            if(resultCode == RESULT_OK){

            }else{
                finish();
            }
        } else if(requestCode == REQUEST_CHECK_SETTINGS){
            if(resultCode == RESULT_OK){

            }else{
                finish();
            }
        }
    }

    private void textDebugging(final String debugMessage){
        Toast.makeText(this, debugMessage, Toast.LENGTH_LONG).show();
    }

    private void getLocationPermission(){
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    private void getLocationSettings(){
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    try {
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MainActivity.this,
                                REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                    }
                }
            }
        });
    }

    private void getBluetoothLeSettings(){
        BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    private void setDevicesListView(){
        ListView devicesListView = findViewById(R.id.deviceListView);
        devicesListViewAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                new ArrayList<String>());
        devicesListView.setAdapter(devicesListViewAdapter);
        AdapterView.OnItemClickListener itemClickedHandler = new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View v, int position, long id) {
                //send bluetoothDevice to next activity through intent here
                String bluetoothDeviceName = devicesListViewAdapter.getItem(position);
                BluetoothDevice bluetoothDevice = bleDeviceLookup.get(bluetoothDeviceName);
                Intent intent = new Intent(MainActivity.this, DeviceConnectedActivity.class);
                intent.putExtra(BLUETOOTH_DEVICE, bluetoothDevice);
                startActivity(intent);
            }
        };
        devicesListView.setOnItemClickListener(itemClickedHandler);
    }

    private void setScanButton(){
        Button scanButton = findViewById(R.id.scanButton);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanLeDevice();
            }
        });
    }

    // TODO: handle case of malicious device mimicking safe device
    private void scanLeDevice() {
        final BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        Handler handler = new Handler();
        final ScanCallback leScanCallback =
                new ScanCallback() {
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {
                        super.onScanResult(callbackType, result);
                        BluetoothDevice bluetoothDevice = result.getDevice();
                        String bluetoothDeviceName = bluetoothDevice.getName();
                        if(bleDeviceLookup.containsKey(bluetoothDeviceName)){
                            if(!bleDeviceLookup.get(bluetoothDeviceName).equals(bluetoothDevice)){
                                duplicateDeviceCounter++;
                                devicesListViewAdapter.add(bluetoothDevice.getName() + duplicateDeviceCounter);
                                bleDeviceLookup.put(bluetoothDeviceName, bluetoothDevice);
                            }
                        }else{
                            devicesListViewAdapter.add(bluetoothDevice.getName());
                            bleDeviceLookup.put(bluetoothDeviceName, bluetoothDevice);
                        }
                    }
                };
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                bluetoothLeScanner.stopScan(leScanCallback);
            }
        }, SCAN_PERIOD);
        bluetoothLeScanner.startScan(leScanCallback);
    }

}