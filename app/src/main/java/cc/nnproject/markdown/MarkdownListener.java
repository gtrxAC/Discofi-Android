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

public interface MarkdownListener {

    /**
     * @param ctx Context passed by application
     */
    void beginMarkdown(Object ctx);

    /**
     * @param ctx Context passed by application
     */
    void endMarkdown(Object ctx);

    /**
     * @param ctx  Context passed by application
     * @param link
     */
    void beginHref(Object ctx, Object link);

    /**
     * @param ctx Context passed by application
     */
    void endHref(Object ctx);

    /**
     * @param ctx      Context passed by application
     * @param language Code language, may be null
     */
    void beginCodeBlock(Object ctx, String language);

    /**
     * @param ctx Context passed by application
     */
    void endCodeBlock(Object ctx);

    /**
     * @param ctx Context passed by application
     */
    void beginBlockQuote(Object ctx);

    /**
     * @param ctx Context passed by application
     */
    void endBlockQuote(Object ctx);

    /**
     * @param ctx Context passed by application
     */
    void beginHeader(Object ctx, int n);

    /**
     * @param ctx Context passed by application
     */
    void endHeader(Object ctx, int n);

    void beginList(Object ctx, boolean ordered);

    void endList(Object ctx, boolean ordered);

    void beginListItem(Object ctx, int n);

    void endListItem(Object ctx);

    void beginSubtext(Object ctx);

    void endSubtext(Object ctx);

    void beginSpoiler(Object ctx);

    void endSpoiler(Object ctx);

    /**
     * @param ctx  Context passed by application
     * @param text Text
     * @param font bitwise combination of font styles
     */
    void append(Object ctx, String text, int font);

    /**
     * @param ctx  Context passed by application
     * @param text Text
     * @param font bitwise combination of font styles
     */
    void appendLink(Object ctx, String text, int font);

    /**
     * @param ctx  Context passed by application
     * @param id   Mentioned user ID (digits only)
     * @param font bitwise combination of font styles
     */
    void appendUserMention(Object ctx, String id, int font);

    /**
     * @param ctx  Context passed by application
     * @param id   Mentioned role ID (digits only)
     * @param font bitwise combination of font styles
     */
    void appendRoleMention(Object ctx, String id, int font);

    /**
     * @param ctx  Context passed by application
     * @param id   Mentioned channel ID (digits only)
     * @param font bitwise combination of font styles
     */
    void appendChannelMention(Object ctx, String id, int font);

    /**
     * @param ctx  Context passed by application
     * @param font bitwise combination of font styles
     */
    void appendEveryone(Object ctx, int font);

    /**
     * @param ctx  Context passed by application
     * @param font bitwise combination of font styles
     */
    void appendHere(Object ctx, int font);

    /**
     * @param ctx      Context passed by application
     * @param name     Emoji name without colons
     * @param id       Emoji ID (digits only)
     * @param animated true for &lt;a:name:id&gt;, false for &lt;:name:id&gt;
     * @param font     bitwise combination of font styles
     */
    void appendDiscordEmoji(Object ctx, String name, String id, boolean animated, int font);

    /**
     * @param ctx        Context passed by application
     * @param emoji      Emoji as written in the source text
     * @param codepoints Code points of the emoji, lowercase hex separated
     *                   by '-', e.g. "1f469-200d-1f4bb"
     * @param font       bitwise combination of font styles
     */
    void appendUnicodeEmoji(Object ctx, String emoji, String codepoints, int font);

    /**
     * @param ctx       Context passed by application
     * @param timestamp Unix timestamp in seconds (digits only)
     * @param style     Timestamp style (t,T,d,D,f,F,R) or null if unspecified
     * @param font      bitwise combination of font styles
     */
    void appendTimestamp(Object ctx, String timestamp, String style, int font);

    /**
     * @param ctx  Context passed by application
     * @param font bitwise combination of font styles
     */
    void appendInlineSpace(Object ctx, int font);

    /**
     * @param ctx Context passed by application
     * @param src Image URL
     * @param alt Image caption, may be null
     */
    void appendImage(Object ctx, String src, String alt);

    /**
     * @param ctx Context passed by application
     */
    void lineBreak(Object ctx);

    /**
     * @param ctx Context passed by application
     */
    void horizontalLine(Object ctx);

}
