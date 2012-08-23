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
import java.net.URL;

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
		public void launch() throws android.content.ActivityNotFoundException;
		public String toString();
		public String getTitle();
		public String getActionName();
	}

	public abstract static class BaseQrContent implements QrContent {
		protected String mContent;
		protected String mTitle;

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
		if (QrContentMail.match(s)) {
			return new QrContentMail(mContext, s);
		} else if (QrContentUrl.match(s)) {
			return new QrContentUrl(mContext, s);
		} else if (QrContentSms.match(s)) {
			return new QrContentSms(mContext, s);
		} else if (QrContentGeo.match(s)) {
			return new QrContentGeo(mContext, s);
		} else if (QrContentPhone.match(s)) {
			return new QrContentPhone(mContext, s);
		} else if (QrContentMarket.match(s)) {
			return new QrContentMarket(mContext, s);
		} else if (QrContentContact.match(s)) {
			return new QrContentContact(mContext, s);
		} else {
			return new QrContentText(mContext, s);
		}
	}

	/* ----------------------- QR type: plain text --------------------- */
	private static class QrContentText extends BaseQrContent {
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

		public static boolean match(String s) {
			return true;
		}
	}

	/* ----------------------- QR type: MECARD contact information --------------------- */
	private static class QrContentContact extends BaseQrContent {
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

		public static boolean match(String s) {
			return (s.startsWith("MECARD:"));
		}
	}
	
	/* ----------------------- QR type: market links --------------------- */
	private static class QrContentMarket extends BaseQrContent {
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

		public static boolean match(String s) {
			return (s.startsWith("market://"));
		}
	}	

	/* ----------------------- QR type: phone number --------------------- */
	private static class QrContentPhone extends BaseQrContent {
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

		public static boolean match(String s) {
			return (s.startsWith("tel:"));
		}
	}	


	/* ----------------------- QR type: geolocation --------------------- */
	private static class QrContentGeo extends BaseQrContent {
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
							" " + altitude + " " + 
							mContext.getResources().getString(R.string.geo_qr_altitude_suffix));
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

		public static boolean match(String s) {
			return (s.startsWith("geo:"));
		}
	}	

	/* ----------------------- QR type: sms --------------------- */
	private static class QrContentSms extends BaseQrContent {
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

		public static boolean match(String s) {
			return (s.startsWith("smsto:"));
		}
	}	
	
	/* ----------------------- QR type: email --------------------- */
	private static class QrContentMail extends BaseQrContent {
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

		public static boolean match(String s) {
			return (s.startsWith("mailto:"));
		}
	}

	/* ----------------------- QR type: url --------------------- */
	private static class QrContentUrl extends BaseQrContent {
		private Context mContext;

		private final static String URL_REGEX = "((https?|ftp)\\:\\/\\/)?" + // SCHEME
			"([a-z0-9+!*(),;?&=\\$_.-]+(\\:[a-z0-9+!*(),;?&=\\$_.-]+)?@)?" + // User and Pass
			"([a-z0-9-.]*)\\.([a-z]{2,3})" + // Host or IP
			"(\\:[0-9]{2,5})?" + // Port
			"(\\/([a-z0-9+\\$_-]\\.?)+)*\\/?" + // Path
			"(\\?[a-z+&\\$_.-][a-z0-9;:@&%=+\\/\\$_.-]*)?" + // GET Query
			"(#[a-z_.-][a-z0-9+\\$_.-]*)?"; // Anchor

		public static final String TOP_LEVEL_DOMAIN_STR_FOR_WEB_URL =
			"(?:"
			+ "(?:aero|arpa|asia|a[cdefgilmnoqrstuwxz])"
			+ "|(?:biz|b[abdefghijmnorstvwyz])"
			+ "|(?:cat|com|coop|c[acdfghiklmnoruvxyz])"
			+ "|d[ejkmoz]"
			+ "|(?:edu|e[cegrstu])"
			+ "|f[ijkmor]"
			+ "|(?:gov|g[abdefghilmnpqrstuwy])"
			+ "|h[kmnrtu]"
			+ "|(?:info|int|i[delmnoqrst])"
			+ "|(?:jobs|j[emop])"
			+ "|k[eghimnprwyz]"
			+ "|l[abcikrstuvy]"
			+ "|(?:mil|mobi|museum|m[acdeghklmnopqrstuvwxyz])"
			+ "|(?:name|net|n[acefgilopruz])"
			+ "|(?:org|om)"
			+ "|(?:pro|p[aefghklmnrstwy])"
			+ "|qa"
			+ "|r[eosuw]"
			+ "|s[abcdeghijklmnortuvyz]"
			+ "|(?:tel|travel|t[cdfghjklmnoprtvwz])"
			+ "|u[agksyz]"
			+ "|v[aceginu]"
			+ "|w[fs]"
			+ "|(?:xn\\-\\-0zwm56d|xn\\-\\-11b5bs3a9aj6g|xn\\-\\-80akhbyknj4f|xn\\-\\-9t4b11yi5a|xn\\-\\-deba0ad|xn\\-\\-g6w251d|xn\\-\\-hgbk6aj7f53bba|xn\\-\\-hlcj6aya9esc7a|xn\\-\\-jxalpdlp|xn\\-\\-kgbechtv|xn\\-\\-zckzah)"
			+ "|y[etu]"
			+ "|z[amw]))";

		/**
		 * Good characters for Internationalized Resource Identifiers (IRI).
		 * This comprises most common used Unicode characters allowed in IRI
		 * as detailed in RFC 3987.
		 * Specifically, those two byte Unicode characters are not included.
		 */
		public static final String GOOD_IRI_CHAR =
			"a-zA-Z0-9\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF";

		/**
		 *  Regular expression pattern to match most part of RFC 3987
		 *  Internationalized URLs, aka IRIs.  Commonly used Unicode characters are
		 *  added.
		 */
		public static final Pattern WEB_URL = Pattern.compile(
			"((?:(http|https|Http|Https|rtsp|Rtsp):\\/\\/(?:(?:[a-zA-Z0-9\\$\\-\\_\\.\\+\\!\\*\\'\\(\\)"
			+ "\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,64}(?:\\:(?:[a-zA-Z0-9\\$\\-\\_"
			+ "\\.\\+\\!\\*\\'\\(\\)\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,25})?\\@)?)?"
			+ "((?:(?:[" + GOOD_IRI_CHAR + "][" + GOOD_IRI_CHAR + "\\-]{0,64}\\.)+"   // named host
			+ TOP_LEVEL_DOMAIN_STR_FOR_WEB_URL
			+ "|(?:(?:25[0-5]|2[0-4]" // or ip address
			+ "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(?:25[0-5]|2[0-4][0-9]"
			+ "|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(?:25[0-5]|2[0-4][0-9]|[0-1]"
			+ "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
			+ "|[1-9][0-9]|[0-9])))"
			+ "(?:\\:\\d{1,5})?)" // plus option port number
			+ "(\\/(?:(?:[" + GOOD_IRI_CHAR + "\\;\\/\\?\\:\\@\\&\\=\\#\\~"  // plus option query params
			+ "\\-\\.\\+\\!\\*\\'\\(\\)\\,\\_])|(?:\\%[a-fA-F0-9]{2}))*)?"
			+ "(?:\\b|$)"); // and finally, a word boundary or end of
							// input.  This is to stop foo.sure from
							// matching as foo.su

		public QrContentUrl(Context ctx, String s) {
			mContext = ctx;
			mContent = s;
			mTitle = mContext.getResources().getString(R.string.url_qr_type_name);
		}

		public void launch() {
			if (!mContent.startsWith("http:") &&
				!mContent.startsWith("https:") &&
				!mContent.startsWith("ftp:")) { mContent = "http://" + mContent; }

			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mContent));
			mContext.startActivity(intent);
		}

		public String toString() {
			return mContent;
		}

		public String getActionName() {
			return mContext.getResources().getString(R.string.help_prompt_url);
		}

		public static boolean match(String s) {
			//Pattern pattern = Pattern.compile(URL_REGEX);
			//Matcher matcher = pattern.matcher(s);
			Matcher matcher = WEB_URL.matcher(s);
			return matcher.matches();
		}
	}

}
