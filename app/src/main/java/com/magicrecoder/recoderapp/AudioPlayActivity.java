package com.magicrecoder.recoderapp;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.magicrecoder.greendao.RecorderInfoDao;
import com.tencent.mm.sdk.modelmsg.SendMessageToWX;
import com.tencent.mm.sdk.modelmsg.WXMediaMessage;
import com.tencent.mm.sdk.modelmsg.WXMusicObject;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AudioPlayActivity extends Activity {
    private static final String TAG = "Lifecycle";
    public static IWXAPI api;
    String WX_APP_ID = "wx96692a70ef7c9064";

    private static SeekBar seekBar;
    private AudioService.MusicInterface mi;
    private MyServiceConn conn;
    private Intent intent;

    private RecorderInfoDao recorderInfoDao;
    private RecorderInfo recorderInfo;
    private String filePath;
    private static String durationTime;
    private static TextView Seekbar_slider_time;

    private  static ImageView playIcon;
    private  static ImageView pauseIcon;
    private  static TextView tx_currentTime;
    TextView tx_maxTime;
    private ImageView backToRecorder;
    private ImageView delAudio;
    private ImageView shareAudio;
    private ImageView shareToFriend;
    private ImageView recorder_star;

    private LinearLayout.LayoutParams miss = new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.MATCH_PARENT,0);
    private LinearLayout.LayoutParams show = new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.MATCH_PARENT,1);

    private List<RecorderInfo> recentPlayData;//listView数据对象
    private PlayListAdapter playListAdapter;//适配器对象

    private TextView tx_play_recorder_name;
    private TextView tx_play_recorder_author;
    private ImageView image_chevron_left;
    private ListView recentPlayListView;//最近播放列表对象

    static Handler handler = new Handler(){//handler是谷歌说明的定义成静态的，
        public void handleMessage(Message msg) {
            switch (msg.what) {
                default:
                case 1:
                    Bundle bundle = msg.getData();
                    boolean isPlay = bundle.getBoolean("isPlaying");
                    int duration = bundle.getInt("duration");
                    int currentPosition = bundle.getInt("currentPosition");
                    //刷新进度条的进度，设置SeekBar的Max和Progress就能够时时更新SeekBar的长度，
                    if (isPlay) {
                        //Log.d(TAG,"正在播放,总时长为"+duration+",当前位置为"+currentPosition);
                        seekBar.setMax(duration);
                        seekBar.setProgress(currentPosition);
                        try {
                            durationTime = updateCurrentTimeText(currentPosition);
                            tx_currentTime.setText(durationTime);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    //根据当前位置判断录音是否结束，结束后更新UI，getCurrentPosition与duration偏移量200以内
                    else {
                        Log.d(TAG, "播放结束,当前位置为" + currentPosition + "seekBar的长度为" + duration);
                        seekBar.setProgress(duration);
                        tx_currentTime.setText(durationTime);
                        LinearLayout.LayoutParams miss = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0);
                        pauseIcon.setLayoutParams(miss);
                        LinearLayout.LayoutParams show = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1);
                        playIcon.setLayoutParams(show);
                    }
                    break;
                case 2:
                    String time = (String)msg.obj;
                    Log.d(TAG,"接收到的消息为"+time);
                    if (time.equals("")){
                        try {
                            Seekbar_slider_time.setVisibility(View.INVISIBLE);
                        }catch (Exception e){
                            Log.e(TAG, "an error occured when collect crash info", e);
                        }
                    }
                    else {
                        try {
                            Seekbar_slider_time.setVisibility(View.VISIBLE);
                            Seekbar_slider_time.setText(time);
                        }catch (Exception e){
                            Log.e(TAG, "an error occured when collect crash info", e);
                        }
                    }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "AudioPlayActivity onCreate 创建 执行");
        setContentView(R.layout.activity_media_play);
        setStatusColor();
        seekBar = (SeekBar) findViewById(R.id.seedBar);
        try {
            Intent intent2 = getIntent();
            recorderInfo = intent2.getParcelableExtra("recorder");
            if (recorderInfo == null){
                AudioPlayActivity.this.finish();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        filePath = recorderInfo.getFilepath();
        durationTime = recorderInfo.getOften();
        Log.d(TAG,"启动播放界面,录音文件路径为"+filePath+"时常为"+durationTime);


        tx_currentTime = (TextView) findViewById(R.id.tx_currentTime);
        tx_currentTime.setText("00:00");
        tx_maxTime = (TextView) findViewById(R.id.tx_maxTime);
        tx_maxTime.setText(durationTime);
        seekBar = (SeekBar) findViewById(R.id.seedBar);

        playIcon = (ImageView) findViewById(R.id.play);
        pauseIcon = (ImageView) findViewById(R.id.pause);
        backToRecorder = (ImageView) findViewById(R.id.backToRecorder);
        delAudio = (ImageView) findViewById(R.id.delAudio);
        shareAudio =(ImageView) findViewById(R.id.image_recorder_share);
        shareToFriend =(ImageView) findViewById(R.id.image_recorder_share_wechat) ;
        tx_play_recorder_name = (TextView) findViewById(R.id.tx_play_recorder_name);
        tx_play_recorder_author = (TextView) findViewById(R.id.tx_play_recorder_author);
        tx_play_recorder_name.setText(recorderInfo.getName());
        tx_play_recorder_author.setText(recorderInfo.getCreate_user());
        image_chevron_left = (ImageView) findViewById(R.id.image_chevron_left);
        recorder_star = (ImageView) findViewById(R.id.image_recorder_star);
        Seekbar_slider_time = (TextView) findViewById(R.id.seekbar_slider_time);

        intent= new Intent(this,AudioService.class);
        startService(intent);
        conn= new MyServiceConn();
        bindService(intent,conn,BIND_AUTO_CREATE);


        api= WXAPIFactory.createWXAPI(this,WX_APP_ID); //初始化api

        api.registerApp(WX_APP_ID); //将APP_ID注册到微信中
    }
    @Override
    public  void onResume() {
        super.onResume();
        //init_play_list();
        recorderInfoDao = ((Recorderapplication) getApplicationContext()).recorderinfoDao;
        String Tag = recorderInfoDao.load(recorderInfo.getId()).getTag();
        Log.d(TAG,Tag);
        if(Tag.equals("1")){
            recorder_star.setImageResource(R.drawable.ic_grade_red_24dp);
            recorder_star.setTag(R.drawable.ic_grade_red_24dp);
        }
        Log.d(TAG, "AudioPlayActivity onResume 获取焦点 执行");
        playIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playIcon.setLayoutParams(miss);
                pauseIcon.setLayoutParams(show);
                //根据seekBar位置判断是继续还是重新开始
                if ( mi.getCurrentPosition()>0 && !(seekBar.getProgress()==seekBar.getMax()) ) {
                    Log.d(TAG,"当前播放位置为"+ mi.getCurrentPosition() +",继续播放");
                    continuePlay();
                }
                else {
                    Log.d(TAG,"重新开始播放");
                    seekBar.setProgress(0);
                    play();
                }
            }
        });
        pauseIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG,"暂停播放录音");
                pauseIcon.setLayoutParams(miss);
                playIcon.setLayoutParams(show);
                pause();
            }
        });
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser){
/*                    Log.d(TAG,"来自用户的操作seekBar");*/
                    seekBar.setThumb(ContextCompat.getDrawable(AudioPlayActivity.this,R.drawable.slider_thumb_pressed));
                    Log.d(TAG,updateCurrentTimeText(progress));
                    if (seekBar.getThumbOffset()== 0) {
                        seekBar.setThumbOffset(15);
                    }
                    seed_slider_time(updateCurrentTimeText(progress));
                }
                else{
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.d(TAG,"onStartTrackingTouch");
                //Seekbar_slider_time.setVisibility(View.VISIBLE);
                //Seekbar_slider_time.setText("00:23");
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mi.seekTo(seekBar.getProgress());
                seekBar.setThumb(ContextCompat.getDrawable(AudioPlayActivity.this,R.drawable.slider_thumb_normal));
                seekBar.setThumbOffset(0);
                seed_slider_time("");
                //Seekbar_slider_time.setVisibility(View.VISIBLE);
            }
        });
        backToRecorder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AudioPlayActivity.this.finish();//这个是关键
            }
        });
        image_chevron_left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AudioPlayActivity.this.finish();//这个是关键
            }
        });
        recorder_star.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Integer integer = (Integer) recorder_star.getTag();
                integer = integer == null ? 0 : integer;
                switch (integer) {
                    default:
                    case R.drawable.ic_grade_white_24dp:
                        recorder_star.setTag(R.drawable.ic_grade_red_24dp);
                        recorder_star.setImageResource(R.drawable.ic_grade_red_24dp);
                        recorderInfo.setTag("1");
                        recorderInfoDao.update(recorderInfo);
                        break;
                    case R.drawable.ic_grade_red_24dp:
                        recorder_star.setTag(R.drawable.ic_grade_white_24dp);
                        recorder_star.setImageResource(R.drawable.ic_grade_white_24dp);
                        recorderInfo.setTag("0");
                        recorderInfoDao.update(recorderInfo);
                        break;
                }
            }
        });

        delAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG,"询问删除文件,暂停播放录音");
                if(mi.isPlaying()) {
                    pauseIcon.setLayoutParams(miss);
                    playIcon.setLayoutParams(show);
                    pause();
                    Log.d(TAG,"已暂停暂停播放录音");
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(AudioPlayActivity.this,R.style.Theme_System_Alert);
                //设置对话框图标，可以使用自己的图片，Android本身也提供了一些图标供我们使用
                //builder.setIcon(android.R.drawable.ic_dialog_alert);
                //设置对话框标题
                builder.setTitle("删除录音");
                //设置对话框内的文本
                builder.setMessage("删除录音文件"+recorderInfo.getName());
                //设置确定按钮，并给按钮设置一个点击侦听，注意这个OnClickListener使用的是DialogInterface类里的一个内部接口
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 执行点击确定按钮的业务逻辑
                        if(recorderInfo != null) {
                            try {
                                dialog.dismiss();
                                String id =""+recorderInfo.getId();
                                Log.d(TAG,"recorder id is"+id);
                                getDao().deleteByKey(recorderInfo.getId());
                                File AudioFile = new File(recorderInfo.getFilepath());
                                boolean result = AudioFile.delete();
                                if (result){
                                    Toast.makeText(getBaseContext(),"删除成功",Toast.LENGTH_SHORT).show();
                                }
                                AudioPlayActivity.this.finish();
                            }
                            catch(Exception e){
                                e.printStackTrace();
                            }
                        }
                    }
                });
                //设置取消按钮
                builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 执行点击取消按钮的业务逻辑
                        dialog.dismiss();
                    }
                });
                //使用builder创建出对话框对象
                AlertDialog dialog = builder.create();
                //显示对话框
                dialog.show();
            }
        });
        shareAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG,"暂停播放录音");
                pauseIcon.setLayoutParams(miss);
                playIcon.setLayoutParams(show);
                pause();
                //new SharePopupWindow(AudioPlayActivity.this, "分享内容", AudioPlayActivity.this.findViewById(R.id.play_layout));
                Uri audioUri = Uri.fromFile(new File(filePath));
                shareAudio("分享","分享录音",recorderInfo.getName(),audioUri);
            }
        });
        shareToFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG,"暂停播放录音");
                pauseIcon.setLayoutParams(miss);
                playIcon.setLayoutParams(show);
                pause();
                Uri audioUri = Uri.fromFile(new File(filePath));
                shareToFriend(audioUri);
                //shareToWeChat();
            }
        });
    }
    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "AudioPlayActivity onStop 不可见 执行");
    }
    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "AudioPlayActivity onPause 失去焦点 执行");
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "AudioPlayActivity onDestroy 销毁 执行");
        exit();
    }
    @Override
    public void onRestart() {
        super.onRestart();
        Log.d(TAG, "AudioPlayActivity onRestart 重新打开 执行");
    }
    private void setStatusColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            WindowManager.LayoutParams params = window.getAttributes();
            int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
            // 设置透明状态栏
            if ((params.flags & bits) == 0) {
                params.flags |= bits;
                window.setAttributes(params);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }
    //初始化最近播放列表
    private void init_play_list() {
    //列表非空，且适配器为空才初始化
        if (recentPlayListView != null && playListAdapter == null){
            LayoutInflater inflater = getLayoutInflater();
            //init_playRecent_data();
            Log.d(TAG,"对象的名称为"+recorderInfo.getName());
            if (recentPlayData == null) {
                recentPlayData = new ArrayList<>();
                Log.d(TAG, "初始化列表数据");
                //按照插入时间倒序排序，也就是说时间晚的会在前面显示
                recentPlayData = getDao().queryBuilder().where(RecorderInfoDao.Properties.Id.notEq(-1)).orderDesc(RecorderInfoDao.Properties.Id).build().list();
                //recentPlayData.add(0,recorderInfo);
            }
            Log.d(TAG,"绑定适配器数据");
            playListAdapter = new PlayListAdapter(inflater, recentPlayData);
            Log.d(TAG,"列表绑定适配器");
            recentPlayListView.setAdapter(playListAdapter);
            //单击监听
            recentPlayListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    RecorderInfo recorderObject = recentPlayData.get(position);
                    File audioFile = new File(recorderObject.getFilepath());
                    if (audioFile.exists()) {
                        //playMusic(audioFile);
                        Log.d(TAG,"要开始播放了");
                        filePath = recorderObject.getFilepath();
                        mi.play(filePath);
                        pauseIcon.setLayoutParams(show);
                        playIcon.setLayoutParams(miss);
                        tx_maxTime.setText(recorderObject.getOften());
                    } else {
                        Toast.makeText(getBaseContext(), "录音文件不存在", Toast.LENGTH_SHORT).show();
                        if (recorderObject.getId() != null) {
                            getDao().deleteByKey(recorderObject.getId());//删除数据库对象
                            recentPlayData.remove(recorderObject);
                            playListAdapter.notifyDataSetChanged();
                        }
                    }
                }
            });
        }
    }

    /*通过 Recorderapplication 类提供的 getDaoSession() 获取具体 Dao*/
    private RecorderInfoDao getDao() {
        return ((Recorderapplication) this.getApplicationContext()).recorderinfoDao;
    }
    class MyServiceConn implements ServiceConnection{
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mi = (AudioService.MusicInterface) service;//中间人
            Log.d(TAG,"获取到mi对象,立即播放");
            play();
            seekBar.setProgress(0);
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    }
    public void play(){
        Log.d(TAG,"调用MediaPlayActivity里的方法play");
        mi.play(filePath);
    }
    public void continuePlay (){
        mi.continuePlay();
    }
    public void pause(){
        mi.pause();
    }
    public void exit() {
        Log.d(TAG,"退出服务");
        unbindService(conn);  //解绑
        stopService(intent);  //停止
    }
    public static String updateCurrentTimeText(int currentPosition){
        try {
            int time= currentPosition/1000;
            if (time >= 3600) {
                int hour = time/3600;
                int minute = (time/60) % 60;
                int second = time % 60;
                String Time = String.format(Locale.getDefault(),"%02d:%02d:%02d", hour, minute, second);
                //tx_currentTime.setText(currentTime);
                return  Time;
            }
            else{
                int minute = time/60;
                int second = time % 60;
                String Time = String.format(Locale.getDefault(),"%02d:%02d", minute, second);
                //tx_currentTime.setText(currentTime);
                return  Time;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    };
    private void seed_slider_time(String slider_time) {
        Message msg=handler.obtainMessage();
        //需要数据传递，用下面方法；
        msg.what=2;
        msg.obj = slider_time;//可以是基本类型，可以是对象，可以是List、map等；
        Log.d(TAG,"发送消息给handler");
        handler.sendMessage(msg);
    }

    private void shareAudio(String dlgTitle, String subject, String content, Uri uri) {
        if (uri == null) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("audio/*");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        if (subject != null && !"".equals(subject)) {
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        }
        if (content != null && !"".equals(content)) {
            intent.putExtra(Intent.EXTRA_TEXT, content);
        }
        // 设置弹出框标题
        if (dlgTitle != null && !"".equals(dlgTitle)) { // 自定义标题
            startActivity(Intent.createChooser(intent, dlgTitle));
        } else { // 系统默认标题
            startActivity(intent);
        }
    }
    /**
     * 分享多图到朋友圈，多张图片加文字
     *
     * @param uris
     */
    private void shareToTimeLine(String title, ArrayList<Uri> uris) {
        Intent intent = new Intent();
        ComponentName comp = new ComponentName("com.tencent.mm", "com.tencent.mm.ui.tools.ShareToTimeLineUI");
        intent.setComponent(comp);
        intent.setAction(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("audio/*");
        intent.putExtra("Kdescription", title);
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        try
        {
            startActivity(intent);
        }
        catch(ActivityNotFoundException e)
        {
            Toast.makeText(this, "微信未安装", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareToWeChat() {
        //创建WXMusicObject对象，用来指定音频Url
        WXMusicObject music =new WXMusicObject();
        music.musicUrl=recorderInfo.getFilepath();
//创建WXMediaMessage对象
        WXMediaMessage msg =new WXMediaMessage();
        msg.mediaObject= music;
        msg.title="分享录音";
        msg.description=recorderInfo.getName();
//设置缩略图
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher);
        Bitmap thumbBmp = Bitmap.createScaledBitmap(bitmap,150,150, true);
        msg.thumbData= bmpToByteArray(thumbBmp, true);
        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.message = msg;
        api.sendReq(req);
    }
    /**
     * 分享图片给好友
     *
     * @param uri
     */
    private void shareToFriend(Uri uri) {
        Intent intent = new Intent();
        ComponentName comp = new ComponentName("com.tencent.mm", "com.tencent.mm.ui.tools.ShareImgUI");
        intent.setComponent(comp);
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("audio");
/*        Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher);
        Bitmap thumbBmp = Bitmap.createScaledBitmap(bitmap,150,150, true);*/
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        try
        {
            startActivity(intent);
        }
        catch(ActivityNotFoundException e)
        {
            Toast.makeText(this, "微信未安装", Toast.LENGTH_SHORT).show();
        }
    }
    private byte[]bmpToByteArray(final Bitmap bitmap, final boolean needRecycle) {

        ByteArrayOutputStream output =new ByteArrayOutputStream();

        bitmap.compress(Bitmap.CompressFormat.PNG,100,output);

        if(needRecycle) bitmap.recycle();

        byte[] result = output.toByteArray();

        try{

            output.close();

        }catch(Exception e) {

            e.printStackTrace();

        }
        return result;
    }

}
