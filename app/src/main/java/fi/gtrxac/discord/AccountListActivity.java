package fi.gtrxac.discord;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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

    public void onCreate(Bundle savedInstanceState) {
        final Context context = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.accounts);

        updateAccountList();

        Button addAccountButton = (Button) findViewById(R.id.addAccountButton);

        addAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), LoginActivity.class);
                startActivity(intent);
            }
        });
    }

    private void updateAccountList() {
        ListView accountList = (ListView) findViewById(R.id.accountList);
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

        accountList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            }
        });
    }
}
