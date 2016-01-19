package com.juniverse.babylistener;

import java.lang.Thread.State;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Contacts;
import android.provider.ContactsContract;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

//import com.google.ads.AdRequest;
//import com.google.ads.AdView;

public class BabyListener extends Activity {

    //private String mVersion;
    public static final int PICK_CONTACT = 1;
    public static final String ACTION_DETECT_STARTED = "com.juniverse.action.ACTION_DETECT_STARTED";
    
    public static final String KEY_PHONENUMBER = "number";
    public static final String KEY_SENSITIVITY = "sensitivity";
    public static final String KEY_FIRST_TIME = "firsttime";
    public static final String KEY_STOP_DETECT = "stopdetect";
    public static final String KEY_STARTED = "keystarted";
    
    private Boolean m_bTestMode = false;
    private AudioDetector detector = null;
    
    private Button btnStart;
    private EditText editPhoneNumber;
    private SeekBar sbSensitivity;
    private String szPhoneNumber;
    private int nSenseLevel;
    private TextView sensitive_text = null;
    private TextView dull_text = null;
    
    private final int MIN_TEXT_SIZE = 14;
    private final int TEXT_SIZE_RANGE = 6;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        savedInstanceState = getIntent().getExtras();
        boolean stopDetect = false;
        if ( savedInstanceState != null )
	        stopDetect = savedInstanceState.getBoolean(KEY_STOP_DETECT, false);
        
        BLDebugger.printLog("BabyListener OnCreate... stopDetect? " + stopDetect);
        
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        szPhoneNumber = pref.getString(KEY_PHONENUMBER, "");
        nSenseLevel = pref.getInt(KEY_SENSITIVITY, 0);

        setContentView(R.layout.main);

	    sensitive_text = (TextView) findViewById(R.id.sensitivy_text);
	    dull_text = (TextView) findViewById(R.id.dull_text);
        editPhoneNumber = (EditText) findViewById(R.id.e_phonenumber);
        sbSensitivity = (SeekBar) findViewById(R.id.s_sensitivity);
	    
        btnStart = (Button) findViewById(R.id.b_start);
        btnStart.setOnClickListener(btnStartListener);
        
        if ( isMyServiceRunning() ) 
        {
        	if ( stopDetect )
        	{
        		stopDetectService();
        	}
        	else
        	{
        		Intent intent = new Intent(this, BabyListening.class);
        		startActivity(intent);
        	}
        }
        
        final Button btnSearch = (Button) findViewById(R.id.b_search);
        btnSearch.setOnClickListener(btnSearchListener);

        final ImageView babyPicture = (ImageView) findViewById(R.id.i_test_image);
        babyPicture.setOnClickListener(babyPicClickListener);

        sbSensitivity.setOnSeekBarChangeListener(sbChangeListener);

        setSensitivityText(nSenseLevel);
        
        editPhoneNumber.setText(szPhoneNumber);
        sbSensitivity.setProgress(nSenseLevel);

        BLDebugger.printLog("BabyListener OnCreate end");
        
        BroadcastReceiver detectStartedReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				Bundle b = intent.getExtras();
				boolean started = false;
				if ( b != null )
					started = b.getBoolean(KEY_STARTED, false);
				
				if ( started )
				{
					btnStart.setText(R.string.label_listening_text);
					editPhoneNumber.setEnabled(false);
					sbSensitivity.setEnabled(false);
				}
				else
				{
					btnStart.setText(R.string.label_start);
					editPhoneNumber.setEnabled(true);
					sbSensitivity.setEnabled(true);
				}
			}
		};
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_DETECT_STARTED);
		registerReceiver(detectStartedReceiver, filter);
    }


    @Override
    protected void onResume() {
        super.onResume();
        final ImageView babyPicture = (ImageView) findViewById(R.id.i_test_image);
        babyPicture.setImageResource(R.drawable.just_baby);
        
        if ( isMyServiceRunning() )
        {
			btnStart.setText(R.string.label_listening_text);
			editPhoneNumber.setEnabled(false);
			sbSensitivity.setEnabled(false);
        }
        else
        {
			btnStart.setText(R.string.label_start);
			editPhoneNumber.setEnabled(true);
			sbSensitivity.setEnabled(true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        BLDebugger.printLog("BabyListener onPause ");
        if ( detector != null && detector.getState() == State.RUNNABLE )
        {
            detector.stopDetection();
            detector.interrupt();
        }
        szPhoneNumber = editPhoneNumber.getText().toString();
        saveVariables(szPhoneNumber, nSenseLevel);

        BLDebugger.printLog("BabyListener onPause end");
    }

	private void saveVariables(final String szPhoneNumber, final int nSenseLevel) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(KEY_PHONENUMBER, szPhoneNumber);
        editor.putInt(KEY_SENSITIVITY, nSenseLevel);
        editor.commit();
        BLDebugger.printLog("saving variables... " + szPhoneNumber + ", " + nSenseLevel);
	}

    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        switch (reqCode) {
            case (PICK_CONTACT) :
                if (resultCode == Activity.RESULT_OK) {
                    Uri contactData = data.getData();
                    Cursor c =  managedQuery(contactData, null, null, null, null);
                    if (c.moveToFirst()) {
                    	String number;
                    	if ( Build.VERSION.SDK_INT > 5 )
                    		number = c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    	else
                    		number = c.getString(c.getColumnIndexOrThrow(Contacts.Phones.NUMBER));

                        final EditText editPhoneNumber = (EditText) findViewById(R.id.e_phonenumber);
                        editPhoneNumber.setText(number);
                    }
                }
            break;
        }
    }

    private Button.OnClickListener btnSearchListener =
        new Button.OnClickListener() {
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_PICK);
            if ( Build.VERSION.SDK_INT > 5 )
	            intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
            else
	            intent.setType(Contacts.Phones.CONTENT_TYPE);
            startActivityForResult(intent, PICK_CONTACT);
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.MENU_SETTING )
        {
            Intent intent = new Intent(BabyListener.this, Settings.class);
            startActivity(intent);
            return true;
        }
        else if ( item.getItemId() == R.id.MENU_REPORT )
        {
			final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
			emailIntent.setType("plain/text");
			emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"juniverse76@gmail.com"});
			emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "[" + getResources().getString(R.string.app_name) + "]");
			startActivity(Intent.createChooser(emailIntent, "Send mail..."));

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

	private Button.OnClickListener btnStartListener =
        new Button.OnClickListener() {
        public void onClick(View v) {
        	if ( isMyServiceRunning() )
        	{
        		stopDetectService();
				btnStart.setText(R.string.label_start);
				editPhoneNumber.setEnabled(true);
				sbSensitivity.setEnabled(true);
        		return;
        	}
        	
            szPhoneNumber = editPhoneNumber.getText().toString();
            if ( szPhoneNumber.length() <= 0 )
            {
            	Toast.makeText(getApplicationContext(), R.string.text_no_number, Toast.LENGTH_SHORT).show();
                return;
            }
            
	        saveVariables(szPhoneNumber, nSenseLevel);
	        
	        Intent intent = new Intent(getApplicationContext(), BabyListening.class);
	        startActivity(intent);
	        
        }
    };

    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ((getApplicationInfo().packageName + ".DetectorService").equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    
    private ImageView.OnClickListener babyPicClickListener =
        new ImageView.OnClickListener() {
        public void onClick(View v) {

        	if ( isMyServiceRunning() )
        		return;
        	
            State state;
            if ( detector == null )
            {
                detector = new AudioDetector();
                detector.setAlertListener(onAlertListener);
            }

            state = detector.getState();
            BLDebugger.printLog("detector state : " + state.name());

            ImageView thisImg = (ImageView)v;
            if ( state == State.RUNNABLE && m_bTestMode )
            {
                m_bTestMode = false;
                thisImg.setImageResource(R.drawable.just_baby);

                detector.stopDetection();
                detector.interrupt();
                //detector = null;
            }
            else if ( state == State.NEW || state == State.TERMINATED )
            {
                if ( state == State.TERMINATED )
                {
                    detector = new AudioDetector();
                    detector.setAlertListener(onAlertListener);
                }
                m_bTestMode = true;
                thisImg.setImageResource(R.drawable.sleeping_baby);

                detector.setDetectLevel(nSenseLevel);
                BLDebugger.printLog("request startDetection!!!!!!!!!!!!");
                detector.initialize();
                detector.start();
                BLDebugger.printLog("Detection STARTED!!!!!!!!!!!!");
            }
        }
    };

    private SeekBar.OnSeekBarChangeListener sbChangeListener =
        new SeekBar.OnSeekBarChangeListener() {

        public void onProgressChanged(SeekBar bar, int progress, boolean fromTouch) {
        	setSensitivityText(progress);
        }

        public void onStartTrackingTouch(SeekBar bar) {
        }

        public void onStopTrackingTouch(SeekBar bar) {
        	nSenseLevel = bar.getProgress();
        	if ( detector != null )
        		detector.setDetectLevel(nSenseLevel);
        }
    };

    private AudioDetector.OnAlertListener onAlertListener = 
        new AudioDetector.OnAlertListener() {
        public void onAlert()
        {
            runOnUiThread(new Runnable() {
                public void run() {
                    ImageView thisImg = (ImageView) findViewById(R.id.i_test_image);
                    thisImg.setImageResource(R.drawable.crying_baby);
                }
            });
            detector.stopDetection();
            detector.interrupt();
        }
    };

	private void setSensitivityText(int progress) {
		float percentage = ((float)progress) / sbSensitivity.getMax();
        
		int rg_for_sensitive = (int) (0x0000007f * (1 - percentage)) + 0x0000007f;
		int rg_for_dull = (int) (0x0000007f * percentage) + 0x0000007f;
		dull_text.setTextColor(0xff000000 + (rg_for_dull * 0x00010000) + ((0x000000ff - rg_for_sensitive) * 0x00000100) + (0x000000ff - rg_for_sensitive) );
		sensitive_text.setTextColor(0xff000000 + (rg_for_sensitive * 0x00010000) + ((0x000000ff - rg_for_dull) * 0x00000100) + (0x000000ff - rg_for_dull) );
		
        float dullSp = MIN_TEXT_SIZE + (percentage * TEXT_SIZE_RANGE);
        float sensitiveSp = MIN_TEXT_SIZE + ( (1 - percentage) * TEXT_SIZE_RANGE);
        sensitive_text.setTextSize(sensitiveSp);
        dull_text.setTextSize(dullSp);
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