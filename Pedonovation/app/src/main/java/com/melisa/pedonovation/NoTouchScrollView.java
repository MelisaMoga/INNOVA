package com.melisa.pedonovation;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

public class NoTouchScrollView extends ScrollView {

    public NoTouchScrollView(Context context) {
        super(context);
    }

    public NoTouchScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NoTouchScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // Do not allow touch events to cause vertical scrolling
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // Do not allow touch events to cause vertical scrolling
        return false;
    }
}