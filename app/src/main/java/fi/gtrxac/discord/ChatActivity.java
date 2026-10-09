package fi.gtrxac.discord;

import fi.gtrxac.discord.model.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.Vector;

/**
 * Message view for a channel or DM: reading (with pagination), clustering,
 * and sending.
 */
public class ChatActivity extends Activity {
    public static final int MESSAGE_LOAD_COUNT = 25;

    private Account account;
    private String channelId;

    private Vector messages;
    private MessageListAdapter adapter;
    private ListView messageList;

    private View replyPanel;
    private TextView replyTo;
    private ToggleButton pingToggle;
    private ImageButton cancelReply;

    private EditText composer;
    private Button sendButton;

    private boolean initialLoadDone;
    private boolean loadingOlder;
    private boolean sending;
    private Message activeReply;
    private boolean mention;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.chat);

        account = Accounts.getActive();

        if (account == null) {
            finish();
            return;
        }

        channelId = getIntent().getStringExtra("channelId");
        String channelName = getIntent().getStringExtra("channelName");
        if (channelName != null) setTitle(channelName);

        messages = new Vector();
        messageList = (ListView) findViewById(R.id.messageList);

        replyPanel = (View) findViewById(R.id.reply_panel);
        replyTo = (TextView) findViewById(R.id.reply_to);
        pingToggle = (ToggleButton) findViewById(R.id.ping_toggle);
        cancelReply = (ImageButton) findViewById(R.id.cancel_reply);

        composer = (EditText) findViewById(R.id.messageComposer);
        sendButton = (Button) findViewById(R.id.sendButton);

        registerForContextMenu(messageList);

        adapter = new MessageListAdapter(this, messages, account);
        messageList.setAdapter(adapter);

        sendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendMessage();
            }
        });

        /*composer.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
                sendMessage();
                return true;
            }
        });*/

        cancelReply.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                replyPanel.setVisibility(View.GONE);
            }
        });

        messageList.setOnScrollListener(new AbsListView.OnScrollListener() {
            public void onScrollStateChanged(AbsListView view, int scrollState) {}

            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                                 int totalItemCount) {
                if (!initialLoadDone || loadingOlder || sending) return;

                // Near the top -> fetch the previous page of history
                if (firstVisibleItem == 0 && messages.size() >= MESSAGE_LOAD_COUNT) {
                    loadOlderMessages();
                }
            }
        });

        loadInitialMessages();
    }

    private static final int EDIT_ID = 0;
    private static final int REPLY_ID = 1;
    private static final int FORWARD_ID = 2;
    private static final int COPY_TEXT_ID = 3;
    private static final int DELETE_ID = 4;

    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, EDIT_ID, 0, "Edit");
        menu.add(0, REPLY_ID, 0, "Reply");
        menu.add(0, FORWARD_ID, 0, "Forward");
        menu.add(0, COPY_TEXT_ID, 0, "Copy Text");
        menu.add(0, DELETE_ID, 0,  "Delete");
    }

    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Message message = (Message) messageList.getItemAtPosition(info.position);

        switch (item.getItemId()) {
            case REPLY_ID:
                activeReply = message;
                replyPanel.setVisibility(View.VISIBLE);
                replyTo.setText("Replying to " + message.author.name);
                return true;
            case COPY_TEXT_ID:
                ClipboardManager clipboard = (ClipboardManager) ChatActivity.this.getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setText(message.content);
                Toast.makeText(ChatActivity.this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void loadInitialMessages() {
        composer.setEnabled(false);
        sendButton.setEnabled(false);
        showProgress(true);

        new Thread() {
            @Override
            public void run() {
                try {
                    final Vector result =
                            account.getMessages(channelId, null, null, MESSAGE_LOAD_COUNT);

                    runOnUiThread(new Runnable() {
                        public void run() {
                            composer.setEnabled(true);
                            sendButton.setEnabled(true);
                            if (isFinishing()) return;
                            showProgress(false);

                            messages.clear();
                            messages.addAll(result);
                            cluster();
                            adapter.notifyDataSetChanged();

                            if (messages.size() > 0) {
                                messageList.setSelection(messages.size() - 1);
                            }
                            initialLoadDone = true;
                        }
                    });
                }
                catch (final DiscordException e) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if (isFinishing()) return;
                            showProgress(false);
                            showInitialLoadError(e);
                        }
                    });
                }
            }
        }.start();
    }

    private void showInitialLoadError(final DiscordException e) {
        new AlertDialog.Builder(this)
                .setTitle(e.getTitle())
                .setMessage(e.getDisplayMessage())
                .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        loadInitialMessages();
                    }
                })
                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .show();
    }

    private void loadOlderMessages() {
        loadingOlder = true;
        showProgress(true);

        final Message first = (Message) messages.firstElement();
        final int firstVisible = messageList.getFirstVisiblePosition();
        final View firstChild = messageList.getChildAt(0);
        final int topOffset = firstChild != null ? firstChild.getTop() : 0;

        new Thread() {
            @Override
            public void run() {
                try {
                    final Vector older = account.getMessages(
                            channelId, Long.toString(first.id), null, MESSAGE_LOAD_COUNT);

                    runOnUiThread(new Runnable() {
                        public void run() {
                            loadingOlder = false;
                            if (isFinishing()) return;
                            showProgress(false);

                            if (older.size() > 0) {
                                for (int i = 0; i < older.size(); i++) {
                                    messages.insertElementAt(older.elementAt(i), 0);
                                }
                                cluster();
                                adapter.notifyDataSetChanged();

                                // Keep the previously visible rows in place
                                messageList.setSelectionFromTop(
                                        firstVisible + older.size(), topOffset);
                            }
                        }
                    });
                }
                catch (final DiscordException e) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            loadingOlder = false;
                            if (isFinishing()) return;
                            showProgress(false);
                            Toast.makeText(ChatActivity.this,
                                    "Failed to load older messages: " + e.message,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }.start();
    }

    private void sendMessage() {
        if (sending) return;

        final Context c = this;
        final String text = composer.getText().toString().trim();
        if (text.length() == 0) return;

        sending = true;
        composer.setEnabled(false);
        sendButton.setEnabled(false);
        replyPanel.setVisibility(View.GONE);
        showProgress(true);

        new Thread() {
            @Override
            public void run() {
                try {
                    final Message created = account.sendMessage(channelId, text, activeReply, pingToggle.isChecked());

                    runOnUiThread(new Runnable() {
                        public void run() {
                            sending = false;
                            composer.setEnabled(true);
                            sendButton.setEnabled(true);
                            if (isFinishing()) return;
                            showProgress(false);

                            messages.add(created);
                            cluster();
                            adapter.notifyDataSetChanged();
                            messageList.setSelection(messages.size() - 1);
                            composer.setText("");
                        }
                    });
                }
                catch (final DiscordException e) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            sending = false;
                            composer.setEnabled(true);
                            sendButton.setEnabled(true);
                            if (isFinishing()) return;
                            showProgress(false);

                            e.show(c);
                        }
                    });
                }
            }
        }.start();
    }

    /**
     * Group consecutive messages from the same author into clusters, so that
     * the author row and avatar are only shown for the first message.
     */
    private void cluster() {
        long clusterStart = 0;
        Message above = null;

        for (int i = 0; i < messages.size(); i++) {
            Message m = (Message) messages.elementAt(i);

            if (above == null || m.shouldShowAuthor(above, clusterStart)) {
                m.showAuthor = true;
                clusterStart = m.id;
            } else {
                m.showAuthor = false;
            }
            above = m;
        }
    }

    private void showProgress(final boolean show) {
        this.setProgressBarVisibility(show);
        this.setProgressBarIndeterminate(show);
    }
}
