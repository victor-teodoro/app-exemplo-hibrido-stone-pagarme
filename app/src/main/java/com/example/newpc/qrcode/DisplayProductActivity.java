package com.example.newpc.qrcode;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import me.pagar.mposandroid.EmvApplication;
import me.pagar.mposandroid.Mpos;
import me.pagar.mposandroid.MposListener;
import me.pagar.mposandroid.MposPaymentResult;
import me.pagar.mposandroid.PaymentMethod;

import static com.example.newpc.qrcode.R.id.view;

public class DisplayProductActivity extends AppCompatActivity {
    ListView paymentsList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_product);

        // Popula a lista de pagamentos
        Bundle extras = getIntent().getExtras();
        JSONArray payments = null;
        try {
            JSONObject paymentsObject = new JSONObject(extras.getString("payments"));
            payments = (JSONArray) paymentsObject.get("payments");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ArrayList paymentsArray = new ArrayList<String>();
        for (int i = 0; i < payments.length(); i++) {
            JSONObject payment = null;
            int amount = 0;
            String date = "";
            try {
                payment = (JSONObject) payments.get(i);
                amount = (int) payment.get("amount");
                String jsDate = (String) payment.get("day");
                date = (String) jsDate.subSequence(8, 10) + "/" + jsDate.subSequence(5, 7) + "/" + jsDate.subSequence(0, 4);
                Log.d("String", date);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            NumberFormat realFormat = NumberFormat.getCurrencyInstance();
            paymentsArray.add("Valor: " + realFormat.format(amount/100.0) + "\t" + "Dia: " + date);
        }

        paymentsList = (ListView) findViewById(R.id.payment_list);
        // Create The Adapter with passing ArrayList as 3rd parameter
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, paymentsArray);
        // Set The Adapter
        paymentsList.setAdapter(arrayAdapter);

        // Permitir fazer GET na thread principal
        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        if (SDK_INT > 8) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
    }
}
