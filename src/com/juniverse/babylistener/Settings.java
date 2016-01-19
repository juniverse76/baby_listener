package com.juniverse.babylistener;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;

// test
public class Settings extends PreferenceActivity
{
	public static final int DEFAULT_SAMPLE_RATE = 8000; 
	public static final int SAMPLING_TIME = 5;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        addPreferencesFromResource(R.xml.settings);
        
        String KEY_PASSWORD = getResources().getString(R.string.key_waiting_time);
        Preference p = findPreference(KEY_PASSWORD);
		String pass = p.getSharedPreferences().getString(KEY_PASSWORD, "5");
		int seconds = 0;
		try {
			seconds = Integer.parseInt(pass);
		}
		catch (Exception e)
		{
			seconds = 0;
		}
		
		p.setSummary(getResources().getString(R.string.setting_time_before_start_des, seconds));
		
		p.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				int seconds = 0;
				try {
					seconds = Integer.parseInt((String)newValue);
				}
				catch (Exception e)
				{
					seconds = 0;
				}
				preference.setSummary(getResources().getString(R.string.setting_time_before_start_des, seconds));
				return true;
			}
		});

    }
    
}
