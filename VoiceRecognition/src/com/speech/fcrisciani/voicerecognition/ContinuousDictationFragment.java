package com.speech.fcrisciani.voicerecognition;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;

public class ContinuousDictationFragment extends Fragment implements RecognitionListener{
	// ----- INTERFACES ----- //
	// Container Activity must implement this interface as a callback for results and dictation flow
		public interface ContinuousDictationFragmentResultsCallback {
			public void onDictationStart();
			public void onResults(ContinuousDictationFragment delegate, ArrayList<String> dictationResults);
			public void onDictationFinish();
		}
		
	// ----- TYPES ----- //
	// Timer task used to reproduce the timeout input error that seems not be called on android 4.1.2
	public class SilenceTimer extends TimerTask {
		@Override
		public void run() {
			onError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT);
		}
	}
	
	// ---- MEMBERS ---- //
	// Callback activity called following dictation process
	private ContinuousDictationFragmentResultsCallback mCallback;
	// Logger tag
	private static final String TAG = "" + ContinuousDictationFragment.class;
	// Speech recognizer instance
	private SpeechRecognizer speech = null;
	// Speech recognition control button
	private ImageButton controlBtn = null;
	// Timer used as timeout for the speech recognition
	private Timer speechTimeout = null;
	
	// ---- METHODS ---- //
	// Method used to update button image as visual feedback of recognition service
	private void buttonChangeState(int state){
		switch (state) {
			case 0:
				controlBtn.setImageDrawable(getResources().getDrawable(R.drawable.white));
				break;
			case 1:
				controlBtn.setImageDrawable(getResources().getDrawable(R.drawable.red));
				break;
			case 2:
				controlBtn.setImageDrawable(getResources().getDrawable(R.drawable.yellow));
				break;
			case 3:
				controlBtn.setImageDrawable(getResources().getDrawable(R.drawable.green));
				break;
			default:
				break;
		}
	}
	// Lazy instantiation method for getting the speech recognizer
	private SpeechRecognizer getSpeechRevognizer(){
		if (speech == null) {
			speech = SpeechRecognizer.createSpeechRecognizer(getActivity());
			speech.setRecognitionListener(this);
		}
		
		return speech;
	}
	/** 
	 * Default constructor 
	 */
	public ContinuousDictationFragment() {
	}

	/**
	 *  onAttach(Activity) called once the fragment is associated with its activity.
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// This makes sure that the container activity has implemented
		// the callback interface. If not, it throws an exception
		try {
			mCallback = (ContinuousDictationFragmentResultsCallback) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement DictationFragmentResultsCallback");
		}
	}

	/**
	 * onCreateView(LayoutInflater, ViewGroup, Bundle) creates and returns the view hierarchy associated with the fragment.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.dictation_fragment, container, false);
		// Associate the button from the interface
		controlBtn = (ImageButton) view.findViewById(R.id.dictationStateButton);
		buttonChangeState(0);
		// Handling method for the button
		controlBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (speech == null) {
					buttonChangeState(1);
					startVoiceRecognitionCycle();
				}
				else {
					buttonChangeState(1);
					stopVoiceRecognition();
				}
			}
		});

		return view;
	}

	/**
	 * Fire an intent to start the voice recognition process.
	 */
	public void startVoiceRecognitionCycle()
	{
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		getSpeechRevognizer().startListening(intent);
		buttonChangeState(1);
	}

	/**
	 * Stop the voice recognition process and destroy the recognizer.
	 */
	public void stopVoiceRecognition()
	{
		speechTimeout.cancel();
		if (speech != null) {
			speech.destroy();

			speech = null;
		}
		
		buttonChangeState(0);
	}

	/* RecognitionListener interface implementation */
	
	@Override
	public void onReadyForSpeech(Bundle params) {
		Log.d(TAG,"onReadyForSpeech");
		// create and schedule the input speech timeout
		speechTimeout = new Timer();
		speechTimeout.schedule(new SilenceTimer(), 3000);
		buttonChangeState(3);
	}
	
	@Override
	public void onBeginningOfSpeech() {
		Log.d(TAG,"onBeginningOfSpeech");
		// Cancel the timeout because voice is arriving
		speechTimeout.cancel();
		buttonChangeState(2);
		// Notify the container activity that dictation is started
		mCallback.onDictationStart();
	}

	@Override
	public void onBufferReceived(byte[] buffer) {
		Log.d(TAG,"onBufferReceived");
	}

	@Override
	public void onEndOfSpeech() {
		Log.d(TAG,"onEndOfSpeech");
		buttonChangeState(0);
		// Notify the container activity that dictation is finished
		mCallback.onDictationFinish();
	}

	@Override
	public void onError(int error) {
		String message;
		boolean restart = true;
		switch (error)
		{
			case SpeechRecognizer.ERROR_AUDIO:
				message = "Audio recording error";
				break;
			case SpeechRecognizer.ERROR_CLIENT:
				message = "Client side error";
				restart = false;
				break;
			case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
				message = "Insufficient permissions";
				restart = false;
				break;
			case SpeechRecognizer.ERROR_NETWORK:
				message = "Network error";
				break;
			case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
				message = "Network timeout";
				break;
			case SpeechRecognizer.ERROR_NO_MATCH:
				message = "No match";
				break;
			case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
				message = "RecognitionService busy";
				break;
			case SpeechRecognizer.ERROR_SERVER:
				message = "error from server";
				break;
			case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
				message = "No speech input";
				break;
			default:
				message = "Not recognised";
				break;
		}
		Log.d(TAG,"onError code:" + error + " message: " + message);

		if (restart) {
			getActivity().runOnUiThread(new Runnable() {
			    public void run() {
			    	getSpeechRevognizer().cancel();
					startVoiceRecognitionCycle();
			    }
			});
		}
	}

	@Override
	public void onEvent(int eventType, Bundle params) {
		Log.d(TAG,"onEvent");
	}

	@Override
	public void onPartialResults(Bundle partialResults) {
		Log.d(TAG,"onPartialResults");
	}

	@Override
	public void onResults(Bundle results) {
		// Restart new dictation cycle
		startVoiceRecognitionCycle();
		// 
		StringBuilder scores = new StringBuilder();
		for (int i = 0; i < results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES).length; i++) {
			scores.append(results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)[i] + " ");
		}
		Log.d(TAG,"onResults: " + results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) + " scores: " + scores.toString());
		// Return to the container activity dictation results 
		if (results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) != null) {
			mCallback.onResults(this, results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION));
		}
	}

	@Override
	public void onRmsChanged(float rmsdB) {
//		Log.d(TAG,"onRmsChanged");
	}

}
