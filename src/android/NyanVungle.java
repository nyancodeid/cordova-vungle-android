package id.nyandev.cordova.vungle.android;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;

import com.vungle.warren.Vungle;
import com.vungle.warren.AdConfig;              // Custom ad configurations
import com.vungle.warren.InitCallback;          // Initialization callback
import com.vungle.warren.LoadAdCallback;        // Load ad callback
import com.vungle.warren.PlayAdCallback;        // Play ad callback
import com.vungle.warren.VungleNativeAd;        // In-Feed and MREC ad
import com.vungle.warren.Vungle.Consent;        // GDPR consent
import com.vungle.warren.VungleSettings;         // Minimum disk space
import com.vungle.warren.error.VungleException; // onError message

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import android.util.Log;

/**
 * This class echoes a string called from JavaScript.
 */
public class NyanVungle extends CordovaPlugin {
  private final String LOG_TAG = "NyanVungle";
  protected String appId;
  protected CallbackContext callbackContextGlobal;
  protected CallbackContext callbackContextTemp;
  protected boolean isSendEvent = false;

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    if (action.equals("initialize")) {
      String appId = args.getString(0);
      this.initializeSDK(appId, callbackContext);
      return true;
    } else if (action.equals("load")) {
      String placementId = args.getString(0);

      this.loadAd(placementId, callbackContext);
      return true;
    } else if (action.equals("isLoaded")) {
      String placementId = args.getString(0);

      this.isLoaded(placementId, callbackContext);
      return true;
    } else if (action.equals("show")) {
      String placementId = args.getString(0);

      this.showAd(placementId, callbackContext);
      return true;
    } else if (action.equals("setCustomRewardedFields")) {
      String name = args.getString(0);
      String title = args.getString(1);
      String body = args.getString(2);
 
      this.setCustomRewardedFields(name, title, body, callbackContext);
    } else if (action.equals("getPlacements")) {
 
      this.getValidPlacements(callbackContext);
    }
    return false;
  }

  private void initializeSDK(String appId, CallbackContext callbackContext) {
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        _initializeVungle(appId, callbackContext);
      }
    });
  }
  private void loadAd(String placementId, CallbackContext callbackContext) {
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        _loadAd(placementId, callbackContext);
      }
    });
  }
  private void isLoaded(String placementId, CallbackContext callbackContext) {
    boolean isCan = Vungle.canPlayAd(placementId);
    JSONObject response = new JSONObject();

    try {
      response.put("placementId", placementId);
      response.put("isLoaded", isCan);
    } catch(JSONException e) {
      callbackContext.error("isLoaded E:" + e.getMessage());
    } 

    callbackContext.success(response);
  }
  private void showAd(String placementId, CallbackContext callbackContext) {
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        _showAd(placementId, callbackContext);
      }
    });
  }
  private void setCustomRewardedFields(String user, String title, String body, CallbackContext callbackContext) {
    Vungle.setIncentivizedFields(
      ((user == null) ? "RewardedUser" : user), 
      ((title == null) ? "RewardedTitle" : title), 
      ((body == null) ? "RewardedBody" : body), 
      "RewardedKeepWatching", 
      "RewardedClose");

    callbackContext.success();
  }
  private void getValidPlacements(CallbackContext callbackContext) {
    Collection<String> placements = Vungle.getValidPlacements();
    JSONArray placementsArray = new JSONArray();

    for (String s : placements) {
      placementsArray.put(s);
    }

    callbackContext.success(placementsArray);
  }

  private void _initializeVungle(String appId, CallbackContext callbackContext) {
    this.appId = appId;

    final long MEGABYTE = 1024L * 1024L;
    final VungleSettings vungleSettings =
      new VungleSettings.Builder()
        .setMinimumSpaceForAd(50 * MEGABYTE)
        .setMinimumSpaceForInit(51 * MEGABYTE)
        .build();

    callbackContextGlobal = callbackContext;

    Vungle.init(appId, cordova.getActivity().getApplicationContext(), new InitCallback() {
      @Override
      public void onSuccess() {
        JSONObject response = new JSONObject();
        try {
          response.put("success", true);
          response.put("appId", appId);
        } catch (JSONException e) {
          _sendExceptionResponse(e);
        }
        _sendResponse(response);
      }

      @Override
      public void onError(Throwable throwable) {
        JSONObject response = new JSONObject();
        try {
          response.put("success", false);
          response.put("appId", appId);
          response.put("error", throwable.getLocalizedMessage());
        } catch (JSONException e) {
          _sendExceptionResponse(e);
        }
        _sendExceptionResponse(throwable);
      }
    
      @Override
      public void onAutoCacheAdAvailable(String placementId) {
        
      }
    }, vungleSettings);
  }
  private void _loadAd(String placementId, CallbackContext callbackContext) {
    isSendEvent = false;

    if (Vungle.isInitialized()) {
      try {
        Vungle.loadAd(placementId, new LoadAdCallback() {
          @Override
          public void onAdLoad(String placementReferenceId) {
              // Placement reference ID for the placement to load ad assets
              if (!isSendEvent) {
                isSendEvent = true;
                callbackContext.success();
              } else {
                isSendEvent = false;

                JSONObject response = new JSONObject();
                JSONObject responseData = new JSONObject();

                try {
                  responseData.put("placementId", placementReferenceId);
                  response.put("isEvent", true);
                  response.put("eventName", "onAdLoaded");
                  response.put("data", responseData);
                } catch(JSONException e) { }

                _sendResponse(response);
              }
          }

          @Override
          public void onError(String placementReferenceId, Throwable throwable) {
              // Placement reference ID for the placement that failed to download ad assets
              // Throwable contains error message
              callbackContext.error("loadAd E: " + throwable.getLocalizedMessage());
          }
        });
      } catch(final Exception e) {
        callbackContext.error("Vungle.loadAd E: " + e.getMessage());
      }
    } else {
      callbackContext.error("LoadAd E: Ads not initialized");
    }

    callbackContext.success();
  }
  private void _showAd(String placementId, CallbackContext callbackContext) {
    AdConfig adConfig = new AdConfig();
    adConfig.setAutoRotate(true);
    adConfig.setMuted(true);

    callbackContextTemp = callbackContext;

    if (Vungle.canPlayAd(placementId)) {
      try {
        Vungle.playAd(placementId, adConfig, vunglePlayAdCallback);
      } catch(final Exception e) {
        callbackContext.error("Vungle.playAd E:" + e.getMessage());
      }
    } else {
      callbackContext.error("showAd E: ads not loaded");
    }

    callbackContext.success();
  }
  private void _sendExceptionResponse(Exception e) {
    JSONObject exception = new JSONObject();
    try {
      exception.put("success", false);
      exception.put("error", e.getMessage());
    } catch(JSONException err) {
      Log.d(LOG_TAG, "sendExecptionResponse E:" + err.getMessage());	
    }

    this._sendResponse(exception);
  }
  private void _sendExceptionResponse(JSONException e) {
    JSONObject exception = new JSONObject();
    try {
      exception.put("success", false);
      exception.put("error", e.getMessage());
    } catch(JSONException err) {
      Log.d(LOG_TAG, "sendExecptionResponse E:" + err.getMessage());	
    }

    this._sendResponse(exception);
  }
  private void _sendExceptionResponse(Throwable e) {
    JSONObject exception = new JSONObject();
    try {
      exception.put("success", false);
      exception.put("error", e.getMessage());
    } catch(JSONException err) {
      Log.d(LOG_TAG, "sendExecptionResponse E:" + err.getMessage());	
    }

    this._sendResponse(exception);
  }
  private void _sendResponse(JSONObject data) {
    PluginResult pr = new PluginResult(PluginResult.Status.OK, data);
    pr.setKeepCallback(true);
    callbackContextGlobal.sendPluginResult(pr);
  }

  private final PlayAdCallback vunglePlayAdCallback = new PlayAdCallback() {
    @Override
    public void onAdStart(String placementReferenceId) {
      // Placement reference ID for the placement to be played
      if (callbackContextTemp != null) {
        callbackContextTemp.success(placementReferenceId);
      }

      JSONObject response = new JSONObject();
      JSONObject data = new JSONObject();

      try {
        data.put("placementId", placementReferenceId);

        response.put("isEvent", true);
        response.put("eventName", "onAdStart");
        response.put("data", data);
      } catch(JSONException e) {
        Log.d(LOG_TAG, "JSON onAdStart E:" + e.getMessage());
      }

      PluginResult pr = new PluginResult(PluginResult.Status.OK, response);
      pr.setKeepCallback(true);
      callbackContextGlobal.sendPluginResult(pr);
    }

    @Override
    public void onAdEnd (String placementReferenceId, boolean completed, boolean isCTAClicked) {
      // Placement reference ID for the placement that has completed ad experience
      // completed has value of true or false to notify whether video was
      // watched for 80% or more
      // isCTAClkcked has value of true or false to indicate whether download button
      // of an ad has been clicked by the user// Placement reference ID for the placement to be played
      JSONObject response = new JSONObject();
      JSONObject data = new JSONObject();

      try {
        data.put("placementId", placementReferenceId);
        data.put("completed", completed);
        data.put("isClicked", isCTAClicked);

        response.put("isEvent", true);
        response.put("eventName", "onAdEnd");
        response.put("data", data);
      } catch(JSONException e) {
        Log.d(LOG_TAG, "JSON onAdEnd E:" + e.getMessage());	
      }

      PluginResult pr = new PluginResult(PluginResult.Status.OK, response);
      pr.setKeepCallback(true);
      callbackContextGlobal.sendPluginResult(pr);
    }
 
    @Override
    public void onError(String placementReferenceId, Throwable throwable) {
      if (callbackContextTemp != null) {
        callbackContextTemp.error("Vungle.playAd.onError E: " + throwable.getMessage());
      }
      // Placement reference ID for the placement that failed to play an ad
      // Throwable contains error message
      JSONObject response = new JSONObject();
      JSONObject data = new JSONObject();

      try {
        data.put("placementId", placementReferenceId);
        data.put("error", throwable.getLocalizedMessage());

        response.put("isEvent", true);
        response.put("eventName", "onAdError");
        response.put("data", data);
      } catch(JSONException e) {
        Log.d(LOG_TAG, "JSON onAdError E:" + e.getMessage());	
      }  

      PluginResult pr = new PluginResult(PluginResult.Status.OK, response);
      pr.setKeepCallback(true);
      callbackContextGlobal.sendPluginResult(pr);
    }
  };
}
