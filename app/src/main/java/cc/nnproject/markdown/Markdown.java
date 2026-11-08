/*
Copyright (c) 2026 Arman Jussupgaliyev

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
package cc.nnproject.markdown;

import java.util.Hashtable;
import java.util.Stack;

/**
 * Markdown parser library
 *
 * @author Shinovon
 * @version 0.6
 */
public class Markdown {
    public static final int FONT_SIZE_SMALL = 1 << 3;
    public static final int FONT_SIZE_MEDIUM = 0;
    public static final int FONT_SIZE_LARGE = 1 << 4;

    public static final int FONT_STYLE_PLAIN = 0;
    public static final int FONT_STYLE_BOLD = 1;
    public static final int FONT_STYLE_ITALIC = 1 << 1;
    public static final int FONT_STYLE_UNDERLINED = 1 << 2;
    public static final int FONT_STYLE_STRIKETHROUGH = 1 << 8;

    public static final int FONT_FACE_SYSTEM = 0;
    public static final int FONT_FACE_MONOSPACE = 1 << 5;

    /**
     * Mask for passing font to MIDP Font.getFont() as first parameter
     */
    public static final int MIDP_FONT_FACE_MASK = FONT_FACE_SYSTEM | FONT_FACE_MONOSPACE;
    /**
     * Mask for passing font to MIDP Font.getFont() as second parameter
     */
    public static final int MIDP_FONT_STYLE_MASK =
        FONT_STYLE_PLAIN | FONT_STYLE_BOLD | FONT_STYLE_ITALIC | FONT_STYLE_UNDERLINED;
    /**
     * Mask for passing font to MIDP Font.getFont() as third parameter
     */
    public static final int MIDP_FONT_SIZE_MASK =
        FONT_SIZE_SMALL | FONT_SIZE_MEDIUM | FONT_SIZE_LARGE;

    private static final int MD_FONT_FACE = 0, MD_FONT_STYLE = 1, MD_FONT_SIZE = 2, MD_TAB = 3,
        MD_SPACES = 4, MD_LAST_TAB = 5, MD_ESCAPE = 6, MD_GT = 7, MD_HEADER = 8, MD_LENGTH = 9,
        MD_ITALIC = 10, MD_BOLD = 11, MD_UNDERLINE = 12, MD_HTML_BOLD = 13, MD_HTML_ITALIC = 14,
        MD_LINE = 15, MD_HTML_PARAGRAPH = 16, MD_HTML_UNDERLINE = 17, MD_HTML_HEADER = 18,
        MD_UNDERSCORE = 19, MD_HASH = 20, MD_GRAVE = 21, MD_STRIKE = 22, MD_ASTERISK = 23,
        MD_HTML_BIG = 24, MD_HTML_LINK = 25, MD_BRACKET = 26, MD_LINK = 27, MD_IMAGE = 28,
        MD_PARENTHESIS = 29, MD_PARAGRAPH = 30, MD_BREAKS = 31, MD_HTML_STRIKE = 32, MD_BLOCKQUOTE =
        33, MD_LAST_GT = 34, MD_SUBTEXT = 35, MD_DISCORD_SPOILER = 36, MD_DISCORD_MENTION = 37,
        MD_DISCORD_TIMESTAMP = 38, MD_DISCORD_EMOJI = 39, MD_COUNT = 40;
    /**
     * Zero width joiner
     */
    private static final int ZERO_WIDTH_JOINER = 0x200D;
    /**
     * Variation selector-16, requests emoji presentation
     */
    private static final int VARIATION_SELECTOR_16 = 0xFE0F;
    /**
     * Variation selector-15, requests text presentation
     */
    private static final int VARIATION_SELECTOR_15 = 0xFE0E;
    /**
     * Combining enclosing keycap, makes digits into emoji
     */
    private static final int ENCLOSING_KEYCAP = 0x20E3;
    private static final int REGIONAL_INDICATOR_FIRST = 0x1F1E6;
    private static final int REGIONAL_INDICATOR_LAST = 0x1F1FF;
    private static final int EMOJI_MODIFIER_FIRST = 0x1F3FB;
    private static final int EMOJI_MODIFIER_LAST = 0x1F3FF;
    private static final int EMOJI_TAG_FIRST = 0xE0020;
    /**
     * Tag sequences (subdivision flags) end with the cancel tag
     */
    private static final int EMOJI_TAG_LAST = 0xE007F;
    private static final int[] EMOJI_PICTOGRAPHICS =
        {0xA9, 0xA9, 0xAE, 0xAE, 0x203C, 0x203C, 0x2049, 0x2049, 0x2122, 0x2122, 0x2139, 0x2139,
            0x2194, 0x2199, 0x21A9, 0x21AA, 0x231A, 0x231B, 0x2328, 0x2328, 0x2388, 0x2388, 0x23CF,
            0x23CF, 0x23E9, 0x23F3, 0x23F8, 0x23FA, 0x24C2, 0x24C2, 0x25AA, 0x25AB, 0x25B6, 0x25B6,
            0x25C0, 0x25C0, 0x25FB, 0x25FE, 0x2600, 0x2605, 0x2607, 0x2612, 0x2614, 0x2685, 0x2690,
            0x2705, 0x2708, 0x2712, 0x2714, 0x2714, 0x2716, 0x2716, 0x271D, 0x271D, 0x2721, 0x2721,
            0x2728, 0x2728, 0x2733, 0x2734, 0x2744, 0x2744, 0x2747, 0x2747, 0x274C, 0x274C, 0x274E,
            0x274E, 0x2753, 0x2755, 0x2757, 0x2757, 0x2763, 0x2767, 0x2795, 0x2797, 0x27A1, 0x27A1,
            0x27B0, 0x27B0, 0x27BF, 0x27BF, 0x2934, 0x2935, 0x2B05, 0x2B07, 0x2B1B, 0x2B1C, 0x2B50,
            0x2B50, 0x2B55, 0x2B55, 0x3030, 0x3030, 0x303D, 0x303D, 0x3297, 0x3297, 0x3299, 0x3299,
            0x1F000, 0x1F0FF, 0x1F10D, 0x1F10F, 0x1F12F, 0x1F12F, 0x1F16C, 0x1F171, 0x1F17E,
            0x1F17F, 0x1F18E, 0x1F18E, 0x1F191, 0x1F19A, 0x1F1AD, 0x1F1E5, 0x1F201, 0x1F20F,
            0x1F21A, 0x1F21A, 0x1F22F, 0x1F22F, 0x1F232, 0x1F23A, 0x1F23C, 0x1F23F, 0x1F249,
            0x1F3FA, 0x1F400, 0x1F53D, 0x1F546, 0x1F64F, 0x1F680, 0x1F6FF, 0x1F774, 0x1F77F,
            0x1F7D5, 0x1F7FF, 0x1F80C, 0x1F80F, 0x1F848, 0x1F84F, 0x1F85A, 0x1F85F, 0x1F888,
            0x1F88F, 0x1F8AE, 0x1F8FF, 0x1F90C, 0x1F93A, 0x1F93C, 0x1F945, 0x1F947, 0x1FAFF,
            0x1FC00, 0x1FFFD,};
    /**
     * Enables {@link #FONT_STYLE_STRIKETHROUGH} font style
     */
    public static boolean enableStrikethroughFont = false;
    /**
     * Enables highlighting of URLs
     */
    public static boolean enableLinksSearching = true;
    /**
     * Enables highlighting of words starting with @
     */
    public static boolean enableHashtagLinks = false;
    /**
     * Enables highlighting of words starting with #
     */
    public static boolean enableTagLinks = false;
    /**
     * Enables parsing of blockquotes
     */
    public static boolean enableBlockquotes = true;
    /**
     * Enables parsing of lists
     */
    public static boolean enableLists = true;
    /**
     * Enables parsing of HTML tags
     */
    public static boolean enableHtmlTags = true;
    /**
     * Enables parsing of horizontal lines
     */
    public static boolean enableHorizontalLines = true;
    /**
     * Enables parsing of images
     */
    public static boolean enableImages = true;
    /**
     * Enables parsing of Unicode emojis
     */
    public static boolean enableEmojis = true;
    /**
     * Make monospace text have bold style
     */
    public static boolean monospaceIsBold = false;
    public static boolean breakOnNewLine = false;
    /**
     * Enables parsing of Discord-specific elements (subtext -#, spoiler ||...||,
     * mentions &lt;@id&gt;/&lt;@&amp;id&gt;/&lt;#id&gt;, @everyone/@here,
     * emoji &lt;:name:id&gt;, timestamps &lt;t:unix:style&gt;).
     */
    public static boolean enableDiscordParsing = true;

    /**
     * @param ui   Parsed Markdown handler
     * @param ctx  Context passed to handler
     * @param src  Source text
     * @param urls URLs table for output, can be null
     */
    public static void parse(MarkdownListener ui, Object ctx, String src, Hashtable urls) {
        if (src == null) {
            ui.beginMarkdown(ctx);
            ui.endMarkdown(ctx);
            return;
        }
        StringBuffer sb = new StringBuffer();
        int d = enableHtmlTags ? src.indexOf('<') : -1;
        int len = src.length();
        if (len == 0) {
            ui.beginMarkdown(ctx);
            ui.endMarkdown(ctx);
            return;
        }
        int o = 0;
        int[] state = new int[MD_COUNT];
        state[MD_FONT_SIZE] = FONT_SIZE_SMALL;
        Stack linksStack = new Stack();
        Stack listsStack = new Stack();

        ui.beginMarkdown(ctx);
        try {
            char[] chars = src.toCharArray();
            char l = 0;
            while (d != -1 || o < len) {
                a:
                {
                    if (o != d) {
                        if (d == -1) d = len;
                        int i;
                        for (i = o; i < d; ++i) {
                            char c = chars[i];
                            if (state[MD_HASH] != 0 &&
                                ((c != '#' && c != ' ') || state[MD_HASH] > 6)) {
                                for (int k = 0; k < state[MD_HASH]; ++k) sb.append('#');
                                state[MD_LENGTH] += state[MD_HASH];
                                state[MD_HASH] = 0;
                            }
                            if (c == '\r' || (c == '\n' && l != '\r')) {
                                l = c;
                                boolean b;
                                boolean escape = state[MD_ESCAPE] == 1;
                                if ((b = state[MD_HEADER] != 0 || state[MD_SUBTEXT] != 0) ||
                                    state[MD_LENGTH] != 0 || escape) {
                                    if (b) {
                                        sb.append('\n');
                                        flush(ctx, ui, sb, state);
                                        state[MD_PARAGRAPH] = 1;
                                    }
                                    state[MD_LAST_TAB] = state[MD_TAB];
                                    state[MD_UNDERSCORE] = 0;
                                    state[MD_HASH] = 0;
                                    state[MD_BOLD] = 0;
                                    state[MD_UNDERLINE] = 0;
                                    state[MD_ITALIC] = 0;
                                    state[MD_HEADER] = 0;
                                    state[MD_SUBTEXT] = 0;
                                    state[MD_ESCAPE] = 0;
                                    state[MD_LENGTH] = 0;
                                    state[MD_SPACES] = 0;
                                    state[MD_TAB] = 0;
                                    state[MD_GRAVE] = 0;
                                    state[MD_STRIKE] = 0;
                                    state[MD_ASTERISK] = 0;
                                    state[MD_BRACKET] = 0;
                                    state[MD_LINK] = 0;
                                    state[MD_IMAGE] = 0;
                                    state[MD_PARENTHESIS] = 0;
                                    state[MD_BREAKS]++;

                                    if (!b) {
                                        sb.append(escape || breakOnNewLine ? '\n' : ' ');
                                        state[MD_PARAGRAPH] = 0;
                                    }
                                }
                                else if (state[MD_LENGTH] == 0 && state[MD_PARAGRAPH] == 0) {
                                    flush(ctx, ui, sb, state);
                                    int j = state[MD_LAST_GT] - state[MD_GT];
                                    while (state[MD_BLOCKQUOTE] != 0 && j != 0) {
                                        ui.endBlockQuote(ctx);
                                        state[MD_BLOCKQUOTE]--;
                                        j--;
                                    }

                                    while (!listsStack.isEmpty()) {
                                        ui.endListItem(ctx);
                                        ui.endList(ctx, ((int[]) listsStack.pop())[0] == 0);
                                    }

                                    ui.lineBreak(ctx);
                                    state[MD_PARAGRAPH] = 1;
                                }

                                state[MD_LAST_GT] = state[MD_GT];
                                state[MD_GT] = 0;
                                continue;
                            }
                            else if (c <= ' ' && l <= ' ') {
                                if ((l == c) &&
                                    (state[MD_SPACES]++ == 0 || state[MD_SPACES] == 2 + 1)) {
                                    state[MD_TAB]++;
                                    if (state[MD_SPACES] != 1) state[MD_SPACES] = 0;
                                }
                                l = c;
                                continue;
                            }
                            else if (state[MD_PARENTHESIS] != 0 && c != ')') {
                                l = c;
                                sb.append(c);
                                continue;
                            }
                            else {
                                state[MD_SPACES] = 0;
                                if (c == '&' && i + 1 != len) { // entity
                                    switch (chars[i + 1]) {
                                        case 'n': // nbsp;
                                            if (i + 5 < len && chars[i + 2] == 'b' &&
                                                chars[i + 3] == 's' && chars[i + 4] == 'p' &&
                                                chars[i + 5] == ';') {
                                                c = ' ';
                                                i += 5;
                                            }
                                            break;
                                        case 'l': // lt
                                            if (i + 3 < len && chars[i + 2] == 't' &&
                                                chars[i + 3] == ';') {
                                                c = '<';
                                                i += 3;
                                            }
                                            break;
                                        case 'g': // gt
                                            if (i + 3 < len && chars[i + 2] == 't' &&
                                                chars[i + 3] == ';') {
                                                c = '>';
                                                i += 3;
                                            }
                                            break;
                                        case 'a': // amp
                                            if (i + 4 < len && chars[i + 2] == 'm' &&
                                                chars[i + 3] == 'p' && chars[i + 4] == ';') {
                                                c = '&';
                                                i += 4;
                                            }
                                            break;
                                        case '#':
                                            int k = i + 2;
                                            boolean hex = chars[k] == 'x';
                                            if (hex) ++k;
                                            try {
                                                //noinspection StatementWithEmptyBody
                                                while (chars[++k] != ';' && k - i < 10) ;
                                                if (k - i == 10) break;
                                                if (hex) {
                                                    c = (char) Integer.parseInt(
                                                        new String(chars, i + 3, k - i - 3), 16);
                                                }
                                                else {
                                                    c = (char) Integer.parseInt(
                                                        new String(chars, i + 2, k - i - 2));
                                                }
                                                i = k;
                                            }
                                            catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                            break;
                                    }
                                }
                                else if (state[MD_ESCAPE] == 0) {
                                    if ((c == '*' || c == '-') && i + 1 < len &&
                                        chars[i + 1] == ' ' && state[MD_TAB] != 0) {
                                        int tabs = state[MD_TAB];
                                        while (tabs-- != 0) {
                                            sb.append("  ");
                                        }
                                    }
                                    if (state[MD_LENGTH] == 0) {
                                        if (!listsStack.isEmpty() &&
                                            !((c == '*' || c == '+' || c == '-') && i + 1 < len &&
                                                chars[i + 1] == ' ')) {
                                            int lineTab = state[MD_TAB];
                                            while (!listsStack.isEmpty() &&
                                                lineTab <= ((int[]) listsStack.peek())[1]) {
                                                flush(ctx, ui, sb, state);
                                                ui.endListItem(ctx);
                                                int[] lt = (int[]) listsStack.pop();
                                                ui.endList(ctx, lt[0] == 0);
                                            }
                                        }
                                        if (state[MD_GT] != 0 && c != '>') {
                                            flush(ctx, ui, sb, state);
                                            if (!enableBlockquotes) {
                                                ui.lineBreak(ctx);
                                                for (int j = 0; j < state[MD_GT]; ++j) {
                                                    sb.append('>');
                                                }
                                            }
                                            else if (state[MD_GT] > state[MD_BLOCKQUOTE]) {
                                                ui.lineBreak(ctx);
                                                while (state[MD_GT] > state[MD_BLOCKQUOTE]) {
                                                    state[MD_BLOCKQUOTE]++;
                                                    ui.beginBlockQuote(ctx);
                                                }
                                            }
                                            else {
                                                while (state[MD_BLOCKQUOTE] > state[MD_GT]) {
                                                    ui.endBlockQuote(ctx);
                                                    state[MD_BLOCKQUOTE]--;
                                                }
                                            }
                                        }
                                        else if (state[MD_GT] == 0 && state[MD_BLOCKQUOTE] != 0 &&
                                            c != '>' && c > ' ') {
                                            flush(ctx, ui, sb, state);
                                            if (enableBlockquotes) {
                                                while (state[MD_BLOCKQUOTE] != 0) {
                                                    ui.endBlockQuote(ctx);
                                                    state[MD_BLOCKQUOTE]--;
                                                }
                                            }
                                        }

                                        if ((c == '*' || c == '+' || c == '-') && i + 1 < len &&
                                            chars[i + 1] == ' ') {
                                            // list item
                                            // TODO ordered
                                            i++;
                                            int tab = state[MD_TAB];

                                            while (!listsStack.empty()) {
                                                int[] t = (int[]) listsStack.peek();
                                                if (tab > t[1]) break;

                                                flush(ctx, ui, sb, state);
                                                ui.endListItem(ctx);
                                                ui.lineBreak(ctx);
                                                if ((tab == t[1] && c != t[0]) || t[1] > tab) {
                                                    ui.endList(ctx, t[0] == 0);
                                                    listsStack.pop();
                                                    continue;
                                                }
                                                break;
                                            }

                                            int[] t;
                                            if (listsStack.empty() ||
                                                tab > (t = (int[]) listsStack.peek())[1]) {
                                                flush(ctx, ui, sb, state);
                                                listsStack.push(t = new int[]{c, tab, 0});
                                                ui.lineBreak(ctx);
                                                ui.beginList(ctx, false);
                                            }

                                            if (!enableLists) {
                                                for (int j = 0; j < tab; ++j) sb.append("  ");
                                                if (t[0] == 0) {
                                                    sb.append(++t[2]).append(". ");
                                                }
                                                else {
                                                    sb.append("* ");
                                                }
                                            }
                                            ui.beginListItem(ctx, t[0] == 0 ? t[2] : 0);

                                            state[MD_LENGTH]++;
                                            continue;
                                        }
                                    }

                                    if (enableEmojis) {
                                        int end = emojiEnd(chars, i, len);
                                        if (end != -1) {
                                            flush(ctx, ui, sb, state);
                                            ui.appendUnicodeEmoji(ctx, src.substring(i, end),
                                                emojiCodepoints(chars, i, end), getFont(state));
                                            i = end - 1;
                                            l = chars[i];
                                            state[MD_LENGTH]++;
                                            continue;
                                        }
                                    }

                                    switch (c) {
                                        case '\t':
                                            c = ' ';
                                            break;
                                        case '\\': // escape
                                            state[MD_ESCAPE] = 1;
                                            l = c;
                                            continue;
                                        case ' ':
                                            if (state[MD_HASH] != 0) {
                                                flush(ctx, ui, sb, state);
                                                l = c;
                                                state[MD_HEADER] = state[MD_HASH];
                                                state[MD_HASH] = 0;
                                                continue;
                                            }
                                            if (state[MD_LENGTH] == 0) {
                                                l = c;
                                                continue;
                                            }
                                            break;
                                        case '>':
                                            if (state[MD_LENGTH] == 0) {
                                                state[MD_GT]++;
                                                l = c;
                                                continue;
                                            }
                                            break;
                                        case '#':
                                            if (state[MD_LENGTH] == 0 && state[MD_HEADER] == 0) {
                                                state[MD_HASH]++;
                                                l = c;
                                                continue;
                                            }
                                            break;
                                        case '-': {
                                            if (state[MD_LENGTH] == 0 && state[MD_HEADER] == 0 &&
                                                state[MD_SUBTEXT] == 0 && state[MD_GRAVE] == 0) {
                                                if (i + 2 < len && chars[i + 1] == '#' &&
                                                    chars[i + 2] == ' ') {
                                                    flush(ctx, ui, sb, state);
                                                    state[MD_SUBTEXT] = 1;
                                                    state[MD_LENGTH]++;
                                                    i += 2;
                                                    l = ' ';
                                                    continue;
                                                }
                                            }
                                            if (enableHorizontalLines && state[MD_LENGTH] == 0) {
                                                if (i + 2 < len && chars[i + 1] == c &&
                                                    chars[i + 2] == c) {
                                                    int k = i;
                                                    //noinspection StatementWithEmptyBody
                                                    while (++k < len && chars[k] != '\n' &&
                                                        chars[k] != '\r') {
                                                        ;
                                                    }
                                                    if (chars[k - 1] == c) {
                                                        i = k - 1;
                                                        state[MD_LINE]++;
                                                        flush(ctx, ui, sb, state);
                                                        continue;
                                                    }
                                                }
                                            }
                                            break;
                                        }
                                        case '*':
                                        case '_': {
                                            int t = c == '*' ? MD_ASTERISK : MD_UNDERSCORE;
                                            if (enableHorizontalLines && state[t] == 0 &&
                                                state[MD_LENGTH] == 0 && i + 2 < len &&
                                                chars[i + 1] == c && chars[i + 2] == c) {
                                                int k = i;
                                                //noinspection StatementWithEmptyBody
                                                while (++k < len && chars[k] != '\n' &&
                                                    chars[k] != '\r') {
                                                    ;
                                                }
                                                if (chars[k - 1] == c) {
                                                    i = k - 1;
                                                    state[MD_LINE]++;
                                                    flush(ctx, ui, sb, state);
                                                    continue;
                                                }
                                            }

                                            if (state[t] == 0 &&
                                                ((c == '_' && l > ' ' && l != '*') ||
                                                    i + 1 >= len)) {
                                                break;
                                            }
                                            int k = i; // line length
                                            //noinspection StatementWithEmptyBody
                                            while (++k < len && chars[k] != '\n' &&
                                                chars[k] != '\r') {
                                                ;
                                            }

                                            if (state[t] == 0) {
                                                if (i + 2 >= k) break;
                                                int j = src.indexOf(c, i + 1);
                                                if (j == -1 || j >= k) break;
                                            }

                                            l = c;
                                            if (i + 1 < k && chars[i + 1] == c) {
                                                String s;
                                                if (i + 2 < k && chars[i + 2] == c) {
                                                    s = c == '*' ? "***" : "___";
                                                    if (state[t] == 3) {
                                                        flush(ctx, ui, sb, state);
                                                        state[t] = 0;
                                                        state[c == '_' ? MD_UNDERLINE : MD_BOLD]--;
                                                        state[MD_ITALIC]--;
                                                        i += 2;
                                                        continue;
                                                    }
                                                    else if (state[t] == 0) {
                                                        int j = src.indexOf(s, i + 1);
                                                        if (i + 6 >= k || j == -1 || j >= k ||
                                                            chars[i + 3] <= ' ' ||
                                                            (j + 3 != k && chars[j + 3] > ' ')) {
                                                            sb.append(s);
                                                            i += 2;
                                                            continue;
                                                        }
                                                        flush(ctx, ui, sb, state);
                                                        state[t] = 3;
                                                        state[c == '_' ? MD_UNDERLINE : MD_BOLD]++;
                                                        state[MD_ITALIC]++;
                                                        i += 2;
                                                        continue;
                                                    }
                                                    sb.append(c).append(c);
                                                    i += 3;
                                                    break;
                                                }
                                                s = c == '*' ? "**" : "__";
                                                if (state[t] == 2) {
                                                    flush(ctx, ui, sb, state);
                                                    state[t] = 0;
                                                    state[c == '_' ? MD_UNDERLINE : MD_BOLD]--;
                                                    i++;
                                                    continue;
                                                }
                                                else if (state[t] == 0) {
                                                    int j = src.indexOf(s, i + 1);
                                                    if (i + 4 >= k || j == -1 || j >= k ||
                                                        chars[i + 2] <= ' ' ||
                                                        chars[j - 1] <= ' ' ||
                                                        (c == '_' && j + 2 != k &&
                                                            chars[j + 2] > ' ' &&
                                                            chars[j + 2] != '*')) {
                                                        sb.append(s);
                                                        i++;
                                                        continue;
                                                    }
                                                    flush(ctx, ui, sb, state);
                                                    state[t] = 2;
                                                    state[c == '_' ? MD_UNDERLINE : MD_BOLD]++;
                                                    i++;
                                                    continue;
                                                }
                                                sb.append(c);
                                                i++;
                                                break;
                                            }

                                            if (state[t] == 1) {
                                                flush(ctx, ui, sb, state);
                                                state[t] = 0;
                                                state[MD_ITALIC] = 0;
                                                continue;
                                            }

                                            if (state[t] == 0) {
                                                int j = src.indexOf(c, i + 1);
                                                if (j == -1 || j >= k || chars[i] <= ' ' ||
                                                    (c == '_' && j + 1 != k && chars[j + 1] > ' ' &&
                                                        chars[j + 1] != '*')) {
                                                    break;
                                                }
                                                flush(ctx, ui, sb, state);
                                                state[t] = 1;
                                                state[MD_ITALIC] = 1;
                                                state[MD_LENGTH]++;
                                                continue;
                                            }
                                            break;
                                        }
                                        case '~': {
                                            if (i + 1 >= len || chars[i + 1] != c) {
                                                break;
                                            }
                                            int k = i; // line length
                                            //noinspection StatementWithEmptyBody
                                            while (++k < len && chars[k] != '\n' &&
                                                chars[k] != '\r') {
                                                ;
                                            }
                                            if (state[MD_STRIKE] == 0) {
                                                if (i + 4 >= k) break;
                                                int j = src.indexOf("~~", i + 1);
                                                if (j == -1 || j >= k || chars[i + 2] <= ' ' ||
                                                    chars[j - 1] <= ' ') {
                                                    break;
                                                }

                                                flush(ctx, ui, sb, state);
                                                state[MD_STRIKE] = 1;
                                                state[MD_LENGTH]++;
                                                i++;
                                                continue;
                                            }
                                            if (state[MD_STRIKE] == 1) {
                                                flush(ctx, ui, sb, state);
                                                state[MD_STRIKE] = 0;
                                                i++;
                                                continue;
                                            }
                                            continue;
                                        }
                                        case '|': {
                                            // Discord spoiler ||...||
                                            if (!enableDiscordParsing || state[MD_GRAVE] != 0) {
                                                break;
                                            }
                                            if (i + 1 >= len || chars[i + 1] != '|') {
                                                break;
                                            }
                                            if (state[MD_DISCORD_SPOILER] == 0) {
                                                int j = src.indexOf("||", i + 2);
                                                if (j == -1 || j == i + 2) {
                                                    break;
                                                }
                                                flush(ctx, ui, sb, state);
                                                state[MD_DISCORD_SPOILER] = 1;
                                                ui.beginSpoiler(ctx);
                                                state[MD_LENGTH]++;
                                                i++;
                                                l = '|';
                                                continue;
                                            }
                                            else {
                                                flush(ctx, ui, sb, state);
                                                state[MD_DISCORD_SPOILER] = 0;
                                                ui.endSpoiler(ctx);
                                                state[MD_LENGTH]++;
                                                i++;
                                                l = '|';
                                                continue;
                                            }
                                        }
                                        case '<': {
                                            // Discord mentions, channel, emoji, timestamp
                                            // (<@id>, <@&id>, <#id>, <:name:id>, <t:unix:style>)
                                            if (!enableDiscordParsing || state[MD_GRAVE] != 0) {
                                                break;
                                            }
                                            int end =
                                                handleDiscordTag(ui, ctx, src, chars, sb, state, i,
                                                    len);
                                            if (end == -1) {
                                                break;
                                            }
                                            i = end;
                                            l = '>';
                                            continue;
                                        }
                                        case '@': {
                                            // Discord @everyone / @here
                                            if (!enableDiscordParsing || state[MD_GRAVE] != 0) {
                                                break;
                                            }
                                            if (i + 9 <= len &&
                                                src.regionMatches(i, "@everyone", 0, 9) &&
                                                (i + 9 == len ||
                                                    !isDiscordWordChar(chars[i + 9]))) {
                                                flush(ctx, ui, sb, state);
                                                ui.appendEveryone(ctx, getFont(state));
                                                state[MD_LENGTH]++;
                                                i += 8;
                                                l = 'e';
                                                continue;
                                            }
                                            if (i + 5 <= len &&
                                                src.regionMatches(i, "@here", 0, 5) &&
                                                (i + 5 == len ||
                                                    !isDiscordWordChar(chars[i + 5]))) {
                                                flush(ctx, ui, sb, state);
                                                ui.appendHere(ctx, getFont(state));
                                                state[MD_LENGTH]++;
                                                i += 4;
                                                l = 'e';
                                                continue;
                                            }
                                            break;
                                        }
                                        case '`': {
                                            if (i + 2 < len && chars[i + 1] == c &&
                                                chars[i + 2] == c) {
                                                int j = src.indexOf('\n', i + 1);
                                                int k = src.indexOf('`', i + 3);
                                                flush(ctx, ui, sb, state);
                                                i += state[MD_GRAVE] = 3;

                                                String lang = null;
                                                if (j != -1 && j < k) {
                                                    lang = src.substring(i, j);
                                                    i = j + 1;
                                                }

                                                ui.beginCodeBlock(ctx, lang);
                                            }
                                            else {
                                                int j = src.indexOf('`', i + 1);
                                                if (j == -1) break;
                                                if (i + 1 < len && chars[i + 1] == c) {
                                                    flush(ctx, ui, sb, state);
                                                    i += state[MD_GRAVE] = 2;
                                                }
                                                else {
                                                    flush(ctx, ui, sb, state);
                                                    i += state[MD_GRAVE] = 1;
                                                }
                                            }
                                            if (state[MD_GRAVE] != 0) {
                                                while (i < len) {
                                                    if ((c = chars[i++]) == '`') {
                                                        if (state[MD_GRAVE] == 1) {
                                                            break;
                                                        }
                                                        else if (state[MD_GRAVE] == 2) {
                                                            if (i < len && chars[i] == c) {
                                                                i++;
                                                                break;
                                                            }
                                                        }
                                                        else if (state[MD_GRAVE] == 3) {
                                                            if (i + 1 < len && chars[i] == c &&
                                                                chars[i + 1] == c) {
                                                                i += 2;
                                                                break;
                                                            }
                                                        }
                                                    }
                                                    else if (c <= ' ' && state[MD_GRAVE] != 3) {
                                                        if (l == ' ') continue;
                                                        c = ' ';
                                                    }
                                                    else if (c == '\t') {
                                                        l = c;
                                                        sb.append("    ");
                                                        continue;
                                                    }
                                                    l = c;
                                                    sb.append(c);
                                                }
                                                flush(ctx, ui, sb, state);
                                                if (state[MD_GRAVE] == 3) {
                                                    ui.endCodeBlock(ctx);
                                                }
                                                o = i;
                                                d = enableHtmlTags ? src.indexOf('<', o) : -1;
                                                state[MD_LENGTH]++;
                                                state[MD_GRAVE] = 0;
                                                l = '`';
                                                break a;
                                            }
                                            continue;
                                        }
                                        case '!': {
                                            if (!enableImages || i + 1 == len ||
                                                chars[i + 1] != '[') {
                                                break;
                                            }
                                            state[MD_IMAGE]++;
                                            continue;
                                        }
                                        case '[': {
                                            //									if
                                            // (state[MD_BRACKET] != 0) {
                                            //										break;
                                            //									}

                                            int k = i; // line length
                                            //noinspection StatementWithEmptyBody
                                            while (++k < len && chars[k] != '\n' &&
                                                chars[k] != '\r') {
                                                ;
                                            }

                                            int n, m;
                                            if ((n = src.indexOf(']', i)) == -1 || n >= k ||
                                                (m = src.indexOf('(', n)) == -1 || m != n + 1 ||
                                                m >= k || (m = src.indexOf(')', m)) == -1 ||
                                                m >= k) {
                                                break;
                                            }

                                            flush(ctx, ui, sb, state);

                                            Object t = new StringBuffer();
                                            linksStack.push(t);
                                            if (state[MD_IMAGE] == 0) ui.beginHref(ctx, t);

                                            l = c;
                                            state[MD_BRACKET]++;
                                            state[MD_LINK]++;
                                            state[MD_LENGTH]++;
                                            continue;
                                        }
                                        case ']': {
                                            if (state[MD_BRACKET] == 0 || i + 1 == len ||
                                                chars[i + 1] != '(') {
                                                break;
                                            }

                                            ((StringBuffer) linksStack.peek())
                                                .append(sb.toString());
                                            if (state[MD_IMAGE] == 0) {
                                                flush(ctx, ui, sb, state);
                                            }
                                            sb.setLength(0);

                                            l = c;
                                            state[MD_BRACKET]--;
                                            //									i++;
                                            continue;
                                        }
                                        case '(': {
                                            if (state[MD_LINK] == 0 || chars[i - 1] != ']') {
                                                break;
                                            }

                                            l = '(';
                                            state[MD_PARENTHESIS] = 1;
                                            continue;
                                        }
                                        case ')': {
                                            if (state[MD_LINK] == 0 || state[MD_PARENTHESIS] == 0) {
                                                break;
                                            }

                                            StringBuffer t = (StringBuffer) linksStack.pop();
                                            if (state[MD_IMAGE] != 0) {
                                                ui.appendImage(ctx, sb.toString(), t.toString());
                                                state[MD_IMAGE]--;
                                            }
                                            else if (t != null) {
                                                if (urls != null) urls.put(t, sb.toString());
                                                ui.endHref(ctx);
                                            }
                                            sb.setLength(0);

                                            l = c;
                                            state[MD_PARENTHESIS]--;
                                            state[MD_LINK]--;
                                            continue;
                                        }
                                        // TODO
//									case '1': {
//										if (state[MD_LENGTH] == 0 && i + 1 != len && chars[i + 1]
// == '.') {
//
//											continue;
//										}
//										break;
//									}
                                        default:
                                            if (c < ' ' && state[MD_LENGTH] == 0) {
                                                l = c;
                                                continue;
                                            }
                                            break;
                                    }
                                }
                                else {
                                    state[MD_ESCAPE] = 0;
                                    switch (c) {
                                        case '\t':
                                            sb.append("    ");
                                            l = c;
                                            state[MD_LENGTH]++;
                                            continue;
                                        case '\\':
                                        case '>':
                                        case '<':
                                        case '*':
                                        case '_':
                                        case '~':
                                        case '`':
                                        case '[':
                                        case ']':
                                        case '(':
                                        case ')':
                                        case '#':
                                        case '@':
                                        case '|':
                                        case ':':
                                        case '-':
                                            break;
                                        default:
                                            sb.append('\\');
                                            break;
                                    }
                                }

                            }
                            l = c;
                            sb.append(c);
                            state[MD_LENGTH]++;
                        }

                        if (state[MD_HTML_LINK] == 0 && sb.length() != 0) {
                            flush(ctx, ui, sb, state);
                        }
                        if (d == len) break;
                    }
                    int e = src.indexOf('>', d);
                    if (e == -1) {
                        // No closing '>', treat '<' as literal text
                        sb.append('<');
                        state[MD_LENGTH]++;
                        o = d + 1;
                        if (o >= len) {
                            flush(ctx, ui, sb, state);
                            break;
                        }
                        d = enableHtmlTags ? src.indexOf('<', o) : -1;
                        if (d == -1) d = len;
                        // Re-enter text processing for the rest; continue outer loop
                        // by falling through to next iteration via break a
                        break a;
                    }
                    if (enableDiscordParsing && state[MD_GRAVE] == 0) {
                        int discordEnd = handleDiscordTag(ui, ctx, src, chars, sb, state, d, len);
                        if (discordEnd != -1) {
                            o = discordEnd + 1;
                            d = enableHtmlTags ? src.indexOf('<', o) : -1;
                            break a;
                        }
                    }
                    if (d + 2 < len) { // format by tags
                        if (chars[d + 1] == '/') {
                            if ((chars[d + 2] == 'b' && chars[d + 3] == '>') ||
                                (chars[d + 2] == 's' && chars[d + 3] == 't')) {
                                // </b> or </strong>
                                state[MD_HTML_BOLD]--;
                            }
                            else if (chars[d + 2] == 'h' && chars[d + 4] == '>') {
                                // </h?>
                                state[MD_HTML_HEADER] = 0;
                            }
                            else if ((chars[d + 2] == 'e' && chars[d + 3] == 'm') ||
                                (chars[d + 2] == 'i' && chars[d + 3] == '>')) {
                                // </em> or </i>
                                state[MD_HTML_ITALIC]--;
                            }
                            else if (chars[d + 2] == 'b' && chars[d + 3] == 'i' &&
                                chars[d + 4] == 'g') {
                                // </small>
                                state[MD_HTML_BIG]--;
                            }
                            else if ((chars[d + 2] == 's' && chars[d + 3] == 'u' &&
                                chars[d + 4] == 'b') ||
                                (chars[d + 2] == 'i' && chars[d + 3] == 'n' &&
                                    chars[d + 4] == 's') ||
                                (chars[d + 2] == 'u' && chars[d + 3] == '>')) {
                                // </sub>, </ins>, </u>
                                state[MD_HTML_UNDERLINE]--;
                                state[1] &= ~FONT_STYLE_UNDERLINED;
                            }
                            else if (chars[d + 2] == 'p' && chars[d + 3] == '>') {
                                // </p>
                                state[MD_HTML_PARAGRAPH]--;
                                sb.append('\n');
                            }
                            else if (chars[d + 2] == 's' && chars[d + 3] == 'm') {
                                // </small>
                            }
                            else if (chars[d + 2] == 's' && chars[d + 3] == '>') {
                                // </s>
                                state[MD_HTML_STRIKE]--;
                            }
                            else if (chars[d + 2] == 'a' && chars[d + 3] == '>') {
                                // </a>
                                state[MD_HTML_LINK]--;

                                flush(ctx, ui, sb, state);

                                if (linksStack.pop() != null) ui.endHref(ctx);
                            }
                            else {
                                sb.append('<');
                                e = d;
                            }
                        }
                        else {
                            if ((chars[d + 1] == 'b' && chars[d + 2] == '>') ||
                                (chars[d + 1] == 's' && chars[d + 2] == 't')) {
                                // <b> or <strong>
                                state[MD_HTML_BOLD]++;
                            }
                            else if (chars[d + 1] == 'h' && chars[d + 3] == '>') {
                                // <h?>
                                state[MD_HTML_HEADER] = chars[d + 2] - '0';
                            }
                            else if ((chars[d + 1] == 'e' && chars[d + 2] == 'm') ||
                                (chars[d + 1] == 'i' && chars[d + 2] == '>')) {
                                // <em> or <i>
                                state[MD_HTML_ITALIC]++;
                            }
                            else if (chars[d + 1] == 'b' && chars[d + 2] == 'i' &&
                                chars[d + 3] == 'g') {
                                // <big>
                                state[MD_HTML_BIG]++;
                            }
                            else if ((chars[d + 1] == 's' && chars[d + 2] == 'u' &&
                                chars[d + 3] == 'b') ||
                                (chars[d + 1] == 'i' && chars[d + 2] == 'n' &&
                                    chars[d + 3] == 's') || (chars[d + 1] == 'u')) {
                                // <sub>, <ins>, <u>
                                state[MD_HTML_UNDERLINE]++;
                                state[1] |= FONT_STYLE_UNDERLINED;
                            }
                            else if (chars[d + 1] == 'b' && chars[d + 2] == 'r') {
                                // <br>
                                sb.append('\n');
                            }
                            else if (chars[d + 1] == 'p' &&
                                (chars[d + 2] == ' ' || chars[d + 2] == '>')) {
                                // <p>
                                state[MD_HTML_PARAGRAPH]++;
                                if (state[MD_BREAKS] != 0) sb.append('\n');
                            }
                            else if (enableImages && chars[d + 1] == 'i' && chars[d + 2] == 'm' &&
                                chars[d + 3] == 'g') {
                                // <img>

                                flush(ctx, ui, sb, state);

                                String url = src.substring(d + 4, e);
                                int i;
                                if ((i = url.indexOf("src=")) != -1) {
                                    url = url.substring(i + 4);
                                    if ((i = url.indexOf(' ')) != -1) {
                                        url = url.substring(0, i);
                                    }

                                    if (url.charAt(0) == '"') {
                                        url = url.substring(1, url.length() - 1);
                                    }

                                    ui.beginHref(ctx, url);
                                    ui.appendImage(ctx, url, null);
                                    ui.endHref(ctx);
                                }
                            }
                            else if (chars[d + 1] == 's' && chars[d + 2] == 'm') {
                                // <small>
                            }
                            else if (chars[d + 1] == 's' && chars[d + 2] == '>') {
                                // <s>
                                state[MD_HTML_STRIKE]++;
                            }
                            else if (chars[d + 1] == 'a' &&
                                (chars[d + 2] == ' ' || chars[d + 2] == '>')) {
                                // <a>
                                flush(ctx, ui, sb, state);
                                state[MD_HTML_LINK]++;

                                String url = src.substring(d + 2, e);
                                int i;
                                if ((i = url.indexOf("href=")) != -1) {
                                    url = url.substring(i + 5);
                                    if ((i = url.indexOf(' ')) != -1) {
                                        url = url.substring(0, i);
                                    }

                                    if (url.charAt(0) == '"') {
                                        url = url.substring(1, url.length() - 1);
                                    }
                                    ui.beginHref(ctx, url);
                                    if (urls != null) urls.put(url, url);
                                    linksStack.push(url);
                                }
                                else {
                                    linksStack.push(null);
                                }
                            }
                            else {
                                sb.append('<');
                                e = d;
                            }

                            // <li> ?
                        }
                    }
                    o = e + 1;
                    d = enableHtmlTags ? src.indexOf('<', o) : -1;
                }
            }
        }
        finally {
            while (!linksStack.empty()) {
                linksStack.pop();
                ui.endHref(ctx);
            }

            if (state[MD_DISCORD_SPOILER] != 0) {
                state[MD_DISCORD_SPOILER] = 0;
                ui.endSpoiler(ctx);
            }

            if (state[MD_GRAVE] == 3) {
                ui.endCodeBlock(ctx);
            }

            while (state[MD_BLOCKQUOTE] != 0) {
                state[MD_BLOCKQUOTE]--;
                ui.endBlockQuote(ctx);
            }
            ui.endMarkdown(ctx);
        }
    }

    private static void flush(Object ctx, MarkdownListener ui, StringBuffer sb, int[] state) {
        if (sb.length() == 0) return;

        boolean isSubtext = state[MD_SUBTEXT] != 0;
        if (state[MD_HEADER] != 0) {
            ui.beginHeader(ctx, state[MD_HEADER]);
        }
        else if (isSubtext) {
            ui.beginSubtext(ctx);
        }
        int space = 0;
        while (sb.length() != 0 && sb.charAt(sb.length() - 1) == ' ') {
            sb.setLength(sb.length() - 1);
            space++;
        }
        String t = sb.toString();
        int f = getFont(state);

        // find links
        if (enableLinksSearching &&
            (state[MD_GRAVE] == 0 && state[MD_LINK] == 0 && state[MD_HTML_LINK] == 0) &&
            (t.indexOf("http://") != -1 || t.indexOf("https://") != -1 ||
                (enableHashtagLinks && t.indexOf('#') != -1) ||
                (enableTagLinks && t.indexOf('@') != -1))) {
            int i, j, k, d = 0;
            while (true) {
                boolean b = false;
                i = t.indexOf("://", d);
                j = enableHashtagLinks ? t.indexOf('#', d) : -1;
                k = enableTagLinks ? t.indexOf('@', d) : -1;
                if (i == -1 && j == -1 && k == -1) break;

                if (k != -1 && (j == -1 || j > k)) {
                    j = k;
                }
                if (j != -1 && (i == -1 || i > j)) {
                    i = j;
                }
                else {
                    b = i != -1;
                }

                if (b) {
                    b:
                    {
                        boolean https;
                        char c;
                        if (i < 4 || ((https = t.charAt(i - 1) != 'p') &&
                            (i < 5 || t.charAt(i - 1) != 's')) ||
                            (i != (j = https ? 5 : 4) && (c = t.charAt(i - j - 1)) > ' ' &&
                                c != '(')) {
                            break b;
                        }
                        j = i - j;
                        boolean valid = false;
                        int len = t.length();
                        for (k = j; k < len; ++k) {
                            c = t.charAt(k);
                            if (c <= ' ' || c == ',') break;
                            if (c == '.') valid = true;
                        }
                        if (!valid) break b;

                        if (i != 0) {
                            ui.append(ctx, t.substring(0, j), f);
                        }
                        ui.appendLink(ctx, t.substring(j, k), f);

                        t = t.substring(k);
                        d = 0;
                        continue;
                    }
                    d = i + 3;
                }
                else {
                    b:
                    {
                        char c;
                        if (i != 0 && (c = t.charAt(i - 1)) > ' ' && c != '(') {
                            break b;
                        }
//						b = t.charAt(i) == '@';
                        int len = t.length();
                        for (k = i + 1; k < len && k < i + 10; ++k) {
                            c = t.charAt(k);
                            if (c <= ' ' || c == ')' || c == ',' || c == '.') break;
//							if (!b && (c < '0' || c > '9')) break b;
                        }
                        if (k == i + 10 || k == i + 1) break b;
                        if (i != 0) {
                            ui.append(ctx, t.substring(0, i), f);
                        }
                        ui.appendLink(ctx, t.substring(i, k), f);

                        t = t.substring(k);
                        d = 0;
                        continue;
                    }
                    d = i + 1;
                }
            }
        }

        if (t.length() != 0) {
            ui.append(ctx, t, f);
        }
        sb.setLength(0);

        if (state[MD_HEADER] != 0) {
            ui.endHeader(ctx, state[MD_HEADER]);
        }
        else if (isSubtext) {
            ui.endSubtext(ctx);
        }
        else if (state[MD_LINE] != 0) {
            if (enableHorizontalLines) ui.horizontalLine(ctx);
            state[MD_LINE] = 0;
            state[MD_PARAGRAPH] = 1;
        }
        else {
            while (space-- != 0) {
                ui.appendInlineSpace(ctx, f);
            }
        }
    }

    private static int getFont(int[] state) {
        int face = 0, style = 0, size = 0;
        if (state[MD_GRAVE] != 0) {
            face = FONT_FACE_MONOSPACE;
            if (monospaceIsBold) style = FONT_STYLE_BOLD;
        }
        else {
            face = state[MD_FONT_FACE];
            style = state[MD_FONT_STYLE];
        }
        size = state[MD_FONT_SIZE];
        if (state[MD_BOLD] != 0 || state[MD_HTML_BOLD] != 0) {
            style |= FONT_STYLE_BOLD;
        }
        if (state[MD_UNDERLINE] != 0 || state[MD_HTML_UNDERLINE] != 0) {
            style |= FONT_STYLE_UNDERLINED;
        }
        if (state[MD_ITALIC] != 0 || state[MD_HTML_ITALIC] != 0) {
            style |= FONT_STYLE_ITALIC;
        }
        if (state[MD_HTML_BIG] != 0) {
            size = state[MD_HTML_BIG] == 1 ? FONT_SIZE_MEDIUM : FONT_SIZE_LARGE;
        }
        if (enableStrikethroughFont && (state[MD_STRIKE] != 0 || state[MD_HTML_STRIKE] != 0)) {
            style |= FONT_STYLE_STRIKETHROUGH;
        }
        int header = state[MD_HEADER];
        switch (header != 0 ? header : state[MD_HTML_HEADER]) {
            case 1:
                size = FONT_SIZE_LARGE;
                style |= FONT_STYLE_BOLD;
                break;
            case 2:
                size = FONT_SIZE_MEDIUM;
                style |= FONT_STYLE_BOLD;
                break;
            case 3:
                size = FONT_SIZE_SMALL;
                style |= FONT_STYLE_BOLD;
                break;
            case 4:
            case 5:
            case 6:
                size = FONT_SIZE_SMALL;
                break;
        }
        return face | style | size;
    }

    private static boolean isDiscordWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private static boolean isDiscordEmojiNameChar(char c) {
        return isDiscordWordChar(c);
    }

    private static boolean isDiscordTimestampStyle(char c) {
        return c == 't' || c == 'T' || c == 'd' || c == 'D' || c == 'f' || c == 'F' || c == 's' ||
            c == 'S' || c == 'R';
    }

    /**
     * Parses a Discord-specific &lt;...&gt; element at the given start index.
     * On success flushes pending text, notifies the listener and returns the index
     * of the closing '&gt;'. On failure returns -1 (caller treats '&lt;' as literal).
     * <p>
     * Supported: &lt;@id&gt;, &lt;@!id&gt;, &lt;@&amp;id&gt;, &lt;#id&gt;,
     * &lt;:name:id&gt;, &lt;a:name:id&gt;, &lt;t:unix&gt;, &lt;t:unix:style&gt;.
     */
    private static int handleDiscordTag(MarkdownListener ui, Object ctx, String src, char[] chars,
        StringBuffer sb, int[] state, int start, int len) {
        if (start + 2 >= len || chars[start] != '<') {
            return -1;
        }
        char c1 = chars[start + 1];
        // Role mention <@&id>
        if (c1 == '@' && start + 3 < len && chars[start + 2] == '&') {
            int p = start + 3;
            int q = p;
            while (q < len && Character.isDigit(chars[q])) q++;
            if (q == p || q >= len || chars[q] != '>') {
                return -1;
            }
            // Reject if contains space/newline before '>' (already ensured: only digits then '>')
            String id = src.substring(p, q);
            flush(ctx, ui, sb, state);
            ui.appendRoleMention(ctx, id, getFont(state));
            state[MD_LENGTH]++;
            state[MD_DISCORD_MENTION] = 0;
            return q;
        }
        // User mention <@id> or <@!id>
        if (c1 == '@') {
            int p = start + 2;
            if (p < len && chars[p] == '!') p++;
            int q = p;
            while (q < len && Character.isDigit(chars[q])) q++;
            if (q != p && q < len && chars[q] == '>') {
                String id = src.substring(p, q);
                flush(ctx, ui, sb, state);
                ui.appendUserMention(ctx, id, getFont(state));
                state[MD_LENGTH]++;
                state[MD_DISCORD_MENTION] = 0;
                return q;
            }
            // Not a user mention, fall through to other checks (e.g. role already handled)
            if (q != p) {
                return -1;
            }
            // If it looked like <@... but invalid, don't try other types starting with '@'
            // (emoji/timestamp/channel start with different chars)
            return -1;
        }
        // Channel mention <#id>
        if (c1 == '#') {
            int p = start + 2;
            int q = p;
            while (q < len && Character.isDigit(chars[q])) q++;
            if (q == p || q >= len || chars[q] != '>') {
                return -1;
            }
            String id = src.substring(p, q);
            flush(ctx, ui, sb, state);
            ui.appendChannelMention(ctx, id, getFont(state));
            state[MD_LENGTH]++;
            state[MD_DISCORD_MENTION] = 0;
            return q;
        }
        // Timestamp <t:unix> or <t:unix:style>
        if (c1 == 't' && start + 3 < len && chars[start + 2] == ':') {
            int p = start + 3;
            int q = p;
            while (q < len && Character.isDigit(chars[q])) q++;
            if (q == p || q >= len) {
                return -1;
            }
            String timestamp = src.substring(p, q);
            String style = null;
            int end;
            if (chars[q] == '>') {
                end = q;
            }
            else if (chars[q] == ':') {
                int s = q + 1;
                if (s >= len || !isDiscordTimestampStyle(chars[s])) {
                    return -1;
                }
                style = src.substring(s, s + 1);
                end = s + 1;
                if (end >= len || chars[end] != '>') {
                    return -1;
                }
            }
            else {
                return -1;
            }
            flush(ctx, ui, sb, state);
            ui.appendTimestamp(ctx, timestamp, style, getFont(state));
            state[MD_LENGTH]++;
            state[MD_DISCORD_TIMESTAMP] = 0;
            return end;
        }
        // Emoji <:name:id> or <a:name:id>
        if (c1 == ':' || (c1 == 'a' && start + 3 < len && chars[start + 2] == ':')) {
            boolean animated = c1 == 'a';
            int p = animated ? start + 3 : start + 2;
            int q = p;
            while (q < len && isDiscordEmojiNameChar(chars[q])) q++;
            if (q - p < 2 || q >= len || chars[q] != ':') {
                return -1;
            }
            String name = src.substring(p, q);
            int r = q + 1;
            int s = r;
            while (s < len && Character.isDigit(chars[s])) s++;
            if (s == r || s >= len || chars[s] != '>') {
                return -1;
            }
            String id = src.substring(r, s);
            flush(ctx, ui, sb, state);
            ui.appendDiscordEmoji(ctx, name, id, animated, getFont(state));
            state[MD_LENGTH]++;
            state[MD_DISCORD_EMOJI] = 0;
            return s;
        }
        return -1;
    }

    private static int emojiEnd(char[] chars, int i, int len) {
        int codePoint = codePointAt(chars, i, len);
        // keycap: '#', '*' or a digit followed by VS16 and the enclosing keycap
        if (codePoint == '#' || codePoint == '*' || (codePoint >= '0' && codePoint <= '9')) {
            return i + 2 < len && chars[i + 1] == VARIATION_SELECTOR_16 &&
                chars[i + 2] == ENCLOSING_KEYCAP ? i + 3 : -1;
        }
        if (isRegionalIndicator(codePoint)) {
            // a flag is exactly two regional indicators, a lone one is not an emoji
            return isRegionalIndicator(codePointAt(chars, i + 2, len)) ? i + 4 : -1;
        }
        // nothing below U+00A9 outside of keycap bases can start an emoji
        if (codePoint < 0xA9 || !isEmojiPicto(codePoint)) return -1;

        int p = i;
        for (; ; ) {
            p += codePointLength(codePoint);
            if (p < len) {
                if (chars[p] == VARIATION_SELECTOR_15) return -1; // text presentation
                if (chars[p] == VARIATION_SELECTOR_16) ++p;
            }
            int next = codePointAt(chars, p, len);
            if (isEmojiModifier(next)) {
                p += codePointLength(next);
                next = codePointAt(chars, p, len);
            }
            if (isEmojiTag(next)) { // subdivision flags
                while (isEmojiTag(next = codePointAt(chars, p, len))) {
                    p += codePointLength(next);
                }
                break;
            }
            if (next != ZERO_WIDTH_JOINER) break;
            // a joiner is only part of an emoji if another emoji follows
            int joined = codePointAt(chars, p + 1, len);
            if (!isEmojiPicto(joined)) break;
            ++p; // skip the joiner
            codePoint = joined;
        }
        return p;
    }

    private static String emojiCodepoints(char[] chars, int start, int end) {
        StringBuffer sb = new StringBuffer();
        for (int i = start; i < end; ) {
            int codePoint = codePointAt(chars, i, end);
            if (codePoint < 0) break;
            if (sb.length() != 0) sb.append('-');
            sb.append(Integer.toHexString(codePoint));
            i += codePointLength(codePoint);
        }
        return sb.toString();
    }

    private static boolean isEmojiPicto(int codePoint) {
        int[] ranges = EMOJI_PICTOGRAPHICS;
        int low = 0, high = ranges.length / 2 - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (codePoint < ranges[mid * 2]) {
                high = mid - 1;
            }
            else if (codePoint > ranges[mid * 2 + 1]) {
                low = mid + 1;
            }
            else {
                return true;
            }
        }
        return false;
    }

    private static boolean isRegionalIndicator(int codePoint) {
        return codePoint >= REGIONAL_INDICATOR_FIRST && codePoint <= REGIONAL_INDICATOR_LAST;
    }

    private static boolean isEmojiModifier(int codePoint) {
        return codePoint >= EMOJI_MODIFIER_FIRST && codePoint <= EMOJI_MODIFIER_LAST;
    }

    private static boolean isEmojiTag(int codePoint) {
        return codePoint >= EMOJI_TAG_FIRST && codePoint <= EMOJI_TAG_LAST;
    }

    private static int codePointAt(char[] chars, int i, int len) {
        if (i >= len) return -1;
        char c = chars[i];
        if (Character.isHighSurrogate(c) && i + 1 < len && Character.isLowSurrogate(chars[i + 1])) {
            return Character.toCodePoint(c, chars[i + 1]);
        }
        return c;
    }

    private static int codePointLength(int codePoint) {
        return codePoint >= 0x10000 ? 2 : 1;
    }

}
