package fi.gtrxac.discord;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
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
import fi.gtrxac.discord.model.Channel;
import fi.gtrxac.discord.model.Guild;
import fi.gtrxac.discord.model.GuildMember;
import fi.gtrxac.discord.model.Message;
import fi.gtrxac.discord.model.Permissions;

/**
 * Message view for a channel or DM: reading (with pagination), clustering,
 * and sending.
 */
public class ChatActivity extends Activity implements GatewayListener {
    private static final String TAG = "Chat";

    private final Vector typingUsers = new Vector();
    private final Handler typingHandler = new Handler();
    private Account account;
    private long guildId;
    private long channelId;
    private String channelName;
    private Vector messages;
    private Vector guildRoles;
    private MessageListAdapter adapter;
    private ListView messageList;
    private View replyPanel;
    private TextView replyTo;
    private ToggleButton pingToggle;
    private ImageButton cancelReply;
    private EditText composer;
    private Button sendButton;
    private TextView typingIndicator;
    private boolean initialLoadDone;
    private boolean loadingOlder;
    private boolean loadingNewer;
    private boolean noMoreOlder;
    private boolean haveNewest = true;
    private boolean sending;
    private boolean atBottom = true;
    private long lastTypingSent;
    private long myPermissions = -1;

    private Message activeReply;
    private Message activeEdit;

    public void onCreate(Bundle savedInstanceState) {
        Theme.load(this);
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.activity_chat);

        account = Accounts.getActive();

        if (account == null) {
            Log.w(TAG, "No active account, finishing");
            finish();
            return;
        }

        account.guildMemberCache.activeRequest = false;

        messages = new Vector();
        messageList = (ListView) findViewById(R.id.messageList);

        replyPanel = (View) findViewById(R.id.replyPanel);
        replyTo = (TextView) findViewById(R.id.replyTo);
        pingToggle = (ToggleButton) findViewById(R.id.pingToggle);
        cancelReply = (ImageButton) findViewById(R.id.cancelReply);

        composer = (EditText) findViewById(R.id.messageComposer);
        sendButton = (Button) findViewById(R.id.sendButton);
        typingIndicator = (TextView) findViewById(R.id.typingIndicator);

        registerForContextMenu(messageList);

        guildId = getIntent().getLongExtra("guildId", 0);
        channelId = getIntent().getLongExtra("channelId", 0);
        channelName = getIntent().getStringExtra("channelName");
        Log.d(TAG, "Opening channel " + channelId + " in guild " + guildId +
            (channelName != null ? " (" + channelName + ")" : ""));
        if (channelName != null) {
            setTitle(channelName);
            composer.setHint(getResources().getString(R.string.msg_composer_hint, channelName));
        }

        sendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (activeEdit != null) {
                    editMessage();
                }
                else {
                    sendMessage();
                }
            }
        });

        composer.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            public void afterTextChanged(Editable s) {
                if (!canSend()) return;
                if (!Settings.sendTyping || s.length() == 0) return;

                long now = System.currentTimeMillis();
                if (now - lastTypingSent < 10000) return;

                lastTypingSent = now;
                new Thread() {
                    @Override
                    public void run() {
                        account.sendTyping(channelId);
                    }
                }.start();
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
                updateComposerState();
            }
        });

        messageList.setOnScrollListener(new AbsListView.OnScrollListener() {
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                int totalItemCount) {
                atBottom =
                    totalItemCount == 0 || firstVisibleItem + visibleItemCount >= totalItemCount;

                if (!initialLoadDone || loadingOlder || loadingNewer || sending) return;

                // At the top -> fetch the previous page of history.
                if (firstVisibleItem == 0 && !noMoreOlder && messages.size() > 0) {
                    loadOlderMessages();
                }
                // At the bottom, but the newest messages are not loaded -> fetch the next page
                else if (atBottom && !haveNewest) {
                    loadNewerMessages();
                }
            }
        });

        long messageId = getIntent().getLongExtra("messageId", 0);
        if (messageId != 0) {
            loadAroundMessage(messageId, true);
        }
        else {
            loadInitialMessages();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Theme.checkAppearanceChanged(this)) return;
        if (messages == null) return;

        GatewayThread.addListener(this);
        Notifications.setOpenChannel(channelId);
        Notifications.clear(channelId);
        account.unreadManager.clearMentions(channelId);

        // Settings may have changed while this activity was in the background.
        for (int i = 0; i < messages.size(); i++) {
            ((Message) messages.elementAt(i)).refreshTimestamp();
        }
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    @Override
    protected void onPause() {
        GatewayThread.removeListener(this);
        Notifications.clearOpenChannel(channelId);

        if (account != null) {
            account.unreadManager.save();
            Settings.save(this);
        }

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
        account.guildMemberCache.activeRequest = false;
    }

    public void onMessageCreate(JSONObject data) {
        Log.d(TAG, "Gateway MESSAGE_CREATE in channel " + data.getString("channel_id", "?") +
            " (message " + data.getString("id", "?") + ")");

        final Message message = new Message(data);
        GuildMember author = account.guildMemberCache.get(guildId, message.author, messages);
        final String name = author.nickname != null ? author.nickname : author.name;
        if (message.channelId != channelId) return;

        account.unreadManager.markRead(channelId, message.id);

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
                if (adapter != null) adapter.notifyDataSetChanged();

                if (scrollToBottom) {
                    messageList.setSelection(messages.size() - 1);
                }

                removeTypingUser(name);
            }
        });
    }

    public void onMessageUpdate(JSONObject data) {
        if (Long.parseLong(data.getString("channel_id", null)) != channelId) return;
        final long messageId = Long.parseLong(data.getString("id"));
        Log.d(TAG, "Gateway MESSAGE_UPDATE for message " + messageId);

        final String newContent = data.getString("content", null);

        runOnUiThread(new Runnable() {
            public void run() {
                if (isFinishing()) return;

                for (int i = 0; i < messages.size(); i++) {
                    Message message = (Message) messages.elementAt(i);
                    if (message.id != messageId) continue;

                    message.isEdited = true;

                    if (newContent != null) {
                        message.content = newContent;
                    }

                    cluster();
                    if (adapter != null) adapter.notifyDataSetChanged();
                    return;
                }
            }
        });
    }

    public void onMessageDelete(JSONObject data) {
        if (Long.parseLong(data.getString("channel_id", null)) != channelId) return;
        final long messageId = Long.parseLong(data.getString("id"));
        Log.d(TAG, "Gateway MESSAGE_DELETE for message " + messageId);

        runOnUiThread(new Runnable() {
            public void run() {
                if (isFinishing()) return;

                for (int i = 0; i < messages.size(); i++) {
                    if (((Message) messages.elementAt(i)).id != messageId) continue;

                    messages.removeElementAt(i);
                    cluster();
                    if (adapter != null) adapter.notifyDataSetChanged();
                    return;
                }
            }
        });
    }

    public void onMessageAck(JSONObject data) {
    }

    public void onTypingStart(JSONObject data) {
        if (Long.parseLong(data.getString("channel_id", null)) != channelId) return;

        String name = null;

        try {
            GuildMember member = new GuildMember(data.getObject("member"), guildRoles);
            if (member.id == account.getUserID()) return;
            name = member.nickname != null ? member.nickname : member.name;
        }
        catch (Exception e) {
            Log.w(TAG, "Failed to read member data, ignoring", e);
        }

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
        account.guildMemberCache.activeRequest = false;

        if (guildId == 0 || guildRoles == null) return;

        final JSONArray members = data.getArray("members");

        final long gatewayGuildId = Long.parseLong(data.getString("guild_id"));
        if (gatewayGuildId != guildId) return;

        final JSONArray notFound = data.getArray("not_found", new JSONArray());
        Log.d(TAG, "Gateway GUILD_MEMBERS_CHUNK for guild " + gatewayGuildId + ": " +
            members.size() + " members, " + notFound.size() + " not found");

        runOnUiThread(new Runnable() {
            public void run() {
                if (isFinishing()) return;

                for (int i = 0; i < notFound.size(); i++) {
                    long id = Long.parseLong(notFound.getString(i));
                    account.guildMemberCache.set(id + gatewayGuildId, null);
                }

                for (int i = 0; i < members.size(); i++) {
                    JSONObject member = members.getObject(i);
                    long id = Long.parseLong(member.getObject("user").getString("id"));
                    account.guildMemberCache.set(id + gatewayGuildId,
                        new GuildMember(member, guildRoles));
                }

                if (adapter != null) adapter.notifyDataSetChanged();
            }
        });
    }

    public void onPresenceUpdate(JSONObject data) {
    }

    public void onChannelUnread(JSONObject data) {
    }

    public void onPassiveUpdateV1(JSONObject data) {
    }

    public void onPassiveUpdateV2(JSONObject data) {
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
        }
        else if (typingUsers.size() == 2) {
            text =
                getString(R.string.typing_two, typingUsers.elementAt(0), typingUsers.elementAt(1));
        }
        else {
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
                menu.findItem(R.id.messageDelete).setVisible(myPermissions != -1 &&
                    Permissions.has(myPermissions, Permissions.MANAGE_MESSAGES));
            }
        }
        catch (Exception e) {
            Log.w(TAG, "Failed to adjust message context menu, ignoring", e);
        }
    }

    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info =
            (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        final Message message = (Message) messageList.getItemAtPosition(info.position);
        GuildMember author = account.guildMemberCache.get(guildId, message.author, messages);
        final String name = author.nickname != null ? author.nickname : author.name;

        switch (item.getItemId()) {
            case R.id.messageReply:
                if (!canSend()) {
                    Toast.makeText(ChatActivity.this, R.string.no_send_permission,
                        Toast.LENGTH_SHORT).show();
                    return true;
                }
                activeReply = message;
                activeEdit = null;
                replyPanel.setVisibility(View.VISIBLE);
                pingToggle.setVisibility(View.VISIBLE);
                replyTo.setText(Html.fromHtml(getResources().getString(R.string.reply_to, name)));
                return true;
            case R.id.messageEdit:
                activeReply = null;
                activeEdit = message;
                replyPanel.setVisibility(View.VISIBLE);
                pingToggle.setVisibility(View.GONE);
                replyTo.setText(R.string.editing_message);
                composer.setText(message.content);
                updateComposerState();
                return true;
            case R.id.messageCopy:
                ClipboardManager clipboard = (ClipboardManager) ChatActivity.this.getSystemService(
                    Context.CLIPBOARD_SERVICE);
                clipboard.setText(message.content);
                Toast.makeText(ChatActivity.this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.messageDelete:
                new AlertDialog.Builder(this).setTitle(R.string.delete_message)
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
                                        Log.e(TAG, "Failed to delete message " + message.id, e);
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
                    }).setNegativeButton(android.R.string.no, null).show();
                return true;
            default:
                Toast.makeText(ChatActivity.this, "Unimplemented: " + item.getTitle(),
                    Toast.LENGTH_LONG).show();
                return false;
        }
    }

    private void loadInitialMessages() {
        Log.d(TAG, "Loading initial messages for channel " + channelId);
        composer.setEnabled(false);
        sendButton.setEnabled(false);
        showProgress(true);

        new Thread() {
            @Override
            public void run() {
                try {
                    final Vector result =
                        account.getMessages(channelId, 0, 0, 0, Settings.messageLoadCount);

                    if (guildId != 0) {
                        guildRoles = account.getGuildRoles(guildId);
                        myPermissions = fetchMyPermissions();
                    }

                    runOnUiThread(new Runnable() {
                        public void run() {
                            if (isFinishing()) return;
                            showProgress(false);

                            messages.clear();
                            messages.addAll(result);
                            cluster();

                            adapter = new MessageListAdapter(ChatActivity.this, messages, account,
                                guildId, guildRoles, new MessageListAdapter.OnReplyClickListener() {
                                public void onReplyClick(Message reply) {
                                    jumpToMessage(reply.id);
                                }
                            });
                            messageList.setAdapter(adapter);

                            if (messages.size() > 0) {
                                messageList.setSelection(messages.size() - 1);
                            }
                            initialLoadDone = true;

                            updateComposerState();
                        }
                    });
                }
                catch (final DiscordException e) {
                    Log.e(TAG, "Failed to load initial messages for channel " + channelId, e);
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
        new AlertDialog.Builder(this).setTitle(e.getTitle()).setMessage(e.getDisplayMessage())
            .setPositiveButton(R.string.action_retry, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    loadInitialMessages();
                }
            }).setNegativeButton(R.string.action_close, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            }).show();
    }

    private void loadOlderMessages() {
        Log.d(TAG, "Loading older messages before " + ((Message) messages.firstElement()).id);
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
                    final Vector older =
                        account.getMessages(channelId, first.id, 0, 0, Settings.messageLoadCount);

                    runOnUiThread(new Runnable() {
                        public void run() {
                            loadingOlder = false;
                            if (isFinishing()) return;
                            showProgress(false);

                            if (older.size() > 0) {
                                // A response shorter than the limit means that
                                // the start of the channel history was reached
                                if (older.size() < Settings.messageLoadCount) noMoreOlder = true;

                                for (int i = older.size() - 1; i >= 0; i--) {
                                    messages.insertElementAt(older.elementAt(i), 0);
                                }
                                cluster();
                                adapter.notifyDataSetChanged();

                                // Keep the previously visible rows in place
                                messageList.setSelectionFromTop(firstVisible + older.size() - 1,
                                    topOffset);
                            }
                            else {
                                // Start of channel history reached
                                noMoreOlder = true;
                            }
                        }
                    });
                }
                catch (final DiscordException e) {
                    Log.e(TAG, "Failed to load older messages for channel " + channelId, e);
                    runOnUiThread(new Runnable() {
                        public void run() {
                            loadingOlder = false;
                            if (isFinishing()) return;
                            showProgress(false);
                            Toast.makeText(ChatActivity.this,
                                    "Failed to load older messages: " + e.message,
                                    Toast.LENGTH_LONG)
                                .show();
                        }
                    });
                }
            }
        }.start();
    }

    public void jumpToMessage(long messageId) {
        if (messageId == 0) return;

        for (int i = 0; i < messages.size(); i++) {
            if (((Message) messages.elementAt(i)).id == messageId) {
                messageList.setSelection(i);
                return;
            }
        }

        loadAroundMessage(messageId, false);
    }

    private void loadAroundMessage(final long messageId, final boolean initial) {
        Log.d(TAG, "Loading messages around " + messageId + " (initial=" + initial + ")");
        if (initial) {
            composer.setEnabled(false);
            sendButton.setEnabled(false);
        }
        showProgress(true);

        new Thread() {
            @Override
            public void run() {
                try {
                    final Vector result =
                        account.getMessages(channelId, 0, 0, messageId, Settings.messageLoadCount);

                    if (initial && guildId != 0 && guildRoles == null) {
                        try {
                            guildRoles = account.getGuildRoles(guildId);
                        }
                        catch (Exception e) {
                            // Role colors fall back to defaults if roles
                            // can't be fetched
                            Log.w(TAG, "Failed to get guild roles for guild " + guildId, e);
                        }

                        myPermissions = fetchMyPermissions();
                    }

                    runOnUiThread(new Runnable() {
                        public void run() {
                            if (initial) updateComposerState();
                            if (isFinishing()) return;
                            showProgress(false);

                            messages.clear();
                            messages.addAll(result);
                            cluster();

                            adapter = new MessageListAdapter(ChatActivity.this, messages, account,
                                guildId, guildRoles, new MessageListAdapter.OnReplyClickListener() {
                                public void onReplyClick(Message reply) {
                                    jumpToMessage(reply.id);
                                }
                            });
                            messageList.setAdapter(adapter);

                            int olderCount = 0;
                            int newerCount = 0;
                            for (int i = 0; i < result.size(); i++) {
                                long id = ((Message) result.elementAt(i)).id;
                                if (id < messageId) olderCount++;
                                if (id > messageId) newerCount++;
                            }
                            noMoreOlder = olderCount < Settings.messageLoadCount / 2;
                            haveNewest = newerCount < Settings.messageLoadCount / 2;

                            for (int i = 0; i < messages.size(); i++) {
                                if (((Message) messages.elementAt(i)).id == messageId) {
                                    messageList.setSelection(i);
                                    initialLoadDone = true;
                                    return;
                                }
                            }

                            // Anchor message was deleted (or can't be viewed):
                            // show the middle of the fetched page instead
                            messageList.setSelection(messages.size() / 2);
                            initialLoadDone = true;
                        }
                    });
                }
                catch (final DiscordException e) {
                    Log.e(TAG, "Failed to load messages around " + messageId, e);
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if (isFinishing()) return;
                            showProgress(false);
                            if (initial) {
                                showInitialLoadError(e);
                            }
                            else {
                                Toast.makeText(ChatActivity.this,
                                        "Failed to load message: " + e.message, Toast.LENGTH_LONG)
                                    .show();
                            }
                        }
                    });
                }
            }
        }.start();
    }

    private void loadNewerMessages() {
        Log.d(TAG, "Loading newer messages after " + ((Message) messages.lastElement()).id);
        loadingNewer = true;
        showProgress(true);

        final Message last = (Message) messages.lastElement();

        new Thread() {
            @Override
            public void run() {
                try {
                    final Vector newer =
                        account.getMessages(channelId, 0, last.id, 0, Settings.messageLoadCount);

                    runOnUiThread(new Runnable() {
                        public void run() {
                            loadingNewer = false;
                            if (isFinishing()) return;
                            showProgress(false);

                            if (newer.size() == 0) {
                                haveNewest = true;
                                return;
                            }
                            if (newer.size() < Settings.messageLoadCount) haveNewest = true;

                            // The gateway may already have added some of these
                            for (int i = 0; i < newer.size(); i++) {
                                Message message = (Message) newer.elementAt(i);
                                boolean exists = false;
                                for (int j = 0; j < messages.size(); j++) {
                                    if (((Message) messages.elementAt(j)).id == message.id) {
                                        exists = true;
                                        break;
                                    }
                                }
                                if (!exists) messages.addElement(message);
                            }
                            cluster();
                            adapter.notifyDataSetChanged();
                            // The previously visible rows stay in place
                        }
                    });
                }
                catch (final DiscordException e) {
                    Log.e(TAG, "Failed to load newer messages for channel " + channelId, e);
                    runOnUiThread(new Runnable() {
                        public void run() {
                            loadingNewer = false;
                            if (isFinishing()) return;
                            showProgress(false);
                            Toast.makeText(ChatActivity.this,
                                    "Failed to load newer messages: " + e.message,
                                    Toast.LENGTH_LONG)
                                .show();
                        }
                    });
                }
            }
        }.start();
    }

    /**
     * Whether the current user can send messages in this channel.
     */
    private boolean canSend() {
        return myPermissions == -1 || Permissions.has(myPermissions, Permissions.SEND_MESSAGES);
    }

    /**
     * Calculate the current user's permissions in this channel. Returns -1 if
     * permissions could not be determined.
     */
    private long fetchMyPermissions() {
        Channel channel = account.channelCache.getChannelByIDInGuild(guildId, channelId);

        try {
            Guild guild = account.getGuild(guildId);
            if (guild != null && guild.permissions != 0) {
                return channel.computePermissions(guildId, guild.permissions, account.userId);
            }
        }
        catch (Exception e) {
            Log.w(TAG, "Failed to calculate permissions for channel " + channelId +
                " in guild " + guildId, e);
        }

        return -1;
    }

    /**
     * Enable or disable the message composer based on the user's permissions
     * and whether a message is currently being sent or edited.
     */
    private void updateComposerState() {
        boolean usable = (canSend() || activeEdit != null) && !sending;

        composer.setEnabled(usable);
        sendButton.setEnabled(usable);

        if (!canSend() && activeEdit == null) {
            composer.setHint(R.string.no_send_permission);
        }
    }

    private void sendMessage() {
        if (sending) return;
        if (!canSend()) return;

        final Context c = this;
        final String text = composer.getText().toString().trim();
        if (text.length() == 0) return;

        Log.d(TAG, "Sending message to channel " + channelId + " (" + text.length() +
            " chars, reply=" + (activeReply != null) + ")");

        sending = true;
        updateComposerState();
        replyPanel.setVisibility(View.GONE);
        showProgress(true);

        new Thread() {
            @Override
            public void run() {
                try {
                    final Message created =
                        account.sendMessage(channelId, text, activeReply, pingToggle.isChecked());
                    runOnUiThread(new Runnable() {
                        public void run() {
                            sending = false;
                            activeReply = null;
                            activeEdit = null;
                            updateComposerState();
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
                    Log.e(TAG, "Failed to send message to channel " + channelId, e);
                    runOnUiThread(new Runnable() {
                        public void run() {
                            sending = false;
                            updateComposerState();
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

        Log.d(TAG, "Editing message " + activeEdit.id + " (" + text.length() + " chars)");

        sending = true;
        updateComposerState();
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
                            updateComposerState();
                            if (isFinishing()) return;
                            showProgress(false);

                            for (int i = 0; i < messages.size(); i++) {
                                Message message = (Message) messages.elementAt(i);
                                if (message.id == edited.id) {
                                    messages.setElementAt(edited, i);
                                    break;
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
                    Log.e(TAG, "Failed to edit message " + activeEdit.id, e);
                    runOnUiThread(new Runnable() {
                        public void run() {
                            sending = false;
                            updateComposerState();
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
            }
            else {
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
