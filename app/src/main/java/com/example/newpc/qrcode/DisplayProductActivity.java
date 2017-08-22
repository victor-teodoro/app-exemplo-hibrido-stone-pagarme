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
import android.widget.Button;
import android.widget.ImageView;
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
    private Button payMposBtn, payCardBtn, payOnCashier;
    private Button connection_btn;
    private ImageView product_image;
    private TextView product_name;
    private TextView product_amount;
    JSONObject productInfo;
    String productURL, encryptionKey, cardId;

    private RequestQueue nRequestQueue;

    String macAddress = null;
    boolean isPagarme = false;

    public String device;
    private String urlString = "https://api.pagar.me/1/subscriptions";
    public JSONObject jsonBody;
    private ProgressBar spinner;

    int amount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_product);
        payMposBtn = (Button) findViewById(R.id.pay_mpos_btn);
        payCardBtn = (Button) findViewById(R.id.pay_card_btn);
        product_image = (ImageView) findViewById(R.id.product_image);
        product_name = (TextView) findViewById(R.id.product_name);
        product_amount = (TextView) findViewById(R.id.product_amount);
        spinner = (ProgressBar) findViewById(R.id.progress_bar);
        spinner.setVisibility(View.GONE);
        final Activity thisActivity = this;

        // Permitir fazer GET na thread principal
        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        if (SDK_INT > 8)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        // Resgata a URL do produto
        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            productURL = "https://solutions-api.herokuapp.com/flytour";
            macAddress = extras.getString("macAddress");
            isPagarme = extras.getBoolean("isPagarme");
        }

        // Baixa as infos do produto
        getAPI(productURL);

        // Paga no caixa
        payOnCashier.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(DisplayProductActivity.this, "Compra registrada com sucesso!\nDirija-se ao caixa mais próximo.", Toast.LENGTH_LONG).show();
            }
        });

        // Paga com cartão tokenizado
        payCardBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showSpinner();
                        }
                    });

                    // Resgata os parâmetros enviados pela API de Soluções
                    JSONArray splitRules = productInfo.getJSONArray("split_rules");
                    JSONObject metadata = productInfo.getJSONObject("metadata");
                    String apiKey = productInfo.getString("api_key");
                    String softDescriptor = productInfo.getString("soft_descriptor");
                    String planId = productInfo.getString("plan_id");
                    JSONObject customer = productInfo.getJSONObject("customer");
                    Log.d("Pagar.me", planId);

                    // Inicializa o JSON da transação
                    jsonBody = new JSONObject();
                    jsonBody.put("api_key", apiKey);
                    jsonBody.put("card_id", cardId);
                    jsonBody.put("amount", amount);
                    jsonBody.put("plan_id", planId);
                    jsonBody.put("customer", customer);
                    jsonBody.put("soft_descriptor", softDescriptor);
                    jsonBody.put("metadata", metadata);
                    jsonBody.put("split_rules", splitRules);

                    // Faz o POST no Pagar.me
                    postApi();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        // Paga com MPOS
        payMposBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent rIntent = new Intent(DisplayProductActivity.this, TransactionActivity.class);
                rIntent.putExtra("amount", amount);
                rIntent.putExtra("product_info", productInfo.toString());
                rIntent.putExtra("macAddress", macAddress);
                rIntent.putExtra("encryption_key", encryptionKey);
                rIntent.putExtra("isPagarme", isPagarme);
                startActivity(rIntent);
            }
        });
    }

    public void onViewCreated(View view, Bundle savedInstanceState)
    {

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if(resultCode == RESULT_OK) {
                String macAddress = data.getStringExtra("macAddress");

                try {
                    Log.e("BT", macAddress);
                    goMpos(macAddress);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void getAPI(final String productURL) {
        nRequestQueue = Volley.newRequestQueue(this);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showSpinner();
            }
        });

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, productURL, productInfo, new Response.Listener<JSONObject>() {
                    public static final String TAG = "Download";

                    @Override
                    public void onResponse(JSONObject t) {
                        String resp = ("Response: " + t.toString());
                        Log.i(TAG,resp);
                        productInfo = t;

                        // Pega a imagem na URL dada e a exibe
                        URL url = null;
                        String name = null;
                        try {
                            url = new URL(productInfo.getString("url"));
                            name = productInfo.getString("name");
                            amount = productInfo.getInt("amount");
                            encryptionKey = productInfo.getString("encryption_key");
                            cardId = productInfo.getString("card_id");
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        Bitmap bmp = null;
                        try {
                            bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hideSpinner();
                            }
                        });
                        product_image.setImageBitmap(bmp);
                        product_name.setText(name);

                        // Mostra o valor formatado pra reais
                        NumberFormat realFormat = NumberFormat.getCurrencyInstance();
                        product_amount.setText(realFormat.format(amount/100.0));
                    }
                }, new Response.ErrorListener() {

                    public static final String TAG = "Download";

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String resp = ("err: " + error.toString());
                        Log.i(TAG,resp);
                    }
                });

        nRequestQueue.add(jsObjRequest);
    }

    public void goMpos(String deviceName) throws IOException {

        //BluetoothDevice deviceAsked = new BluetoothDevice(deviceName);
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
        final ProgressDialog progress = new ProgressDialog(this);

        try {
            final Mpos mpos = new Mpos(device, encryptionKey, cont);


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
                    Log.d("Pagar.me", "receive initialization!");

                    // Pagar a trx
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showSpinner();
                        }
                    });
                    ArrayList<EmvApplication> l = new ArrayList<EmvApplication>();
                    EmvApplication masterCredit = new EmvApplication(PaymentMethod.CreditCard, "master");
                    l.add(masterCredit);
                    mpos.payAmount(amount, null, PaymentMethod.CreditCard);

                }

                public void receiveNotification(String notification) {
                    Log.d("Pagar.me", "Got Notification " + notification);
                }

                @Override
                public void receiveOperationCompleted() {
                }

                public void receiveTableUpdated(boolean loaded) {
                    Log.d("Pagar.me", "received table updated loaded = " + loaded);

                    ArrayList<EmvApplication> l = new ArrayList<EmvApplication>();
                    EmvApplication masterCredit = new EmvApplication(PaymentMethod.CreditCard, "master");
                    l.add(masterCredit);
                    mpos.payAmount(amount, null, PaymentMethod.CreditCard);
                }

                public void receiveFinishTransaction() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            hideSpinner();
                            showSuccess();
                        }
                    });
                    Log.d("Pagar.me", "Finished transaction");
                    progress.dismiss();
                    mpos.close("TRANS. APROVADA");

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

                    try {
                        // Resgata os parâmetros enviados pela API de Soluções
                        JSONArray splitRules = productInfo.getJSONArray("split_rules");
                        JSONObject metadata = productInfo.getJSONObject("metadata");
                        String apiKey = productInfo.getString("api_key");
                        String softDescriptor = productInfo.getString("soft_descriptor");

                        // Inicializa o JSON da transação
                        jsonBody = new JSONObject();
                        jsonBody.put("api_key", apiKey);
                        jsonBody.put("card_hash", cardHash);
                        jsonBody.put("amount", amount);
                        jsonBody.put("soft_descriptor", softDescriptor);
                        jsonBody.put("metadata", metadata);
                        jsonBody.put("split_rules", splitRules);

                        // Faz o POST no Pagar.me
                        postApiMpos(mpos);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                public void receiveError(int error) {
                    Log.d("Pagar.me", "Received error " + error);
                    if (error == 11){
                        mpos.close("APROVADO!");
                        Toast.makeText(getApplicationContext(), "Transação enviada com sucesso e salva no banco.", Toast.LENGTH_SHORT).show();
                    }
                    else
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

    private void postApiMpos(final Mpos mpos) {

        nRequestQueue = Volley.newRequestQueue(this);

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.POST, urlString, jsonBody, new Response.Listener<JSONObject>() {

                    public static final String TAG = "POST Pagar.me";

                    @Override
                    public void onResponse(JSONObject t) {
                        String resp = ("Response: " + t.toString());
                        Log.i(TAG,resp);
                        try {
                            String r = t.get("acquirer_response_code").toString();
                            //String emv_r = t.get("card_emv_response").toString();
                            Log.d("Pagar.me", "resp = " + r);
                            //Log.d("Pagar.me", "emv_resp = " + emv_r);
                            mpos.finishTransaction(true, Integer.parseInt((String) t.get("acquirer_response_code".toString())), "000000000.0000");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

                    public static final String TAG = "POST Pagar.me";

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String resp = ("err: " + error.toString());
                        Log.i(TAG,resp);
                    }
                });

        nRequestQueue.add(jsObjRequest);
    }

    private void postApi() {

        nRequestQueue = Volley.newRequestQueue(this);

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.POST, urlString, jsonBody, new Response.Listener<JSONObject>() {

                    public static final String TAG = "POST Pagar.me";

                    @Override
                    public void onResponse(JSONObject t) {
                        String resp = ("Response: " + t.toString());
                        Log.i(TAG, resp);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hideSpinner();
                                showSuccess();
                            }
                        });
                    }
                }, new Response.ErrorListener() {

                    public static final String TAG = "POST Pagar.me";

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String resp = ("err: " + error.toString());
                        Log.i(TAG,resp);
                    }
                });

        nRequestQueue.add(jsObjRequest);
    }

    public void showSpinner() {
        spinner.setVisibility(View.VISIBLE);
    }
    public void hideSpinner() {
        spinner.setVisibility(View.GONE);
    }

    public void showSuccess() {
        Toast.makeText(getApplicationContext(), "Transação enviada com sucesso.", Toast.LENGTH_LONG).show();
    }
}
