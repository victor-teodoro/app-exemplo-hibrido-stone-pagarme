package com.example.newpc.qrcode;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

import stone.application.interfaces.StoneCallbackInterface;
import stone.cache.ApplicationCache;
import stone.providers.DownloadTablesProvider;
import stone.utils.GlobalInformations;

public class SalesmanScreenActivity extends AppCompatActivity {
    Button payWithCard;
    TextView qtde1, qtde2, qtde3;
    private ProgressBar spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_salesman_screen);

        qtde1 = (TextView) findViewById(R.id.qtde1);
        qtde1.setText("10");
        qtde2 = (TextView) findViewById(R.id.qtde2);
        qtde2.setText("10");
        qtde3 = (TextView) findViewById(R.id.qtde3);
        qtde3.setText("10");
        calcValorFinal();

        // Salva o spinner e pinta de branco
        spinner = (ProgressBar) findViewById(R.id.progress_bar);
        spinner.setVisibility(View.GONE);

        payWithCard = (Button) (Button) findViewById(R.id.pay_with_gf);
        payWithCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONObject request = new JSONObject();
                int amount = calcValorInt();
                try {
                    request.put("amount", amount);
                    request.put("date", "2017-09-29");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.d("Request Enviada", request.toString());
                sendToGF(request);
            }
        });
    }

    private void sendToGF(JSONObject request) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showSpinner();
            }
        });
        updateAndroidSecurityProvider();

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        String uri = "https://solutions-api.herokuapp.com/garantia_fornecedor";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST, uri, request,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("GarantiaSuccess", response.toString());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hideSpinner();
                            }
                        });
                        Toast.makeText(getApplicationContext(), "Operação Bem-sucedida", Toast.LENGTH_SHORT).show();
                        Intent rIntent = new Intent(SalesmanScreenActivity.this, ReaderActivity.class);
                        rIntent.putExtra("payments", response.toString());
                        startActivity(rIntent);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error == null || error.networkResponse == null) {
                    return;
                }

                String body;
                //get status code here
                final String statusCode = String.valueOf(error.networkResponse.statusCode);
                //get response body and parse with appropriate encoding
                try {
                    body = new String(error.networkResponse.data,"UTF-8");
                    Log.d("MundiError", body);
                } catch (UnsupportedEncodingException e) {
                    // exception
                }
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> params = new HashMap<String, String>();
                params.put("Authorization", "Basic c2tfdGVzdF9uTzdwZXhRVWJkZjIwakx3Ong=");
                //params.put("Content-Type", "application/json");
                params.put("Accept", "application/json");
                return params;
            }
        };
        requestQueue.add(jsonObjectRequest);
    }

    public void adicionar(View view) {
        if (view.getId() == R.id.button3) {
            TextView text1 = (TextView) findViewById(R.id.qtde1);
            int cont = Integer.valueOf(text1.getText().toString());
            cont += 10;
            text1.setText(String.valueOf(cont));
            calcValorFinal();
        }

        if (view.getId() == R.id.button5) {
            TextView text2 = (TextView) findViewById(R.id.qtde2);
            int cont = Integer.valueOf(text2.getText().toString());
            cont += 10;
            text2.setText(String.valueOf(cont));
            calcValorFinal();
        }

        if (view.getId() == R.id.button7) {
            TextView text3 = (TextView) findViewById(R.id.qtde3);
            int cont = Integer.valueOf(text3.getText().toString());
            cont += 10;
            text3.setText(String.valueOf(cont));
            calcValorFinal();
        }
    }

    public void subtrair(View view) {
        if (view.getId() == R.id.button4) {
            TextView text1 = (TextView) findViewById(R.id.qtde1);
            int cont = Integer.valueOf(text1.getText().toString());
            cont -= 10;
            if (cont < 0) cont = 0;
            text1.setText(String.valueOf(cont));
            calcValorFinal();
        }
        if (view.getId() == R.id.button6) {
            TextView text2 = (TextView) findViewById(R.id.qtde2);
            int cont = Integer.valueOf(text2.getText().toString());
            cont -= 10;
            if (cont < 0) cont = 0;
            text2.setText(String.valueOf(cont));
            calcValorFinal();
        }
        if (view.getId() == R.id.button8) {
            TextView text3 = (TextView) findViewById(R.id.qtde3);
            int cont = Integer.valueOf(text3.getText().toString());
            cont -= 10;
            if (cont < 0) cont = 0;
            text3.setText(String.valueOf(cont));
            calcValorFinal();
        }
    }

    public void calcValorFinal() {
        TextView valorFinal = (TextView) findViewById(R.id.valorFinal);
        TextView text1 = (TextView) findViewById(R.id.qtde1);
        TextView text2 = (TextView) findViewById(R.id.qtde2);
        TextView text3 = (TextView) findViewById(R.id.qtde3);
        double val = Integer.valueOf(text1.getText().toString()) * 24.30 + Integer.valueOf(text2.getText().toString()) * 25.60 + Integer.valueOf(text3.getText().toString()) * 29.90;
        // Mostra o valor formatado pra reais
        NumberFormat realFormat = NumberFormat.getCurrencyInstance();
        valorFinal.setText(realFormat.format(val));
    }

    public int calcValorInt() {
        TextView text1 = (TextView) findViewById(R.id.qtde1);
        TextView text2 = (TextView) findViewById(R.id.qtde2);
        TextView text3 = (TextView) findViewById(R.id.qtde3);
        int amount = (int) ((Integer.valueOf(text1.getText().toString()) * 24.30 + Integer.valueOf(text2.getText().toString()) * 25.60 + Integer.valueOf(text3.getText().toString()) * 29.90) * 100);
        return amount;
    }

    private void updateAndroidSecurityProvider() {
        try {
            ProviderInstaller.installIfNeeded(this);
        } catch (GooglePlayServicesRepairableException e) {

        } catch (GooglePlayServicesNotAvailableException e) {
            Log.e("SecurityException", "Google Play Services not available.");
        }
    }

    public void showSpinner() {
        spinner.setVisibility(View.VISIBLE);
    }
    public void hideSpinner() {
        spinner.setVisibility(View.GONE);
    }
}
