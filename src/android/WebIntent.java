package nl.peterbierman.webintent;

import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Array;
import java.util.Arrays;

import org.apache.cordova.CordovaActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Html;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaResourceApi;
import org.apache.cordova.PluginResult;
import org.apache.cordova.CordovaActivity;

import android.content.ClipData;
import android.content.ContentResolver;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * WebIntent is a Cordova plugin that bridges Android intents and web
 * applications:
 *
 * 1. Web apps can spawn intents that call native Android applications.
 * 2. After setting up correct intent filters for Cordova applications,
 *    Android intents can be handled by Cordova web applications.
 *
 * @author Peter Bierman
 *
 */
public class WebIntent extends CordovaPlugin {

    private static final String LOG_TAG = "WebIntent";
    private CallbackContext onNewIntentCallbackContext = null;

    /**
     * @return true iff if the action was executed successfully, else false.
     */
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        try {
            if ("getIntent".equals(action)) {
                if (args.length() != 0) {
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                    return false;
                }
                Intent i = ((CordovaActivity)this.cordova.getActivity()).getIntent();
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, getIntentJson(i)));
                return true;
            } else if ("onNewIntent".equals(action)) {
                // Save reference to the callback; will be called on "new intent" events.
                this.onNewIntentCallbackContext = callbackContext;

                if (args.length() != 0) {
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                    return false;
                }

                PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
                result.setKeepCallback(true); // Reuse the callback on intent events.
                callbackContext.sendPluginResult(result);
                return true;
            } 
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
            return false;
        } catch (Exception e) {
            final String errorMessage = e.getMessage();
            Log.e(LOG_TAG, errorMessage);
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION, errorMessage));
            return false;
        }
    }

    @Override
    public void onNewIntent(Intent intent) {

        if (this.onNewIntentCallbackContext != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, getIntentJson(intent));
            result.setKeepCallback(true);
            this.onNewIntentCallbackContext.sendPluginResult(result);
        }
    }

    /**
     * Return JSON representation of intent attributes
     *
     * @param intent
     * Credit: https://github.com/napolitano/cordova-plugin-intent
     */
    private JSONObject getIntentJson(Intent intent) {
        JSONObject intentJSON = null;
        ClipData clipData = null;
        JSONObject[] items = null;
        ContentResolver cR = this.cordova.getActivity().getApplicationContext().getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            clipData = intent.getClipData();
            if(clipData != null) {
                int clipItemCount = clipData.getItemCount();
                items = new JSONObject[clipItemCount];

                for (int i = 0; i < clipItemCount; i++) {

                    ClipData.Item item = clipData.getItemAt(i);

                    try {
                        items[i] = new JSONObject();
                        items[i].put("htmlText", item.getHtmlText());
                        items[i].put("intent", item.getIntent());
                        items[i].put("text", item.getText());
                        items[i].put("uri", item.getUri());

                        if (item.getUri() != null) {
                            String type = cR.getType(item.getUri());
                            String extension = mime.getExtensionFromMimeType(cR.getType(item.getUri()));

                            items[i].put("type", type);
                            items[i].put("extension", extension);
                        }

                    } catch (Exception e) {
                        Log.d(LOG_TAG, " Error thrown during intent > JSON conversion");
                        Log.d(LOG_TAG, e.getMessage());
                        Log.d(LOG_TAG, Arrays.toString(e.getStackTrace()));
                    }

                }
            }
        }

        try {
            intentJSON = new JSONObject();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if(items != null) {
                    intentJSON.put("clipItems", new JSONArray(items));
                }
            }

            intentJSON.put("type", intent.getType());
            intentJSON.put("extras", toJsonObject(intent.getExtras()));
            intentJSON.put("action", intent.getAction());
            intentJSON.put("categories", intent.getCategories());
            intentJSON.put("flags", intent.getFlags());
            intentJSON.put("component", intent.getComponent());
            intentJSON.put("data", intent.getData());
            intentJSON.put("package", intent.getPackage());

            return intentJSON;
        } catch (Exception e) {
            Log.d(LOG_TAG, " Error thrown during intent > JSON conversion");
            Log.d(LOG_TAG, e.getMessage());
            Log.d(LOG_TAG, Arrays.toString(e.getStackTrace()));

            return null;
        }
    }

    private static JSONObject toJsonObject(Bundle bundle) {
        //  Credit: https://github.com/napolitano/cordova-plugin-intent
        try {
            return (JSONObject) toJsonValue(bundle);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot convert bundle to JSON: " + e.getMessage(), e);
        }
    }

    private static Object toJsonValue(final Object value) throws Exception {
        //  Credit: https://github.com/napolitano/cordova-plugin-intent
        if (value == null) {
            return null;
        } else if (value instanceof Bundle) {
            final Bundle bundle = (Bundle) value;
            final JSONObject result = new JSONObject();
            for (final String key : bundle.keySet()) {
                result.put(key, toJsonValue(bundle.get(key)));
            }
            return result;
        } else if (value.getClass().isArray()) {
            final JSONArray result = new JSONArray();
            int length = Array.getLength(value);
            for (int i = 0; i < length; ++i) {
                result.put(i, toJsonValue(Array.get(value, i)));
            }
            return result;
        } else if (
                value instanceof String
                        || value instanceof Boolean
                        || value instanceof Integer
                        || value instanceof Long
                        || value instanceof Double) {
            return value;
        } else {
            return String.valueOf(value);
        }
    }

}
