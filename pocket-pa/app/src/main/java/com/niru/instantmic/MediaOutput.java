package com.niru.instantmic;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** System media routing only. Android Settings handles pairing and connecting accessories. */
final class MediaOutput {
    static final int AUTOMATIC = -1;
    static final int SYSTEM = 0;
    static final AudioAttributes ATTRIBUTES = buildAttributes();

    private MediaOutput() { }

    private static AudioAttributes buildAttributes() {
        AudioAttributes.Builder builder = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH);
        if (Build.VERSION.SDK_INT >= 29) {
            builder.setAllowedCapturePolicy(AudioAttributes.ALLOW_CAPTURE_BY_NONE);
        }
        return builder.build();
    }

    static List<AudioDeviceInfo> bluetoothDevices(AudioManager manager) {
        List<AudioDeviceInfo> devices = new ArrayList<>();
        for (AudioDeviceInfo device : manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
            if (isBluetoothMedia(device)) devices.add(device);
        }
        return devices;
    }

    static boolean isBuiltInSpeaker(AudioDeviceInfo device) {
        if (device == null) return false;
        int type = device.getType();
        return type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                || type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE;
    }

    private static boolean isBluetoothMedia(AudioDeviceInfo device) {
        int type = device.getType();
        return type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                || (Build.VERSION.SDK_INT >= 31 && (type == AudioDeviceInfo.TYPE_BLE_HEADSET
                        || type == AudioDeviceInfo.TYPE_BLE_SPEAKER));
    }

    static AudioDeviceInfo preferred(AudioManager manager, List<AudioDeviceInfo> devices, int selection) {
        if (selection == SYSTEM || devices.isEmpty()) return null;
        if (selection != AUTOMATIC) {
            for (AudioDeviceInfo device : devices) {
                if (device.getId() == selection) return device;
            }
            return null;
        }
        if (Build.VERSION.SDK_INT >= 33) {
            for (AudioDeviceInfo current : manager.getAudioDevicesForAttributes(ATTRIBUTES)) {
                for (AudioDeviceInfo device : devices) {
                    if (device.getId() == current.getId()) return device;
                }
            }
        }
        return devices.get(0);
    }

    @SuppressWarnings("deprecation") // API 26–35 report one routed device.
    static List<AudioDeviceInfo> routedDevices(AudioTrack track) {
        if (Build.VERSION.SDK_INT >= 36) return track.getRoutedDevices();
        AudioDeviceInfo route = track.getRoutedDevice();
        return route == null ? Collections.emptyList() : Collections.singletonList(route);
    }

    static String label(Context context, AudioDeviceInfo device) {
        switch (device.getType()) {
            case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER:
                return context.getString(R.string.output_phone);
            case AudioDeviceInfo.TYPE_BUILTIN_EARPIECE:
                return context.getString(R.string.output_earpiece);
            case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:
            case AudioDeviceInfo.TYPE_WIRED_HEADSET:
                return context.getString(R.string.output_wired);
            default:
                CharSequence name = device.getProductName();
                if (name != null && name.length() > 0) return name.toString();
                return context.getString(isBluetoothMedia(device)
                        ? R.string.output_bluetooth : R.string.output_system);
        }
    }
}
