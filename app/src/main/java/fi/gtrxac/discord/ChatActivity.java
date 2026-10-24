package fi.gtrxac.discord;

import fi.gtrxac.discord.model.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
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

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;

/**
 * Message view for a channel or DM: reading (with pagination), clustering,
 * and sending.
 */
public class ChatActivity extends Activity implements GatewayListener {
    public static final int MESSAGE_LOAD_COUNT = 25;

    private Account account;
    private long channelId;
    private String channelName;

    private Vector messages;
    private MessageListAdapter adapter;
    private ListView messageList;

    private View replyPanel;
    private TextView replyTo;
    private ToggleButton pingToggle;
    private ImageButton cancelReply;

    private EditText composer;
    private Button sendButton;

    private TextView typingIndicator;
    private final Vector typingUsers = new Vector();
    private final Handler typingHandler = new Handler();

    private boolean initialLoadDone;
    private boolean loadingOlder;
    private boolean sending;
    private boolean atBottom = true;
    private long lastTypingSent;

    private Message activeReply;
    private Message activeEdit;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.chat);

        account = Accounts.getActive();

        if (account == null) {
            finish();
            return;
        }

        messages = new Vector();
        messageList = (ListView) findViewById(R.id.messageList);

        replyPanel = (View) findViewById(R.id.reply_panel);
        replyTo = (TextView) findViewById(R.id.reply_to);
        pingToggle = (ToggleButton) findViewById(R.id.ping_toggle);
        cancelReply = (ImageButton) findViewById(R.id.cancel_reply);

        composer = (EditText) findViewById(R.id.messageComposer);
        sendButton = (Button) findViewById(R.id.sendButton);
        typingIndicator = (TextView) findViewById(R.id.typingIndicator);

        registerForContextMenu(messageList);

        channelId = getIntent().getLongExtra("channelId", 0);
        channelName = getIntent().getStringExtra("channelName");
        if (channelName != null) {
            setTitle(channelName);
            composer.setHint(getResources().getString(R.string.msg_composer_hint, channelName));
        }

        adapter = new MessageListAdapter(this, messages, account);
        messageList.setAdapter(adapter);

        sendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (activeEdit != null) {
                    editMessage();
                } else {
                    sendMessage();
                }
            }
        });

        composer.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            public void afterTextChanged(Editable s) {
                if (s.length() == 0) return;

                long now = System.currentTimeMillis();
                if (now - lastTypingSent < 10000) return;

                lastTypingSent = now;
                account.sendTyping(channelId);
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
                if (activeEdit != null) {
                    composer.setText("");
                }
                activeReply = null;
                activeEdit = null;
                replyPanel.setVisibility(View.GONE);
            }
        });

        messageList.setOnScrollListener(new AbsListView.OnScrollListener() {
            public void onScrollStateChanged(AbsListView view, int scrollState) {}

            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                                 int totalItemCount) {
                atBottom = totalItemCount == 0
                        || firstVisibleItem + visibleItemCount >= totalItemCount;

                if (!initialLoadDone || loadingOlder || sending) return;

                // Near the top -> fetch the previous page of history
                if (firstVisibleItem == 0 && messages.size() >= MESSAGE_LOAD_COUNT) {
                    loadOlderMessages();
                }
            }
        });

        loadInitialMessages();
    }

    @Override
    protected void onResume() {
        super.onResume();
        GatewayThread.addListener(this);
    }

    @Override
    protected void onPause() {
        GatewayThread.removeListener(this);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        GatewayThread.removeListener(this);
        typingHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    public void onGatewayConnected() {
    }

    public void onGatewayDisconnected(final String reason) {
    }

    public void onGatewayReady(JSONObject data) {
    }

    public void onMessageCreate(JSONObject data) {
        final Message message = new Message(data);
        if (message.channelId != channelId) return;

        runOnUiThread(new Runnable() {
            public void run() {
                if (isFinishing()) return;

                // The REST response may already have added our own message.
                for (int i = 0; i < messages.size(); i++) {
                    if (((Message) messages.elementAt(i)).id == message.id) return;
                }

                boolean scrollToBottom = atBottom || messages.size() == 0;
                messages.addElement(message);
                cluster();
                adapter.notifyDataSetChanged();

                if (scrollToBottom) {
                    messageList.setSelection(messages.size() - 1);
                }

                removeTypingUser(message.author.name);
            }
        });
    }

    public void onMessageUpdate(JSONObject data) {
        if (Long.parseLong(data.getString("channel_id", null)) != channelId) return;
        final long messageId = Long.parseLong(data.getString("id"));

        final String newContent = data.getString("content", null);
        final String editedTimestamp = data.getString("edited_timestamp", null);

        runOnUiThread(new Runnable() {
            public void run() {
                if (isFinishing()) return;

                for (int i = 0; i < messages.size(); i++) {
                    Message message = (Message) messages.elementAt(i);
                    if (message.id != messageId) continue;

                    if (newContent != null) {
                        message.content = newContent;
                        if (editedTimestamp != null) {
                            message.editedTimestampIso = editedTimestamp;
                        } else if (message.editedTimestampIso == null) {
                            message.editedTimestampIso = "";
                        }
                    }

                    cluster();
                    adapter.notifyDataSetChanged();
                    return;
                }
            }
        });
    }

    public void onMessageDelete(JSONObject data) {
        if (Long.parseLong(data.getString("channel_id", null)) != channelId) return;
        final long messageId = Long.parseLong(data.getString("id"));

        runOnUiThread(new Runnable() {
            public void run() {
                if (isFinishing()) return;

                for (int i = 0; i < messages.size(); i++) {
                    if (((Message) messages.elementAt(i)).id != messageId) continue;

                    messages.removeElementAt(i);
                    cluster();
                    adapter.notifyDataSetChanged();
                    return;
                }
            }
        });
    }

    public void onTypingStart(JSONObject data) {
        if (Long.parseLong(data.getString("channel_id", null)) != channelId) return;

        String name = null;

        try {
            JSONObject user = data.getObject("member").getObject("user");
            if (Long.parseLong(user.getString("id")) == account.getUserID()) return;
            name = user.getString("global_name", null);
            if (name == null) name = user.getString("username", null);
        }
        catch (Exception e) {}

        if (name == null) name = channelName;
        if (name == null || name.length() == 0) return;

        final String typingName = name;
        runOnUiThread(new Runnable() {
            public void run() {
                if (isFinishing()) return;
                addTypingUser(typingName);
            }
        });
    }

    public void onGuildMembersChunk(JSONObject data) {
    }

    public void onPresenceUpdate(JSONObject data) {
    }

    public void onReadStates(JSONArray entries) {
    }

    private void addTypingUser(final String name) {
        if (name == null || name.length() == 0) return;

        if (!typingUsers.contains(name)) {
            typingUsers.addElement(name);
        }
        updateTypingIndicator();

        typingHandler.postDelayed(new Runnable() {
            public void run() {
                typingUsers.removeElement(name);
                updateTypingIndicator();
            }
        }, 10000);
    }

    private void removeTypingUser(String name) {
        if (typingUsers.removeElement(name)) {
            updateTypingIndicator();
        }
    }

    private void updateTypingIndicator() {
        if (typingIndicator == null) return;

        if (typingUsers.size() == 0) {
            typingIndicator.setVisibility(View.GONE);
            return;
        }

        String text;
        if (typingUsers.size() == 1) {
            text = getString(R.string.typing_single, typingUsers.elementAt(0));
        } else if (typingUsers.size() == 2) {
            text = getString(R.string.typing_two,
                    typingUsers.elementAt(0), typingUsers.elementAt(1));
        } else {
            text = getString(R.string.typing_multiple);
        }

        typingIndicator.setText(text);
        typingIndicator.setVisibility(View.VISIBLE);
    }

    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.message, menu);

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Message message = (Message) messageList.getItemAtPosition(info.position);
        if (message.isForwarded) menu.findItem(R.id.messageEdit).setVisible(false);
        try {
            if (message.author.id != account.getUserID()) {
                menu.findItem(R.id.messageEdit).setVisible(false);
                menu.findItem(R.id.messageDelete).setVisible(false); // TODO: user permissions
            }
        } catch (Exception e) {}
    }

    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        final Message message = (Message) messageList.getItemAtPosition(info.position);

        switch (item.getItemId()) {
            case R.id.messageReply:
                activeReply = message;
                activeEdit = null;
                replyPanel.setVisibility(View.VISIBLE);
                pingToggle.setVisibility(View.VISIBLE);
                replyTo.setText(Html.fromHtml(getResources().getString(R.string.reply_to, message.author.name)));
                return true;
            case R.id.messageEdit:
                activeReply = null;
                activeEdit = message;
                replyPanel.setVisibility(View.VISIBLE);
                pingToggle.setVisibility(View.GONE);
                replyTo.setText(R.string.editing_message);
                composer.setText(message.content);
                return true;
            case R.id.messageCopy:
                ClipboardManager clipboard = (ClipboardManager) ChatActivity.this.getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setText(message.content);
                Toast.makeText(ChatActivity.this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.messageDelete:
                new AlertDialog.Builder(this)
                        .setTitle(R.string.delete_message)
                        .setMessage(R.string.message_delete_warning)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                showProgress(true);
                                new Thread() {
                                    @Override
                                    public void run() {
                                        try {
                                            account.deleteMessage(message);
                                            runOnUiThread(new Runnable() {
                                                public void run() {
                                                    if (isFinishing()) return;
                                                    showProgress(false);

                                                    if (messages.contains(message)) {
                                                        messages.removeElement(message);
                                                        cluster();
                                                        adapter.notifyDataSetChanged();
                                                    }
                                                }
                                            });
                                        }
                                        catch (final DiscordException e) {
                                            runOnUiThread(new Runnable() {
                                                public void run() {
                                                    if (isFinishing()) return;
                                                    showProgress(false);
                                                    Toast.makeText(ChatActivity.this,
                                                            "Failed to delete message: " + e.message,
                                                            Toast.LENGTH_LONG).show();
                                                }
                                            });
                                        }
                                    }
                                }.start();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .show();
                return true;
            default:
                Toast.makeText(ChatActivity.this, "Unimplemented: " + item.getTitle(), Toast.LENGTH_LONG).show();
                return false;
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
                            account.getMessages(channelId, 0, 0, MESSAGE_LOAD_COUNT);

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
                .setPositiveButton(R.string.action_retry, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        loadInitialMessages();
                    }
                })
                .setNegativeButton(R.string.action_close, new DialogInterface.OnClickListener() {
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
                            channelId, first.id, 0, MESSAGE_LOAD_COUNT);

                    runOnUiThread(new Runnable() {
                        public void run() {
                            loadingOlder = false;
                            if (isFinishing()) return;
                            showProgress(false);

                            if (older.size() > 0) {
                                for (int i = older.size() - 1; i >= 0; i--) {
                                    messages.insertElementAt(older.elementAt(i), 0);
                                }
                                cluster();
                                adapter.notifyDataSetChanged();

                                // Keep the previously visible rows in place
                                messageList.setSelectionFromTop(
                                        firstVisible + older.size() - 1, topOffset);
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
                            activeReply = null;
                            activeEdit = null;
                            composer.setEnabled(true);
                            sendButton.setEnabled(true);
                            if (isFinishing()) return;
                            showProgress(false);

                            boolean exists = false;
                            for (int i = 0; i < messages.size(); i++) {
                                if (((Message) messages.elementAt(i)).id == created.id) {
                                    exists = true;
                                    break;
                                }
                            }
                            if (!exists) {
                                messages.add(created);
                            }
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

    private void editMessage() {
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
                    final Message edited = account.editMessage(activeEdit, text);

                    runOnUiThread(new Runnable() {
                        public void run() {
                            sending = false;
                            activeReply = null;
                            activeEdit = null;
                            composer.setEnabled(true);
                            sendButton.setEnabled(true);
                            if (isFinishing()) return;
                            showProgress(false);

                            for (int i = 0; i < messages.size(); i++) {
                                Message message = (Message) messages.elementAt(i);
                                if (message.id == edited.id) {
                                    message = edited;
                                }
                            }
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
