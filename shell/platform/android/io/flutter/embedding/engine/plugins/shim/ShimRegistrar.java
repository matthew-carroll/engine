package io.flutter.embedding.engine.plugins.shim;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.platform.PlatformViewRegistry;
import io.flutter.view.FlutterMain;
import io.flutter.view.FlutterView;
import io.flutter.view.TextureRegistry;

/**
 * A {@link PluginRegistry.Registrar} that is shimmed to use the new Android embedding and plugin
 * API behind the scenes.
 * <p>
 * Instances of {@code ShimRegistrar}s are vended internally by a {@link ShimPluginRegistry}.
 */
class ShimRegistrar implements PluginRegistry.Registrar, FlutterPlugin, ActivityAware {
  private static final String TAG = "ShimRegistrar";

  private final Map<String, Object> globalRegistrarMap;
  private final String pluginId;
  private final Set<PluginRegistry.ViewDestroyListener> viewDestroyListeners = new HashSet<>();
  private final Set<PluginRegistry.RequestPermissionsResultListener> requestPermissionsResultListeners = new HashSet<>();
  private final Set<PluginRegistry.ActivityResultListener> activityResultListeners = new HashSet<>();
  private final Set<PluginRegistry.NewIntentListener> newIntentListeners = new HashSet<>();
  private final Set<PluginRegistry.UserLeaveHintListener> userLeaveHintListeners = new HashSet<>();
  private FlutterPlugin.FlutterPluginBinding pluginBinding;
  private ActivityPluginBinding activityPluginBinding;

  public ShimRegistrar(@NonNull String pluginId, @NonNull Map<String, Object> globalRegistrarMap) {
    this.pluginId = pluginId;
    this.globalRegistrarMap = globalRegistrarMap;
  }

  @Override
  public Activity activity() {
    return activityPluginBinding != null ? activityPluginBinding.getActivity() : null;
  }

  @Override
  public Context context() {
    return pluginBinding != null ? pluginBinding.getApplicationContext() : null;
  }

  @Override
  public Context activeContext() {
    return activityPluginBinding == null ? context() : activity();
  }

  @Override
  public BinaryMessenger messenger() {
    return pluginBinding != null ? pluginBinding.getFlutterEngine().getDartExecutor() : null;
  }

  @Override
  public TextureRegistry textures() {
    return pluginBinding != null ? pluginBinding.getFlutterEngine().getRenderer() : null;
  }

  @Override
  public PlatformViewRegistry platformViewRegistry() {
    return null;
  }

  @Override
  public FlutterView view() {
    throw new UnsupportedOperationException("The new embedding does not support the old FlutterView.");
  }

  @Override
  public String lookupKeyForAsset(String asset) {
    return FlutterMain.getLookupKeyForAsset(asset);
  }

  @Override
  public String lookupKeyForAsset(String asset, String packageName) {
    return FlutterMain.getLookupKeyForAsset(asset, packageName);
  }

  @Override
  public PluginRegistry.Registrar publish(Object value) {
    globalRegistrarMap.put(pluginId, value);
    return this;
  }

  @Override
  public PluginRegistry.Registrar addRequestPermissionsResultListener(PluginRegistry.RequestPermissionsResultListener listener) {
    requestPermissionsResultListeners.add(listener);

    if (activityPluginBinding != null) {
      activityPluginBinding.addRequestPermissionsResultListener(listener);
    } else {
      Log.w(TAG, "Tried to add a RequestPermissionsResultListener but no Activity is currently"
          + " attached to the associated FlutterEngine");
    }

    return this;
  }

  @Override
  public PluginRegistry.Registrar addActivityResultListener(PluginRegistry.ActivityResultListener listener) {
    activityResultListeners.add(listener);

    if (activityPluginBinding != null) {
      activityPluginBinding.addActivityResultListener(listener);
    } else {
      Log.w(TAG, "Tried to add an ActivityResultListener but no Activity is currently"
          + " attached to the associated FlutterEngine");
    }

    return this;
  }

  @Override
  public PluginRegistry.Registrar addNewIntentListener(PluginRegistry.NewIntentListener listener) {
    newIntentListeners.add(listener);

    if (activityPluginBinding != null) {
      activityPluginBinding.addOnNewIntentListener(listener);
    } else {
      Log.w(TAG, "Tried to add a NewIntentListener but no Activity is currently"
          + " attached to the associated FlutterEngine");
    }

    return this;
  }

  @Override
  public PluginRegistry.Registrar addUserLeaveHintListener(PluginRegistry.UserLeaveHintListener listener) {
    userLeaveHintListeners.add(listener);

    if (activityPluginBinding != null) {
      activityPluginBinding.addOnUserLeaveHintListener(listener);
    } else {
      Log.w(TAG, "Tried to add a UserLeaveHintListener but no Activity is currently"
          + " attached to the associated FlutterEngine");
    }

    return this;
  }

  @Override
  @NonNull
  public PluginRegistry.Registrar addViewDestroyListener(@NonNull PluginRegistry.ViewDestroyListener listener) {
    viewDestroyListeners.add(listener);
    return this;
  }

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
    pluginBinding = binding;
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    for (PluginRegistry.ViewDestroyListener listener : viewDestroyListeners) {
      // The following invocation might produce unexpected behavior in old plugins because
      // we have no FlutterNativeView to pass to onViewDestroy(). This is a limitation of this shim.
      listener.onViewDestroy(null);
    }

    pluginBinding = null;
  }

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    activityPluginBinding = binding;
    addExistingListenersToActivityPluginBinding();
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    activityPluginBinding = null;
  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
    activityPluginBinding = binding;
    addExistingListenersToActivityPluginBinding();
  }

  @Override
  public void onDetachedFromActivity() {
    activityPluginBinding = null;
  }

  private void addExistingListenersToActivityPluginBinding() {
    for (PluginRegistry.RequestPermissionsResultListener listener : requestPermissionsResultListeners) {
      activityPluginBinding.addRequestPermissionsResultListener(listener);
    }
    for (PluginRegistry.ActivityResultListener listener : activityResultListeners) {
      activityPluginBinding.addActivityResultListener(listener);
    }
    for (PluginRegistry.NewIntentListener listener : newIntentListeners) {
      activityPluginBinding.addOnNewIntentListener(listener);
    }
    for (PluginRegistry.UserLeaveHintListener listener : userLeaveHintListeners) {
      activityPluginBinding.addOnUserLeaveHintListener(listener);
    }
  }
}
