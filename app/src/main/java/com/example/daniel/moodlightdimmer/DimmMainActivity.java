package com.example.daniel.moodlightdimmer;


import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
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
    private Button buttonSet, buttonReconnect;
    private Integer color;
    private MqttAndroidClient client;
    private String device = "sender";
    private String publisher_topic;
    private String broker = "tcp://raspi2:1883";
    private Spinner spinnerlights;
    private CheckBox checkBoxInstantSet;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dimm_main);

        red = (SeekBar) findViewById(R.id.seekBarRed);
        green = (SeekBar) findViewById(R.id.seekBarGreen);
        blue = (SeekBar) findViewById(R.id.seekBarBlue);
        buttonSet = (Button) findViewById(R.id.buttonSet);
        buttonReconnect = (Button) findViewById(R.id.buttonReconnect);
        checkBoxInstantSet = (CheckBox) findViewById(R.id.checkBoxInstantSet);

        color = 0xFF000000;
        spinnerlights = (Spinner) findViewById(R.id.spinnerLights);
        spinnerlights.setAdapter(new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item, new String[] {"All", "Kitchen", "Automat", "South East","South West","North East","North West"} ));

        String macaddress = getWifiMacAddress();

        publisher_topic = "kitchen/switch/android/" + macaddress;

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

        buttonReconnect.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!client.isConnected()) ConnectMQTT();
            }
        });




    }

    private void SetButtonColor(Boolean sendCommand)
    {
        color = (0xFF<<24) | ((int)(red.getProgress()*2.55)<<16) | ((int)(green.getProgress()*2.55)<<8) | ((int)(blue.getProgress()*2.55));
        buttonSet.setBackgroundColor(color);
        if(client.isConnected() && device == "sender")
        {
            //String topic = publisher_topic;sf
            //String payload = Integer.toHexString(color);

            if(sendCommand) SendCommand(spinnerlights.getSelectedItem().toString() + ";                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 " +Integer.toHexString(color).substring(2), publisher_topic);
        }
        else if(!client.isConnected() && device == "sender")
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
                    // if connection was succesful, subscripe to topic if app is a receiver
                    Log.d("MQTT Connection", "onSuccess");
                    if (device == "receiver") {
                        String topic = publisher_topic;

                        int qos = 1;
                        try {
                            IMqttToken subToken = client.subscribe(topic, qos);
                            subToken.setActionCallback(new IMqttActionListener() {
                                @Override
                                public void onSuccess(IMqttToken asyncActionToken) {
                                    Log.d("MQTT Subscription", "Successful");
                                }

                                @Override
                                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

                                }
                            });

                        } catch (MqttSecurityException e) {
                            e.printStackTrace();
                        } catch (MqttException e) {
                            e.printStackTrace();

                        }
                    }
                }
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Log.d("FragmentActivity", "onFailure");
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
                Log.d("Connection", "Lost");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                try {
                    if(device == "receiver") {
                        String msg = new String(message.getPayload());
                        msg = msg.substring(2);
                        color = 0xFF000000 | Integer.parseInt(msg, 16);
                        red.setProgress(((color & 0xFF0000) >> 16) * 100 / 255);
                        green.setProgress(((color & 0xFF00) >> 8) * 100 / 255);
                        blue.setProgress(((color & 0xFF)) * 100 / 255);
                        SetButtonColor(false);
                    }
                }
                catch (Exception e)
                {
                    Log.d("MesageArrived", "Exeption");
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });


    }

    private void SendCommand(String payload, String topic)
    {
        //TODO Send which lights should be set
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

    public static String getWifiMacAddress() {
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
