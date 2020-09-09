package com.example.lora_comms_app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import java.util.UUID;

public class BluetoothGattAndroidService extends Service {

    private static final int ONGOING_NOTIFICATION_ID = 117;
    private static final String CHANNEL_ID = "CHANNEL_ID";
    private static final String TAG = "BluetoothGattAndroidService";
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    private static final UUID UART_TX_CHARACTERISTIC_ID = UUID
            .fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final UUID UART_RX_CHARACTERISTIC_ID = UUID
            .fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final UUID UART_SERVICE_ID = UUID
            .fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");

    private static final String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    private static final String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    private static final String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public static final String ACTION_DATA_FROM_SERVICE_AVAILABLE =
            "ACTION_DATA_FROM_SERVICE_AVAILABLE";
    public static final String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    private final BroadcastReceiver activityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            final String data = intent.getStringExtra(DeviceConnectedActivity.ACTIVITY_STRING_DATA);
            if(action.equals(DeviceConnectedActivity.ACTION_ACTIVITY_MESSAGE)){
                writeNUSRxCharacteristic(data);
            }
        }
    };

    private int connectionState = STATE_DISCONNECTED;
    private BluetoothGatt mBluetoothGatt;

    public BluetoothGattAndroidService() {
    }


    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundWithNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        connectToBluetoothGatt(intent);
        registerBroadcastReceiver();
        return START_NOT_STICKY;
    }

    // TODO: Return the communication channel to the service.
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    // TODO: add option somewhere to terminate this service programmatically
    // TODO: relinquish bluetoothGatt when service terminates
    @Override
    public void onDestroy(){

    }

    private void registerBroadcastReceiver(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(DeviceConnectedActivity.ACTION_ACTIVITY_MESSAGE);
        this.registerReceiver(activityReceiver, filter);
    }

    private void startForegroundWithNotification() {
        Intent notificationIntent = new Intent(this, DeviceConnectedActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification =
                new Notification.Builder(this, CHANNEL_ID)
                        .setContentTitle(getText(R.string.notification_title))
                        .setContentText(getText(R.string.notification_message))
                        .setSmallIcon(R.mipmap.ic_launcher_round)
                        .setContentIntent(pendingIntent)
                        .setTicker(getText(R.string.ticker_text))
                        .build();

        startForeground(ONGOING_NOTIFICATION_ID, notification);
    }

    public UUID convertFromInteger(long value) {
        final long MSB = 0x0000000000001000L;
        final long LSB = 0x800000805f9b34fbL;
        return new UUID(MSB | (value << 32), LSB);
    }

    private void broadcastUpdate(final String action, final String data) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_DATA, data);
        sendBroadcast(intent);
    }

    private void connectToBluetoothGatt(final Intent intent){
        BluetoothDevice bluetoothDevice = intent.getParcelableExtra(DeviceConnectedActivity.BLUETOOTH_DEVICE);
        mBluetoothGatt = bluetoothDevice.connectGatt(this, false, gattCallback);
    }

    private void writeNUSRxCharacteristic(final String data){
        BluetoothGattCharacteristic rxCharacteristic = mBluetoothGatt
                .getService(UART_SERVICE_ID)
                .getCharacteristic(UART_RX_CHARACTERISTIC_ID);
        rxCharacteristic.setValue(data);
        mBluetoothGatt.writeCharacteristic(rxCharacteristic);
    }

    private final BluetoothGattCallback gattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    String intentAction;
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        connectionState = STATE_CONNECTED;
                        Log.i(TAG, "Connected to GATT server.");
                        Log.i(TAG, "Attempting to start service discovery:" +
                                mBluetoothGatt.discoverServices());

                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        connectionState = STATE_DISCONNECTED;
                        Log.i(TAG, "Disconnected from GATT server.");
                    }
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        BluetoothGattCharacteristic characteristic = gatt
                                .getService(UART_SERVICE_ID)
                                .getCharacteristic(UART_TX_CHARACTERISTIC_ID);
                        gatt.setCharacteristicNotification(characteristic, true);
                        BluetoothGattDescriptor descriptor = characteristic
                                .getDescriptor(convertFromInteger(0x2902));
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);
                    } else {
                        Log.w(TAG, "onServicesDiscovered received: " + status);
                    }
                }

                @Override
                public void onDescriptorWrite(BluetoothGatt gatt,
                                              BluetoothGattDescriptor descriptor,
                                              int status){
                    BluetoothGattCharacteristic characteristic = gatt
                            .getService(UART_SERVICE_ID)
                            .getCharacteristic(UART_TX_CHARACTERISTIC_ID);
                    characteristic.setValue(new byte[]{1, 1});
                    gatt.writeCharacteristic(characteristic);
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt,
                                                    BluetoothGattCharacteristic characteristic) {
                    final byte[] data = characteristic.getValue();
                    if (data != null && data.length > 0) {
                        /*
                        final StringBuilder stringBuilder = new StringBuilder(data.length);
                        for(byte byteChar : data){
                            stringBuilder.append(String.format("%02X ", byteChar));
                        }
                        String bleData = stringBuilder.toString();
                         */
                        final String bleData = new String(data);
                        broadcastUpdate(ACTION_DATA_FROM_SERVICE_AVAILABLE, bleData);
                    }
                }

                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt,
                                                  BluetoothGattCharacteristic characteristic,
                                                  int status){
                    Log.i(TAG, "finished writing back");
                }
            };
}
