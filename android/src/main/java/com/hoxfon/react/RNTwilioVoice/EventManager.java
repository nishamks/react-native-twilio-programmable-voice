package com.hoxfon.react.RNTwilioVoice;

import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import static com.hoxfon.react.RNTwilioVoice.TwilioVoiceModule.TAG;

public class EventManager {

  private ReactApplicationContext mContext;

  public static final String EVENT_DEVICE_READY = "deviceReady";
  public static final String EVENT_DEVICE_NOT_READY = "deviceNotReady";

  public EventManager(ReactApplicationContext context) {
    mContext = context;
  }

  public void sendEvent(String eventName, @Nullable WritableMap params) {
    if (BuildConfig.DEBUG) {
      Log.d(TAG, "sendEvent "+eventName+" params "+params);
    }
    if (mContext.hasActiveCatalystInstance()) {
      mContext
          .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
          .emit(eventName, params);
    } else {
      if (BuildConfig.DEBUG) {
        Log.d(TAG, "failed Catalyst instance not active");
      }
    }
  }
}