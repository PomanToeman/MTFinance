package com.example.mtfinance.src.viewmodels.interfaces;

import androidx.lifecycle.LiveData;
import java.util.Map;

/**
 * Base interface for form ViewModels enforcing validation error maps, individual field error resets,
 * message state management, and form clearing.
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
}
