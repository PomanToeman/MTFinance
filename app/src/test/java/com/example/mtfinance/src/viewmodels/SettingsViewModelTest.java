package com.example.mtfinance.src.viewmodels;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.lifecycle.MutableLiveData;

import com.example.mtfinance.InstantExecutorExtension;
import com.example.mtfinance.src.repositories.SystemRepository;
import com.example.mtfinance.src.repositories.SystemService;
import com.example.mtfinance.src.trackingengine.SystemPreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.Executor;

@ExtendWith(InstantExecutorExtension.class)
public class SettingsViewModelTest {

    @Mock
    private SystemService systemService;

    @Mock
    private SystemRepository systemRepository;

    private SettingsViewModel viewModel;
    private MutableLiveData<SystemPreferences> preferencesLiveData;
    private final Executor synchronousExecutor = Runnable::run;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        preferencesLiveData = new MutableLiveData<>(new SystemPreferences("SYSTEM"));
        when(systemService.getPreferencesLive()).thenReturn(preferencesLiveData);
        when(systemService.getPreferencesSync()).thenReturn(new SystemPreferences("SYSTEM"));

        viewModel = new SettingsViewModel(systemService, systemRepository, synchronousExecutor);
    }

    @Test
    public void getSystemPreferences_returnsServiceLiveData() {
        assertAll(
            () -> assertNotNull(viewModel.getSystemPreferences()),
            () -> assertEquals("SYSTEM", viewModel.getSystemPreferences().getValue().getThemeMode())
        );
    }

    @Test
    public void setProvisionalThemeMode_andSave_delegatesToRepository() {
        viewModel.setProvisionalThemeMode("DARK");
        viewModel.saveChanges();
        verify(systemRepository).updatePreferences(any());
    }
}
