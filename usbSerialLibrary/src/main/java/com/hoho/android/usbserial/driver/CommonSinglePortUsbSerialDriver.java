package com.hoho.android.usbserial.driver;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import java.io.IOException;
import java.security.AccessControlException;

public abstract class CommonSinglePortUsbSerialDriver extends CommonUsbSerialPort implements UsbSerialDriver {

    protected final UsbDevice mDevice;
    protected UsbDeviceConnection mConnection;
    public static final int DEFAULT_READ_BUFFER_SIZE = 16 * 1024;
    public static final int DEFAULT_WRITE_BUFFER_SIZE = 16 * 1024;

    protected final Object mReadBufferLock = new Object();
    protected final Object mWriteBufferLock = new Object();

    /** Internal read buffer.  Guarded by {@link #mReadBufferLock}. */
    protected byte[] mReadBuffer;

    /** Internal write buffer.  Guarded by {@link #mWriteBufferLock}. */
    protected byte[] mWriteBuffer;


    public CommonSinglePortUsbSerialDriver(UsbDevice device) {
       mDevice = device;
        mReadBuffer = new byte[DEFAULT_READ_BUFFER_SIZE];
        mWriteBuffer = new byte[DEFAULT_WRITE_BUFFER_SIZE];
    }

    /**
     * Sets the size of the internal buffer used to exchange data with the USB
     * stack for read operations.  Most users should not need to change this.
     *
     * @param bufferSize the size in bytes
     */
    public final void setReadBufferSize(int bufferSize) {
        synchronized (mReadBufferLock) {
            if (bufferSize == mReadBuffer.length) {
                return;
            }
            mReadBuffer = new byte[bufferSize];
        }
    }

    /**
     * Sets the size of the internal buffer used to exchange data with the USB
     * stack for write operations.  Most users should not need to change this.
     *
     * @param bufferSize the size in bytes
     */
    public final void setWriteBufferSize(int bufferSize) {
        synchronized (mWriteBufferLock) {
            if (bufferSize == mWriteBuffer.length) {
                return;
            }
            mWriteBuffer = new byte[bufferSize];
        }
    }

 // Implementors implement their port specific initialization
    // in this method.
    protected abstract boolean initDriverSpecific(UsbManager usbManager)
            throws IOException, AccessControlException;

    // Implementors implement their port specific initialization
    // in this method.
    protected abstract boolean initDriverSpecific()
            throws IOException, AccessControlException;
    // Implementors implement their port specific deinitialization
    // in this method.
    protected abstract void deinitDriverSpecific() throws IOException;

    @Override
    protected final boolean initPortSepcific(UsbManager usbManager) throws IOException, AccessControlException {
//        mConnection = CommonUsbSerialDriver.openDeviceConnection(usbManager, mDevice);
        return initDriverSpecific(usbManager);
    }
    @Override
    protected final boolean initOnlyPortSepcific(UsbDeviceConnection connection) throws IOException, AccessControlException {
        mConnection = connection;
       return initDriverSpecific();
    }
    @Override
    protected final void deinitPortSpecific() throws IOException {
        try {
            deinitDriverSpecific();
            mConnection.close();
            mConnection = null;
        } finally {
            if (mConnection != null) {
                try {
                    mConnection.close();
                    mConnection = null;
                } catch (Exception e) {
                }
            }
        }
    }
    

    @Override
    protected void portClosed() {
    }

    @Override
    public UsbDevice getDevice() {
        return mDevice;
    }

    @Override
    public int getPortCount() {
        return 1;
    }

    @Override
    public UsbSerialPort getPort(int i) throws IndexOutOfBoundsException {
        if (i == 0) {
            return this;
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

}
