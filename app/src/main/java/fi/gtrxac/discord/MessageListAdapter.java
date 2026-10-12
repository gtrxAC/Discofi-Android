package fi.gtrxac.discord;

import fi.gtrxac.discord.model.*;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.format.DateFormat;
import android.text.style.StyleSpan;
import android.text.util.Linkify;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Vector;
import java.util.regex.Pattern;

/**
 * Renders message rows for the chat screen.
 */
public class MessageListAdapter extends BaseAdapter {
    private final Context context;
    private final Vector messages;
    private final Account account;
    private final boolean enableMention;
    private final LayoutInflater inflater;
    private final int maxAttachmentHeight;
    private final int maxAttachmentWidth;

    public MessageListAdapter(Context context, Vector messages, Account account) {
        this(context, messages, account, true);
    }

    public MessageListAdapter(Context context, Vector messages, Account account, boolean enableMention) {
        this.context = context;
        this.messages = messages;
        this.account = account;
        this.enableMention = enableMention;
        this.inflater = LayoutInflater.from(context);
        this.maxAttachmentHeight = context.getResources().getDisplayMetrics().heightPixels;
        this.maxAttachmentWidth = context.getResources().getDisplayMetrics().widthPixels;
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
            if (enableMention) {
                try {
                    if ((message.mentions != null && message.mentions.containsKey(Long.toString(account.getUserID()))) || message.mentionEveryone) {
                        convertView.setBackgroundResource(R.color.message_mentioned_background);
                    } else {
                        convertView.setBackgroundResource(0);
                    }
                }
                catch (Exception e) {
                    convertView.setBackgroundResource(0);
                }
            }

            // Author row + avatar: hidden for messages merged into the cluster
            if (message.showAuthor) {
                holder.metadata.setVisibility(View.VISIBLE);
                holder.avatar.getLayoutParams().height = dp(48);
                holder.author.setText(message.author.name);
                holder.timestamp.setText(message.timestamp);
                loadAvatar(holder.avatar, message.author.getAvatarUrl(account.getCdnUrl()));
            } else {
                holder.metadata.setVisibility(View.GONE);
                holder.avatar.getLayoutParams().height = 0;
            }

            // Reply preview
            if (message.replyAuthor != null) {
                holder.reply.setVisibility(View.VISIBLE);
                loadAvatar(holder.replyAvatar, message.replyAuthor.getAvatarUrl(account.getCdnUrl()));
                holder.replyAuthor.setText(message.replyAuthor.name);
                holder.replyContent.setText(message.replyContent);
            } else {
                holder.reply.setVisibility(View.GONE);
            }

            if (message.content.length() > 0) {
                holder.content.setVisibility(View.VISIBLE);
                holder.content.setText(message.content);
                MarkdownRenderer renderer = new MarkdownRenderer(context, message);
                holder.content.setText(renderer.render(message.content));
                holder.content.setMovementMethod(LinkTextView.LocalLinkMovementMethod.getInstance());
            } else {
                holder.content.setVisibility(View.GONE);
            }

            bindAttachments(holder.attachments, message);
        }

        return convertView;
    }

    /**
     * Fill the attachment container: image/video previews first, then the
     * file rows, then embeds. Reused rows may still hold views from an older
     * message, so the container is always reset first.
     */
    private void bindAttachments(LinearLayout container, Message message) {
        container.removeAllViews();

        if (message.attachments != null) {
            for (int i = 0; i < message.attachments.size(); i++) {
                addAttachment(container, (Attachment) message.attachments.elementAt(i));
            }
        }

        if (message.embeds != null) {
            for (int i = 0; i < message.embeds.size(); i++) {
                addEmbed(container, (Embed) message.embeds.elementAt(i));
            }
        }
    }

    private void addAttachment(LinearLayout container, final Attachment attachment) {
        if (attachment.previewable) {
            // TODO: properly handle videos
            ImageView image = new ImageView(context);
            image.setBackgroundResource(R.color.embed_background);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    openInBrowser(account.getCdnAssetUrl(attachment.proxyUrl));
                }
            });

            int[] dims = getAttachmentSize(attachment, false);
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(dims[0], dims[1]);
            imageParams.topMargin = dp(4);

            image.setLayoutParams(imageParams);
            container.addView(image);

            ImageCache.load(image, account.getCdnAssetUrl(attachment.getPreviewUrl(maxAttachmentWidth)));
        } else {
            View view = inflater.inflate(R.layout.attachment, container, false);
            TextView name = (TextView) view.findViewById(R.id.attachment_filename);
            TextView size = (TextView) view.findViewById(R.id.attachment_size);

            name.setText(attachment.filename);
            name.setTextColor(name.getLinkTextColors());
            size.setText(Attachment.formatSize(attachment.size));
            view.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    openInBrowser(account.getCdnAssetUrl(attachment.proxyUrl));
                }
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.topMargin = dp(4);
            container.addView(view, params);
        }
    }

    private int[] getAttachmentSize(Attachment attachment, boolean isEmbed) {
        float density = context.getResources().getDisplayMetrics().density;

        // Width of the message content column: 4+4dp root padding, the 48dp
        // avatar and the 8dp column margin.
        int columnWidth = maxAttachmentWidth - dp(64);
        // Embed: 4dp bar and 4dp column margin
        if (isEmbed) columnWidth -= dp(12);
        if (columnWidth <= 0) columnWidth = maxAttachmentWidth;

        // Small images are shown at their natural size instead of being
        // stretched across the whole column.
        int width = (int) (attachment.width * density + 0.5f);
        if (width > columnWidth) width = columnWidth;

        int height = (int) (width * attachment.height / attachment.width);
        if (height > maxAttachmentHeight) {
            // Taller than the screen: shrink the width too, so the box keeps
            // matching the aspect ratio and the image fills it exactly.
            height = maxAttachmentHeight;
            width = (int) (height * attachment.width / attachment.height);
        }

        return new int[] { width, height };
    }

    private void addEmbed(LinearLayout container, final Embed embed) {
        View view = inflater.inflate(R.layout.embed, container, false);
        View bar = view.findViewById(R.id.embed_color);
        TextView title = (TextView) view.findViewById(R.id.embed_title);
        LinkTextView description = (LinkTextView) view.findViewById(R.id.embed_content);
        TextView siteName = (TextView) view.findViewById(R.id.embed_site_name);

        LinearLayout fields = (LinearLayout) view.findViewById(R.id.embed_fields);

        ImageView image = (ImageView) view.findViewById(R.id.embed_image);
        ImageView video = (ImageView) view.findViewById(R.id.embed_video);
        ImageView icon = (ImageView) view.findViewById(R.id.embed_icon);

        View author = view.findViewById(R.id.embed_author);
        TextView authorName = (TextView) view.findViewById(R.id.embed_author_name);
        ImageView authorIcon = (ImageView) view.findViewById(R.id.embed_author_icon);

        View footer = view.findViewById(R.id.embed_footer);
        TextView footerContent = (TextView) view.findViewById(R.id.embed_footer_content);
        ImageView footerIcon = (ImageView) view.findViewById(R.id.embed_footer_icon);

        bar.setBackgroundColor(embed.color | 0xFF000000);

        if (embed.title != null) {
            title.setText(embed.title);
        } else {
            title.setVisibility(View.GONE);
        }

        if (embed.url != null) {
            title.setTextColor(title.getLinkTextColors());
            title.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    openInBrowser(embed.url);
                }
            });
        }

        if (embed.description != null) {
            MarkdownRenderer renderer = new MarkdownRenderer(context, null);
            description.setText(renderer.render(embed.description));
            description.setMovementMethod(LinkTextView.LocalLinkMovementMethod.getInstance());
        } else {
            description.setVisibility(View.GONE);
        }

        if (embed.provider != null) {
            siteName.setText(embed.provider.name);
            siteName.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    openInBrowser(embed.provider.url);
                }
            });
        } else {
            siteName.setVisibility(View.GONE);
        }

        if (embed.fields != null) {
            for (int i = 0; i < embed.fields.size(); i++) {
                Embed.Field field = embed.fields.elementAt(i);
                if (field == null) continue;

                LinearLayout column = new LinearLayout(context);
                column.setOrientation(LinearLayout.VERTICAL);

                if (field.name != null) {
                    TextView name = new TextView(context);
                    name.setText(field.name);
                    name.setTextColor(context.getResources().getColor(android.R.color.primary_text_dark));
                    name.setTypeface(null, Typeface.BOLD);
                    column.addView(name);
                }
                if (field.value != null) {
                    TextView value = new TextView(context);
                    value.setText(field.value);
                    value.setTextColor(context.getResources().getColor(android.R.color.primary_text_dark));
                    column.addView(value);
                }

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                params.topMargin = dp(4);
                fields.addView(column, params);
            }
        } else {
            fields.setVisibility(View.GONE);
        }

        if (embed.image != null) {
            int[] dims = getAttachmentSize(embed.image, true);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dims[0], dims[1]);
            if (embed.title != null || embed.description != null) params.topMargin = dp(4);
            image.setLayoutParams(params);
            ImageCache.load(image, account.getCdnAssetUrl(embed.image.getPreviewUrl(maxAttachmentWidth)));
        } else {
            image.setVisibility(View.GONE);
        }

        if (embed.video != null && embed.thumbnail != null) {
            int[] dims = getAttachmentSize(embed.thumbnail, true);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dims[0], dims[1]);
            if (embed.title != null || embed.description != null) params.topMargin = dp(4);
            video.setLayoutParams(params);
            ImageCache.load(video, account.getCdnAssetUrl(embed.thumbnail.getPreviewUrl(maxAttachmentWidth)));
            video.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    openInBrowser(embed.video.url);
                }
            });
        } else {
            video.setVisibility(View.GONE);
        }

        if (embed.video == null && embed.thumbnail != null) {
            if (embed.type == Embed.Type.ARTICLE) {
                int[] dims = getAttachmentSize(embed.thumbnail, true);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dims[0], dims[1]);
                if (embed.title != null || embed.description != null) params.topMargin = dp(4);
                image.setLayoutParams(params);
                ImageCache.load(image, account.getCdnAssetUrl(embed.thumbnail.getPreviewUrl(maxAttachmentWidth)));
                image.setVisibility(View.VISIBLE);
                icon.setVisibility(View.GONE);
            } else {
                ImageCache.load(icon, account.getCdnAssetUrl(embed.thumbnail.getPreviewUrl(dp(48))));
            }
        } else {
            icon.setVisibility(View.GONE);
        }

        if (embed.author != null) {
            authorName.setText(embed.author.name);
            authorName.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    openInBrowser(embed.author.url);
                }
            });
            if (embed.author.proxyIconUrl != null) {
                ImageCache.load(authorIcon, account.getCdnAssetUrl(embed.author.proxyIconUrl));
            } else if (embed.author.iconUrl != null) {
                ImageCache.load(authorIcon, embed.author.iconUrl);
            } else {
                authorIcon.setVisibility(View.GONE);
            }
        } else {
            author.setVisibility(View.GONE);
        }

        String footerText = embed.footer != null ? embed.footer.text : null;
        if (embed.timestamp != null) {
            String timestamp = formatEmbedTimestamp(embed.timestamp);
            if (footerText != null && footerText.length() > 0) {
                footerText += " \u00b7 ";
            } else {
                footerText = "";
            }
            footerText += timestamp;
        }

        if (footerText != null) {
            footer.setVisibility(View.VISIBLE);
            footerContent.setText(footerText);
            if (embed.footer.proxyIconUrl != null) {
                ImageCache.load(footerIcon, account.getCdnAssetUrl(embed.footer.proxyIconUrl));
            } else if (embed.footer.iconUrl != null) {
                ImageCache.load(footerIcon, embed.footer.iconUrl);
            } else {
                footerIcon.setVisibility(View.GONE);
            }
        } else {
            footer.setVisibility(View.GONE);
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(4);
        container.addView(view, params);
    }

    private String formatEmbedTimestamp(String timestamp) {
        Date date = parseEmbedTimestamp(timestamp);
        if (date == null) return timestamp;

        return DateFormat.getDateFormat(context).format(date)
                + " " + DateFormat.getTimeFormat(context).format(date);
    }

    private Date parseEmbedTimestamp(String timestamp) {
        String value = timestamp.trim();
        int timeSeparator = value.indexOf('T');
        if (timeSeparator < 0) return null;

        if (value.endsWith("Z")) {
            value = value.substring(0, value.length() - 1) + "+0000";
        } else {
            int zoneStart = Math.max(value.lastIndexOf('+'), value.lastIndexOf('-'));
            if (zoneStart > timeSeparator && value.length() - zoneStart == 6
                    && value.charAt(zoneStart + 3) == ':') {
                value = value.substring(0, zoneStart + 3)
                        + value.substring(zoneStart + 4);
            }
        }

        int zoneStart = Math.max(value.lastIndexOf('+'), value.lastIndexOf('-'));
        if (zoneStart <= timeSeparator) zoneStart = value.length();

        int fractionStart = value.indexOf('.', timeSeparator);
        boolean hasFraction = fractionStart >= 0 && fractionStart < zoneStart;
        if (hasFraction) {
            String fraction = value.substring(fractionStart + 1, zoneStart);
            if (fraction.length() == 0) return null;
            for (int i = 0; i < fraction.length(); i++) {
                if (fraction.charAt(i) < '0' || fraction.charAt(i) > '9') return null;
            }
            String milliseconds = (fraction + "000").substring(0, 3);
            value = value.substring(0, fractionStart + 1) + milliseconds
                    + value.substring(zoneStart);
        }

        SimpleDateFormat parser = new SimpleDateFormat(
                hasFraction ? "yyyy-MM-dd'T'HH:mm:ss.SSSZ" : "yyyy-MM-dd'T'HH:mm:ssZ",
                Locale.US);
        parser.setLenient(false);
        ParsePosition position = new ParsePosition(0);
        Date date = parser.parse(value, position);
        return date != null && position.getIndex() == value.length() ? date : null;
    }

    /**
     * Load an avatar, falling back to the default icon for users without a
     * custom one (ImageCache.load(null) would just clear the view).
     */
    private void loadAvatar(ImageView view, String url) {
        if (url == null) {
            view.setTag(null);
            view.setImageResource(R.mipmap.ic_launcher);
        } else {
            ImageCache.load(view, url);
        }
    }

    private void openInBrowser(String url) {
        if (url == null) return;
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
        catch (ActivityNotFoundException e) {
            Toast.makeText(context, "No app can open this link",
                    Toast.LENGTH_LONG).show();
        }
        catch (Exception e) {
            Toast.makeText(context, "Failed to open link: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private int dp(int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    private static class ViewHolder {
        View msg;
        View metadata;
        TextView author;
        TextView timestamp;
        LinkTextView content;
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
            content = (LinkTextView) view.findViewById(R.id.msg_content);
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
