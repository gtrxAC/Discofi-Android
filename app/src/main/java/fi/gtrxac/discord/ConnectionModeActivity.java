package fi.gtrxac.discord;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;

/**
 * Created by gtrx on 30.8.2026.
 */

public class ConnectionModeActivity extends Activity {
    private View proxySettings;

    public void onCreate(Bundle savedInstanceState) {
        final Context context = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.connection);

        App.pendingAccount = new Account();

        // Proxy connection URLs hidden by default, show them when selecting proxy mode
        proxySettings = findViewById(R.id.proxySettings);
        proxySettings.setVisibility(View.GONE);

        RadioGroup connectionGroup = (RadioGroup) findViewById(R.id.modeSelection);

        connectionGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                App.pendingAccount.useProxy = (checkedId == R.id.proxyOption);
                proxySettings.setVisibility(App.pendingAccount.useProxy ? View.VISIBLE : View.GONE);
            }
        });

        CheckBox gatewayCheckBox = (CheckBox) findViewById(R.id.gatewayCheckBox);

        gatewayCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                App.pendingAccount.useGateway = isChecked;
            }
        });

        Button continueButton = (Button) findViewById(R.id.continueButton);

        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (App.pendingAccount.useProxy) {
                    EditText apiUrlField = (EditText) findViewById(R.id.apiUrlField);
                    EditText gatewayUrlField = (EditText) findViewById(R.id.gatewayUrlField);
                    EditText cdnUrlField = (EditText) findViewById(R.id.cdnUrlField);

                    String apiUrl = apiUrlField.getText().toString().trim();
                    String gatewayUrl = gatewayUrlField.getText().toString().trim();
                    String cdnUrl = cdnUrlField.getText().toString().trim();

                    boolean hasError = false;
                    apiUrlField.setError(null);
                    gatewayUrlField.setError(null);
                    cdnUrlField.setError(null);

                    if (apiUrl.length() == 0) {
                        apiUrlField.setError("API URL is required");
                        hasError = true;
                    }

                    if (App.pendingAccount.useGateway && gatewayUrl.length() == 0) {
                        gatewayUrlField.setError("Gateway URL is required for real-time chat");
                        hasError = true;
                    }

                    if (hasError) return;

                    App.pendingAccount.apiUrl = apiUrl;
                    App.pendingAccount.gatewayUrl = gatewayUrl;
                    App.pendingAccount.cdnUrl = cdnUrl;
                }

                Intent intent = new Intent(v.getContext(), LoginActivity.class);
                startActivity(intent);
            }
        });
    }
}
