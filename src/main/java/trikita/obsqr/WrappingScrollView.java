package trikita.obsqr;

import android.content.Context;
import android.widget.ScrollView;
import android.util.AttributeSet;
import android.view.View;

public class WrappingScrollView extends ScrollView {

	public final static int MAX_HEIGHT = 100;

	public WrappingScrollView(Context c) {
		this(c, null);
	}

	public WrappingScrollView(Context c, AttributeSet attrs) {
		this(c, attrs, 0);
	}

	public WrappingScrollView(Context c, AttributeSet attrs, int defStyle) {
		super(c, attrs, defStyle);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(MAX_HEIGHT, MeasureSpec.AT_MOST);
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
}
