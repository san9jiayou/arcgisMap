package com.woozoom.maplibrary;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

/**
 * Created by zhangjian on 2018/10/17.
 */

public class Util {
    public static final String MAPTYPE = "ArcgisMapType";//0地图，1卫星，
    public static final String BASEPATH = Environment.getExternalStorageDirectory().getAbsolutePath()+"/agriPP/";
    public static String ADVERTNAME = BASEPATH + "map/tile/";
    private static final String WOOZOOM_SHARE = "woozoom_share";

    public static void clearShareData(Context context) {
        SharedPreferences.Editor sharedata = context.getSharedPreferences(WOOZOOM_SHARE, 0).edit();
        sharedata.clear();
        sharedata.commit();
    }

    public static void setIntShareData(Context context,String tag, int data) {
        SharedPreferences.Editor sharedata = context.getSharedPreferences(WOOZOOM_SHARE, 0).edit();
        sharedata.putInt(tag, data);
        sharedata.commit();
    }

    public static int getIntShareData(Context context,String tag,int defaultStr) {
        SharedPreferences sharedata = context.getSharedPreferences(WOOZOOM_SHARE, 0);
        int data = sharedata.getInt(tag, defaultStr);
        return data;
    }
    public static void setStringShareData(Context context,String tag, String data) {
        SharedPreferences.Editor sharedata = context.getSharedPreferences(WOOZOOM_SHARE, 0).edit();
        sharedata.putString(tag, data);
        sharedata.commit();
    }

    public static String getStringShareData(Context context,String tag,String defaultStr) {
        SharedPreferences sharedata = context.getSharedPreferences(WOOZOOM_SHARE, 0);
        String data = sharedata.getString(tag, defaultStr);
        return data;
    }
}
