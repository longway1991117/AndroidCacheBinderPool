// IBinderPool.aidl
package com.longway.binder;

// Declare any non-default types here with import statements

interface IBinderPool {
   IBinder queryBinderObj(int code);
      void registerBinderClass(int code, String clz);
      void unregisterBinderClass(int code);
}
