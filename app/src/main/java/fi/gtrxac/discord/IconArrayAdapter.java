package fi.gtrxac.discord;

import android.content.Context;
import android.graphics.Color;
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
    private ArrayList<Boolean> unreadFlags;
    private ArrayList<Integer> mentionCounts;

    public IconArrayAdapter(Context context, ArrayList<String> items, ArrayList<String> iconUrls) {
        this(context, items, iconUrls, null, null);
    }

    public IconArrayAdapter(Context context, ArrayList<String> items, ArrayList<String> iconUrls,
        ArrayList<Boolean> unreadFlags, ArrayList<Integer> mentionCounts) {
        super(context, 0, items);
        this.iconUrls = iconUrls;
        this.unreadFlags = unreadFlags;
        this.mentionCounts = mentionCounts;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView =
                LayoutInflater.from(getContext()).inflate(R.layout.icon_list_item, parent, false);
        }

        String item = getItem(position);
        String url = iconUrls.get(position);

        ImageView image = (ImageView) convertView.findViewById(R.id.itemImage);
        TextView text = (TextView) convertView.findViewById(R.id.itemText);
        ImageView unreadIndicator = (ImageView) convertView.findViewById(R.id.itemUnread);
        TextView mentionIndicator = (TextView) convertView.findViewById(R.id.itemMentions);

        text.setText(item);

        if (url == null) {
            Glide.clear(image);
            image.setImageResource(R.mipmap.ic_launcher);
        }
        else {
            image.setImageDrawable(null);

            if (Settings.squareAvatars) {
                Glide.with(getContext()).load(url).thumbnail(0.5f).into(image);
            }
            else {
                Glide.with(getContext()).load(url).thumbnail(0.5f)
                    .transform(new CircleTransform(getContext())).into(image);
            }
        }

        boolean unread = unreadFlags != null && unreadFlags.get(position);
        unreadIndicator.setVisibility(unread ? View.VISIBLE : View.INVISIBLE);

        int mentions =
            mentionCounts != null && position < mentionCounts.size() ? mentionCounts.get(position)
                .intValue() : 0;

        if (mentions > 0) {
            mentionIndicator.setText(Integer.toString(mentions));
            mentionIndicator.setVisibility(View.VISIBLE);
        }
        else {
            mentionIndicator.setVisibility(View.GONE);
        }

        if (Settings.lightTheme) {
            unreadIndicator.setImageResource(R.drawable.unread_indicator_light);
        }
        else {
            unreadIndicator.setImageResource(R.drawable.unread_indicator);
        }

        return convertView;
    }
}
