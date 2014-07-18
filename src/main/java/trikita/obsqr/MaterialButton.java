package trikita.obsqr;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Region;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.animation.AccelerateInterpolator;
import android.widget.Button;
import android.widget.Toast;
import android.widget.TextView;
import android.animation.Animator;
import android.os.Handler;
import android.annotation.TargetApi;

@TargetApi(11)
public class MaterialButton extends TextView implements Animator.AnimatorListener {

	private float mDownX;
	private float mDownY;

	private float mRadius;
	private float mAlpha;

	private Paint mPaint;

	private ObjectAnimator mRadiusAnimator;
	private ObjectAnimator mAlphaAnimator;

	public MaterialButton(Context context) {
		this(context, null);
	}

	public MaterialButton(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public MaterialButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mPaint = new Paint();
		mPaint.setColor(0x18000000);
		setClickable(true);
	}

	@Override
	public boolean onTouchEvent(final MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mDownX = event.getX();
				mDownY = event.getY();
				mRadiusAnimator = ObjectAnimator.ofFloat(this, "radius", 0, getWidth()*1.5f);
				mRadiusAnimator.setInterpolator(new AccelerateInterpolator());
				mRadiusAnimator.setDuration(200);
				mRadiusAnimator.start();
				mAlpha = 0x18;
				if (mAlphaAnimator != null) {
					mAlphaAnimator.cancel();
					mAlphaAnimator = null;
				}
				mRadiusAnimator.removeListener(this);
				break;
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP:
				if (mRadiusAnimator.isRunning()) {
					mRadiusAnimator.addListener(this);
				} else {
					this.onAnimationEnd(null);
				}
				break;
		} 
		return super.onTouchEvent(event);
	}

	public void onAnimationCancel(Animator animation) {}
	public void onAnimationRepeat(Animator animation) {}
	public void onAnimationStart(Animator animation) {}
	public void onAnimationEnd(Animator animation) {
		mAlphaAnimator = ObjectAnimator.ofFloat(this, "alpha", 0x18, 0).setDuration(200);
		mAlphaAnimator.setInterpolator(new AccelerateInterpolator());
		mAlphaAnimator.start();
	}

	public void setAlpha(float alpha) {
		mAlpha = alpha;
		invalidate();
	}

	public void setRadius(float radius) {
		mRadius = radius;
		invalidate();
	}

	@Override
	protected void onDraw(final Canvas canvas) {
		super.onDraw(canvas);
		mPaint.setAlpha((int) mAlpha);
		canvas.drawCircle(mDownX, mDownY, mRadius, mPaint);
	}

	private Handler mHandler = new Handler();

	@Override
	public boolean performClick() {
		mHandler.postDelayed(new Runnable() {
			public void run() {
				MaterialButton.super.performClick();
			}
		}, 200);
		return true;
	}
}

