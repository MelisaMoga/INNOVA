package com.melisa.pedonovation;

import android.view.View;

public class Utilities {
    public static final String FILE_NAME_FORMAT = "TestData_%s.txt";

    /**
     * Sets the visibility of multiple UI elements.
     *
     * @param visibility The visibility status (View.VISIBLE or View.GONE).
     * @param views      The UI elements whose visibility will be set.
     */
    public static void setVisibility(int visibility, View... views) {
        for (View view : views) {
            if (view != null) {
                view.setVisibility(visibility);
            }
        }
    }


}
