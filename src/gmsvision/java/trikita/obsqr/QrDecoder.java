package trikita.obsqr;

import android.content.Context;
import android.graphics.ImageFormat;
import android.util.SparseArray;

import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.android.gms.vision.Frame;

import java.nio.ByteBuffer;

public class QrDecoder {

	private BarcodeDetector mDetector;

	public QrDecoder(Context c) {
		mDetector = new BarcodeDetector.Builder(c).build();
	}

	public String decode(int width, int height, byte []imgData) {
		String result = null;
		ByteBuffer buffer = ByteBuffer.wrap(imgData);
		SparseArray<Barcode> barcodes = mDetector.detect(new Frame.Builder()
				.setImageData(buffer, width, height, ImageFormat.NV21).build());
		if (barcodes != null) {
			for(int i = 0; i < barcodes.size(); i++) {
				Barcode b = barcodes.get(barcodes.keyAt(i));
				if (b.rawValue != null) {
					result = b.rawValue;
				}
				System.out.println("Barcode = " + b.rawValue);
			}
		}
		return result;
	}
}

