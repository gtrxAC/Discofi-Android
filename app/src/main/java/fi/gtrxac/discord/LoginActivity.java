package fi.gtrxac.discord;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

/**
 * Created by gtrx on 29.8.2026.
 */

public class LoginActivity extends Activity {
    private static final String TOKEN_REGEX =
            "[A-Za-z0-9-_]{20,28}.[A-Za-z0-9-_]{4,8}.[A-Za-z0-9-_]{32,}";

    private EditText emailField;
    private EditText passwordField;
    private EditText tokenField;

    public void onCreate(Bundle savedInstanceState) {
        final Context context = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        initPasswordLoginComponents();
        initTokenLoginComponents();
    }

    private void initPasswordLoginComponents() {
        emailField = (EditText) findViewById(R.id.emailField);
        passwordField = (EditText) findViewById(R.id.passwordField);

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
        tokenField = (EditText) findViewById(R.id.tokenField);

        Button tokenLoginButton = (Button) findViewById(R.id.tokenLoginButton);

        tokenLoginButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String token = tokenField.getText().toString();

                if (token.length() == 0) {
                    tokenField.setError("Token is required");
                    return;
                }

                /*if (token.length() < 66 || token.length() > 76 || !token.matches(TOKEN_REGEX)) {
                    tokenField.setError("Invalid token. The token should be a string of about 70 characters.");
                    return;
                }
                */

                tokenField.setError(null);

                finishLogin(v.getContext(), token.replace("\"", "").trim());
            }
        });
    }

    private void initiateLogin() {
        final Context c = this;

        final ProgressDialog dialog = ProgressDialog.show(c, "", "Logging in...", true);

        new Thread() {
            @Override
            public void run() {

                try {
                    Thread.sleep(5000);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                    }
                });

                finishLogin(c, "Mzk4ODc4NzI3OTM4ODk5OTc3.FAKEFAKE." + App.pendingAccount.password);
                finish();
            }
        }.start();
    }

    public static void finishLogin(Context c, String token) {
        App.pendingAccount.token = token;

        Accounts.list.addElement(App.pendingAccount);
        App.pendingAccount = null;
        Settings.save(c);

        Intent intent = new Intent(c, AccountListActivity.class);
        c.startActivity(intent);
    }

}
