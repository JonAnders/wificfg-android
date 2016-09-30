package no.jamundsen.wificfg;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.Set;

public class DeviceListActivity extends AppCompatActivity {

    BluetoothAdapter btAdapter;
    Button scanButton;
    ListView otherDeviceList;
    ArrayAdapter<String> otherDeviceArrayAdapter;

    int REQUEST_COARSE_LOCATION = 1337; // This is weird...

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        ListView pairedDeviceList = (ListView)findViewById(R.id.pairedDeviceList);
        otherDeviceList = (ListView)findViewById(R.id.otherDeviceList);

        // Hack to get the divider line at the bottom of the list
        pairedDeviceList.addFooterView(new View(this));
        otherDeviceList.addFooterView(new View(this));

        ArrayAdapter<String> pairedDeviceArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        pairedDeviceList.setAdapter(pairedDeviceArrayAdapter);

        otherDeviceArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        otherDeviceList.setAdapter(otherDeviceArrayAdapter);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null){
            Toast.makeText(this, "Bluetooth not available", Toast.LENGTH_LONG).show();
            return;
        }

        Set<BluetoothDevice> bondedDevices = btAdapter.getBondedDevices();
        if (bondedDevices.size() > 0) {
            for (BluetoothDevice device : bondedDevices) {
                pairedDeviceArrayAdapter.add(device.getName());
            }
        }
        else {
            pairedDeviceArrayAdapter.add("No paired devices...");
        }

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(broadcastReceiver, filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(broadcastReceiver, filter);

        scanButton = (Button)findViewById(R.id.scanButton);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scan();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (btAdapter != null)
            btAdapter.cancelDiscovery();

        this.unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (requestCode == REQUEST_COARSE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                btAdapter.startDiscovery();
            }
            else {
                scanButton.setVisibility(View.VISIBLE);
            }
        }
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                // Don't list paired devices again
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    otherDeviceArrayAdapter.add(device.getName());
                }
            }
            else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                scanButton.setVisibility(View.VISIBLE);
                if (otherDeviceArrayAdapter.getCount() == 0)
                    otherDeviceArrayAdapter.add("No devices found!");
            }
        }
    };

    private void scan() {
        scanButton.setVisibility(View.GONE);

        otherDeviceArrayAdapter.clear();

        if (btAdapter.isDiscovering())
            btAdapter.cancelDiscovery();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_COARSE_LOCATION);
        }
        else {
            btAdapter.startDiscovery();
        }
    }
}
