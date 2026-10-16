package com.example.earsplit;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Experimental capture tester; original app audio is NOT isolated. */
public class MainActivity extends Activity {
    private static final int REQ_AUDIO = 1001, REQ_PROJECTION = 1002;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<AppChoice> installed = new ArrayList<>();
    private Spinner left, right;
    private TextView connected, status, levels;
    private LinearLayout controls;
    private Button start, stop;
    private final Runnable updater = new Runnable() {
        @Override public void run() {
            boolean hasOutput = HeadphoneDetector.connected(MainActivity.this);
            controls.setVisibility(hasOutput ? View.VISIBLE : View.GONE);
            connected.setText(hasOutput ? "Headphones connected ✓" :
                    "Connect Bluetooth earbuds or wired headphones to continue.");
            connected.setTextColor(hasOutput ? 0xff137849 : 0xffad6300);
            start.setEnabled(hasOutput && !CaptureService.running);
            stop.setEnabled(CaptureService.running);
            levels.setText("LEFT: " + CaptureService.leftLevel + "/100\nRIGHT: " + CaptureService.rightLevel +
                    "/100\n(A zero value can mean silence, paused audio, or capture blocked.)");
            status.setText(CaptureService.running ? "Capture test running in foreground service" :
                    CaptureService.lastMessage);
            handler.postDelayed(this, 500);
        }
    };

    static final class AppChoice implements Comparable<AppChoice> {
        final String label, pkg;
        final int uid;
        AppChoice(String label, String pkg, int uid) {
            this.label=label; this.pkg=pkg; this.uid=uid;
        }
        @Override public String toString() {
            return uid < 0 ? label : label + " (" + pkg + ")";
        }
        @Override public int compareTo(AppChoice other) {
            return label.compareToIgnoreCase(other.label);
        }
    }

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        loadApps();
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(22), dp(18), dp(22));
        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
        root.addView(text("EarSplit — App Audio Test", 24));
        root.addView(text("Choose installed apps for each earbud. This diagnostic app tests permitted audio capture; it does NOT mute the original stereo sound.", 14));
        connected = text("Checking connected headphones…", 16);
        root.addView(connected);
        controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        root.addView(controls);
        controls.addView(text("LEFT EARBUD — APP", 17));
        left = choice();
        controls.addView(left, new LinearLayout.LayoutParams(-1, dp(52)));
        controls.addView(text("RIGHT EARBUD — APP", 17));
        right = choice();
        controls.addView(right, new LinearLayout.LayoutParams(-1, dp(52)));
        start = new Button(this);
        start.setText("START CAPTURE TEST");
        start.setOnClickListener(v -> startCapture());
        controls.addView(start);
        stop = new Button(this);
        stop.setText("STOP CAPTURE TEST");
        stop.setOnClickListener(v -> {
            Intent i = new Intent(this, CaptureService.class);
            i.setAction(CaptureService.STOP);
            startService(i);
        });
        controls.addView(stop);
        levels = text("Capture levels appear here", 15);
        controls.addView(levels);
        root.addView(text("INSTRUCTIONS\n1. Connect earbuds.\n2. Select two different apps.\n3. Tap Start and accept Android's audio/screen-sharing permission. Select Entire screen if prompted.\n4. Switch to Spotify / Instagram and play audio.\n5. Return here to check capture levels.\n\nWARNING: Captured audio is experimentally relayed to L/R channels, but original source audio still plays in both ears.", 14));
        status = text("Ready", 14);
        status.setTextColor(0xff336699);
        root.addView(status);
    }

    private int dp(int x) {
        return (int)(x * getResources().getDisplayMetrics().density + .5f);
    }
    private TextView text(String s, int size) {
        TextView v = new TextView(this);
        v.setText(s); v.setTextSize(size);
        v.setPadding(0, dp(7), 0, dp(7));
        return v;
    }
    private Spinner choice() {
        Spinner s = new Spinner(this);
        ArrayAdapter<AppChoice> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, installed);
        s.setAdapter(adapter);
        return s;
    }
    private void loadApps() {
        installed.add(new AppChoice("Choose installed app", "", -1));
        PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> infos = pm.queryIntentActivities(intent, 0);
        List<AppChoice> found = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ResolveInfo x : infos) {
            if (x.activityInfo == null) continue;
            String pkg = x.activityInfo.packageName;
            if (pkg.equals(getPackageName()) || !seen.add(pkg)) continue;
            try {
                android.content.pm.ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                found.add(new AppChoice(pm.getApplicationLabel(ai).toString(), pkg, ai.uid));
            } catch (PackageManager.NameNotFoundException ignored) { }
        }
        Collections.sort(found);
        installed.addAll(found);
    }
    private void startCapture() {
        if (!HeadphoneDetector.connected(this)) { error("Connect earphones first."); return; }
        AppChoice l = (AppChoice)left.getSelectedItem(), r=(AppChoice)right.getSelectedItem();
        if (l==null || r==null || l.uid<0 || r.uid<0 || l.uid==r.uid) {
            error("Select two different installed apps."); return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
            return;
        }
        MediaProjectionManager m=(MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (m==null) { error("Screen/audio capture unsupported."); return; }
        startActivityForResult(m.createScreenCaptureIntent(), REQ_PROJECTION);
    }
    @Override public void onRequestPermissionsResult(int req, String[] p, int[] results) {
        super.onRequestPermissionsResult(req, p, results);
        if (req==REQ_AUDIO) {
            if (results.length>0 && results[0]==PackageManager.PERMISSION_GRANTED) startCapture();
            else error("Audio recording permission is required.");
        }
    }
    @Override protected void onActivityResult(int req, int code, Intent data) {
        super.onActivityResult(req,code,data);
        if (req!=REQ_PROJECTION) return;
        if (code!=RESULT_OK || data==null) { error("Capture permission denied."); return; }
        AppChoice l=(AppChoice)left.getSelectedItem(), r=(AppChoice)right.getSelectedItem();
        Intent i=new Intent(this,CaptureService.class);
        i.setAction(CaptureService.START);
        i.putExtra(CaptureService.CONSENT_CODE,code);
        i.putExtra(CaptureService.CONSENT_DATA,data);
        i.putExtra(CaptureService.LEFT_UID,l.uid);
        i.putExtra(CaptureService.RIGHT_UID,r.uid);
        try { startForegroundService(i); } catch(Exception e) { error("Service failed: "+e.getMessage()); }
    }
    private void error(String s) {
        new AlertDialog.Builder(this).setTitle("EarSplit Test").setMessage(s)
                .setPositiveButton("OK",null).show();
    }
    @Override protected void onResume() { super.onResume();handler.removeCallbacks(updater);handler.post(updater); }
    @Override protected void onPause() { super.onPause();handler.removeCallbacks(updater); }
}
