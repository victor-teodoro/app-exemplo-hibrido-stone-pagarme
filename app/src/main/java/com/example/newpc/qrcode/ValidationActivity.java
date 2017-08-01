package com.example.newpc.qrcode;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;
import stone.application.StoneStart;
import stone.application.interfaces.StoneCallbackInterface;
import stone.environment.Environment;
import stone.providers.ActiveApplicationProvider;
import stone.user.UserModel;
import stone.utils.Stone;

@RuntimePermissions
public class ValidationActivity extends AppCompatActivity {
    private static final String TAG = "ValidationActivity";
    private static final int REQUEST_PERMISSION_SETTINGS = 100;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_validation);
        ValidationActivityPermissionsDispatcher.initiateAppWithCheck(this);
        Stone.setEnvironment(Environment.SANDBOX);
    }

    @NeedsPermission({Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_EXTERNAL_STORAGE})
    public void initiateApp() {
        /*
        Este deve ser, obrigatoriamente, o primeiro metodo
         * a ser chamado. E um metodo que trabalha com sessao.
		 */
        List<UserModel> user = StoneStart.init(this);

        // se retornar nulo, voce provavelmente nao ativou a SDK
        // ou as informacoes da Stone SDK foram excluidas
        if (user == null) {

            List<String> stoneCodeList = new ArrayList<>();
            // Adicione seu Stonecode abaixo, como string.
            stoneCodeList.add("115577674");

            final ActiveApplicationProvider activeApplicationProvider = new ActiveApplicationProvider(this, stoneCodeList);
            activeApplicationProvider.setDialogMessage("Ativando o aplicativo...");
            activeApplicationProvider.setDialogTitle("Aguarde");
            activeApplicationProvider.setActivity(ValidationActivity.this);
            activeApplicationProvider.setWorkInBackground(false); // informa se este provider ira rodar em background ou nao
            activeApplicationProvider.setConnectionCallback(new StoneCallbackInterface() {

				/* Sempre que utilizar um provider, intancie esta Interface.
                 * Ela ira lhe informar se o provider foi executado com sucesso ou nao
				 */

                /* Metodo chamado se for executado sem erros */
                public void onSuccess() {
                    Toast.makeText(getApplicationContext(), "Ativado com sucesso, iniciando o aplicativo", Toast.LENGTH_SHORT).show();
                    continueApplication();
                }

                /* metodo chamado caso ocorra alguma excecao */
                public void onError() {
                    Toast.makeText(getApplicationContext(), "Erro na ativacao do aplicativo, verifique a lista de erros do provider", Toast.LENGTH_SHORT).show();
                    /* Chame o metodo abaixo para verificar a lista de erros. Para mais detalhes, leia a documentacao: */
                    Log.e("Stone", "onError: " + activeApplicationProvider.getListOfErrors().toString());

                }
            });
            activeApplicationProvider.execute();
        } else {

			/* caso ja tenha as informacoes da SDK e chamado o ActiveApplicationProvider anteriormente
               sua aplicacao podera seguir o fluxo normal */
            continueApplication();

        }
    }

    @OnPermissionDenied({Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_EXTERNAL_STORAGE})
    void showDenied() {
        buildPermissionDialog(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ValidationActivityPermissionsDispatcher.initiateAppWithCheck(ValidationActivity.this);
            }
        });
    }

    @OnNeverAskAgain({Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_EXTERNAL_STORAGE})
    void showNeverAskAgain() {
        buildPermissionDialog(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivityForResult(intent, REQUEST_PERMISSION_SETTINGS);
            }
        });
    }

    private void continueApplication() {
        int SPLASH_DISPLAY_LENGTH = 1000;
        new Handler().postDelayed(new Runnable() {
            public void run() {
                // habilita o modo desenvolvedor
                Intent mainIntent = new Intent(ValidationActivity.this, ConnectPinpadActivity.class);
                startActivity(mainIntent);
            }
        }, SPLASH_DISPLAY_LENGTH);
    }

    @OnShowRationale({Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_EXTERNAL_STORAGE})
    void showRationale(final PermissionRequest request) {
        buildPermissionDialog(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                request.proceed();
            }
        });
    }

    private void buildPermissionDialog(DialogInterface.OnClickListener listener) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Android 6.0")
                .setCancelable(false)
                .setMessage("Com a versão do android igual ou superior ao Android 6.0," +
                        " é necessário que você aceite as permissões para o funcionamento do app.\n\n")
                .setPositiveButton("OK", listener)
                .create().show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_SETTINGS) {
            ValidationActivityPermissionsDispatcher.initiateAppWithCheck(this);
        }
        ValidationActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }
}

