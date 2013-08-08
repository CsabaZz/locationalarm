
package com.bitknights.locationalarm.utils;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.Collator;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.bitknights.locationalarm.R;
import com.bitknights.locationalarm.StaticContextApplication;

public class Utils {
    public static final String TAG = "locationalarm-";

    public static final String LINK_TO_MARKETPLACE = "http://market.android.com/details?id=com.bitknights.locationalarm";

    public static ShadingTouchListener touchListener = new ShadingTouchListener();

    public static Typeface NormalTypeface;
    public static Typeface BoldTypeface;
    public static Typeface ItalicTypeface;

    public static void copyStream(InputStream is, OutputStream os) {
        final int buffer_size = 1024;
        try {
            byte[] bytes = new byte[buffer_size];
            int count = is.read(bytes, 0, buffer_size);
            while (count > -1) {
                os.write(bytes, 0, count);
                count = is.read(bytes, 0, buffer_size);
            }
            os.flush();
        } catch (Exception ex) {
            Log.e(TAG, ex.getLocalizedMessage());
            ex.printStackTrace();
        }
    }

    public static boolean isOnline() {
        final Context context = StaticContextApplication.getAppContext();
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni_w = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo ni_m = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        return (ni_w != null && ni_w.isConnected()) || (ni_m != null && ni_m.isConnected());
    }

    public static int getDip(int value) {
        final Context context = StaticContextApplication.getAppContext();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context
                .getResources()
                .getDisplayMetrics());
    }

    public static float getDip(float value) {
        final Context context = StaticContextApplication.getAppContext();
        return TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context.getResources()
                        .getDisplayMetrics());
    }

    public static int getSip(int value) {
        final Context context = StaticContextApplication.getAppContext();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, context
                .getResources()
                .getDisplayMetrics());
    }

    public static float getSip(float value) {
        final Context context = StaticContextApplication.getAppContext();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, context.getResources()
                .getDisplayMetrics());
    }

    @SuppressWarnings("deprecation")
    public static void setWindow(Window window) {
        window.addFlags(WindowManager.LayoutParams.FLAG_DITHER);
        window.setFormat(PixelFormat.RGBA_8888);
    }

    public static void hideKeyboard() {
        final Activity activity = StaticContextApplication.getCurrentActivityContext();
        if (activity.getCurrentFocus() == null) {
            return;
        }

        InputMethodManager inputManager = (InputMethodManager) activity
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);
    }

    /**
     * @param s first string
     * @param t second string
     * @param caseSensitive treat characters differing in case only as equal -
     *            will be ignored if a collator is given
     * @param collator used to compare subwords that aren't numbers - if null,
     *            characters will be compared individually based on their
     *            Unicode value
     * @return zero if <code>s</code> and <code>t</code> are equal, a value less
     *         than zero if <code>s</code> lexicographically precedes
     *         <code>t</code> and a value larger than zero if <code>s</code>
     *         lexicographically follows <code>t</code>
     */
    public static int compareNatural(String s, String t, boolean caseSensitive, Collator collator) {
        int sIndex = 0;
        int tIndex = 0;

        if (s == null && t == null) {
            return 0;
        } else if (s == null) {
            return -1;
        } else if (t == null) {
            return 1;
        }

        int sLength = s.length();
        int tLength = t.length();

        while (true) {
            // both character indices are after a subword (or at zero)

            // Check if one string is at end
            if (sIndex == sLength && tIndex == tLength) {
                return 0;
            }
            if (sIndex == sLength) {
                return -1;
            }
            if (tIndex == tLength) {
                return 1;
            }

            // Compare sub word
            char sChar = s.charAt(sIndex);
            char tChar = t.charAt(tIndex);

            boolean sCharIsDigit = Character.isDigit(sChar);
            boolean tCharIsDigit = Character.isDigit(tChar);

            if (sCharIsDigit && tCharIsDigit) {
                // Compare numbers

                // skip leading 0s
                int sLeadingZeroCount = 0;
                while (sChar == '0') {
                    ++sLeadingZeroCount;
                    ++sIndex;
                    if (sIndex == sLength) {
                        break;
                    }
                    sChar = s.charAt(sIndex);
                }
                int tLeadingZeroCount = 0;
                while (tChar == '0') {
                    ++tLeadingZeroCount;
                    ++tIndex;
                    if (tIndex == tLength) {
                        break;
                    }
                    tChar = t.charAt(tIndex);
                }
                boolean sAllZero = sIndex == sLength || !Character.isDigit(sChar);
                boolean tAllZero = tIndex == tLength || !Character.isDigit(tChar);
                if (sAllZero && tAllZero) {
                    continue;
                }
                if (sAllZero && !tAllZero) {
                    return -1;
                }
                if (tAllZero) {
                    return 1;
                }

                int diff = 0;
                do {
                    if (diff == 0) {
                        diff = sChar - tChar;
                    }
                    ++sIndex;
                    ++tIndex;
                    if (sIndex == sLength && tIndex == tLength) {
                        return diff != 0 ? diff : sLeadingZeroCount - tLeadingZeroCount;
                    }
                    if (sIndex == sLength) {
                        if (diff == 0) {
                            return -1;
                        }
                        return Character.isDigit(t.charAt(tIndex)) ? -1 : diff;
                    }
                    if (tIndex == tLength) {
                        if (diff == 0) {
                            return 1;
                        }
                        return Character.isDigit(s.charAt(sIndex)) ? 1 : diff;
                    }
                    sChar = s.charAt(sIndex);
                    tChar = t.charAt(tIndex);
                    sCharIsDigit = Character.isDigit(sChar);
                    tCharIsDigit = Character.isDigit(tChar);
                    if (!sCharIsDigit && !tCharIsDigit) {
                        // both number sub words have the same length
                        if (diff != 0) {
                            return diff;
                        }
                        break;
                    }
                    if (!sCharIsDigit) {
                        return -1;
                    }
                    if (!tCharIsDigit) {
                        return 1;
                    }
                } while (true);
            } else {
                // Compare words
                if (collator != null) {
                    // To use the collator the whole subwords have to be
                    // compared - character-by-character comparision
                    // is not possible. So find the two subwords first
                    int aw = sIndex;
                    int bw = tIndex;
                    do {
                        ++sIndex;
                    } while (sIndex < sLength && !Character.isDigit(s.charAt(sIndex)));
                    do {
                        ++tIndex;
                    } while (tIndex < tLength && !Character.isDigit(t.charAt(tIndex)));

                    String as = s.substring(aw, sIndex);
                    String bs = t.substring(bw, tIndex);
                    int subwordResult = collator.compare(as, bs);
                    if (subwordResult != 0) {
                        return subwordResult;
                    }
                } else {
                    // No collator specified. All characters should be ascii
                    // only. Compare character-by-character.
                    do {
                        if (sChar != tChar) {
                            if (caseSensitive) {
                                return sChar - tChar;
                            }
                            sChar = Character.toUpperCase(sChar);
                            tChar = Character.toUpperCase(tChar);
                            if (sChar != tChar) {
                                sChar = Character.toLowerCase(sChar);
                                tChar = Character.toLowerCase(tChar);
                                if (sChar != tChar) {
                                    return sChar - tChar;
                                }
                            }
                        }
                        ++sIndex;
                        ++tIndex;
                        if (sIndex == sLength && tIndex == tLength) {
                            return 0;
                        }
                        if (sIndex == sLength) {
                            return -1;
                        }
                        if (tIndex == tLength) {
                            return 1;
                        }
                        sChar = s.charAt(sIndex);
                        tChar = t.charAt(tIndex);
                        sCharIsDigit = Character.isDigit(sChar);
                        tCharIsDigit = Character.isDigit(tChar);
                    } while (!sCharIsDigit && !tCharIsDigit);
                }
            }
        }
    }

    public static ArrayList<String> pullLinks(String text) {
        ArrayList<String> links = new ArrayList<String>();

        String normalPattern = "\\(?\\b(http://|www[.])[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_()|]";
        String codedPattern = "\\(?\\b(http:\\/\\/|www[.])[-A-Za-z0-9+&@#\\/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#\\/%=~_()|]";

        addUrls(text, normalPattern, links);
        addUrls(text, codedPattern, links);

        return links;
    }

    private static void addUrls(String text, String regex, ArrayList<String> links) {
        if (TextUtils.isEmpty(text)) {
            return;
        }

        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(text);

        while (m.find()) {
            String urlStr = m.group();

            if (urlStr.startsWith("(") && urlStr.endsWith(")")) {
                urlStr = urlStr.substring(1, urlStr.length() - 1);
            }

            links.add(urlStr);
        }
    }

    public static int getScreenWidth() {
        final Resources resources = StaticContextApplication.getStaticResources();
        DisplayMetrics dm = resources.getDisplayMetrics();
        return dm.widthPixels;
    }

    public static void showNoConnectionDialog() {
        final Activity activity = StaticContextApplication.getCurrentActivityContext();
        if (activity == null || activity.isFinishing()) {
            return;
        }

        AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
        dialog.setCancelable(false);
        dialog.setTitle(R.string.dialogWarningTitle);
        dialog.setMessage(R.string.dialogNoConnectionText);
        dialog.setPositiveButton(android.R.string.ok, null);
        dialog.show();
    }

    public static void setTypeface(TextView textview, Typeface typeface) {
        textview.setPaintFlags(textview.getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG);
        textview.setTypeface(typeface);
    }

    public static void setTypeface(TextView textview, Typeface typeface, int style) {
        textview.setPaintFlags(textview.getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG);
        textview.setTypeface(typeface, style);
    }

}
