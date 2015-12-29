package trikita.obsqr;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.test.AndroidTestCase;
import android.test.InstrumentationTestCase;

import java.util.HashMap;
import java.util.Map;

import trikita.obsqr.test.R;

public class ZBarDecoderTest extends InstrumentationTestCase {

	public void testBitmaps() {
		Map<Integer, String> qrs = new HashMap<Integer, String>() {{
			put(R.drawable.qr1, "http://www.qrstuff.com/");
			put(R.drawable.qr2, "http://www.qrdroid.com");
			put(R.drawable.qr3, "http://moo.com");
		}};
		for (Map.Entry<Integer, String> qr : qrs.entrySet()) {
			try {
				Bitmap bitmap =
					BitmapFactory.decodeResource(getInstrumentation().getContext().getResources(), qr.getKey());
				String s = new QrDecoder(getInstrumentation().getContext())
					.decode(bitmap.getWidth(), bitmap.getHeight(), nv21(bitmap));
				assertEquals(qr.getValue(), s);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private byte[] nv21(Bitmap bitmap) {
		int w = bitmap.getWidth();
		int h = bitmap.getHeight();
		int [] argb = new int[w * h];
		bitmap.getPixels(argb, 0, w, 0, 0, w, h);
		byte yuv[] = new byte[w*h + (h+1)*(w+1)/2];
		encodeYUV420SP(yuv, argb, w, h);
		bitmap.recycle();
		return yuv;
	}

	private void encodeYUV420SP(byte[] yuv420sp, int[] argb, int width, int height) {
		int frameSize = width * height;
		int yIndex = 0;
		int uvIndex = frameSize;
		int a, R, G, B, Y, U, V;
		int index = 0;
		for (int j = 0; j < height; j++) {
			for (int i = 0; i < width; i++) {
				a = (argb[index] & 0xff000000) >> 24;
				R = (argb[index] & 0xff0000) >> 16;
				G = (argb[index] & 0xff00) >> 8;
				B = (argb[index] & 0xff) >> 0;

				Y = ( (  66 * R + 129 * G +  25 * B + 128) >> 8) +  16;
				U = ( ( -38 * R -  74 * G + 112 * B + 128) >> 8) + 128;
				V = ( ( 112 * R -  94 * G -  18 * B + 128) >> 8) + 128;

				yuv420sp[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
				if (j % 2 == 0 && index % 2 == 0) { 
					yuv420sp[uvIndex++] = (byte)((V<0) ? 0 : ((V > 255) ? 255 : V));
					yuv420sp[uvIndex++] = (byte)((U<0) ? 0 : ((U > 255) ? 255 : U));
				}
				index++;
			}
		}
	}
}
