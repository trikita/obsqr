package trikita.obsqr;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import android.content.ClipData;
import android.app.Application;
import android.provider.ContactsContract;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/* This class provides QR content parsing for all the types of QRs.
 * The parsers of the different types of QRs implement nested interface QrContent
 * and consist of two basic methods toString() for getting decoded content
 * and launch() for launching an appropriate service/application that is 
 * connected with current type of information
 */

public class QrParser {
	private final static String tag = "QrParser";
	private Context mContext;

	private static QrParser mInstance = null;

	public interface QrContent {
		public void launch();
		public String toString();
	}

	private QrParser() { }

	public static QrParser getInstance() {
		if (mInstance == null) {
			return new QrParser();
		}
		return mInstance;
	}

	public void setContext(Context ctx) {
		mContext = ctx;
	}

	/** Returns the appropriate parser for current type of decoded content */
	public QrContent parse(String s) {
		Log.d(tag, "parse()");
		if (s.startsWith("http://")) {
			return new QrContentUrl(mContext, s);
		} else if (s.startsWith("mailto:")) {
			return new QrContentMail(mContext, s);
		} else if (s.startsWith("smsto:")) {
			return new QrContentSms(mContext, s);
		} else if (s.startsWith("geo:")) {
			return new QrContentGeo(mContext, s);
		} else if (s.startsWith("tel:")) {
			return new QrContentPhone(mContext, s);
		} else if (s.startsWith("market://")) {
			return new QrContentMarket(mContext, s);
		} else if (s.startsWith("MECARD:")) {
			return new QrContentContact(mContext, s);
		} else {
			return new QrContentText(mContext, s);
		}
	}

	/* ----------------------- QR type: plain text --------------------- */
	private class QrContentText implements QrContent {
		private String mTitle;
		private String mContent;
		private Context mContext;

		public QrContentText(Context ctx, String s) {
			mContext = ctx;
			mContent = s;
			mTitle = mContext.getResources().getString(R.string.text_qr_type_name);
		}

		public void launch() {
			ClipboardManager clipboard = ClipboardManager.newInstance(mContext);
			clipboard.setText(mContent);
			Toast.makeText(mContext, "Copy text to buffer", Toast.LENGTH_LONG).show();
		}

		public String toString() {
			return mTitle + "\n" + mContent;
		}
	}

	/* ----------------------- QR type: MECARD contact information --------------------- */
	private class QrContentContact implements QrContent {
		private String mTitle;
		private String mContent;
		private Context mContext;

		/* Contact info */
		private String mName;
		private String mPhone;
		private String mAddress;
		private String mEmail;
		private String mCompany;

		public QrContentContact(Context ctx, String s) {
			mContext = ctx;
			mContent = s;
			mTitle = mContext.getResources().getString(R.string.contact_qr_type_name);
		}

		public void launch() {
			Intent intent = new Intent(Intent.ACTION_INSERT);
			intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
			if (mName != null) {
				intent.putExtra(ContactsContract.Intents.Insert.NAME, mName);
			}
			if (mPhone != null) {
				intent.putExtra(ContactsContract.Intents.Insert.PHONE, mPhone);
			}
			if (mAddress != null) {
				intent.putExtra(ContactsContract.Intents.Insert.POSTAL, mAddress);
			}
			if (mEmail != null) {
				intent.putExtra(ContactsContract.Intents.Insert.EMAIL, mEmail);
			}
			if (mCompany != null) {
				intent.putExtra(ContactsContract.Intents.Insert.COMPANY, mCompany);
			}
			
			mContext.startActivity(intent);
		}

		private void parseContact() {
			String contact = mContent.substring(7);
			Log.d(tag, "contact "+contact);
			String[] tokens = contact.split("(?<!\\\\);");
			for (int i = 0; i < tokens.length; i++) {
				tokens[i] = tokens[i].replace("\\", "");
				Log.d(tag, "token "+tokens[i]);
				if (tokens[i].startsWith("N:")) {
					mName = tokens[i].substring(2);
				}
				if (tokens[i].startsWith("TEL:")) {
					mPhone = tokens[i].substring(4);
				}
				if (tokens[i].startsWith("ADR:")) {
					mAddress = tokens[i].substring(4);
				}
				if (tokens[i].startsWith("EMAIL:")) {
					mEmail = tokens[i].substring(6);
				}
				if (tokens[i].startsWith("ORG:")) {
					mCompany = tokens[i].substring(4);
				}
			}
		}

		public String toString() {
			parseContact();

			StringBuilder res = new StringBuilder(mTitle + "\n");
			if (mName != null) res.append("Name: " + mName + "\n");
			if (mPhone != null) res.append("Phone: " + mPhone + "\n");
			if (mAddress != null) res.append("Address: " + mAddress + "\n");
			if (mEmail != null) res.append("Email: " + mEmail + "\n");
			if (mCompany != null) res.append("Company: " + mCompany);
			return res.toString();
		}
	}
	
	/* ----------------------- QR type: market links --------------------- */
	private class QrContentMarket implements QrContent {
		private String mTitle;
		private String mContent;
		private Context mContext;

		public QrContentMarket(Context ctx, String s) {
			mContext = ctx;
			mContent = s;
			mTitle = mContext.getResources().getString(R.string.market_qr_type_name);
		}

		public void launch() {
			Intent intent = new Intent(Intent.ACTION_VIEW, 
					Uri.parse(mContent));
			mContext.startActivity(intent);
		}

		public String toString() {
			return mTitle + "\n" + mContent;
		}
	}	

	/* ----------------------- QR type: phone number --------------------- */
	private class QrContentPhone implements QrContent {
		private String mTitle;
		private String mContent;
		private Context mContext;

		public QrContentPhone(Context ctx, String s) {
			mContext = ctx;
			mContent = s;
			mTitle = mContext.getResources().getString(R.string.phone_qr_type_name);
		}

		public void launch() {
			Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(mContent));
			mContext.startActivity(intent);
		}

		public String toString() {
			return mTitle + "\n" + mContent.substring(4);
		}
	}	


	/* ----------------------- QR type: geolocation --------------------- */
	private class QrContentGeo implements QrContent {
		private String mTitle;
		private String mContent;
		private Context mContext;

		public QrContentGeo(Context ctx, String s) {
			mContext = ctx;
			mContent = s;
			mTitle = mContext.getResources().getString(R.string.geo_qr_type_name);
		}

		public void launch() {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mContent));
			mContext.startActivity(intent);
		}

		public String toString() {
			String[] s = mContent.substring(4).split(",");
			float latitude = Float.parseFloat(s[0]);
			float longtitude = Float.parseFloat(s[1]);
			String res = "Latitude: " + Math.abs(latitude) + "\u00b0 " + (latitude < 0 ? "S" : "N") + 
				"\n" + "Longtitude: " + Math.abs(longtitude) + "\u00b0 " + (longtitude < 0 ? "W" : "E");
			if (s.length > 2) {
				float height = (Float.parseFloat(s[2]));
				res = res + "\n" + "Meters above the ground: " + s[2];
			}
			return mTitle + "\n" + res;
		}
	}	

	/* ----------------------- QR type: sms --------------------- */
	private class QrContentSms implements QrContent {
		private String mTitle;
		private String mContent;
		private Context mContext;

		public QrContentSms(Context ctx, String s) {
			mContext = ctx;
			mContent = s;
			mTitle = mContext.getResources().getString(R.string.sms_qr_type_name);
		}

		public void launch() {
			String[] s = mContent.split(":");
			String uri= s[0] + ":" + s[1];
			Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(uri));
			intent.putExtra("compose_mode", true);

			if (s.length > 2) {
				intent.putExtra("sms_body", s[2]);
			}
			mContext.startActivity(intent);
		}

		public String toString() {
			String[] s = mContent.split(":");
			String res = "Phone number: " + s[1];
			if (s.length > 2) {
				res = res + "\n" + "Text: " + s[2];
			}
			return mTitle + "\n" + res;
		}
	}	
	
	/* ----------------------- QR type: email --------------------- */
	private class QrContentMail implements QrContent {
		private String mTitle;
		private String mContent;
		private Context mContext;

		public QrContentMail(Context ctx, String s) {
			mContext = ctx;
			mContent = s;
			mTitle = mContext.getResources().getString(R.string.email_qr_type_name);
		}

		public void launch() {
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain"); 
			intent.putExtra(Intent.EXTRA_EMAIL, new String[]{mContent.substring(7)});
			mContext.startActivity(Intent.createChooser(intent, "Send mail..."));
		}

		public String toString() {
			return mTitle + "\n" + mContent.substring(7);
		}
	}

	/* ----------------------- QR type: url --------------------- */
	private class QrContentUrl implements QrContent {
		private String mTitle;
		private String mContent;
		private Context mContext;

		public QrContentUrl(Context ctx, String s) {
			mContext = ctx;
			mContent = s;
			mTitle = mContext.getResources().getString(R.string.url_qr_type_name);
		}

		public void launch() {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mContent));
			mContext.startActivity(intent);
		}

		public String toString() {
			return mTitle + "\n" + mContent;
		}
	}

}
