package trikita.obsqr;

public class Zbar {
	static {
		System.loadLibrary("zbar");
	}

	public native String process(int width, int height, byte []imgData);
}
