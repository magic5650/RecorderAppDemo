package com.magicrecoder.recoderapp;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;

/**
 * Created by Administrator on 2016-09-06.
 */
public class RecorderAdapter extends BaseAdapter {
    private static final String TAG = "Lifecycle";
    private List<RecorderInfo> mData;//定义数据
    private LayoutInflater mInflater;//定义Inflater,加载自定义布局
    public RecorderAdapter(LayoutInflater inflater,List<RecorderInfo> data){
        mInflater = inflater;
        mData = data;
    }
    @Override
    public int getCount(){
        return mData.size();
    }
    @Override
    public  Object getItem(int position){
        return position;
    }
    @Override
    public long getItemId(int position){
        return position;
    }

    public void refresh(List<RecorderInfo> Data) {
        mData = Data;
        notifyDataSetChanged();
    }

    /**
     * 局部刷新
     * @param convertView
     * @param itemIndex
     */
    public void updateView(View convertView, int itemIndex) {
        if(convertView == null) {
            return;
        }
        //从convertView中取得holder
        ViewHolder holder = (ViewHolder) convertView.getTag();
        holder.id = (TextView) convertView.findViewById(R.id.id);
        holder.icon = (ImageView) convertView.findViewById(R.id.icon);
        holder.name = (TextView) convertView.findViewById(R.id.name);
        holder.often = (TextView) convertView.findViewById(R.id.often);
        holder.info = (TextView) convertView.findViewById(R.id.info);
        holder.create_time = (TextView) convertView.findViewById(R.id.create_time);
        holder.action = (ImageView) convertView.findViewById(R.id.action);

        RecorderInfo recorder = mData.get(itemIndex);
        String id=""+recorder.getId();
        holder.id.setText(id);
        holder.icon.setImageResource(recorder.getIcon());
        holder.name.setText(recorder.getName());
        holder.often.setText(recorder.getOften());
        holder.info.setText(recorder.getInfo());
        holder.create_time.setText(recorder.getCreate_time());
        holder.action.setImageResource(recorder.getAction());
        Log.d(TAG,"正在updateView,位置为"+itemIndex);
    }
    //在外面先定义，ViewHolder静态类
    static class ViewHolder
    {
        public TextView id;
        public ImageView icon;
        public TextView name;
        public TextView often;
        public TextView info;
        public TextView create_time;
        public ImageView action;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup){
        //Log.d(TAG,"正在绘制item"+"位置为"+position);
        ViewHolder holder = null;
        RecorderInfo recorder = mData.get(position);
        //如果缓存convertView为空，则需要创建View
        if(convertView == null) {
            holder = new ViewHolder();
            //根据自定义的Item布局加载布局
            convertView = mInflater.inflate(R.layout.recorderlist, null);
            //获取自定义布局中每一个控件的对象
            holder.id = (TextView) convertView.findViewById(R.id.id);
            holder.icon = (ImageView) convertView.findViewById(R.id.icon);
            holder.name = (TextView) convertView.findViewById(R.id.name);
            holder.often = (TextView) convertView.findViewById(R.id.often);
            holder.info = (TextView) convertView.findViewById(R.id.info);
            holder.create_time = (TextView) convertView.findViewById(R.id.create_time);
            holder.action = (ImageView) convertView.findViewById(R.id.action);
            convertView.setTag(holder);//非常重要
        }
        else
        {
            holder = (ViewHolder)convertView.getTag();//非常重要
        }
        //将数据一一添加到自定义的布局中
        String id=""+recorder.getId();
        holder.id.setText(id);
        holder.icon.setImageResource(recorder.getIcon());
        holder.name.setText(recorder.getName());
        holder.often.setText(recorder.getOften());
        holder.info.setText(recorder.getInfo());
        holder.create_time.setText(recorder.getCreate_time());
        holder.action.setImageResource(recorder.getAction());
        holder.action.setTag(position);
        holder.action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {//4创建系统点击监听事件
                if (ClinkListener != null){
                    int position = (Integer) view.getTag();
                    View v = view;
                    ClinkListener.onIconClick(position,v);
                }
            }
        });
        return convertView;
    }
    // 1.0定义按钮接口
    public interface OnIconClickListener {
        public void onIconClick(int position, View view);// 设置一个方法,Activity调用时可以获取按钮对象和按钮位置
    }
    private OnIconClickListener ClinkListener;//2初始化监听器
    public void setIconClickListener(OnIconClickListener ClinkListener){//3提供Set方法让Activity调用
        this.ClinkListener=ClinkListener;
    }
}
