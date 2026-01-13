package com.melisa.innovamotionapp.ui.helpers;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.melisa.innovamotionapp.sync.SensorAssignmentService;
import com.melisa.innovamotionapp.utils.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * Reusable component for supervisor email autocomplete.
 * 
 * Attaches to a TextInputEditText and shows a dropdown popup with matching
 * supervisor emails as the user types.
 * 
 * Usage:
 * <pre>
 *     SupervisorEmailAutocomplete autocomplete = new SupervisorEmailAutocomplete(context);
 *     autocomplete.attachTo(emailInput, emailInputLayout, email -> {
 *         // Handle email selection
 *     });
 * </pre>
 */
public class SupervisorEmailAutocomplete {
    private static final String TAG = "SupervisorAutocomplete";
    private static final int DEBOUNCE_DELAY_MS = Constants.SEARCH_DEBOUNCE_DELAY_MS;
    private static final int MIN_QUERY_LENGTH = 2;
    
    private final Context context;
    private final SensorAssignmentService assignmentService;
    private final Handler searchHandler;
    private final List<String> emailList;
    private final ArrayAdapter<String> emailAdapter;
    
    private TextInputEditText inputField;
    private TextInputLayout inputLayout;
    private PopupWindow suggestionPopup;
    private ListView suggestionListView;
    private OnEmailSelectedListener selectionListener;
    
    private Runnable searchRunnable;
    private boolean isSettingTextProgrammatically = false;
    
    /**
     * Callback for when an email is selected from the dropdown.
     */
    public interface OnEmailSelectedListener {
        void onEmailSelected(String email);
    }
    
    public SupervisorEmailAutocomplete(@NonNull Context context) {
        this.context = context;
        this.assignmentService = SensorAssignmentService.getInstance(context);
        this.searchHandler = new Handler(Looper.getMainLooper());
        this.emailList = new ArrayList<>();
        this.emailAdapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, emailList);
    }
    
    /**
     * Attach autocomplete functionality to an input field.
     * 
     * @param input The TextInputEditText to attach to
     * @param layout The containing TextInputLayout (for error handling)
     * @param listener Callback for when an email is selected
     */
    public void attachTo(@NonNull TextInputEditText input, 
                         @Nullable TextInputLayout layout,
                         @NonNull OnEmailSelectedListener listener) {
        this.inputField = input;
        this.inputLayout = layout;
        this.selectionListener = listener;
        
        createSuggestionPopup();
        setupTextWatcher();
        setupFocusListener();
        
        Log.d(TAG, "Attached to input field");
    }
    
    /**
     * Create the suggestion popup window.
     */
    private void createSuggestionPopup() {
        suggestionListView = new ListView(context);
        suggestionListView.setAdapter(emailAdapter);
        suggestionListView.setDividerHeight(1);
        suggestionListView.setDivider(ContextCompat.getDrawable(context, android.R.color.transparent));
        suggestionListView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.white));
        suggestionListView.setElevation(8f);
        
        // Handle item clicks
        suggestionListView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < emailList.size()) {
                String selectedEmail = emailList.get(position);
                Log.d(TAG, "Selected email: " + selectedEmail);
                
                selectEmail(selectedEmail);
            }
        });
        
        // Create popup window
        suggestionPopup = new PopupWindow(
                suggestionListView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        suggestionPopup.setOutsideTouchable(true);
        suggestionPopup.setFocusable(false);
        suggestionPopup.setElevation(8f);
        suggestionPopup.setBackgroundDrawable(
                ContextCompat.getDrawable(context, android.R.drawable.editbox_dropdown_light_frame)
        );
    }
    
    /**
     * Setup text change listener for debounced search.
     */
    private void setupTextWatcher() {
        inputField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Skip if setting text programmatically
                if (isSettingTextProgrammatically) {
                    return;
                }
                
                // Cancel previous search
                cancelPendingSearch();
                
                // Schedule new search with debounce
                if (s.length() >= MIN_QUERY_LENGTH) {
                    searchRunnable = () -> searchSupervisors(s.toString());
                    searchHandler.postDelayed(searchRunnable, DEBOUNCE_DELAY_MS);
                } else {
                    hidePopup();
                }
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    
    /**
     * Setup focus listener to hide popup on focus loss.
     */
    private void setupFocusListener() {
        inputField.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                hidePopup();
            }
        });
    }
    
    /**
     * Search for supervisors matching the query.
     */
    private void searchSupervisors(String query) {
        Log.d(TAG, "Searching for supervisors: " + query);
        
        assignmentService.searchSupervisorsByEmail(query, new SensorAssignmentService.SearchCallback() {
            @Override
            public void onResult(List<SensorAssignmentService.SupervisorInfo> supervisors) {
                emailList.clear();
                for (SensorAssignmentService.SupervisorInfo info : supervisors) {
                    emailList.add(info.email);
                }
                emailAdapter.notifyDataSetChanged();
                
                if (!emailList.isEmpty() && inputField.hasFocus()) {
                    showPopup();
                } else {
                    hidePopup();
                }
            }
            
            @Override
            public void onError(String error) {
                Log.w(TAG, "Search error: " + error);
                // Don't show error to user - just continue with manual entry
                hidePopup();
            }
        });
    }
    
    /**
     * Select an email and notify listener.
     */
    private void selectEmail(String email) {
        isSettingTextProgrammatically = true;
        inputField.setText(email);
        inputField.setSelection(email.length());
        isSettingTextProgrammatically = false;
        
        if (inputLayout != null) {
            inputLayout.setError(null);
        }
        
        hidePopup();
        
        if (selectionListener != null) {
            selectionListener.onEmailSelected(email);
        }
    }
    
    /**
     * Show the suggestion popup.
     */
    public void showPopup() {
        if (suggestionPopup != null && !suggestionPopup.isShowing() && inputField != null) {
            try {
                // Calculate width to match input layout
                int width = inputLayout != null ? inputLayout.getWidth() : inputField.getWidth();
                suggestionPopup.setWidth(width);
                
                // Show below the input field
                suggestionPopup.showAsDropDown(inputField, 0, 0);
                Log.d(TAG, "Popup shown with " + emailList.size() + " items");
            } catch (Exception e) {
                Log.w(TAG, "Error showing popup", e);
            }
        }
    }
    
    /**
     * Hide the suggestion popup.
     */
    public void hidePopup() {
        if (suggestionPopup != null && suggestionPopup.isShowing()) {
            try {
                suggestionPopup.dismiss();
                Log.d(TAG, "Popup hidden");
            } catch (Exception e) {
                Log.w(TAG, "Error hiding popup", e);
            }
        }
    }
    
    /**
     * Cancel any pending search.
     */
    public void cancelPendingSearch() {
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
            searchRunnable = null;
        }
    }
    
    /**
     * Set the text programmatically without triggering search.
     */
    public void setText(String text) {
        isSettingTextProgrammatically = true;
        inputField.setText(text);
        if (text != null) {
            inputField.setSelection(text.length());
        }
        isSettingTextProgrammatically = false;
    }
    
    /**
     * Get the current text.
     */
    public String getText() {
        if (inputField != null && inputField.getText() != null) {
            return inputField.getText().toString().trim();
        }
        return "";
    }
    
    /**
     * Check if the popup is currently showing.
     */
    public boolean isPopupShowing() {
        return suggestionPopup != null && suggestionPopup.isShowing();
    }
    
    /**
     * Clean up resources.
     */
    public void detach() {
        cancelPendingSearch();
        hidePopup();
        inputField = null;
        inputLayout = null;
        selectionListener = null;
    }
}
