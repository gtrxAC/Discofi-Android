package fi.gtrxac.discord;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class IconArrayAdapter extends ArrayAdapter<String> {
    private int icon;

    public IconArrayAdapter(Context context, ArrayList<String> items, int icon) {
        super(context, 0, items);
        this.icon = icon;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.icon_list_item, parent, false);
        }

        String item = getItem(position);

        ImageView image = (ImageView) convertView.findViewById(R.id.itemImage);
        TextView text = (TextView) convertView.findViewById(R.id.itemText);

        text.setText(item);
        image.setImageResource(icon);

        return convertView;
    }
}