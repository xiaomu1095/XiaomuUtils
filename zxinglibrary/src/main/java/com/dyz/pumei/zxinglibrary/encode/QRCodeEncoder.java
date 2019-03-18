package com.dyz.pumei.zxinglibrary.encode;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;

import com.dyz.pumei.zxinglibrary.Contents;
import com.dyz.pumei.zxinglibrary.Intents;
import com.dyz.pumei.zxinglibrary.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * This class does the work of decoding the user's request and extracting all the data
 * to be encoded in a barcode.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
final class QRCodeEncoder {

  private static final String TAG = QRCodeEncoder.class.getSimpleName();

  private static final int WHITE = 0xFFFFFFFF;
  private static final int BLACK = 0xFF000000;

  private final Context activity;
  private String contents;
  private String displayContents;
  private String title;
  private BarcodeFormat format;
  private final int dimension;
  private final boolean useVCard;

  QRCodeEncoder(Context activity, Intent intent, int dimension, boolean useVCard) throws WriterException {
    this.activity = activity;
    this.dimension = dimension;
    this.useVCard = useVCard;
    String action = intent.getAction();
    if (Intents.Encode.ACTION.equals(action)) {
      encodeContentsFromZXingIntent(intent);
    }
  }

  String getContents() {
    return contents;
  }

  String getDisplayContents() {
    return displayContents;
  }

  String getTitle() {
    return title;
  }

  boolean isUseVCard() {
    return useVCard;
  }

  // It would be nice if the string encoding lived in the core ZXing library,
  // but we use platform specific code like PhoneNumberUtils, so it can't.
  private void encodeContentsFromZXingIntent(Intent intent) {
     // Default to QR_CODE if no format given.
    String formatString = intent.getStringExtra(Intents.Encode.FORMAT);
    format = null;
    if (formatString != null) {
      try {
        format = BarcodeFormat.valueOf(formatString);
      } catch (IllegalArgumentException iae) {
        // Ignore it then
      }
    }
    if (format == null || format == BarcodeFormat.QR_CODE) {
      String type = intent.getStringExtra(Intents.Encode.TYPE);
      if (type != null && !type.isEmpty()) {
        this.format = BarcodeFormat.QR_CODE;
        encodeQRCodeContents(intent, type);
      }
    } else {
      String data = intent.getStringExtra(Intents.Encode.DATA);
      if (data != null && !data.isEmpty()) {
        contents = data;
        displayContents = data;
        title = activity.getString(R.string.contents_text);
      }
    }
  }

  private void encodeFromTextExtras(Intent intent) throws WriterException {
    // Notice: Google Maps shares both URL and details in one text, bummer!
    String theContents = ContactEncoder.trim(intent.getStringExtra(Intent.EXTRA_TEXT));
    if (theContents == null) {
      theContents = ContactEncoder.trim(intent.getStringExtra("android.intent.extra.HTML_TEXT"));
      // Intent.EXTRA_HTML_TEXT
      if (theContents == null) {
        theContents = ContactEncoder.trim(intent.getStringExtra(Intent.EXTRA_SUBJECT));
        if (theContents == null) {
          String[] emails = intent.getStringArrayExtra(Intent.EXTRA_EMAIL);
          if (emails != null) {
            theContents = ContactEncoder.trim(emails[0]);
          } else {
            theContents = "?";
          }
        }
      }
    }

    // Trim text to avoid URL breaking.
    if (theContents == null || theContents.isEmpty()) {
      throw new WriterException("Empty EXTRA_TEXT");
    }
    contents = theContents;
    // We only do QR code.
    format = BarcodeFormat.QR_CODE;
    if (intent.hasExtra(Intent.EXTRA_SUBJECT)) {
      displayContents = intent.getStringExtra(Intent.EXTRA_SUBJECT);
    } else if (intent.hasExtra(Intent.EXTRA_TITLE)) {
      displayContents = intent.getStringExtra(Intent.EXTRA_TITLE);
    } else {
      displayContents = contents;
    }
    title = activity.getString(R.string.contents_text);
  }

  private void encodeQRCodeContents(Intent intent, String type) {
    switch (type) {
      case Contents.Type.TEXT:
        String textData = intent.getStringExtra(Intents.Encode.DATA);
        if (textData != null && !textData.isEmpty()) {
          contents = textData;
          displayContents = textData;
          title = activity.getString(R.string.contents_text);
        }
        break;

      case Contents.Type.EMAIL:
        String emailData = ContactEncoder.trim(intent.getStringExtra(Intents.Encode.DATA));
        if (emailData != null) {
          contents = "mailto:" + emailData;
          displayContents = emailData;
          title = activity.getString(R.string.contents_email);
        }
        break;

      case Contents.Type.PHONE:
        String phoneData = ContactEncoder.trim(intent.getStringExtra(Intents.Encode.DATA));
        if (phoneData != null) {
          contents = "tel:" + phoneData;
          displayContents = ContactEncoder.formatPhone(phoneData);
          title = activity.getString(R.string.contents_phone);
        }
        break;

      case Contents.Type.SMS:
        String smsData = ContactEncoder.trim(intent.getStringExtra(Intents.Encode.DATA));
        if (smsData != null) {
          contents = "sms:" + smsData;
          displayContents = ContactEncoder.formatPhone(smsData);
          title = activity.getString(R.string.contents_sms);
        }
        break;


      case Contents.Type.LOCATION:
        Bundle locationBundle = intent.getBundleExtra(Intents.Encode.DATA);
        if (locationBundle != null) {
          // These must use Bundle.getFloat(), not getDouble(), it's part of the API.
          float latitude = locationBundle.getFloat("LAT", Float.MAX_VALUE);
          float longitude = locationBundle.getFloat("LONG", Float.MAX_VALUE);
          if (latitude != Float.MAX_VALUE && longitude != Float.MAX_VALUE) {
            contents = "geo:" + latitude + ',' + longitude;
            displayContents = latitude + "," + longitude;
            title = activity.getString(R.string.contents_location);
          }
        }
        break;
    }
  }

  private static List<String> getAllBundleValues(Bundle bundle, String[] keys) {
    List<String> values = new ArrayList<>(keys.length);
    for (String key : keys) {
      Object value = bundle.get(key);
      values.add(value == null ? null : value.toString());
    }
    return values;
  }

  private static List<String> toList(String[] values) {
    return values == null ? null : Arrays.asList(values);
  }

  Bitmap encodeAsBitmap() throws WriterException {
    String contentsToEncode = contents;
    if (contentsToEncode == null) {
      return null;
    }
    Map<EncodeHintType,Object> hints = null;
    String encoding = guessAppropriateEncoding(contentsToEncode);
    if (encoding != null) {
      hints = new EnumMap<>(EncodeHintType.class);
      hints.put(EncodeHintType.CHARACTER_SET, encoding);
    }
    BitMatrix result;
    try {
      result = new MultiFormatWriter().encode(contentsToEncode, format, dimension, dimension, hints);
    } catch (IllegalArgumentException iae) {
      // Unsupported format
      return null;
    }
    int width = result.getWidth();
    int height = result.getHeight();
    int[] pixels = new int[width * height];
    for (int y = 0; y < height; y++) {
      int offset = y * width;
      for (int x = 0; x < width; x++) {
        pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
      }
    }

    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
    return bitmap;
  }

  private static String guessAppropriateEncoding(CharSequence contents) {
    // Very crude at the moment
    for (int i = 0; i < contents.length(); i++) {
      if (contents.charAt(i) > 0xFF) {
        return "UTF-8";
      }
    }
    return null;
  }

}
