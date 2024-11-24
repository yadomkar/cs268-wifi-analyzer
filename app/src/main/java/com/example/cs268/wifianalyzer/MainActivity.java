package com.example.cs268.wifianalyzer;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.location.LocationManager;
import android.os.Environment;
import android.os.Handler;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView wifiInfoTextView;
    private ListView wifiListView;
    private Button saveButton;
    private List<ScanResult> scanResults;
    private ArrayList<String> wifiList = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private WifiManager wifiManager;
    private WifiInfo wifiInfo;
    private String filePath;
    private int count = 0;
    private int stop = 0;
    private int sum = 0;
    private double average = 0;

    private static final String DEFAULT_FILE_NAME = "record001";
    private static final int PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wifiInfoTextView = findViewById(R.id.info);
        saveButton = findViewById(R.id.saveButton);
        wifiListView = findViewById(R.id.wifiList);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(this, "WiFi is disabled. Enabling WiFi...", Toast.LENGTH_SHORT).show();
            wifiManager.setWifiEnabled(true);
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, wifiList);
        wifiListView.setAdapter(adapter);
        filePath = getExternalFilesDir("Wifi Strength Results").toString();

        requestLocationPermission();

        if (!isLocationEnabled()) {
            Toast.makeText(this, "Please enable location services.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        }
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Permission", "ACCESS_FINE_LOCATION granted.");
            } else {
                Toast.makeText(this, "Location permission is required for WiFi scanning.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(wifiReceiver);
    }

    public void getWifiInfo(View view) {
        wifiInfoTextView.setText("");
        stop = 0;
        count = 0;
        sum = 0;

        if (!isExternalStorageAvailable() || isExternalStorageReadOnly()) {
            saveButton.setEnabled(false);
            return;
        }

        wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo.getNetworkId() != -1) {
            displayWifiInfo();
            startRssiLevelTracking();
        } else {
            Toast.makeText(this, "You are not connected to a WiFi network.", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayWifiInfo() {
        int ip = wifiInfo.getIpAddress();
        String ipAddress = Formatter.formatIpAddress(ip);
        String macAddress = wifiInfo.getMacAddress();
        int linkSpeed = wifiInfo.getLinkSpeed();
        int networkId = wifiInfo.getNetworkId();
        int frequency = wifiInfo.getFrequency();
        String ssid = wifiInfo.getSSID();
        String bssid = wifiInfo.getBSSID();
        int rssiLevel = WifiManager.calculateSignalLevel(wifiInfo.getRssi(), 5);

        String info = String.format("IP Address: %s\nMAC Address: %s\nNetwork ID: %d\nSSID: %s\nBSSID: %s\nLink Speed: %d Mbps\nFrequency: %d MHz\nSignal Level: %d/5\n\n",
                ipAddress, macAddress, networkId, ssid, bssid, linkSpeed, frequency, rssiLevel);

        wifiInfoTextView.append(info);
    }

    private void startRssiLevelTracking() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (count < 60 && stop == 0) {
                    trackRssiLevel();
                    handler.postDelayed(this, 1000);
                }
            }
        }, 1000);
    }

    private void trackRssiLevel() {
        wifiInfo = wifiManager.getConnectionInfo();
        int rssiLevel = wifiInfo.getRssi();
        sum += rssiLevel;
        wifiInfoTextView.append(String.format("%ds --> RSSI Level: %d\n", count + 1, rssiLevel));
        count++;

        if (count == 60) {
            average = sum / 60.0;
            wifiInfoTextView.append(String.format("Average RSSI Level: %.2f dBm", average));
        }
    }

    public void getWifiList(View view) {
        wifiList.clear();
        if (wifiManager.startScan()) {
            Toast.makeText(this, "Scanning WiFi networks...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "WiFi scan failed. Try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private final BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            scanResults = wifiManager.getScanResults();
            for (ScanResult scanResult : scanResults) {
                wifiList.add(scanResult.SSID + " (" + scanResult.level + " dBm)");
            }
            adapter.notifyDataSetChanged();
        }
    };

    private static boolean isExternalStorageReadOnly() {
        return Environment.MEDIA_MOUNTED_READ_ONLY.equals(Environment.getExternalStorageState());
    }

    private static boolean isExternalStorageAvailable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    public void save(View view) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View promptsView = inflater.inflate(R.layout.prompts, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(promptsView);

        final EditText userInput = promptsView.findViewById(R.id.editTextDialogUserInput);

        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, id) -> {
                    String fileName = userInput.getText().toString();
                    File file = new File(filePath, fileName);

                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        fos.write(wifiInfoTextView.getText().toString().getBytes());
                        Toast.makeText(this, "File saved at " + filePath, Toast.LENGTH_SHORT).show();
                        wifiInfoTextView.setText("Information will appear here...");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                })
                .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel());

        alertDialogBuilder.create().show();
    }

    public void stopInfo(View view) {
        stop = 1;
    }

    public void viewSaved(View view) {
        startActivity(new Intent(this, Load.class));
    }
}
