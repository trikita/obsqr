package trikita.obsqr;

import android.view.View;
import android.view.ViewGroup;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import android.hardware.Camera;
import android.hardware.Camera.Size;

import android.content.Context;
import android.util.AttributeSet;

import android.os.Handler;
import android.os.Build;

import java.util.List;
import java.io.IOException;

import android.util.Log;
import android.view.Surface;

/**
 * A simple wrapper around a Camera and a SurfaceView that renders a centered preview of the Camera
 * to the surface. Also provided autofocus effect.
 * 
 */

class CameraPreview extends ViewGroup implements SurfaceHolder.Callback,
	Camera.AutoFocusCallback, Camera.PreviewCallback {

	private final static String tag = "CameraPreview";
	/* It'll be 2 sec between two autoFocus() calls */
	private final static int AUTOFOCUS_FREQUENCY = 2000;

	private SurfaceView mSurfaceView;
	private SurfaceHolder mHolder;

	private Size mPreviewSize;
	private List<Size> mSupportedPreviewSizes;
	private Camera mCamera;
	private Camera.Parameters mParams = null;	
	private int mCameraId;
	private boolean mRotated = false;

	private Zbar zbar = new Zbar();

	private boolean mFocusModeOn;
	private OnQrDecodedListener mOnQrDecodedListener;

	public interface OnQrDecodedListener {
		public void onQrDecoded(String url);
	}

	private Handler mAutoFocusHandler = new Handler();
	private Runnable mAutoFocusRunnable = new Runnable() {
		@Override
		public void run() {
			mFocusModeOn = true;
			if (mCamera != null) {
				mCamera.autoFocus(CameraPreview.this);
			}
		}
	};

	public CameraPreview(Context context) {
		this(context, null, 0);
	}

	public CameraPreview(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public CameraPreview(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		Log.d(tag, "Create CameraPreview");
		mContext = context;

		mSurfaceView = new SurfaceView(context);
		addView(mSurfaceView);

		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = mSurfaceView.getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	public void setOnQrDecodedListener(OnQrDecodedListener l) {
		mOnQrDecodedListener = l; 
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// We purposely disregard child measurements because act as a
		// wrapper to a SurfaceView that centers the camera preview instead
		// of stretching it.
		Log.d(tag, "onMeasure");
		final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
		final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
		setMeasuredDimension(width, height);
		Log.d(tag, "setMeasuredDimension w=" + width + " h=" + height);

		if (mSupportedPreviewSizes != null) {
			mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
			Log.d(tag, "mPreviewSize w=" + mPreviewSize.width + " h=" + mPreviewSize.height);
		}
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		Log.d(tag, "onLayout");
		if (changed && getChildCount() > 0) {
			final View child = getChildAt(0);

			final int width = r - l;
			final int height = b - t;

			int previewWidth = width;
			int previewHeight = height;

			if (mPreviewSize != null) {
				if (mRotated) {
					previewWidth = mPreviewSize.width;
					previewHeight = mPreviewSize.height;
				} else {
					previewWidth = mPreviewSize.height;
					previewHeight = mPreviewSize.width;
				}
			}

			Log.d(tag, "Preview width="+previewWidth+" height="+previewHeight);
			Log.d(tag, "View width="+width+", height="+height);

			// Center the child SurfaceView within the parent.
			if (previewWidth * height > width * previewHeight) {
				Log.d(tag, "Scaling by height");
				final int scaledChildWidth = previewWidth * height / previewHeight;
				int dx = (int)((width - scaledChildWidth) * 0.5);
				Log.d(tag, "Scaled width: " + scaledChildWidth + ", dx="+dx);
				child.layout(dx, 0, width - dx, height);
				Log.d(tag, "l="+dx+" t=0"+" r="+(width-dx)+" b="+height);
			} else {
				Log.d(tag, "Scaling by width");
				final int scaledChildHeight = previewHeight * width / previewWidth;
				int dy = (int)((height - scaledChildHeight) * 0.5);
				Log.d(tag, "Scaled height: " + scaledChildHeight+ ", dy="+dy);
				child.layout(0, 0, width, height - 2*dy);
				Log.d(tag, "l="+0+" t="+dy+" r="+width+" b="+(height-dy));
			}
		} else {
			Log.d(tag, "Nothing to do in onLayout()");
		}
	}

	private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
		final double ASPECT_TOLERANCE = 0.1;
		double targetRatio = (double) w / h;
		if (sizes == null) return null;

		Size optimalSize = null;
		double minDiff = Double.MAX_VALUE;

		Log.d(tag, "getOptimalPreviewSize for w=" + w + " h=" + h);

		// Try to find an size match aspect ratio and size
		for (Size size : sizes) {
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
			double diff = Math.pow(Math.abs(size.height - h), 2) +
				Math.pow(Math.abs(size.width - w), 2);
			if (diff < minDiff) {
				optimalSize = size;
				minDiff = diff;
			}
		}

		// Cannot find the one match the aspect ratio, ignore the requirement
		if (optimalSize == null) {
			minDiff = Double.MAX_VALUE;
			for (Size size : sizes) {
				double diff = Math.pow(Math.abs(size.height - h), 2) +
					Math.pow(Math.abs(size.width - w), 2);
				if (diff < minDiff) {
					optimalSize = size;
					minDiff = diff;
				}
			}
		}
		Log.d(tag, "Optimal size is " + optimalSize.width + "x" + optimalSize.height);
		return optimalSize;
	}
	
	private void setCameraDisplayOrientation(int rotation) {
		Log.d(tag, "setCameraDisplayOrientation");
		if (mCamera == null) return;
		Camera.CameraInfo info = new Camera.CameraInfo();
		Log.d(tag, "mCameraId="+mCameraId);
		Camera.getCameraInfo(mCameraId, info);

		int degrees = 0;
		switch (rotation) {
			case Surface.ROTATION_0: degrees = 0; mRotated = false; break;
			case Surface.ROTATION_90: degrees = 90; mRotated = true; break;
			case Surface.ROTATION_180: degrees = 180; mRotated = false; break;
			case Surface.ROTATION_270: degrees = 270; mRotated = true; break;
		}

		Log.d(tag, "Camera rotated: " + degrees + " degrees");

		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			Log.d(tag, "Front facing camera");
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360;  // compensate the mirror
		} else {  // back-facing
			Log.d(tag, "Back facing camera");
			result = (info.orientation - degrees + 360) % 360;
		}
		mCamera.stopPreview();
		mCamera.setDisplayOrientation(result);
	}

	public boolean acquireCamera(int rotation) {
		Log.d(tag, "acquireCamera");
		if (mCamera != null) {
			setCameraDisplayOrientation(rotation);
			mCamera.startPreview();
			mCamera.setPreviewCallback(this);
			mAutoFocusHandler.postDelayed(mAutoFocusRunnable, AUTOFOCUS_FREQUENCY);
		} else {
			mCamera = openCamera();
			if (mCamera == null) {
				return false;
			}

			mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
			for (Size s : mSupportedPreviewSizes) {
				Log.d(tag, "Preview size: " + s.width + "x" + s.height);
			}
			requestFocus();
			setCameraDisplayOrientation(rotation);
		}

		return true;
	}

	private Camera openCamera() {
		Camera camera = null;
		try {
			camera = Camera.open();
		} catch (RuntimeException e) {
			Log.d(tag, "RuntimeException was thrown while opening the camera");
			return null;
		}

		if (camera != null) {
			Log.d(tag, "Back facing camera open by default");
			mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
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
						mCameraId = camIdx;
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

	public void releaseCamera() {
		mAutoFocusHandler.removeCallbacks(mAutoFocusRunnable);
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.cancelAutoFocus();
		}
	}

	// ----------------------- SurfaceHolder.Callbacks ------------------- //
	public void surfaceCreated(SurfaceHolder holder) {
		// The Surface has been created, acquire the camera and tell it where
		// to draw.
		Log.d(tag, "surfaceCreated");
		try {
			if (mCamera != null) {
				mCamera.setPreviewDisplay(mHolder);
				mCamera.setPreviewCallback(this);
			}
		} catch (IOException exception) {
			if (mCamera != null) {
				mCamera.release();
				mCamera = null;
			}
			Log.e(tag, "IOException caused by setPreviewDisplay()", exception);
		}
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		// Now that the size is known, set up the camera parameters and begin
		// the preview.
		Log.d(tag, "surfaceChanged: w="+w+",h="+h);
		if (mCamera != null) {
			mParams = mCamera.getParameters();
			
			mParams.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
			requestLayout();

			mCamera.setParameters(mParams);
			mCamera.startPreview();
			// Launch autofocus mode 
			mAutoFocusHandler.postDelayed(mAutoFocusRunnable, AUTOFOCUS_FREQUENCY);
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		// Surface will be destroyed when we return, so stop the preview.
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
		
		mCamera.setPreviewCallback(null);
		int width = mParams.getPreviewSize().width;  
		int height = mParams.getPreviewSize().height;
		// Get decoded string
		Log.d(tag, "onPreviewFrame w=" + width + " h=" + height);
		String s = zbar.process(width, height, data);
		Log.d(tag, "Decoded string: " +s);
		if (s != null) {
			Log.d(tag, "============= URL: " + s + " =================");
			mOnQrDecodedListener.onQrDecoded(s);
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
