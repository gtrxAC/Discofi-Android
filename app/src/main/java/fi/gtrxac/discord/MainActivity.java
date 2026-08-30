package fi.gtrxac.discord;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
	public void onCreate(Bundle savedInstanceState) {
		final Context context = this;
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

        Settings.load(this);

        if (Accounts.getActive() == null) {
            goToLoginScreen();
            return;
        }

        /*TextView t = (TextView) findViewById(R.id.mainTest);
        t.setText("Current account " + Accounts.getActive().getDisplayTitle());

        Button b = (Button) findViewById(R.id.logoutTest);

        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToLoginScreen();
            }
        });*/
	}

	private void goToLoginScreen() {
        Intent intent = new Intent(this, AccountListActivity.class);
        startActivity(intent);
        finish();
    }

}
