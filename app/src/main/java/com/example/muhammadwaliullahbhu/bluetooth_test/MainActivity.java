package com.example.muhammadwaliullahbhu.bluetooth_test;

//Imports---------------------------------------------------------------------------------------------
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.util.Log;
import android.content.Intent;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.UUID;

//------------------------------------------------------------------------------------------------------



public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    ////Global variables---------------------------------------------------------------------------------
    private static final String TAG = "MainActivity ";
    BluetoothAdapter myBluetoothAdapter;

    // Connection related
    BluetoothChatService mBluetoothConnection;
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    BluetoothDevice mBTDevice;


    // Lists and Adapters
    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
    public DeviceListAdapter mDeviceListAdapter;
    ListView ListViewDeviceList;

    //Buttons
    //Button btnOnOff;
    //Button btnDiscoverable;
    Button btnSend;

    //Switch
    Switch swBtOnOff;
    Switch swDiscoverable;
    Switch swAutoDiscovery;

    //Edit Texts
    EditText etMessageSend;

    //Text Views
    TextView tvMessage;
    TextView tvMessageTitle;
    TextView tvAvailableDevices;

    //Progress bar circle---
    ProgressBar progressBarDiscoverDevice;


    // Broadcast Receivers
    // Create a BroadcastReceiver for ACTION_FOUND------------------------------------------
    private final BroadcastReceiver myBR_btOnOff = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(myBluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        myBluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "myBroadcastREceiver1: STATE TURNING OFF");
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "myBroadcastREceiver1: STATE ON");
                        //setting the switch Button text----------------
                        toggleText_swBTOnOff();
                        //start discovering remote bluetooth devices-----
                        discoverBTDevices();
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "onReceive: STATE TURNING ON");
                }
                //Toggle Text of Bluetooth on/off btn when this button is pressed.
                //toggleText_btnOnOff(btnOnOff);
            }
        }
    };



    // Broadcast Receiver for changes made due to Discoverability On/Off---------------------------------
    private final BroadcastReceiver myBR_OnDiscoveribility = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {
                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                switch (mode) {
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG, " myBR_OnDiscoveribility: Discoveribility Enabled");
                        showThisToast("Discoverable mode is on.");
                        //start chatService and listening as server device-----
                        mBluetoothConnection = new BluetoothChatService(MainActivity.this);
                        mBluetoothConnection.startServer();
                        break;

                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG, "myBR_OnDiscoveribility: Discoveribility Disabled. Able to receive connection");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG, "myBR_OnDiscoveribility: Discoveribility Disabled. Not able to receive connection");
                        swDiscoverable.setChecked(false);
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG, "myBR_OnDiscoveribility: Connecting..");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG, "myBR_OnDiscoveribility: Connected!");
                        break;
                }
            }
        }
    };




    // Broadcast Receiver to Discover Devices-------------------------------------------------------------
    private BroadcastReceiver myBR_OnDiscoverDevices = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: DiscoverDevices");
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mBTDevices.add(device);
                Log.d(TAG, "onReceive: ACTION FOUND:  " + device.getName()
                                        + ": " + device.getAddress());

                // Creating and setting the adapter for ListViewDeviceList(ListView)
                mDeviceListAdapter = new DeviceListAdapter(
                        context, R.layout.device_adapter_view, mBTDevices);
                ListViewDeviceList.setAdapter(mDeviceListAdapter);
            }

            if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)){
                Log.d(TAG, "onReceive: ACTION_DISCOVERY_STARTED.");
                progressBarDiscoverDevice.setIndeterminate(true);
                progressBarDiscoverDevice.setVisibility(View.VISIBLE);
            }
            if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)){
                Log.d(TAG, "onReceive: ACTION_DISCOVERY_FINISHED.");
                progressBarDiscoverDevice.setIndeterminate(false);
                progressBarDiscoverDevice.setVisibility(View.INVISIBLE);

                if( swAutoDiscovery.isChecked() == true){
                    activateAutoDiscovery();
                }
            }
        }
    };



    // Broadcast Receiver to create the bond(pairing) with other devices--------------------------
    private BroadcastReceiver myBR_OnBonding = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                // 3 cases
                //case1: bonded already
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, "myBR_OnBonding: BOND_BONDED");
                    mBTDevice = mDevice;
                }

                //case2: creating a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "myBR_OnBonding: BOND_BONDING");
                }

                //case3: breaking a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "myBR_OnBonding: BOND_NONE");
                }
            }
        }
    };


    //--------------------------------------- End of Global variables -------------------------------------////




    /* Override Methods  ***************************************************************** */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Retrieving defaultAdapter -------------------------------
        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Remote Device List related -------------------------------
        tvAvailableDevices = (TextView) findViewById(R.id.tvAvailableDevicesID);
        ListViewDeviceList = (ListView) findViewById(R.id.ListViewDevicesID);

        // Buttons and buttons related -----
        btnSend = (Button) findViewById(R.id.btnMessageSendID);
        etMessageSend = (EditText) findViewById(R.id.etMessageSendID);

        // Switch Bluetooth on/off--------------------
        swBtOnOff = (Switch) findViewById(R.id.swBtOnOffID);
        swBtOnOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton btnView, boolean isChecked) {
                Log.d(TAG, "switchBT: switch button is pressed..");
                switchBT_OnOff(isChecked);
            }
        });


        // Switch to toggle Discoverable on/off--------------------
        swDiscoverable = (Switch) findViewById(R.id.swDiscoverableID);
        swDiscoverable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton btnView, boolean isChecked) {
                Log.d(TAG, "swDiscoverable: Toggle Discoverable. isChecked = "+isChecked);
                toggle_Discoverable(isChecked);
            }
        });

        // Switch Auto Discovery-----------------
        swAutoDiscovery = (Switch) findViewById(R.id.swAutoDiscoveryID);
        swAutoDiscovery.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton btnView, boolean isChecked) {
                toggle_autoDiscovery(isChecked);
            }
        });

        // progress bar --------------------------------
        progressBarDiscoverDevice = (ProgressBar) findViewById(R.id.progressBarDiscoverDeviceID);

        // Message related -----------------------------
        tvMessageTitle = (TextView) findViewById(R.id.tvMessageTitleID);
        tvMessage = (TextView) findViewById(R.id.tvMessageID);

        // Make each Device on the list clickable----------------
        ListViewDeviceList.setOnItemClickListener(MainActivity.this);

        // Toggle Text of Bluetooth on/off Button
        toggleText_swBTOnOff();

        //discover other Bluetooth devices-----------------------
        if(swBtOnOff.isChecked()){
            Log.d(TAG, "oncreate: swBtOnOff.isChecked(): discovering Devices.");
            discoverBTDevices();
        }

        // BroadCasts when Bond state changes (pairing) -----------------------
        IntentFilter bondIntentFilter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(myBR_OnBonding, bondIntentFilter);

        // Switch on the Bluetooth at the startup-------------
        swBtOnOff.setChecked(true);

        // Oncreate View Visiblity----

    }




    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            unregisterReceiver(myBR_btOnOff);
        } catch(Exception e){};

        try{
        unregisterReceiver(myBR_OnDiscoveribility);
        }catch(Exception e){};

        try {
            unregisterReceiver(myBR_OnDiscoverDevices);
        }catch(Exception e){};

        try{
        unregisterReceiver(myBR_OnBonding);
        }catch(Exception e){};

        myBluetoothAdapter.disable();
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

        //At first cancel discovery because it is very memory intensive.
        stopBT_Discovery();

        // Getting the details of the Clicked Device.
        Log.d(TAG, "onItemClick: You Clicked on a device.");
        BluetoothDevice clickedDevice = mBTDevices.get(position);

        if(clickedDevice != null) {
            Log.d(TAG, "onItemClick: Device found");
            String deviceName = clickedDevice.getName();
            String deviceAddress = clickedDevice.getAddress();

            Log.d(TAG, "onItemClick: deviceName = " + deviceName);
            Log.d(TAG, "onItemClick: deviceAddress = " + deviceAddress);

            //Create the bond (pairing)
            // Note: Requires API 18+
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
                Log.d(TAG, "Trying to pair with " + deviceName);
                clickedDevice.createBond();
                mBTDevice = clickedDevice;
                //mBluetoothConnection = new BluetoothChatService(MainActivity.this);
            }

            // start Bluetooth connection as a client ----------------
            startBTConnection(mBTDevice, MY_UUID_INSECURE);
        }
        else{
            Log.d(TAG, "onItemClick: Device not found");
            showThisToast("Selected Device is not available anymore.");
        }
    }




    /* Custom Methods **************************************************************************

    1) switchBT_OnOff()                   // Switching Bluetooth on/off.
    2) toggle_Discoverable()               // Toggle Bluetooth Discoverability.
    3) btnSend_onClick()                  // Send message over the bluetooth.
    4) discoverBTDevices()                // Discover other devices.
    5) enableDiscoverability()            // Enable Discoverability.
    6) disableDiscoverability()           // Disable Discoverability.
    7) startBTConnection()                // Start chat service method.
    8) clearningMessageTextViews()        // Clearing message related textViews.
    9) update_tvMessage()                 // Update message TextView with received message.
    10) toggleText_swBTOnOff()            // Toggling the Text of Bluetooth on/off Button.
    11) clearingDeviceList()              // Clearing ListViewDeviceList.
    12) showThisToast()                   // Showing a toast message passed as parameter to this method.
    13) CheckBTPermission()               // permission check for android API version greater than 23.
    14) setViewsInvisible()
    15) setViewsVisible()
    16) toggle_autoDiscovery()
    17) activateAutoDiscovery()           // activate AutoDiscovery process.

    ******************************************************************************************** */


    // (1) Switching Bluetooth on/off ------------------------------------------------------------
    private void switchBT_OnOff(Boolean isChecked){
        Log.d(TAG, "switchBT_OnOff: start.. ");
        if(isChecked == true){
            Log.d(TAG, "switchBT: switchBT_OnOff: isChecked = True ");
            if (myBluetoothAdapter == null) {
                Log.d(TAG, "enableDisableBT: Does not have BT capablities.");
                swBtOnOff.setChecked(false);
            }
            else if (!myBluetoothAdapter.isEnabled()) {
                //enabling Bluetooth--
                myBluetoothAdapter.enable();
                swBtOnOff.setText("On");
                //Intent enableBTIntent = new Intent(myBluetoothAdapter.ACTION_REQUEST_ENABLE);
                //startActivity(enableBTIntent);
                IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                registerReceiver(myBR_btOnOff, BTIntent);
            }
            setViewsVisible();
        }
        else if (isChecked == false){
            Log.d(TAG, "switchBT: switchBT_OnOff: isChecked = False ");
            if(myBluetoothAdapter != null) {
                myBluetoothAdapter.disable();
                swBtOnOff.setText("Off");
                clearingDeviceList();

                setViewsInvisible();
            }
        }
        Log.d(TAG, "switchBT_OnOff: end.. ");
    }



    // (2) Toggle Bluetooth Discoverability ----------------------------------------------------------------
    private void toggle_Discoverable(Boolean isChecked){

        Log.d(TAG, "toggle_Discoverable: Start.");
        //check if BT is on---
        if(!myBluetoothAdapter.isEnabled()){
            switchBT_OnOff(isChecked);
        }

        if(isChecked == true){
            // clearing tvMessageTitle----------------------
            clearningMessageTextViews();

            // closing previous connections -------------------------------------------------------
            if (mBluetoothConnection != null) {
                Log.d(TAG, "toggle_Discoverable: Closing prev connections..");
                mBluetoothConnection.closeAllThreads();
            }

            // Setting the Filter for Bluetooth ACTION_SCAN_MODE_CHANGED-----
            IntentFilter intentFilter = new IntentFilter(myBluetoothAdapter.
                    ACTION_SCAN_MODE_CHANGED);
            registerReceiver(myBR_OnDiscoveribility, intentFilter);

            // enable discoverability to be seen by other bluetooth devices-------------------------
            if (myBluetoothAdapter.getScanMode() !=
                    BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                Log.d(TAG, "toggle_Discoverable: Making Device discoverable..");
                enableDiscoverability();
                //Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                //discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, duration);
                //startActivity(discoverableIntent);
            }

            // create BluetoothChatService object if Discoverability is on-----------------------
            else if (myBluetoothAdapter.getScanMode() == BluetoothAdapter.
                    SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                // Start bluetooth server to listen for remote device -----------
                mBluetoothConnection = new BluetoothChatService(MainActivity.this);
                mBluetoothConnection.startServer();
            }
        }
        else if(isChecked == false){
            mBluetoothConnection.closeServer();
            disableDiscoverability();
        }
    }




    // (3) Send message over the bluetooth-----------------------------------------------------------
    public void btnSend_onClick(View view) {
        String etSendMessage = etMessageSend.getText().toString();
        Log.d(TAG, "btnSend_onClick: etSendMessage:" + etSendMessage + ":--");

        if (!etSendMessage.matches("")) {
            //Log.d(TAG, "btnSend_onClick: inside if--");

            if (mBluetoothConnection != null) {
                byte[] bytes = etSendMessage.getBytes(Charset.defaultCharset());
                mBluetoothConnection.write(bytes);
            } else if (mBluetoothConnection == null) {
                //showing this toast at the bottom of the screen----------
                showThisToast("No device is connected.");
            }

            etMessageSend.setText("");
        }
    }




    // (4) Discover other devices-----------------------------------------------------------------
    private void discoverBTDevices(){
        //cancelDiscovery if it was running previously--
        //myBluetoothAdapter.cancelDiscovery();

        Log.d(TAG, "btnDiscover: Looking for unpaired devices.");
        //clearing the ListView (if previously used)-----------
        clearingDeviceList(); //clearing the ListView (if previously used)

        // Closing all ChatService threads --------------------
        if (mBluetoothConnection != null) {
            mBluetoothConnection.closeAllThreads();
        }
        CheckBTPermission();
        // startDiscovery() will start finding bluetooth devices for 12 seconds--------------------
        myBluetoothAdapter.startDiscovery();

        IntentFilter discoverDevicesFilter = new IntentFilter();
        discoverDevicesFilter.addAction(BluetoothDevice.ACTION_FOUND);
        discoverDevicesFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        discoverDevicesFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(myBR_OnDiscoverDevices, discoverDevicesFilter);

        // Start bluetoothChatService object -----------------------------
        if (mBluetoothConnection == null) {
            mBluetoothConnection = new BluetoothChatService(MainActivity.this);
        }
    }




    // (5) Enable Discoverability---------------------------------------------
    private void enableDiscoverability(){
        Log.e(TAG,"enableDiscoverability: start.");

        Method method;
        try {
            //method reflection --- hidden method---- (no idea what's going on here..)---
            method = myBluetoothAdapter.getClass().
                                getMethod("setScanMode", int.class, int.class);

            Log.e(TAG,"enableDiscoverability: Discoverability successful");
            method.invoke(myBluetoothAdapter,
                                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE,1);
        }
        catch (Exception e){
            Log.e(TAG, "enableDiscoverability: Discoverability failed.");
        }
    }




    // (6) Disable Discoverability------------------------------------------------------
    private void disableDiscoverability() {
        Log.e(TAG,"disableDiscoverability: start..");
        try {
            //method reflection --- hidden method---- (no idea what's going on here..)---
            Method setDiscoverableTimeout = BluetoothAdapter.class.
                    getMethod("setDiscoverableTimeout", int.class);
            setDiscoverableTimeout.setAccessible(true);
            Method setScanMode = BluetoothAdapter.class.
                    getMethod("setScanMode", int.class, int.class);
            setScanMode.setAccessible(true);

            setDiscoverableTimeout.invoke(myBluetoothAdapter, 1);
            setScanMode.invoke(myBluetoothAdapter, BluetoothAdapter.SCAN_MODE_NONE, 1);
            Log.e(TAG,"disableDiscoverability: end..");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    // (7) Start chat service method------------------------------------------------------------
    private void startBTConnection(BluetoothDevice device, UUID uuid) {
        //clearing messageTextviews--------
        clearningMessageTextViews();

        Log.d(TAG, "startBTConnection: Initializing RFCOM Bluetooth Connection.");

        if ((device != null) && (uuid != null)) {
            Log.d(TAG, "startBTConnection: Device and UUID are valid..");
            if (mBluetoothConnection != null) {
                Log.d(TAG, "startBTConnection: mBluetoothConnection is also valid");
                mBluetoothConnection.startClient(device, uuid);
            } else {
                Log.d(TAG, "startBTConnection: mBluetoothConnection is NOT VALID.");
            }
        } else {
            Toast thisToast = Toast.makeText(MainActivity.this,
                    "Please select a bluetooth device form the list.", Toast.LENGTH_SHORT);
            thisToast.setGravity(Gravity.BOTTOM, 0, 0);
            thisToast.show();
        }
    }



    // (8)clearing message related textViews------------------------
    private void clearningMessageTextViews() {
        tvMessageTitle.setText("Message: ");
        tvMessage.setText("");
    }



    // (9) update the message text view with received message------------------
    private void update_tvMessage(String message) {
        tvMessage.setText(message);
    }



    // (10) Toggling the Text of Bluetooth on/off Button.
    // This method is called at the start of the program.-------------------------------
    private void toggleText_swBTOnOff(){
        if(myBluetoothAdapter.isEnabled()){
            Log.d(TAG, "toggleText_swBTOnOff: Bluetooth is on.");
            swBtOnOff.setText("On");
            if(swBtOnOff.isChecked() != true) {
                swBtOnOff.setChecked(true);
            }
            setViewsVisible();
        }
        else if(!myBluetoothAdapter.isEnabled()){
            Log.d(TAG, "toggleText_swBTOnOff: Bluetooth is off.");
            swBtOnOff.setText("Off");
            swBtOnOff.setChecked(false);
            clearingDeviceList();
            setViewsInvisible();
        }
    }




    // (11) clearing ListViewDeviceList for re-searching---------------------------------------------------------------
    private void clearingDeviceList(){
        ListViewDeviceList.setAdapter(null);
        mBTDevices.clear();
    }




    // (12)showing a toast with the message passing as paramter to this method---------------------
    private void showThisToast(String toastMSG){
        Toast toast = Toast.makeText(MainActivity.this,
                toastMSG, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM, 0, 0);
        toast.show();
    }





    // (13) permission check for android API version greater than 23-------------------------------------------
    private void CheckBTPermission(){
        Log.d(TAG, "CheckBTPermission: Checking Bluetooth Permissions.");
        //if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if(permissionCheck != 0){
                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
            }
        }
        else{
            Log.d(TAG, "checkBTPermission: No need to check permissions. SDK version < LOLLIPOP");
        }
    }





    // (14)
    private void setViewsInvisible(){
        swDiscoverable.setVisibility(View.INVISIBLE);
        swAutoDiscovery.setVisibility(View.INVISIBLE);
        tvAvailableDevices.setVisibility(View.INVISIBLE);
        progressBarDiscoverDevice.setVisibility(View.INVISIBLE);
        ListViewDeviceList.setVisibility(View.INVISIBLE);

        tvMessageTitle.setVisibility(View.INVISIBLE);
        tvMessage.setVisibility(View.INVISIBLE);
        etMessageSend.setVisibility(View.INVISIBLE);
        btnSend.setVisibility(View.INVISIBLE);

    }



    // (15)
    private void setViewsVisible(){
        swDiscoverable.setVisibility(View.VISIBLE);
        swAutoDiscovery.setVisibility(View.VISIBLE);
        tvAvailableDevices.setVisibility(View.VISIBLE);
        progressBarDiscoverDevice.setVisibility(View.VISIBLE);
        ListViewDeviceList.setVisibility(View.VISIBLE);

         // Make Message related Views invisible ---
        tvMessageTitle.setVisibility(View.INVISIBLE);
        tvMessage.setVisibility(View.INVISIBLE);
        etMessageSend.setVisibility(View.INVISIBLE);
        btnSend.setVisibility(View.INVISIBLE);


    }




    // (16)
    private void toggle_autoDiscovery(Boolean isChecked){
        if(swAutoDiscovery.isChecked() == true){
            discoverBTDevices();
        }
        else{
        }
    }


    // (17) start DiscoverBTdevice again when startDiscover() stops.......
    private void activateAutoDiscovery(){
        final long delayDuration = 3000; // in milliseconds..
        final Handler handler = new Handler();
        final Runnable task = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "ACTION_DISCOVERY_FINISHED: swAutoDiscovery.isChecked = True: " +
                        "activateAutoDiscovery: run(). ");
                showThisToast("Start Remote Bluetooth Device Discovery Process.");
                discoverBTDevices();
            }
        };
        handler.postDelayed(task, delayDuration);
    }



    // (18)
    private void stopBT_Discovery(){
        swAutoDiscovery.setChecked(false);
        myBluetoothAdapter.cancelDiscovery();
    }







    //// Custom Methods------------------------------------------------------------------------------------
    // Turning Bluetooth On/Off ------------------------------------------------------------------------
    /*
    public void btnBTOnOff_OnClick(View view) {
        if (myBluetoothAdapter == null) {
            Log.d(TAG, "enableDisableBT: Does not have BT capablities.");
        } else if (!myBluetoothAdapter.isEnabled()) {
            //myBluetoothAdapter.enable();
            Intent enableBTIntent = new Intent(myBluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);

            //myProgressDialog.dismiss();
        } else if (myBluetoothAdapter.isEnabled()) {
            myBluetoothAdapter.disable();
        }

        IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(myBR_btOnOff, BTIntent);
    }*/



    /*
    // BT Discoverable method---------------------------------------------------------------------------
    public void btnDiscoverable_OnClick(View view) {
        // clearing tvMessageTitle----------------------
        clearningMessageTextViews();

        // closing previous connections -------------------------------------------------------
        if (mBluetoothConnection != null) {
            Log.d(TAG, "btnDiscoverable_OnClick: Closing prev connections..");
            mBluetoothConnection.closeAllThreads();
        }

        // enable discoverability to be seen by other bluetooth devices-------------------------
        if (myBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            int duration = 10;
            Log.d(TAG, "btnDiscoverable_OnClick: Making Device discoverable for " +
                                                String.valueOf(duration) + " seconds.");

            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, duration);
            startActivity(discoverableIntent);

            IntentFilter intentFilter = new IntentFilter(myBluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
            registerReceiver(myBR_OnDiscoveribility, intentFilter);
        }

        // create BluetoothChatService object -----------------------
        else if (myBluetoothAdapter.getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            // Start bluetooth server to listen for remote device -----------
            mBluetoothConnection = new BluetoothChatService(MainActivity.this);
            mBluetoothConnection.startServer();
        }
    }*/


    /*
    // Discover other devices-----------------------------------------------------------------
    public void btnDiscoverDevices_onClick(View view) {
        Log.d(TAG, "btnDiscover: Looking for unpaired devices.");

        //clearing the ListView (if previously used)-----------
        clearingDeviceList(); //clearing the ListView (if previously used)

        // Closing all threads regarding connections --------------------
        if (mBluetoothConnection != null) {
            mBluetoothConnection.closeAllThreads();
        }

        // stopping previous discovery process if running-------
        if (myBluetoothAdapter.isDiscovering()) {
            myBluetoothAdapter.cancelDiscovery();
            Log.d(TAG, "btnDiscover: Canceling discovery.");
        }

        CheckBTPermission();

        // startDiscovery() will start finding bluetooth devices for 12 seconds--------------------
        myBluetoothAdapter.startDiscovery();

        IntentFilter discoverDevicesFilter = new IntentFilter();
        discoverDevicesFilter.addAction(BluetoothDevice.ACTION_FOUND);
        discoverDevicesFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        discoverDevicesFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(myBR_OnDiscoverDevices, discoverDevicesFilter);


        // Start bluetoothChatService object -----------------------------
        if (mBluetoothConnection == null) {
            mBluetoothConnection = new BluetoothChatService(MainActivity.this);
        }
    }*/



    /*
    // To startServer connection ----------------------------------------------------------------------
    public void btnStartConnection_onClick(View view) {

        // Method to startServer connection.
        // Remember the connection will fail and app will crash if the-
        // -intended device is not paired first.
        startBTConnection(mBTDevice, MY_UUID_INSECURE);
    }
    */


    /*
    // Toggling the Text of Bluetooth on/off Button------------------------------------------------------------
    private void toggleText_btnOnOff(Button btnOnOff){
        Log.d(TAG, "toggleText_btnOnOff: started..");

        if(myBluetoothAdapter.isEnabled()){
            btnOnOff.setText("Turn Off");

        }
        else if(!myBluetoothAdapter.isEnabled()){
            btnOnOff.setText("Turn On");
            clearingDeviceList();
        }
    }*/
}





