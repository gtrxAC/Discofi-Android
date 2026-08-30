package fi.gtrxac.discord;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends Activity {
	public void onCreate(Bundle savedInstanceState) {
		final Context context = this;
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

        Settings.load(this);

        // not logged in -> go to login screen
        if (Accounts.getActive() == null) {
            Intent intent = new Intent(this, AccountListActivity.class);
            startActivity(intent);
            finish();
        }
	}

}
