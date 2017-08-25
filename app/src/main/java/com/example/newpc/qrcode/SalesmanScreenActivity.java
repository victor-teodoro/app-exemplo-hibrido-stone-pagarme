package com.example.newpc.qrcode;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_salesman_screen);

        payWithCard = (Button) (Button) findViewById(R.id.pay_card_btn);
        payWithCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONObject order = getOrder();
                mundipaggOrder(order);
            }
        });
    }

    private JSONArray getItems() {
        // Cria e popula a lista de itens no pedido
        JSONObject item = new JSONObject();
        JSONArray items = new JSONArray();
        try {
            // Item 1
            item.put("amount", 190);
            item.put("description", "Livanto");
            TextView text1 = (TextView) findViewById(R.id.qtde1);
            item.put("quantity", Integer.valueOf(text1.getText().toString()));
            items.put(0, item);

            // Item 2
            item.put("amount", 190);
            item.put("description", "Voluto");
            TextView text2 = (TextView) findViewById(R.id.qtde2);
            item.put("quantity", Integer.valueOf(text2.getText().toString()));
            items.put(1, item);

            // Item 3
            item.put("amount", 190);
            item.put("description", "Cosi");
            TextView text3 = (TextView) findViewById(R.id.qtde3);
            item.put("quantity", Integer.valueOf(text3.getText().toString()));
            items.put(2, item);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return items;
    }

    private JSONObject getCustomer() {
        // Cria e popula os dados do customer
        JSONObject customer = new JSONObject();
        try {
            customer.put("email", "vteodoro@stone.com.br");
            customer.put("name", "Victor Teodoro");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return customer;
    }

    private JSONArray getPayments() {
        // Cria e popula os pagamentos
        JSONArray payments = new JSONArray();
        JSONObject payment = new JSONObject();
        JSONObject creditCard = new JSONObject();
        try {
            payment.put("payment_method", "credit_card");
            creditCard.put("card_id", "card_5Z8AnBVsOh5nGWey");
            payment.put("credit_card", creditCard);
            payments.put(0, payment);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return payments;
    }

    private JSONObject getOrder() {
        //Cria e popula o objeto order
        JSONArray items = getItems();
        JSONObject customer = getCustomer();
        JSONArray payments = getPayments();
        JSONObject order = new JSONObject();
        try {
            order.put("items", items);
            order.put("customer", customer);
            order.put("payments", payments);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return order;
    }

    private void mundipaggOrder(JSONObject order) {
        updateAndroidSecurityProvider();

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        String uri = "https://api.mundipagg.com/core/v1/orders";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST, uri, order,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("MundiSuccess", response.toString());
                        Toast.makeText(getApplicationContext(), "Transação Bem-sucedida", Toast.LENGTH_SHORT).show();
                        Intent rIntent = new Intent(SalesmanScreenActivity.this, MainActivity.class);
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
            calcValorFinal(view);
        }

        if (view.getId() == R.id.button5) {
            TextView text2 = (TextView) findViewById(R.id.qtde2);
            int cont = Integer.valueOf(text2.getText().toString());
            cont += 10;
            text2.setText(String.valueOf(cont));
            calcValorFinal(view);
        }

        if (view.getId() == R.id.button7) {
            TextView text3 = (TextView) findViewById(R.id.qtde3);
            int cont = Integer.valueOf(text3.getText().toString());
            cont += 10;
            text3.setText(String.valueOf(cont));
            calcValorFinal(view);
        }
    }

    public void subtrair(View view) {
        if (view.getId() == R.id.button4) {
            TextView text1 = (TextView) findViewById(R.id.qtde1);
            int cont = Integer.valueOf(text1.getText().toString());
            cont -= 10;
            if (cont < 0) cont = 0;
            text1.setText(String.valueOf(cont));
            calcValorFinal(view);
        }
        if (view.getId() == R.id.button6) {
            TextView text2 = (TextView) findViewById(R.id.qtde2);
            int cont = Integer.valueOf(text2.getText().toString());
            cont -= 10;
            if (cont < 0) cont = 0;
            text2.setText(String.valueOf(cont));
            calcValorFinal(view);
        }
        if (view.getId() == R.id.button8) {
            TextView text3 = (TextView) findViewById(R.id.qtde3);
            int cont = Integer.valueOf(text3.getText().toString());
            cont -= 10;
            if (cont < 0) cont = 0;
            text3.setText(String.valueOf(cont));
            calcValorFinal(view);
        }
    }

    public void calcValorFinal(View View) {
        TextView valorFinal = (TextView) findViewById(R.id.valorFinal);
        TextView text1 = (TextView) findViewById(R.id.qtde1);
        TextView text2 = (TextView) findViewById(R.id.qtde2);
        TextView text3 = (TextView) findViewById(R.id.qtde3);
        double val = Integer.valueOf(text1.getText().toString()) * 1.90 + Integer.valueOf(text2.getText().toString()) * 1.90 + Integer.valueOf(text3.getText().toString()) * 1.90;
        // Mostra o valor formatado pra reais
        NumberFormat realFormat = NumberFormat.getCurrencyInstance();
        valorFinal.setText(realFormat.format(val));
    }

    private void updateAndroidSecurityProvider() {
        try {
            ProviderInstaller.installIfNeeded(this);
        } catch (GooglePlayServicesRepairableException e) {

        } catch (GooglePlayServicesNotAvailableException e) {
            Log.e("SecurityException", "Google Play Services not available.");
        }
    }
}
