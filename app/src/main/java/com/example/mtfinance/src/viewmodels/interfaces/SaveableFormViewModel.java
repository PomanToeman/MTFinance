package com.example.mtfinance.src.viewmodels.interfaces;

/**
 * Interface for saveable form ViewModels.
 * @param <K> the enum type representing field error keys
 */
public interface SaveableFormViewModel<K extends Enum<K>> extends BaseFormViewModel<K> {
    void save();
}
