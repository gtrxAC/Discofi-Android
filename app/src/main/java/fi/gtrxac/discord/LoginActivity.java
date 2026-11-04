package fi.gtrxac.discord;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.util.Hashtable;

import cc.nnproject.json.JSONObject;

import fi.gtrxac.bluewap.Util;

/**
 * Created by gtrx on 29.8.2026.
 */

public class LoginActivity extends Activity {
    private static final String TOKEN_REGEX =
            "[A-Za-z0-9-_]{20,28}.[A-Za-z0-9-_]{4,8}.[A-Za-z0-9-_]{32,}";
    private static final int TOKEN_FILE_REQUEST = 1;

    private EditText emailField;
    private EditText passwordField;
    private EditText tokenField;

    public void onCreate(Bundle savedInstanceState) {
        final Context context = this;
        Theme.load(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initPasswordLoginComponents();
        initTokenLoginComponents();
    }

    private void initPasswordLoginComponents() {
        emailField = (EditText) findViewById(R.id.emailField);
        passwordField = (EditText) findViewById(R.id.passwordField);

        if (App.pendingAccount != null) {
            if (App.pendingAccount.email != null) {
                emailField.setText(App.pendingAccount.email);
            }
            if (App.pendingAccount.password != null) {
                TextView existingPasswordText = (TextView) findViewById(R.id.existingPasswordText);
                existingPasswordText.setVisibility(View.VISIBLE);
            }
        }

        Button loginButton = (Button) findViewById(R.id.loginButton);

        loginButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailField.getText().toString().trim();
                String password = passwordField.getText().toString();

                boolean hasError = false;
                emailField.setError(null);
                passwordField.setError(null);

                if (email.length() == 0) {
                    emailField.setError("Email address is required");
                    hasError = true;
                }
                else if (!email.contains("@")) {
                    emailField.setError("Invalid email address");
                    hasError = true;
                }

                // Reuse the password saved with the account if being edited
                if (password.length() == 0 && App.pendingAccount != null) {
                    password = App.pendingAccount.password;
                }

                if (password.length() == 0) {
                    passwordField.setError("Password is required");
                    hasError = true;
                }

                if (hasError) return;

                App.pendingAccount.email = email;
                App.pendingAccount.password = password;

                initiateLogin();
            }
        });
    }

    private void initTokenLoginComponents() {
        final Context c = this;

        tokenField = (EditText) findViewById(R.id.tokenField);
        Button tokenLoginButton = (Button) findViewById(R.id.tokenLoginButton);
        Button tokenFileImportButton = (Button) findViewById(R.id.tokenFileImportButton);

        if (App.pendingAccount != null) {
            tokenField.setText(App.pendingAccount.token);
        }

        tokenLoginButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String token = tokenField.getText().toString().trim();
                if (token.length() == 0) {
                    tokenField.setError("Token is required");
                    return;
                }
                tokenField.setError(null);
                loginToken(token);
            }
        });

        tokenFileImportButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(c, FilePickerActivity.class), TOKEN_FILE_REQUEST);
            }
        });
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == TOKEN_FILE_REQUEST && resultCode == RESULT_OK) {
            try {
                InputStream is = this.getContentResolver().openInputStream(data.getData());
                byte[] tokenBytes = Util.readBytes(is);
                String token = Util.bytesToString(tokenBytes).trim();
                if (token == null || token.length() == 0) {
                    Toast.makeText(this, "Token file is empty", Toast.LENGTH_LONG).show();
                    return;
                }
                loginToken(token);
            } catch (Exception e) {
                e.printStackTrace();
                new DiscordException(e).show(this);
            }
        }
    }

    private void loginToken(final String token) {
        final Context c = this;

        final ProgressDialog dialog = ProgressDialog.show(c, "", "Logging in...", true);

        new Thread() {
            @Override
            public void run() {
                App.pendingAccount.token = token;

                try {
                    App.pendingAccount.fetchUserProfile();
                }
                catch (final DiscordException e) {
                    App.pendingAccount.token = null;
                    runOnUiThread(new Runnable() {
                        public void run() {
                            dialog.dismiss();
                            if (isFinishing()) return;
                            e.show(c);
                        }
                    });
                    return;
                }

                completeLogin(c, dialog, token);
            }
        }.start();
    }

    /**
     * Password login: fingerprint -> /auth/login -> (optionally) TOTP.
     */
    private void initiateLogin() {
        final Context c = this;

        final ProgressDialog dialog = ProgressDialog.show(c, "", "Logging in...", true);

        new Thread() {
            @Override
            public void run() {
                try {
                    Account acc = App.pendingAccount;

                    // 1. Get a login fingerprint from the experiments endpoint
                    JSONObject experiments =
                            (JSONObject) acc.fetchJSON("/experiments?with_guild_experiments=true");
                    String fingerprint = experiments.getString("fingerprint", null);

                    // 2. Submit email + password
                    JSONObject loginBody = new JSONObject();
                    loginBody.put("login", acc.email);
                    loginBody.put("password", acc.password);
                    loginBody.put("undelete", false);
                    loginBody.put("login_source", (Object) null);
                    loginBody.put("gift_code_sku_id", (Object) null);

                    Hashtable<String, String> headers = new Hashtable<>();
                    if (fingerprint != null) {
                        headers.put("X-Fingerprint", fingerprint);
                    }

                    JSONObject loginResponse = (JSONObject) acc.postJSON("/auth/login", loginBody, headers);

                    if (loginResponse.getBoolean("mfa", false)) {
                        // 3a. Multi-factor authentication required -> TOTP screen
                        App.pendingTicket = loginResponse.getString("ticket");
                        App.pendingLoginInstanceID = loginResponse.getString("login_instance_id");
                        App.pendingFingerprint = fingerprint;

                        runOnUiThread(new Runnable() {
                            public void run() {
                                dialog.dismiss();
                                if (isFinishing()) return;
                                startActivity(new Intent(c, MfaActivity.class));
                            }
                        });
                    } else {
                        // 3b. No MFA -> token returned directly
                        final String token = loginResponse.getString("token");
                        completeLogin(c, dialog, token);
                    }
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

    /**
     * Fetch the user profile for the given token and finish the login.
     * Runs on a background thread.
     */
    static void completeLogin(final Context c, final ProgressDialog dialog, final String token) {
        App.pendingAccount.token = token;

        try {
            App.pendingAccount.fetchUserProfile();
        }
        catch (DiscordException e) {
            e.printStackTrace();
        }

        runOnUiThreadStatic(c, new Runnable() {
            public void run() {
                if (dialog != null) dialog.dismiss();
                finishLogin(c, token);
                if (c instanceof Activity) {
                    ((Activity) c).finish();
                }
            }
        });
    }

    private static void runOnUiThreadStatic(final Context c, final Runnable r) {
        if (c instanceof Activity) {
            ((Activity) c).runOnUiThread(r);
        } else {
            r.run();
        }
    }

    public static void finishLogin(Context c, String token) {
        App.pendingAccount.token = token;

        // Add the account if it doesn't already exist.
        if (!Accounts.list.contains(App.pendingAccount)) {
            Accounts.list.addElement(App.pendingAccount);
        }
        App.pendingAccount = null;
        Settings.save(c);

        // Clear the connection setup / login / MFA screens from the back stack
        Intent intent = new Intent(c, AccountListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        c.startActivity(intent);
    }

}
