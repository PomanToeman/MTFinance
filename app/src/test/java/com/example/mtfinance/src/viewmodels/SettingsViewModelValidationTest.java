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
import com.example.mtfinance.src.CurrencyFormat;
import com.example.mtfinance.src.repositories.SystemRepository;
import com.example.mtfinance.src.repositories.SystemService;
import com.example.mtfinance.src.repositories.TrackingRepository;
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

    private static final String DEFAULT_THEME_SYSTEM = "SYSTEM";
    private static final String THEME_DARK = "DARK";
    private static final String INVALID_START_DAY_OUT_OF_RANGE = "99";
    private static final String EMPTY_STRING = "";
    private static final String STATUS_RED = "RED";
    private static final String STATUS_ORANGE = "ORANGE";

    @Mock
    private SystemService systemService;

    @Mock
    private SystemRepository systemRepository;

    @Mock
    private TrackingRepository trackingRepository;

    @Mock
    private java.util.logging.Logger logger;

    private SettingsViewModel viewModel;
    private final Executor synchronousExecutor = Runnable::run;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        SystemPreferences prefs = new SystemPreferences(DEFAULT_THEME_SYSTEM);
        when(systemService.getPreferencesLive()).thenReturn(new MutableLiveData<>(prefs));
        when(systemService.getPreferencesSync()).thenReturn(prefs);

        viewModel = new SettingsViewModel(systemService, systemRepository, trackingRepository, synchronousExecutor, logger);
        viewModel.getFieldErrors().observeForever(e -> {});
        viewModel.getChangesMade().observeForever(c -> {});
        viewModel.getSaveStatusColor().observeForever(s -> {});
        viewModel.getGenericErrorMessage().observeForever(g -> {});
    }

    @Test
    public void setProvisionalStartDay_emptyString_populatesEmptyFieldError() {
        viewModel.setProvisionalStartDayOfMonth(EMPTY_STRING);

        assertAll(
            () -> assertTrue(viewModel.hasFieldErrors()),
            () -> assertEquals(STATUS_RED, viewModel.getSaveStatusColor().getValue()),
            () -> assertTrue(viewModel.getFieldErrors().getValue().get(SettingsViewModel.SystemPreferenceErrorFields.START_DAY_OF_MONTH).contains("cannot be empty"))
        );
    }

    @Test
    public void setProvisionalStartDay_invalidDay_populatesFieldErrorAndRedStatus() {
        viewModel.setProvisionalStartDayOfMonth(INVALID_START_DAY_OUT_OF_RANGE);

        assertAll(
            () -> assertTrue(viewModel.hasFieldErrors()),
            () -> assertEquals(STATUS_RED, viewModel.getSaveStatusColor().getValue()),
            () -> assertTrue(viewModel.getFieldErrors().getValue().get(SettingsViewModel.SystemPreferenceErrorFields.START_DAY_OF_MONTH).contains("between 1 and 31"))
        );
    }

    @Test
    public void setProvisionalThemeMode_validChange_setsOrangeStatusAndChangesMade() {
        viewModel.setProvisionalThemeMode(THEME_DARK);

        assertAll(
            () -> assertFalse(viewModel.hasFieldErrors()),
            () -> assertTrue(viewModel.getChangesMade().getValue()),
            () -> assertEquals(STATUS_ORANGE, viewModel.getSaveStatusColor().getValue())
        );
    }

    @Test
    public void multipleUpdates_sequentialChanges_evaluatesStateCorrectly() {
        viewModel.setProvisionalThemeMode("DARK");
        viewModel.setProvisionalCurrencyFormat(CurrencyFormat.USD);
        viewModel.setProvisionalStartDayOfMonth("10");

        assertAll(
            () -> assertFalse(viewModel.hasFieldErrors()),
            () -> assertEquals(SettingsSaveState.PROVISIONAL, viewModel.getSaveState().getValue()),
            () -> assertEquals("DARK", viewModel.getProvisionalThemeMode().getValue()),
            () -> assertEquals(CurrencyFormat.USD, viewModel.getProvisionalCurrencyFormat().getValue()),
            () -> assertEquals("10", viewModel.getProvisionalStartDayOfMonthInput().getValue())
        );
    }

    @Test
    public void saveChanges_whenExceptionThrown_rollsBackAndSetsGenericErrorMessage() {
        viewModel.setProvisionalThemeMode(THEME_DARK);
        doThrow(new SettingsPreferenceViolationException("Invalid Preference"))
                .when(systemRepository).updatePreferences(any());

        viewModel.saveChanges();

        assertAll(
            () -> assertEquals(STATUS_RED, viewModel.getSaveStatusColor().getValue()),
            () -> assertTrue(viewModel.getGenericErrorMessage().getValue().contains("Preference configuration violation occurred")),
            () -> assertEquals(DEFAULT_THEME_SYSTEM, viewModel.getProvisionalThemeMode().getValue())
        );
    }
}
