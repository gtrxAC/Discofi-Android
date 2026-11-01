package fi.gtrxac.discord;

import fi.gtrxac.discord.model.*;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.format.DateFormat;
import android.text.style.QuoteSpan;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
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

import com.bumptech.glide.DrawableRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

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
    private final Account acc;
    private final boolean enableMention;
    private final LayoutInflater inflater;
    private final int maxAttachmentHeight;
    private final int maxAttachmentWidth;

    public MessageListAdapter(Context context, Vector messages, Account acc) {
        this(context, messages, acc, true);
    }

    public MessageListAdapter(Context context, Vector messages, Account acc, boolean enableMention) {
        this.context = context;
        this.messages = messages;
        this.acc = acc;
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

        if (enableMention) {
            try {
                if ((message.mentions != null && message.mentions.containsKey(Long.toString(acc.getUserID()))) || message.mentionEveryone) {
                    convertView.setBackgroundResource(Theme.messageMentionedBackground);
                } else {
                    convertView.setBackgroundResource(0);
                }
            }
            catch (Exception e) {
                convertView.setBackgroundResource(0);
            }
        }

        // Reply preview
        if (message.reply != null && Settings.showReplies) {
            holder.reply.setVisibility(View.VISIBLE);
            if (!Settings.compactMode) {
                holder.replyAvatar.setVisibility(View.VISIBLE);
                loadAvatar(holder.replyAvatar, message.reply.author.getAvatarUrl(acc.getCdnUrl()));
            } else {
                holder.replyAvatar.setVisibility(View.GONE);
            }
            holder.replyAuthor.setText(message.reply.author.name);
            if (message.reply.author.system) {
                holder.replyBadge.setVisibility(View.VISIBLE);
                holder.replyBadge.setText(R.string.badge_system);
            } else if (message.reply.author.bot) {
                holder.replyBadge.setVisibility(View.VISIBLE);
                holder.replyBadge.setText(R.string.badge_bot);
            } else {
                holder.replyBadge.setVisibility(View.GONE);
            }
            if (message.reply.content.length() > 0) {
                holder.replyContent.setVisibility(View.VISIBLE);
                if (Settings.useMarkdown) {
                    MarkdownRenderer renderer = new MarkdownRenderer(context, message.reply,
                            acc.getCdnUrl(), holder.replyContent);
                    holder.replyContent.setText(renderer.render(message.reply.content));
                } else {
                    holder.replyContent.setText(message.reply.content);
                }
            } else {
                holder.replyContent.setVisibility(View.GONE);
            }
        } else {
            holder.reply.setVisibility(View.GONE);
        }

        if (Settings.compactMode || message.isStatus) {
            holder.msg.setVisibility(View.GONE);
            holder.compact.setVisibility(View.VISIBLE);

            SpannableStringBuilder sb = new SpannableStringBuilder(message.timestamp + " " + message.author.name);
            sb.setSpan(new TextAppearanceSpan(context, android.R.style.TextAppearance_Small), 0, message.timestamp.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            // TODO: badge on author
            sb.setSpan(new StyleSpan(Typeface.BOLD), message.timestamp.length(), sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (message.content.length() > 0) {
                SpannableStringBuilder content;
                if (Settings.useMarkdown) {
                    MarkdownRenderer renderer = new MarkdownRenderer(context, message,
                            acc.getCdnUrl(), holder.compactText, false);
                    content = renderer.render(message.content);
                } else {
                    content = new SpannableStringBuilder(message.content);
                }

                if (message.isEdited) {
                    int start = content.length();
                    content.append(" " + context.getResources().getString(R.string.edited));
                    int end = content.length();
                    content.setSpan(new TextAppearanceSpan(context, android.R.style.TextAppearance_Small), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                if (message.isForwarded) {
                    SpannableStringBuilder forwarded = new SpannableStringBuilder("\n" + context.getResources().getString(R.string.forwarded));
                    forwarded.setSpan(new TextAppearanceSpan(context, android.R.style.TextAppearance_Small), 1, forwarded.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    forwarded.setSpan(new StyleSpan(Typeface.ITALIC), 1, forwarded.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    forwarded.append("\n");
                    forwarded.append(content);
                    forwarded.setSpan(new QuoteSpan(Theme.quoteColor), 1, forwarded.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE | (128 << Spanned.SPAN_PRIORITY_SHIFT));
                    content = forwarded;
                }

                sb.append(" ");
                sb.append(content);
                holder.compactText.setMovementMethod(LinkTextView.LocalLinkMovementMethod.getInstance());
            }
            holder.compactText.setText(sb);
            bindAttachments(holder.compactAttachments, message);
        } else {
            holder.msg.setVisibility(View.VISIBLE);
            holder.compact.setVisibility(View.GONE);

            // Author row + avatar: hidden for messages merged into the cluster
            if (message.showAuthor) {
                holder.metadata.setVisibility(View.VISIBLE);
                holder.avatar.getLayoutParams().height = dp(48);
                holder.author.setText(message.author.name);
                if (message.author.system) {
                    holder.badge.setVisibility(View.VISIBLE);
                    holder.badge.setText(R.string.badge_system);
                } else if (message.author.bot) {
                    holder.badge.setVisibility(View.VISIBLE);
                    holder.badge.setText(R.string.badge_bot);
                } else {
                    holder.badge.setVisibility(View.GONE);
                }
                holder.timestamp.setText(message.timestamp);
                loadAvatar(holder.avatar, message.author.getAvatarUrl(acc.getCdnUrl()));
            } else {
                holder.metadata.setVisibility(View.GONE);
                holder.avatar.getLayoutParams().height = 0;
            }

            if (message.content.length() > 0) {
                holder.content.setVisibility(View.VISIBLE);

                SpannableStringBuilder content;
                if (Settings.useMarkdown) {
                    MarkdownRenderer renderer = new MarkdownRenderer(context, message,
                            acc.getCdnUrl(), holder.content);
                    content = renderer.render(message.content);
                } else {
                    content = new SpannableStringBuilder(message.content);
                }

                if (message.isEdited) {
                    int start = content.length();
                    content.append(" " + context.getResources().getString(R.string.edited));
                    int end = content.length();
                    content.setSpan(new TextAppearanceSpan(context, android.R.style.TextAppearance_Small), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                if (message.isForwarded) {
                    SpannableStringBuilder forwarded = new SpannableStringBuilder(context.getResources().getString(R.string.forwarded));
                    forwarded.setSpan(new TextAppearanceSpan(context, android.R.style.TextAppearance_Small), 0, forwarded.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    forwarded.setSpan(new StyleSpan(Typeface.ITALIC), 0, forwarded.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    forwarded.append("\n");
                    forwarded.append(content);
                    forwarded.setSpan(new QuoteSpan(Theme.quoteColor), 0, forwarded.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE | (128 << Spanned.SPAN_PRIORITY_SHIFT));
                    content = forwarded;
                }

                holder.content.setText(content);
                holder.content.setMovementMethod(LinkTextView.LocalLinkMovementMethod.getInstance());
            }
            else {
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
        releaseAttachmentViews(container);
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

    /**
     * Stop animated previews and cancel pending loads before their views are
     * discarded. GIF drawables would keep decoding frames even when their
     * associated ImageView is no longer on screen.
     */
    private void releaseAttachmentViews(LinearLayout container) {
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (!(child instanceof ImageView)) continue;

            Drawable drawable = ((ImageView) child).getDrawable();
            if (drawable instanceof GlideDrawable) {
                ((GlideDrawable) drawable).stop();
            }
            Glide.clear(child);
        }
    }

    private void addAttachment(LinearLayout container, final Attachment attachment) {
        if (Settings.showImages && attachment.previewable) {
            // TODO: properly handle videos
            ImageView image = new ImageView(context);
            image.setBackgroundColor(Theme.embedBackground);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    openInBrowser(acc.getCdnAssetUrl(attachment.proxyUrl));
                }
            });

            int[] dims = getAttachmentSize(attachment, false);
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(dims[0], dims[1]);
            imageParams.topMargin = dp(4);

            image.setLayoutParams(imageParams);
            container.addView(image);

            String url = acc.getCdnAssetUrl(attachment.getPreviewUrl(maxAttachmentWidth));

            String fallbackUrl = attachment.animated ?
                acc.getCdnAssetUrl(attachment.getStillPreviewUrl(maxAttachmentWidth)) :
                null;

            loadImage(image, url, fallbackUrl);
        }
        else {
            View view = inflater.inflate(R.layout.attachment, container, false);
            view.setBackgroundColor(Theme.embedBackground);
            TextView name = (TextView) view.findViewById(R.id.attachment_filename);
            TextView size = (TextView) view.findViewById(R.id.attachment_size);

            name.setText(attachment.filename);
            name.setTextColor(name.getLinkTextColors());
            size.setText(Attachment.formatSize(attachment.size));
            view.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    openInBrowser(acc.getCdnAssetUrl(attachment.proxyUrl));
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
        view.setBackgroundColor(Theme.embedBackground);
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
            title.setTextColor(context.getResources().getColor(Settings.lightTheme ? android.R.color.black : android.R.color.white));
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
            MarkdownRenderer renderer = new MarkdownRenderer(context, null,
                    acc.getCdnUrl(), description);
            description.setText(renderer.render(embed.description));
            description.setTextColor(context.getResources().getColor(Settings.lightTheme ? android.R.color.black : android.R.color.white));
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
                    name.setTextColor(context.getResources().getColor(Settings.lightTheme ? android.R.color.black : android.R.color.white));
                    name.setTypeface(null, Typeface.BOLD);
                    column.addView(name);
                }
                if (field.value != null) {
                    TextView value = new TextView(context);
                    value.setText(field.value);
                    value.setTextColor(context.getResources().getColor(Settings.lightTheme ? android.R.color.black : android.R.color.white));
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

        if (Settings.showImages && embed.image != null) {
            int[] dims = getAttachmentSize(embed.image, true);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dims[0], dims[1]);
            if (embed.title != null || embed.description != null) params.topMargin = dp(4);
            image.setLayoutParams(params);
            loadImage(image, acc.getCdnAssetUrl(embed.image.getPreviewUrl(maxAttachmentWidth)));
        } else {
            image.setVisibility(View.GONE);
        }

        if (Settings.showImages && (embed.video != null && embed.thumbnail != null)) {
            int[] dims = getAttachmentSize(embed.thumbnail, true);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dims[0], dims[1]);
            if (embed.title != null || embed.description != null) params.topMargin = dp(4);
            video.setLayoutParams(params);
            loadImage(video, acc.getCdnAssetUrl(embed.thumbnail.getPreviewUrl(maxAttachmentWidth)));
            video.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    openInBrowser(embed.video.url);
                }
            });
        } else {
            video.setVisibility(View.GONE);
        }

        if (Settings.showImages && (embed.video == null && embed.thumbnail != null)) {
            if (embed.type == Embed.Type.ARTICLE) {
                int[] dims = getAttachmentSize(embed.thumbnail, true);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dims[0], dims[1]);
                if (embed.title != null || embed.description != null) params.topMargin = dp(4);
                image.setLayoutParams(params);
                loadImage(image, acc.getCdnAssetUrl(embed.thumbnail.getPreviewUrl(maxAttachmentWidth)));
                image.setVisibility(View.VISIBLE);
                icon.setVisibility(View.GONE);
            } else {
                loadImage(icon, acc.getCdnAssetUrl(embed.thumbnail.getPreviewUrl(dp(48))));
            }
        } else {
            icon.setVisibility(View.GONE);
        }

        if (embed.author != null) {
            authorName.setText(embed.author.name);
            authorName.setTextColor(context.getResources().getColor(Settings.lightTheme ? android.R.color.black : android.R.color.white));
            authorName.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    openInBrowser(embed.author.url);
                }
            });
            if (Settings.showImages) {
                if (embed.author.proxyIconUrl != null) {
                    loadImage(authorIcon, acc.getCdnAssetUrl(embed.author.proxyIconUrl), true);
                } else if (embed.author.iconUrl != null) {
                    loadImage(authorIcon, embed.author.iconUrl, true);
                } else {
                    authorIcon.setVisibility(View.GONE);
                }
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
            footerContent.setTextColor(context.getResources().getColor(Settings.lightTheme ? android.R.color.black : android.R.color.white));
            if (Settings.showImages) {
                if (embed.footer.proxyIconUrl != null) {
                    loadImage(footerIcon, acc.getCdnAssetUrl(embed.footer.proxyIconUrl), true);
                } else if (embed.footer.iconUrl != null) {
                    loadImage(footerIcon, embed.footer.iconUrl, true);
                } else {
                    footerIcon.setVisibility(View.GONE);
                }
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

    private void loadImage(ImageView view, String url) {
        loadImage(view, url, false);
    }

    private void loadImage(ImageView view, String url, boolean rounded) {
        loadImage(view, url, null, rounded);
    }

    private void loadImage(ImageView view, String url, String fallbackUrl) {
        loadImage(view, url, fallbackUrl, false);
    }

    private void loadImage(final ImageView view, final String url, final String fallbackUrl, final boolean rounded) {
        view.setImageDrawable(null);
        if (url == null) {
            Glide.clear(view);
            return;
        }

        DrawableRequestBuilder<String> request = Glide.with(context).load(url).thumbnail(0.5f);

        if (rounded && !Settings.squareAvatars) {
            request.transform(new CircleTransform(context));
        }

        if (fallbackUrl != null) {
            request = request.listener(new RequestListener<String, GlideDrawable>() {
                public boolean onException(Exception e, String model, Target<GlideDrawable> target,
                        boolean isFirstResource) {
                    // An animated preview can fail (too big for memory, invalid
                    // gif data); show the still frame instead.
                    loadImage(view, fallbackUrl, rounded);
                    return true;
                }

                public boolean onResourceReady(GlideDrawable resource, String model,
                        Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                    return false;
                }
            });
        }

        request.into(view);
    }

    /**
     * Load an avatar, falling back to the default icon for users without a
     * custom one.
     */
    private void loadAvatar(ImageView view, String url) {
        if (url == null) {
            Glide.clear(view);
            view.setImageResource(R.mipmap.ic_launcher);
        } else {
            loadImage(view, url, true);
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
        TextView badge;
        TextView timestamp;
        LinkTextView content;
        ImageView avatar;
        LinearLayout attachments;

        View reply;
        TextView replyAuthor;
        TextView replyBadge;
        TextView replyContent;
        ImageView replyAvatar;

        View compact;
        LinkTextView compactText;
        LinearLayout compactAttachments;

        public ViewHolder(View view) {
            msg = view.findViewById(R.id.message);
            metadata = view.findViewById(R.id.msg_metadata);
            author = (TextView) view.findViewById(R.id.msg_author);
            badge = (TextView) view.findViewById(R.id.msg_badge);
            timestamp = (TextView) view.findViewById(R.id.msg_timestamp);
            content = (LinkTextView) view.findViewById(R.id.msg_content);
            avatar = (ImageView) view.findViewById(R.id.msg_avatar);
            attachments = (LinearLayout) view.findViewById(R.id.msg_attachments);

            reply = view.findViewById(R.id.msg_reply);
            replyAuthor = (TextView) view.findViewById(R.id.reply_author);
            replyBadge = (TextView) view.findViewById(R.id.reply_badge);
            replyContent = (TextView) view.findViewById(R.id.reply_content);
            replyAvatar = (ImageView) view.findViewById(R.id.reply_avatar);

            compact = view.findViewById(R.id.compact);
            compactText = (LinkTextView) view.findViewById(R.id.compact_text);
            compactAttachments = (LinearLayout) view.findViewById(R.id.compact_attachments);
        }
    }
}
