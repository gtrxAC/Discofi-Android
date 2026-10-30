package fi.gtrxac.discord;

import fi.gtrxac.discord.model.*;

import android.app.Activity;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
    private static final int TAB_GUILDS = 0;
    private static final int TAB_DMS = 1;
    private static final int TAB_INBOX = 2;

    private ListView guildList;
    private ListView dmChannelList;
    private ListView inboxList;
    private boolean dmChannelsLoaded = false;
    private boolean inboxLoaded = false;

    private Vector<Guild> guilds = new Vector<>();
    private Vector<DMChannel> dmChannels = new Vector<>();
    private Vector<Message> inbox = new Vector<>();

    public void onCreate(Bundle savedInstanceState) {
		final Context context = this;

        Settings.load(this);
        Theme.load(this);

		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_PROGRESS);
		setContentView(R.layout.main);

        final Account acc = Accounts.getActive();

        if (acc == null) {
            goToLoginScreen();
            return;
        }

        acc.startGateway();

        try {
            setTitle(acc.getDisplayTitle());
        }
        catch (DiscordException e) {
            e.printStackTrace();
        }

        TabHost tabHost = getTabHost();
        tabHost.setup();

        TabHost.TabSpec guildsTab = tabHost.newTabSpec("guildsTab");
        guildsTab.setIndicator(getResources().getString(R.string.guilds));
        guildsTab.setContent(R.id.guildsTab);
        tabHost.addTab(guildsTab);

        TabHost.TabSpec dmsTab = tabHost.newTabSpec("dmsTab");
        dmsTab.setIndicator(getResources().getString(R.string.dms));
        dmsTab.setContent(R.id.dmsTab);
        tabHost.addTab(dmsTab);

        TabHost.TabSpec inboxTab = tabHost.newTabSpec("inboxTab");
        inboxTab.setIndicator(getResources().getString(R.string.inbox));
        inboxTab.setContent(R.id.inboxTab);
        tabHost.addTab(inboxTab);

        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                if ("dmsTab".equals(tabId) && !dmChannelsLoaded) {
                    loadDmChannels(context, acc);
                } else if ("inboxTab".equals(tabId) && !inboxLoaded) {
                    loadInbox(context, acc);
                }
            }
        });

        guildList = (ListView) findViewById(R.id.guildList);
        dmChannelList = (ListView) findViewById(R.id.dmList);
        inboxList = (ListView) findViewById(R.id.inboxList);

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
                intent.putExtra("channelName", (!dm.isGroup ? "@" : "") + dm.name);
                startActivity(intent);
            }
        });

        loadGuilds(context, acc);
	}

    @Override
    protected void onResume() {
        super.onResume();

        if (Theme.checkAppearanceChanged(this)) return;

        // Settings may have changed while the settings screen was open.
        for (int i = 0; i < inbox.size(); i++) {
            inbox.elementAt(i).refreshTimestamp();
        }
        if (guildList != null) guildList.invalidateViews();
        if (dmChannelList != null) dmChannelList.invalidateViews();
        if (inboxList != null) inboxList.invalidateViews();
    }

	private void loadGuilds(final Context context, final Account acc) {
        if (acc.guildsAvailable()) {
            try {
                guildsCallback(context, acc.getGuilds(false));
            }
            catch (DiscordException e) {
                e.show(context);
            }
        }
        else {
            showProgress(true);

            new Thread() {
                @Override
                public void run() {
                    fetchGuilds(context, acc, false);
                }
            }.start();
        }
    }

    private void loadDmChannels(final Context context, final Account acc) {
        if (acc.dmChannelsAvailable()) {
            try {
                dmChannelsCallback(context, acc.getDmChannels(false));
            }
            catch (DiscordException e) {
                e.show(context);
            }
        }
        else {
            showProgress(true);

            new Thread() {
                @Override
                public void run() {
                    fetchDmChannels(context, acc, false);
                }
            }.start();
        }
    }

    private void loadInbox(final Context context, final Account acc) {
        if (acc.inboxAvailable()) {
            try {
                inboxCallback(context, acc.getInbox(false));
            }
            catch (DiscordException e) {
                e.show(context);
            }
        }
        else {
            showProgress(true);

            new Thread() {
                @Override
                public void run() {
                    fetchInbox(context, acc, false);
                }
            }.start();
        }
    }

    private void fetchGuilds(final Context context, final Account acc, final boolean refresh) {
        try {
            final Vector<Guild> guilds = acc.getGuilds(refresh);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    guildsCallback(context, guilds);
                }
            });
        }
        catch (final DiscordException e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showProgress(false);
                    e.show(context);
                }
            });
        }
    }

    private void fetchDmChannels(final Context context, final Account acc, final boolean refresh) {
        try {
            final Vector<DMChannel> dmChannels = acc.getDmChannels(refresh);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dmChannelsCallback(context, dmChannels);
                }
            });
        }
        catch (final DiscordException e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showProgress(false);
                    e.show(context);
                }
            });
        }
    }

    private void fetchInbox(final Context context, final Account acc, final boolean refresh) {
        try {
            final Vector<Message> inbox = acc.getInbox(refresh);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    inboxCallback(context, inbox);
                }
            });
        }
        catch (final DiscordException e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showProgress(false);
                    e.show(context);
                }
            });
        }
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
            dmChannelItems.add((!dm.isGroup ? "@" : "") + dm.name);
            dmChannelIcons.add(dm.getIconUrl(Accounts.getActive().getCdnUrl()));
        }

        ArrayAdapter<String> adapter = new IconArrayAdapter(c, dmChannelItems, dmChannelIcons);

        dmChannelList.setAdapter(adapter);

        showProgress(false);
        Settings.save(c);
        dmChannelsLoaded = true;
    }

    public void inboxCallback(Context c, Vector<Message> inbox) {
        this.inbox = inbox;

        if (inbox.size() == 0) {
            TextView inboxEmptyText = (TextView) findViewById(R.id.inboxEmptyText);
            inboxEmptyText.setVisibility(View.VISIBLE);
        } else {
            for (int i = 0; i < inbox.size(); i++) {
                Message m = inbox.elementAt(i);
                m.showAuthor = true;
            }
            inboxList.setAdapter(new MessageListAdapter(c, inbox, Accounts.getActive(), false));
        }

        showProgress(false);
        inboxLoaded = true;
    }

    private void goToLoginScreen() {
        final Account acc = Accounts.getActive();
        if (acc != null) acc.gateway.stop = true;
        Intent intent = new Intent(this, AccountListActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mainRefresh:
                refreshCurrentTab();
                return true;
            case R.id.mainSettings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.mainLogout:
                goToLoginScreen();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void refreshCurrentTab() {
        final Account acc = Accounts.getActive();
        if (acc == null) {
            goToLoginScreen();
            return;
        }
        final Context context = this;
        showProgress(true);

        new Thread() {
            @Override
            public void run() {
                switch (getTabHost().getCurrentTab()) {
                    case TAB_GUILDS:
                        fetchGuilds(context, acc, true);
                        break;
                    case TAB_DMS:
                        fetchDmChannels(context, acc, true);
                        break;
                    case TAB_INBOX:
                        fetchInbox(context, acc, true);
                        break;
                }
            }
        }.start();
    }

    private void showProgress(final boolean show) {
        this.setProgressBarVisibility(show);
        this.setProgressBarIndeterminate(show);
    }
}
