package fi.gtrxac.discord;

import fi.gtrxac.discord.model.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Vector;

/**
 * Shows a guild's channels grouped by category.
 */
public class ChannelListActivity extends Activity {
    private Account account;
    private String guildId;
    private Vector categories;
    private Vector rows;
    private ListView channelList;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.channel_list);

        account = Accounts.getActive();

        if (account == null) {
            finish();
            return;
        }

        String guildId = getIntent().getStringExtra("guildId");
        this.guildId = guildId;
        String guildName = getIntent().getStringExtra("guildName");
        if (guildName != null) setTitle(guildName);

        channelList = (ListView) findViewById(R.id.channelList);
        rows = new Vector();

        channelList.setAdapter(new ChannelListAdapter(this));
        channelList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Row row = (Row) rows.elementAt(position);
                if (row.category) return;

                Channel channel = row.channel;
                if (!channel.isMessageable()) {
                    Toast.makeText(ChannelListActivity.this,
                            "This channel type is not supported yet", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(ChannelListActivity.this, ChatActivity.class);
                intent.putExtra("channelId", channel.id);
                intent.putExtra("channelName", channel.name);
                startActivity(intent);
            }
        });

        loadChannels(guildId);
    }

    private void loadChannels(final String guildId) {
        showProgress(true);

        new Thread() {
            @Override
            public void run() {
                try {
                    final Vector categories = account.getGuildChannels(guildId);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isFinishing()) return;
                            populate(categories);
                        }
                    });
                }
                catch (final DiscordException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isFinishing()) return;
                            showProgress(false);
                            showError(e);
                        }
                    });
                }
            }
        }.start();
    }

    private void populate(Vector categories) {
        showProgress(false);

        this.categories = categories;
        rows.clear();

        for (int i = 0; i < categories.size(); i++) {
            ChannelCategory category = (ChannelCategory) categories.elementAt(i);

            if (category.name != null) {
                Row header = new Row();
                header.category = true;
                header.name = category.name;
                rows.add(header);
            }

            for (int j = 0; j < category.children.size(); j++) {
                Row row = new Row();
                row.channel = (Channel) category.children.elementAt(j);
                row.name = row.channel.name;
                rows.add(row);
            }
        }

        ((ChannelListAdapter) channelList.getAdapter()).notifyDataSetChanged();
    }

    private void showError(final DiscordException e) {
        new AlertDialog.Builder(this)
                .setTitle(e.getTitle())
                .setMessage(e.getDisplayMessage())
                .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        loadChannels(guildId);
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showProgress(final boolean show) {
        this.setProgressBarVisibility(show);
        this.setProgressBarIndeterminate(show);
    }

    private static class Row {
        boolean category;
        String name;
        Channel channel;
    }

    private class ChannelListAdapter extends BaseAdapter {
        private static final int TYPE_CATEGORY = 0;
        private static final int TYPE_CHANNEL = 1;

        private final LayoutInflater inflater;

        public ChannelListAdapter(Context context) {
            inflater = LayoutInflater.from(context);
        }

        public int getCount() {
            return rows.size();
        }

        public Object getItem(int position) {
            return rows.elementAt(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public int getViewTypeCount() {
            return 2;
        }

        public int getItemViewType(int position) {
            return ((Row) rows.elementAt(position)).category ? TYPE_CATEGORY : TYPE_CHANNEL;
        }

        public boolean isEnabled(int position) {
            Row row = (Row) rows.elementAt(position);
            return !row.category;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            Row row = (Row) rows.elementAt(position);

            if (convertView == null) {
                convertView = inflater.inflate(row.category ? R.layout.category_header : R.layout.channel_row, parent, false);
            }

            TextView text = (TextView) convertView;
            text.setText(row.name);
            return convertView;
        }
    }
}
