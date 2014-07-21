package trikita.obsqr;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.*;
import android.content.DialogInterface;

public class ObsqrActivity extends Activity implements CameraPreview.OnQrDecodedListener {

	private final static String tag = "ObsqrActivity";

	/* Display decoded QR content on screen for 3 sec */
	private final static int DURATION_OF_KEEPING_TEXT_ON = 3000; 

	public final static int MAX_HORIZONTAL_BUTTON_TEXT_LENGTH = 12;

	@InjectView(R.id.surface) CameraPreview mCameraPreview;
	@InjectView(R.id.container) ViewGroup mContainer;
	@InjectView(R.id.tv_title) TextView mQrTitleView;
	@InjectView(R.id.dialog_content) ViewGroup mDialogContent;

	@InjectView(R.id.vertical_buttons_container) ViewGroup mVerticalButtonsContainer;
	@InjectView(R.id.tv_vertical_action) TextView mVerticalActionView;
	@InjectView(R.id.tv_vertical_cancel) TextView mVerticalCancelView;

	@InjectView(R.id.horizontal_buttons_container) ViewGroup mHorizontalButtonsContainer;
	@InjectView(R.id.tv_horizontal_action) TextView mHorizontalActionView;
	@InjectView(R.id.tv_horizontal_cancel) TextView mHorizontalCancelView;

	private QrContent mQrContent = null;

	private String mLastKnownContent = "";

	private final Handler mKeepTextOnScreenHandler = new Handler();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.act_camera);
		ButterKnife.inject(this);

		mHorizontalCancelView.setText(mHorizontalCancelView.getText().toString().toUpperCase());
		mVerticalCancelView.setText(mVerticalCancelView.getText().toString().toUpperCase());
		mCameraPreview.setOnQrDecodedListener(this);
	}

	@OnClick({R.id.tv_vertical_action, R.id.tv_horizontal_action})
	public void onActionClick(View v) {
		if (mQrContent != null) {
			try {
				mQrContent.action();
			} catch (ActivityNotFoundException e) {
				Toast.makeText(this, getString(R.string.alert_msg_activity_not_found),
						Toast.LENGTH_LONG).show();
			}
		}
	}

	@OnClick({R.id.container, R.id.tv_vertical_cancel, R.id.tv_horizontal_cancel})
	public void onDialogCancelClick(View v) {
		cancel();
	}

	@Override
	public void onQrDecoded(String s) {
		if (mLastKnownContent.equals(s) && mQrContent == null) { // Same content was cancelled
			return;
		}
		mLastKnownContent = s;
		mKeepTextOnScreenHandler.removeCallbacksAndMessages(null);

		mQrContent = QrContent.from(this, s);

		// show dialog with QR content
		mContainer.setVisibility(View.VISIBLE);

		mQrTitleView.setText(mQrContent.getTitle());

		String action = mQrContent.getAction();

		int maxHorizontalTextLength = MAX_HORIZONTAL_BUTTON_TEXT_LENGTH;
			
		if (action.length() < maxHorizontalTextLength) {
			mHorizontalButtonsContainer.setVisibility(View.VISIBLE);
			mVerticalButtonsContainer.setVisibility(View.GONE);
			mHorizontalActionView.setText(mQrContent.getAction().toUpperCase());
		} else {
			mVerticalButtonsContainer.setVisibility(View.VISIBLE);
			mHorizontalButtonsContainer.setVisibility(View.GONE);
			mVerticalActionView.setText(mQrContent.getAction().toUpperCase());
		}

		View v = mQrContent.render();
		mDialogContent.removeAllViews();
		mDialogContent.addView(v);

		// display decoded QR content on screen for 3 sec and hide it
		mKeepTextOnScreenHandler.postDelayed(new Runnable() {
			public void run() {
				cancel();
			}
		}, DURATION_OF_KEEPING_TEXT_ON);
	}

	@Override
	public void onQrNotFound() {
		mLastKnownContent = "";
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (mQrContent == null) {
			return super.onKeyDown(keyCode, event);
		}
		// Pressing DPAD, Volume keys or Camera key would call the QR action
		switch (keyCode) {
			case KeyEvent.KEYCODE_DPAD_CENTER:
				mQrContent.action();
				return true;
		}
		return super.onKeyDown(keyCode, event);
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

		cancel();
		mCameraPreview.releaseCamera();
	}

	@Override
	public void onBackPressed() {
		Log.d(tag, "onBackPressed()");
		if (mQrContent != null) {
			cancel();
		} else {
			super.onBackPressed();
		}
	}

	private void cancel() {
		mKeepTextOnScreenHandler.removeCallbacksAndMessages(null);
		mContainer.setVisibility(View.INVISIBLE);
		mQrContent = null;
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
}
