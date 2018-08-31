package trikita.obsqr;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;

public class ObsqrActivity extends Activity implements CameraPreview.OnQrDecodedListener {

	public final static int PERMISSIONS_REQUEST = 100;

	private CameraPreview mCameraPreview;
	private QrContentDialog mDialog;

	private QrContent mQrContent = null;

	private String mLastKnownContent = "";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

        mCameraPreview = findViewById(R.id.surface);
        mDialog = findViewById(R.id.container);

		mCameraPreview.setOnQrDecodedListener(this);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
				requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST);
			}
		}
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
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (mQrContent == null) {
			return super.dispatchKeyEvent(event);
		}
		// Pressing DPAD, Volume keys or Camera key would call the QR action
		switch (event.getKeyCode()) {
			case KeyEvent.KEYCODE_DPAD_CENTER:
			case KeyEvent.KEYCODE_CAMERA:
			case KeyEvent.KEYCODE_VOLUME_UP:
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				mQrContent.performAction();
				return true;
		}
		return super.dispatchKeyEvent(event);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
				return;
			}
		}
		boolean success = mCameraPreview.acquireCamera(getWindowManager()
			.getDefaultDisplay().getRotation());
		if (!success) {
			new AlertDialog.Builder(this)
					.setMessage(getString(R.string.dlg_alert_msg))
					.setCancelable(false)
                    .setPositiveButton(getString(R.string.dlg_alert_ok_btn_caption), (dialog, id) -> {
                        ObsqrActivity.this.finish();
                        dialog.dismiss();
                    })
					.create().show();
		}
	}

	@Override
	protected void onPause() {
		mDialog.close();
		mCameraPreview.releaseCamera();
		super.onPause();
	}

	@Override
	public void onBackPressed() {
		if (!mDialog.close()) {
			super.onBackPressed();
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		if (requestCode == PERMISSIONS_REQUEST &&
				grantResults.length == 1 &&
				grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			recreate();
			return;
		}
		finish();
	}
}
