package com.juniverse.babylistener;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class BabyListening extends Activity {
	private static final int START_COUNTDOWN = 1;
	
	private int nSenseLevel;
	private String szPhoneNumber;
	private boolean bOnPause = false;
	private boolean bOnResume = false;
	private int nWaitingTime = 5;

	private TextView mMessageText;
	final Handler mHandler = new Handler( )
	{
		@Override
		public void handleMessage(Message msg) {
			if ( msg.what == START_COUNTDOWN )
			{
				nWaitingTime = msg.arg1;
				
				if ( nWaitingTime <= 0 )
				{
					mMessageText.setText(R.string.label_listening);
					startDetectService();
				}
				else
				{
					mMessageText.setText(getResources().getQuantityString(R.plurals.label_start_countdown, nWaitingTime, nWaitingTime));
					msg = mHandler.obtainMessage(START_COUNTDOWN);
					msg.arg1 = nWaitingTime - 1;
					mHandler.sendMessageDelayed(msg, 1000);
				}
				
			}
			super.handleMessage(msg);
		}
	};
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		BLDebugger.printLog("BabyListening onCreate");

		setContentView(R.layout.listen_mode);
		mMessageText = (TextView)findViewById(R.id.mode_text);

		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);

		szPhoneNumber = pref.getString(BabyListener.KEY_PHONENUMBER, "");
		nSenseLevel = pref.getInt(BabyListener.KEY_SENSITIVITY, 0);
		bOnPause = pref.getBoolean("onpause", bOnPause);
		bOnResume = pref.getBoolean("onresume", bOnResume);

		findViewById(R.id.mode_text).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if ( nWaitingTime > 0 )
					mHandler.removeMessages(START_COUNTDOWN);
				stopDetectService();
				nWaitingTime = 0;
				finish();
			}
		});
		
        BroadcastReceiver detectStartedReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				Bundle b = intent.getExtras();
				boolean started = false;
				if ( b != null )
					started = b.getBoolean(BabyListener.KEY_STARTED, false);
				
				if ( !started )
				{
					finish();
				}
			}
		};
		IntentFilter filter = new IntentFilter();
		filter.addAction(BabyListener.ACTION_DETECT_STARTED);
		registerReceiver(detectStartedReceiver, filter);

		if ( isMyServiceRunning() )
		{
			mMessageText.setText(R.string.label_listening);
			nWaitingTime = 0;
			return;
		}

		String wt = pref.getString(getResources().getString(R.string.key_waiting_time), "5");
		try
		{
			nWaitingTime = Integer.parseInt(wt);
		}
		catch(Exception e)
		{
			nWaitingTime = 0;
		}

		if ( nWaitingTime <= 0 )
		{
			startDetectService();
			mMessageText.setText(R.string.label_listening);
		}
		else
		{
			Message msg = mHandler.obtainMessage(START_COUNTDOWN);
			msg.arg1 = nWaitingTime - 1;
			mHandler.sendMessageDelayed(msg, 1000);
			mMessageText.setText(getResources().getQuantityString(R.plurals.label_start_countdown, nWaitingTime, nWaitingTime));
		}
	}

	@Override
	protected void onPause() {
		if ( nWaitingTime > 0 )
		{
			mHandler.removeMessages(START_COUNTDOWN);
//			Toast.makeText(this, R.string.message_listening_mode_canceled, Toast.LENGTH_LONG).show();
			startDetectService();
			finish();
		}
		
		super.onPause();
	}


	private void startDetectService() {
		Intent intent = new Intent(getApplicationContext(), DetectorService.class);
		intent.putExtra(BabyListener.KEY_PHONENUMBER, szPhoneNumber);
		intent.putExtra(BabyListener.KEY_SENSITIVITY, nSenseLevel);
		startService(intent);
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

    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ((getApplicationInfo().packageName + ".DetectorService").equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

}