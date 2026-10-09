package fi.gtrxac.discord;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class MessageRowLayout extends LinearLayout {
    public MessageRowLayout(Context context) {
        super(context);
    }

    public MessageRowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MessageRowLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean hasFocusable() {
        return false;
    }
}
