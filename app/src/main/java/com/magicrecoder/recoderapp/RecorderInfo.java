package com.magicrecoder.recoderapp;

import android.os.Parcel;
import android.os.Parcelable;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Index;
import org.greenrobot.greendao.annotation.Keep;
import org.greenrobot.greendao.annotation.Unique;

/**
 * Created by Administrator on 2016-09-05.
 */
@Entity
public class RecorderInfo implements Parcelable {
    @Id(autoincrement = true)
    private  Long id;
    @Index
    private String filepath;//录音文件路径
    private int icon;  //录音图标
    @Unique
    private String name;//录音名字
    private String often;//录音时常
    private String info;//录音信息
    private String create_time;//录音创建时间
    private String create_user;//创建者
    private String tag;//录音标签
    private int action;
    @Keep
    public RecorderInfo(Long id, String filepath, int icon, String name,
            String often, String info, String create_time, String create_user,
            String tag, int action) {
        this.id = id;
        this.filepath = filepath;
        this.icon = icon;
        this.name = name;
        this.often = often;
        this.info = info;
        this.create_time = create_time;
        this.create_user = create_user;
        this.tag = tag;
        this.action=action;
    }

    protected RecorderInfo(Parcel in) {
        id = in.readLong();
        filepath = in.readString();
        icon = in.readInt();
        name = in.readString();
        often = in.readString();
        info = in.readString();
        create_time = in.readString();
        create_user = in.readString();
        tag = in.readString();
        action = in.readInt();
    }

    @Generated(hash = 1817692510)
    public RecorderInfo() {
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(filepath);
        dest.writeInt(icon);
        dest.writeString(name);
        dest.writeString(often);
        dest.writeString(info);
        dest.writeString(create_time);
        dest.writeString(create_user);
        dest.writeString(tag);
        dest.writeInt(action);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<RecorderInfo> CREATOR = new Creator<RecorderInfo>() {
        @Override
        public RecorderInfo createFromParcel(Parcel in) {
            return new RecorderInfo(in);
        }

        @Override
        public RecorderInfo[] newArray(int size) {
            return new RecorderInfo[size];
        }
    };

    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getFilepath() {
        return this.filepath;
    }
    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }
    public int getIcon() {
        return this.icon;
    }
    public void setIcon(int icon) {
        this.icon = icon;
    }
    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getOften() {
        return this.often;
    }
    public void setOften(String often) {
        this.often = often;
    }
    public String getInfo() {
        return this.info;
    }
    public void setInfo(String info) {
        this.info = info;
    }
    public String getCreate_time() {
        return this.create_time;
    }
    public void setCreate_time(String create_time) {
        this.create_time = create_time;
    }
    public String getCreate_user() {
        return this.create_user;
    }
    public void setCreate_user(String create_user) {
        this.create_user = create_user;
    }
    public String getTag() {
        return this.tag;
    }
    public void setTag(String tag) {
        this.tag = tag;
    }
    public int getAction() {
        return this.action;
    }
    public void setAction(int action) {
        this.action = action;
    }
}
