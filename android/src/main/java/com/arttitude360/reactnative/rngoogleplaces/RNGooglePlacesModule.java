package com.arttitude360.reactnative.rngoogleplaces;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.app.Activity;
import android.net.Uri;
import android.text.TextUtils;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

import java.util.ArrayList;
import java.util.List;

public class RNGooglePlacesModule extends ReactContextBaseJavaModule implements ActivityEventListener {

	private ReactApplicationContext reactContext;
    private Promise pendingPromise;
    public static final String TAG = "RNGooglePlaces";

    public static int AUTOCOMPLETE_REQUEST_CODE = 360;
    public static String REACT_CLASS = "RNGooglePlaces";

    public RNGooglePlacesModule(ReactApplicationContext reactContext) {
    	super(reactContext);

    	this.reactContext = reactContext;
    	this.reactContext.addActivityEventListener(this);
    }

    @Override
    public String getName() {
        return REACT_CLASS;
    }


    /**
     * Called after the autocomplete activity has finished to return its result.
     */
    @Override
    public void onActivityResult(Activity activity, final int requestCode, final int resultCode, final Intent data) {

        // Check that the result was from the autocomplete widget.
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                // Get the user's selected place from the Intent.
                Place place = PlaceAutocomplete.getPlace(this.reactContext.getApplicationContext(), data);
                Log.i(TAG, "Place Selected: " + place.getName());

                // Display attributions if required.
                CharSequence attributions = place.getAttributions();

                WritableMap map = Arguments.createMap();
                map.putDouble("latitude", place.getLatLng().latitude);
                map.putDouble("longitude", place.getLatLng().longitude);
                map.putString("name", place.getName().toString());
                map.putString("address", place.getAddress().toString());
                
                if (!TextUtils.isEmpty(place.getPhoneNumber())) {
                	map.putString("phoneNumber", place.getPhoneNumber().toString());
                }
                if (null != place.getWebsiteUri()) {
                	map.putString("website", place.getWebsiteUri().toString());
                }
                map.putString("placeID", place.getId());
                if (!TextUtils.isEmpty(attributions)) {
                	map.putString("attributions", attributions.toString());
                }      

				resolvePromise(map);

            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this.reactContext.getApplicationContext(), data);
                Log.e(TAG, "Error: Status = " + status.toString());
                rejectPromise("error", new Error(status.toString()));
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // Indicates that the activity closed before a selection was made. For example if
                // the user pressed the back button.
                rejectPromise("cancel", new Error("Search cancelled"));
            }
        }
    }

    /**
     * Exposed React's methods
     */

    @ReactMethod
    public void openAutocompleteModal(final Promise promise) {
        
        this.pendingPromise = promise;
        Activity currentActivity = getCurrentActivity();

        try {
            // The autocomplete activity requires Google Play Services to be available. The intent
            // builder checks this and throws an exception if it is not the case.
            Intent intent = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                    .build(currentActivity);
            currentActivity.startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
        } catch (GooglePlayServicesRepairableException e) {
            // Indicates that Google Play Services is either not installed or not up to date. Prompt
            // the user to correct the issue.
            GoogleApiAvailability.getInstance().getErrorDialog(currentActivity, e.getConnectionStatusCode(),
                    0 /* requestCode */).show();
        } catch (GooglePlayServicesNotAvailableException e) {
            // Indicates that Google Play Services is not available and the problem is not easily
            // resolvable.
            String message = "Google Play Services is not available: " +
                    GoogleApiAvailability.getInstance().getErrorString(e.errorCode);

            Log.e(TAG, message);
        }
    }

    private void rejectPromise(String code, Error err) {
        if (this.pendingPromise != null) {
            this.pendingPromise.reject(code, err);
            this.pendingPromise = null;
        }
    }

    private void resolvePromise(Object data) {
        if (this.pendingPromise != null) {
            this.pendingPromise.resolve(data);
            this.pendingPromise = null;
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
    }  
}