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
public class PlayListAdapter extends BaseAdapter {
    private static final String TAG = "Lifecycle";
    private List<RecorderInfo> mData;//定义数据
    private LayoutInflater mInflater;//定义Inflater,加载自定义布局
    public PlayListAdapter(LayoutInflater inflater, List<RecorderInfo> data){
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

    //在外面先定义，ViewHolder静态类
    static class ViewHolder
    {
        public ImageView icon;
        public TextView name;
        public TextView often;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup){
        Log.d(TAG,"正在绘制item"+"位置为"+position);
        ViewHolder holder = null;
        RecorderInfo recorder = mData.get(position);
        if(convertView == null) {
            holder = new ViewHolder();
            //根据自定义的Item布局加载布局
            convertView = mInflater.inflate(R.layout.playlist, null);
            //获取自定义布局中每一个控件的对象
            holder.icon = (ImageView) convertView.findViewById(R.id.play_icon);
            holder.name = (TextView) convertView.findViewById(R.id.play_name);
            holder.often = (TextView) convertView.findViewById(R.id.play_often);
            convertView.setTag(holder);//非常重要
        }
        else
        {
            holder = (ViewHolder)convertView.getTag();//非常重要
        }
        //将数据一一添加到自定义的布局中
        holder.icon.setImageResource(recorder.getIcon());
        holder.name.setText(recorder.getName());
        holder.often.setText(recorder.getOften());
        return convertView;
    }
}
