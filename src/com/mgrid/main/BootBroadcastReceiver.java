package com.mgrid.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**开机自启动*/
public class BootBroadcastReceiver extends BroadcastReceiver { 
    static final String action_boot="android.intent.action.BOOT_COMPLETED";  
  
    @Override
    public void onReceive(Context context, Intent intent) { 
        if (intent.getAction().equals(action_boot)){  
            Intent ootStartIntent=new Intent(context, MGridActivity.class);  
            ootStartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 
            ootStartIntent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
            context.startActivity(ootStartIntent);  
        } 
    }  
} 