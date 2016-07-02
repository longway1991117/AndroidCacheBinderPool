package com.longway.binder.binderPool;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by longway
 */
public class BinderService extends Service {
    private static final String TAG = BinderService.class.getSimpleName();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return remoteBinder;
    }

    private BinderPool.RemoteBinder remoteBinder = new BinderPool.RemoteBinder();

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }
}
