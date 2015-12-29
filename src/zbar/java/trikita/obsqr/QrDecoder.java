package trikita.obsqr;

import android.content.Context;

/* This class provides a wrapper for launching zbar library function
 * that implements qr code recognition
 */

public class QrDecoder {
	static {
		System.loadLibrary("zbar");
	}

	public QrDecoder(Context c) {}

	public native String decode(int width, int height, byte []imgData);
}
