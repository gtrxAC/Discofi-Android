package fi.gtrxac.discord;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Vector;

import cc.nnproject.json.JSONArray;

public class MainActivity extends TabActivity {
    private ListView guildList;
    private ListView dmChannelList;
    private ProgressDialog loadingDialog;
    private boolean dmChannelsLoaded = false;

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

        TabHost tabHost = getTabHost();
        tabHost.setup();

        TabHost.TabSpec guildsTab = tabHost.newTabSpec("guildsTab");
        guildsTab.setIndicator("Servers");
        guildsTab.setContent(R.id.guildsTab);
        tabHost.addTab(guildsTab);

        TabHost.TabSpec dmsTab = tabHost.newTabSpec("dmsTab");
        dmsTab.setIndicator("Direct Messages");
        dmsTab.setContent(R.id.dmsTab);
        tabHost.addTab(dmsTab);

        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                if (!dmChannelsLoaded) {
                    loadDmChannels(context, acc);
                }
            }
        });

        guildList = (ListView) findViewById(R.id.guildList);
        dmChannelList = (ListView) findViewById(R.id.dmList);

        loadGuilds(context, acc);
	}

	private void loadGuilds(final Context context, final Account acc) {
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
                    fetchGuilds(context, acc);
                }
            }.start();
        }
    }

    private void loadDmChannels(final Context context, final Account acc) {
        if (acc.dmChannelsAvailable()) {
            try {
                dmChannelsCallback(context, acc.getDmChannels(false));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            loadingDialog = ProgressDialog.show(context, "", "Loading conversations...", true);

            new Thread() {
                @Override
                public void run() {
                    fetchDmChannels(context, acc);
                }
            }.start();
        }
    }

	private void fetchGuilds(final Context context, Account acc) {
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

    private void fetchDmChannels(final Context context, Account acc) {
        try {
            final Vector<DMChannel> dmChannels = acc.getDmChannels(false);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dmChannelsCallback(context, dmChannels);
                }
            });
        }
        catch (Exception e) {
            e.printStackTrace();
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

    public void dmChannelsCallback(Context c, Vector<DMChannel> dmChannels) {
        ArrayList<String> dmChannelItems = new ArrayList<>();

        for (int i = 0; i < dmChannels.size(); i++) {
            dmChannelItems.add(dmChannels.elementAt(i).name);
        }

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(c, android.R.layout.simple_list_item_1, dmChannelItems);

        dmChannelList.setAdapter(adapter);

        if (loadingDialog != null) loadingDialog.dismiss();
        dmChannelsLoaded = true;
    }

	private void goToLoginScreen() {
        Intent intent = new Intent(this, AccountListActivity.class);
        startActivity(intent);
        finish();
    }

}
