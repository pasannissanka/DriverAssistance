package com.example;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsActivity extends AppCompatActivity {
    // app preference keys
    public static final String KEY_APP_PREF_BOOL_DETECTION = "cbp_detection";
    public static final String KEY_APP_PREF_STRING_DEF_SPEED = "lp_default_speed_limit";
    public static final String KEY_APP_PREF_BOOL_METRICS = "cbp_metrics";
    public static final String KEY_MODEL_PREF_DOUBLE_THRESHOLD = "etp_threshold";
    public static final String KEY_MODEL_PREF_DOUBLE_NMS_THRESHOLD = "etp_nms_threshold";
    public static final String KEY_MODEL_PREF_DOUBLE_K_MIN_HITS = "etp_k_min_hits";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            EditTextPreference thresholdPreference = findPreference(KEY_MODEL_PREF_DOUBLE_THRESHOLD);
            setEditPreferenceType(thresholdPreference);

            EditTextPreference nmsThresholdPreference = findPreference(KEY_MODEL_PREF_DOUBLE_NMS_THRESHOLD);
            setEditPreferenceType(nmsThresholdPreference);

            EditTextPreference kMinHitsPreference = findPreference(KEY_MODEL_PREF_DOUBLE_K_MIN_HITS);
            setEditPreferenceType(kMinHitsPreference);
        }

        private void setEditPreferenceType(EditTextPreference editTextPreference) {
            if (editTextPreference != null) {
                editTextPreference.setOnBindEditTextListener(
                        new EditTextPreference.OnBindEditTextListener() {
                            @Override
                            public void onBindEditText(@NonNull EditText editText) {
                                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                            }
                        }
                );
            }
        }
    }
}