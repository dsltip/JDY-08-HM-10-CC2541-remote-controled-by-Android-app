package com.example.test1;

import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;
import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity{



    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 2;
    private static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 3;


    public BluetoothManager BTmanager;
    public BluetoothAdapter BTadapter;
    public BluetoothLeScanner BTscanner;
    public BluetoothGatt gatt;
    private Button startButton;
    private Button stopButton;
    private Button connectButton;
    private Button disconnectButton;
    private Button uploadButton;
    private Button lockButton;
    private Button unlockButton;
    private TextView IDTextView;
    private TextView TextViewLog;
    private CheckBox autoCheckbox;
    private EditText editText;
    private String bt_data;
    int count_beacons_total = 0;
    private int mStatus;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        IDTextView = findViewById(R.id.participant_ID);
        startButton = findViewById(R.id.start_button);
        stopButton = findViewById(R.id.stop_button);
        connectButton = findViewById(R.id.connect_button);
        disconnectButton = findViewById(R.id.disconnect_button);
        lockButton = findViewById(R.id.lock_button);
        unlockButton = findViewById(R.id.unlock_button);
        TextViewLog = findViewById(R.id.textView);
        uploadButton = findViewById(R.id.upload_button);
        autoCheckbox = (CheckBox)findViewById(R.id.checkBox);
        autoCheckbox.setChecked(getEnabled(getApplicationContext()));
        startButton.setOnClickListener(new buttonClick());
        stopButton.setOnClickListener(new buttonClick());
        uploadButton.setOnClickListener(new buttonClick());
        connectButton.setOnClickListener(new buttonClick());
        lockButton.setOnClickListener(new buttonClick());
        unlockButton.setOnClickListener(new buttonClick());
        disconnectButton.setOnClickListener(new buttonClick());
        editText = (EditText)findViewById(R.id.participant_ID);
        editText.setText(getMAC(getApplicationContext()));
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("BLEupdates"));
        init();
        if(autoCheckbox.isChecked())if (BTadapter.isEnabled())connectButtonClicked();
    }
    public static boolean getEnabled(Context context) {
        return context.getSharedPreferences("carlock_shared_pref", 0).getBoolean("auto_enabled", false);
    }
    public static String getMAC(Context context) {
        return context.getSharedPreferences("carlock_shared_pref", 0).getString("mac", "64:CF:D9:29:A6:39");
    }

    public static void saveMAC(Context context, String mac) {
        SharedPreferences sp = context.getSharedPreferences("carlock_shared_pref", 0);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("mac", mac);
        editor.apply();
    }
    public static void saveEnabled(Context context, boolean state) {
        SharedPreferences sp = context.getSharedPreferences("carlock_shared_pref", 0);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("auto_enabled", state);
        editor.apply();
    }

    public void onCheckboxClicked(View view) {
        saveEnabled(getApplicationContext(),autoCheckbox.isChecked());
        //Toast.makeText(MainActivity.this, "Saved", Toast.LENGTH_LONG).show();
    }

    private void startButtonClicked(){
        saveMAC(getApplicationContext(),editText.getText().toString());
        if (BTscanner == null)BTscanner = BTadapter.getBluetoothLeScanner();
        if (BTscanner != null) {
            List<ScanFilter> scanFilters = new ArrayList<>();
            //scanFilters.add(new ScanFilter.Builder().setServiceUuid(ESTIMOTE_SERVICE_UUID).build());
            scanFilters.add(new ScanFilter.Builder().setDeviceAddress(editText.getText().toString()).build());
            final ScanSettings SCAN_SETTINGS =
                    new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                            .setReportDelay(0).setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES).build();
            BTscanner.startScan(scanFilters,SCAN_SETTINGS,scanCallback);
            Toast.makeText(this, "Start", Toast.LENGTH_SHORT).show();

        }

        /*String participant_ID = IDTextView.getText().toString();

        Intent serviceIntent = new Intent(this, MyService.class);
        serviceIntent.putExtra("inputExtra", participant_ID);

        ContextCompat.startForegroundService(this, serviceIntent);*/
        //IDTextView.setText("start");
        startButton.setVisibility(View.INVISIBLE);
        stopButton.setVisibility(View.VISIBLE);
    }
    private void stopButtonClicked(){

        /*Intent serviceIntent = new Intent(this, MyService.class);
        stopService(serviceIntent);*/
        if (BTscanner != null) {
            BTscanner.stopScan(scanCallback);
        }
        startButton.setVisibility(View.VISIBLE);
        stopButton.setVisibility(View.INVISIBLE);
    }
    private void uploadButtonClicked(){

        /*http2 task = new http2();
        task.navURL = "http://xxx.ru/carvolt.php?s=" + TextViewLog.getText().toString();
        task.mContext = this;
        task.tvl = IDTextView;
        task.execute();*/


    }
    public ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            ScanRecord scanRecord = result.getScanRecord();
            final int rssi = result.getRssi();
            //result.getDevice().getAddress();
            String MAC = result.getDevice().getAddress();
            // long btTimestampMillis = System.currentTimeMillis() - SystemClock.elapsedRealtime() +  result.getTimestampNanos() / 1000000;
            if (scanRecord == null) {
                return;
            }
            //if (MAC.equals("DD:EE:FF:AA:DD:01")) {
            count_beacons_total++;
            //sendMessageToActivity(Integer.toString(count_beacons_total));
            //sendMessageToActivity(result.getDevice().getAddress() + " " + Integer.toString(count_beacons_total));
            TextViewLog.setText(Integer.toString(count_beacons_total));
            //TextViewLog.setText("Yes");
            //sendMessageToActivity(toHexString(scanRecord.getBytes()).substring(10,62) + "&c=" + Integer.toString(count_beacons_total));

            //beacons_data.append(result.getDevice().getAddress()).append(",").append(toHexString(scanRecord.getBytes())).append(System.lineSeparator());
        }
        @Override
        public void onScanFailed(int errorCode) {
            switch (errorCode) {
                case SCAN_FAILED_ALREADY_STARTED:
                    Log.d("Bluetooth scan failed","SCAN_FAILED_ALREADY_STARTED");
                    break;
                case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                    Log.d("Bluetooth scan failed","SCAN_FAILED_APPLICATION_REGISTRATION_FAILED");
                    break;
                case SCAN_FAILED_FEATURE_UNSUPPORTED:
                    Log.d("Bluetooth scan failed","SCAN_FAILED_FEATURE_UNSUPPORTED");
                    break;
                case SCAN_FAILED_INTERNAL_ERROR:
                    Log.d("Bluetooth scan failed","SCAN_FAILED_INTERNAL_ERROR");
                    break;
                default:
                    Log.d("Bluetooth scan failed","Scan failed, unknown error code");
                    break;
            }
        }
    };

    private void connectButtonClicked(){
        saveMAC(getApplicationContext(),editText.getText().toString());
        BluetoothDevice device = BTadapter.getRemoteDevice(editText.getText().toString());
        gatt =  device.connectGatt(this, false, mGattcallback, TRANSPORT_LE);
        TextViewLog.setText("Connecting...");
    }
    private void disconnectButtonClicked(){
        if (gatt != null) {
            if ((mStatus != BluetoothProfile.STATE_DISCONNECTING)
                    && (mStatus != BluetoothProfile.STATE_DISCONNECTED)) {
                //Toast.makeText(this, "Disconnect", Toast.LENGTH_SHORT).show();
                gatt.disconnect();
                TextViewLog.setText("Disconnecting...");
            }
        }

    }
    private void lockButtonClicked(){
        BluetoothGattCharacteristic ch = gatt.getService(UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")).getCharacteristic(UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb"));
        if(ch != null) {
            ch.setValue(01,FORMAT_UINT8,0);
            Toast.makeText(this, "Writing...", Toast.LENGTH_SHORT).show();
            gatt.writeCharacteristic(ch);
        } else Toast.makeText(this, "Null...", Toast.LENGTH_SHORT).show();
    }
    private void unlockButtonClicked(){
        BluetoothGattCharacteristic ch = gatt.getService(UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")).getCharacteristic(UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb"));
        if(ch != null) {
            ch.setValue(02,FORMAT_UINT8,0);
            Toast.makeText(this, "Writing...", Toast.LENGTH_SHORT).show();
            gatt.writeCharacteristic(ch);
        } else Toast.makeText(this, "Null...", Toast.LENGTH_SHORT).show();
    }

    private final BluetoothGattCallback mGattcallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mStatus = newState;
                TextViewLog.setText("Connected!");
                //lockButton.setVisibility(View.VISIBLE);
                //unlockButton.setVisibility(View.VISIBLE);
                //disconnectButton.setVisibility(View.VISIBLE);

                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mStatus = newState;
                TextViewLog.setText("Disconnected");
                gatt.close();
                gatt = null;
                lockButton.setBackgroundTintList(getColorStateList(android.R.color.darker_gray));
                unlockButton.setBackgroundTintList(getColorStateList(android.R.color.darker_gray));

            }
        };

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            TextViewLog.setText("Discovered");
            lockButton.setBackgroundTintList(getColorStateList(android.R.color.holo_green_light));
            unlockButton.setBackgroundTintList(getColorStateList(android.R.color.holo_green_light));

            for (BluetoothGattService service : gatt.getServices()) {
                if ((service == null) || (service.getUuid() == null)) {
                    continue;
                }

                //TextViewLog.setText(service.getUuid().toString());
                //Log.e(TAG, service.getUuid().toString());
                //if (BleUuid.SERVICE_DEVICE_INFORMATION.equalsIgnoreCase(service.getUuid().toString())) {

                //}
                /*if (BleUuid.SERVICE_DEVICE_INFORMATION.equalsIgnoreCase(service.getUuid().toString())) {
                    //Log.e(TAG, "eq to SERVICE_DEVICE_INFORMATION");
					//BluetoothGattCharacteristic ch1 = new BluetoothGattCharacteristic(
					//		UUID.fromString(BleUuid.CHAR_MANUFACTURER_NAME_STRING),
					//		BluetoothGattCharacteristic.PROPERTY_READ,
					//		BluetoothGattCharacteristic.PERMISSION_READ);
                    BluetoothGattCharacteristic ch1 = mConnGatt.getService(UUID.fromString(BleUuid.SERVICE_DEVICE_INFORMATION)).getCharacteristic(UUID.fromString(BleUuid.CHAR_MANUFACTURER_NAME_STRING));
                    //mReadManufacturerNameButton.setTag(service.getCharacteristic(UUID.fromString(BleUuid.CHAR_MANUFACTURER_NAME_STRING)));
					//mReadSerialNumberButton.setTag(service.getCharacteristic(UUID.fromString(BleUuid.CHAR_SERIAL_NUMBEAR_STRING)));
                    runOnUiThread(new Runnable() {
                        public void run() {
                            mReadManufacturerNameButton.setEnabled(true);
                            mReadSerialNumberButton.setEnabled(true);
                        };
                    });
                }*/
                /*if (BleUuid.SERVICE_IMMEDIATE_ALERT.equalsIgnoreCase(service.getUuid().toString())) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            mWriteAlertLevelButton.setEnabled(true);
                        };
                    });
                    mWriteAlertLevelButton.setTag(service
                            .getCharacteristic(UUID
                                    .fromString(BleUuid.CHAR_ALERT_LEVEL)));
                }*/
            }
        };
/*
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,BluetoothGattCharacteristic characteristic, int status) {
            Log.e(TAG, "on read");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "success");
                if (BleUuid.CHAR_MANUFACTURER_NAME_STRING
                        .equalsIgnoreCase(characteristic.getUuid().toString())) {
                    final String name = characteristic.getStringValue(0);

                    runOnUiThread(new Runnable() {
                        public void run() {
                            mReadManufacturerNameButton.setText(name);
                            setProgressBarIndeterminateVisibility(false);
                        };
                    });
                } else if (BleUuid.CHAR_SERIAL_NUMBEAR_STRING
                        .equalsIgnoreCase(characteristic.getUuid().toString())) {
                    final String name = characteristic.getStringValue(0);

                    runOnUiThread(new Runnable() {
                        public void run() {
                            mReadSerialNumberButton.setText(name);
                            setProgressBarIndeterminateVisibility(false);
                        };
                    });
                }

            }
        }
*/
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            TextViewLog.setText("Write OK");
            /*runOnUiThread(new Runnable() {
                public void run() {
                    setProgressBarIndeterminateVisibility(false);
                };
            });*/
        };
    };
    class buttonClick implements View.OnClickListener{
        @Override
        public void onClick(View v){
            switch (v.getId()) {
                case R.id.start_button:
                    startButtonClicked();
                    break;
                case R.id.stop_button:
                    stopButtonClicked();
                    break;
                case R.id.upload_button:
                    uploadButtonClicked();
                    break;
                case R.id.connect_button:
                    connectButtonClicked();
                    break;
                case R.id.disconnect_button:
                    disconnectButtonClicked();
                    break;
                case R.id.lock_button:
                    lockButtonClicked();
                    break;
                case R.id.unlock_button:
                    unlockButtonClicked();
                    break;
            }
        }
    }


    public void onResume(){
        super.onResume();

    }

    public void onPause(){
        super.onPause();

    }
    public void onStop(){
        super.onStop();
        if (gatt != null) {
            if ((mStatus != BluetoothProfile.STATE_DISCONNECTING)
                    && (mStatus != BluetoothProfile.STATE_DISCONNECTED)) {
                //Toast.makeText(this, "Disconnect", Toast.LENGTH_SHORT).show();
                gatt.disconnect();
                TextViewLog.setText("Disconnecting...");
            }
        }
        BTadapter.disable();//turn off BT!!!
        Intent serviceIntent = new Intent(this, MyService.class);
        stopService(serviceIntent);
        finish();
    }


    // Attempts to create the scanner.
    private void init() {
        // New Android M+ permission check requirement.

        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs coarse location access");
            builder.setMessage("Please grant coarse location access so this app can scan for beacons");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }

        if (this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            /*final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs external storage access");
            builder.setMessage("Please grant external storage access so this app can save the data collected");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
                }
            });
            builder.show();*/
        }


        BTmanager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BTadapter = BTmanager.getAdapter();
        if (BTadapter == null) {
            showFinishingAlertDialog("Bluetooth Error", "Bluetooth not detected on device");
        } else if (!BTadapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            this.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
        } else {
            BTscanner = BTadapter.getBluetoothLeScanner();
        }

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                // Bluetooth is now enabled
                if(autoCheckbox.isChecked())if (BTadapter.isEnabled())connectButtonClicked();
            } else {
                // User denied the request or an error occurred
            }
        }
    }
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String message = intent.getStringExtra("Status");
            TextViewLog.setText(message);
            uploadButton.setVisibility(View.VISIBLE);
            // Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    };
    // Pops an AlertDialog that quits the app on OK.
    private void showFinishingAlertDialog(String title, String message) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                }).show();
    }
    public static void http2res(Context context,TextView tv,String tost){
        tv.setText(tost);
    }
    public static class http2 extends AsyncTask<Void, Void, String> {

        String navURL;
        Context mContext;
        TextView tvl;
        @Override
        protected String doInBackground(Void... params) {
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            try {
                URL url = new URL(navURL);

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                forecastJsonStr = buffer.toString();
                return forecastJsonStr;
            } catch (IOException e) {
                //Log.e("PlaceholderFragment", "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        // Log.e("PlaceholderFragment", "Error closing stream", e);
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            http2res(mContext,tvl,s);
        }
    }
/*    private void logErrorAndShowToast(String message) {
        Toast.makeText(this.getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        Log.e("error", message);
    }*/

}
