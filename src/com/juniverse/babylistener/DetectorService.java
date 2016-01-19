package com.juniverse.babylistener;

import java.lang.Thread.State;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.widget.Toast;

public class DetectorService extends Service 
{
    private AudioDetector detector = null;
    private int mSenseLevel;
    private String mPhoneNumber;
	private NotificationManager mNM;
	private int NOTIFICATION = R.string.service_started;
    private int nPrevRingerMode;

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onStart(Intent intent, int startId) {
    	startDetector(intent, startId);
    	
	}

	private void startDetector(Intent intent, int startId) {
		BLDebugger.printLog("DetectorService onStart ");
    	
        // 매너모드로 전환.
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        nPrevRingerMode = audioManager.getRingerMode();
        BLDebugger.printLog("nPrevRingerMode get? " + nPrevRingerMode);
        audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        
        Bundle b = intent.getExtras();
        mSenseLevel = -1;
        mPhoneNumber = null;
        if ( b != null )
        {
        	mSenseLevel = b.getInt(BabyListener.KEY_SENSITIVITY);
        	mPhoneNumber = b.getString(BabyListener.KEY_PHONENUMBER);
        }
        
        if ( mSenseLevel < 0 || mPhoneNumber == null )
        {
	        stopSelf(startId);
        	return;
        }
        
        if ( startDetect() )
        {
        	Intent actionIntent = new Intent();
        	actionIntent.setAction(BabyListener.ACTION_DETECT_STARTED);
        	actionIntent.putExtra(BabyListener.KEY_STARTED, true);
        	sendBroadcast(actionIntent);
        }
	}

	@Override
	public void onCreate() {
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		showNotification();
	}

    @Override
    public void onDestroy() {
        mNM.cancel(NOTIFICATION);
        Toast.makeText(this, R.string.message_listening_mode_ended, Toast.LENGTH_SHORT).show();
        stopDetect();
        
    	Intent actionIntent = new Intent();
    	actionIntent.setAction(BabyListener.ACTION_DETECT_STARTED);
    	actionIntent.putExtra(BabyListener.KEY_STARTED, false);
    	sendBroadcast(actionIntent);
        
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean send = pref.getBoolean("onpause", true);
    	BLDebugger.printLog("sending sms...? " + send);
        if ( send )
        {
	        final SmsManager sms = SmsManager.getDefault();
	        sms.sendTextMessage(mPhoneNumber, null, getString(R.string.message_listening_mode_ended), null, null);
        }
        
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("onpause", pref.getBoolean("onpause_temp", true));
        editor.commit();
        
        BLDebugger.printLog("nPrevRingerMode put? " + nPrevRingerMode);
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setRingerMode(nPrevRingerMode);
    }
    
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
    	startDetector(intent, startId);
		
		return START_STICKY;
	}

	private void showNotification() {
		CharSequence text = getText(R.string.msg_started);
		Notification notification = new Notification(R.drawable.ear_icon, text, System.currentTimeMillis());
		Intent intent = new Intent(this, DetectStopActivity.class);
		intent.putExtra(BabyListener.KEY_STOP_DETECT, true);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
		notification.setLatestEventInfo(this, getText(R.string.msg_stop_dected), text, contentIntent);
		mNM.notify(NOTIFICATION, notification);
	}

	private boolean startDetect()
	{
        State state;
        if ( detector == null )
        {
            detector = new AudioDetector();
            detector.setAlertListener(onAlertListener);
        }

        state = detector.getState();

        if ( state == State.NEW || state == State.TERMINATED )
        {
            if ( state == State.TERMINATED )
            {
                detector = new AudioDetector();
                detector.setAlertListener(onAlertListener);
                detector.setCallCheckListener(onCheckListener);
            }

            detector.setDetectLevel(mSenseLevel);
            detector.initialize();
            detector.start();
            
            return true;
        }
        
        return false;
    }

	private void stopDetect()
	{
        if ( detector == null )
        	return;
        
        State state = detector.getState();
        if ( state == State.RUNNABLE )
        {
            detector.stopDetection();
            detector.interrupt();
        }
	}

    private AudioDetector.OnAlertListener onAlertListener = 
        new AudioDetector.OnAlertListener() {
        public void onAlert()
        {
        	detector.pauseDetect();
        	
        	// make call
            String targetNumber = "tel://" + mPhoneNumber;
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse(targetNumber));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    };

    private AudioDetector.OnCallCheckListener onCheckListener = new AudioDetector.OnCallCheckListener() {
		public boolean onCheck() {
			TelephonyManager tm = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
			int state = 0;
			if ( tm != null )
				state = tm.getCallState();

			if ( state == TelephonyManager.CALL_STATE_IDLE )
			{
	        	detector.resumeDetect();
				return true;
			}
			
			return false;
		}
	};
}
