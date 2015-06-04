package com.sohu.kuaizhan.sitebuilder.utils;

import android.content.Context;
import android.content.SharedPreferences;

public abstract class BaseSpUtil {

    private static final String SEPARATOR = "_";

    private Context mContext;
    private String mName;
    private int mMode;

    protected BaseSpUtil(Context mContext, String mName, int mMode) {
        this.mContext = mContext;
        this.mName = mContext.getPackageName() + SEPARATOR + mName;
        this.mMode = mMode;
    }

    // get String
    protected String getString(String key) {
        SharedPreferences sp = mContext.getSharedPreferences(mName, mMode);
        return sp.getString(key, "");
    }

    // put String
    protected boolean putString(String key, String value) {
        SharedPreferences sp = mContext.getSharedPreferences(mName, mMode);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(key, value);
        return editor.commit();
    }

    // get Boolean
    protected Boolean getBoolean(String key) {
        SharedPreferences sp = mContext.getSharedPreferences(mName, mMode);
        return sp.getBoolean(key, false);
    }

    // put Boolean
    protected boolean putBoolean(String key, Boolean value) {
        SharedPreferences sp = mContext.getSharedPreferences(mName, mMode);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(key, value);
        return editor.commit();
    }

    // get long
    protected long getLong(String key) {
        SharedPreferences sp = mContext.getSharedPreferences(mName, mMode);
        return sp.getLong(key, 0L);
    }

    // put long
    protected boolean putLong(String key, long value) {
        SharedPreferences sp = mContext.getSharedPreferences(mName, mMode);
        SharedPreferences.Editor editor = sp.edit();
        editor.putLong(key, value);
        return editor.commit();
    }

    protected boolean clear() {
        SharedPreferences sp = mContext.getSharedPreferences(mName, mMode);
        SharedPreferences.Editor editor = sp.edit();
        editor.clear();
        return editor.commit();
    }

}
