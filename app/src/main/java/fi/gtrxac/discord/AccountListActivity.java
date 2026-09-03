package fi.gtrxac.discord;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * Created by gtrx on 29.8.2026.
 */

public class AccountListActivity extends Activity {
    private ListView accountList;

    public void onCreate(Bundle savedInstanceState) {
        final Context context = this;
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
                items.add(acc.getDisplayTitle());
            }

            ArrayAdapter<String> adapter =
                    new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);

            accountList.setAdapter(adapter);
        }
    }

    private static final int EDIT_ID = 0;
    private static final int DELETE_ID = 1;

    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, EDIT_ID, 0, "Edit");
        menu.add(0, DELETE_ID, 0,  "Delete");
    }

    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case EDIT_ID:
                //editNote(info.id);
                return true;
            case DELETE_ID:
                //deleteNote(info.id);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

}
