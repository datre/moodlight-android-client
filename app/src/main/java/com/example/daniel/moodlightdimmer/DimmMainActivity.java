package com.example.daniel.moodlightdimmer;


import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;


public class DimmMainActivity extends AppCompatActivity {

    private SeekBar red, green, blue;
    private Button buttonSet, buttonReconnect, buttonRandom;
    private Integer color;
    private MqttAndroidClient client;
    private String publisher_topic;
    private String broker = "tcp://homer:1883";
    private Spinner spinnerlights;
    private CheckBox checkBoxInstantSet;
    private String[] adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dimm_main);

        red = (SeekBar) findViewById(R.id.seekBarRed);
        green = (SeekBar) findViewById(R.id.seekBarGreen);
        blue = (SeekBar) findViewById(R.id.seekBarBlue);
        buttonSet = (Button) findViewById(R.id.buttonSet);
        buttonReconnect = (Button) findViewById(R.id.buttonReconnect);
        buttonRandom = (Button) findViewById(R.id.buttonRandom);
        checkBoxInstantSet = (CheckBox) findViewById(R.id.checkBoxInstantSet);

        color = 0xFF000000;

        adapter = new String[] {"1", "2", "3", "4","5","6","7", "8", "9", "10", "all"};
        spinnerlights = (Spinner) findViewById(R.id.spinnerLights);
        spinnerlights.setAdapter(new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item, adapter));

        publisher_topic = "kitchen/switches/android/" + getWifiMacAddress();

        red.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) SetButtonColor(checkBoxInstantSet.isChecked());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        green.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser) SetButtonColor(checkBoxInstantSet.isChecked());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        blue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) SetButtonColor(checkBoxInstantSet.isChecked());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        ConnectMQTT(); // Init Connection


        buttonSet.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (client.isConnected()) {
                    SetButtonColor(true);
                    //SendCommand(publisher_topic, spinnerlights.getSelectedItem().toString() + Integer.toHexString(color));
                }
            }
        });

        buttonRandom.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                SetButtonColor(true, true);
            }
        });

        buttonReconnect.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!client.isConnected()) ConnectMQTT();
            }
        });




    }
    private void SetButtonColor(Boolean sendCommand)
    {
        SetButtonColor(sendCommand, false);
    }

    private void SetButtonColor(Boolean sendCommand, Boolean rand)
    {
        color = (0xFF<<24) | ((int)(red.getProgress()*2.55)<<16) | ((int)(green.getProgress()*2.55)<<8) | ((int)(blue.getProgress()*2.55));
        buttonSet.setBackgroundColor(color);
        if(client.isConnected() && !rand)
        {
            if(sendCommand) SendCommand("colorchange:Lamp" + spinnerlights.getSelectedItem().toString() + "#" + Integer.toHexString(color).substring(2), publisher_topic);
        }
        else if(client.isConnected())
        {
            if(sendCommand) SendCommand("colorchange:Lamp" + spinnerlights.getSelectedItem().toString() + "#rand", publisher_topic);
        }
        else
        {
            Toast.makeText(this.getApplicationContext(), "Not Connected", Toast.LENGTH_SHORT);
        }
        return;
    }

    private void ConnectMQTT()
    {
        String clientId = MqttClient.generateClientId();
        final MqttAndroidClient client =
                new MqttAndroidClient(this.getApplicationContext(), broker,
                        clientId);
        try {
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    Log.d("MQTT Connection", "Sucessfully Connected");
                }
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Log.d("MQTT Connection", "Exeption: " + exception.getMessage());
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }

        this.client = client;

        //Callback for message arrived and connection lost
        this.client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.d("Connection", "Lost Cause: " + cause.toString());
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });


    }

    private void SendCommand(String payload, String topic)
    {
        try {
            byte[] encodedPayload = payload.getBytes();
            MqttMessage msg = new MqttMessage(encodedPayload);
            client.publish(topic, msg);
        }
        catch (MqttException e)
        {
            e.printStackTrace();
        }
    }

    private String getWifiMacAddress() {
        try {
            String interfaceName = "wlan0";
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (!intf.getName().equalsIgnoreCase(interfaceName)){
                    continue;
                }
                byte[] mac = intf.getHardwareAddress();
                if (mac==null){
                    return "";
                }

                StringBuilder buf = new StringBuilder();
                for (byte aMac : mac) {
                    buf.append(String.format("%02X:", aMac));
                }
                if (buf.length()>0) {
                    buf.deleteCharAt(buf.length() - 1);
                }
                return buf.toString();
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
    }


}
