package com.example.newpc.qrcode;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Set;

import me.pagar.mposandroid.EmvApplication;
import me.pagar.mposandroid.Mpos;
import me.pagar.mposandroid.MposListener;
import me.pagar.mposandroid.MposPaymentResult;
import me.pagar.mposandroid.PaymentMethod;
import stone.application.enums.ErrorsEnum;
import stone.application.enums.InstalmentTransactionEnum;
import stone.application.enums.TypeOfTransactionEnum;
import stone.application.interfaces.StoneCallbackInterface;
import stone.providers.LoadTablesProvider;
import stone.providers.TransactionProvider;
import stone.utils.GlobalInformations;
import stone.utils.StoneTransaction;

public class TransactionActivity extends AppCompatActivity {

    TextView textFinalValue;
    TextView valueTextView;
    TextView numberInstallmentsTextView;
    //EditText valueEditText;
    RadioGroup radioGroup;
    RadioButton debitRadioButton;
    Button sendButton;
    Spinner instalmentsSpinner;
    int amount;
    boolean isPagarme = false;
    String encryptionKey, macAddress;
    JSONObject productInfo;
    private RequestQueue nRequestQueue;
    private String urlString = "https://api.pagar.me/1/transactions";
    private ProgressBar spinner;
    public JSONObject jsonBody;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            amount = extras.getInt("amount");
            macAddress = extras.getString("macAddress");
            encryptionKey = extras.getString("encryption_key");
            isPagarme = extras.getBoolean("isPagarme");
            try {
                productInfo = new JSONObject(extras.getString("product_info"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        textFinalValue = (TextView) findViewById(R.id.textFinalValue);
        // Mostra o valor formatado pra reais
        NumberFormat realFormat = NumberFormat.getCurrencyInstance();
        textFinalValue.setText(realFormat.format(amount/100.0));

        valueTextView = (TextView) findViewById(R.id.textViewValue);
        spinner = (ProgressBar) findViewById(R.id.progress_bar);
        hideSpinner();
        numberInstallmentsTextView = (TextView) findViewById(R.id.textViewInstallments);
        //valueEditText = (EditText) findViewById(R.id.editTextValue);
        radioGroup = (RadioGroup) findViewById(R.id.radioGroupDebitCredit);
        sendButton = (Button) findViewById(R.id.buttonSendTransaction);
        instalmentsSpinner = (Spinner) findViewById(R.id.spinnerInstallments);
        debitRadioButton = (RadioButton) findViewById(R.id.radioDebit);

        numberInstallmentsTextView.setVisibility(View.INVISIBLE);
        instalmentsSpinner.setVisibility(View.INVISIBLE);

        spinnerAction();
        radioGroupClick();
        sendTransaction();
    }

    private void radioGroupClick() {
        radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.radioDebit) {
                    numberInstallmentsTextView.setVisibility(View.INVISIBLE);
                    instalmentsSpinner.setVisibility(View.INVISIBLE);
                } else {
                    numberInstallmentsTextView.setVisibility(View.VISIBLE);
                    instalmentsSpinner.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    public void sendTransaction() {
        sendButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if(isPagarme) {
                    try {
                        goMpos(macAddress);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    // Cria o objeto de transacao. Usar o "GlobalInformations.getPinpadFromListAt"
                    // significa que devera estar conectado com ao menos um pinpad, pois o metodo
                    // cria uma lista de conectados e conecta com quem estiver na posicao "0".
                    StoneTransaction stoneTransaction = new StoneTransaction(GlobalInformations.getPinpadFromListAt(0));

                    // A seguir deve-se popular o objeto.
                    stoneTransaction.setAmount(String.valueOf(amount));
                    stoneTransaction.setEmailClient(null);
                    stoneTransaction.setRequestId(null);
                    stoneTransaction.setUserModel(GlobalInformations.getUserModel(0));

                    // AVISO IMPORTANTE: Nao e recomendado alterar o campo abaixo do
                    // ITK, pois ele gera um valor unico. Contudo, caso seja
                    // necessario, faca conforme a linha abaixo.
                    stoneTransaction.setInitiatorTransactionKey("SEU_IDENTIFICADOR_UNICO_AQUI");

                    // Informa a quantidade de parcelas.
                    stoneTransaction.setInstalmentTransactionEnum(InstalmentTransactionEnum.getAt(instalmentsSpinner.getSelectedItemPosition()));

                    // Verifica a forma de pagamento selecionada.
                    if (debitRadioButton.isChecked()) {
                        stoneTransaction.setTypeOfTransaction(TypeOfTransactionEnum.DEBIT);
                    } else {
                        stoneTransaction.setTypeOfTransaction(TypeOfTransactionEnum.CREDIT);
                    }

                    // Processo para envio da transacao.
                    final TransactionProvider provider = new TransactionProvider(TransactionActivity.this, stoneTransaction, GlobalInformations.getPinpadFromListAt(0));
                    provider.setWorkInBackground(false);
                    provider.setDialogMessage("Enviando..");
                    provider.setDialogTitle("Aguarde");

                    provider.setConnectionCallback(new StoneCallbackInterface() {
                        public void onSuccess() {
                            Toast.makeText(getApplicationContext(), "Transação enviada com sucesso e salva no banco. Para acessar, use o TransactionDAO.", Toast.LENGTH_SHORT).show();
                            finish();
                        }

                        public void onError() {
                            Toast.makeText(getApplicationContext(), "Erro na transação", Toast.LENGTH_SHORT).show();
                            if (provider.theListHasError(ErrorsEnum.NEED_LOAD_TABLES)) { // code 20
                                LoadTablesProvider loadTablesProvider = new LoadTablesProvider(TransactionActivity.this, provider.getGcrRequestCommand(), GlobalInformations.getPinpadFromListAt(0));
                                loadTablesProvider.setDialogMessage("Subindo as tabelas");
                                loadTablesProvider.setWorkInBackground(false); // para dar feedback ao usuario ou nao.
                                loadTablesProvider.setConnectionCallback(new StoneCallbackInterface() {
                                    public void onSuccess() {
                                        sendButton.performClick(); // simula um clique no botao de enviar transacao para reenviar a transacao.
                                    }

                                    public void onError() {
                                        Toast.makeText(getApplicationContext(), "Sucesso.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                loadTablesProvider.execute();
                            }
                        }
                    });
                    provider.execute();
                }
            }
        });
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
                    mpos.close("TRANS. APROVADA");
                    finish();
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
                        jsonBody.put("installments", instalmentsSpinner.getSelectedItemPosition());
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

    private void spinnerAction() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.installments_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        instalmentsSpinner.setAdapter(adapter);
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
