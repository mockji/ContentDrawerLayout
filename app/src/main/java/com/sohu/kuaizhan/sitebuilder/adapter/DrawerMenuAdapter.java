package com.sohu.kuaizhan.sitebuilder.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.sohu.kuaizhan.sitebuilder.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhulijun on 6/2/15.
 */
public class DrawerMenuAdapter extends BaseAdapter {
    Context mContext;
    List<String> mMenuItems;
    LayoutInflater  mInflater;
    public DrawerMenuAdapter(Context context){
        super();
        mContext = context;
        mInflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mMenuItems = new ArrayList<>();

        {
            mMenuItems.add("小飞侠");
            mMenuItems.add("海报");
            mMenuItems.add("设置");
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MenuViewHolder holder;
        if(null == convertView){
            convertView = mInflater.inflate(R.layout.item_drawer_menu,null);
            assert convertView != null;
            holder = new MenuViewHolder();
            holder.mMenuIcon = (ImageView)convertView.findViewById(R.id.icon_drawer_menu);
            holder.mMenuTitle = (TextView)convertView.findViewById(R.id.title_drawer_menu);
            convertView.setTag(holder);
        }else {
            holder = (MenuViewHolder)convertView.getTag();
        }

        String item = mMenuItems.get(position);
        holder.mMenuTitle.setText(item);

        return convertView;
    }

    @Override
    public int getCount() {
        return mMenuItems.size();
    }

    @Override
    public Object getItem(int position) {
        return mMenuItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public static class MenuViewHolder{
        public ImageView mMenuIcon;
        public TextView mMenuTitle;
        public ImageView mMenuSubIcon;
    }
}
