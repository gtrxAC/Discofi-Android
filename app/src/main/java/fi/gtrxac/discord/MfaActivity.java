package fi.gtrxac.discord;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONObject;
import fi.gtrxac.bluewap.http.HTTP;

/**
 * TOTP entry screen for the second step of password login.
 */
public class MfaActivity extends Activity {
    private EditText totpField;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mfa_entry);

        if (App.pendingAccount == null) {
            finish();
            return;
        }

        totpField = (EditText) findViewById(R.id.totpField);

        Button continueButton = (Button) findViewById(R.id.mfaContinueButton);
        continueButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String code = totpField.getText().toString().trim();

                if (code.length() == 0) {
                    totpField.setError("Authentication code is required");
                    return;
                }

                totpField.setError(null);
                submitTotp(code);
            }
        });
    }

    private void submitTotp(final String code) {
        final ProgressDialog dialog = ProgressDialog.show(this, "", "Logging in...", true);

        new Thread() {
            @Override
            public void run() {
                try {
                    Account acc = App.pendingAccount;

                    JSONObject body = new JSONObject();
                    body.put("code", code);
                    body.put("ticket", App.pendingTicket);
                    body.put("gift_code_sku_id", (Object) null);
                    body.put("login_source", "LoginSource.EMAIL_PW");
                    if (App.pendingFingerprint != null) {
                        body.put("login_fingerprint", App.pendingFingerprint);
                    }

                    HTTP request = acc.createApiRequest("POST", "/auth/mfa/totp");
                    if (App.pendingFingerprint != null) {
                        request.setHeader("X-Fingerprint", App.pendingFingerprint);
                    }
                    request.setData(body.build());

                    int responseCode = request.getResponseCode();
                    String responseText = request.getResponseString();

                    if (responseCode < 200 || responseCode >= 300) {
                        throw new Exception(acc.getApiErrorMessage(responseCode, responseText));
                    }

                    final String token = JSON.getObject(responseText).getString("token");

                    // completeLogin finishes this activity once done
                    LoginActivity.completeLogin(MfaActivity.this, dialog, token);
                }
                catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            dialog.dismiss();
                            if (isFinishing()) return;

                            new AlertDialog.Builder(MfaActivity.this)
                                    .setTitle("Login failed")
                                    .setMessage(e.getMessage())
                                    .setPositiveButton("OK", null)
                                    .show();
                        }
                    });
                }
            }
        }.start();
    }
}
