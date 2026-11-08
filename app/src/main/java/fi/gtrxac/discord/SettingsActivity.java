package fi.gtrxac.discord;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity
    implements SharedPreferences.OnSharedPreferenceChangeListener {

    public void onCreate(Bundle savedInstanceState) {
        Theme.load(this);
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        addPreferencesFromResource(R.xml.preferences);

        updateListPreferenceSummary("theme");
        updateListPreferenceSummary("fontSize");
        updateListPreferenceSummary("avatarShape");
        updateListPreferenceSummary("timeFormat");
        updateListPreferenceSummary("useDirectCdn");

        EditTextPreference messageLoadCount =
            (EditTextPreference) findPreference("messageLoadCount");
        messageLoadCount
            .setSummary(getString(R.string.message_load_count_summary, messageLoadCount.getText()));
        if (messageLoadCount != null) {
            messageLoadCount
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        int value = Integer.parseInt(newValue.toString().trim());
                        if (value < Settings.MESSAGE_LOAD_COUNT_MIN ||
                            value > Settings.MESSAGE_LOAD_COUNT_MAX) {
                            Toast
                                .makeText(SettingsActivity.this, R.string.message_load_count_dialog,
                                    Toast.LENGTH_LONG).show();
                            return false;
                        }
                        return true;
                    }
                });
        }
    }


    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(this);
    }

    protected void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        EditTextPreference messageLoadCount =
            (EditTextPreference) findPreference("messageLoadCount");
        messageLoadCount
            .setSummary(getString(R.string.message_load_count_summary, messageLoadCount.getText()));

        updateListPreferenceSummary(key);

        Settings.lightTheme = prefs.getString("theme", "dark").equals("light");
        Settings.fontSize =
            Theme.FontSize.valueOf(prefs.getString("fontSize", "MEDIUM").toUpperCase());
        Settings.squareAvatars = prefs.getString("avatarShape", "circle").equals("square");
        Settings.use12hTime = prefs.getString("timeFormat", "24").equals("12");
        Settings.messageLoadCount = Integer.valueOf(prefs
            .getString("messageLoadCount", Integer.toString(Settings.MESSAGE_LOAD_COUNT_DEFAULT)));
        Settings.sendTyping = prefs.getBoolean("sendTyping", true);
        Settings.autoReconnect = prefs.getBoolean("autoReconnect", true);
        Settings.showReplies = prefs.getBoolean("showReplies", true);
        Settings.useMarkdown = prefs.getBoolean("textFormatting", true);
        Settings.showImages = prefs.getBoolean("showImages", true);
        Settings.compactMode = prefs.getBoolean("compactMode", false);
        Settings.useNameColors = prefs.getBoolean("useNameColors", true);
        Settings.useDirectCdn = prefs.getBoolean("useDirectCdn", true);

        if (key.equals("theme") || key.equals("fontSize")) {
            getListView().post(new Runnable() {
                public void run() {
                    Theme.checkAppearanceChanged(SettingsActivity.this);
                }
            });
        }
    }

    private void updateListPreferenceSummary(String key) {
        Preference preference = findPreference(key);
        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            CharSequence entry = listPreference.getEntry();
            if (entry == null) {
                entry = listPreference.getValue();
            }
            listPreference.setSummary(entry);
        }
    }
}
