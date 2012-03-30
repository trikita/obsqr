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
import android.content.ActivityNotFoundException;

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
		public String getTitle();
		public String getActionName();
	}

	public abstract class BaseQrContent {
		protected String mContent;
		protected String mTitle;

		public abstract void launch();
		public abstract String toString();
		public abstract String getActionName();

		public String getTitle() {
			return mTitle;
		}
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
		if (s.startsWith("http://") || s.startsWith("https://")) {
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
	private class QrContentText extends BaseQrContent implements QrContent {
		private Context mContext;

		public QrContentText(Context ctx, String s) {
			mContext = ctx;
			mContent = s;
			mTitle = mContext.getResources().getString(R.string.text_qr_type_name);
		}

		public void launch() {
			ClipboardManager clipboard = ClipboardManager.newInstance(mContext);
			clipboard.setText(mContent);
			String text = mContext.getResources().getString(R.string.text_qr_action_name);
			Toast.makeText(mContext, text, Toast.LENGTH_LONG).show();
		}

		public String toString() {
			return mContent;
		}

		public String getActionName() {
			return mContext.getResources().getString(R.string.help_prompt_text); 
		}
	}

	/* ----------------------- QR type: MECARD contact information --------------------- */
	private class QrContentContact extends BaseQrContent implements QrContent {
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

			StringBuilder res = new StringBuilder();
			String text;
			if (mName != null) { 
				text = mContext.getResources().getString(R.string.contact_qr_name_title);
				res.append(text + " " + mName + "\n");
			}
			if (mPhone != null) {
				text = mContext.getResources().getString(R.string.contact_qr_phone_title);
				res.append(text + " " + mPhone + "\n");
			}
			if (mAddress != null) {
				text = mContext.getResources().getString(R.string.contact_qr_address_title);
				res.append(text + " " + mAddress + "\n");
			}
			if (mEmail != null) {
				text = mContext.getResources().getString(R.string.contact_qr_email_title);
				res.append(text + " " + mEmail + "\n");
			}
			if (mCompany != null) { 
				text = mContext.getResources().getString(R.string.contact_qr_company_title);
				res.append(text + " " + mCompany);
			}
			return res.toString();
		}

		public String getActionName() {
			return mContext.getResources().getString(R.string.help_prompt_contact);
		}
	}
	
	/* ----------------------- QR type: market links --------------------- */
	private class QrContentMarket extends BaseQrContent implements QrContent {
		private Context mContext;

		public QrContentMarket(Context ctx, String s) {
			mContext = ctx;
			mContent = s;
			mTitle = mContext.getResources().getString(R.string.market_qr_type_name);
		}

		public void launch() {
			try {
				Intent intent = new Intent(Intent.ACTION_VIEW, 
						Uri.parse(mContent));
				mContext.startActivity(intent);
			} catch (ActivityNotFoundException e) {
				Toast.makeText(mContext, mContext.getResources()
						.getString(R.string.alert_msg_invalid_market_link),
						Toast.LENGTH_SHORT).show();
			}
		}

		public String toString() {
			return mContent;
		}

		public String getActionName() {
			return mContext.getResources().getString(R.string.help_prompt_market);
		}
	}	

	/* ----------------------- QR type: phone number --------------------- */
	private class QrContentPhone extends BaseQrContent implements QrContent {
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
			return mContent.substring(4);
		}

		public String getActionName() {
			return mContext.getResources().getString(R.string.help_prompt_phone);
		}
	}	


	/* ----------------------- QR type: geolocation --------------------- */
	private class QrContentGeo extends BaseQrContent implements QrContent {
		private Context mContext;
		private boolean mIsValidData;

		public QrContentGeo(Context ctx, String s) {
			mContext = ctx;
			mContent = s;
			mTitle = mContext.getResources().getString(R.string.geo_qr_type_name);
		}

		public void launch() {
			if (mIsValidData) {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mContent));
				mContext.startActivity(intent);
			}
		}

		public String toString() {
			String[] tokens = mContent.substring(4).split("\\?q=");
			StringBuilder res = new StringBuilder();
			if (tokens.length == 2 && tokens[1].length() > 0) {
				res.append(mContext.getResources().getString(R.string.geo_qr_title_title) +
						" " + tokens[1] + "\n");
			}

			String[] params = tokens[0].split(",");
			if (params.length < 2 || params.length > 3) {
				return mContext.getResources().getString(R.string.unsupported_data_text);
			}

			try {
				float latitude = Float.parseFloat(params[0]);
				String southMark = mContext.getResources().getString(R.string.geo_qr_latitude_south);
				String northMark = mContext.getResources().getString(R.string.geo_qr_latitude_north);
				res.append(mContext.getResources().getString(R.string.geo_qr_latitude_title) +
						" " + Math.abs(latitude) + "\u00b0 " + (latitude < 0 ? southMark : northMark));
				float longtitude = Float.parseFloat(params[1]);	
				String westMark = mContext.getResources().getString(R.string.geo_qr_longitude_west);
				String eastMark = mContext.getResources().getString(R.string.geo_qr_longitude_east);
				res.append("\n" + mContext.getResources().getString(R.string.geo_qr_longitude_title) + 
						" " + Math.abs(longtitude) + "\u00b0 " + (longtitude < 0 ? westMark : eastMark));
				if (params.length == 3) {
					float altitude = Float.parseFloat(params[2]);	
					res.append("\n" + mContext.getResources().getString(R.string.geo_qr_altitude_title) + 
							" " + altitude + " " + mContext.getResources().getString(R.string.geo_qr_altitude_suffix));
				}
				mIsValidData = true;
				return res.toString();
			} catch (NumberFormatException e) {
				return mContext.getResources().getString(R.string.unsupported_data_text);
			}
		}

		public String getActionName() {
			return mContext.getResources().getString(R.string.help_prompt_geo);
		}
	}	

	/* ----------------------- QR type: sms --------------------- */
	private class QrContentSms extends BaseQrContent implements QrContent {
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
			String text = mContext.getResources().getString(R.string.sms_qr_phone_title);
			String res = text + " " + s[1];
			if (s.length > 2) {
				text = mContext.getResources().getString(R.string.sms_qr_message_title);
				res = res + "\n" + text + " " + s[2];
			}
			return res;
		}

		public String getActionName() {
			return mContext.getResources().getString(R.string.help_prompt_sms);
		}
	}	
	
	/* ----------------------- QR type: email --------------------- */
	private class QrContentMail extends BaseQrContent implements QrContent {
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
			String text = mContext.getResources().getString(R.string.email_qr_send_dlg_title);
			mContext.startActivity(Intent.createChooser(intent, text));
		}

		public String toString() {
			return mContent.substring(7);
		}

		public String getActionName() {
			return mContext.getResources().getString(R.string.help_prompt_email);
		}
	}

	/* ----------------------- QR type: url --------------------- */
	private class QrContentUrl extends BaseQrContent implements QrContent {
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
			return mContent;
		}

		public String getActionName() {
			return mContext.getResources().getString(R.string.help_prompt_url);
		}
	}

}
