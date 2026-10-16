package com.example.earsplit;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import java.util.Arrays;

/** Audio capture PROTOTYPE. Does not suppress original apps' audio streams. */
public class CaptureService extends Service {
    public static final String START="com.example.earsplit.START";
    public static final String STOP="com.example.earsplit.STOP";
    public static final String CONSENT_CODE="consent_code";
    public static final String CONSENT_DATA="consent_data";
    public static final String LEFT_UID="left_uid";
    public static final String RIGHT_UID="right_uid";
    public static volatile boolean running=false;
    public static volatile int leftLevel=0,rightLevel=0;
    public static volatile String lastMessage="Connect headphones and select two apps.";
    private static final int RATE=48000, FRAMES=960;
    private MediaProjection projection;
    private MediaProjection.Callback callback;
    private CaptureWorker left,right;
    private AudioTrack track;
    private Thread relay;

    @Override public void onCreate() {
        super.onCreate();
        NotificationManager nm=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm!=null) nm.createNotificationChannel(new NotificationChannel(
                "earsplit_capture","EarSplit capture",NotificationManager.IMPORTANCE_LOW));
    }
    private Notification notification() {
        return new Notification.Builder(this,"earsplit_capture")
                .setContentTitle("EarSplit capture test running")
                .setContentText("App audio monitoring; original audio is still playing")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true).build();
    }
    @Override public int onStartCommand(Intent i,int flags,int id) {
        if(i==null) return START_NOT_STICKY;
        if(STOP.equals(i.getAction())) {
            lastMessage="Test stopped."; stopSelf(); return START_NOT_STICKY;
        }
        if(!START.equals(i.getAction()) || running) return START_NOT_STICKY;
        if(!HeadphoneDetector.connected(this)){
            lastMessage="Headphones disconnected."; stopSelf(); return START_NOT_STICKY;
        }
        try {
            Intent data;
            if(Build.VERSION.SDK_INT>=33) data=i.getParcelableExtra(CONSENT_DATA,Intent.class);
            else data=i.getParcelableExtra(CONSENT_DATA);
            int code=i.getIntExtra(CONSENT_CODE,0);
            int lUid=i.getIntExtra(LEFT_UID,-1),rUid=i.getIntExtra(RIGHT_UID,-1);
            if(data==null || lUid<0 || rUid<0 || lUid==rUid)
                throw new IllegalArgumentException("Missing consent or invalid app selection");
            startForeground(303,notification(),ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION |
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            MediaProjectionManager pm=(MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            if(pm==null) throw new IllegalStateException("Projection service missing");
            projection=pm.getMediaProjection(code,data);
            if(projection==null) throw new IllegalStateException("Projection unavailable");
            callback=new MediaProjection.Callback(){
                @Override public void onStop(){
                    lastMessage="Android ended the capture session; restart with approval.";
                    stopSelf();
                }
            };
            projection.registerCallback(callback,new Handler(Looper.getMainLooper()));
            left=new CaptureWorker(lUid,true);
            right=new CaptureWorker(rUid,false);
            track=createTrack();
            running=true;
            leftLevel=rightLevel=0;
            lastMessage="Capture active (original audio is not isolated).";
            left.start();right.start();
            relay=new Thread(this::relayLoop,"EarSplit stereo relay");
            relay.start();
        } catch(Exception ex) {
            lastMessage="Capture failed: "+ex.getClass().getSimpleName()+": "+ex.getMessage();
            stopSelf();
        }
        return START_NOT_STICKY;
    }
    private AudioRecord makeRecord(int uid) {
        AudioPlaybackCaptureConfiguration config=
                new AudioPlaybackCaptureConfiguration.Builder(projection)
                .addMatchingUid(uid)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build();
        AudioFormat fmt=new AudioFormat.Builder().setSampleRate(RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_IN_MONO).build();
        int min=AudioRecord.getMinBufferSize(RATE,AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        if(min<=0) throw new IllegalStateException("Unsupported recording format");
        AudioRecord result=new AudioRecord.Builder().setAudioFormat(fmt)
                .setBufferSizeInBytes(Math.max(min*2,FRAMES*4))
                .setAudioPlaybackCaptureConfig(config).build();
        if(result.getState()!=AudioRecord.STATE_INITIALIZED) {
            result.release(); throw new IllegalStateException("AudioRecord init failed");
        }
        return result;
    }
    private AudioTrack createTrack() {
        AudioFormat fmt=new AudioFormat.Builder().setSampleRate(RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build();
        int min=AudioTrack.getMinBufferSize(RATE,AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);
        if(min<=0) throw new IllegalStateException("Unsupported playback format");
        AudioTrack t=new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(fmt)
                .setBufferSizeInBytes(Math.max(min*2,FRAMES*8))
                .setTransferMode(AudioTrack.MODE_STREAM).build();
        if(t.getState()!=AudioTrack.STATE_INITIALIZED){
            t.release(); throw new IllegalStateException("AudioTrack init failed");
        }
        AudioManager audio=(AudioManager)getSystemService(Context.AUDIO_SERVICE);
        if(audio!=null) for(AudioDeviceInfo d:audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)){
            int type=d.getType();
            if(type==AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
               type==AudioDeviceInfo.TYPE_BLE_HEADSET ||
               type==AudioDeviceInfo.TYPE_WIRED_HEADSET ||
               type==AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
               type==AudioDeviceInfo.TYPE_USB_HEADSET){
                t.setPreferredDevice(d);break;
            }
        }
        return t;
    }
    private final class CaptureWorker {
        final boolean isLeft;
        final int uid;
        AudioRecord record;
        Thread thread;
        volatile short[] frame = new short[FRAMES];
        CaptureWorker(int uid,boolean left){this.uid=uid;this.isLeft=left;}
        void start(){
            record=makeRecord(uid);
            record.startRecording();
            thread=new Thread(()->{
                short[] buf=new short[FRAMES];
                try {
                    while(running&&!Thread.currentThread().isInterrupted()){
                        int n=record.read(buf,0,FRAMES,AudioRecord.READ_BLOCKING);
                        if(n<0){lastMessage="Audio capture read error: "+n;stopSelf();break;}
                        if(n==0) continue;
                        int peak=0;
                        for(int x=0;x<n;x++)peak=Math.max(peak,Math.abs((int)buf[x]));
                        int val=Math.min(100,Math.round(peak*100f/32768f));
                        if(isLeft)leftLevel=val;else rightLevel=val;
                        frame=Arrays.copyOf(buf,FRAMES);
                    }
                }catch(Exception e){
                    if(running){lastMessage="Capture error: "+e.getMessage();stopSelf();}
                }
            },isLeft?"EarSplit left":"EarSplit right");
            thread.start();
        }
        void stop(){
            if(record!=null){
                try{record.stop();}catch(Exception ignored){}
                record.release();record=null;
            }
            if(thread!=null){thread.interrupt();thread=null;}
        }
    }
    private void relayLoop() {
        try{
            track.play();
            short[] result=new short[FRAMES*2];
            while(running&&!Thread.currentThread().isInterrupted()){
                if(!HeadphoneDetector.connected(this)){
                    lastMessage="Disconnected; stopped capture.";stopSelf();break;
                }
                short[] l=left.frame,r=right.frame;
                for(int x=0;x<FRAMES;x++){
                    result[x*2]=(short)(l[x]*0.22f);
                    result[x*2+1]=(short)(r[x]*0.22f);
                }
                int wrote=track.write(result,0,result.length,AudioTrack.WRITE_BLOCKING);
                if(wrote<0){lastMessage="Relay write error: "+wrote;stopSelf();break;}
            }
        }catch(Exception ex){
            if(running){lastMessage="Relay error: "+ex.getMessage();stopSelf();}
        }
    }
    @Override public void onDestroy(){
        running=false;leftLevel=rightLevel=0;
        if(left!=null){left.stop();left=null;}
        if(right!=null){right.stop();right=null;}
        if(relay!=null){relay.interrupt();relay=null;}
        if(track!=null){
            try{track.pause();}catch(Exception ignored){}
            try{track.flush();}catch(Exception ignored){}
            track.release();track=null;
        }
        if(projection!=null){
            if(callback!=null)projection.unregisterCallback(callback);
            projection.stop();projection=null;
        }
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent){return null;}
}
