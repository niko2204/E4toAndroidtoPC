package com.empatica.sample;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.empatica.empalink.ConnectionNotAllowedException;
import com.empatica.empalink.EmpaDeviceManager;
import com.empatica.empalink.config.EmpaSensorStatus;
import com.empatica.empalink.config.EmpaSensorType;
import com.empatica.empalink.config.EmpaStatus;
import com.empatica.empalink.delegate.EmpaDataDelegate;
import com.empatica.empalink.delegate.EmpaStatusDelegate;

//datagram
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.IOException;
import java.net.UnknownHostException;
import android.os.AsyncTask;
import android.view.View.OnClickListener;


public class MainActivity extends AppCompatActivity implements EmpaDataDelegate, EmpaStatusDelegate {

    //datgram
    private TextView txtResponse;
    private EditText edtTextAddress, edtTextPort;
    private Button btnConnect, btnClear;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final long STREAMING_TIME = 10000000; // Stops streaming 10 seconds after connection

    private static final String EMPATICA_API_KEY = "94cedaf55d034e86a117a79759a2058e"; // TODO insert your API Key here

    private EmpaDeviceManager deviceManager;

    private TextView accel_xLabel;
    private TextView accel_yLabel;
    private TextView accel_zLabel;
    private TextView bvpLabel;
    private TextView edaLabel;
    private TextView ibiLabel;
    private TextView temperatureLabel;
    private TextView batteryLabel;
    private TextView statusLabel;
    private TextView deviceNameLabel;
    private RelativeLayout dataCnt;

    float ibi_temp; //temporary variable.
    float gsr_temp; //전송을 위한 gsr

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //datagram
        edtTextAddress = (EditText) findViewById(R.id.address);
        edtTextPort = (EditText) findViewById(R.id.port);
        btnConnect = (Button) findViewById(R.id.connect);
        btnClear = (Button) findViewById(R.id.clear);
        txtResponse = (TextView) findViewById(R.id.response);

        // Initialize vars that reference UI components
        statusLabel = (TextView) findViewById(R.id.status);
        dataCnt = (RelativeLayout) findViewById(R.id.dataArea);
        accel_xLabel = (TextView) findViewById(R.id.accel_x);
        accel_yLabel = (TextView) findViewById(R.id.accel_y);
        accel_zLabel = (TextView) findViewById(R.id.accel_z);
        bvpLabel = (TextView) findViewById(R.id.bvp);
        edaLabel = (TextView) findViewById(R.id.eda);
        ibiLabel = (TextView) findViewById(R.id.ibi);
        temperatureLabel = (TextView) findViewById(R.id.temperature);
        batteryLabel = (TextView) findViewById(R.id.battery);
        deviceNameLabel = (TextView) findViewById(R.id.deviceName);

        // Create a new EmpaDeviceManager. MainActivity is both its data and status delegate.
        deviceManager = new EmpaDeviceManager(getApplicationContext(), this, this);
        // Initialize the Device Manager using your API key. You need to have Internet access at this point.
        deviceManager.authenticateWithAPIKey(EMPATICA_API_KEY);

        //datagram
        btnConnect.setOnClickListener(buttonConnectOnClickListener);
        btnClear.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                txtResponse.setText("");
            }
        });
    }

    OnClickListener buttonConnectOnClickListener = new OnClickListener() {

        public void onClick(View arg0) {
            NetworkTask myClientTask = new NetworkTask(
                    edtTextAddress.getText().toString(),
                    Integer.parseInt(edtTextPort.getText().toString())
            );
            myClientTask.execute();
        }
    };

    public class NetworkTask extends AsyncTask<Void, Void, Void> {

        String dstAddress;
        int dstPort;
        String response;
        byte[] buffer = new byte[1024];
        byte[] buffer_gsr = new byte[1024];
        float temp;
        int msg1, msg_gsr;
        //	Socket socket = new Socket(dstAddress, dstPort);
        DatagramSocket datagramSocket;// = new DatagramSocket();
        //	InputStream inputStream = socket.getInputStream();
        InetAddress serverAddress;// = InetAddress.getByName(dstAddress);
        //	ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1024);

        DatagramSocket datagramSocket_gsr;




        NetworkTask(String addr, int port) {
            dstAddress = addr;
            dstPort = port;

        }


        @Override
        protected Void doInBackground(Void... arg0) {

            try {
                //	Socket socket = new Socket(dstAddress, dstPort);
                datagramSocket = new DatagramSocket();
                datagramSocket_gsr = new DatagramSocket();
                //	InputStream inputStream = socket.getInputStream();
                serverAddress = InetAddress.getByName(dstAddress);
                //	ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1024);


                while(true) {
                    // String msg = "54321";
                    if(ibi_temp == 0) {
                        temp = 0;
                    }else{
                        temp = 60/ibi_temp ;  // heart beat in 1 min = 60 second / IBI.
                    }
                    msg1 = (int) temp;
                    // msg = bvpLabel;
                    //buffer = intToByteArray(msg1;
                    // buffer = floatToByteArray(msg1);

                    //   msg1 = msg1%100;

                    msg_gsr = (int)Math.floor(gsr_temp*100);

                    Log.d("tag", "GSR:" + msg_gsr);

                    buffer = Integer.toString(msg1).getBytes();
          //          buffer_gsr = Float.toString(gsr_temp).getBytes();
                    buffer_gsr = Integer.toString(msg_gsr).getBytes();


                    //	int bytesRead;
                    DatagramPacket outPacket = new DatagramPacket(buffer, buffer.length, serverAddress, dstPort);
                    DatagramPacket outPacket_gsr = new DatagramPacket(buffer_gsr,buffer_gsr.length,serverAddress,dstPort+1);
                    //   DatagramPacket inPacket = new DatagramPacket(buffer, buffer.length);

                    datagramSocket.send(outPacket); //전송
                    datagramSocket_gsr.send(outPacket_gsr);
                    //    datagramSocket.receive(inPacket); //수신

                    //   System.out.println("received data:" + new String(inPacket.getData()));
                    SystemClock.sleep(1000);
                }
                //   datagramSocket.close();

                //	while ((bytesRead = inputStream.read(buffer)) != -1) {
                //		byteArrayOutputStream.write(buffer, 0, bytesRead);
                //	}

                //	socket.close();
                //	response = byteArrayOutputStream.toString("UTF-8");

            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            txtResponse.setText(response);
            datagramSocket.close();
            super.onPostExecute(result);
        }

        private byte[] floatToByteArray(float value) {
            int floatValue = Float.floatToIntBits(value);
            return intToByteArray(floatValue);
        }

        private float byteArrayToFloat(byte bytes[]) {
            int value = byteArrayToInt(bytes);
            return Float.intBitsToFloat(value);
        }

        private byte[] intToByteArray(int value) {
            byte[] byteArray = new byte[4];
            byteArray[0] = (byte) (value >>24);
            byteArray[1] = (byte) (value >>16);
            byteArray[2] = (byte) (value >>8);
            byteArray[3] = (byte) (value);
            return byteArray;

        }

        //big endian 방식
        private int byteArrayToInt(byte bytes[]) {
            return (((int)bytes[0] & 0xff) <<24 |
                    (((int)bytes[1] & 0xff) <<16) |
                    (((int)bytes[2] & 0xff) <<8) |
                    (((int)bytes[3] & 0xff)));
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        deviceManager.stopScanning();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        deviceManager.cleanUp();
    }

    @Override
    public void didDiscoverDevice(BluetoothDevice bluetoothDevice, String deviceName, int rssi, boolean allowed) {
        // Check if the discovered device can be used with your API key. If allowed is always false,
        // the device is not linked with your API key. Please check your developer area at
        // https://www.empatica.com/connect/developer.php
        if (allowed) {
            // Stop scanning. The first allowed device will do.
            deviceManager.stopScanning();
            try {
                // Connect to the device
                deviceManager.connectDevice(bluetoothDevice);
                updateLabel(deviceNameLabel, "To: " + deviceName);
            } catch (ConnectionNotAllowedException e) {
                // This should happen only if you try to connect when allowed == false.
                Toast.makeText(MainActivity.this, "Sorry, you can't connect to this device", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void didRequestEnableBluetooth() {
        // Request the user to enable Bluetooth
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // The user chose not to enable Bluetooth
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            // You should deal with this
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void didUpdateSensorStatus(EmpaSensorStatus status, EmpaSensorType type) {
        // No need to implement this right now
    }

    @Override
    public void didUpdateStatus(EmpaStatus status) {
        // Update the UI
        updateLabel(statusLabel, status.name());

        // The device manager is ready for use
        if (status == EmpaStatus.READY) {
            updateLabel(statusLabel, status.name() + " - Turn on your device");
            // Start scanning
            deviceManager.startScanning();
            // The device manager has established a connection
        } else if (status == EmpaStatus.CONNECTED) {
            // Stop streaming after STREAMING_TIME
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dataCnt.setVisibility(View.VISIBLE);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // Disconnect device
                            deviceManager.disconnect();
                        }
                    }, STREAMING_TIME);
                }
            });
            // The device manager disconnected from a device
        } else if (status == EmpaStatus.DISCONNECTED) {
            updateLabel(deviceNameLabel, "");
        }
    }

    @Override
    public void didReceiveAcceleration(int x, int y, int z, double timestamp) {
        updateLabel(accel_xLabel, "" + x);
        updateLabel(accel_yLabel, "" + y);
        updateLabel(accel_zLabel, "" + z);
    }

    @Override
    public void didReceiveBVP(float bvp, double timestamp) {
        updateLabel(bvpLabel, "" + bvp);
    }

    @Override
    public void didReceiveBatteryLevel(float battery, double timestamp) {
        updateLabel(batteryLabel, String.format("%.0f %%", battery * 100));
    }

    @Override
    public void didReceiveGSR(float gsr, double timestamp) {

        updateLabel(edaLabel, "" + gsr);
        gsr_temp = gsr;
    }

    @Override
    public void didReceiveIBI(float ibi, double timestamp) {
        updateLabel(ibiLabel, "" + ibi);
        ibi_temp = ibi;
    }

    @Override
    public void didReceiveTemperature(float temp, double timestamp) {
        updateLabel(temperatureLabel, "" + temp);
    }

    // Update a label with some text, making sure this is run in the UI thread
    private void updateLabel(final TextView label, final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                label.setText(text);
            }
        });
    }
}
