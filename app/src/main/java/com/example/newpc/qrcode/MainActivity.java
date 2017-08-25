package com.example.newpc.qrcode;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import stone.application.interfaces.StoneCallbackInterface;
import stone.cache.ApplicationCache;
import stone.providers.DownloadTablesProvider;
import stone.utils.GlobalInformations;

public class MainActivity extends AppCompatActivity {
    Button replenishment_btn, sales_screen_btn, nespressoTower;
    ImageView stoneLogo;

    String macAddress;
    boolean isPagarme;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        replenishment_btn = (Button) findViewById(R.id.replenishment_btn);
        sales_screen_btn = (Button) findViewById(R.id.salesman_screen_btn);
        nespressoTower = (Button) findViewById(R.id.nespresso_tower);
        stoneLogo = (ImageView) findViewById(R.id.stone_logo);
        stoneLogo.setScaleType(ImageView.ScaleType.FIT_XY);


        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            macAddress = extras.getString("macAddress");
            isPagarme = extras.getBoolean("isPagarme");
        }

        replenishment_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent rIntent = new Intent(MainActivity.this, ReplenishmentActivity.class);
                startActivity(rIntent);
            }
        });

        sales_screen_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent rIntent = new Intent(MainActivity.this, CPFActivity.class);
                startActivity(rIntent);
            }
        });

        nespressoTower.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Chama a activity que mostra o produto e passa a URL de onde pegar as infos
                Intent intent = new Intent(MainActivity.this, DisplayProductActivity.class);
                intent.putExtra("product_url", "https://solutions-api.herokuapp.com/nespresso");
                intent.putExtra("macAddress", macAddress);
                intent.putExtra("isPagarme", isPagarme);
                startActivity(intent);
            }
        });
    }

    protected void onResume() {
        super.onResume();
//        if (GlobalInformations.isDeveloper() == true) {
//            Toast.makeText(getApplicationContext(), "Modo desenvolvedor", 1).show();
//        }

        // IMPORTANTE: Mantenha esse provider na sua MAIN, pois ele ira baixar as
        // tabelas AIDs e CAPKs dos servidores da Stone e sera utilizada quando necessÃ¡rio.
        ApplicationCache applicationCache = new ApplicationCache(getApplicationContext());
        if (!applicationCache.checkIfHasTables()) {

            // Realiza processo de download das tabelas em sua totalidade.
            DownloadTablesProvider downloadTablesProvider = new DownloadTablesProvider(MainActivity.this, GlobalInformations.getUserModel(0));
            downloadTablesProvider.setDialogMessage("Baixando as tabelas, por favor aguarde");
            downloadTablesProvider.setWorkInBackground(false); // para dar feedback ao usuario ou nao.
            downloadTablesProvider.setConnectionCallback(new StoneCallbackInterface() {
                public void onSuccess() {
                    Toast.makeText(getApplicationContext(), "Tabelas baixadas com sucesso", Toast.LENGTH_SHORT).show();
                }

                public void onError() {
                    Toast.makeText(getApplicationContext(), "Erro no download das tabelas", Toast.LENGTH_SHORT).show();
                }
            });
            downloadTablesProvider.execute();
        }
    }
}
