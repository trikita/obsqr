package trikita.obsqr;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.MailTo;
import android.net.ParseException;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.Spannable;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class QrContent {
    public final String rawContent;

    public final String title;
    public final String action;
    public final Spannable content;

	protected final Context context;

    private QrContent(Context c, String s, String title, String action, Spannable content) {
		this.context = c;
        this.rawContent = s;
        this.action = action;
        this.title = title;
        this.content = content;
    }

    public void performAction() {
		try {
			context.startActivity(getActionIntent());
		} catch (ActivityNotFoundException e) {
			// FIXME: wrong string resource
			Toast.makeText(context, context.getString(R.string.alert_msg_invalid_market_link),
					Toast.LENGTH_SHORT).show();
		}
	}

	public Intent getActionIntent() {
		return new Intent(Intent.ACTION_VIEW, Uri.parse(rawContent));
	}

	// Helper utils: copy text to primary clipboard, split qr string into tokens etc
    private static void copyToClipboard(Context context, String s) {
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText(ClipDescription.MIMETYPE_TEXT_PLAIN, s));
		Toast.makeText(context, context.getString(R.string.text_qr_action_name),
				Toast.LENGTH_LONG).show();
    }

	private static Map<String, String> parse(String s, String... keys) {
		Map<String, String> tokens = new HashMap<>();

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
					String token = builder.toString();
					for (String key : keys) {
						if (token.startsWith(key+":")) {
							tokens.put(key, token.substring(key.length()+1));
						}
					}
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

	public static Spannable spannable(String s) {
		return Spannable.Factory.getInstance().newSpannable(s);
	}

	public static QrContent from(Context c, String s) {
		String m = s.toLowerCase();
		if (m.matches(GooglePlayContent.MATCH)) {
			return new GooglePlayContent(c, s);
		} else if (m.matches(WebUrlContent.MATCH)) {
			return new WebUrlContent(c, s);
		} else if (m.matches(EmailContent.MATCH)) {
			return new EmailContent(c, s);
		} else if (m.matches(PhoneNumberContent.MATCH)) {
			return new PhoneNumberContent(c, s);
		} else if (m.matches(SmsContent.MATCH)) {
			return new SmsContent(c, s);
		} else if (m.matches(ContactContent.MATCH)) {
			return new ContactContent(c, s);
		} else if (m.matches(GeoLocationContent.MATCH)) {
			return new GeoLocationContent(c, s);
		} else if (m.matches(WifiContent.MATCH)) {
			return new WifiContent(c, s);
		} else {
			return new QrMixedContent(c, s);
		}
	}

	/** Mixed content: plain text that may contain some URLs, emails etc */
	static class QrMixedContent extends QrContent {
		public QrMixedContent(Context c, String s) {
			super(c, s, c.getString(R.string.title_text), c.getString(R.string.action_text), spannable(s));
		}
		public void performAction() {
			copyToClipboard(context, rawContent);
		}
	}

	/** Web URL */
	static class WebUrlContent extends QrContent {
		public final static String MATCH = android.util.Patterns.WEB_URL.pattern();
		public WebUrlContent(Context c, String s) {
			super(c, s, c.getString(R.string.title_url), c.getString(R.string.action_url), url(s));
		}
		private static Spannable url(String s) {
			if (!s.startsWith("http:") && !s.startsWith("https:") && !s.startsWith("ftp:")) {
				s = "http://" + s;
			}
			return spannable(s);
		}
	}

	/** E-mail address */
	static class EmailContent extends QrContent {
		public final static String MATCH = "mailto:(.*)";
		public EmailContent(Context c, String s) {
			super(c, s, c.getString(R.string.title_email),
					c.getString(R.string.action_email), getContent(c, s));
		}
		private static Spannable getContent(Context c, String s) {
			try {
				MailTo uri = MailTo.parse("mailto:" + s.substring(7));
				System.out.println("" + uri.getTo());
				return spannable(uri.getTo());
			} catch (ParseException e) {
				e.printStackTrace();
				return spannable(s);
			}
		}
		public Intent getActionIntent() {
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain");
			try {
				MailTo uri = MailTo.parse("mailto:" + rawContent.substring(7));
				intent.putExtra(Intent.EXTRA_EMAIL, uri.getTo());
				intent.putExtra(Intent.EXTRA_SUBJECT, uri.getSubject());
				intent.putExtra(Intent.EXTRA_TEXT, uri.getBody());
			} catch (ParseException e) {
				intent.putExtra(Intent.EXTRA_EMAIL, new String[]{content.toString()});
			}
			String text = context.getString(R.string.email_qr_send_dlg_title);
			return Intent.createChooser(intent, text);
		}
	}

	/** SMS */
	static class SmsContent extends QrContent {
		public final static String MATCH = "smsto:(.*)";
		public SmsContent(Context c, String s) {
			super(c, s, c.getString(R.string.title_sms), c.getString(R.string.action_sms), getContent(c, s));
		}
		private static Spannable getContent(Context c, String raw) {
			String[] s = raw.split(":");
			String text = c.getString(R.string.sms_qr_phone_title);
			String res = text + " " + s[1];
			if (s.length > 2) {
				text = c.getString(R.string.sms_qr_message_title);
				res = res + "\n" + text + " " + s[2];
			}
			return spannable(res);
		}
		public Intent getActionIntent() {
			String[] s = rawContent.split(":");
			String uri= s[0] + ":" + s[1];
			Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(uri));
			intent.putExtra("compose_mode", true);
			if (s.length > 2) {
				intent.putExtra("sms_body", s[2]);
			}
			return intent;
		}
	}

	/** Phone number */
	static class PhoneNumberContent extends QrContent {
		public final static String MATCH = "tel:(.*)";
		public PhoneNumberContent(Context c, String s) {
			super(c, s, c.getString(R.string.title_phone), c.getString(R.string.action_phone), spannable(s.substring(4)));
		}
		public Intent getActionIntent() {
			return new Intent(Intent.ACTION_DIAL, Uri.parse(rawContent));
		}
	}

	/** Geolocation */
	static class GeoLocationContent extends QrContent {
		public final static String MATCH = "geo:(.*)";
		public GeoLocationContent(Context c, String s) {
			super(c, s, c.getString(R.string.title_geo), c.getString(R.string.action_geo), spannable(getContent(c, s)));
		}
		private static String getContent(Context context, String s) {
			String[] tokens = s.substring(4).split("\\?q=");
			StringBuilder res = new StringBuilder();
			if (tokens.length == 2 && tokens[1].length() > 0) {
				res.append(context.getString(R.string.geo_qr_title_title) +
						" " + tokens[1] + "\n");
			}

			String[] params = tokens[0].split(",");
			if (params.length < 2 || params.length > 3) {
				return context.getString(R.string.unsupported_data_text);
			}

			try {
				float latitude = Float.parseFloat(params[0]);
				String southMark = context.getString(R.string.geo_qr_latitude_south);
				String northMark = context.getString(R.string.geo_qr_latitude_north);
                res.append(context.getString(R.string.geo_qr_latitude_title)).append(" ")
                        .append(Math.abs(latitude)).append("\u00b0 ").append(latitude < 0 ? southMark : northMark);
                float longitude = Float.parseFloat(params[1]);
				String westMark = context.getString(R.string.geo_qr_longitude_west);
				String eastMark = context.getString(R.string.geo_qr_longitude_east);
                res.append("\n").append(context.getString(R.string.geo_qr_longitude_title)).append(" ")
                        .append(Math.abs(longitude)).append("\u00b0 ").append(longitude < 0 ? westMark : eastMark);
				if (params.length == 3) {
					float altitude = Float.parseFloat(params[2]);
                    res.append("\n").append(context.getString(R.string.geo_qr_altitude_title)).append(" ").append(altitude).append(" ").append(context.getString(R.string.geo_qr_altitude_suffix));
				}
				return res.toString();
			} catch (NumberFormatException e) {
				return context.getString(R.string.unsupported_data_text);
			}
		}
	}

	/** Contact information */
	static class ContactContent extends QrContent {
		public final static String MATCH = "mecard:(.*)";
		private static String FIELDS[] = new String[]{"N", "TEL", "ADR", "EMAIL", "ORG"};
		private static int FIELD_NAMES[] = new int[]{
				R.string.contact_qr_name_title,
				R.string.contact_qr_phone_title,
				R.string.contact_qr_address_title,
				R.string.contact_qr_email_title,
				R.string.contact_qr_company_title,
		};
		private static String INTENT_FIELDS[] = new String[]{
				ContactsContract.Intents.Insert.NAME,
				ContactsContract.Intents.Insert.PHONE,
				ContactsContract.Intents.Insert.POSTAL,
				ContactsContract.Intents.Insert.EMAIL,
				ContactsContract.Intents.Insert.COMPANY,
		};

		public ContactContent(Context c, String s) {
			super(c, s, c.getString(R.string.title_contact), c.getString(R.string.action_contact), getContent(c, s));
		}

		private static Spannable getContent(Context c, String s) {
			StringBuilder res = new StringBuilder();
			Map<String, String> tokens = parse(s.substring(7), FIELDS);
			for (int i = 0; i < FIELDS.length; i++) {
				if (tokens.get(FIELDS[i]) != null) {
					res.append(c.getString(FIELD_NAMES[i])).append(" ")
							.append(tokens.get(FIELDS[i])).append('\n');
				}
			}
			return spannable(res.toString());
		}

		public Intent getActionIntent() {
			Intent intent = new Intent(Intent.ACTION_INSERT);
			intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
			Map<String, String> tokens = parse(rawContent.substring(7), FIELDS);
			for (int i = 0; i < FIELDS.length; i++) {
				if (tokens.get(FIELDS[i]) != null) {
					intent.putExtra(INTENT_FIELDS[i], tokens.get(FIELDS[i]));
				}
			}
			return intent;
		}
	}

	/** Google Play URL */
	static class GooglePlayContent extends QrContent {
		public final static String MATCH = "market://(details\\?id=)?(.*)";

		public GooglePlayContent(Context c, String s) {
			super(c, s, c.getString(R.string.title_market), c.getString(R.string.action_market), getContent(s));
		}
		private static Spannable getContent(String s) {
			Matcher m = Pattern.compile(MATCH).matcher(s);
			if (m.matches() && m.group(1) != null) {
				return spannable(m.group(2));
			} else {
				return spannable(s);
			}
		}
	}

	/** WiFi access point */
	static class WifiContent extends QrContent {
		public final static String MATCH = "wifi:(.*)";
		private final static String[] FIELDS = new String[]{"T", "S", "P"};
		private final static int[] FIELD_NAMES = new int[]{
				R.string.wifi_qr_security_title,
				R.string.wifi_qr_ssid_title,
				R.string.wifi_qr_password_title,
		};
		public WifiContent(Context c, String s) {
			super(c, s, c.getString(R.string.title_wifi), c.getString(R.string.action_wifi), getContent(c, s));
		}
		public static Spannable getContent(Context context, String s) {
			StringBuilder res = new StringBuilder();
			Map<String, String> tokens = parse(s.substring(5), FIELDS);
			for (int i = 0; i < FIELDS.length; i++) {
				if (tokens.get(FIELDS[i]) != null) {
					res.append(context.getString(FIELD_NAMES[i])).append(' ')
							.append(tokens.get(FIELDS[i])).append('\n');
				}
			}
			return spannable(res.toString());
		}

		public Intent getActionIntent() {
			String passwd = parse(rawContent.substring(5), FIELDS).get("P");
			if (passwd != null) {
				copyToClipboard(context, passwd);
			}
			return new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
		}
	}
}
