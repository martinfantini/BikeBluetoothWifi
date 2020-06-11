package com.example.bikebluetoothwifi;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bikebluetoothwifi.io.BluetoothConnection;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

public class BluetoothActivity  extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private Switch switchBlue;

    private Button listBtn;
    private Button searchBtn;
    private BluetoothAdapter myBluetoothAdapter;
    private Set<BluetoothDevice> pairedDevices;
    private ListView listFoundDevices;
    private ArrayAdapter<String> btArrayAdapter;

    //mapa donde tengo guarda la informacion de los datos del bluethoot
    private HashMap<String,String> mapBulethoot = new HashMap<String,String>();

    private boolean registerReciever = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth_activity);

        switchBlue = (Switch) findViewById(R.id.switch_bluetooth);
        listBtn = (Button) findViewById( R.id.list_bluetooth);
        searchBtn = (Button) findViewById( R.id.search_bluetooth);

        // take an instance of BluetoothAdapter - Bluetooth radio
        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(myBluetoothAdapter == null) {
            listBtn.setEnabled(false);
            searchBtn.setEnabled(false);
            switchBlue.setEnabled(false);
            Toast.makeText(getApplicationContext(),"Your device does not support Bluetooth",
                    Toast.LENGTH_LONG).show();
        } else {

             //Button to enable
            switchBlue.setChecked( myBluetoothAdapter.isEnabled() );
            switchBlue.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView,
                                             boolean isChecked) {
                    if(isChecked)
                    {
                        Intent turnOnIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(turnOnIntent, REQUEST_ENABLE_BT);

                        Toast.makeText(getApplicationContext(),"Bluetooth turned on" ,
                                    Toast.LENGTH_LONG).show();
                    }
                    else
                    {
                        myBluetoothAdapter.disable();
                        Toast.makeText(getApplicationContext(),"Bluetooth turned off",
                                Toast.LENGTH_LONG).show();
                    }
                }
            });

            //Add elemnts to the list.
            listFoundDevices = (ListView) findViewById(R.id.devices_found);
            btArrayAdapter = new ArrayAdapter<String>(BluetoothActivity.this, android.R.layout.simple_list_item_1);
            listFoundDevices.setAdapter(btArrayAdapter);

            listBtn = (Button)findViewById(R.id.list_bluetooth);
            listBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // get paired devices
                    pairedDevices = myBluetoothAdapter.getBondedDevices();

                    // put it's one to the adapter
                    for(BluetoothDevice device : pairedDevices) {
                        btArrayAdapter.add(device.getName());
                        mapBulethoot.put(device.getName(),device.getAddress());
                    }
                    Toast.makeText(getApplicationContext(),"Show Paired Devices",
                            Toast.LENGTH_SHORT).show();
                }
            });

            searchBtn  = (Button)findViewById(R.id.search_bluetooth);
            searchBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (myBluetoothAdapter.isDiscovering()) {
                        // the button is pressed when it discovers, so cancel the discovery
                        myBluetoothAdapter.cancelDiscovery();
                    }
                    else {
                        btArrayAdapter.clear();
                        myBluetoothAdapter.startDiscovery();
                        registerReciever = true;
                        registerReceiver(bReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
                    }
                }
            });

            listFoundDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                public void onItemClick(AdapterView<?> arg0, View view, int arg2,long arg3)
                {
                    myBluetoothAdapter.cancelDiscovery();
                    String connectioName = listFoundDevices.getItemAtPosition(arg2).toString();
                    Toast.makeText(getApplicationContext(), "Bluetooth connection name " + connectioName,Toast.LENGTH_SHORT).show();
                    String address = mapBulethoot.get( connectioName );
                    BluetoothDevice device = myBluetoothAdapter.getRemoteDevice( address );

                    UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
                    BluetoothSocket mmSocket = null;
                    try {
                        mmSocket = device.createRfcommSocketToServiceRecord(uuid);
                        myBluetoothAdapter.cancelDiscovery();
                        mmSocket.connect();
                        BluetoothConnection.GetInstance().SetInputStream(mmSocket.getInputStream());
                        BluetoothConnection.GetInstance().SetOutputStream(mmSocket.getOutputStream());
                        Toast.makeText(getApplicationContext(), "Bluetooth connected",Toast.LENGTH_SHORT).show();
                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                }
            });

        }

        findViewById(R.id.bluetooth_main_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent k = new Intent(BluetoothActivity.this, MainActivity.class);
                startActivity(k);
                finish();
            }
        });
    }

    BroadcastReceiver bReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // add the name and the MAC address of the object to the arrayAdapter
                btArrayAdapter.add(device.getName());
                btArrayAdapter.notifyDataSetChanged();
                mapBulethoot.put(device.getName(),device.getAddress());
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(registerReciever)
            unregisterReceiver(bReceiver);
    }
}
