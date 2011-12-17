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

public class QrdentActivity extends Activity 
	implements SurfaceHolder.Callback, Camera.PreviewCallback {

	private final static String tag = "ZbarActivity";
	/* Don't perceive click events on mTextView till 3 sec run out */
	private final static int DURATION_OF_KEEPING_TEXT_ON = 3000; 

	private QrParser mParser;
	private QrParser.QrContent mQrContent;

	private SurfaceView mPreview;
	private SurfaceHolder mHolder;

	private Zbar zbar = new Zbar();

	private Camera mCamera;
	private Camera.Parameters mParams = null;	

	private TextView mTextView;

	private Handler mKeepTextOnScreenHandler = new Handler();
	private Runnable mTextVisibleRunnable = new Runnable() {
		@Override
		public void run() {
			mTextView.setVisibility(View.INVISIBLE);
			mTextView.setText("???");
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
		mTextView = (TextView) findViewById(R.id.text);

		mTextView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_UP) { 
					if (mQrContent != null) mQrContent.launch();	
					mTextView.setVisibility(View.INVISIBLE);
				}
				return true;
			}
		});
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


		// Make the activity a surface holder callback
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
		mKeepTextOnScreenHandler.removeCallbacks(mTextVisibleRunnable);	
		mTextView.setVisibility(View.INVISIBLE);
	}

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
	}

	@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			// Ought to be override
			Log.d(tag, "surfaceChanged: w="+width+",h="+height);
		}

	@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			// Ought to be override
			Log.d(tag, "surfaceDestroyed");
			if (mCamera != null) {
				mCamera.stopPreview();
				mCamera.setPreviewCallback(null);
				mCamera.release();
				mCamera = null;
			}
		}

	public void onPreviewFrame(byte[] data, Camera camera) {
		if (mParams == null) {
			mParams = mCamera.getParameters();
		}
		mCamera.setPreviewCallback(null);
		int width = mParams.getPreviewSize().width;  
		int height = mParams.getPreviewSize().height;
		String s = zbar.process(width, height, data);
		if (s != null) {
			Log.d(tag, "============= URL: " + s + " =================");
			mKeepTextOnScreenHandler.removeCallbacks(mTextVisibleRunnable);
			mQrContent = mParser.parse(s);
			mTextView.setVisibility(View.VISIBLE);
			mTextView.setText(mQrContent.toString());
			mKeepTextOnScreenHandler.postDelayed(mTextVisibleRunnable, 
					DURATION_OF_KEEPING_TEXT_ON);
		}
		mCamera.setPreviewCallback(this);
	}

}
