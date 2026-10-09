package fi.gtrxac.discord;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.Hashtable;

import cc.nnproject.json.JSONObject;

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
        final Context c = this;
        final ProgressDialog dialog = ProgressDialog.show(this, "", "Logging in...", true);

        new Thread() {
            @Override
            public void run() {
                try {
                    Account acc = App.pendingAccount;

                    JSONObject body = new JSONObject();
                    body.put("code", code);
                    body.put("ticket", App.pendingTicket);
                    body.put("login_source", (Object) null);
                    body.put("gift_code_sku_id", (Object) null);
                    body.put("login_instance_id", App.pendingLoginInstanceID);

                    Hashtable<String, String> headers = new Hashtable<>();
                    if (App.pendingFingerprint != null) {
                        headers.put("X-Fingerprint", App.pendingFingerprint);
                    }

                    JSONObject response = (JSONObject) acc.postJSON("/auth/mfa/totp", body, headers);

                    final String token = response.getString("token");

                    // completeLogin finishes this activity once done
                    LoginActivity.completeLogin(c, dialog, token);
                }
                catch (final DiscordException e) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            dialog.dismiss();
                            if (isFinishing()) return;
                            e.show(c);
                        }
                    });
                }
            }
        }.start();
    }
}
