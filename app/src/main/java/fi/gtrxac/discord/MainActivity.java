package fi.gtrxac.discord;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Vector;

import cc.nnproject.json.JSONArray;

public class MainActivity extends Activity {
    private ListView guildList;
    private ProgressDialog loadingDialog;

    public void onCreate(Bundle savedInstanceState) {
		final Context context = this;
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

        Settings.load(this);

        final Account acc = Accounts.getActive();

        if (acc == null) {
            goToLoginScreen();
            return;
        }

        setTitle(acc.getDisplayTitle());

        ListView dmList = (ListView) findViewById(R.id.dmList);
        dmList.setVisibility(View.GONE);

        guildList = (ListView) findViewById(R.id.guildList);

        if (acc.guildsAvailable()) {
            try {
                guildsCallback(context, acc.getGuilds(false));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            loadingDialog = ProgressDialog.show(context, "", "Loading servers...", true);

            new Thread() {
                @Override
                public void run() {
                    try {
                        final Vector<Guild> guilds = acc.getGuilds(false);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                guildsCallback(context, guilds);
                            }
                        });
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
	}

	public void guildsCallback(Context c, Vector<Guild> guilds) {
        ArrayList<String> guildItems = new ArrayList<>();

        for (int i = 0; i < guilds.size(); i++) {
            guildItems.add(guilds.elementAt(i).name);
        }

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(c, android.R.layout.simple_list_item_1, guildItems);

        guildList.setAdapter(adapter);

        if (loadingDialog != null) loadingDialog.dismiss();
    }

	private void goToLoginScreen() {
        Intent intent = new Intent(this, AccountListActivity.class);
        startActivity(intent);
        finish();
    }

}
