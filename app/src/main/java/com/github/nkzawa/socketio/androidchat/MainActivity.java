package com.github.nkzawa.socketio.androidchat;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    /*BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    BluetoothLeScanner btScanner;
    GattServices gattServices;
    Button startScanningButton;
    Button stopScanningButton;
    TextView peripheralTextView;
    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int MANUFACTURER_SPECIFIC_DATA = 0XFF;*/

    private EditText mInputMessageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Object bl = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);

/*
        startScanningButton = (Button) findViewById(R.id.StartScanButton);
        startScanningButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startScanning();
            }
        });

        stopScanningButton = (Button) findViewById(R.id.StopScanButton);
        stopScanningButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopScanning();
            }
        });
        stopScanningButton.setVisibility(View.INVISIBLE);

        btManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();


        if (btAdapter != null && !btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent,REQUEST_ENABLE_BT);
        }

        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }*/
    }
/*
    // Device scan callback.
    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if(result.getDevice().getAddress().equalsIgnoreCase("e3:ef:56:0C:c0:37")) {
                String isBounded = result.getDevice().getBondState() != 10 ? "Yes" : "No";

                String device = " Device Name: " + result.getDevice().getName() + "\n" +
                        " Rssi: " + result.getRssi() + "\n" +
                        " Address: " + result.getDevice().getAddress() + "\n" +
                        " Bounded: " + isBounded + "\n" +
                        " Type: " + result.getDevice().getType() + "\n" +
                        " Services: " + getServiceName(result) + "\n";

                //peripheralTextView.append(device);

                sendData(device);


            }
        }
    };

    private void sendData(String device) {
        //PACK DATA IN A BUNDLE
        Bundle bundle = new Bundle();
        bundle.putString("DEVICE_KEY", device);

        //PASS OVER THE BUNDLE TO OUR FRAGMENT
        MainFragment mainFragment = new MainFragment();
        mainFragment.setArguments(bundle);

        //THEN NOW SHOW OUR FRAGMENT
        getSupportFragmentManager().beginTransaction().replace(R.id.container,mainFragment).commit();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    public String getServiceName(ScanResult result){
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        Map<ParcelUuid, byte[]> serviceData = result.getScanRecord().getServiceData();
        for (ParcelUuid pu : serviceData.keySet()) {
            String str = "";
            int number = -1;
            switch (pu.toString()) {
                //BATTERY_SERVICE
                case "0000180f-0000-1000-8000-00805f9b34fb":
                    sb.append("BATTERY_SERVICE: ");
                    number = serviceData.get(pu)[0];
                    str = String.valueOf(number);
                    break;
                //USER_DATA
                case "0000181c-0000-1000-8000-00805f9b34fb":
                    sb.append("USER_DATA: ");
                    str = new String(serviceData.get(pu), StandardCharsets.UTF_8);
                    break;
                //USER_INDEX
                case "00002a9a-0000-1000-8000-00805f9b34fb":
                    sb.append("USER_INDEX: ");
                    str = new String(serviceData.get(pu), StandardCharsets.UTF_8);
                    break;
                //LANGUAGE
                case "00002aa2-0000-1000-8000-00805f9b34fb":
                    sb.append("LANGUAGE: ");
                    str = new String(serviceData.get(pu), StandardCharsets.UTF_8);
                    break;
                //FIRST_NAME
                case "00002a8a-0000-1000-8000-00805f9b34fb":
                    sb.append("FIRST_NAME: ");
                    str = new String(serviceData.get(pu), StandardCharsets.UTF_8);
                    break;
                //LAST_NAME
                case "00002a90-0000-1000-8000-00805f9b34fb":
                    sb.append("LAST_NAME: ");
                    str = new String(serviceData.get(pu), StandardCharsets.UTF_8);
                    break;
                //DATE_TIME
                case "00002a08-0000-1000-8000-00805f9b34fb":
                    sb.append("DATE_TIME: ");
                    break;
                //CURRENT_TIME
                case "00001805-0000-1000-8000-00805f9b34fb":
                    sb.append("CURRENT_TIME: ");
                    break;
                //ALERT_NOTIFICATION
                case "00001811-0000-1000-8000-00805f9b34fb":
                    sb.append("ALERT_NOTIFICATION: ");
                    break;
                //GENERIC_ATTRIBUTE
                case "00001801-0000-1000-8000-00805f9b34fb":
                    sb.append("GENERIC_ATTRIBUTE: ");
                    break;
                //LOCATION_AND_NAVIGATION
                case "00001819-0000-1000-8000-00805f9b34fb":
                    sb.append("LOCATION_AND_NAVIGATION: ");
                    str = new String(serviceData.get(pu), StandardCharsets.UTF_8);
                    break;
                //LOCATION_NAME
                case "00002ab5-0000-1000-8000-00805f9b34fb":
                    sb.append("LOCATION_NAME: ");
                    str = new String(serviceData.get(pu), StandardCharsets.UTF_8);
                    break;
                //NAVIGATION
                case "00002a68-0000-1000-8000-00805f9b34fb":
                    sb.append("NAVIGATION: ");
                    break;
                default:
                    sb.append(pu);
                    str = new String(serviceData.get(pu), StandardCharsets.UTF_8);
                    break;
            }
            //sb.append(pu);
            //String str = new String(serviceData.get(pu), StandardCharsets.UTF_8);
            sb.append(" => ");
            sb.append(str);
            sb.append("\n");
        }

        return sb.toString();
    }

    public void startScanning() {
        System.out.println("start scanning");
        startScanningButton.setVisibility(View.INVISIBLE);
        stopScanningButton.setVisibility(View.VISIBLE);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.startScan(leScanCallback);
            }
        });
    }

    public void stopScanning() {
        System.out.println("stopping scanning");
        startScanningButton.setVisibility(View.VISIBLE);
        stopScanningButton.setVisibility(View.INVISIBLE);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.stopScan(leScanCallback);
            }
        });
    }
*/
}