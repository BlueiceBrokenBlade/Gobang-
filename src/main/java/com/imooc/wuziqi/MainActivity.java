package com.imooc.wuziqi;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import cn.bmob.push.BmobPush;
import cn.bmob.v3.Bmob;
import cn.bmob.v3.BmobInstallation;
import cn.bmob.v3.BmobPushManager;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.PushListener;

public class MainActivity extends Activity {
    private WuziqiPanel wuziqipanel;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wuziqipanel=(WuziqiPanel) findViewById(R.id.id_wuziqi);
        wuziqipanel.deleteAllData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.wuziqimenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id=item.getItemId();
        if(id==R.id.menu_wuziqi){
            wuziqipanel.restart();
            return true;
        }
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        wuziqipanel.deleteAllData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        wuziqipanel.deleteAllData();
    }
}
