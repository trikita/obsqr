package trikita.obsqr;

import android.app.Activity;
import android.app.Dialog;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface;

import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import android.view.KeyEvent;
import android.view.MotionEvent;

import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import android.net.Uri;

import android.util.Log;

public class ObsqrActivity extends Activity implements
	CameraPreview.OnQrDecodedListener,
	View.OnTouchListener, View.OnKeyListener {

	private final static String tag = "ObsqrActivity";

	private final static String APPLICATION_LINK_ON_PLAY_MARKET =
		"market://details?id=trikita.obsqr";
	
	/* Display decoded QR content on screen for 3 sec */
	private final static int DURATION_OF_KEEPING_TEXT_ON = 3000; 

	/* Shared preferences file name */
	private final static String PREFS_NAME = "ObsqrSharedPreferences";

	private QrParser mParser;
	private QrParser.QrContent mQrContent;

	private CameraPreview mCameraPreview;

	private View mDecodedQrView;
	private TextView mQrTitleView;
	private TextView mQrContentView;
	private TextView mHelpView;

	private final Handler mKeepTextOnScreenHandler = new Handler();
	private final Runnable mTextVisibleRunnable = new Runnable() {
		@Override
		public void run() {
			mDecodedQrView.setVisibility(View.INVISIBLE);
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.act_camera);

		// init QR parser
		mParser = QrParser.getInstance();
		mParser.setContext(this);

		// init device camera
		mCameraPreview = (CameraPreview) findViewById(R.id.surface);
		mCameraPreview.setOnQrDecodedListener(this);

		// setup views
		// decoded qr content will be shown in textview
		mDecodedQrView = (View) findViewById(R.id.l_text_container);
		mQrTitleView = (TextView) findViewById(R.id.tv_title);
		mQrContentView = (TextView) findViewById(R.id.tv_qrcontent);
		mHelpView = (TextView) findViewById(R.id.tv_help);

		// By tapping on textview user can deal with decoded qr content properly
		mDecodedQrView.setOnTouchListener(this);
		
		// By clicking on dpad button user can deal with decoded qr content properly as well
		mDecodedQrView.setOnKeyListener(this);
	}

	@Override
	public void onQrDecoded(String s) {
		mKeepTextOnScreenHandler.removeCallbacks(mTextVisibleRunnable);
		mQrContent = mParser.parse(s);

		// show QR content in textview
		mDecodedQrView.setVisibility(View.VISIBLE);
		mQrContentView.setText(mQrContent.toString());
		mQrTitleView.setText(mQrContent.getTitle());
		mHelpView.setText(mQrContent.getActionName());

		// display decoded QR content on screen for 3 sec and hide it
		mKeepTextOnScreenHandler.postDelayed(mTextVisibleRunnable, 
			DURATION_OF_KEEPING_TEXT_ON);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_UP) { 
			openQrContent();
		}
		return true;
	}

	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
			openQrContent();
		}
		return true;
	}

	/**
	 * Launches appropriate service for decoded content
	 * messenger - for qr code containing sms,
	 * browser - for qr code containing url,
	 * maps - for qr code containing geolocation, etc...
	**/
	private void openQrContent() {
		try {
			if (mQrContent != null) mQrContent.launch();	
			// and hide textview 
			mDecodedQrView.setVisibility(View.INVISIBLE);
		} catch (android.content.ActivityNotFoundException e) {
			Toast.makeText(ObsqrActivity.this, ObsqrActivity.this
				.getString(R.string.alert_msg_activity_not_found),
				Toast.LENGTH_LONG).show();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(tag, "onResume()");
		
		boolean success = mCameraPreview.acquireCamera(getWindowManager()
			.getDefaultDisplay().getRotation());
		if (!success) {
			openAlertDialog();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d(tag, "onPause()");

		mKeepTextOnScreenHandler.removeCallbacks(mTextVisibleRunnable);	
		mDecodedQrView.setVisibility(View.INVISIBLE);
		mCameraPreview.releaseCamera();
	}

	@Override
	public void onBackPressed() {
		// Ask user to rate Obsqr on Play Market when they leave the app first time
		SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
		boolean wasFirstVisit = prefs.getBoolean("first_visit_passed", false);
		// if there was a first visit, don't bother user with rate dialog again
		// just leave the application
		if (wasFirstVisit) {
			super.onBackPressed();
			return;
		}

		// ..otherwise, open rate dialog
		turnRateRequestOff();
		openRateDialog();
	}

	/** 
	 * Sets up alert dialog views and presents the dialog to user
	 * when camera could not be opened
	 */
	private void openAlertDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getString(R.string.dlg_alert_msg))
			.setCancelable(false)
			.setPositiveButton(getString(R.string.dlg_alert_ok_btn_caption),
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					ObsqrActivity.this.finish();
					dialog.dismiss();
				}
			});
		AlertDialog alert = builder.create();	
		alert.show();
	}

	/** 
	 * Sets up rate dialog views and presents the dialog to user
	 * when they leave the application first time
	 */
	private void openRateDialog() {
		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.dlg_ask_to_rate);
		dialog.setCancelable(false);

		// setup dialog child views
		Button rateBtn = (Button) dialog.findViewById(R.id.dlg_btn_rate);
		Button ignoreBtn = (Button) dialog.findViewById(R.id.dlg_btn_ignore);

		rateBtn.setOnClickListener(new View.OnClickListener() {
			@Override 
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW, 
						Uri.parse(APPLICATION_LINK_ON_PLAY_MARKET));
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

	/**
	 * Remembers first visit by setting a flag in shared preferences
	 */
	private void turnRateRequestOff() {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();

		editor.putBoolean("first_visit_passed", true);
		editor.apply();
	}
}
