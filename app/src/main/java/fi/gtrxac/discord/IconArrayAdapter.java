package fi.gtrxac.discord;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class IconArrayAdapter extends ArrayAdapter<String> {
    private ArrayList<String> iconUrls;

    public IconArrayAdapter(Context context, ArrayList<String> items, ArrayList<String> iconUrls) {
        super(context, 0, items);
        this.iconUrls = iconUrls;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.icon_list_item, parent, false);
        }

        String item = getItem(position);
        String url = iconUrls.get(position);

        ImageView image = (ImageView) convertView.findViewById(R.id.itemImage);
        TextView text = (TextView) convertView.findViewById(R.id.itemText);

        text.setText(item);

        if (url == null) {
            Glide.clear(image);
            image.setImageResource(R.mipmap.ic_launcher);
        } else {
            image.setImageDrawable(null);

            if (Settings.squareAvatars) {
                Glide.with(getContext())
                    .load(url)
                    .thumbnail(0.5f)
                    .into(image);
            } else {
                Glide.with(getContext())
                    .load(url)
                    .thumbnail(0.5f)
                    .transform(new CircleTransform(getContext()))
                    .into(image);
            }
        }

        return convertView;
    }
}
