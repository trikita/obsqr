package trikita.obsqr;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;

import hugo.weaving.DebugLog;

public class ObsqrActivity extends Activity implements CameraPreview.OnQrDecodedListener {

	private CameraPreview mCameraPreview;
	private QrContentDialog mDialog;

	private QrContent mQrContent = null;

	private String mLastKnownContent = "";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mCameraPreview = (CameraPreview) findViewById(R.id.surface);
		mDialog = (QrContentDialog) findViewById(R.id.container);

		mCameraPreview.setOnQrDecodedListener(this);
	}

	@Override
	public void onQrDecoded(String s) {
		if (mLastKnownContent.equals(s) && mQrContent == null) { // Same content was cancelled
			return;
		}
		mLastKnownContent = s;
		mQrContent = QrContent.from(this, s);
		mDialog.open(mQrContent);
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
				mQrContent.performAction();
				return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@DebugLog
	@Override
	protected void onResume() {
		super.onResume();
		boolean success = mCameraPreview.acquireCamera(getWindowManager()
			.getDefaultDisplay().getRotation());
		if (!success) {
			new AlertDialog.Builder(this)
					.setMessage(getString(R.string.dlg_alert_msg))
					.setCancelable(false)
					.setPositiveButton(getString(R.string.dlg_alert_ok_btn_caption),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									ObsqrActivity.this.finish();
									dialog.dismiss();
								}
							})
					.create().show();
		}
	}

	@DebugLog
	@Override
	protected void onPause() {
		mDialog.close();
		mCameraPreview.releaseCamera();
		super.onPause();
	}

	@DebugLog
	@Override
	public void onBackPressed() {
		if (!mDialog.close()) {
			super.onBackPressed();
		}
	}
}
