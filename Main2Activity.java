package com.ashley.apptest13652518;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.Series;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Build.VERSION;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;
import com.ashley.apptest13652518.R;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;



public class Main2Activity extends AppCompatActivity {

    TextView title;
    TextView tvMeasurement;

    private static String pulseValue;
    private static double _pulseGraph;
    private static String address;

    private static final UUID MY_UUID = UUID.fromString("baa9c05d-ec2e-4f41-b577-92422fae6c05");
    private BluetoothAdapter mBTAdapter;
    private BluetoothSocket mBTSocket;

    private Handler handlerGetData;
    private ConnectedThread mConnectedThread;

    Handler mHandler = new Handler();
    Runnable mTimer;
    LineGraphSeries<DataPoint> mSeries1;
    double graph2LastXValue = 104;


    public void PulseRateDetail() {
        // TODO Auto-generated constructor stub
        this.mBTAdapter = null;
        this.mBTSocket = null;
    }

    @SuppressWarnings("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        tvMeasurement = (TextView) findViewById(R.id.tv_valuePulseRate);

        GraphView graph = (GraphView) findViewById(R.id.graph);

        mSeries1 = new LineGraphSeries<>();

        //Graphics of the plot
        mSeries1.setColor(Color.RED);
        mSeries1.setThickness(10);
        graph.addSeries(mSeries1);
        graph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);

        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(60);

        mSeries1.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                // TODO Auto-generated method stub
                Toast.makeText(getApplicationContext(), dataPoint.getY() + "",
                        Toast.LENGTH_SHORT).show();
            }
        });

        mTimer = new Runnable() {
            @Override
            public void run() {
                graph2LastXValue += 1d;
                mSeries1.appendData(new DataPoint(graph2LastXValue, _pulseGraph), true,
                        60);
                tvMeasurement.setText(pulseValue);
                mHandler.postDelayed(this, 300);
            }
        };
        mHandler.postDelayed(mTimer, 300);

        this.mBTAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTState();

        handlerGetData = new Handler(){

            @Override
            public void handleMessage(Message msg) {
                // TODO Auto-generated method stub
                super.handleMessage(msg);

                if(msg.what == 1){
                    byte[] buffer= (byte[]) msg.obj;
                    int begin = (int) msg.arg1;
                    int end = (int) msg.arg2;

                    if(begin == 0 && end == 19) {
                        String readmsg = new String(buffer);
                        readmsg = readmsg.substring(begin, end);

                        pulseValue = readmsg.substring(3,5);
                        _pulseGraph = Double.parseDouble(pulseValue);

                        readmsg = "";
                    }
                }
            }
        };


    }

    private void disconnectedSocket() {
        // TODO Auto-generated method stub
        if (address != null) {
            try{
                this.mBTSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onResume(){
        //TODO Auto-generated method stub
        super.onResume();
        try{
            Thread.sleep(500);
        } catch (InterruptedException e) {
            //TODO Auto-generated catch block
            e.printStackTrace();
        }

        connectSocket();
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        disconnectedSocket();
    }

    private void checkBTState() {
        if(this.mBTAdapter == null) {
            Toast.makeText(getBaseContext(), "Error: Bluetooth not supported!", Toast.LENGTH_SHORT).show();
            finish();

        } else if (!this.mBTAdapter.isEnabled()) {
            startActivityForResult(new Intent("android.bluetooth.adapter.action.REQUEST_ENABLE"), 1);
        }
    }

    private void connectSocket() {
        Intent getAdress = getIntent();
        address = getAdress.getStringExtra("_keyaddress");

        if (address != null) {
            Toast.makeText(getApplicationContext(), "Connected address:" + address, Toast.LENGTH_SHORT).show();
            try {
                this.mBTSocket = createBluetoothSocket(this.mBTAdapter.getRemoteDevice(address));
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.mBTAdapter.cancelDiscovery();
            try {
                this.mBTSocket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            this.mConnectedThread = new ConnectedThread(this.mBTSocket);
            this.mConnectedThread.start();
        } else {
            Toast.makeText(getApplicationContext(), "Device not available!", Toast.LENGTH_LONG).show();
            return;
        }
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        if (VERSION.SDK_INT >= 10){
            try{
                return (BluetoothSocket) device.getClass()
                        .getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class })
                        .invoke(device, new Object[] { MY_UUID });
            } catch (Exception e) {
            }
        }
        return device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket){
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.mmInStream = tmpIn;
            this.mmOutStream = tmpOut;
        }

        public void run(){
            byte[] buffer = new byte[1024];
            int bytes = 0;
            int begin =0;

            while(true) {
                try{
                    bytes += mmInStream.read(buffer, bytes, buffer.length - bytes);

                    for(int i = begin; i < bytes; i++){
                        if(buffer[i] == "#".getBytes()[0]){
                                handlerGetData.obtainMessage(1, begin, i, buffer).sendToTarget();
                                begin = i + 1;
                                if (i == bytes -1){
                                    bytes = 0;
                                    begin = 0;
                                }
                        }
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }
    }

}
