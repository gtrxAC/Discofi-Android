package fi.gtrxac.discord;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by Leap on 27.10.2026.
 */

public class FilePickerActivity extends Activity {
	private File currentDirectory;
	private ListView filePicker;
	private ArrayList<File> entries;
	private ArrayAdapter<String> adapter;

	public void onCreate(Bundle savedInstanceState) {
		Theme.load(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_file_picker);

		filePicker = (ListView) findViewById(R.id.filePicker);

		entries = new ArrayList<File>();
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<String>());
		filePicker.setAdapter(adapter);

		filePicker.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				File selected = entries.get(position);

				if (selected.isDirectory()) {
					browse(selected);
				} else if (selected.isFile()) {
					Intent intent = new Intent();
					intent.setData(Uri.fromFile(selected));
					setResult(Activity.RESULT_OK, intent);
					finish();
				}
			}
		});

		browse(Environment.getExternalStorageDirectory());
	}

	private void browse(File directory) {
		if (directory == null || !directory.isDirectory() || !directory.canRead()) {
			Toast.makeText(this, "Cannot read this directory", Toast.LENGTH_SHORT).show();
			return;
		}

		File[] files = directory.listFiles();

		if (files == null) {
			Toast.makeText(this, "Unable to list directory contents", Toast.LENGTH_SHORT).show();
			return;
		}

		currentDirectory = directory;
		setTitle(directory.getAbsolutePath());
		entries.clear();

		// Display directories before files.
		ArrayList<File> directories = new ArrayList<File>();
		ArrayList<File> regularFiles = new ArrayList<File>();

		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			if (file.isDirectory() && file.canRead()) {
				directories.add(file);
			} else if (file.isFile()) {
				regularFiles.add(file);
			}
		}

		Comparator<File> byName = new Comparator<File>() {
			@Override
			public int compare(File a, File b) {
				return a.getName().compareToIgnoreCase(b.getName());
			}
		};

		Collections.sort(directories, byName);
		Collections.sort(regularFiles, byName);

		if (!currentDirectory.getAbsolutePath().equals(Environment.getExternalStorageDirectory().getAbsolutePath())) {
			entries.add(currentDirectory.getParentFile());
		}
		entries.addAll(directories);
		entries.addAll(regularFiles);

		adapter.clear();

		for (int i = 0; i < entries.size(); i++) {
			File file = entries.get(i);
			String name = file.getName();

			if (file.isDirectory()) {
				name = name + "/";
				if (file.getAbsolutePath().equals(currentDirectory.getParentFile().getAbsolutePath())) {
					name = "../";
				}
			}

			adapter.add(name);
		}

		adapter.notifyDataSetChanged();
	}

	public void onBackPressed() {
		if (currentDirectory != null && !currentDirectory.getAbsolutePath().equals(Environment.getExternalStorageDirectory().getAbsolutePath())) {
			browse(currentDirectory.getParentFile());
		} else {
			setResult(Activity.RESULT_CANCELED);
			finish();
		}
	}

	public boolean onKeyDown(int key, KeyEvent event) {
		if (key == KeyEvent.KEYCODE_BACK) {
			onBackPressed();
			return true;
		}
		return super.onKeyDown(key, event);
	}

}
