package com.example.mtfinance.src.viewmodels.interfaces;

/**
 * Interface for deletable form ViewModels supporting both save and delete capabilities.
 * @param <K> the enum type representing field error keys
 */
public interface DeletableFormViewModel<K extends Enum<K>> extends SaveableFormViewModel<K> {
    void delete();
}
