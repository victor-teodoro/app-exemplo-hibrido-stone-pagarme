package com.example.newpc.qrcode;

import android.app.Activity;
import android.content.Intent;
import android.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.security.ProviderInstaller;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

public class ReplenishmentActivity extends AppCompatActivity {
    Button replenishment_btn;
    ImageView stoneLogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_replenishment);
        replenishment_btn = (Button) findViewById(R.id.replenishment_btn);
        stoneLogo = (ImageView) findViewById(R.id.stone_logo);
        stoneLogo.setScaleType(ImageView.ScaleType.FIT_XY);

        // Create replenishment popup
        android.support.v4.app.FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        ReplenishmentDialog replenishmentDialog = new ReplenishmentDialog();
        replenishmentDialog.show(fragmentTransaction, "Replenishment");
    }
}
