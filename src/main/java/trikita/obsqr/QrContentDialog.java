package trikita.obsqr;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class QrContentDialog extends FrameLayout {

	/* Display decoded QR content on screen for 3 sec */
	private final static int DURATION_OF_KEEPING_TEXT_ON = 3000;

	public final static int MAX_HORIZONTAL_BUTTON_TEXT_LENGTH = 12;

    private Runnable mCloseDialogRunnable = this::close;
	private QrContent mContent;

	private TextView mTitleText;
	private TextView mContentText;
	private TextView mCancelButton;
	private TextView mActionButton;

	public QrContentDialog(Context c) {
		this(c, null);
	}

	public QrContentDialog(Context c, AttributeSet attrs) {
		super(c, attrs, 0);
	}

	public QrContentDialog(Context c, AttributeSet attrs, int defStyle) {
		super(c, attrs, defStyle);
	}

	protected void onFinishInflate() {
		super.onFinishInflate();
        mTitleText = findViewById(R.id.tv_title);
        mContentText = findViewById(R.id.tv_content);
        mActionButton = findViewById(R.id.btn_action);
        mActionButton.setOnClickListener(v -> QrContentDialog.this.performAction());
        mCancelButton = findViewById(R.id.btn_cancel);
        mCancelButton.setOnClickListener(v -> QrContentDialog.this.close());
	}

	public void open(QrContent content) {
		mContent = content;
		mTitleText.setText(mContent.title);
		mContentText.setText(mContent.content);
		mActionButton.setText(mContent.action);
		removeCallbacks(mCloseDialogRunnable);
		postDelayed(mCloseDialogRunnable, DURATION_OF_KEEPING_TEXT_ON);
		setVisibility(View.VISIBLE);

		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mCancelButton.getLayoutParams();
		if (mContent.action.length() < MAX_HORIZONTAL_BUTTON_TEXT_LENGTH) {
			params.addRule(RelativeLayout.BELOW, 0);
			params.addRule(RelativeLayout.LEFT_OF, R.id.btn_action);
			params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
		} else {
			params.addRule(RelativeLayout.BELOW, R.id.btn_action);
			params.addRule(RelativeLayout.LEFT_OF, 0);
			params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		}
	}

	public boolean close() {
		if (mContent == null) {
			return false;
		}
		removeCallbacks(mCloseDialogRunnable);
		mContent = null;
		setVisibility(View.INVISIBLE);
		return true;
	}

	public void performAction() {
		if (mContent != null) {
			mContent.performAction();
		}
	}
}
