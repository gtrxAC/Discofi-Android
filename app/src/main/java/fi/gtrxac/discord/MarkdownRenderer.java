package fi.gtrxac.discord;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.SystemClock;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.BackgroundColorSpan;
import android.text.style.BulletSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.QuoteSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.ReplacementSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Stack;

import cc.nnproject.markdown.Markdown;
import cc.nnproject.markdown.MarkdownListener;
import fi.gtrxac.discord.model.Message;

public class MarkdownRenderer implements MarkdownListener {

    private static final int SPAN_FLAGS = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;

    // Spans sorted by priority (highest first) determine margin order.
    // Quotes ([128,255]) outrank bullets (<128).
    private static final int QUOTE_SPAN_PRIORITY = 255;
    private static final int QUOTE_SPAN_PRIORITY_FLOOR = 128;
    private static final int BULLET_PRIORITY_BASE = QUOTE_SPAN_PRIORITY_FLOOR - 1;

    private final Context context;
    private final Resources resources;
    private final Message message;
    private final String cdnUrl;
    private final TextView view;
    private final boolean enableBigEmoji;

    private final Hashtable urls = new Hashtable();
    private final Stack<Object> links = new Stack<>();
    private final Stack<int[]> bulletItems = new Stack<>();
    private final Stack<Integer> quoteStarts = new Stack<>();
    private SpannableStringBuilder out;
    private int listIndent;
    private int codeStart = -1;
    private int subtextStart = -1;
    private int spoilerStart = -1;
    private boolean emojiOnly;
    private int emojiCount;

    public MarkdownRenderer(Context context, Message message, String cdnUrl, TextView view) {
        this(context, message, cdnUrl, view, true);
    }

    public MarkdownRenderer(Context context, Message message, String cdnUrl, TextView view,
        boolean enableBigEmoji) {
        this.context = context.getApplicationContext();
        this.resources = context.getResources();
        this.message = message;
        this.cdnUrl = cdnUrl;
        this.view = view;
        this.enableBigEmoji = enableBigEmoji;
    }

    private static boolean hasContent(CharSequence text) {
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) > ' ') return true;
        }
        return false;
    }

    // region Helpers

    private static int rebase(int index, int indent, int removed) {
        // Index at or before cut stays; inside dropped run collapses onto cut.
        return index > indent ? Math.max(indent, index - removed) : index;
    }

    public SpannableStringBuilder render(String markdown) {
        Markdown.enableStrikethroughFont = true;
        Markdown.enableBlockquotes = true;
        Markdown.enableLists = true;
        Markdown.enableHtmlTags = false;
        Markdown.enableHorizontalLines = false;
        Markdown.enableImages = false;
        Markdown.enableEmojis = true;
        if (message != null) Markdown.enableDiscordParsing = true;
        Markdown.monospaceIsBold = false;
        Markdown.breakOnNewLine = true;

        urls.clear();
        links.clear();
        bulletItems.clear();
        quoteStarts.clear();
        listIndent = 0;
        codeStart = subtextStart = spoilerStart = -1;
        emojiOnly = true;
        emojiCount = 0;
        out = new SpannableStringBuilder();

        Markdown.parse(this, out, markdown, urls);

        if (enableBigEmoji && (emojiOnly && emojiCount != 0)) {
            for (EmojiSpan emoji : out.getSpans(0, out.length(), EmojiSpan.class)) {
                emoji.setBig(true);
            }
        }
        return out;
    }

    private void applyFontSpans(int start, int end, int font) {
        if ((font & Markdown.FONT_STYLE_BOLD) != 0) {
            out.setSpan(new StyleSpan(Typeface.BOLD), start, end, SPAN_FLAGS);
        }
        if ((font & Markdown.FONT_STYLE_ITALIC) != 0) {
            out.setSpan(new StyleSpan(Typeface.ITALIC), start, end, SPAN_FLAGS);
        }
        if ((font & Markdown.FONT_STYLE_UNDERLINED) != 0) {
            out.setSpan(new UnderlineSpan(), start, end, SPAN_FLAGS);
        }
        if ((font & Markdown.FONT_STYLE_STRIKETHROUGH) != 0) {
            out.setSpan(new StrikethroughSpan(), start, end, SPAN_FLAGS);
        }
        if ((font & Markdown.FONT_FACE_MONOSPACE) != 0) {
            out.setSpan(new TypefaceSpan("monospace"), start, end, SPAN_FLAGS);
            out.setSpan(new BackgroundColorSpan(Theme.codeBackground), start, end, SPAN_FLAGS);
            out.setSpan(new ForegroundColorSpan(resources
                    .getColor(Settings.lightTheme ? android.R.color.black : android.R.color.white)),
                start, end, SPAN_FLAGS);
        }

        float scale = 1f;
        if ((font & Markdown.FONT_SIZE_LARGE) != 0) {
            scale = 1.5f;
        }
        else if ((font & Markdown.FONT_SIZE_SMALL) == 0) scale = 1.2f; // MEDIUM
        if (scale != 1f) {
            out.setSpan(new RelativeSizeSpan(scale), start, end, SPAN_FLAGS);
        }
    }

    private void applyLinkSpan(int start, int end, final Object link) {
        if (link == null) return;
        out.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                handleLink(link);
            }
        }, start, end, SPAN_FLAGS);
    }

    private void appendStyled(String text, int font, final Object link) {
        int start = out.length();
        out.append(text);
        int end = out.length();
        if (start == end) return;

        applyFontSpans(start, end, font);
        applyLinkSpan(start, end, link);
    }

    private void appendMentionStyled(String text, int font, int color) {
        int start = out.length();
        out.append(text);
        int end = out.length();
        if (start == end) return;

        int background = Theme.mentionBackground;
        int foreground = Theme.mentionForeground;
        if (color != 0) {
            background = Color
                .argb((int) (255 * 0.1), Color.red(color), Color.green(color), Color.blue(color));
            foreground = color;
        }

        applyFontSpans(start, end, font);
        out.setSpan(new BackgroundColorSpan(background), start, end, SPAN_FLAGS);
        out.setSpan(new ForegroundColorSpan(foreground), start, end, SPAN_FLAGS);
        if (!links.isEmpty()) {
            final Object link = links.peek();
            applyLinkSpan(start, end, link);
        }
    }

    private void appendMentionStyled(String text, int font) {
        appendMentionStyled(text, font, 0);
    }

    private String formatDiscordTimestamp(String timestamp, String style) {
        long unix;
        try {
            unix = Long.parseLong(timestamp);
        }
        catch (NumberFormatException e) {
            return "<t:" + timestamp + (style != null ? ":" + style : "") + ">";
        }
        Date date = new Date(unix * 1000L);
        // FIXME: use android.text.format.DateFormat on android >1.5
        try {
            if (style.equals("t")) {
                return new SimpleDateFormat("HH:mm").format(date);
            }
            else if (style.equals("T")) {
                return new SimpleDateFormat("HH:mm:ss").format(date);
            }
            else if (style.equals("d")) {
                return new SimpleDateFormat("dd/MM/yyyy").format(date);
            }
            else if (style.equals("D")) {
                return new SimpleDateFormat("d MMMM yyyy").format(date);
            }
            else if (style.equals("f")) {
                return new SimpleDateFormat("d MMMM yyyy HH:mm").format(date);
            }
            else if (style.equals("F")) {
                return new SimpleDateFormat("EEEE, d MMMM yyyy HH:mm").format(date);
            }
            else if (style.equals("s")) {
                return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(date);
            }
            else if (style.equals("S")) {
                return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(date);
            }
            else if (style.equals("R")) {
                long diffSec = Math.abs(System.currentTimeMillis() - date.getTime()) / 1000L;
                boolean future = date.getTime() > System.currentTimeMillis();
                String rel;
                long amount;
                String unit;
                if (diffSec < 60) {
                    amount = diffSec;
                    unit = "second";
                }
                else if (diffSec < 3600) {
                    amount = diffSec / 60;
                    unit = "minute";
                }
                else if (diffSec < 86400) {
                    amount = diffSec / 3600;
                    unit = "hour";
                }
                else if (diffSec < 2592000) {
                    amount = diffSec / 86400;
                    unit = "day";
                }
                else if (diffSec < 31536000) {
                    amount = diffSec / 2592000;
                    unit = "month";
                }
                else {
                    amount = diffSec / 31536000;
                    unit = "year";
                }
                rel = amount + " " + unit + (amount == 1 ? "" : "s");
                return future ? "in " + rel : rel + " ago";
            }
        }
        catch (Exception e) {
        }
        return new SimpleDateFormat("d MMMM yyyy HH:mm").format(date);
    }

    private void handleLink(Object link) {
        // link is either a key into the urls table or the URL itself
        Object resolved = urls.get(link);
        String url = resolved != null ? resolved.toString() : link.toString();
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
        catch (ActivityNotFoundException e) {
            Toast.makeText(context, "No app can open this link", Toast.LENGTH_LONG).show();
        }
        catch (Exception e) {
            Toast.makeText(context, "Failed to open link: " + e.getMessage(), Toast.LENGTH_LONG)
                .show();
        }
    }

    private void applyBulletSpan(int start, int depth, int end) {
        if (start < 0 || start >= end) return;
        // Parser emits next item's indent before endListItem; range can spill onto next line.
        // Trim trailing whitespace so BulletSpan stays within its paragraph.
        while (end > start) {
            char c = out.charAt(end - 1);
            if (c != ' ' && c != '\t') break;
            end--;
        }
        if (start >= end) return;
        int gapWidth = (int) (8 * resources.getDisplayMetrics().density + 0.5f);
        // Trim trailing whitespace so BulletSpan stays within its paragraph.
        // Depth-based priority stays below every quote's.
        int priority = Math.max(1, BULLET_PRIORITY_BASE - Math.max(1, depth));
        out.setSpan(new BulletSpan(gapWidth), start, end,
            SPAN_FLAGS | (priority << Spanned.SPAN_PRIORITY_SHIFT));
    }

    public void beginMarkdown(Object ctx) {
    }

    // endregion

    // region MarkdownListener

    public void endMarkdown(Object ctx) {
        // Close any remaining bullet items (parser doesn't call endListItem for trailing lists).
        while (!bulletItems.isEmpty()) {
            int[] item = bulletItems.pop();
            applyBulletSpan(item[0], item[1], out.length());
        }
        listIndent = 0;
    }

    public void beginHref(Object ctx, Object link) {
        links.push(link);
    }

    public void endHref(Object ctx) {
        if (!links.isEmpty()) links.pop();
    }

    public void beginCodeBlock(Object ctx, String language) {
        codeStart = out.length();
    }

    public void endCodeBlock(Object ctx) {
        if (codeStart >= out.length()) return;
        out.setSpan(new TypefaceSpan("monospace"), codeStart, out.length(), SPAN_FLAGS);
        out.setSpan(new BackgroundColorSpan(Theme.codeBackground), codeStart, out.length(),
            SPAN_FLAGS);
        out.setSpan(new ForegroundColorSpan(resources
                .getColor(Settings.lightTheme ? android.R.color.black : android.R.color.white)),
            codeStart, out.length(), SPAN_FLAGS);
        codeStart = -1;
    }

    public void beginBlockQuote(Object ctx) {
        quoteStarts.push(out.length());
    }

    public void endBlockQuote(Object ctx) {
        int start = quoteStarts.pop();
        if (start >= out.length()) return;
        // Outranks bullets so quote bar is outermost; enclosing quotes outrank nested ones for
        // left-to-right bar stacking.
        int depth = quoteStarts.size() + 1;
        int priority = Math.max(QUOTE_SPAN_PRIORITY_FLOOR, QUOTE_SPAN_PRIORITY - (depth - 1));
        out.setSpan(new QuoteSpan(Theme.quoteColor), start, out.length(),
            SPAN_FLAGS | (priority << Spanned.SPAN_PRIORITY_SHIFT));
    }

    public void beginHeader(Object ctx, int n) {
        // Header gets parsed as large+bold via append(), don't do anything
    }

    public void endHeader(Object ctx, int n) {
    }

    public void beginSubtext(Object ctx) {
        subtextStart = out.length();
    }

    public void endSubtext(Object ctx) {
        if (subtextStart >= out.length()) return;
        out.setSpan(new TextAppearanceSpan(context, android.R.style.TextAppearance_Small),
            subtextStart, out.length(), SPAN_FLAGS);
        subtextStart = -1;
    }

    public void beginSpoiler(Object ctx) {
        spoilerStart = out.length();
    }

    public void endSpoiler(Object ctx) {
        if (spoilerStart >= out.length()) return;
        out.setSpan(new BackgroundColorSpan(Theme.codeBackground), spoilerStart, out.length(),
            SPAN_FLAGS);
        out.setSpan(new ForegroundColorSpan(resources
                .getColor(Settings.lightTheme ? android.R.color.black : android.R.color.white)),
            spoilerStart, out.length(), SPAN_FLAGS);
        final ForegroundColorSpan hide = new ForegroundColorSpan(Theme.codeBackground);
        out.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                try {
                    Spannable text = (Spannable) ((TextView) widget).getText();
                    text.removeSpan(hide);
                    text.removeSpan(this);
                    widget.invalidate();
                }
                catch (Exception ignored) {
                }
            }
        }, spoilerStart, out.length(), SPAN_FLAGS);
        out.setSpan(hide, spoilerStart, out.length(), SPAN_FLAGS);
        spoilerStart = -1;
    }

    public void beginList(Object ctx, boolean ordered) {
        listIndent++;
    }

    public void endList(Object ctx, boolean ordered) {
        listIndent--;
    }

    private void dropIndent(int indent) {
        int removed = out.length() - indent;
        if (removed == 0) return;
        out.delete(indent, out.length());
        // Plain offsets; SpannableStringBuilder won't rebase them automatically.
        codeStart = rebase(codeStart, indent, removed);
        subtextStart = rebase(subtextStart, indent, removed);
        spoilerStart = rebase(spoilerStart, indent, removed);
        for (int i = 0; i < quoteStarts.size(); i++) {
            quoteStarts.set(i, rebase(quoteStarts.get(i), indent, removed));
        }
        for (int[] item : bulletItems) {
            item[0] = rebase(item[0], indent, removed);
        }
    }

    public void beginListItem(Object ctx, int n) {
        // Drop leading whitespace; BulletSpan draws at line start.
        // nested items indent via enclosing BulletSpan margins.
        int indent = out.length();
        while (indent > 0) {
            char c = out.charAt(indent - 1);
            if (c != ' ' && c != '\t') break;
            indent--;
        }
        dropIndent(indent);

        // BulletSpan only draws at line start; indentation already removed.
        if (n == 0) {
            bulletItems.push(new int[]{out.length(), listIndent});
        }
        else {
            // Ordered item: keep the number prefix, no BulletSpan.
            // Push a marker so begin/end stays balanced.
            out.append(n + ". ");
            bulletItems.push(new int[]{-1, listIndent});
        }
    }

    public void endListItem(Object ctx) {
        int[] item = bulletItems.pop();
        applyBulletSpan(item[0], item[1], out.length());
    }

    public void append(Object ctx, String text, int font) {
        if (hasContent(text)) emojiOnly = false;
        appendStyled(text, font, links.isEmpty() ? null : links.peek());
    }

    public void appendLink(Object ctx, String text, int font) {
        if (hasContent(text)) emojiOnly = false;
        // Bare URL in text: the text itself is the link
        appendStyled(text, font, links.isEmpty() ? text : links.peek());
    }

    public void appendUserMention(Object ctx, String id, int font) {
        emojiOnly = false;
        appendMentionStyled("@" + message.mentions.get(id), font);
    }

    public void appendRoleMention(Object ctx, String id, int font) {
        emojiOnly = false;
        appendMentionStyled("@" + id, font); // TODO: parse roles
    }

    public void appendChannelMention(Object ctx, String id, int font) {
        emojiOnly = false;
        appendMentionStyled("#" + id, font); // TODO: parse channels
    }

    public void appendEveryone(Object ctx, int font) {
        emojiOnly = false;
        appendMentionStyled("@everyone", font);
    }

    public void appendHere(Object ctx, int font) {
        emojiOnly = false;
        appendMentionStyled("@here", font);
    }

    public void appendDiscordEmoji(Object ctx, String name, String id, boolean animated, int font) {
        emojiCount++;
        appendEmojiText(":" + name + ":", font,
            EmojiSpan.custom(context, view, name, cdnUrl, id, animated));
    }

    public void appendUnicodeEmoji(Object ctx, String emoji, String codepoints, int font) {
        emojiCount++;
        appendEmojiText(emoji, font, EmojiSpan.unicode(context, view, emoji, codepoints));
    }

    private void appendEmojiText(String text, int font, EmojiSpan span) {
        int start = out.length();
        out.append(text);
        int end = out.length();
        if (start == end) return;

        applyFontSpans(start, end, font);
        if (span != null) out.setSpan(span, start, end, SPAN_FLAGS);
        if (!links.isEmpty()) {
            applyLinkSpan(start, end, links.peek());
        }
    }

    public void appendTimestamp(Object ctx, String timestamp, String style, int font) {
        emojiOnly = false;
        String formatted = formatDiscordTimestamp(timestamp, style);
        int start = out.length();
        out.append(formatted);
        int end = out.length();
        if (start == end) return;
        applyFontSpans(start, end, font);
        out.setSpan(new BackgroundColorSpan(Theme.codeBackground), start, end, SPAN_FLAGS);
        out.setSpan(new ForegroundColorSpan(resources
                .getColor(Settings.lightTheme ? android.R.color.black : android.R.color.white)), start,
            end, SPAN_FLAGS);
        if (!links.isEmpty()) {
            applyLinkSpan(start, end, links.peek());
        }
    }

    public void appendInlineSpace(Object ctx, int font) {
        out.append(' ');
    }

    public void appendImage(Object ctx, String src, String alt) {
    }

    public void lineBreak(Object ctx) {
    }

    public void horizontalLine(Object ctx) {
    }

    private static class EmojiSpan extends ReplacementSpan implements Drawable.Callback {
        private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
        private final RectF rect = new RectF();
        private final TextPaint textPaint = new TextPaint();
        private final String fallbackText;
        private boolean big = false;
        private int size;
        private Bitmap bitmap;
        private GifDrawable gif;
        private TextView view;
        private boolean gifStopping;

        private EmojiSpan(String fallbackText) {
            this.fallbackText = fallbackText;
        }

        static EmojiSpan unicode(Context context, TextView view, String emoji, String codepoints) {
            EmojiSpan span = new EmojiSpan(emoji);
            String stripped = codepoints.indexOf("-fe0f") == -1 ? null
                : assetUrl(codepoints.replace("-fe0f", ""));
            span.load(context, view, assetUrl(codepoints), stripped);
            return span;
        }

        static EmojiSpan custom(Context context, TextView view, String name, String cdnUrl,
            String id, boolean animated) {
            EmojiSpan span = new EmojiSpan(name);
            String pngUrl = cdnUrl + "/emojis/" + id + ".png?size=64";
            span.load(context, view, animated ? cdnUrl + "/emojis/" + id + ".gif?size=64" : pngUrl,
                animated ? pngUrl : null);
            return span;
        }

        private static String assetUrl(String codepoints) {
            return "file:///android_asset/twemoji/" + codepoints + ".png";
        }

        private void load(Context context, TextView view, String url, String fallbackUrl) {
            DiskCacheStrategy strategy =
                url.startsWith("file:") ? DiskCacheStrategy.NONE : DiskCacheStrategy.RESULT;
            Glide.with(context).load(url).diskCacheStrategy(strategy).thumbnail(0.5f)
                .into(new EmojiTarget(this, view, fallbackUrl));
        }

        void setGif(GifDrawable drawable, TextView view) {
            this.gif = drawable;
            this.view = view;
            drawable.setCallback(this);
            drawable.start();
        }

        private void stopGif() {
            if (gifStopping || gif == null) return;
            gifStopping = true;
            gif.stop();
            gifStopping = false;
        }

        public void invalidateDrawable(Drawable who) {
            // The view may have been recycled to another message; stop to save CPU
            if (!isIn(view)) {
                gif.setCallback(null);
                stopGif();
                return;
            }
            // Pause until the next draw instead of decoding frames in the background
            // (View.OnAttachStateChangeListener does not exist before API 12)
            if (view.getWindowVisibility() != View.VISIBLE) {
                stopGif();
                return;
            }
            view.postInvalidate();
        }

        public void scheduleDrawable(Drawable who, Runnable what, long when) {
            view.postDelayed(what, Math.max(0, when - SystemClock.uptimeMillis()));
        }

        public void unscheduleDrawable(Drawable who, Runnable what) {
            view.removeCallbacks(what);
        }

        private boolean isIn(TextView view) {
            CharSequence text = view.getText();
            return text instanceof Spanned && ((Spanned) text).getSpanStart(this) != -1;
        }

        void setBig(boolean big) {
            this.big = big;
        }

        @Override
        public int getSize(Paint paint, CharSequence text, int start, int end,
            Paint.FontMetricsInt fm) {
            size = (int) (paint.getTextSize() * (big ? 2.4f : 1.2f) + 0.5f);
            if (fm != null) {
                if (big) {
                    fm.ascent = -size;
                    fm.top = -size;
                }
                else {
                    paint.getFontMetricsInt(fm);
                }
            }
            return size;
        }

        @Override
        public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top,
            int y, int bottom, Paint paint) {
            int imageTop = top + (bottom - top - size) / 2;
            if (gif != null) {
                // Resume animation if it was paused while the window was hidden.
                if (!gif.isRunning() && !gifStopping &&
                    view.getWindowVisibility() == View.VISIBLE && isIn(view)) {
                    gif.start();
                }
                gif.setBounds((int) x, imageTop, (int) x + size, imageTop + size);
                gif.draw(canvas);
            }
            else if (bitmap != null) {
                rect.set(x, imageTop, x + size, imageTop + size);
                canvas.drawBitmap(bitmap, null, rect, bitmapPaint);
            }
            else {
                textPaint.set(paint);
                float width = textPaint.measureText(fallbackText);
                if (width > size) textPaint.setTextSize(textPaint.getTextSize() * size / width);
                Paint.FontMetricsInt fm = textPaint.getFontMetricsInt();
                canvas.drawText(fallbackText, x, imageTop + (size - fm.ascent - fm.descent) / 2f,
                    textPaint);
            }
        }
    }

    private static class EmojiTarget extends SimpleTarget<GlideDrawable> {
        private final EmojiSpan span;
        private final TextView view;
        private final String fallbackUrl;

        EmojiTarget(EmojiSpan span, TextView view, String fallbackUrl) {
            this.span = span;
            this.view = view;
            this.fallbackUrl = fallbackUrl;
        }

        @Override
        public void onResourceReady(GlideDrawable resource,
            GlideAnimation<? super GlideDrawable> glideAnimation) {
            if (resource instanceof GifDrawable) {
                GifDrawable gif = (GifDrawable) resource;
                Bitmap firstFrame = gif.getFirstFrame();
                Bitmap.Config config = firstFrame.getConfig();
                if (config == null) config = Bitmap.Config.ARGB_8888;
                span.setGif(new GifDrawable(gif, firstFrame.copy(config, false),
                    gif.getFrameTransformation()), view);
            }
            else if (resource instanceof GlideBitmapDrawable) {
                span.bitmap = ((GlideBitmapDrawable) resource).getBitmap();
            }
            view.postInvalidate();
        }

        @Override
        public void onLoadFailed(Exception e, Drawable errorDrawable) {
            if (fallbackUrl != null) {
                span.load(view.getContext(), view, fallbackUrl, null);
            }
        }
    }

    // endregion
}
