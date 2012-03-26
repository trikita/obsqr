package trikita.obsqr;

import android.app.Activity;
import android.app.Dialog;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Build;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface;

import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import android.view.MotionEvent;
import android.view.KeyEvent;

import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;

import android.hardware.Camera;
import android.graphics.ImageFormat;
import java.io.IOException;
import android.net.Uri;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.text.DateFormat;

import android.util.Log;

public class ObsqrActivity extends Activity 
	implements Camera.PreviewCallback, Camera.AutoFocusCallback {

	private final static String tag = "ObsqrActivity";
	/* Don't perceive click events on mTextContainer till 3 sec run out */
	private final static int DURATION_OF_KEEPING_TEXT_ON = 3000; 
	/* It'll be 2 sec between two autoFocus() calls */
	private final static int AUTOFOCUS_FREQUENCY = 2000;
	/* Shared preferences title */
	private final static String PREFS_NAME = "ObsqrSharedPreferences";

	private QrParser mParser;
	private QrParser.QrContent mQrContent;

	private Preview mPreview;

	private Zbar zbar = new Zbar();

	private Camera mCamera;
	private Camera.Parameters mParams = null;	
	private boolean mFocusModeOn;

	private LinearLayout mTextContainer;
	private TextView mQrTitleView;
	private TextView mQrContentView;
	private TextView mHelpView;

	private Handler mKeepTextOnScreenHandler = new Handler();
	private Runnable mTextVisibleRunnable = new Runnable() {
		@Override
		public void run() {
			mTextContainer.setVisibility(View.INVISIBLE);
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

		mPreview = (Preview) findViewById(R.id.surface);
		// Decoded qr content will be shown in textview
		mTextContainer = (LinearLayout) findViewById(R.id.l_text_container);
		mQrTitleView = (TextView) findViewById(R.id.tv_title);
		mQrContentView = (TextView) findViewById(R.id.tv_qrcontent);
		mHelpView = (TextView) findViewById(R.id.tv_help);

		// By tapping on textview user can deal with decoded qr content properly
		mTextContainer.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_UP) { 
					// Launch appropriate service for decoded content
					// messenger - for qr code containing sms,
					// browser - for qr code containing url,
					// maps - for qr code containing geolocation, etc...
					if (mQrContent != null) mQrContent.launch();	
					// and hide textview 
					mTextContainer.setVisibility(View.INVISIBLE);
				}
				return true;
			}
		});
		
		// By clicking on dpad button user can deal with decoded qr content properly as well
		mTextContainer.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
					if (mQrContent != null) mQrContent.launch();	
					mTextContainer.setVisibility(View.INVISIBLE);
				}
				return true;
			}
		}); 
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(tag, "onResume()");
		mPreview.requestFocus();

		mCamera = openCamera();
		if (mCamera == null) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Are you sure you want to exit?")
				.setCancelable(false)
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						ObsqrActivity.this.finish();
						dialog.dismiss();
					}
				});
			AlertDialog alert = builder.create();	
			alert.show();
			return;
		}

		mCamera.setPreviewCallback(this);
		mPreview.setCamera(mCamera);
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d(tag, "onPause()");
		// Kill all the other threads that were created for periodic operations
		if (mCamera != null) {
			mPreview.setCamera(null);
			mCamera.setPreviewCallback(null);
			mCamera.release();
			mCamera = null;
		}

		mKeepTextOnScreenHandler.removeCallbacks(mTextVisibleRunnable);	
		mAutoFocusHandler.removeCallbacks(mAutoFocusRunnable);
		mTextContainer.setVisibility(View.INVISIBLE);
	}

	@Override
	public void onBackPressed() {
		// Restore saved settings from shared preferences
		SharedPreferences prefs = getSharedPreferences(PREFS_NAME, 0);
		// if the boolean value exists there - it means don't show the dialog anymore
		// and provide a common behavior after clicking on back button
		boolean wasFirstVisit = prefs.getBoolean("first_visit_passed", false);
		if (wasFirstVisit) {
			super.onBackPressed();
			return;
		}

		turnRateRequestOff();
		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.dlg_ask_to_rate);
		dialog.setCancelable(false);

		Button rateBtn = (Button) dialog.findViewById(R.id.dlg_btn_rate);
		Button ignoreBtn = (Button) dialog.findViewById(R.id.dlg_btn_ignore);

		rateBtn.setOnClickListener(new View.OnClickListener() {
			@Override 
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW, 
						Uri.parse("market://details?id=trikita.obsqr"));
				ObsqrActivity.this.startActivity(intent);
				dialog.dismiss();
				ObsqrActivity.super.onBackPressed();
			}
		});

		ignoreBtn.setOnClickListener(new View.OnClickListener() {
			@Override 
			public void onClick(View v) {
				dialog.dismiss();
				ObsqrActivity.super.onBackPressed();
			}
		});
		
		dialog.show();
	}

	private void turnRateRequestOff() {
		// We need an Editor object to make preference changes.
		// All objects are from android.context.Context
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();

		// Save that user has already seen the request to give feedback so don't ask him to rate 
		// this app anymore
		editor.putBoolean("first_visit_passed", true);

		// Commit the edits
		editor.commit();
	}

	private Camera openCamera() {
		Camera camera = Camera.open();

		if (camera != null) {
			Log.d(tag, "Back facing camera open by default");
		} else {
			final int sdkVersion = Integer.parseInt(Build.VERSION.SDK);
			if (sdkVersion >= Build.VERSION_CODES.GINGERBREAD) {
				int cameraCount = Camera.getNumberOfCameras();
				for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
					if (camera != null) break;
					try {
						camera = Camera.open(camIdx);
						Camera.CameraInfo info = new Camera.CameraInfo();
						Camera.getCameraInfo(camIdx, info);
						if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
							Log.d(tag, "Back camera open");
						} else if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
							Log.d(tag, "Front camera open");
						} else {
							Log.d(tag, "Unknown camera facing");
						}
					} catch (RuntimeException e) {
						Log.d(tag, "Camera failed to open: " + e.toString());
					}
				}
			}
			else return null; 
		}

		return camera; 
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
			mTextContainer.setVisibility(View.VISIBLE);
			mQrContentView.setText(mQrContent.toString());
			mQrTitleView.setText(mQrContent.getTitle());
			mHelpView.setText(mQrContent.getActionName());
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
