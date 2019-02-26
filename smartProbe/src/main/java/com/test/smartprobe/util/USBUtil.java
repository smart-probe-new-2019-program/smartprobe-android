package com.test.smartprobe.util;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.List;


public class USBUtil {


    public UsbManager mManager = null;


    private UsbSerialPort[] sPort = null;
    private int[] sPort_PID = null;
    private final int DEFAULT_READ_BUFFER_SIZE = 16 * 1024;
    private final int READ_WAIT_MILLIS = 1000;
    private final ByteBuffer mReadBuffer = ByteBuffer.allocate(DEFAULT_READ_BUFFER_SIZE);


    public boolean isVendorIdSupported(int vid) {
        if (vid == 1240)    // Custom Circuit solution easyIO
            return true;
        if (vid == 1659)    // prolific
            return true;
//	if(vid == 1027 )	// FTDI
//		return true;

        return false;
    }


    public ArrayList<String> GetUSBSerialDevices(Context context) {


        mManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

        ArrayList<String> deviceList = new ArrayList<String>();

        sPort = null;    // delete all Ports previously defined
        sPort_PID = null;

        Log.d("Test", "GetUSBDevices Called");

        List<UsbSerialDriver> driverList = UsbSerialProber.findAllDevices(mManager);

        int MaxCDC = driverList.size();


        if (MaxCDC == 0) {
            Log.d(LogUtil.TAG, "No driver for serial has been found");
            return deviceList;
        }


        Log.d(LogUtil.TAG, "drivers no: " + MaxCDC);


        int cnt = 0;
        for (int i = 0; i < MaxCDC; i++) // add all driver and ports to the driver list
        {
            UsbSerialDriver usbSerialDriver = driverList.get(i);
            if (usbSerialDriver == null) {
                Log.d(LogUtil.TAG, "Serial Port Driver " + i + " is null");
                continue;
            }
            Log.d(LogUtil.TAG, "Driver Name: " + driverList.get(i).getShortDeviceName());

            UsbDevice thisDevice = driverList.get(i).getDevice();

            Log.d("Test", "Device ID: " + thisDevice.getDeviceId() + ",vendorID " + thisDevice.getVendorId() + ",Product ID:" + thisDevice.getProductId());
            if (isVendorIdSupported(thisDevice.getVendorId())) {

                sPort = new UsbSerialPort[driverList.get(i).getPortCount()];
                sPort_PID = new int[driverList.get(i).getPortCount()];
                String deviceName = null;
                for (int j = 0; j < driverList.get(i).getPortCount(); j++) {
                    sPort_PID[cnt] = thisDevice.getProductId();
                    sPort[cnt++] = driverList.get(i).getPort(j);
                    deviceName = driverList.get(i).getShortDeviceName() + ",P" + j;
                    deviceList.add(deviceName);

                    Log.d(LogUtil.TAG, "Device added:" + deviceName);
                }
            } else {

                Log.d(LogUtil.TAG, "Connected device is not supported ... Vendor id: " + thisDevice.getVendorId());
                continue;
            }
        }


        Log.d("Test", "END of For");
//        context.unregisterReceiver(mUsbReceiver);
//        isRegistered = false;
        return deviceList;
    }

    public UsbSerialPort getPort() {
        return sPort[0];
    }

    public boolean openPort(int portId) {
        try {

            if (!sPort[portId].isOpen()) {
                LogUtil.writeLogTest("Trying to open");
                if (sPort[portId].open(mManager))
                    LogUtil.writeLogTest("Device opened");
                else
                    return false;
            }

        } catch (AccessControlException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
        return sPort[portId].isOpen();
    }

    public boolean isOpen(int portId) {
        return sPort[portId].isOpen();
    }

    public boolean closePort() {
        try {
            if (sPort != null && sPort.length == 1)
                sPort[0].close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean closePort(int portId) {
        try {
            sPort[portId].close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public byte[] read(int portId) throws Exception {
        byte[] data = null;
        int len = sPort[portId].read(mReadBuffer.array(), READ_WAIT_MILLIS);
        if (len > 0) {
            Log.d(LogUtil.TAG, "Read data len=" + len);

            data = new byte[len];
            mReadBuffer.get(data, 0, len);
            mReadBuffer.clear();
        }

        return data;
    }

    public int read(int portId, byte[] rxBuf, int timeout) throws IOException {
        int len;
        len = sPort[portId].read(rxBuf, timeout);
        return len;
    }

    public int write(int portId, byte[] txBuf, int dlen, int timeout) throws IOException {
        int retlen;
//	notifyAll();
        if (sPort[portId] == null) {
            Log.d(LogUtil.TAG, "Port is null .....");
            return -1;
        }
        retlen = sPort[portId].write(txBuf, dlen, timeout);
        return retlen;
    }


    public int getProductId(int portId) {
        return sPort_PID[portId];
    }


}
