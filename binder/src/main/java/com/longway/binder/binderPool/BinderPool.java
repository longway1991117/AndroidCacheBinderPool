package com.longway.binder.binderPool;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;

import com.longway.binder.IBinderPool;

import java.util.concurrent.CountDownLatch;

/**
 * Created by longway
 */
public class BinderPool {
    private static final String TAG = BinderPool.class.getSimpleName();
    private final CountDownLatch mCountDownLatch = new CountDownLatch(1);
    private static volatile BinderPool sBinderPool;
    private IBinderPool mIBinderPool;
    private Context mContext;

    public void registerBinderClass(int code, String clz) {
        try {
            mIBinderPool.registerBinderClass(code, clz);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void unregisterBinderClass(int code) {
        try {
            mIBinderPool.unregisterBinderClass(code);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private Context convertContext(Context context) {
        if (context == null) {
            throw new NullPointerException("context==null");
        }
        if (!(context instanceof Application)) {
            return context.getApplicationContext();
        }
        return context;
    }

    public static BinderPool getInstance(Context context) {
        if (sBinderPool == null) {
            synchronized (BinderPool.class) {
                if (sBinderPool == null) {
                    sBinderPool = new BinderPool(context);
                }
            }
        }
        return sBinderPool;
    }

    public BinderPool(Context context) {
        mContext = convertContext(context);
        connectService(mContext);
    }

    public IBinder queryBinderObj(int code) {
        IBinder iBinder = null;
        try {
            if (mIBinderPool != null) {
                return mIBinderPool.queryBinderObj(code);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return iBinder;
    }

    private boolean connectService(Context context) {
        Intent service = new Intent(context, BinderService.class);
        ComponentName componentName = context.startService(service);
        Log.d(TAG, componentName.toShortString());
        boolean success = context.bindService(service, connection, Context.BIND_AUTO_CREATE);
        try {
            mCountDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "connectService:" + success);
        return success;
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mIBinderPool = IBinderPool.Stub.asInterface(service);
            try {
                mIBinderPool.asBinder().linkToDeath(deathRecipient, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            mCountDownLatch.countDown();
            Log.d(TAG, "onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "service disconnected... <<" + name);
        }
    };

    private IBinder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            connectService(mContext);
        }
    };

    public static class RemoteBinder extends IBinderPool.Stub {
        private SparseArray<IBinder> mRemoteBinder = new SparseArray<>();
        private SparseArray<String> mRemoteBinderClass = new SparseArray<>();

        public void unregisterBinderClass(int code) throws RemoteException {
            synchronized (mRemoteBinderClass) {
                int index = mRemoteBinderClass.indexOfKey(code);
                if (index >= 0) {
                    mRemoteBinderClass.removeAt(index);
                }
            }
        }

        public RemoteBinder() {
            super();
        }


        @Override
        public IBinder queryBinderObj(int code) throws RemoteException {
            IBinder iBinder = mRemoteBinder.get(code);
            if (iBinder != null) {
                return iBinder;
            }
            Class<?> clz = null;
            try {
                clz = Class.forName(mRemoteBinderClass.get(code));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            if (clz != null) {
                try {
                    iBinder = (IBinder) clz.newInstance();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (ClassCastException e) {
                    e.printStackTrace();
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
                if (iBinder != null) {
                    mRemoteBinder.put(code, iBinder);
                }
            }
            Log.d(TAG, "IBinder <<" + iBinder);
            return iBinder;
        }

        @Override
        public void registerBinderClass(int code, String clz) throws RemoteException {
            synchronized (mRemoteBinderClass) {
                mRemoteBinderClass.put(code, clz);
            }
        }
    }

}
