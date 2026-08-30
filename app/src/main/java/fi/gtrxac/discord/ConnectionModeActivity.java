package fi.gtrxac.discord;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioGroup;

/**
 * Created by gtrx on 30.8.2026.
 */

public class ConnectionModeActivity extends Activity {
    private View proxySettings;
    private boolean useProxy = false;
    private boolean useGateway = true;

    public void onCreate(Bundle savedInstanceState) {
        final Context context = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.accounts);

        App.pendingAccount = new Account();

        // Proxy connection URLs hidden by default, show them when selecting proxy mode
        proxySettings = findViewById(R.id.proxySettings);
        proxySettings.setVisibility(View.GONE);

        RadioGroup connectionGroup = (RadioGroup) findViewById(R.id.modeSelection);

        connectionGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                useProxy = (checkedId == R.id.proxyOption);
                proxySettings.setVisibility(useProxy ? View.VISIBLE : View.GONE);
            }
        });

        CheckBox gatewayCheckBox = (CheckBox) findViewById(R.id.gatewayCheckBox);

        gatewayCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                useGateway = isChecked;
            }
        });

        Button continueButton = (Button) findViewById(R.id.continueButton);

        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                Intent intent = new Intent(v.getContext(), LoginActivity.class);
                startActivity(intent);
            }
        });
    }
}
