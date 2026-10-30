package fi.gtrxac.discord;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import fi.gtrxac.bluewap.Util;
import fi.gtrxac.tinytls.TinyTLS;

/**
 * Created by gtrx on 29.8.2026.
 */

public class AccountListActivity extends Activity {
    private ListView accountList;

    public void onCreate(Bundle savedInstanceState) {
        final Context context = this;
        Theme.load(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.accounts);

        Accounts.setActive(-1);
        Settings.save(this);

        accountList = (ListView) findViewById(R.id.accountList);

        registerForContextMenu(accountList);

        // Account selected -> login to that account and go to main menu
        accountList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Accounts.setActive(position);
                Settings.save(view.getContext());

                Intent intent = new Intent(view.getContext(), MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        accountList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                return false;
            }
        });

        updateAccountList();

        Button addAccountButton = (Button) findViewById(R.id.addAccountButton);

        addAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                App.pendingAccount = new Account();
                Intent intent = new Intent(v.getContext(), ConnectionModeActivity.class);
                startActivity(intent);
            }
        });
    }

    private void updateAccountList() {
        View noAccountsText = findViewById(R.id.noAccountsText);

        if (Accounts.list.size() == 0) {
            accountList.setVisibility(View.GONE);
            noAccountsText.setVisibility(View.VISIBLE);
        }
        else {
            accountList.setVisibility(View.VISIBLE);
            noAccountsText.setVisibility(View.GONE);

            ArrayList<String> items = new ArrayList<>();

            for (int i = 0; i < Accounts.list.size(); i++) {
                Account acc = Accounts.list.elementAt(i);

                try {
                    items.add(acc.getDisplayTitle());
                }
                catch (DiscordException e) {
                    e.printStackTrace();
                    items.add("(unknown)");
                }
            }

            ArrayAdapter<String> adapter =
                    new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);

            accountList.setAdapter(adapter);
        }
    }

    private static final int MENU_TEST_TLS = 0;

    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.account_list, menu);
    }

    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId()) {
            case R.id.accountEdit:
                App.pendingAccount = Accounts.list.elementAt(info.position);
                startActivity(new Intent(this, ConnectionModeActivity.class));
                return true;
            case R.id.accountDelete:
                promptDelete(info.position);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void promptDelete(final int position) {
        final Account acc = Accounts.list.elementAt(position);

        String name;
        try {
            name = acc.getDisplayTitle();
        }
        catch (DiscordException e) {
            e.printStackTrace();
            name = "(unknown)";
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_account)
                .setMessage(getResources().getString(R.string.account_delete_warning, name))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Accounts.list.removeElement(acc);
                        Settings.save(AccountListActivity.this);
                        updateAccountList();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_TEST_TLS, 0, "Test TLS");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_TEST_TLS:
                testTLS();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void testTLS() {
        final StringBuffer log = new StringBuffer();
        Toast.makeText(this, "Testing TLS...", Toast.LENGTH_SHORT).show();

        new Thread() {
            @Override
            public void run() {
                TinyTLS tls = new TinyTLS(log);

                try {
                    tls.connect("discord.com", 8443);

                    InputStream is = tls.openInputStream();
                    OutputStream os = tls.openOutputStream();

                    byte[] req = Util.stringToBytes(
                            "GET / HTTP/1.1\r\n"
                            + "Host: discord.com\r\n"
                            + "User-Agent: TinyTLS/1.0\r\n"
                            + "Connection: close\r\n"
                            + "\r\n");

                    os.write(req);

                    log.append(Util.bytesToString(Util.readBytes(is)));
                }
                catch (Exception e) {
                    log.append("TLS FAILED: " + e);
                    e.printStackTrace();
                }
                finally {
                    tls.close();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new AlertDialog.Builder(AccountListActivity.this)
                                .setTitle("TLS test")
                                .setMessage(log.toString())
                                .setPositiveButton("Close", null)
                                .show();
                    }
                });
            }
        }.start();
    }

}
