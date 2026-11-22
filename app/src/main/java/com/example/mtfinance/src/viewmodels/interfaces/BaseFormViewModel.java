package com.example.mtfinance.src.viewmodels.interfaces;

import android.os.Looper;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.Map;

/**
 * Base interface for form ViewModels enforcing validation error maps, individual field error resets,
 * message state management, form clearing, and thread-safe LiveData updates.
 * @param <K> the enum type representing field error keys
 */
public interface BaseFormViewModel<K extends Enum<K>> {
    LiveData<Map<K, String>> getFieldErrors();

    /**
     * Convenience method returning the error map LiveData.
     */
    default LiveData<Map<K, String>> getErrorMap() {
        return getFieldErrors();
    }

    boolean hasFieldErrors();
    void resetFieldErrors();
    void resetFieldError(K field);
    LiveData<String> getErrorMessage();
    LiveData<String> getSuccessMessage();
    void resetMessages();
    void clear();

    /**
     * Thread-safe helper to update LiveData values from either main or background threads.
     */
    default <T> void updateLiveData(MutableLiveData<T> liveData, T value) {
        try {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                liveData.setValue(value);
            } else {
                liveData.postValue(value);
            }
        } catch (Exception e) {
            liveData.postValue(value);
        }
    }
}
