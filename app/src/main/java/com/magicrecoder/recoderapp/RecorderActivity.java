package com.magicrecoder.recoderapp;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.magicrecoder.autoupdate.UpdateChecker;
import com.magicrecoder.greendao.DaoSession;
import com.magicrecoder.greendao.RecorderInfoDao;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.io.FilenameFilter;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;


public class RecorderActivity extends AppCompatActivity {

    private static final String TAG = "Lifecycle";
    //定义数据
    private List<RecorderInfo> recorderData;//listView数据对象
    private RecorderAdapter recorderAdapter;//适配器对象

    private MediaRecorder mRecorder;//录音对象
    private List<File> mTmpFile = new ArrayList<File>();//临时录音文件组
    private int mSeagments =  1;//临时录音文件后缀
    private String Filename;//录音文件绝对路径
    private RecorderInfo AddObject;//添加的录音对象

    private ListView recorderListView;//列表对象
    private ImageView begin_record;
    private ImageView pause_record;
    private ImageView reset_record;
    private Chronometer chronometer;//定义计时器

    long timeWhenStopped = 0;
    private RecorderInfoDao recorderInfoDao;//数据库连接对象
    DaoSession daoSession;//数据库连接session
    private TelephonyManager tManager;
    private MyListener listener;
    Intent intent;
    static String updateUrl = "http://1.shiningrecord.applinzi.com/version.json";

    private long mExitTime;//按下返回键当前时间


    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(msg.what==1){
                Log.d(TAG,"接收到的信息为1，更新list");
                init_recorder_list();
            }
            if(msg.what==2){
                Log.d(TAG,"接收到合成录音成功的消息，保存信息并更新list");
                save_record_info();
                recorderData.add(0,AddObject);//永远在第一个，列表第一个显示
                recorderAdapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recorder);
        intent=getIntent();

        new Thread(new Runnable() {
            @Override
            public void run() {
                initRecorderData();
                Message msg = Message.obtain();
                msg.what = 1;
                mHandler.sendMessage(msg);
                Log.d(TAG,"发送信息，通知UI更新");
            }
        }).start();
        daoSession = ((Recorderapplication) this.getApplicationContext()).daoSession;
        recorderInfoDao = ((Recorderapplication) this.getApplicationContext()).recorderinfoDao;
        recorderListView = (ListView) findViewById(R.id.recorder_ListView) ;
        chronometer = (Chronometer) findViewById(R.id.chronometer);
        begin_record = (ImageView) findViewById(R.id.begin_record);
        pause_record =(ImageView) findViewById(R.id.pause_record);
        reset_record =(ImageView) findViewById(R.id.reset_record);
        //监听电话状态
        tManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        listener = new MyListener();
        tManager.listen(listener,PhoneStateListener.LISTEN_CALL_STATE);
        ClickListener2();
        //setStatusColor();
        //长按菜单弹出操作，注册列表
        registerForContextMenu(recorderListView);
        //添加Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //Toolbar监听
        if (toolbar != null) {
            toolbar.setOnMenuItemClickListener(onMenuItemClick);
        }
        Log.d(TAG, "MainActivity onCreate 创建，执行");
    }
    private class MyListener extends PhoneStateListener {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            try {
                switch (state) {
                    case TelephonyManager.CALL_STATE_IDLE://空闲状态。
                        break;
                    case TelephonyManager.CALL_STATE_RINGING://零响状态。
                        //停止录音
                        record_pause();
                        break;
                    case TelephonyManager.CALL_STATE_OFFHOOK://通话状态
                        //停止录音
                        record_continue();
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    //launchMode为singleTask的时候，通过Intent启到一个Activity,如果系统已经存在一个实例，系统就会将请求发送到这个实例上，
    // 但这个时候，系统就不会再调用通常情况下我们处理请求数据的onCreate方法，而是调用onNewIntent方法，如下所示:
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG,"MainActivity onNewIntent 创建，执行");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "MainActivity onStart 可见 执行");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "MainActivity onResume 获取焦点 执行");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "MainActivity onPause 失去焦点 执行");
    }

    @Override
    public void onStop() {
        super.onStop();
        Integer integer = (Integer) begin_record.getTag();
        integer = integer == null ? 0 : integer;
        if (integer == R.drawable.ic_pause_circle_filled_red_24dp) {
            recordNotification();
        }

        Log.d(TAG, "MainActivity onStop 不可见 执行");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        tManager.listen(listener, PhoneStateListener.LISTEN_NONE);
        if (mTmpFile.size()>0) {
            for (File f : mTmpFile)
                f.delete();
        }
        Log.d(TAG, "MainActivity onDestroy 销毁 执行");
    }

    @Override
    public void onRestart() {
        super.onRestart();
        int beforeNum = recorderData.size();
        //daoSession.clear();
        int afterNum = recorderInfoDao.queryBuilder().where(RecorderInfoDao.Properties.Id.notEq(-1)).orderDesc(RecorderInfoDao.Properties.Id).build().list().size();
        Log.d(TAG,"beforeNum is "+beforeNum+"afterNum is "+afterNum);
        if (beforeNum>afterNum) {
            recorderData.clear();
            recorderData.addAll(recorderInfoDao.queryBuilder().where(RecorderInfoDao.Properties.Id.notEq(-1)).orderDesc(RecorderInfoDao.Properties.Id).build().list());
            recorderAdapter.notifyDataSetChanged();
            Log.d(TAG, "MainActivity onRestart 重新打开 执行");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "MainActivity onSaveInstanceState 保存数据");
    }

    @Override
    public void onRestoreInstanceState(Bundle saveInstanceState) {
        super.onRestoreInstanceState(saveInstanceState);
        Log.d(TAG, "MainActivity onRestoreInstanceState 保存数据");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /*toolbar menu监听*/
    private Toolbar.OnMenuItemClickListener onMenuItemClick = new Toolbar.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem menuitem) {
            switch (menuitem.getItemId()) {
                case R.id.more_action:
                    Log.d(TAG, "点击菜单");
                    return true;
                case R.id.search_bar:
                    Log.d(TAG, "搜索");
                    return true;
                case R.id.menu_more:
                    return true;
                case R.id.menu_help:
                    Log.d(TAG, "帮助");
                    final AlertDialog helpDialog = new AlertDialog.Builder(RecorderActivity.this).create();
                    final View helpView = View.inflate(RecorderActivity.this, R.layout.help_dialog, null);
                    helpDialog.setView(helpView);
                    final TextView back = (TextView) helpView.findViewById(R.id.back);
                    final ImageView image = (ImageView) helpView.findViewById(R.id.loveImage);
                    image.setImageResource(R.drawable.help_pic);
                    back.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            helpDialog.dismiss();
                        }
                    });
                    helpDialog.show();
                    return true;
                case R.id.menu_update:
                    Log.d(TAG, "软件更新");
                    UpdateChecker.checkForDialog(RecorderActivity.this);
                    return true;
                case R.id.menu_import_his_recorder:
                    Log.d(TAG, "加载历史文件");
                    import_history_recorder();
                    return true;
            }
            return false;
        }
    };
    /*设置状态栏颜色*/
    private void setStatusColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintResource(R.color.color_toolbar);
            tintManager.setStatusBarTintEnabled(true);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            Context context = getApplicationContext();
            int color = ContextCompat.getColor(context, R.color.color_toolbar);
            window.setStatusBarColor(color);
        }
    }

    /*初始化列表数据*/
    private void initRecorderData() {
        if (recorderData == null) {
            recorderData = new ArrayList<>();
            Log.d(TAG, "初始化数据库连接");
            //按照插入时间倒序排序，也就是说时间晚的会在前面显示
            recorderData = recorderInfoDao.queryBuilder().where(RecorderInfoDao.Properties.Id.notEq(-1)).orderDesc(RecorderInfoDao.Properties.Id).build().list();
            Log.d(TAG, "初始化列表数据结束");
        }
    }
    //初始化列表
    private void init_recorder_list() {
        //列表非空，且适配器为空才初始化
        if (recorderListView != null && recorderAdapter == null) {
            LayoutInflater inflater = getLayoutInflater();
            //initRecorderData();
            Log.d(TAG,"绑定适配器数据");
            recorderAdapter = new RecorderAdapter(inflater, recorderData);
            Log.d(TAG,"列表绑定适配器");
            recorderListView.setAdapter(recorderAdapter);
            //单击监听
            recorderListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Integer integer = (Integer) begin_record.getTag();
                    integer = integer == null ? 0 : integer;
                    if (integer == R.drawable.ic_pause_circle_filled_red_24dp) {
                        record_stop();
                    }
                    RecorderInfo recorderObject = recorderData.get(position);
                    File audioFile = new File(recorderObject.getFilepath());
                    if (audioFile.exists()) {
                        Log.d(TAG,"要开始播放了");
                        playAudio(recorderObject);
                    } else {
                        Toast.makeText(getBaseContext(), "录音文件不存在", Toast.LENGTH_SHORT).show();
                        if (recorderObject.getId() != null) {
                            recorderInfoDao.deleteByKey(recorderObject.getId());//删除数据库对象
                            recorderData.remove(recorderObject);
                            recorderAdapter.notifyDataSetChanged();
                        }
                    }
                }
            });
            //5.点击更多图标监听，实现接口setIconClickListener
            recorderAdapter.setIconClickListener(new RecorderAdapter.OnIconClickListener() {
                @Override
                public void onIconClick(final int position, View v) {
                    Log.d(TAG, "点击位置为" + position + "图标为");
                    final AlertDialog dialog = new AlertDialog.Builder(RecorderActivity.this).create();
                    final View dialogView = View.inflate(RecorderActivity.this, R.layout.custom_dialog, null);
                    final EditText editText = (EditText) dialogView.findViewById(R.id.fileName);
                    final TextView dialogTitle = (TextView) dialogView.findViewById(R.id.dialogTitle);
                    final TextView mBtnOK = (TextView) dialogView.findViewById(R.id.btnOK);
                    final TextView mBtnNO = (TextView) dialogView.findViewById(R.id.btnNO);

                    PopupMenu actionMenu = new PopupMenu(RecorderActivity.this, v);
                    getMenuInflater().inflate(R.menu.recorder_menu, actionMenu.getMenu());
                    actionMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        RecorderInfo recorderObject = recorderData.get(position);
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.delRecorder:
                                    if (recorderObject.getId() != null) {
                                        Log.d(TAG, "" + recorderObject.getId());
                                        recorderInfoDao.deleteByKey(recorderObject.getId());//删除数据库对象
                                        recorderData.remove(recorderObject);
                                        recorderAdapter.notifyDataSetChanged();
                                    }
                                    if (removeAudioFile(recorderObject.getFilepath()))//删除文件
                                    {
                                        Toast.makeText(getBaseContext(), "删除文件成功", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(getBaseContext(), "删除文件失败", Toast.LENGTH_SHORT).show();
                                    }
                                    return true;
                                case R.id.modifyName:
                                    dialogTitle.setText("修改名称");
                                    editText.setText(recorderObject.getName());
                                    editText.selectAll();
                                    editText.setFocusable(true);
                                    editText.setFocusableInTouchMode(true);
                                    editText.requestFocus();
                                    editText.selectAll();
                                    Timer timer = new Timer();
                                    timer.schedule(new TimerTask() {
                                        public void run() {
                                            InputMethodManager inputManager = (InputMethodManager) editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                                            inputManager.showSoftInput(editText, 0);
                                        }
                                    }, 200);
                                    mBtnOK.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            String newName = editText.getText().toString();
                                            Log.d(TAG, newName);
                                            Log.d(TAG, "索引为" + position);
                                            if (newName.equals(recorderObject.getName())) {
                                                Toast.makeText(getBaseContext(), "名称未改动", Toast.LENGTH_SHORT).show();
                                                editText.selectAll();
                                            }
                                            else {
                                                int count = recorderInfoDao.queryBuilder().where(RecorderInfoDao.Properties.Name.eq(newName)).build().list().size();
                                                if (count >= 1) {
                                                    Toast.makeText(getBaseContext(), "有重名录音", Toast.LENGTH_SHORT).show();
                                                    editText.selectAll();
                                                } else {
                                                    recorderObject.setName(newName);
                                                    recorderInfoDao.update(recorderObject);
                                                    recorderData.get(position).setName(newName);
                                                    updateView(position);//
                                                    //recorderAdapter.notifyDataSetChanged();
                                                    Toast.makeText(getBaseContext(), "修改成功", Toast.LENGTH_SHORT).show();
                                                    dialog.dismiss();
                                                }
                                            }
                                        }
                                    });
                                    mBtnNO.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            dialog.dismiss();
                                        }
                                    });
                                    dialog.setView(dialogView);
                                    dialog.show();
                                    Log.d(TAG, "修改记录");
                                    return true;
                                case R.id.modifyInfo:
                                    dialogTitle.setText("修改备注");
                                    editText.setText(recorderObject.getInfo());
                                    editText.setFocusable(true);
                                    editText.setFocusableInTouchMode(true);
                                    editText.requestFocus();
                                    editText.selectAll();
                                    Timer timer2 = new Timer();
                                    timer2.schedule(new TimerTask() {
                                        public void run() {
                                            InputMethodManager inputManager = (InputMethodManager) editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                                            inputManager.showSoftInput(editText, 0);
                                        }
                                    }, 200);
                                    mBtnOK.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            String newInfo = editText.getText().toString();
                                            Log.d(TAG, newInfo);
                                            recorderObject.setInfo(newInfo);
                                            recorderInfoDao.update(recorderObject);
                                            recorderData.get(position).setInfo(newInfo);
                                            updateView(position);//只刷新一条
                                            //recorderAdapter.notifyDataSetChanged();//会全部刷新
                                            Toast.makeText(getBaseContext(), "修改成功", Toast.LENGTH_SHORT).show();
                                            dialog.dismiss();
                                        }
                                    });
                                    mBtnNO.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            dialog.dismiss();
                                        }
                                    });
                                    dialog.setView(dialogView);
                                    dialog.show();
                                    Log.d(TAG, "添加备注");
                                    return true;
                                default:
                                    return false;
                            }
                        }
                    });
                    actionMenu.show();
                }
            });
        }
        else{
            Log.d(TAG,"recorderAdapter非空,无需更新");
        }
    }

    /* 按钮监听*/
    private void ClickListener2() {
        assert begin_record != null;
        begin_record.setImageResource(R.drawable.ic_radio_button_checked_red_24dp);
        begin_record.setOnClickListener(new ImageView.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Integer integer = (Integer) begin_record.getTag();
                integer = integer == null ? 0 : integer;
                switch (integer) {
                    default:
                    case R.drawable.ic_radio_button_checked_red_24dp:
                        record_start();
                        break;
                    case R.drawable.ic_pause_circle_filled_red_24dp:
                        record_stop();
                        break;
                }
            }
        });
        pause_record.setVisibility(View.INVISIBLE);
        pause_record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Integer integer = (Integer) pause_record.getTag();
                integer = integer == null ? 0 : integer;
                switch (integer) {
                    default:
                    case R.drawable.ic_pause_red_24dp:
                        record_pause();
                        break;
                    case R.drawable.ic_continue_red_24dp:
                        record_continue();
                        break;
                }
            }
        });
        reset_record.setVisibility(View.INVISIBLE);
        reset_record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reset_record();
            }
        });

    }

    //长按列表弹出操作
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        //menu.setHeaderTitle("操作菜单");
        //添加菜单项
        menu.add(0, 0, 0, "删除记录");
        menu.add(0, 1, 0, "修改名称");
        menu.add(0, 2, 0, "添加备注");
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    //给菜单项添加事件
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        final int position = (int) recorderListView.getAdapter().getItemId(info.position);//获取点击listView的索引位置
        final AlertDialog dialog = new AlertDialog.Builder(this).create();
        final View dialogView = View.inflate(this, R.layout.custom_dialog, null);
        final EditText editText = (EditText) dialogView.findViewById(R.id.fileName);
        final TextView dialogTitle=(TextView) dialogView.findViewById(R.id.dialogTitle);
        final TextView mBtnOK = (TextView) dialogView.findViewById(R.id.btnOK);
        final TextView mBtnNO = (TextView) dialogView.findViewById(R.id.btnNO);
        final RecorderInfo recorderObject= recorderData.get(position);
        switch (item.getItemId()) {
            case 0:
                if (recorderObject.getId() != null) {
                    Log.d(TAG,""+recorderObject.getId());
                    recorderInfoDao.deleteByKey(recorderObject.getId());//删除数据库对象
                    recorderData.remove(recorderObject);
                    recorderAdapter.notifyDataSetChanged();
                }
                if(removeAudioFile(recorderObject.getFilepath()))//删除文件
                {
                    Toast.makeText(getApplicationContext(),"删除文件成功",Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(getApplicationContext(),"删除文件失败",Toast.LENGTH_SHORT).show();
                }
                return true;
            case 1:
                dialogTitle.setText("修改名称");
                editText.setText(recorderObject.getName());
                editText.selectAll();
                mBtnOK.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String newName = editText.getText().toString();
                        Log.d(TAG, newName);
                        Log.d(TAG, "索引为" + position);
                        if (newName.equals(recorderObject.getName())) {
                            Toast.makeText(getBaseContext(), "名称未改动", Toast.LENGTH_SHORT).show();
                            editText.selectAll();
                        } else {
                            int count = recorderInfoDao.queryBuilder().where(RecorderInfoDao.Properties.Name.eq(newName)).build().list().size();
                            if (count >= 1) {
                                Toast.makeText(getBaseContext(), "有重名录音", Toast.LENGTH_SHORT).show();
                                editText.selectAll();
                            } else {
                                recorderObject.setName(newName);
                                recorderInfoDao.update(recorderObject);
                                recorderData.get(position).setName(newName);
                                updateView(position);
                                //recorderAdapter.notifyDataSetChanged();
                                Toast.makeText(getBaseContext(), "修改成功", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            }
                        }
                    }
                });
                mBtnNO.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
                dialog.setView(dialogView);
                dialog.show();
                Log.d(TAG, "修改记录");
                return true;
            case 2:
                dialogTitle.setText("修改备注");
                editText.setText(recorderObject.getInfo());
                editText.selectAll();
                mBtnOK.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String newInfo = editText.getText().toString();
                        Log.d(TAG, newInfo);
                        recorderObject.setInfo(newInfo);
                        recorderInfoDao.update(recorderObject);
                        recorderData.get(position).setInfo(newInfo);
                        updateView(position);
                        Toast.makeText(getBaseContext(), "修改成功", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                });
                mBtnNO.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
                dialog.setView(dialogView);
                dialog.show();
                Log.d(TAG, "添加备注");
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    /* 播放录音文件 */
    private void playAudio(RecorderInfo recorderObject) {
        Intent intent = new Intent(this, AudioPlayActivity.class);
        Log.d(TAG,"放入的文件名为"+recorderObject.getFilepath());
        intent.putExtra("recorder",recorderObject);
        startActivity(intent);
    }

    private void updateView(int itemIndex) {
        //得到第一个可显示控件的位置，
        int visiblePosition = recorderListView.getFirstVisiblePosition();
        //只有当要更新的view在可见的位置时才更新，不可见时，跳过不更新
        if (itemIndex - visiblePosition >= 0) {
            //得到要更新的item的view
            View view = recorderListView.getChildAt(itemIndex - visiblePosition);
            //调用adapter更新界面
            recorderAdapter.updateView(view, itemIndex);
        }
    }
    //删除文件
    private boolean removeAudioFile(String Filename) {
        File AudioFile = new File(Filename);
        return AudioFile.delete();
    };
    //导入历史录音
    private void import_history_recorder() {
        File dir = getRecordDir();
        int count = 0;
        int add = 0;
        try {
            String[] FileList = dir.list(new MusicFilter());
            for (int i = 0; i < FileList.length; i++) {
                File amrFile=new File(dir,FileList[i]);
                Log.d(TAG,amrFile.getName());
                if(amrFile.exists()) {
                    Date date= new Date();
                    Long fileTs = amrFile.lastModified();
                    if( fileTs < date.getTime()) {
                        count = recorderInfoDao.queryBuilder().where(RecorderInfoDao.Properties.Filepath.eq(amrFile.getPath())).build().list().size();
                        if (count == 0) {
                            int icon=R.drawable.ic_play_circle_filled_red_48dp;
                            String name=amrFile.getName().substring(0,amrFile.getName().lastIndexOf("."));
                            String often=GetFilePlayTime(amrFile);
                            String info="添加备注";
                            Date create_date=new Date(amrFile.lastModified());
                            DateFormat format = new SimpleDateFormat("MM月dd日",Locale.getDefault());
                            String create_time=format.format(create_date);
                            String create_user="诗宁";
                            String tag="0";
                            int action=R.drawable.ic_expand_more_grep_24dp;
                            RecorderInfo recorderInfo=new RecorderInfo(null,amrFile.getPath(),icon,name,often,info,create_time,create_user,tag,action);
                            Log.d(TAG,"添加录音对象");
                            recorderInfoDao.insert(recorderInfo);
                            add = add + 1;
                            //recorderData.add(recorderInfo);
                        }
                    }
                }
            }
        }catch (NullPointerException e){
            e.printStackTrace();
        }
        //recorderAdapter.notifyDataSetChanged();
        //daoSession.clear();
        if (add > 0) {
            recorderData.clear();
            recorderData.addAll(recorderInfoDao.queryBuilder().where(RecorderInfoDao.Properties.Id.notEq(-1)).orderDesc(RecorderInfoDao.Properties.Id).build().list());
            recorderAdapter.notifyDataSetChanged();
        }
        Toast.makeText(getBaseContext(),"导入历史录音"+add+"条",Toast.LENGTH_SHORT).show();
    }

    /*开播录音*/
    private void record_start() {
        begin_record.setImageResource(R.drawable.ic_pause_circle_filled_red_24dp);
        begin_record.setTag(R.drawable.ic_pause_circle_filled_red_24dp);

        pause_record.setImageResource(R.drawable.ic_pause_red_24dp);
        pause_record.setTag(R.drawable.ic_pause_red_24dp);
        pause_record.setVisibility(View.VISIBLE);

        reset_record.setImageResource(R.drawable.ic_reset_red_24dp);
        reset_record.setVisibility(View.VISIBLE);

        int color = ContextCompat.getColor(getBaseContext(), R.color.color_black);
        chronometer.setTextColor(color);
        chronometer.setFormat(null);
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();


        Filename = "录音"+ time_format.format(new Date());
        startRecording();
        Log.d(TAG,"开始录音");
    }
    //暂停录音
    private void record_pause() {
        pause_record.setImageResource(R.drawable.ic_continue_red_24dp);
        pause_record.setTag(R.drawable.ic_continue_red_24dp);
        pause_record.setVisibility(View.VISIBLE);

        int color2 = ContextCompat.getColor(getBaseContext(), R.color.color_grey);
        chronometer.setTextColor(color2);
        timeWhenStopped = chronometer.getBase() - SystemClock.elapsedRealtime();
        chronometer.setFormat(null);
        chronometer.stop();

        pauseRecording();
        Log.d(TAG,"暂停录音");
    }
    //继续录音
    private  void record_continue() {
        pause_record.setImageResource(R.drawable.ic_pause_red_24dp);
        pause_record.setTag(R.drawable.ic_pause_red_24dp);
        pause_record.setVisibility(View.VISIBLE);

        int color = ContextCompat.getColor(getBaseContext(), R.color.color_black);
        chronometer.setTextColor(color);
        chronometer.setFormat(null);
        chronometer.setBase(SystemClock.elapsedRealtime() + timeWhenStopped);
        chronometer.start();
        Log.d(TAG,"暂停录音");
        startRecording();
    }
    //结束录音
    private void record_stop(){
        pause_record.setVisibility(View.INVISIBLE);
        reset_record.setVisibility(View.INVISIBLE);

        begin_record.setImageResource(R.drawable.ic_radio_button_checked_red_24dp);
        begin_record.setTag(R.drawable.ic_radio_button_checked_red_24dp);

        int color2 = ContextCompat.getColor(getBaseContext(), R.color.color_grey);

        chronometer.setTextColor(color2);
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.stop();
        timeWhenStopped = 0;
        stopRecording();
    }
    //重置录音
    private void reset_record() {
        pause_record.setVisibility(View.INVISIBLE);
        reset_record.setVisibility(View.INVISIBLE);

        begin_record.setImageResource(R.drawable.ic_radio_button_checked_red_24dp);
        begin_record.setTag(R.drawable.ic_radio_button_checked_red_24dp);

        int color2 = ContextCompat.getColor(getBaseContext(), R.color.color_grey);
        chronometer.setTextColor(color2);
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.stop();
        timeWhenStopped = 0;

        pauseRecording();
        for (File f : mTmpFile)
            f.delete();
        mTmpFile.clear();
        mSeagments = 1;
    }

    private void startRecording(){
        Filename = "录音"+ time_format.format(new Date());
        //File file = new File(FileUtils.getAmrFilePath(Filename)+mSeagments);
        File file = new File(getRecordDir(),Filename+".amr"+mSeagments);
        mTmpFile.add(file);
        mSeagments++;
        if(file.exists()){
            if(file.delete())
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }else{
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);//
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mRecorder.setOutputFile(file.getAbsolutePath());
        mRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener(){
            @Override
            public void onError(MediaRecorder mr, int what, int extra) {
                mRecorder.reset();
            }
        });
        try {
            mRecorder.prepare();
            mRecorder.start();
        } catch (Exception e) {
            e.printStackTrace();
            mRecorder.release();
        }finally{
        }
    }
    private void pauseRecording(){
        if(mRecorder!=null){
            Log.d(TAG,"暂停录音");
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
    }
    private void stopRecording(){
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
        //final File finalFile = new File(FileUtils.getAmrFilePath(Filename));
        final File finalFile = new File(getRecordDir(),Filename+".amr");
        if (!finalFile.exists()) {
            try {
                finalFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                FileOutputStream fileOutputStream = null;
                try {
                    fileOutputStream = new FileOutputStream(finalFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                for (int i = 0; i < mTmpFile.size(); i++) {
                    File tmpFile = mTmpFile.get(i);
                    FileInputStream fis = null;
                    try {
                        fis = new FileInputStream(tmpFile);
                        byte[] tmpBytes = new byte[fis.available()];
                        int lenght = tmpBytes.length;
                        if (i == 0) {
                            while (fis.read(tmpBytes) != -1) {
                                fileOutputStream.write(tmpBytes, 0, lenght);
                            }
                        } else {
                            while (fis.read(tmpBytes) != -1) {
                                fileOutputStream.write(tmpBytes, 6, lenght - 6);
                            }
                        }
                        fileOutputStream.flush();
                        fis.close();
                        Log.d(TAG,"共合成的录音文件为"+tmpFile.getName());
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        fis = null;
                    }
                }
                Log.d(TAG,"合成录音进程结束");
                if(finalFile.exists()){
                    Message msg =Message.obtain();
                    msg.what=2;
                    mHandler.sendMessage(msg);
                }
                try {
                    if (fileOutputStream != null)
                        fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    fileOutputStream = null;
                    for (File f : mTmpFile)
                        f.delete();
                    mTmpFile.clear();
                    mSeagments = 1;
                    Log.d(TAG,"合成录音进程结束，初始化参数");
                }
            }
        }).start();
    }

    //保存录音信息，并刷新列表
    private void save_record_info() {
        if (recorderListView != null) {
            File dir = getRecordDir();
            String FileName=Filename + ".amr";
            File File = new File(dir, FileName);
            String filepath=File.getPath();
            Log.d(TAG, "文件绝对路径"+filepath);
            int icon=R.drawable.ic_play_circle_filled_red_48dp;
            String name=Filename.substring(0,FileName.lastIndexOf("."));
            String often = GetFilePlayTime(File);
            Log.d(TAG,"录音时常为"+often);
            String info="添加备注";
            Date create_date=new Date(File.lastModified());
            DateFormat format = new SimpleDateFormat("MM月dd日",Locale.getDefault());
            String create_time=format.format(create_date);
            String create_user="诗宁";
            String tag="0";
            int action=R.drawable.ic_expand_more_grep_24dp;
            AddObject=new RecorderInfo(null,filepath,icon,name,often,info,create_time,create_user,tag,action);
            recorderInfoDao.insert(AddObject);
/*            recorderData.add(0,recorderInfo);//永远在第一个，列表第一个显示
            recorderAdapter.notifyDataSetChanged();*/
        }
    }
    /*获取录音时常*/
    private String GetFilePlayTime(File file) {
        Date date;
        Date oneHour;
        SimpleDateFormat sy1,sy2;
        String dateFormat = "error";
        try {
            sy1 = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());//设置为时分秒的格式
            sy2 = new SimpleDateFormat("mm:ss", Locale.getDefault());//设置为时分秒的格式

            //使用媒体库获取播放时间
            MediaPlayer mediaPlayer;
            mediaPlayer = MediaPlayer.create(getBaseContext(), Uri.parse(file.getPath()));

            //使用Date格式化播放时间mediaPlayer.getDuration()
            date = sy1.parse("00:00:00");
            oneHour = sy1.parse("01:00:00");
            date.setTime(mediaPlayer.getDuration() + date.getTime());//用消除date.getTime()时区差
            if (date.getTime()<oneHour.getTime()) {
                dateFormat = sy2.format(date);
            }
            else {
                dateFormat = sy1.format(date);
            }
            mediaPlayer.release();

        } catch (ParseException e) {
            e.printStackTrace();
        }
        return dateFormat;
    }
    /*录音文件默认保存格式*/
    private static SimpleDateFormat time_format = new SimpleDateFormat("HHmmss", Locale.getDefault());//24小时制
    /*录音文件存放路径*/
    @Nullable
    private File getRecordDir() {
        if (sdcardIsValid()) {
            String path = Environment.getExternalStorageDirectory() + "/record";
            File dir = new File(path);
            if (!dir.exists()) {
                dir.mkdir();
            }
            return dir;
        } else {
            return null;
        }
    }
    /*判断是否有SD卡*/
    private boolean sdcardIsValid() {
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            return true;
        } else {
            Toast.makeText(getBaseContext(), "没有SD卡", Toast.LENGTH_LONG).show();
        }
        return false;
    }
    //通知正在录音
    private void recordNotification() {
        //创建大图标的Bitmap
        Bitmap LargeBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        NotificationCompat.Builder mBuilder =
                (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                        .setLargeIcon(LargeBitmap)
                        .setSmallIcon(R.drawable.ic_pause_circle_filled_red_24dp)
                        .setContentTitle("正在录音,点击返回")
                        .setContentText(Filename)
                        .setAutoCancel(true);
        Intent resultIntent = new Intent(this, RecorderActivity.class);
        resultIntent.setAction(Intent.ACTION_MAIN);
        resultIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, (int) SystemClock.uptimeMillis(), resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);

        mBuilder.setFullScreenIntent(resultPendingIntent,true);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
// mId allows you to update the notification later on.
        mNotificationManager.notify(1, mBuilder.build());
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Integer integer = (Integer) begin_record.getTag();
        integer = integer == null ? 0 : integer;
        if (keyCode == KeyEvent.KEYCODE_BACK && integer == R.drawable.ic_pause_circle_filled_red_24dp) {
            if ((System.currentTimeMillis() - mExitTime) > 2000) {// 如果两次按键时间间隔大于2000毫秒，则不退出
                Toast.makeText(this, "正在录音,再按一次退出", Toast.LENGTH_SHORT).show();
                mExitTime = System.currentTimeMillis();// 更新mExitTime
            } else {
                reset_record();
                finish();
                //System.exit(0);// 否则退出程序
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /* 过滤文件类型 */
    class MusicFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return (name.endsWith(".amr"));
        }
    }
}
