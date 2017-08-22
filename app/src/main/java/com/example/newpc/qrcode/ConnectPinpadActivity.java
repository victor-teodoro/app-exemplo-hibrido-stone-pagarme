package com.example.newpc.qrcode;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import stone.application.interfaces.StoneCallbackInterface;
import stone.cache.ApplicationCache;
import stone.providers.DownloadTablesProvider;
import stone.utils.GlobalInformations;

public class ConnectPinpadActivity extends AppCompatActivity {
    Button connectPagarme, connectStone, payWithCard;
    ImageView stoneLogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_pinpad);
        connectPagarme = (Button) findViewById(R.id.connect_pagarme);
        stoneLogo = (ImageView) findViewById(R.id.stone_logo);
        stoneLogo.setScaleType(ImageView.ScaleType.FIT_XY);
        connectPagarme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent rIntent = new Intent(ConnectPinpadActivity.this, ConnectPagarmeActivity.class);
                startActivity(rIntent);
            }
        });
    }
}
