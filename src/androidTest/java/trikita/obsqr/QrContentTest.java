package trikita.obsqr;

import android.test.AndroidTestCase;

import java.util.HashMap;
import java.util.Map;

public class QrContentTest extends AndroidTestCase {

	public void testBrokenRegexp() {
		String s = "http://qrs.ly/z24icxy";
		assertFalse(s.matches(QrContent.GooglePlayContent.MATCH));
		// For some reason, Moto G's String.match() caused ANR on this string
		s = "https://play.google.com/store/apps/details?id=com.mvl.ThunderValley";
		assertFalse(s.matches(QrContent.GooglePlayContent.MATCH));
		assertTrue(s.matches(QrContent.WebUrlContent.MATCH));
	}

	public void testGooglePlayMatcher() {
		for (Map.Entry<String, String> e : new HashMap<String, String>() {{
			put("market://details?id=com.example", "com.example");
			put("market://details?id=foo", "foo");
			put("market://details.id.foo", "market://details.id.foo");
			put("market://search?pub=trikita", "market://search?pub=trikita");
			put("market://search?q=Some+Query&c=apps", "market://search?q=Some+Query&c=apps");
			put("market://apps/collections/editors_choice", "market://apps/collections/editors_choice");
		}}.entrySet()) {
			QrContent qr = QrContent.from(getContext(), e.getKey());
			assertEquals(QrContent.GooglePlayContent.class, qr.getClass());
			assertEquals(e.getValue(), qr.content.toString());
		}
	}

	public void testUrlMatcher() {
		for (Map.Entry<String, String> e : new HashMap<String, String>() {{
			put("http://example.com", "http://example.com");
			put("https://example.com", "https://example.com");
			put("ftp://example.com", null);
			put("example.com", "http://example.com");
			put("link to http://example.com", null);
		}}.entrySet()) {
			QrContent qr = QrContent.from(getContext(), e.getKey());
			if (e.getValue() != null) {
				assertEquals(QrContent.WebUrlContent.class, qr.getClass());
				assertEquals(e.getValue(), qr.content.toString());
			} else {
				assertFalse(qr instanceof QrContent.WebUrlContent);
			}
		}
	}

	public void testEmailMatcher() {
		for (Map.Entry<String, String> e : new HashMap<String, String>() {{
			put("mailto:johndoe@example.com", "johndoe@example.com");
			put("MAILTO:johndoe@example.com", "johndoe@example.com");
			put("MAILTO:johndoe@example.com?subject=Hello+world", "johndoe@example.com");
		}}.entrySet()) {
			QrContent qr = QrContent.from(getContext(), e.getKey());
			if (e.getValue() != null) {
				assertEquals(QrContent.EmailContent.class, qr.getClass());
				assertEquals(e.getValue(), qr.content.toString());
			} else {
				assertFalse(qr instanceof QrContent.EmailContent);
			}
		}
	}

	public void testSmsMatcher() {
		QrContent content;
		content = QrContent.from(getContext(), "smsto:+123456789");
		content = QrContent.from(getContext(), "smsto:+18554407400:I am interested in using Scanova");
		// SMSTO
		assertEquals(QrContent.SmsContent.class, content.getClass());
	}

	public void testPhoneNumberMatcher() {
		QrContent content;
		content = QrContent.from(getContext(), "tel:+123456789");
		// TEL:
		assertTrue(content instanceof QrContent.PhoneNumberContent);
	}

	public void testWifiMatcher() {
		QrContent content;
		content = QrContent.from(getContext(), "WIFI:S:Example;T:WPA;P:example123;;");
		assertEquals(QrContent.WifiContent.class, content.getClass());
	}

	public void testContactMatcher() {
		QrContent content;
		// TODO lots of other examples
		content = QrContent.from(getContext(), "MECARD:N:John Doe;EMAIL:john@example.com;;");
		assertEquals(QrContent.ContactContent.class, content.getClass());
	}

	public void testGeolocationMatcher() {
		QrContent content;
		content = QrContent.from(getContext(), "geo:0,0");
		assertEquals(QrContent.GeoLocationContent.class, content.getClass());
	}
}
