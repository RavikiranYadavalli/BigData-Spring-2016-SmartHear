package com.smarthear.audiodetection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SoundServiceReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent myIntent = new Intent(context, SoundDetectionService.class);
        context.startService(myIntent);
    }
}
