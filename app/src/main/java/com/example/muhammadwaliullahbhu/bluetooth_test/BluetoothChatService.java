package com.example.muhammadwaliullahbhu.bluetooth_test;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

/**
 * Created by MuhammadWaliUllahBhu on 12/4/2017.
 */




public class BluetoothChatService {

    // Global Variables --------------------------------------------------------
    private static final String TAG = "BluetoothChatService";
    private static final String appName = "myApp_BluetoothTest";
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final int SERVER = 1;
    private final int CLIENT = 2;

    private final BluetoothAdapter myBluetoothAdapter;
    Context myContext;

    private ListenThread myInsecureAcceptThread;

    private ConnectThread myConnectThread;

    private ConnectedThread myConnectedThread;

    private BluetoothDevice mDevice;
    private UUID  deviceUUID;
    ProgressDialog myProgressDialog;



    // Constructor ---------------------------------------------------------------
    public BluetoothChatService(Context context){
        myContext = context;
        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //startServer();
    }



    /* **********************************************************************
    We will define and use three Threads -
    1) ListenThread
    2) ConnectThread
    3) ConnectedThread
    ************************************************************************ */



    /* **********************************************************************
    1) ListenThread
    - This thread runs while Listening for incoming connections.
    - It behaves like a server-side client.
    - It runs until a connection is accepted or cancelled.
    ************************************************************************* */
    private class ListenThread extends Thread{

        private final BluetoothServerSocket myServerSocket;

        public ListenThread(){
            BluetoothServerSocket temp = null;

            // Create a new Listening server socket
            try {
                temp = myBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName,
                                                                            MY_UUID_INSECURE);
                Log.d(TAG, "ListenThread: Setting up this device as SERVER" +
                                                        " using: " + MY_UUID_INSECURE);
            }
            catch(IOException e){

            }
            myServerSocket = temp;
        }


        // This method will be called automatically when an 'ListenThread' type object is created.
        public void run(){
            Log.d(TAG, "run: ListenThread Running.");
            BluetoothSocket mySocket = null;

            try {
                /* ******************************************************************
                1) This is a blocking call and will only return on a successful connection or--
                   an exception.
                2) This accept() will wait to hear from a client socket to connect on the given Port.
                3) A socket on the client device tries to connect to the serverSocket--
                   with the given IP address and Port.
                4) If the connection is successful, client gets a socket which is capable of--
                   communicating with the server.
                5) Then on the server the accept() returns a socket--
                   that is manageConnectedSocket to the client's socket.
                ******************************************************************** */
                mySocket = myServerSocket.accept();
                showMessageViews();
                //getting the remote manageConnectedSocket device
                mDevice = mySocket.getRemoteDevice();

                Log.d(TAG, "run: RFCOMM server socket start..");
            }
            catch(IOException e){
                Log.e(TAG, "ListenThread IOExeption: " + e.getMessage());
            }

            if(mySocket != null){
                // calling to start the ConnectedThread and mentioning this is the server device----
                manageConnectedSocket(mySocket, mDevice, SERVER);

                // Closing ServerSocket---------
                try {
                    myServerSocket.close();
                    // Switch off the Discoverable Switch Button--
                    ((Activity) myContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Switch swDiscoverable = (Switch)((Activity) myContext).
                                                        findViewById(R.id.swDiscoverableID);
                            swDiscoverable.setChecked(false);
                        }
                    });
                    Log.d(TAG, "run: Closing ServerSocket.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            Log.i(TAG, "END ListenThread. ");
        }



        public void cancel(){
            Log.d(TAG, "cancel ListenThread: start..");
            try{
                myServerSocket.close();
                Log.d(TAG, "cancel ListenThread: ListenThread Closed.");

            }
            catch(IOException e){
                Log.e(TAG, "cancel: Closing of ListenThread ServerSocket failed. " + e.getMessage());
            }
        }
    }





    /* ********************************************************************
    2) ConnectThread
    - This thread runs while attempting to make an outgoing connection with a device.
    - It runs straight through;
    - The connection either succeeds or fails.
    ********************************************************************** */
    private class ConnectThread extends Thread{
        private BluetoothSocket clientSocket;

        public ConnectThread(BluetoothDevice device, UUID uuid){
            Log.d(TAG, "ConnectThread: started.");
            mDevice = device;
            deviceUUID = uuid;
        }



        // This method will be called automatically when an 'ConnectThread' object is created.
        public void run(){
            BluetoothSocket temp = null;
            Log.i(TAG,"RUN: ConnectThread");

            // Get a BluetoothSocket for a connection with given BluetoothDevice.
            try{
                Log.d(TAG, "ConnectThread: Trying to create InsecureRFcommSocket " +
                                "as REMOTED CLIENT using UUID." + MY_UUID_INSECURE);
                temp = mDevice.createInsecureRfcommSocketToServiceRecord(deviceUUID);
                Log.d(TAG, "ConnectThread: Temporary socket is created. ");
            }
            catch(IOException e){
                Log.e(TAG, "ConnectThread: Could not create InsecureRFcommSocket " +
                                        e.getMessage());
            }

            clientSocket = temp;

            /* Always cancel BT discovery because it will slow down a connection. **********
              1)Switch off the auto Discover process.
              2) Stop the discovery process
            ******************************************************************************* */
            stopBT_Discovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return--
                // on a successful connection or an exception.
                clientSocket.connect();

                showMessageViews();
                Log.d(TAG, "run: ConnectThread: Connected.Remote Device: " +
                                        clientSocket.getRemoteDevice().getName());
                // start connectedThread------
                manageConnectedSocket(clientSocket, mDevice, CLIENT);
            }
            catch (IOException e) {
                Log.d(TAG, "run: ConnectThread can not connect wtih the BTdevice. " +
                                        e.getMessage());
                // Closing the socket--------------------------------------------
                try {
                    clientSocket.close();
                    Log.d(TAG, "run: Closed Socket");
                }
                catch (IOException e1) {
                    Log.d(TAG, "myConnectThread: run Unable to close connection in Socket" +
                            e1.getMessage());
                }

                Log.d(TAG, "run: ConnectThread: Could not connect to UUID: " +
                            MY_UUID_INSECURE);


                // dismissing ProgressDialog------------------------------------
                try{
                    myProgressDialog.dismiss();
                    Log.d(TAG, "run: ConnectThread: dismissing ProgressDialog. ");
                }
                catch(NullPointerException ne){
                    ne.printStackTrace();
                }


                // showing Cannot connect toast -----------------------------------------
                ((Activity) myContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast tryAgainToast = Toast.makeText(myContext, "Cannot connect. " +
                                " Selected device is not found.", Toast.LENGTH_SHORT);
                        tryAgainToast.setGravity(Gravity.BOTTOM, 0, 0);
                        tryAgainToast.show();
                    }
                });
            }

        }



        public void cancel(){
            try {
                Log.d(TAG, "cancel: Cancelling Client Socket.");
                clientSocket.close();
            }
            catch (IOException e) {
                Log.d(TAG, "cancel: close() of clientSocket is ConnectThread failed." +
                                    e.getMessage());
            }
        }
    }






    /* ***************************************************************************
    3) ConnectedThread
    - This thread is responsible for maintaining the BTConnection, Sending data, Receiving
      incoming data through input/output stream respectively.
    ****************************************************************************** */
    private class ConnectedThread extends Thread{
        private final BluetoothSocket mySocket;
        private final InputStream myInStream;
        private final OutputStream myOutStream;
        private String incomingMessage;
        Intent anIntent;

        public ConnectedThread(BluetoothSocket  socket){
            Log.d(TAG, "ConnectedThread: Starting.");
            mySocket = socket;
            InputStream tempIn = null;
            OutputStream tempOut = null;


            //Dismiss the progressDialog when connection is etablised.
            try{
                myProgressDialog.dismiss();
            }
            catch(NullPointerException e){
                e.printStackTrace();
            }


            try {
                tempIn = mySocket.getInputStream();
                tempOut = mySocket.getOutputStream();
            }
            catch(IOException e){
            }

            myInStream = tempIn;
            myOutStream = tempOut;
        }


        public void run(){

            Log.d(TAG, "ConnectedThread: run: started.");

            byte[] buffer = new byte[1024]; // buffer store for the stream.
            int bytes; // bytes returned from read().

            while(true){
                Log.d(TAG, "run: inside while Loop. waiting to receive message.." );
                // Read from the InputStream
                try {
                    /* **********************************************************************
                    --  myInStream.read(buffer)--
                    1) Reads some number of bytes from the input-stream
                    2) and stores them into the buffer array buffer.
                    3) The number of bytes actually read is returned as an integer.
                    4) This method blocks until--
                            i)input data is available,
                            ii)end of file is detected,
                            iii) an exception is thrown.
                    ************************************************************************** */
                    bytes = myInStream.read(buffer);

                    incomingMessage = new String(buffer, 0, bytes);
                    Log.d(TAG, "InputStream: " + incomingMessage);


                    updateIncomingMessage(incomingMessage);


                }
                catch(IOException e){
                    Log.d(TAG, "read: Error reading InputStream " + e.getMessage());
                    hideMessageViews();
                    //clearingMessageTextViews();
                    break;
                }
            }
        }



        // call this from the main Activity to send data to the remote device.
        public void write(byte[] bytes){
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "write: Writing to outputStream " + text);
            try{
                myOutStream.write(bytes);
            }
            catch(IOException e){
                Log.d(TAG, "write: Error writing to outputStream " + e.getMessage());
            }
        }



        // Showing incoming message from other manageConnectedSocket device ---------------
        public void updateIncomingMessage(String incomingMessage){
            final String msg = incomingMessage;
            ((Activity) myContext).runOnUiThread(new Runnable(){
                @Override
                public void run(){
                    TextView tvMSG = (TextView)((Activity) myContext).findViewById(R.id.tvMessageID);
                    tvMSG.setText(msg);
                }
            });
        }



        // call this from the main Activity to receive data to the remote device.
        public String read(){
            return incomingMessage;
        }



        // Call this from the main Activity to shutdown the connection.
        public void cancel(){
            if(myInStream != null){
                try{
                    myInStream.close();
                    Log.d(TAG, "ConnectedThread: Canceling: closing InputStream" );
                } catch (IOException e) {
                    Log.d(TAG, "ConnectedThread: Canceling: InputStream IOException" );
                }
            }

            if(myOutStream != null){
                try{
                    myOutStream.close();
                    Log.d(TAG, "ConnectedThread: Canceling: closing OutputStream" );
                } catch (IOException e) {
                    Log.d(TAG, "ConnectedThread: Canceling: OutputStream IOException" );
                }
            }


            try{
                mySocket.close();
                Log.d(TAG, "ConnectedThread: Canceling: closing manageConnectedSocket socket." );
            }
            catch(IOException e){
            }
        }
    }
    // All threads are finished----------------------------------------------------------------





    /* /// custom methods of BluetoothChatService Class---------------------------------
    1) startServer()                        // Initiate ListenThread
    2) startClient()                        // Initiate ConnectThread
    3) manageConnectedSocket()              // Initiate ConnectedThread
    4) showConnectionResultToUser()
    5) write()
    6) closeAllThreads()
    */

    /* ******************************************************************************
    - Initiate the ListenThread. (Start the chat service.)
    - Specifically startServer AccepthThread to begin a session in listening (server) mode.
    - Called by the Activity onResume().
   ********************************************************************************* */
    public synchronized  void startServer(){
        Log.d(TAG, "startServer");

        // Cancel any thread attempting to make a connection.
        if (myConnectThread != null){
            myConnectThread.cancel();
            myConnectThread = null;
        }

        if (myConnectedThread != null){
            myConnectedThread.cancel();
            myConnectedThread = null;
        }

        if (myInsecureAcceptThread == null ){
            myInsecureAcceptThread = new ListenThread();
            myInsecureAcceptThread.start();     // built-in function to start a thread.
        }
    }




    /* *********************************************************************
   - Initiate the ConnectThread.
   - ListenThread starts and sits waiting for a connection.
   - Then ConnectThread starts and attempts to make a connection with the other devices ListenThread.
   *********************************************************************** */
    public void startClient(BluetoothDevice device, UUID uuid) {
        Log.d(TAG, "startClient: Started.");


        // Cancel this device to Listen as server-------
        if(myInsecureAcceptThread != null){
            Log.d(TAG, "startClient: Cancelling  ListenThread" );
            myInsecureAcceptThread.cancel();
            myInsecureAcceptThread = null;
        }

        // Cancel if this device is previously connected as a client to other device----
        if (myConnectThread != null){
            Log.d(TAG, "startClient: Cancelling  ConnectThread" );
            myConnectThread.cancel();
            myConnectThread = null;
        }

        // Cancel if this device is previously connected ----
        if (myConnectedThread != null){
            Log.d(TAG, "startClient: Cancelling  ConnectedThread" );
            myConnectedThread.cancel();
            myConnectedThread = null;
        }


        // showing progress dialog----------
        myProgressDialog = ProgressDialog.show(myContext, "Connecting Bluetooth",
                "Please wait ....", true);

        // Initiate the ConnectThread--------
        myConnectThread = new ConnectThread(device, uuid);
        myConnectThread.start();       // built-in function to start a thread.
    }




    /* *************************************************************************
    - Initiate the ConnectedThread.
    **************************************************************************** */
    private synchronized void manageConnectedSocket(BluetoothSocket mmSocket,
                                                    final BluetoothDevice mmDevice, int serverOrClient){

        if (mmSocket != null){
            if(mmDevice != null) {
                if (serverOrClient == CLIENT){
                    Log.d(TAG, "manageConnectedSocket: Starting. " +
                            "Connected Server Device name: **** " + mmDevice.getName() + " ****");
                }

                else if(serverOrClient == SERVER){
                    Log.d(TAG, "manageConnectedSocket: Starting. " +
                            "Connected Client Device name: " + mmDevice.getName());
                }

                showConnectionResultToUser(mmDevice);
            }
            else{
                Log.d(TAG, "manageConnectedSocket: No server device is found.");
            }
            //Start the thread to manage the connection and perform transmissions
            myConnectedThread = new ConnectedThread(mmSocket);
            myConnectedThread.start();   //start() is a Built-in function.
        }

        else{
            // showing a Toast----------------
            ((Activity) myContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast connectionToast = Toast.makeText(myContext, "Cannot connect with " +
                            mmDevice.getName(), Toast.LENGTH_SHORT);
                    connectionToast.setGravity(Gravity.BOTTOM, 0, 0);
                    connectionToast.show();
                }
            });
        }
    }




    // updating messageTitle and Showing toast message and  for manageConnectedSocket device ---------------
    private void showConnectionResultToUser(final BluetoothDevice mmDevice) {
        {
            ((Activity) myContext).runOnUiThread(new Runnable(){
                @Override
                public void run(){

                    Log.d(TAG, "showConnectionResultToUser: Starting... ");
                    // update messageTitle
                    TextView tvMSGTitle = (TextView)((Activity) myContext).findViewById(R.id.tvMessageTitleID);
                    tvMSGTitle.setText("Message From " + mmDevice.getName() + ": ");
                    // showing the Toast
                    Toast connectionToast = Toast.makeText(myContext, "Connected with " +
                                    mmDevice.getName(), Toast.LENGTH_SHORT);
                    connectionToast.setGravity(Gravity.BOTTOM, 0, 0);
                    connectionToast.show();
                }
            });
        }
    }


    /* *******************************************************************
    - Write to the ConnectedThread in an unsynchronized manner.
    - @param out the bytes to write.
    - @see ConnectedThread #write(byte[])
    ********************************************************************** */
    public void write(byte[] out){
        Log.d(TAG, "write: Write called");

        //perform the write
        myConnectedThread.write(out);
    }



    // Close previous connection ------
    public void closeAllThreads(){

        if (myConnectThread != null){
            Log.d(TAG, "closePrevConn: Cancelling  ConnectThread" );
            myConnectThread.cancel();
            myConnectThread = null;
        }

        if (myConnectedThread != null){
            Log.d(TAG, "closePrevConn: Cancelling  ConnectedThread" );
            myConnectedThread.cancel();
            myConnectedThread = null;
        }

        if(myInsecureAcceptThread  != null){
            Log.d(TAG, "closePrevConn: Cancelling  ListenThread" );
            myInsecureAcceptThread.cancel();
            myInsecureAcceptThread = null;
        }
    }


    // Closing ListenThread(Server)------------------------------
    public void closeServer(){
        if(myInsecureAcceptThread  != null){
            Log.d(TAG, "closeServer: Cancelling  ListenThread" );
            myInsecureAcceptThread.cancel();
            myInsecureAcceptThread = null;
        }
    }


    // Clearing Message TextViews---------------------------
    private void clearingMessageTextViews(){
        ((Activity) myContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {

                TextView tvMessageTitle = (TextView)((Activity) myContext).
                                                    findViewById(R.id.tvMessageTitleID);
                tvMessageTitle.setText("Message: ");

                TextView tvMessageID = (TextView)((Activity) myContext).
                                                findViewById(R.id.tvMessageID);
                tvMessageID.setText("");
            }
        });
    }




    /* ************************************************
     1)Switch off the auto Discover process.
     2) Stop the discovery process
    ************************************************* */
    private void stopBT_Discovery(){
        ((Activity) myContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Switch swAutoDiscovery = (Switch)((Activity) myContext).
                        findViewById(R.id.swAutoDiscoveryID);
                swAutoDiscovery.setChecked(false);
            }
        });

        myBluetoothAdapter.cancelDiscovery();
    }



    // showMessage related all Views ---------------------------
    private void showMessageViews(){
        ((Activity) myContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView tvMessageTitle = (TextView)((Activity) myContext).
                                                        findViewById(R.id.tvMessageTitleID);
                TextView tvMessageID = (TextView)((Activity) myContext).
                                                            findViewById(R.id.tvMessageID);
                EditText etMessageSendID = (EditText)((Activity) myContext).
                                                        findViewById(R.id.etMessageSendID);
                Button btnSendID = (Button)((Activity) myContext).findViewById(R.id.btnMessageSendID);

                tvMessageTitle.setVisibility(View.VISIBLE);
                tvMessageID.setVisibility(View.VISIBLE);
                etMessageSendID.setVisibility(View.VISIBLE);
                btnSendID.setVisibility(View.VISIBLE);
            }
        });
    }


    // Hide Message related all Views------------
    private void hideMessageViews(){
        ((Activity) myContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView tvMessageTitle = (TextView)((Activity) myContext).
                                                findViewById(R.id.tvMessageTitleID);
                TextView tvMessageID = (TextView)((Activity) myContext).
                                                findViewById(R.id.tvMessageID);
                EditText etMessageSendID = (EditText)((Activity) myContext).
                                                findViewById(R.id.etMessageSendID);
                Button btnSendID = (Button)((Activity) myContext).findViewById(R.id.btnMessageSendID);

                tvMessageTitle.setText("Message: ");
                tvMessageID.setText("");

                tvMessageTitle.setVisibility(View.INVISIBLE);
                tvMessageID.setVisibility(View.INVISIBLE);
                etMessageSendID.setVisibility(View.INVISIBLE);
                btnSendID.setVisibility(View.INVISIBLE);
            }
        });
    }

}
