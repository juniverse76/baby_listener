package com.juniverse.babylistener;

import android.media.AudioRecord;
import android.media.AudioFormat;
import android.media.MediaRecorder.AudioSource;

    // TODO 마이크에서 계속 음성데이터를 받아야 한다.
    // 이거를 따로 처리하는 클래스를 만들어서 테스트 시에도 사용할 수 있게 하자.
public class AudioDetector extends Thread {
	abstract interface OnAlertListener
	{
		public abstract void onAlert();
	}
	
	abstract interface OnCallCheckListener
	{
		public abstract boolean onCheck();
	}
	
	private boolean mPaused = false;
	private volatile boolean m_bStopDetection = false;
	
	private static int m_nBufferSize = 0;
	short[] m_soundSample;
	int m_nBytesRead;
	
	private AudioRecord recorder = null;
	private int detectLevel;
	private OnAlertListener alertListener = null;
	private OnCallCheckListener checkListener = null;
	private final int MAX_DETECT_LENGTH = 100;
	private final int DEFAULT_DETECT_LEVEL = 1000;
	private final int PAUSE_SLEEP_TIME = 5000;
	
	public AudioDetector()
	{
		m_nBufferSize = AudioRecord.getMinBufferSize(Settings.DEFAULT_SAMPLE_RATE, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
		m_soundSample = new short[m_nBufferSize];
	}
	
	public void run() {
		// AudioRecord 생성
		recorder = new AudioRecord(AudioSource.MIC, Settings.DEFAULT_SAMPLE_RATE, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, m_nBufferSize );
		int res = recorder.getState();
		if ( res == 0 )
		{
			recorder = null;
			return;
		}

		mPaused = false;
		
		// 녹음 시작...
		try{
			recorder.startRecording();
		}
		catch (Exception e)
		{
		    BLDebugger.printLog("Error while starting the recording!");
			return;
		}
		
        while (!m_bStopDetection) {
        	if ( mPaused && (checkListener != null) )
        	{
				try {
					sleep(PAUSE_SLEEP_TIME);
					if ( !checkListener.onCheck() )
						continue;
					
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
        	}
 
        	m_nBytesRead = recorder.read(m_soundSample, 0, m_nBufferSize);
        	if ( doDetection() )
        		alertListener.onAlert();
        }
        
        if (m_bStopDetection)
            BLDebugger.printLog("STOP!!!");
        
		try{
			recorder.stop();
			recorder.release(); 
			recorder = null;
		}
		catch (Exception e){
		    BLDebugger.printLog("Error while stopping the recording!");
		}
	}
	
	// 0~100
	public void setDetectLevel(int nDetectLevel)
	{
		detectLevel = nDetectLevel + 1;
		if ( detectLevel > 100 )
			detectLevel = 100;
		detectLevel *= DEFAULT_DETECT_LEVEL;
		BLDebugger.printLog("detectLevel... " + Integer.toString(detectLevel));
	}
	
	public void setCallCheckListener(OnCallCheckListener listener)
	{
		checkListener = listener;
	}
	
	public void setAlertListener(OnAlertListener listener)
	{
		alertListener = listener;
	}
	
	public void stopDetection()
	{
		m_bStopDetection = true;
	}
	
	public boolean initialize()
	{
		if ( alertListener == null )
			return false;
		
		m_bStopDetection = false;
		return true;
	}
	
	public void pauseDetect()
	{
		mPaused = true;
	}

	public void resumeDetect()
	{
		mPaused = false;
	}

	private boolean doDetection()
	{
		int tracer, startPoint, endPoint, hits;
		startPoint = (m_nBytesRead + MAX_DETECT_LENGTH) >> 1;
		endPoint = startPoint + MAX_DETECT_LENGTH;
		if ( endPoint >= m_nBytesRead )
		{
			startPoint = 0;
			endPoint = m_nBytesRead - 1;
		}
        
		hits = 0;
        for ( tracer = startPoint; tracer < endPoint; tracer++ )
        {
        	if ( m_soundSample[tracer] > detectLevel || m_soundSample[tracer] < -detectLevel )
        	{
        		hits++;
        	}
        }
        if ( hits > 10 )
        	return true;
		return false;
	}
}