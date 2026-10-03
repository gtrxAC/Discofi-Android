package fi.gtrxac.discord;

import fi.gtrxac.discord.model.*;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Vector;

/**
 * Renders message rows for the chat screen.
 */
public class MessageListAdapter extends BaseAdapter {
    private final Context context;
    private final Vector messages;
    private final Account account;
    private final LayoutInflater inflater;
    private final int maxAttachmentHeight;

    public MessageListAdapter(Context context, Vector messages, Account account) {
        this.context = context;
        this.messages = messages;
        this.account = account;
        this.inflater = LayoutInflater.from(context);
        this.maxAttachmentHeight = context.getResources().getDisplayMetrics().heightPixels;
    }

    public int getCount() {
        return messages.size();
    }

    public Object getItem(int position) {
        return messages.elementAt(position);
    }

    public long getItemId(int position) {
        return ((Message) messages.elementAt(position)).id;
    }

    public int getViewTypeCount() {
        return 1;
    }

    public boolean isEnabled(int position) {
        return false;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.message, parent, false);

            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Message message = (Message) messages.elementAt(position);

        if (message.isStatus) {
            holder.msg.setVisibility(View.GONE);
            holder.status.setVisibility(View.VISIBLE);

            SpannableStringBuilder sb = new SpannableStringBuilder(
                    message.author.name + " " + message.content);
            sb.setSpan(new StyleSpan(Typeface.BOLD), 0,
                    message.author.name.length(),
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            holder.statusText.setText(sb);
            holder.statusTimestamp.setText(message.timestamp);
        } else {
            // Author row + avatar: hidden for messages merged into the cluster
            if (message.showAuthor) {
                holder.metadata.setVisibility(View.VISIBLE);
                holder.avatar.getLayoutParams().height = dp(48);
                holder.author.setText(message.author.name);
                holder.timestamp.setText(message.timestamp);
                ImageCache.load(holder.avatar, message.author.getAvatarUrl(account.getCdnUrl()));
            } else {
                holder.metadata.setVisibility(View.GONE);
                holder.avatar.getLayoutParams().height = 0;
            }

            // Reply preview
            if (message.replyAuthor != null) {
                holder.reply.setVisibility(View.VISIBLE);
                ImageCache.load(holder.replyAvatar, message.replyAuthor.getAvatarUrl(account.getCdnUrl()));
                holder.replyAuthor.setText(message.replyAuthor.name);
                holder.replyContent.setText(message.replyContent);
            } else {
                holder.reply.setVisibility(View.GONE);
            }

            if (message.content.length() > 0) {
                holder.content.setVisibility(View.VISIBLE);
                holder.content.setText(message.content);
            } else {
                holder.content.setVisibility(View.GONE);
            }

            // Image attachments: reused rows may still hold images from an older
            // message, so always reset the container first.
            holder.attachments.removeAllViews();

            if (message.imageAttachments != null) {
                for (int i = 0; i < message.imageAttachments.size(); i++) {
                    String url = account.getCdnAssetUrl(
                            (String) message.imageAttachments.elementAt(i));

                    ImageView image = new ImageView(context);
                    image.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    image.setAdjustViewBounds(true);
                    image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    image.setMaxHeight(maxAttachmentHeight);
                    image.setPadding(0, dp(4), 0, 0);
                    holder.attachments.addView(image);

                    ImageCache.load(image, url);
                }
            }
        }

        return convertView;
    }

    private int dp(int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    private static class ViewHolder {
        View msg;
        View metadata;
        TextView author;
        TextView timestamp;
        TextView content;
        ImageView avatar;
        LinearLayout attachments;

        View reply;
        TextView replyAuthor;
        TextView replyContent;
        ImageView replyAvatar;

        View status;
        TextView statusText;
        TextView statusTimestamp;

        public ViewHolder(View view) {
            msg = view.findViewById(R.id.message);
            metadata = view.findViewById(R.id.msg_metadata);
            author = (TextView) view.findViewById(R.id.msg_author);
            timestamp = (TextView) view.findViewById(R.id.msg_timestamp);
            content = (TextView) view.findViewById(R.id.msg_content);
            avatar = (ImageView) view.findViewById(R.id.msg_avatar);
            attachments = (LinearLayout) view.findViewById(R.id.msg_attachments);

            reply = view.findViewById(R.id.msg_reply);
            replyAuthor = (TextView) view.findViewById(R.id.reply_author);
            replyContent = (TextView) view.findViewById(R.id.reply_content);
            replyAvatar = (ImageView) view.findViewById(R.id.reply_avatar);

            status = view.findViewById(R.id.status);
            statusText = (TextView) view.findViewById(R.id.status_text);
            statusTimestamp = (TextView) view
                    .findViewById(R.id.status_timestamp);
        }
    }
}
