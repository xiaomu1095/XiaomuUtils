/*
 * Copyright 2009 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xiaomu.zxinglib.integration;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;

import com.xiaomu.zxinglib.CaptureActivity;
import com.xiaomu.zxinglib.Intents;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>A utility class which helps ease integration with Barcode Scanner via {@link Intent}s. This is a simple
 * way to invoke barcode scanning and receive the result, without any need to integrate, modify, or learn the
 * project's source code.</p>
 *
 * <h2>Initiating a barcode scan</h2>
 *
 * <p>To integrate, create an instance of {@code IntentIntegrator} and call {@link #initiateScan()} and wait
 * for the result in your app.</p>
 *
 * <p>It does require that the Barcode Scanner (or work-alike) application is installed. The
 * {@link #initiateScan()} method will prompt the user to download the application, if needed.</p>
 *
 * <p>There are a few steps to using this integration. First, your {@link Activity} must implement
 * the method {@link Activity#onActivityResult(int, int, Intent)} and include a line of code like this:</p>
 *
 * <pre>{@code
 * public void onActivityResult(int requestCode, int resultCode, Intent intent) {
 *   IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
 *   if (scanResult != null) {
 *     // handle scan result
 *   }
 *   // else continue with any other code you need in the method
 *   ...
 * }
 * }</pre>
 *
 * <p>This is where you will handle a scan result.</p>
 *
 * <p>Second, just call this in response to a user action somewhere to begin the scan process:</p>
 *
 * <pre>{@code
 * IntentIntegrator integrator = new IntentIntegrator(yourActivity);
 * integrator.initiateScan();
 * }</pre>
 *
 * <p>Note that {@link #initiateScan()} returns an {@link AlertDialog} which is non-null if the
 * user was prompted to download the application. This lets the calling app potentially manage the dialog.
 * In particular, ideally, the app dismisses the dialog if it's still active in its {@link Activity#onPause()}
 * method.</p>
 * 
 * <p>Finally, you can use {@link #addExtra(String, Object)} to add more parameters to the Intent used
 * to invoke the scanner. This can be used to set additional options not directly exposed by this
 * simplified API.</p>
 * 
 * <p>By default, this will only allow applications that are known to respond to this intent correctly
 * do so. The apps that are allowed to response can be set with {@link #setTargetApplications(List)}.
 * For example, set to {@link #TARGET_BARCODE_SCANNER_ONLY} to only target the Barcode Scanner app itself.</p>
 *
 * <h2>Enabling experimental barcode formats</h2>
 *
 * <p>Some formats are not enabled by default even when scanning with {@link #ALL_CODE_TYPES}, such as
 * PDF417. Use {@link #initiateScan(Collection)} with
 * a collection containing the names of formats to scan for explicitly, like "PDF_417", to use such
 * formats.</p>
 *
 * @author Sean Owen
 * @author Fred Lin
 * @author Isaac Potoczny-Jones
 * @author Brad Drehmer
 * @author gcstang
 */
public class IntentIntegrator {

  public static final int REQUEST_CODE = 0x0000c0de; // Only use bottom 16 bits
  private static final String TAG = IntentIntegrator.class.getSimpleName();

  private static final String BS_PACKAGE = "com.xiaomu.zxinglib";
  private static final String BSPLUS_PACKAGE = "com.dyz.pumei.xiaomuutilapplication";

  // supported barcode formats
  public static final Collection<String> PRODUCT_CODE_TYPES = list("UPC_A", "UPC_E", "EAN_8", "EAN_13", "RSS_14");
  public static final Collection<String> ONE_D_CODE_TYPES =
      list("UPC_A", "UPC_E", "EAN_8", "EAN_13", "CODE_39", "CODE_93", "CODE_128",
           "ITF", "RSS_14", "RSS_EXPANDED");
  public static final Collection<String> QR_CODE_TYPES = Collections.singleton("QR_CODE");
  public static final Collection<String> DATA_MATRIX_TYPES = Collections.singleton("DATA_MATRIX");

  public static final Collection<String> ALL_CODE_TYPES = null;
  
  public static final List<String> TARGET_BARCODE_SCANNER_ONLY = Collections.singletonList(BS_PACKAGE);
  public static final List<String> TARGET_ALL_KNOWN = list(
          BSPLUS_PACKAGE,             // Barcode Scanner+
          BS_PACKAGE                  // Barcode Scanner
      );

  // Should be FLAG_ACTIVITY_NEW_DOCUMENT in API 21+.
  // Defined once here because the current value is deprecated, so generates just one warning
  private static final int FLAG_NEW_DOC = Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET;
  
  private final Activity activity;
  private final Fragment fragment;

  private List<String> targetApplications;
  private final Map<String,Object> moreExtras = new HashMap<String,Object>(3);

  private Class<?> captureActivity;
  /**
   * @param activity {@link Activity} invoking the integration
   */
  public IntentIntegrator(Activity activity) {
    this.activity = activity;
    this.fragment = null;
    initializeConfiguration();
  }

  /**
   * @param fragment {@link Fragment} invoking the integration.
   *  {@link #startActivityForResult(Intent, int)} will be called on the {@link Fragment} instead
   *  of an {@link Activity}
   */
  public IntentIntegrator(Fragment fragment) {
    this.activity = fragment.getActivity();
    this.fragment = fragment;
    initializeConfiguration();
  }

  private void initializeConfiguration() {
    targetApplications = TARGET_ALL_KNOWN;
  }
  
  public Collection<String> getTargetApplications() {
    return targetApplications;
  }
  
  public final void setTargetApplications(List<String> targetApplications) {
    if (targetApplications.isEmpty()) {
      throw new IllegalArgumentException("No target applications");
    }
    this.targetApplications = targetApplications;
  }
  
  public void setSingleTargetApplication(String targetApplication) {
    this.targetApplications = Collections.singletonList(targetApplication);
  }

  public Map<String,?> getMoreExtras() {
    return moreExtras;
  }

  public final void addExtra(String key, Object value) {
    moreExtras.put(key, value);
  }

  /**
   * Initiates a scan for all known barcode types with the default camera.
   *
   * @return the {@link AlertDialog} that was shown to the user prompting them to download the app
   *   if a prompt was needed, or null otherwise.
   */
  public final AlertDialog initiateScan() {
    return initiateScan(ALL_CODE_TYPES, -1);
  }
  
  /**
   * Initiates a scan for all known barcode types with the specified camera.
   *
   * @param cameraId camera ID of the camera to use. A negative value means "no preference".
   * @return the {@link AlertDialog} that was shown to the user prompting them to download the app
   *   if a prompt was needed, or null otherwise.
   */
  public final AlertDialog initiateScan(int cameraId) {
    return initiateScan(ALL_CODE_TYPES, cameraId);
  }

  /**
   * Initiates a scan, using the default camera, only for a certain set of barcode types, given as strings corresponding
   * to their names in ZXing's {@code BarcodeFormat} class like "UPC_A". You can supply constants
   * like {@link #PRODUCT_CODE_TYPES} for example.
   *
   * @param desiredBarcodeFormats names of {@code BarcodeFormat}s to scan for
   * @return the {@link AlertDialog} that was shown to the user prompting them to download the app
   *   if a prompt was needed, or null otherwise.
   */
  public final AlertDialog initiateScan(Collection<String> desiredBarcodeFormats) {
    return initiateScan(desiredBarcodeFormats, -1);
  }
  
  /**
   * Initiates a scan, using the specified camera, only for a certain set of barcode types, given as strings corresponding
   * to their names in ZXing's {@code BarcodeFormat} class like "UPC_A". You can supply constants
   * like {@link #PRODUCT_CODE_TYPES} for example.
   *
   * @param desiredBarcodeFormats names of {@code BarcodeFormat}s to scan for
   * @param cameraId camera ID of the camera to use. A negative value means "no preference".
   * @return the {@link AlertDialog} that was shown to the user prompting them to download the app
   *   if a prompt was needed, or null otherwise
   */
  public final AlertDialog initiateScan(Collection<String> desiredBarcodeFormats, int cameraId) {
    Intent intentScan = new Intent(activity, getCaptureActivity());
    intentScan.setAction(Intents.Scan.ACTION);
    intentScan.addCategory(Intent.CATEGORY_DEFAULT);

    // check which types of codes to scan for
    if (desiredBarcodeFormats != null) {
      // set the desired barcode types
      StringBuilder joinedByComma = new StringBuilder();
      for (String format : desiredBarcodeFormats) {
        if (joinedByComma.length() > 0) {
          joinedByComma.append(',');
        }
        joinedByComma.append(format);
      }
      intentScan.putExtra(Intents.Scan.FORMATS, joinedByComma.toString());
    }

    // check requested camera ID
    if (cameraId >= 0) {
      intentScan.putExtra(Intents.Scan.CAMERA_ID, cameraId);
    }

    intentScan.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    intentScan.addFlags(FLAG_NEW_DOC);
    attachMoreExtras(intentScan);
    startActivityForResult(intentScan, REQUEST_CODE);
    return null;
  }

  protected Class<?> getDefaultCaptureActivity() {
    return CaptureActivity.class;
  }

  public Class<?> getCaptureActivity() {
    if (captureActivity == null) {
      captureActivity = getDefaultCaptureActivity();
    }
    return captureActivity;
  }

  /**
   * Start an activity. This method is defined to allow different methods of activity starting for
   * newer versions of Android and for compatibility library.
   *
   * @param intent Intent to start.
   * @param code Request code for the activity
   * @see Activity#startActivityForResult(Intent, int)
   * @see Fragment#startActivityForResult(Intent, int)
   */
  protected void startActivityForResult(Intent intent, int code) {
    if (fragment == null) {
      activity.startActivityForResult(intent, code);
    } else {
      fragment.startActivityForResult(intent, code);
    }
  }
  
  /**
   * <p>Call this from your {@link Activity}'s
   * {@link Activity#onActivityResult(int, int, Intent)} method.</p>
   *
   * @param requestCode request code from {@code onActivityResult()}
   * @param resultCode result code from {@code onActivityResult()}
   * @param intent {@link Intent} from {@code onActivityResult()}
   * @return null if the event handled here was not related to this class, or
   *  else an {@link IntentResult} containing the result of the scan. If the user cancelled scanning,
   *  the fields will be null.
   */
  public static IntentResult parseActivityResult(int requestCode, int resultCode, Intent intent) {
    if (requestCode == REQUEST_CODE) {
      if (resultCode == Activity.RESULT_OK) {
        String contents = intent.getStringExtra(Intents.Scan.RESULT);
        String formatName = intent.getStringExtra(Intents.Scan.RESULT_FORMAT);
        byte[] rawBytes = intent.getByteArrayExtra(Intents.Scan.RESULT_BYTES);
        int intentOrientation = intent.getIntExtra(Intents.Scan.RESULT_ORIENTATION, Integer.MIN_VALUE);
        Integer orientation = intentOrientation == Integer.MIN_VALUE ? null : intentOrientation;
        String errorCorrectionLevel = intent.getStringExtra(Intents.Scan.RESULT_ERROR_CORRECTION_LEVEL);
        return new IntentResult(contents,
                                formatName,
                                rawBytes,
                                orientation,
                                errorCorrectionLevel);
      }
      return new IntentResult();
    }
    return null;
  }

  private static List<String> list(String... values) {
    return Collections.unmodifiableList(Arrays.asList(values));
  }

  private void attachMoreExtras(Intent intent) {
    for (Map.Entry<String,Object> entry : moreExtras.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      // Kind of hacky
      if (value instanceof Integer) {
        intent.putExtra(key, (Integer) value);
      } else if (value instanceof Long) {
        intent.putExtra(key, (Long) value);
      } else if (value instanceof Boolean) {
        intent.putExtra(key, (Boolean) value);
      } else if (value instanceof Double) {
        intent.putExtra(key, (Double) value);
      } else if (value instanceof Float) {
        intent.putExtra(key, (Float) value);
      } else if (value instanceof Bundle) {
        intent.putExtra(key, (Bundle) value);
      } else {
        intent.putExtra(key, value.toString());
      }
    }
  }

}
