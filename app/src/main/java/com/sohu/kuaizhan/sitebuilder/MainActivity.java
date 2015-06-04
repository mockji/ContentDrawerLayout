package com.sohu.kuaizhan.sitebuilder;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.ListView;

import com.sohu.kuaizhan.sitebuilder.adapter.DrawerMenuAdapter;
import com.sohu.kuaizhan.sitebuilder.widget.DrawerNav;


public class MainActivity extends ActionBarActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ListView menus = (ListView)findViewById(R.id.lv_menu_list);
        menus.setAdapter(new DrawerMenuAdapter(this));
    }

}
