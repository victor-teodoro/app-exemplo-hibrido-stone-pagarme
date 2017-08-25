package com.example.newpc.qrcode;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

import me.pagar.mposandroid.EmvApplication;
import me.pagar.mposandroid.Mpos;
import me.pagar.mposandroid.MposListener;
import me.pagar.mposandroid.MposPaymentResult;
import me.pagar.mposandroid.PaymentMethod;

public class ConnectPagarmeActivity extends AppCompatActivity implements OnItemClickListener {

    static BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    static boolean btConnected = false;
    ListView listView;

    String macAddress;
    private ProgressBar spinner;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devices);
        spinner = (ProgressBar) findViewById(R.id.progress_bar);
        spinner.setVisibility(View.GONE);

        listView = (ListView) findViewById(R.id.listDevicesActivity);
        listView.setOnItemClickListener(this);
        turnBluetoothOn();
        listBluetoothDevices();
    }

    public void listBluetoothDevices() {

        // Lista de Pinpads para passar para o BluetoothConnectionProvider.
        ArrayAdapter<String> btArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        // Lista todos os dispositivos pareados.
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                btArrayAdapter.add(String.format("%s_%s", device.getName(), device.getAddress()));
            }
        }

        // Exibe todos os dispositivos da lista.
        listView.setAdapter(btArrayAdapter);
    }

    public void turnBluetoothOn() {
        try {
            mBluetoothAdapter.enable();
            do {
            } while (!mBluetoothAdapter.isEnabled());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        // Pega o pinpad selecionado do ListView.
        String[] pinpadInfo = listView.getAdapter().getItem(position).toString().split("_");
        macAddress = pinpadInfo[1];

        // Atualiza as tabelas
        try {
            goMpos(pinpadInfo[1]);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void goMpos(String deviceName) throws IOException {
        final Context cont = getApplicationContext();
        BluetoothDevice result = null;

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        Set<BluetoothDevice> devices = adapter.getBondedDevices();
        if (devices != null) {
            for (BluetoothDevice device : devices) {
                if (deviceName.equals(device.getAddress())) {
                    result = device;
                    break;
                }
            }
        }
        BluetoothDevice device = result;

        try {
            final Mpos mpos = new Mpos(device, "ek_test_G84bs5wa355FioxHAC00lGUe1f1p4O", cont);

            mpos.addListener(new MposListener() {

                public void bluetoothConnected() {
                    Log.d("Pagar.me", "Bluetooth connected.");
                    mpos.initialize();
                }

                public void bluetoothDisconnected() {
                    Log.d("Pagar.me", "Bluetooth disconnected.");
                }

                public void bluetoothErrored(int error) {
                    Log.d("Pagar.me", "Received bluetooth error");
                }

                public void receiveInitialization() {
                    try {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showSpinner();
                            }
                        });
                        mpos.downloadEMVTablesToDevice(true);
                    } catch (Exception e) {
                        Log.d("Pagar.me", "Got error in initialization and table update " + e.getMessage());
                    }
                }

                public void receiveNotification(String notification) {
                    Log.d("Pagar.me", "Got Notification " + notification);
                }

                @Override
                public void receiveOperationCompleted() {

                }

                public void receiveTableUpdated(boolean loaded) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            hideSpinner();
                            showSuccess();
                        }
                    });
                    Log.d("Pagar.me", "Tables updated!");
                    mpos.close("Tabelas OK");
                    Intent intent = new Intent(ConnectPagarmeActivity.this, MainActivity.class);
                    intent.putExtra("macAddress", macAddress);
                    intent.putExtra("isPagarme", true);
                    startActivity(intent);
                }

                public void receiveFinishTransaction() {
                    Log.d("Pagar.me", "Finished transaction");
                    mpos.close("TRANS. APROVADA");
                    Toast.makeText(getApplicationContext(), "Transação enviada com sucesso.", Toast.LENGTH_SHORT).show();
                }

                public void receiveClose() {
                    Log.d("Pagar.me", "Receive close");
                    mpos.closeConnection();
                }

                public void receiveCardHash(String cardHash, MposPaymentResult result) {
                    Log.d("Pagar.me", "Card Hash is " + cardHash);
                    Log.d("Pagar.me", "Card Brand is " + result.cardBrand);
                    Log.d("Pagar.me", "FD = " + result.cardFirstDigits + " LD = " + result.cardLastDigits);
                    Log.d("Pagar.me", "ONL = " + result.isOnline);
                }

                public void receiveError(int error) {
                    Log.d("Pagar.me", "Received error " + error);
                    mpos.closeConnection();
                }

                public void receiveOperationCancelled() {
                    Log.d("Pagar.me", "Cancel");
                }
            });

            Log.d("Pagar.me", "Telling to initialize");
            mpos.openConnection(false);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showSpinner() {
        spinner.setVisibility(View.VISIBLE);
    }
    public void hideSpinner() {
        spinner.setVisibility(View.GONE);
    }
    public void showSuccess() {
        Toast.makeText(getApplicationContext(), "Pareamento bem-sucedido.", Toast.LENGTH_LONG).show();
    }
}
