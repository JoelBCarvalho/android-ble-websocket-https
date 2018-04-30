package com.github.nkzawa.socketio.androidchat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;


/**
 * A chat fragment containing messages view and input form.
 */
public class MainFragment extends Fragment {

    private static final String TAG = "MainFragment";

    private static final int REQUEST_LOGIN = 0;

    private static final int TYPING_TIMER_LENGTH = 600;
    public static final int RSSI_MAX_VALUE = 999;

    private RecyclerView mMessagesView;
    private EditText mInputMessageView;
    private List<Message> mMessages = new ArrayList<Message>();
    private RecyclerView.Adapter mAdapter;
    private boolean mTyping = false;
    private Handler mTypingHandler = new Handler();
    private String mUsername;
    private Socket mSocket;

    private Boolean isConnected = true;

    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    BluetoothLeScanner btScanner;
    GattServices gattServices;
    Button startScanningButton;
    Button stopScanningButton;
    TextView peripheralTextView;
    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int MANUFACTURER_SPECIFIC_DATA = 0XFF;

    ArrayList<String> allowedDevices = new ArrayList<String>();

    private static int COUNTER_TO_REFRESH = 2;
    private int closestValue = 999;
    private String closestDevice = "";
    long startTime = 0;
    long endTime = 0;


    //total sum, counter, avg
    Map<String, Triplet<Integer,Integer,Integer>> averageRssi = new HashMap<>();
    Map<String, Triplet<Integer,Integer, String>> deviceCalibration = new HashMap<>();

    public void setDeviceCalibration() {
        this.deviceCalibration.put("C3:EE:8A:25:95:BE", new Triplet(1, 77, "yellow"));//yellow
        this.deviceCalibration.put("D6:2E:68:99:5A:23", new Triplet(1, 67, "orange"));//orange
        this.deviceCalibration.put("F0:FF:9A:76:F2:52", new Triplet(1, 64, "pink"));//pink
        this.deviceCalibration.put("EA:93:6D:E3:D6:18", new Triplet(1, 64, "green"));//green

    }

    public void setAllowedDevices() {
        this.allowedDevices.add("E3:EF:56:0C:C0:37");
        this.allowedDevices.add("FF:01:16:F0:08:9B");
        this.allowedDevices.add("E6:40:DC:44:5C:9D");
    }

    public ArrayList<String> getAllowedDevices() {
        return allowedDevices;
    }

    public MainFragment() {
        super();
    }


    // This event fires 1st, before creation of fragment or any views
    // The onAttach method is called when the Fragment instance is associated with an Activity.
    // This does not mean the Activity is fully initialized.
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mAdapter = new MessageAdapter(context, mMessages);
        if (context instanceof Activity){
            //this.listener = (MainActivity) context;
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        ChatApplication app = (ChatApplication) getActivity().getApplication();
        mSocket = app.getSocket();
        mSocket.on(Socket.EVENT_CONNECT,onConnect);
        mSocket.on(Socket.EVENT_DISCONNECT,onDisconnect);
        mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        mSocket.on("new message", onNewMessage);
        mSocket.on("user joined", onUserJoined);
        mSocket.on("user left", onUserLeft);
        mSocket.on("typing", onTyping);
        mSocket.on("stop typing", onStopTyping);
        mSocket.connect();

        startSignIn();

        final Handler h = new Handler();
        h.postDelayed(new Runnable()
        {
            private long time = 0;

            @Override
            public void run()
            {
                // do stuff then
                // can call h again after work!
                time += 1000;
                //Log.d("TimerExample", "Going for... " + time);

                refreshBeacons();
                //attemptSend("ble", closestDevice);
                //closestValue = 999;

                h.postDelayed(this, 500);
            }
        }, 500); // 1 second delay (takes millis)
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mSocket.disconnect();

        mSocket.off(Socket.EVENT_CONNECT, onConnect);
        mSocket.off(Socket.EVENT_DISCONNECT, onDisconnect);
        mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.off(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        mSocket.off("new message", onNewMessage);
        mSocket.off("user joined", onUserJoined);
        mSocket.off("user left", onUserLeft);
        mSocket.off("typing", onTyping);
        mSocket.off("stop typing", onStopTyping);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setAllowedDevices();
        setDeviceCalibration();

        startScanningButton = (Button) view.findViewById(R.id.StartScanButton);
        startScanningButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startScanning();
            }
        });

        stopScanningButton = (Button) view.findViewById(R.id.StopScanButton);
        stopScanningButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopScanning();
            }
        });
        stopScanningButton.setVisibility(View.INVISIBLE);

        btManager = (BluetoothManager)getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();
        if (btAdapter != null && !btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent,REQUEST_ENABLE_BT);
        }

        int permissionCheck = ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)){
                Toast.makeText(getActivity(), "The permission to get BLE location data is required", Toast.LENGTH_SHORT).show();
            }else{
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }else{
            Toast.makeText(getActivity(), "Location permissions already granted", Toast.LENGTH_SHORT).show();
        }
/*
        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity().getApplicationContext());
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

        mMessagesView = (RecyclerView) view.findViewById(R.id.messages);
        mMessagesView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mMessagesView.setAdapter(mAdapter);

        mInputMessageView = (EditText) view.findViewById(R.id.message_input);
        mInputMessageView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int id, KeyEvent event) {
                if (id == R.id.send || id == EditorInfo.IME_NULL) {
                    attemptSend();
                    return true;
                }
                return false;
            }
        });
        mInputMessageView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (null == mUsername) return;
                if (!mSocket.connected()) return;

                if (!mTyping) {
                    mTyping = true;
                    mSocket.emit("typing");
                }

                mTypingHandler.removeCallbacks(onTypingTimeout);
                mTypingHandler.postDelayed(onTypingTimeout, TYPING_TIMER_LENGTH);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        ImageButton sendButton = (ImageButton) view.findViewById(R.id.send_button);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptSend();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Activity.RESULT_OK != resultCode) {
            getActivity().finish();
            return;
        }

        mUsername = data.getStringExtra("username");
        int numUsers = data.getIntExtra("numUsers", 1);

        addLog(getResources().getString(R.string.message_welcome));
        addParticipantsLog(numUsers);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_leave) {
            leave();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void addLog(String message) {
        mMessages.add(new Message.Builder(Message.TYPE_LOG)
                .message(message).build());
        mAdapter.notifyItemInserted(mMessages.size() - 1);
        scrollToBottom();
    }

    private void addParticipantsLog(int numUsers) {
        addLog(getResources().getQuantityString(R.plurals.message_participants, numUsers, numUsers));
    }

    private void addMessage(String username, String message) {
        mMessages.add(new Message.Builder(Message.TYPE_MESSAGE)
                .username(username).message(message).build());
        mAdapter.notifyItemInserted(mMessages.size() - 1);
        scrollToBottom();
    }

    private void addTyping(String username) {
        mMessages.add(new Message.Builder(Message.TYPE_ACTION)
                .username(username).build());
        mAdapter.notifyItemInserted(mMessages.size() - 1);
        scrollToBottom();
    }

    private void removeTyping(String username) {
        for (int i = mMessages.size() - 1; i >= 0; i--) {
            Message message = mMessages.get(i);
            if (message.getType() == Message.TYPE_ACTION && message.getUsername().equals(username)) {
                mMessages.remove(i);
                mAdapter.notifyItemRemoved(i);
            }
        }
    }

    private void attemptSend(String user, String message) {
        if (null == user) return;
        if (!mSocket.connected()) return;

        mTyping = false;

        if (TextUtils.isEmpty(message)) {
            mInputMessageView.requestFocus();
            return;
        }

        mInputMessageView.setText("");
        addMessage(user, message);

        // perform the sending message attempt.
        mSocket.emit("new message", message);
    }

    private void attemptSend() {
        if (null == mUsername) return;
        if (!mSocket.connected()) return;

        mTyping = false;

        String message = mInputMessageView.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            mInputMessageView.requestFocus();
            return;
        }

        mInputMessageView.setText("");
        addMessage(mUsername, message);

        // perform the sending message attempt.
        mSocket.emit("new message", message);
    }

    private void startSignIn() {
        mUsername = null;
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        startActivityForResult(intent, REQUEST_LOGIN);
    }

    private void leave() {
        mUsername = null;
        mSocket.disconnect();
        mSocket.connect();
        startSignIn();
    }

    private void scrollToBottom() {
        mMessagesView.scrollToPosition(mAdapter.getItemCount() - 1);
    }

    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(!isConnected) {
                        if(null!=mUsername)
                            mSocket.emit("add user", mUsername);
                        Toast.makeText(getActivity().getApplicationContext(),
                                R.string.connect, Toast.LENGTH_LONG).show();
                        isConnected = true;
                    }
                }
            });
        }
    };

    private Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "diconnected");
                    isConnected = false;
                    Toast.makeText(getActivity().getApplicationContext(),
                            R.string.disconnect, Toast.LENGTH_LONG).show();
                }
            });
        }
    };

    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.e(TAG, "Error connecting");
                    Toast.makeText(getActivity().getApplicationContext(),
                            R.string.error_connect, Toast.LENGTH_LONG).show();
                }
            });
        }
    };

    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    String message;
                    try {
                        username = data.getString("username");
                        message = data.getString("message");
                    } catch (JSONException e) {
                        Log.e(TAG, e.getMessage());
                        return;
                    }

                    removeTyping(username);
                    addMessage(username, message);
                }
            });
        }
    };

    private Emitter.Listener onUserJoined = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    int numUsers;
                    try {
                        username = data.getString("username");
                        numUsers = data.getInt("numUsers");
                    } catch (JSONException e) {
                        Log.e(TAG, e.getMessage());
                        return;
                    }

                    addLog(getResources().getString(R.string.message_user_joined, username));
                    addParticipantsLog(numUsers);
                }
            });
        }
    };

    private Emitter.Listener onUserLeft = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    int numUsers;
                    try {
                        username = data.getString("username");
                        numUsers = data.getInt("numUsers");
                    } catch (JSONException e) {
                        Log.e(TAG, e.getMessage());
                        return;
                    }

                    addLog(getResources().getString(R.string.message_user_left, username));
                    addParticipantsLog(numUsers);
                    removeTyping(username);
                }
            });
        }
    };

    private Emitter.Listener onTyping = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    try {
                        username = data.getString("username");
                    } catch (JSONException e) {
                        Log.e(TAG, e.getMessage());
                        return;
                    }
                    addTyping(username);
                }
            });
        }
    };

    private Emitter.Listener onStopTyping = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    try {
                        username = data.getString("username");
                    } catch (JSONException e) {
                        Log.e(TAG, e.getMessage());
                        return;
                    }
                    removeTyping(username);
                }
            });
        }
    };

    private Runnable onTypingTimeout = new Runnable() {
        @Override
        public void run() {
            if (!mTyping) return;

            mTyping = false;
            mSocket.emit("stop typing");
        }
    };

    ///BLE
    // Device scan callback.
    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            //if(result.getDevice().getAddress().equalsIgnoreCase("e3:ef:56:0C:c0:37")) {
            //if(getAllowedDevices().contains(result.getDevice().getAddress())) {
            //if(result.getDevice().getName() != null && result.getDevice().getName().startsWith("Puck")) {
            updateDevicesRssi(result.getDevice().getAddress(), result.getRssi());
            //String isBounded = result.getDevice().getBondState() != 10 ? "Yes" : "No";
            //}
            /*String closestDevice = result.getDevice().getName() + ";" +
                    averageRssi.get(result.getDevice().getAddress()).third + ";" +
                    result.getRssi() + ";" +
                    result.getDevice().getAddress() + ";" +
                    isBounded + ";" +
                    result.getDevice().getType() + ";" +
                    getServiceName(result) + "\n";
            attemptSend("ble", closestDevice);*/
            //checkRefreshStatus();
        }
    };

    private boolean checkRefreshStatus() {
        //if all value collected and have been at least 5 then flag to refresh
        boolean refreshNow = true;

        if(averageRssi.size() < 3) {
            return !refreshNow;
        }

        //if all beacons were registered at least X times
        /*for(String address : averageRssi.keySet()){
            if(averageRssi.get(address).second < COUNTER_TO_REFRESH) {
                refreshNow = false;
                break;
            }
        }*/
        //if any beacon was registeres at least X times
        for(String address : averageRssi.keySet()){
            if(averageRssi.get(address).second < COUNTER_TO_REFRESH) {
                refreshNow = false;
                break;
            }
        }
        //avoid old values noise by restarting
        if(refreshNow) {
            refreshBeacons();
        }

        return refreshNow;
    }

    private void refreshBeacons() {
        String device = "";
        Integer rssiValue = RSSI_MAX_VALUE;
        double acum_rssi = 0;
        List<Long> values = new ArrayList<>();
        int counter = 0;
        if(averageRssi.size()>3) {
            Map<String, Triplet<Integer,Integer,Integer>> sortedMapDesc = sortByComparator(averageRssi, true);

            for (String address : sortedMapDesc.keySet()) {
                Triplet<Integer, Integer, Integer> value = sortedMapDesc.get(address);
                Integer stableAvgRssi = value.third;
                counter++;

                int meterRef = 70;
                String color = "";
                if(deviceCalibration.get(address) != null) {
                    meterRef = deviceCalibration.get(address).second;
                    color = deviceCalibration.get(address).third;
                } else {
                    meterRef = 1;
                }
                double distMeters = Math.pow(2, ((double) (-stableAvgRssi + meterRef) / -6));
                acum_rssi = acum_rssi + Math.round(distMeters);
                values.add(Math.round(distMeters));
                device = address + ";" + stableAvgRssi + ";" + Math.round(distMeters) + ";" + color;
                rssiValue = stableAvgRssi;

                attemptSend("ble", device + ";\n");

                /*
                if (stableAvgRssi < RSSI_MAX_VALUE ) {
                    closestDevice = device + ";!\n";
                    closestValue = rssiValue;
                    attemptSend("ble", closestDevice + ";!\n");
                } else {f
                }*/

                //Triplet<Integer, Integer, Integer> refreshedValue = new Triplet<>(stableAvgRssi, 1, stableAvgRssi);
                //averageRssi.put(address, refreshedValue);
            }

            attemptSend("mean", (acum_rssi/counter) + "");
            attemptSend("median", median(values)+"");
            attemptSend("mode", mode(values)+"");

            averageRssi.clear();
        }
    }

    // the array double[] m MUST BE SORTED
    public static double median(List<Long> m) {
        int middle = m.size()/2;
        if (m.size()%2 == 1) {
            return m.get(middle);
        } else {
            return m.get(middle-1) + m.get(middle) / 2.0;
        }
    }

    public static double mode(List<Long> a) {
        long maxValue =0, maxCount = 0;

        for (int i = 0; i < a.size(); ++i) {
            int count = 0;
            for (int j = 0; j < a.size(); ++j) {
                if (a.get(j) == a.get(i)) ++count;
            }
            if (count > maxCount) {
                maxCount = count;
                maxValue = a.get(i);
            }
        }

        return maxValue;
    }

    public void updateDevicesRssi(String address, int newRssi) {
        Triplet<Integer,Integer, Integer> getOldValue = averageRssi.get(address);
        Integer positiveNewRssi = Math.abs(newRssi);
        if(getOldValue == null) {
            averageRssi.put(address, new Triplet(positiveNewRssi, 1, positiveNewRssi));
        } else {
            Integer oldTotalSum = getOldValue.first;
            Integer oldCounter = getOldValue.second;
            Integer oldAvgRssi = getOldValue.third;

            Integer newTotalSum = oldTotalSum + positiveNewRssi;
            Integer newCounter = oldCounter+1;
            Integer newAvgRssi = Integer.valueOf(Math.floorDiv(newTotalSum, newCounter));

            Triplet<Integer, Integer, Integer> newValue = new Triplet(newTotalSum, oldCounter+1, newAvgRssi);

            averageRssi.put(address, newValue);
        }

        /*
        int newRssiAbs = Math.abs(newRssi);
        if(closestValue > newRssiAbs){
            closestValue = newRssiAbs;
        }

        double distMeters = Math.pow(2, (closestValue-74)/6);
        String closestDevice = "min dist: " + Math.round(distMeters)  + closestValue + "\n";
        attemptSend("ble", closestDevice);*/
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity().getApplicationContext());
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
                ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
                List<ScanFilter> filters = createScanFilters();

                btScanner.startScan(filters, settings, leScanCallback);

                //btScanner.startScan(leScanCallback);
            }
        });
    }

    public void stopScanning() {
        System.out.println("stopping scanning");
        startScanningButton.setVisibility(View.VISIBLE);
        stopScanningButton.setVisibility(View.INVISIBLE);
        averageRssi.clear();
        closestDevice = "";
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.stopScan(leScanCallback);
            }
        });
    }

    private List<ScanFilter> createScanFilters() {
        ScanFilter.Builder builder = new ScanFilter.Builder();
        ArrayList<ScanFilter> scanFilters = new ArrayList<>();
        /*UUID asServiceId = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
        builder.setServiceUuid(new ParcelUuid(asServiceId));
        scanFilters.add(builder.build());
*/
        ScanFilter.Builder builder1 = new ScanFilter.Builder();
        UUID asServiceId1 = UUID.fromString("00001803-494c-4f47-4943-544543480000");
        builder1.setServiceUuid(new ParcelUuid(asServiceId1));
        scanFilters.add(builder1.build());
/*
        ScanFilter.Builder builder2 = new ScanFilter.Builder();
        builder2.setDeviceAddress("FF:01:16:F0:08:9B");
        scanFilters.add(builder2.build());

        ScanFilter.Builder builder3 = new ScanFilter.Builder();
        builder3.setDeviceAddress("E6:40:DC:44:5C:9D");
        scanFilters.add(builder3.build());
*/

        return scanFilters;
    }

    private static Map<String, Triplet<Integer,Integer,Integer>> sortByComparator(Map<String, Triplet<Integer,Integer,Integer>> unsortMap, final boolean order)
    {

        List<Map.Entry<String, Triplet<Integer,Integer,Integer>>> list = new LinkedList<Map.Entry<String, Triplet<Integer,Integer,Integer>>>(unsortMap.entrySet());

        // Sorting the list based on values
        Collections.sort(list, new Comparator<Map.Entry<String, Triplet<Integer,Integer,Integer>>>()
        {
            public int compare(Map.Entry<String, Triplet<Integer,Integer,Integer>> o1,
                               Map.Entry<String, Triplet<Integer,Integer,Integer>> o2)
            {
                if (order)
                {
                    return o1.getValue().third.compareTo(o2.getValue().third);
                }
                else
                {
                    return o2.getValue().third.compareTo(o1.getValue().third);

                }
            }
        });

        // Maintaining insertion order with the help of LinkedList
        Map<String, Triplet<Integer,Integer,Integer>> sortedMap = new LinkedHashMap<String, Triplet<Integer,Integer,Integer>>();
        for (Map.Entry<String, Triplet<Integer,Integer,Integer>> entry : list)
        {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

    public class Triplet<T, U, V> {

        private final T first;
        private final U second;
        private final V third;

        public Triplet(T first, U second, V third) {
            this.first = first;
            this.second = second;
            this.third = third;
        }

        public T getFirst() { return first; }
        public U getSecond() { return second; }
        public V getThird() { return third; }
    }
}

