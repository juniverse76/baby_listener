package com.juniverse.babylistener;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;

public class AudioSampler extends Thread {

	private AudioRecord recorder = null;
	private static int m_nBufferSize = 0;
	short[] m_soundSample;
	int m_nBytesRead;
	int m_nMaxBytes;

	public AudioSampler()
	{
		m_nBufferSize = AudioRecord.getMinBufferSize(Settings.DEFAULT_SAMPLE_RATE, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
		//m_nMaxBytes = m_nBufferSize * Settings.SAMPLING_TIME;
		m_nMaxBytes = Settings.DEFAULT_SAMPLE_RATE * Settings.SAMPLING_TIME;
		BLDebugger.printLog("AudioSampler.. init.. m_nBufferSize(" + m_nBufferSize + "), m_nMaxBytes(" + m_nMaxBytes + ")");
		m_soundSample = new short[m_nMaxBytes];
	}

	public void run() 
	{
		// AudioRecord »ý¼º
		recorder = new AudioRecord(AudioSource.MIC, Settings.DEFAULT_SAMPLE_RATE, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, m_nBufferSize );
		int res = recorder.getState();
		if ( res == 0 )
		{
			recorder = null;
			return;
		}

		try{
			recorder.startRecording();
		}
		catch (Exception e)
		{
			BLDebugger.printLog("Error while starting the recording!");
			return;
		}

		int totalReadBytes = 0;
		int nByteLeft = m_nMaxBytes;
		BLDebugger.printLog("sampling......");
		while ( nByteLeft > 0 ) {
			m_nBytesRead = recorder.read(m_soundSample, totalReadBytes, nByteLeft);
			totalReadBytes += m_nBytesRead;
			nByteLeft -= m_nBytesRead;
		}

		try{
			recorder.stop();
			recorder.release(); 
			recorder = null;
		}
		catch (Exception e){
			BLDebugger.printLog("Error while stopping the recording!");
		}
		
		trimSilence();
		makeSoundFootPrint();
		BLDebugger.printLog("Done sampling..." + totalReadBytes);
	}

	private void trimSilence()
	{
		
	}
	
	private void makeSoundFootPrint()
	{
		BLDebugger.printLog("makeSoundFootPrint... size? " + m_soundSample.length);
		int length = m_soundSample.length;
		short prev2 = 0, prev = 0, cur;
		int peaks = 0;
		double sumOfPeakSquare = 0;
		try
		{
			
		for ( int i = 0; i < length; i++ )
		{
			cur = m_soundSample[i];
			if ( cur < prev && prev2 < prev )
			{
				peaks++;
				sumOfPeakSquare += (prev * prev);
			}
			prev2 = prev;
			prev = cur;
		}
		}
		catch (Exception e)
		{
		}
		
		float fr = peaks / Settings.SAMPLING_TIME;
		BLDebugger.printLog("makeSoundFootPrint... frequecy? " + fr);
		double rms = Math.sqrt(sumOfPeakSquare / peaks);
		BLDebugger.printLog("makeSoundFootPrint... RMS? " + rms);
	}
}
