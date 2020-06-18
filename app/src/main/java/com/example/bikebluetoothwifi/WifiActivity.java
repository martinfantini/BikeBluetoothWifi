package com.example.bikebluetoothwifi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bikebluetoothwifi.io.WifiConnection;
import com.example.bikebluetoothwifi.thread.BluetoothRunner;
import com.example.bikebluetoothwifi.thread.WifiRunner;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WifiActivity  extends AppCompatActivity {

    private String strIp = "192.168.1.39";
    private String strPort = "10000";

    private String regExIpPort = "(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):(\\d{1,5})";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifi_activity);

        boolean isIpPort = false;

        final EditText mEdit = (EditText) findViewById(R.id.editText_ip_port);
        mEdit.setText(strIp+":"+strPort);

        findViewById(R.id.button_wifi_connect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mEdit.getText().toString().isEmpty()) {
                    Pattern p = Pattern.compile(regExIpPort);
                    Matcher m = p.matcher(mEdit.getText().toString());
                    if ( m.find() ) {
                        //Toast.makeText(v.getContext().getApplicationContext(),"Ip: " +  m.group(1) + " and Port: "+ m.group(2),Toast.LENGTH_LONG).show();
                        WifiConnection.GetInstance().setIp(m.group(1));
                        WifiConnection.GetInstance().setPort(m.group(2));

                        WifiConnection.GetInstance().setWifiRunnerThread( new Thread( new WifiRunner()));
                            WifiConnection.GetInstance().getWifiRunnerThread().start();
                    }
                    else {
                        Toast.makeText(v.getContext().getApplicationContext(),"Ip:Port test is empty",Toast.LENGTH_LONG).show();
                    }
                }}});

        findViewById(R.id.wifi_main_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent k = new Intent(WifiActivity.this, MainActivity.class);
                startActivity(k);
                finish();
            }
        });
    }
}
