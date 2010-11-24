/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /Users/vamsi/Documents/workspace/Plaudible/src/com/vn/plaudible/PLSInterface.aidl
 */
package com.vn.plaudible;
public interface PLSInterface extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.vn.plaudible.PLSInterface
{
private static final java.lang.String DESCRIPTOR = "com.vn.plaudible.PLSInterface";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.vn.plaudible.PLSInterface interface,
 * generating a proxy if needed.
 */
public static com.vn.plaudible.PLSInterface asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = (android.os.IInterface)obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.vn.plaudible.PLSInterface))) {
return ((com.vn.plaudible.PLSInterface)iin);
}
return new com.vn.plaudible.PLSInterface.Stub.Proxy(obj);
}
public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_stopReading:
{
data.enforceInterface(DESCRIPTOR);
this.stopReading();
reply.writeNoException();
return true;
}
case TRANSACTION_readArticle:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.readArticle(_arg0);
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.vn.plaudible.PLSInterface
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
public void stopReading() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_stopReading, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void readArticle(java.lang.String text) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(text);
mRemote.transact(Stub.TRANSACTION_readArticle, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_stopReading = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_readArticle = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
}
public void stopReading() throws android.os.RemoteException;
public void readArticle(java.lang.String text) throws android.os.RemoteException;
}
