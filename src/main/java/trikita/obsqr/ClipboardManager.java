package trikita.obsqr;

import android.os.Build;
import android.content.Context;
import android.annotation.TargetApi;

/**
 * This class provides a basic wrapper around the built-in ClipboardManager
 * class that manages copying data to and from the system clipboard.  This
 * provides a wrapper around the API-specific versions of the class to return
 * the proper object for the platform we're currently running on.
 */
public abstract class ClipboardManager {
	protected static Context mContext;

	public abstract void setText(CharSequence text);

	public static ClipboardManager newInstance(Context ctx) {
		mContext = ctx;

		final int sdkVersion = Integer.parseInt(Build.VERSION.SDK);
		if (sdkVersion < Build.VERSION_CODES.HONEYCOMB) {
			return new OldClipboardManager();
		} else {
			return new HoneycombClipboardManager();
		}
	}
	
	/**
	 * The old ClipboardManager, which is a under android.text.  This is
	 * the version to use for all Android versions less than 3.0. 
	 */
	private static class OldClipboardManager extends ClipboardManager {
		private final android.text.ClipboardManager clippy;
		
		public OldClipboardManager() {
			clippy = (android.text.ClipboardManager) mContext
				.getSystemService(Context.CLIPBOARD_SERVICE);
		}
		
		@Override
		public void setText(CharSequence text) {
			clippy.setText(text);
		}
	}
	
	@TargetApi(11)
	private static class HoneycombClipboardManager extends ClipboardManager {
		private final android.content.ClipboardManager clippy;
		private android.content.ClipData clipData;
		
		public HoneycombClipboardManager() {
			clippy = (android.content.ClipboardManager) mContext
				.getSystemService(Context.CLIPBOARD_SERVICE);
		}
		
		@Override
		public void setText(CharSequence text) {
			clipData = android.content.ClipData
				.newPlainText(android.content.ClipDescription.MIMETYPE_TEXT_PLAIN, text);
			clippy.setPrimaryClip(clipData);
		}
	}
}
