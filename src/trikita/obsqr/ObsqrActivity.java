package trikita.obsqr;

import android.app.Activity;
import android.os.Bundle;

import android.view.SurfaceView;
import android.view.SurfaceHolder;

import android.widget.TextView;

import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import android.hardware.Camera;
import android.graphics.ImageFormat;
import java.io.IOException;

import android.util.Log;
import android.content.Intent;
import android.os.Handler;
import android.net.Uri;
import android.view.MotionEvent;
import android.view.KeyEvent;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.text.DateFormat;

public class ObsqrActivity extends Activity 
	implements SurfaceHolder.Callback, Camera.PreviewCallback, Camera.AutoFocusCallback {

	private final static String tag = "ObsqrActivity";
	/* Don't perceive click events on mTextView till 3 sec run out */
	private final static int DURATION_OF_KEEPING_TEXT_ON = 3000; 
	/* It'll be 2 sec between two autoFocus() calls */
	private final static int AUTOFOCUS_FREQUENCY = 2000;

	private QrParser mParser;
	private QrParser.QrContent mQrContent;

	private SurfaceView mPreview;
	private SurfaceHolder mHolder;

	private Zbar zbar = new Zbar();

	private Camera mCamera;
	private Camera.Parameters mParams = null;	
	private boolean mFocusModeOn;

	private TextView mTextView;

	private Handler mKeepTextOnScreenHandler = new Handler();
	private Runnable mTextVisibleRunnable = new Runnable() {
		@Override
		public void run() {
			mTextView.setVisibility(View.INVISIBLE);
		}
	};

	private Handler mAutoFocusHandler = new Handler();
	private Runnable mAutoFocusRunnable = new Runnable() {
		@Override
		public void run() {
			mFocusModeOn = true;
			if (mCamera != null) {
				mCamera.autoFocus(ObsqrActivity.this);
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.act_camera);

		mParser = QrParser.getInstance();
		mParser.setContext(this);

		mPreview = (SurfaceView) findViewById(R.id.surface);
		// Decoded qr content will be shown in textview
		mTextView = (TextView) findViewById(R.id.text);

		// By tapping on textview user can deal with decoded qr content properly
		mTextView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_UP) { 
					// Launch appropriate service for decoded content
					// messenger - for qr code containing sms,
					// browser - for qr code containing url,
					// maps - for qr code containing geolocation, etc...
					if (mQrContent != null) mQrContent.launch();	
					// and hide textview 
					mTextView.setVisibility(View.INVISIBLE);
				}
				return true;
			}
		});
		
		// By clicking on dpad button user can deal with decoded qr content properly as well
		mTextView.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
					if (mQrContent != null) mQrContent.launch();	
					mTextView.setVisibility(View.INVISIBLE);
				}
				return true;
			}
		}); 


		mHolder = mPreview.getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(tag, "onResume()");
		mPreview.requestFocus();
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d(tag, "onPause()");
		// Kill all the other threads that were created for periodic operations
		mKeepTextOnScreenHandler.removeCallbacks(mTextVisibleRunnable);	
		mAutoFocusHandler.removeCallbacks(mAutoFocusRunnable);
		mTextView.setVisibility(View.INVISIBLE);
	}

	/* ---------------------- SurfaceHolder.Callback --------------------- */
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(tag, "surfaceCreated");

		mCamera = Camera.open();
		try {
			mCamera.setPreviewCallback(this);
			mCamera.setPreviewDisplay(mHolder);
			mCamera.getParameters().setPreviewFormat(ImageFormat.NV21);
		} catch (IOException io) {
			Log.d(tag, io.toString());
			mCamera.release();
			mCamera = null;
		}
		mCamera.startPreview();
		// Launch autofocus mode 
		mAutoFocusHandler.postDelayed(mAutoFocusRunnable, AUTOFOCUS_FREQUENCY);
	}

	@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			Log.d(tag, "surfaceChanged: w="+width+",h="+height);
		}

	@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			Log.d(tag, "surfaceDestroyed");
			if (mCamera != null) {
				mCamera.stopPreview();
				mCamera.setPreviewCallback(null);
				mCamera.release();
				mCamera = null;
			}
		}

	/* ---------------------- PreviewCallback --------------------- */
	public void onPreviewFrame(byte[] data, Camera camera) {
		if (mFocusModeOn) return;
		if (mParams == null) {
			mParams = mCamera.getParameters();
		}
		mCamera.setPreviewCallback(null);
		int width = mParams.getPreviewSize().width;  
		int height = mParams.getPreviewSize().height;
		// Get decoded string
		String s = zbar.process(width, height, data);
		if (s != null) {
			Log.d(tag, "============= URL: " + s + " =================");
			mKeepTextOnScreenHandler.removeCallbacks(mTextVisibleRunnable);
			mQrContent = mParser.parse(s);
			// Show textview with qr content
			mTextView.setVisibility(View.VISIBLE);
			mTextView.setText(mQrContent.toString());
			// Keep textview on screen 3 sec and hide it
			mKeepTextOnScreenHandler.postDelayed(mTextVisibleRunnable, 
					DURATION_OF_KEEPING_TEXT_ON);
		}
		mCamera.setPreviewCallback(this);
	}

	/* ---------------------- AutoFocusCallback --------------------- */
	@Override
	public void onAutoFocus(boolean success, Camera camera) {
		Log.d(tag, "onAutoFocus()");
		mAutoFocusHandler.postDelayed(mAutoFocusRunnable, AUTOFOCUS_FREQUENCY);
		mFocusModeOn = false;
	}
}
