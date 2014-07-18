package trikita.obsqr;

import android.view.View;
import android.content.Context;
import android.widget.TextView;
import android.util.Log;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.content.Intent;
import android.provider.ContactsContract;
import android.widget.Toast;
import java.util.List;
import java.util.ArrayList;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

public abstract class QrContent {

	public final static String tag = "QrContent";

	protected Context mContext;
	protected String mText;

	abstract int getTitleStringId();
	abstract int getActionStringId();
	abstract void action();

	private QrContent(Context c, String s) {
		mContext = c;
		mText = s;
	}

	public String getTitle() {
		return mContext.getString(getTitleStringId());
	}

	public String getAction() {
		return mContext.getString(getActionStringId());
	}

	public View render() {
		TextView tv = new TextView(mContext);
		tv.setTextSize(14);
		tv.setTextColor(0x8a000000);
		tv.setText(mText);
		return tv;
	}

	public String getText() {
		return mText;
	}

	public static List<String> getTokens(String s) {
		List<String> tokens = new ArrayList<String>();

		int len = s.length();
		StringBuilder builder = new StringBuilder();
		boolean escaped = false;

		// treat a char sequence between two non-escaped semicolons
		// as a single token
		for (int i = 0; i < len; i++) {
			if (escaped) {
				builder.append(s.charAt(i));
				escaped = false;
			} else {
				if (s.charAt(i) == ';') {
					tokens.add(builder.toString());
					builder = new StringBuilder();
				} else if (s.charAt(i) == '\\') {
					escaped = true;
				} else {
					builder.append(s.charAt(i));
				}
			}
		}
		return tokens;
	}

	public static QrContent from(Context c, String s) {
		if (s.matches(GooglePlayContent.MATCH)) {
			return new GooglePlayContent(c, s);
		} else if (s.matches(WebUrlContent.MATCH)) {
			return new WebUrlContent(c, s);
		} else if (s.matches(EmailContent.MATCH)) {
			return new EmailContent(c, s);
		} else if (s.matches(PhoneNumberContent.MATCH)) {
			return new PhoneNumberContent(c, s);
		} else if (s.matches(SmsContent.MATCH)) {
			return new SmsContent(c, s);
		} else if (s.matches(ContactContent.MATCH)) {
			return new ContactContent(c, s);
		} else if (s.matches(GeoLocationContent.MATCH)) {
			return new GeoLocationContent(c, s);
		} else if (s.matches(WifiContent.MATCH)) {
			return new WifiContent(c, s);
		} else {
			return new QrMixedContent(c, s);
		}
	}

	/**
	 * Mixed content: plain text that may contain some URLs, emails etc
	 */
	static class QrMixedContent extends QrContent {

		public QrMixedContent(Context c, String s) {
			super(c, s);
		}
		public int getTitleStringId() { return R.string.title_text; }
		public int getActionStringId() { return R.string.action_text; }

		public void action() {
			Log.d(tag, "action: copy to clipboard " + mText);
			ClipboardManager.newInstance(mContext).setText(mText);
			Toast.makeText(mContext, mContext.getString(R.string.text_qr_action_name),
					Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * Web URL
	 */
	static class WebUrlContent extends QrContent {
		public final static String MATCH = 
			"(https?:\\/\\/)?([\\da-z\\.-]+)\\.([a-z\\.]{2,6})([\\/\\w \\.-]*)*\\/?";
		public WebUrlContent(Context c, String s) {
			super(c, s);
		}

		public int getTitleStringId() { return R.string.title_url; }
		public int getActionStringId() { return R.string.action_url; }

		public void action() {
			String s = mText;
			if (!s.startsWith("http:") && !s.startsWith("https:") && !s.startsWith("ftp:")) {
				s = "http://" + s;
			}
			mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(s)));
		}
	}

	/**
	 * E-mail address
	 */
	static class EmailContent extends QrContent {
		// "^([a-z0-9_\.-]+)@([\da-z\.-]+)\.([a-z\.]{2,6})$"
		public final static String MATCH = "mailto:(.*)";
		public EmailContent(Context c, String s) {
			super(c, s);
		}

		public int getTitleStringId() { return R.string.title_email; }
		public int getActionStringId() { return R.string.action_email; }

		public void action() {
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain"); 
			intent.putExtra(Intent.EXTRA_EMAIL, new String[]{mText.substring(7)});
			String text = mContext.getString(R.string.email_qr_send_dlg_title);
			mContext.startActivity(Intent.createChooser(intent, text));
		}
	}

	/**
	 * SMS
	 */
	static class SmsContent extends QrContent {
		public final static String MATCH = "smsto:(.*)";
		private String mOriginalUri;
		public SmsContent(Context c, String s) {
			super(c, s);
			mOriginalUri = s;
			mText = toString();
		}

		public int getTitleStringId() { return R.string.title_sms; }
		public int getActionStringId() { return R.string.action_sms; }

		public void action() {
			String[] s = mOriginalUri.split(":");
			String uri= s[0] + ":" + s[1];
			Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(uri));
			intent.putExtra("compose_mode", true);

			if (s.length > 2) {
				intent.putExtra("sms_body", s[2]);
			}
			mContext.startActivity(intent);
		}

		public String toString() {
			String[] s = mText.split(":");
			String text = mContext.getString(R.string.sms_qr_phone_title);
			String res = text + " " + s[1];
			if (s.length > 2) {
				text = mContext.getString(R.string.sms_qr_message_title);
				res = res + "\n" + text + " " + s[2];
			}
			return res;
		}

	}

	/**
	 * Phone number
	 */
	static class PhoneNumberContent extends QrContent {
		public final static String MATCH = "tel:(.*)";
		private String mOriginalUri;
		public PhoneNumberContent(Context c, String s) {
			super(c, s);
			mOriginalUri = s;
			mText = s.substring(4);
		}

		public int getTitleStringId() { return R.string.title_phone; }
		public int getActionStringId() { return R.string.action_phone; }

		public void action() {
			mContext.startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse(mOriginalUri)));
		}
	}

	/**
	 * Geolocation
	 */
	static class GeoLocationContent extends QrContent {
		public final static String MATCH = "geo:(.*)";
		private boolean mIsValidData;
		private String mOriginalGeoUri;

		public GeoLocationContent(Context c, String s) {
			super(c, s);
			mOriginalGeoUri = s;
			mText = toString();
		}

		public int getTitleStringId() { return R.string.title_geo; }
		public int getActionStringId() { return R.string.action_geo; }

		public String toString() {
			String[] tokens = mText.substring(4).split("\\?q=");
			StringBuilder res = new StringBuilder();
			if (tokens.length == 2 && tokens[1].length() > 0) {
				res.append(mContext.getString(R.string.geo_qr_title_title) +
						" " + tokens[1] + "\n");
			}

			String[] params = tokens[0].split(",");
			if (params.length < 2 || params.length > 3) {
				return mContext.getString(R.string.unsupported_data_text);
			}

			try {
				float latitude = Float.parseFloat(params[0]);
				String southMark = mContext.getString(R.string.geo_qr_latitude_south);
				String northMark = mContext.getString(R.string.geo_qr_latitude_north);
				res.append(mContext.getString(R.string.geo_qr_latitude_title) +
						" " + Math.abs(latitude) + "\u00b0 " + (latitude < 0 ? southMark : northMark));
				float longtitude = Float.parseFloat(params[1]);	
				String westMark = mContext.getString(R.string.geo_qr_longitude_west);
				String eastMark = mContext.getString(R.string.geo_qr_longitude_east);
				res.append("\n" + mContext.getString(R.string.geo_qr_longitude_title) + 
						" " + Math.abs(longtitude) + "\u00b0 " + (longtitude < 0 ? westMark : eastMark));
				if (params.length == 3) {
					float altitude = Float.parseFloat(params[2]);	
					res.append("\n" + mContext.getString(R.string.geo_qr_altitude_title) + 
							" " + altitude + " " + 
							mContext.getString(R.string.geo_qr_altitude_suffix));
				}
				mIsValidData = true;
				return res.toString();
			} catch (NumberFormatException e) {
				return mContext.getString(R.string.unsupported_data_text);
			}
		}

		public void action() {
			if (mIsValidData) {
				mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(mOriginalGeoUri)));
			}
		}
	}

	/**
	 * Contact information
	 */
	static class ContactContent extends QrContent {
		public final static String MATCH = "MECARD:(.*)";

		private String mName;
		private String mPhone;
		private String mAddress;
		private String mEmail;
		private String mCompany;

		public ContactContent(Context c, String s) {
			super(c, s);
			mText = toString();
		}

		public int getTitleStringId() { return R.string.title_contact; }
		public int getActionStringId() { return R.string.action_contact; }

		private void parseContact() {
			String contact = mText.substring(7);
			Log.d(tag, "contact " + contact);

			List<String> tokens = getTokens(contact);
			for (String token : tokens) {
				if (token.startsWith("N:")) {
					mName = token.substring(2);
				}
				if (token.startsWith("TEL:")) {
					mPhone = token.substring(4);
				}
				if (token.startsWith("ADR:")) {
					mAddress = token.substring(4);
				}
				if (token.startsWith("EMAIL:")) {
					mEmail = token.substring(6);
				}
				if (token.startsWith("ORG:")) {
					mCompany = token.substring(4);
				}
			}
		}

		public String toString() {
			parseContact();

			StringBuilder res = new StringBuilder();
			String text;
			if (mName != null) { 
				text = mContext.getString(R.string.contact_qr_name_title);
				res.append(text + " " + mName + "\n");
			}
			if (mPhone != null) {
				text = mContext.getString(R.string.contact_qr_phone_title);
				res.append(text + " " + mPhone + "\n");
			}
			if (mAddress != null) {
				text = mContext.getString(R.string.contact_qr_address_title);
				res.append(text + " " + mAddress + "\n");
			}
			if (mEmail != null) {
				text = mContext.getString(R.string.contact_qr_email_title);
				res.append(text + " " + mEmail + "\n");
			}
			if (mCompany != null) { 
				text = mContext.getString(R.string.contact_qr_company_title);
				res.append(text + " " + mCompany);
			}
			return res.toString();
		}

		public void action() {
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
	}

	/**
	 * Google Play URL
	 */
	static class GooglePlayContent extends QrContent {
		public final static String MATCH = "market://(.*)";
		public final static String GOOGLEPLAY_ID = "market://details\\?id=(.*)";

		public GooglePlayContent(Context c, String s) {
			super(c, s);
			if (s.matches(GOOGLEPLAY_ID)) {
				Matcher m = Pattern.compile(GOOGLEPLAY_ID).matcher(s);
				m.find();
				mText = m.group(1);
			}
		}

		public int getTitleStringId() { return R.string.title_market; }
		public int getActionStringId() { return R.string.action_market; }

		public void action() {
			try {
				mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(mText)));
			} catch (ActivityNotFoundException e) {
				Toast.makeText(mContext, mContext.getResources()
						.getString(R.string.alert_msg_invalid_market_link),
						Toast.LENGTH_SHORT).show();
			}
		}
	}

	/**
	 * WiFi access point
	 */
	static class WifiContent extends QrContent {
		public final static String MATCH = "WIFI:(.*)";

		private String mType;
		private String mNetworkSsid;
		private String mPassword;
		private boolean mSsidHidden;
		private String mOriginalUri;

		public WifiContent(Context c, String s) {
			super(c, s);
			mOriginalUri = s;
			mText = toString();
		}

		public int getTitleStringId() { return R.string.title_wifi; }
		public int getActionStringId() { return R.string.action_wifi; }

		private void parseWifi() {
			String wifi = mOriginalUri.substring(5);
			Log.d(tag, "wifi " + wifi);

			List<String> tokens = getTokens(wifi);
			for (String token : tokens) {
				if (token.startsWith("T:")) {
					mType = token.substring(2);
				}
				if (token.startsWith("S:")) {
					mNetworkSsid = token.substring(2);
				}
				if (token.startsWith("P:")) {
					mPassword = token.substring(2);
				}
				if (token.startsWith("H:")) {
					mSsidHidden = Boolean.valueOf(token.substring(2));
				}
			}

			if (mType == null) {
				mType = "nopass";
			}
		}

		public String toString() {
			parseWifi();

			StringBuilder res = new StringBuilder();
			String text;
			if (mType != null) { 
				text = mContext.getString(R.string.wifi_qr_security_title);
				res.append(text + " " + mType + "\n");
			}
			if (mNetworkSsid != null) {
				text = mContext.getString(R.string.wifi_qr_ssid_title);
				res.append(text + " " + mNetworkSsid + "\n");
			}
			if (mPassword != null) {
				text = mContext.getString(R.string.wifi_qr_password_title);
				res.append(text + " " + mPassword + "\n");
			}

			return res.toString();
		}

		public void action() {
			WifiConfiguration conf = new WifiConfiguration();
			conf.SSID = "\"" + mNetworkSsid + "\"";

			if (mType.equals("WEP")) {
				if (mPassword.matches("[0-9A-Fa-f]+")) {
					conf.wepKeys[0] = mPassword;
				} else {
					conf.wepKeys[0] = "\"" + mPassword + "\""; 
				}
				conf.wepTxKeyIndex = 0;

				conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
				conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
			} else if (mType.equals("WPA")) {
				conf.preSharedKey = "\""+ mPassword +"\"";
			} else if (mType.equals("nopass")) {
				conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
			} else {
				Log.d(tag, "failed to read wifi configuration");
				return;
			}

			WifiManager wifiManager = (WifiManager) mContext
				.getSystemService(Context.WIFI_SERVICE); 
			if (!wifiManager.isWifiEnabled()) {
				Log.d(tag, "enable wifi");
				wifiManager.setWifiEnabled(true);
			} else {
				Log.d(tag, "wifi is already enabled");
			}

			int networkId = wifiManager.addNetwork(conf);
			wifiManager.saveConfiguration();
			if (networkId != -1) {
				Log.d(tag, "added new network " + mNetworkSsid + " successfully");
				if (wifiManager.enableNetwork(networkId, true)) {
					Toast.makeText(mContext,
						mContext.getString(R.string.alert_msg_wifi_connected),
						Toast.LENGTH_LONG).show();
					Log.d(tag, "connected to network " + mNetworkSsid + " successfully");
				} else {
					Toast.makeText(mContext,
						mContext.getString(R.string.alert_msg_wifi_failed),
						Toast.LENGTH_LONG).show();
					Log.d(tag, "failed to connect to network " + mNetworkSsid);
				}
			} else {
				Log.d(tag, "failed to add new wifi network");
			}
		}
	}
}
