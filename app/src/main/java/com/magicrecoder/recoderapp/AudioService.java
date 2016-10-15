package com.magicrecoder.recoderapp;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Administrator on 2016-09-12.
 */
public class AudioService extends Service {
    private static final String TAG = "Lifecycle";
    private AudioManager mAm;
    MediaPlayer player;
    private Timer timer;
    private int isPlaying = 1;//判断是否正在播放中
    private int isPlayComplete = 1;//判断播放是否结束
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG,"返回接口类AudioController对象");
        return  new AudioController();
    }
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"启动音乐播放服务");
        player = new MediaPlayer();
        mAm = (AudioManager) getSystemService(AUDIO_SERVICE);
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.d(TAG,"播放结束");
                isPlayComplete = 0;
            }
        });
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        //停止播放
        player.stop();
        //释放占用的资源，此时player对象已经废掉了，但是player对象还在。
        player.release();
        player = null;
        if(timer != null){
            timer.cancel();
            timer = null;
        }
        mAm.abandonAudioFocus(afChangeListener);
        Log.d(TAG,"AudioService Destroy");
    }
    AudioManager.OnAudioFocusChangeListener afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                // Pause playback
                pause();
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                // Resume playback
                //continuePlay();
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                // mAm.unregisterMediaButtonEventReceiver(RemoteControlReceiver);
                mAm.abandonAudioFocus(afChangeListener);
                // Stop playback
                stop();
            }
        }
    };
    private boolean requestFocus() {
        // Request audio focus for playback
        int result = mAm.requestAudioFocus(afChangeListener,
                // Use the music stream.
                AudioManager.STREAM_MUSIC,
                // Request permanent focus.
                AudioManager.AUDIOFOCUS_GAIN);
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }
    MediaPlayer.OnCompletionListener completionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer player) {
            if(!player.isLooping()){
                mAm.abandonAudioFocus(afChangeListener);
            }
        }
    };

    class AudioController extends Binder implements MusicInterface {
        @Override
        public void play(String filePath) {
            AudioService.this.play(filePath);
        }
        @Override
        public  void pause() {
            AudioService.this.pause();
        }
        @Override
        public void continuePlay() {
            AudioService.this.continuePlay();
        }
        @Override
        public void seekTo(int process) {
            AudioService.this.seekTo(process);
        }
        @Override
        public boolean isPlaying () { return  AudioService.this.isPlaying(); };
        @Override
        public int getCurrentPosition () { return  AudioService.this.getCurrentPosition(); };
    }

    //播放录音
    public void play(String filePath) {
        //重置
        player.reset();
        Log.d(TAG,"录音路径为"+filePath);
        if (requestFocus()) {
            try {
                Log.d(TAG, "调用服务接口实现类播放录音");
                //加载多媒体文件
                player.setDataSource(filePath);//播放本地音频可以同步准备，调用主线程的player.prepare()方法和start()方法，因为主线程知道prepare()好了之后再start()
//            player.setDataSource("http://192.168.13.119:8080/bzj.mp3");  //播放网络音频是一个耗时操作必须要开启子线城异步准备调用player.prepareAsync()方法，这个方法默认就是开启子线城，不能直接调用主线程的start()方法因为主线程不知道子线城什么时候准备完毕，因此要在主线程做监听setOnPreparedListener()，当子线城准备完了之后主线程监听到了之后主线程才能start()
//            player.prepare();  因为调用了硬件所以要做准备
                player.prepareAsync();   //异步准备，开启子线程加载资源
                Log.d(TAG, "准备好后获取会话ID" + player.getAudioSessionId());
//            player.start();
                player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {//准备监听
                    //prepare()方法准备完毕时，此方法调用
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        player.start();
                        Log.d(TAG, "开始播放中");
                        isPlaying = 1;
                        isPlayComplete = 1;
                        if (timer != null){
                            timer = null;
                        }
                        addTimer();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    //继续播放
    public void continuePlay() {
        player.start();
        Log.d(TAG, "继续播放中");
        isPlaying = 1;
        isPlayComplete = 1;
        if (timer != null){
            timer = null;
        }
        addTimer();
    }
    //暂停播放
    public void pause(){
        player.pause();
    }
    //放弃播放
    public void stop() {
        if (player != null) {
            player.stop();
        }
    }
    //更新进度条
    public void seekTo(int progress){
        //isPlaying = 1;
        //isPlay = 1;
        player.seekTo(progress);
    }
    public void addTimer(){
        if(timer == null) {
            Log.d(TAG, "创建timer对象");
            timer = new Timer();//timer就是开启子线程执行任务，与纯粹的子线城不同的是可以控制子线城执行的时间，
        }
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                //Log.d(TAG, "TimeTask正在运行");
                //获取歌曲总时长
                int duration = player.getDuration();
                //获取歌曲当前播放进度
                int currentPosition = player.getCurrentPosition();
                if (isPlaying != 0) {
                    Message msg = AudioPlayActivity.handler.obtainMessage();
                    msg.what=1;
                    //把进度封装至消息对象中
                    Bundle bundle = new Bundle();
                    bundle.putInt("duration", duration);
                    bundle.putInt("currentPosition", currentPosition);
                    bundle.putBoolean("isPlaying", true);
                    msg.setData(bundle);
                    AudioPlayActivity.handler.sendMessage(msg);
                    //Log.d(TAG, "播放进行中,发送音乐位置消息给主线程");
                    if (!player.isPlaying()) {
                        isPlaying = 0;
                    }
                }
                else {
                    Message msg2 = AudioPlayActivity.handler.obtainMessage();
                    msg2.what=1;
                    Bundle bundle2 = new Bundle();
                    bundle2.putInt("duration", duration);
                    bundle2.putInt("currentPosition", currentPosition);
                    if (isPlayComplete == 0) {
                        bundle2.putBoolean("isPlaying", false);
                    }
                    else{
                        bundle2.putBoolean("isPlaying", true);
                    }
                    msg2.setData(bundle2);
                    AudioPlayActivity.handler.sendMessage(msg2);
                    Log.d(TAG, "发送消息给主线程,播放已结束");
                    Log.d(TAG, "结束TimeTask");
                    timer.cancel();
                }
            }
            //开始计时任务后的5毫秒后第一次执行run方法，以后每500毫秒执行一次
        }, 100, 500);
    }
    //获取播放状态
    public boolean isPlaying () {
        return player.isPlaying();
    };
    //获取当前播放位置
    public int getCurrentPosition () {
        int position = 0;
        try {
            position = player.getCurrentPosition();
            return position;
        }catch(Exception e){
            e.printStackTrace();
        }
        return position;
    };

    public interface MusicInterface {
        void play(String filePath);
        void pause();
        void continuePlay();
        void seekTo(int progress);
        boolean isPlaying();
        int getCurrentPosition();
    }
}