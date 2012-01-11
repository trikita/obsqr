package trikita.obsqr;

/* This class provides a wrapper for launching zbar library function
 * that implements qr code recognition
 */

public class Zbar {
	static {
		System.loadLibrary("zbar");
	}

	public native String process(int width, int height, byte []imgData);
}
