package com.hui.bluetooth_enable;

import android.app.Activity;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;

import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/** FlutterBluePlugin updated for Activity Result API */
public class BluetoothEnablePlugin implements FlutterPlugin, ActivityAware, MethodCallHandler {
    private static final String TAG = "BluetoothEnablePlugin";

    private Activity activity;
    private MethodChannel channel;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private Result pendingResult;

    private ActivityResultLauncher<Intent> enableBluetoothLauncher;

    @Override
    public void onAttachedToEngine(FlutterPlugin.FlutterPluginBinding binding) {
        this.channel = new MethodChannel(binding.getBinaryMessenger(), "bluetooth_enable");
        this.channel.setMethodCallHandler(this);
    }

    @Override
    public void onDetachedFromEngine(FlutterPlugin.FlutterPluginBinding binding) {
        this.channel.setMethodCallHandler(null);
        this.channel = null;
    }

    @Override
    public void onAttachedToActivity(ActivityPluginBinding binding) {
        this.activity = binding.getActivity();
        this.mBluetoothManager = (BluetoothManager) this.activity.getSystemService(Context.BLUETOOTH_SERVICE);
        this.mBluetoothAdapter = mBluetoothManager.getAdapter();

        // Register new Activity Result Launcher
        enableBluetoothLauncher = binding.getActivity().registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (pendingResult == null) return;
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        pendingResult.success("true");
                    } else {
                        pendingResult.success("false");
                    }
                }
        );
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        releaseResources();
    }

    @Override
    public void onReattachedToActivityForConfigChanges(ActivityPluginBinding binding) {
        onAttachedToActivity(binding);
    }

    @Override
    public void onDetachedFromActivity() {
        releaseResources();
    }

    private void releaseResources() {
        this.activity = null;
        this.mBluetoothManager = null;
        this.mBluetoothAdapter = null;
        this.pendingResult = null;
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        if (mBluetoothAdapter == null && !"isAvailable".equals(call.method)) {
            result.error("bluetooth_unavailable", "the device does not have bluetooth", null);
            return;
        }

        // Request runtime permission if needed
        ActivityCompat.requestPermissions(this.activity,
                new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                1);

        switch (call.method) {
            case "enableBluetooth":
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                pendingResult = result;
                enableBluetoothLauncher.launch(enableBtIntent);
                break;

            case "customEnable":
                try {
                    if (!mBluetoothAdapter.isEnabled()) {
                        mBluetoothAdapter.disable();
                        Thread.sleep(500); // short delay
                        mBluetoothAdapter.enable();
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, "customEnable", e);
                }
                result.success("true");
                break;

            default:
                result.notImplemented();
                break;
        }
    }
}
