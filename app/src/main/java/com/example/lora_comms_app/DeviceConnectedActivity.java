package com.example.lora_comms_app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class DeviceConnectedActivity extends AppCompatActivity {

    public static final String BLUETOOTH_DEVICE = "BLUETOOTH_DEVICE";
    public static final String ACTION_ACTIVITY_MESSAGE = "ACTION_ACTIVITY_MESSAGE";
    public static final String ACTIVITY_STRING_DATA = "ACTIVITY_STRING_DATA";

    private EditText editText;
    private Button sendButton;
    private RecyclerView mMessageRecycler;
    private MessageListAdapter mMessageListAdapter;
    private ArrayList<Message> messageList = new ArrayList<>();


    private final BroadcastReceiver bluetoothGattAndroidServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            final String data = intent.getStringExtra(BluetoothGattAndroidService.EXTRA_DATA);
            if(action.equals(BluetoothGattAndroidService.ACTION_DATA_FROM_SERVICE_AVAILABLE)){
                Log.i("DCA", "received data");
                Log.i("DCA", "action: " + action);
                DeviceMessage deviceMessage = new DeviceMessage();
                deviceMessage.setMessageData(data);
                mMessageListAdapter.addMessage(deviceMessage);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_connected);
    }

    @Override
    protected void onStart(){
        super.onStart();

        mMessageRecycler = findViewById(R.id.reyclerview_message_list);
        mMessageListAdapter = new MessageListAdapter(this, messageList);
        mMessageRecycler.setLayoutManager(new LinearLayoutManager(this));
        mMessageRecycler.setAdapter(mMessageListAdapter);

        setEditText();
        setSendButton();
        registerBroadcastReceiver();
        startBluetoothGattAndroidService();
    }

    private void startBluetoothGattAndroidService(){
        Intent intent = getIntent();
        BluetoothDevice bluetoothDevice = intent.getParcelableExtra(MainActivity.BLUETOOTH_DEVICE);

        Intent bluetoothGattServiceIntent = new Intent(DeviceConnectedActivity.this, BluetoothGattAndroidService.class);
        bluetoothGattServiceIntent.putExtra(BLUETOOTH_DEVICE, bluetoothDevice);
        Log.i("onStart", "starting foreground service");

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            DeviceConnectedActivity.this.startForegroundService(bluetoothGattServiceIntent);
        } else {
            startService(bluetoothGattServiceIntent);
        }
    }

    private void broadcastUpdate(final String action, final String data) {
        final Intent intent = new Intent(action);
        intent.putExtra(ACTIVITY_STRING_DATA, data);
        sendBroadcast(intent);
    }

    // TODO: use a local broadcasting type instead of a global broadcast
    // TODO: unregister broadcaster when activity ends!
    private void registerBroadcastReceiver(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothGattAndroidService.ACTION_DATA_FROM_SERVICE_AVAILABLE);
        this.registerReceiver(bluetoothGattAndroidServiceReceiver, filter);
    }

    private void setSendButton(){
        sendButton = findViewById(R.id.button_chatbox_send);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String userSendData = editText.getText().toString();
                editText.setText("");
                editText.getText().clear();
                AppMessage appMessage = new AppMessage();
                appMessage.setMessageData(userSendData);
                mMessageListAdapter.addMessage(appMessage);
                broadcastUpdate(ACTION_ACTIVITY_MESSAGE, userSendData);
            }
        });
    }

    private void setEditText(){
        editText = findViewById(R.id.edittext_chatbox);
    }
}