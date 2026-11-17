package com.example.mtfinance.src.viewmodels;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import androidx.lifecycle.MutableLiveData;

import com.example.mtfinance.InstantExecutorExtension;
import com.example.mtfinance.src.repositories.SystemRepository;
import com.example.mtfinance.src.repositories.SystemService;
import com.example.mtfinance.src.trackingengine.SettingsPreferenceViolationException;
import com.example.mtfinance.src.trackingengine.SystemPreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.Executor;

@ExtendWith(InstantExecutorExtension.class)
public class SettingsViewModelValidationTest {

    @Mock
    private SystemService systemService;

    @Mock
    private SystemRepository systemRepository;

    private SettingsViewModel viewModel;
    private final Executor synchronousExecutor = Runnable::run;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        SystemPreferences prefs = new SystemPreferences("SYSTEM");
        when(systemService.getPreferencesLive()).thenReturn(new MutableLiveData<>(prefs));
        when(systemService.getPreferencesSync()).thenReturn(prefs);

        viewModel = new SettingsViewModel(systemService, systemRepository, synchronousExecutor);
        viewModel.getFieldErrors().observeForever(e -> {});
        viewModel.getChangesMade().observeForever(c -> {});
        viewModel.getSaveStatusColor().observeForever(s -> {});
        viewModel.getGenericErrorMessage().observeForever(g -> {});
    }

    @Test
    public void setProvisionalStartDay_emptyString_populatesEmptyFieldError() {
        viewModel.setProvisionalStartDayOfMonth("");

        assertAll(
            () -> assertTrue(viewModel.hasFieldErrors()),
            () -> assertEquals("RED", viewModel.getSaveStatusColor().getValue()),
            () -> assertTrue(viewModel.getFieldErrors().getValue().get(SettingsViewModel.SystemPreferenceErrorFields.START_DAY_OF_MONTH).contains("cannot be empty"))
        );
    }

    @Test
    public void setProvisionalStartDay_invalidDay_populatesFieldErrorAndRedStatus() {
        viewModel.setProvisionalStartDayOfMonth("99");

        assertAll(
            () -> assertTrue(viewModel.hasFieldErrors()),
            () -> assertEquals("RED", viewModel.getSaveStatusColor().getValue()),
            () -> assertTrue(viewModel.getFieldErrors().getValue().get(SettingsViewModel.SystemPreferenceErrorFields.START_DAY_OF_MONTH).contains("between 1 and 31"))
        );
    }

    @Test
    public void setProvisionalThemeMode_validChange_setsOrangeStatusAndChangesMade() {
        viewModel.setProvisionalThemeMode("DARK");

        assertAll(
            () -> assertFalse(viewModel.hasFieldErrors()),
            () -> assertTrue(viewModel.getChangesMade().getValue()),
            () -> assertEquals("ORANGE", viewModel.getSaveStatusColor().getValue())
        );
    }

    @Test
    public void saveChanges_whenExceptionThrown_rollsBackAndSetsGenericErrorMessage() {
        viewModel.setProvisionalThemeMode("DARK");
        doThrow(new SettingsPreferenceViolationException("Invalid Preference"))
                .when(systemRepository).updatePreferences(any());

        viewModel.saveChanges();

        assertAll(
            () -> assertEquals("RED", viewModel.getSaveStatusColor().getValue()),
            () -> assertTrue(viewModel.getGenericErrorMessage().getValue().contains("Preference configuration violation occurred")),
            () -> assertEquals("SYSTEM", viewModel.getProvisionalThemeMode().getValue())
        );
    }
}
