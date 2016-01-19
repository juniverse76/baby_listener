package com.juniverse.babylistener;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class DetectStopActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		stopDetectService();
		finish();
	}
	
	private void stopDetectService() {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		boolean temp = pref.getBoolean("onpause", true);
		SharedPreferences.Editor editor = pref.edit();
		editor.putBoolean("onpause_temp", temp);
		editor.putBoolean("onpause", false);
		editor.commit();

		stopService(new Intent(getApplicationContext(), DetectorService.class));
	}

}
