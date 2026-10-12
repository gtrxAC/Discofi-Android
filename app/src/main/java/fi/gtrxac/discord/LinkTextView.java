package fi.gtrxac.discord;

import android.content.Context;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.method.Touch;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.TextView;

public class LinkTextView extends TextView {
    private boolean linkHit;
    private boolean linkGesture;
    private ClickableSpan pressedLink;
    private long linkDownTime;

    public LinkTextView(Context context) {
        super(context);
    }

    public LinkTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LinkTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        linkHit = false;
        if (action == MotionEvent.ACTION_DOWN) {
            CharSequence text = getText();
            boolean clickable = text instanceof Spannable
                    && LocalLinkMovementMethod.INSTANCE.findLinkAt(
                            this, (Spannable) text, event) != null;
            setLongClickable(clickable);
            if (!clickable) {
                return false;
            }
        } else if (!linkGesture) {
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                setLongClickable(false);
            }
            return false;
        }

        super.onTouchEvent(event);
        boolean handled = linkHit;
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            linkGesture = false;
            pressedLink = null;
            setLongClickable(false);
        }
        return handled;
    }

    private void startLinkGesture(ClickableSpan link, MotionEvent event) {
        linkGesture = true;
        pressedLink = link;
        linkDownTime = event.getEventTime();
        linkHit = true;
    }

    private boolean isLinkGesture() {
        return linkGesture;
    }

    private void finishLinkGesture(ClickableSpan link, MotionEvent event,
                                  TextView widget) {
        if (pressedLink != null && pressedLink == link
                && event.getEventTime() - linkDownTime
                < ViewConfiguration.getLongPressTimeout()) {
            pressedLink.onClick(widget);
        }
        linkHit = true;
    }

    public static class LocalLinkMovementMethod extends LinkMovementMethod {
        private static final LocalLinkMovementMethod INSTANCE =
                new LocalLinkMovementMethod();

        public static LocalLinkMovementMethod getInstance() {
            return INSTANCE;
        }

        @Override
        public boolean onTouchEvent(TextView widget, Spannable buffer,
                                    MotionEvent event) {
            int action = event.getAction();

            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP
                    || action == MotionEvent.ACTION_MOVE
                    || action == MotionEvent.ACTION_CANCEL) {
                LinkTextView linkWidget = widget instanceof LinkTextView
                        ? (LinkTextView) widget : null;
                boolean activeGesture = linkWidget != null && linkWidget.isLinkGesture();
                if (action == MotionEvent.ACTION_CANCEL && activeGesture) {
                    Selection.removeSelection(buffer);
                    linkWidget.linkHit = true;
                    return true;
                }

                if (action == MotionEvent.ACTION_MOVE && activeGesture) {
                    if (linkWidget.pressedLink != findLinkAt(widget, buffer, event)) {
                        linkWidget.pressedLink = null;
                        Selection.removeSelection(buffer);
                    }
                    return true;
                }

                if (action == MotionEvent.ACTION_UP && activeGesture) {
                    ClickableSpan link = findLinkAt(widget, buffer, event);
                    linkWidget.finishLinkGesture(link, event, widget);
                    Selection.removeSelection(buffer);
                    return true;
                }

                if (action != MotionEvent.ACTION_DOWN) {
                    Selection.removeSelection(buffer);
                    Touch.onTouchEvent(widget, buffer, event);
                    return false;
                }

                ClickableSpan link = findLinkAt(widget, buffer, event);
                if (link != null && linkWidget != null) {
                    linkWidget.startLinkGesture(link, event);
                    Selection.setSelection(buffer, buffer.getSpanStart(link),
                            buffer.getSpanEnd(link));
                    return true;
                }

                Selection.removeSelection(buffer);
                Touch.onTouchEvent(widget, buffer, event);
                return false;
            }

            return Touch.onTouchEvent(widget, buffer, event);
        }

        private ClickableSpan findLinkAt(TextView widget, Spannable buffer,
                                         MotionEvent event) {
            Layout layout = widget.getLayout();
            if (layout == null) return null;

            int x = (int) event.getX() - widget.getTotalPaddingLeft()
                    + widget.getScrollX();
            int y = (int) event.getY() - widget.getTotalPaddingTop()
                    + widget.getScrollY();

            if (x < 0 || x > layout.getWidth() || y < 0 || y > layout.getHeight()) {
                return null;
            }

            int line = layout.getLineForVertical(y);
            int offset = layout.getOffsetForHorizontal(line, x);
            return findLinkAt(buffer, offset);
        }

        private ClickableSpan findLinkAt(Spannable buffer, int offset) {
            ClickableSpan[] links = buffer.getSpans(offset, offset, ClickableSpan.class);
            for (int i = 0; i < links.length; i++) {
                int start = buffer.getSpanStart(links[i]);
                int end = buffer.getSpanEnd(links[i]);
                if (offset >= start && offset < end) return links[i];
            }
            return null;
        }
    }
}
