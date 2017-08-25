package com.example.newpc.qrcode;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.SyncStateContract;
import android.support.v4.app.DialogFragment;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.newpc.qrcode.R;
import com.google.android.gms.security.ProviderInstaller;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.security.ProviderInstaller;

import me.pagar.mposandroid.Mpos;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;

import static android.provider.Telephony.Carriers.PASSWORD;

public class ReplenishmentDialog extends DialogFragment {
    ReplenishmentActivity replenishmentActivity;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        replenishmentActivity = (ReplenishmentActivity) getActivity();
        builder.setView(inflater.inflate(R.layout.fragment_replenishment_dialog, null));
        builder.setMessage("Comprar suas cápsulas preferidas?")
                .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Paga o pedido
                        JSONObject order = getOrder();
                        mundipaggOrder(order);
                    }
                })
                .setNegativeButton("Ainda não", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }

    private JSONArray getItems() {
        // Cria e popula a lista de itens no pedido
        JSONObject item = new JSONObject();
        JSONArray items = new JSONArray();
        try {
            // Item 1
            item.put("amount", 190);
            item.put("description", "Livanto");
            item.put("quantity", 20);
            items.put(0, item);

            // Item 2
            item.put("amount", 190);
            item.put("description", "Voluto");
            item.put("quantity", 20);
            items.put(1, item);

            // Item 3
            item.put("amount", 190);
            item.put("description", "Cosi");
            item.put("quantity", 20);
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

        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
        String uri = "https://api.mundipagg.com/core/v1/orders";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST, uri, order,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("MundiSuccess", response.toString());
                        showPickOnBoutique();
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

        /*
        OkHttpClient client = new OkHttpClient();

        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, "{\r\n  \"items\": [\r\n    {\r\n      \"amount\": 190,\r\n      \"description\": \"Voluto\",\r\n      \"quantity\": 20\r\n    },\r\n    {\r\n      \"amount\": 190,\r\n      \"description\": \"Ristretto\",\r\n      \"quantity\": 20\r\n    },\r\n    {\r\n      \"amount\": 190,\r\n      \"description\": \"Cosi\",\r\n      \"quantity\": 20\r\n    }\r\n  ],\r\n  \"customer\": {\r\n    \"name\": \"Victor Teodoro\",\r\n    \"email\": \"victor.teodoro@pagar.me\"\r\n  },\r\n  \"payments\": [{\r\n    \t\"payment_method\": \"credit_card\",\r\n    \t\"credit_card\": {\r\n    \t\t\"card_id\": \"card_5Z8AnBVsOh5nGWey\"\r\n    \t}\r\n    }]\r\n}");
        Request request = new Request.Builder()
                .url("https://api.mundipagg.com/core/v1/orders")
                .post(body)
                .addHeader("content-type", "application/json")
                .addHeader("authorization", "Basic c2tfdGVzdF9uTzdwZXhRVWJkZjIwakx3Ong=")
                .addHeader("cache-control", "no-cache")
                .addHeader("postman-token", "8e5f1278-646e-6651-f46e-aad60d262818")
                .build();

        Response response = client.newCall(request).execute();
        */

        /*
        try {
            HttpResponse<String> response = Unirest.post("https://api.mundipagg.com/core/v1/orders")
                    .header("content-type", "application/json")
                    .header("authorization", "Basic c2tfdGVzdF9uTzdwZXhRVWJkZjIwakx3Ong=")
                    .header("cache-control", "no-cache")
                    .header("postman-token", "79eb29d6-ae7a-7404-f89a-692d3ba261a9")
                    .body("{\r\n  \"items\": [\r\n    {\r\n      \"amount\": 190,\r\n      \"description\": \"Livanto\",\r\n      \"quantity\": 20\r\n    },\r\n    {\r\n      \"amount\": 190,\r\n      \"description\": \"Ristretto\",\r\n      \"quantity\": 20\r\n    },\r\n    {\r\n      \"amount\": 190,\r\n      \"description\": \"Voluto\",\r\n      \"quantity\": 20\r\n    }\r\n  ],\r\n  \"customer\": {\r\n    \"name\": \"Victor Teodoro\",\r\n    \"email\": \"victor.teodoro@pagar.me\"\r\n  },\r\n  \"payments\": [{\r\n      \"payment_method\": \"credit_card\",\r\n      \"credit_card\": {\r\n        \"card_id\": \"card_5Z8AnBVsOh5nGWey\"\r\n      }\r\n    }]\r\n}")
                    .asString();
            Log.d("Mundi", response.toString());
        } catch (UnirestException e) {
            e.printStackTrace();
        }
    }
    */
    }

    private void showPickOnBoutique() {
        GetItOnBoutiqueDialog nextFrag = new GetItOnBoutiqueDialog();
        // Create replenishment popup
        android.support.v4.app.FragmentTransaction fragmentTransaction = replenishmentActivity.getSupportFragmentManager().beginTransaction();
        GetItOnBoutiqueDialog getItOnBoutiqueDialog = new GetItOnBoutiqueDialog();
        getItOnBoutiqueDialog.show(fragmentTransaction, "Replenishment");
    }

    private void updateAndroidSecurityProvider() {
        try {
            ProviderInstaller.installIfNeeded(getContext());
        } catch (GooglePlayServicesRepairableException e) {

        } catch (GooglePlayServicesNotAvailableException e) {
            Log.e("SecurityException", "Google Play Services not available.");
        }
    }
}