package fi.gtrxac.discord;

import fi.gtrxac.discord.model.*;

import android.app.Activity;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
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
    private boolean dmChannelsLoaded = false;

    private Vector<Guild> guilds = new Vector<>();
    private Vector<DMChannel> dmChannels = new Vector<>();

    public void onCreate(Bundle savedInstanceState) {
		final Context context = this;
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_PROGRESS);
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

        guildList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Guild guild = guilds.elementAt(position);

                Intent intent = new Intent(context, ChannelListActivity.class);
                intent.putExtra("guildId", guild.id);
                intent.putExtra("guildName", guild.name);
                startActivity(intent);
            }
        });

        dmChannelList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DMChannel dm = dmChannels.elementAt(position);

                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra("channelId", dm.id);
                intent.putExtra("channelName", dm.name);
                startActivity(intent);
            }
        });

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
            showProgress(true);

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
            showProgress(true);

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
        catch (final Exception e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    fetchFailed("Failed to load servers", e);
                }
            });
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
        catch (final Exception e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    fetchFailed("Failed to load conversations", e);
                }
            });
        }
    }

    private void fetchFailed(String title, Exception e) {
        showProgress(false);

        new android.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(e.getMessage())
                .setPositiveButton("OK", null)
                .show();
    }

    public void guildsCallback(Context c, Vector<Guild> guilds) {
        this.guilds = guilds;

        ArrayList<String> guildItems = new ArrayList<>();
        ArrayList<String> guildIcons = new ArrayList<>();

        for (int i = 0; i < guilds.size(); i++) {
            Guild g = guilds.elementAt(i);
            guildItems.add(g.name);
            guildIcons.add(g.getIconUrl(Accounts.getActive().getCdnUrl()));
        }

        ArrayAdapter<String> adapter = new IconArrayAdapter(c, guildItems, guildIcons);

        guildList.setAdapter(adapter);

        showProgress(false);
        Settings.save(c);
    }

    public void dmChannelsCallback(Context c, Vector<DMChannel> dmChannels) {
        this.dmChannels = dmChannels;

        ArrayList<String> dmChannelItems = new ArrayList<>();
        ArrayList<String> dmChannelIcons = new ArrayList<>();

        for (int i = 0; i < dmChannels.size(); i++) {
            DMChannel dm = dmChannels.elementAt(i);
            dmChannelItems.add(dm.name);
            dmChannelIcons.add(dm.getIconUrl(Accounts.getActive().getCdnUrl()));
        }

        ArrayAdapter<String> adapter = new IconArrayAdapter(c, dmChannelItems, dmChannelIcons);

        dmChannelList.setAdapter(adapter);

        showProgress(false);
        Settings.save(c);
        dmChannelsLoaded = true;
    }

	private void goToLoginScreen() {
        Intent intent = new Intent(this, AccountListActivity.class);
        startActivity(intent);
        finish();
    }

    private void showProgress(final boolean show) {
        this.setProgressBarVisibility(show);
        this.setProgressBarIndeterminate(show);
    }
}
