package com.hoho.android.usbserial.driver;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.AccessControlException;
import java.util.LinkedHashMap;
import java.util.Map;

//import org.apache.http.impl.conn.tsccm.WaitingThread;

/**
 * USB CDC/ACM serial driver implementation.
 *
 * @author mike wakerly (opensource@hoho.com)
 * @see <a
 * href="http://www.usb.org/developers/devclass_docs/usbcdc11.pdf">Universal
 * Serial Bus Class Definitions for Communication Devices, v1.1</a>
 */
public class CCSSerialDriver extends CommonSinglePortUsbSerialDriver {

    private final String TAG = "USB";//CCSSerialDriver.class.getSimpleName();

    protected UsbEndpoint mControlEndpoint;
    protected UsbEndpoint mReadEndpoint;
    protected UsbEndpoint mWriteEndpoint;

    private boolean mRts = false;
    private boolean mDtr = false;

    private static final int USB_RECIP_INTERFACE = 0x01;
    private static final int USB_RT_ACM = UsbConstants.USB_DIR_OUT
            | UsbConstants.USB_TYPE_CLASS
            | USB_RECIP_INTERFACE;

    private static final int SET_LINE_CODING = 0x20;  // USB CDC 1.1 section 6.2
    //    private static final int GET_LINE_CODING = 0x21;
    private static final int SET_CONTROL_LINE_STATE = 0x22;
//    private static final int SEND_BREAK = 0x23;

    protected static final int STATUS_FLAG_CD = 0x01;
    protected static final int STATUS_FLAG_DSR = 0x02;
    protected static final int STATUS_FLAG_RI = 0x08;
    protected static final int STATUS_FLAG_CTS = 0x80;

    protected static final int CCS_CDC_Class = 2;
    protected static final int CCS_CDC_subClass = 2;
    protected static final int CCS_CDC_Protocol = 1;

    private int baudRate = 2400;
    private float stopBits = 1;
    int index = 0;

    byte[] portSetting = new byte[7];
    private UsbDevice usbDevice;
    private UsbInterface intf;
    private String parity = "None";
    private int dataBits = 8;
    protected byte[] recvBuffer = new byte[1024];

    protected int mBaudRate = -1, mDataBits = -1, mStopBits = -1, mParity = -1;
    private boolean mEnableAsyncReads;
    private UsbManager usbManager;

    public CCSSerialDriver(UsbDevice device) {
        super(device);

        /*
        Use print(device) method to get detail information about our smart probe device.
        Base on our smart probe device we need to write code to init endpoints to read data.
         */
        print(device);
    }

    @Override
    protected boolean initDriverSpecific(UsbManager usbManager) throws IOException, AccessControlException {
        this.usbManager=usbManager;
        return initDriverSpecific();
    }

    /**
     * initDriverSpecific() will invoke when supported device is connected.
     *
     *
     * @throws IOException
     * @throws AccessControlException
     */
    @Override
    protected boolean initDriverSpecific() throws IOException, AccessControlException {
        mEnableAsyncReads = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1);

        /*
        Call initEndpoint() to initialize endpoints and to set driver settings( Eg: baudRate,stopbits,parity etc)
        */

       return initEndpoints();
    }

    @Override
    protected void deinitDriverSpecific() throws IOException {

        if (this.intf != null)
            this.mConnection.releaseInterface(this.intf);
        this.mConnection.close();
        this.intf = null;
        this.usbDevice = null;
    }

    protected int sendAcmControlMessage(int request, int value, byte[] buf) {
        return mConnection.controlTransfer(
                USB_RT_ACM, request, value, 0, buf, buf != null ? buf.length : 0, 5000);
    }

    protected boolean initEndpoints() throws IOException {

        /*
        initDevice() is the custom method we wrote to support our new smart probe device.
        */
        return initDevice(mDevice);
    }

    /**
     * Call this method to read data from USb device.
     *
     * @param dest the destination byte buffer
     * @param timeoutMillis the timeout for reading
     * @return len of bytes read
     * @throws IOException
     */
    @Override
    public int read(byte[] dest, int timeoutMillis) throws IOException {
        /*
        If SDK version above 17 use this code to read data
         */

        if (mEnableAsyncReads) {
            final UsbRequest request = new UsbRequest();
            try {
                request.initialize(mConnection, mReadEndpoint);
                final ByteBuffer buf = ByteBuffer.wrap(dest);
                if (!request.queue(buf, dest.length)) {
                    throw new IOException("Error queueing request.");
                }

                final UsbRequest response = mConnection.requestWait();
                if (response == null) {
                    throw new IOException("Null response");
                }

                final int nread = buf.position();
                if (nread > 0) {
                    //Log.d(TAG, HexDump.dumpHexString(dest, 0, Math.min(32, dest.length)));
                    return nread;
                } else {
                    return 0;
                }
            } finally {
                request.close();
            }
        }

        /*
        Default code to read daat from USB device.
         */

        final int numBytesRead;
        synchronized (mReadBufferLock) {
            int readAmt = Math.min(dest.length, mReadBuffer.length);
            numBytesRead = mConnection.bulkTransfer(mReadEndpoint, mReadBuffer, readAmt,
                    timeoutMillis);
            if (numBytesRead < 0) {
                // This sucks: we get -1 on timeout, not 0 as preferred.
                // We *should* use UsbRequest, except it has a bug/api oversight
                // where there is no way to determine the number of bytes read
                // in response :\ -- http://b.android.com/28023
                if (timeoutMillis == Integer.MAX_VALUE) {
                    // Hack: Special case "~infinite timeout" as an error.
                    return -1;
                }
                return 0;
            }
            System.arraycopy(mReadBuffer, 0, dest, 0, numBytesRead);
        }
        return numBytesRead;
    }

    @Override
    public int write(byte[] src, final int length, int timeoutMillis) throws IOException {
        Log.d("Test", "before write:");
        final int count = mConnection.bulkTransfer(mWriteEndpoint, src, length, timeoutMillis);
        Log.d("Test", "Write Count:" + count);
        return (count < 0) ? 0 : count;
    }

    @Override
    public void setParameters(int baudRate, int dataBits, int stopBits, int parity) throws IOException {
        if ((mBaudRate == baudRate) && (mDataBits == dataBits)
                && (mStopBits == stopBits) && (mParity == parity)) {
            // Make sure no action is performed if there is nothing to change
            return;
        }

        byte stopBitsByte;
        switch (stopBits) {
            case STOPBITS_1:
                stopBitsByte = 0;
                break;
            case STOPBITS_1_5:
                stopBitsByte = 1;
                break;
            case STOPBITS_2:
                stopBitsByte = 2;
                break;
            default:
                throw new IllegalArgumentException("Bad value for stopBits: " + stopBits);
        }

        byte parityBitesByte;
        switch (parity) {
            case PARITY_NONE:
                parityBitesByte = 0;
                break;
            case PARITY_ODD:
                parityBitesByte = 1;
                break;
            case PARITY_EVEN:
                parityBitesByte = 2;
                break;
            case PARITY_MARK:
                parityBitesByte = 3;
                break;
            case PARITY_SPACE:
                parityBitesByte = 4;
                break;
            default:
                throw new IllegalArgumentException("Bad value for parity: " + parity);
        }

        byte[] msg = {
                (byte) (baudRate & 0xff),
                (byte) ((baudRate >> 8) & 0xff),
                (byte) ((baudRate >> 16) & 0xff),
                (byte) ((baudRate >> 24) & 0xff),
                stopBitsByte,
                parityBitesByte,
                (byte) dataBits};
        int res = 0;
        res = sendAcmControlMessage(SET_LINE_CODING, 0, msg);
        Log.d(TAG, "Set Serial Param return: " + res);

        mBaudRate = baudRate;
        mDataBits = dataBits;
        mStopBits = stopBits;
        mParity = parity;
    }

    @Override
    public boolean getCD() throws IOException {
        return false;  // TODO
    }

    @Override
    public boolean getCTS() throws IOException {
        return false;  // TODO
    }

    @Override
    public boolean getDSR() throws IOException {
        return false;  // TODO
    }

    @Override
    public boolean getDTR() throws IOException {
        return mDtr;
    }

    @Override
    public void setDTR(boolean value) throws IOException {
        mDtr = value;
        setDtrRts();
    }

    @Override
    public boolean getRI() throws IOException {
        return false;  // TODO
    }

    @Override
    public boolean getRTS() throws IOException {
        return mRts;
    }

    @Override
    public void setRTS(boolean value) throws IOException {
        mRts = value;
        setDtrRts();
    }

    @Override
    public String getShortDeviceName() {
        String shortDeviceName = null;

        switch (mDevice.getVendorId()) {

            case UsbId.VENDOR_CCS:  // ADded for test
                if (mDevice.getProductId() == UsbId.CCS_EasyIO_1061) {
                    shortDeviceName = "CCS_EasyIo_1061";
                }
                if (mDevice.getProductId() == UsbId.CCS_DataLogger_1058) {
                    shortDeviceName = "CCS_DataLogger_1058";
                }

                if (mDevice.getProductId() == UsbId.CCS_SmartLine_RotaScope_f668) {
                    shortDeviceName = "CCS_SmartLine_RotaScope_f668";
                }
                break;
        }

        if (shortDeviceName == null) {
            shortDeviceName = "CDC";
        }

        return shortDeviceName;
    }

    public int getPID() {
        return mDevice.getProductId();
    }

    private void setDtrRts() {
        int value = (mRts ? 0x2 : 0) | (mDtr ? 0x1 : 0);
        int res = sendAcmControlMessage(SET_CONTROL_LINE_STATE, value, null);
//        Log.d(TAG,"SetDtrRts Returns: " +res);
    }

    public static Map<Integer, int[]> getSupportedDevices() {
        final Map<Integer, int[]> supportedDevices = new LinkedHashMap<Integer, int[]>();


        supportedDevices.put(Integer.valueOf(UsbId.VENDOR_CCS),
                new int[]{
                        UsbId.CCS_DataLogger_1058,
                        UsbId.CCS_EasyIO_1061,
                        UsbId.CCS_SmartLine_RotaScope_f668
                });


        return supportedDevices;
    }


    /**
     * @param paramFloat  - parity
     * @param paramString - dataBits
     * @param paramInt    - baudRate
     * @return
     */
    public boolean setLineControl(float paramFloat, String paramString, int paramInt) {
        System.out.println("public boolean setLineControl(float stopBits, String parity, int dataBits)");
        System.out.println("stopBits, parity, dataBits baudRate = " + baudRate + " paramFloat= " + paramFloat + " paramString= " + paramString + " paramInt = " + paramInt);
        byte[] arrayOfByte = makeLineControl(this.baudRate, paramFloat, paramString, paramInt);
        this.mConnection.controlTransfer(128, 0, 0, 0, null, 0, 0);//to reset
        this.mConnection.controlTransfer(33, 0, 0, 0, null, 0, 0);//to reset
        if (this.mConnection.controlTransfer(33, 32, 0, this.index, arrayOfByte, 7, 100) < 0) {
            System.out.println("Set Line Control Fail");
            return false;
        }
        return true;
    }

    /**
     * @param paramInt1   -baudRate
     * @param paramFloat  -parity
     * @param paramString - dataBits
     * @param paramInt2   -baudRate
     * @return
     */
    private byte[] makeLineControl(int paramInt1, float paramFloat, String paramString, int paramInt2) {
        this.portSetting[0] = (byte) (paramInt1 & 0xFF);
        this.portSetting[1] = (byte) (0xFF & paramInt1 >> 8);
        this.portSetting[2] = (byte) (0xFF & paramInt1 >> 16);
        this.portSetting[3] = (byte) (0xFF & paramInt1 >> 24);
        if (1.0F == paramFloat) {
            this.portSetting[4] = 0;
            if ("None".equals(paramString))
                this.portSetting[5] = 0;
        }
        // while (true)
        {
//                if ((paramInt2 >= 5) && (paramInt2 <= 8))
//                throw new IllegalArgumentException("Invalid data bits.");
            if (1.5D == paramFloat) {
                this.portSetting[4] = 1;

            }
            if (2.0F == paramFloat) {
                this.portSetting[4] = 2;

            }

            if ("Odd".equals(paramString)) {
                this.portSetting[5] = 1;

            }
            if ("Even".equals(paramString)) {
                this.portSetting[5] = 2;

            }
            if ("Mark".equals(paramString)) {
                this.portSetting[5] = 3;

            }

            this.portSetting[5] = 4;
        }

        this.portSetting[6] = (byte) paramInt2;
        return this.portSetting;
    }

    private int findCDCControlInterface(UsbDevice paramUsbDevice) {
        Log.e(TAG, "paramUsbDevice.getInterfaceCount() = " + paramUsbDevice.getInterfaceCount());
        for (int i = 0; ; i++) {
            Log.e(TAG, "paramUsbDevice.getInterfaceCount() = " + paramUsbDevice.getInterfaceCount());
            if (i >= paramUsbDevice.getInterfaceCount())
                i = -1;
            do
                return i;
            while (2 == paramUsbDevice.getInterface(i).getInterfaceClass());
        }
    }

    /**
     * Find interface of new smart probe device to declare end points.
     * If interface is notchosenn correctly read data will not be accurate.
     *
     * @param paramUsbDevice
     */

    private UsbInterface findCDCDataInterface(UsbDevice paramUsbDevice) {
        Log.e(TAG, "********************findCDCDataInterface*****************************");
        UsbInterface localUsbInterface = null;
        for (int i = 0; i < paramUsbDevice.getInterfaceCount(); i++) {

            Log.e(TAG, i + "  getInterfaceClass = = " + paramUsbDevice.getInterface(i).getInterfaceClass());

            if (10 == paramUsbDevice.getInterface(i).getInterfaceClass()) {
                localUsbInterface = paramUsbDevice.getInterface(i);
                break;
            }


        }
        return localUsbInterface;
    }


    /**
     * Find interface of old smart probe device to declare end points.
     * If interface is not chosenn correctly read data will not be accurate.
     *
     * @param paramUsbDevice
     */
    private UsbInterface findCDCDataOldDeviceInterface(UsbDevice paramUsbDevice) {
        Log.e(TAG, "********************Old findCDCDataInterface*****************************");
        UsbInterface localUsbInterface = paramUsbDevice.getInterface(1);

        /*As default 2nd interface is using for old device based on old code used.*/
        if (paramUsbDevice.getInterfaceCount() >= 2)
            localUsbInterface = paramUsbDevice.getInterface(1);


        return localUsbInterface;
    }


    /**
     * Initialize read and write endpoint to read  and write data to USB Serial communication.
     * If read and write end points are not set we can't read data.
     *
     * @param paramUsbInterface
     */
    private void checkEndPoints(UsbInterface paramUsbInterface) {
        Log.e(TAG, "End point count = " + paramUsbInterface.getEndpointCount());

        for (int i = 0; i < paramUsbInterface.getEndpointCount(); i++) {

            UsbEndpoint localUsbEndpoint = paramUsbInterface.getEndpoint(i);
            Log.e(TAG, "index = " + i + "  End point type = " + localUsbEndpoint.getType() + " getDirection() = " + localUsbEndpoint.getDirection());
            if (2 == localUsbEndpoint.getType()) {
                /*
                localUsbEndpoint.getDirection() 128 means read direction
                 */
                if (128 == localUsbEndpoint.getDirection())
                    this.mReadEndpoint = localUsbEndpoint;
            }


            if (localUsbEndpoint.getDirection() == 0)
                this.mWriteEndpoint = localUsbEndpoint;


        }

    }

    /**
     * Given this class to just log interface class information
     *
     * @param paramInt
     * @return
     */
    public String getUSBClassDescription(int paramInt) {
        switch (paramInt) {
            default:
                return "Unknown";
            case 0:
                return "Unspecified";
            case 1:
                return "Audio";
            case 2:
                return "Communications and CDC Control";
            case 3:
                return "HID (Human Interface Device)";
            case 5:
                return "Physical";
            case 6:
                return "Image";
            case 7:
                return "Printer";
            case 8:
                return "Mass Storage";
            case 9:
                return "Hub";
            case 10:
                return "CDC-Data";
            case 11:
                return "Smart Card";
            case 13:
                return "Content Security";
            case 14:
                return "Video";
            case 15:
                return "Personal Healthcare";
            case 220:
                return "Diagnostic Device";
            case 224:
                return "Wireless Controller";
            case 239:
                return "Miscellaneous";
            case 254:
                return "Application Specific";
            case 255:
        }
        return "Vendor Specific";
    }

    /**
     * To log connected USB device information.
     * Based on this information we are initializing inteface and endpoints
     *
     * @param paramUsbDevice
     */
    protected void print(UsbDevice paramUsbDevice) {
        Log.e(TAG, "Device ID: " + paramUsbDevice.getDeviceId());
        Log.e(TAG, "Device Name: " + UsbDevice.getDeviceName(paramUsbDevice.getDeviceId()));
        Log.e(TAG, "Device Class: " + getUSBClassDescription(paramUsbDevice.getDeviceClass()));
        Log.e(TAG, "Device Sub Class: " + getUSBClassDescription(paramUsbDevice.getDeviceSubclass()));
        Log.e(TAG, "Device Protocol: " + paramUsbDevice.getDeviceProtocol());
        Log.e(TAG, "Vendor ID: " + paramUsbDevice.getVendorId());
        Log.e(TAG, "Product ID: " + paramUsbDevice.getProductId());
        Log.e(TAG, "Interface Count: " + paramUsbDevice.getInterfaceCount());
        UsbInterface localUsbInterface;
        int j;
        for (int i = 0; ; i++) {
            if (i >= paramUsbDevice.getInterfaceCount())
                break;
            localUsbInterface = paramUsbDevice.getInterface(i);
            Log.e(TAG, "-----------------------------------------------");
            Log.e(TAG, "Interface ID: " + localUsbInterface.getId());
            Log.e(TAG, "Interface Class: " + getUSBClassDescription(localUsbInterface.getInterfaceClass()));
            Log.e(TAG, "Interface Sub Class: " + getUSBClassDescription(localUsbInterface.getInterfaceSubclass()));
            Log.e(TAG, "Interface Protocol: " + localUsbInterface.getInterfaceProtocol());
            Log.e(TAG, "Interface Endpoint Count: " + localUsbInterface.getEndpointCount());
            for (j = 0; j < localUsbInterface.getEndpointCount(); j++) {
                UsbEndpoint localUsbEndpoint = localUsbInterface.getEndpoint(j);
                Log.e(TAG, "Endpoint Number: " + localUsbEndpoint.getEndpointNumber());
                Log.e(TAG, "Endpoint Address: " + localUsbEndpoint.getAddress());
                String str;
                StringBuilder localStringBuilder = new StringBuilder("Endpoint Direction: ");
                if (128 == localUsbEndpoint.getDirection())
                    str = "IN";
                else
                    str = "OUT";

                Log.e(TAG, str);
                Log.e(TAG, "Endpoint Interval: " + localUsbEndpoint.getInterval());
                Log.e(TAG, "Endpoint Max Packet Size: " + localUsbEndpoint.getMaxPacketSize());
                Log.e(TAG, "Endpoint Type: " + getEndPointTypeName(localUsbEndpoint.getType()));

            }

        }
        Log.e(TAG, "===============================================");

    }

    /**
     * To Log end point type name while print device details.
     *
     * @param paramInt
     * @return
     */
    protected String getEndPointTypeName(int paramInt) {
        switch (paramInt) {
            default:
                return "Unknown";
            case 0:
                return "Control endpoint type (endpoint zero)";
            case 3:
                return "Interrupt endpoint type";
            case 1:
                return "Isochronous endpoint type (currently not supported)";
            case 2:
        }
        return "Bulk endpoint type";
    }

    public int getBaudrate() {
        return this.baudRate;
    }

    public int[] getFlowControl() {
        return null;
    }

    public int getLineControl() {
        this.mConnection.controlTransfer(161, 0, 0, 0, null, 0, 0);//to reset
        if (this.mConnection.controlTransfer(161, 33, 0, this.index, this.portSetting, 7, 100) < 0) {
            System.out.println("Get Line Control Fail");
            return -1;
        }
        int i = 0xFF & this.portSetting[0] | (0xFF & this.portSetting[1]) << 8 | (0xFF & this.portSetting[2]) << 16 | (0xFF & this.portSetting[3]) << 24;
        System.out.println("Current Baudrate is " + i);
        switch (this.portSetting[4]) {
            default:
                switch (this.portSetting[5]) {
                    default:
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                }
            case 0:
            case 1:
            case 2:
        }

        return -1;
    }

    /**
     * To set new baudrate, as default baurd rate is 9600
     *
     * @param paramInt
     * @return
     */
    public boolean setBaudrate(int paramInt) {
        this.baudRate = paramInt;
        return true;
    }

    /**
     * Contain code to initialize all required device data.
     * Interface and end points are initialize in this code to communicate with USB port.
     *
     * @param paramUsbDevice
     * @return
     */
    public boolean initDevice(UsbDevice paramUsbDevice) {
        System.out.println("initDevice");
        this.usbDevice = paramUsbDevice;
        if (paramUsbDevice != null) {
            do {
                // print(paramUsbDevice);

                Log.e(TAG, "-----------------------------------------------");
                /*
                A device will contain multiple interface,we ned to select control interface to init endpoints.
                */
                this.index = findCDCControlInterface(paramUsbDevice);

                Log.e(TAG, "Selected interface index = " + index);
                if (-1 == this.index) {
                    return false;
                }

                /*
                For our new smart probe device, using interface with class count 10 only we are able to read data properly.
                * If we try to use other interface data will be corrupted .
                *
                * Note: For old smart probe device 2nd interface is using as default to read data.
                * */
                this.intf = findCDCDataInterface(paramUsbDevice);
                if (this.intf == null) {

                    System.out.println("<<<<  Interface not found,check connected device is old  >>>");
                    /*If interface not found,before exit we can check connected device is old or not
                    *
                     */

                    baudRate = 9600;
                    this.intf = findCDCDataOldDeviceInterface(paramUsbDevice);
                    if (this.intf == null)
                        return false;
                }
                this.mConnection = usbManager.openDevice(paramUsbDevice);

                if (this.mConnection == null) {
                    System.out.println("USB Connection is null.");

                    return false;
                }
                //******************
                this.mConnection.controlTransfer(0x40, 0, 0, 0, null, 0, 0);  // reset
                // mConnection.controlTransfer(0×40,
                // 0, 1, 0, null, 0,
                // 0);//clear Rx
                this.mConnection.controlTransfer(0x40, 0, 2, 0, null, 0, 0);  // clear Tx

                 //********************************
                if (!this.mConnection.claimInterface(this.intf, true)) {
                    Log.e(TAG, "Exclusive interface access failed!");
                    return false;
                }

                /*
                After interface is initialized we can declare read and write end points
                */
                checkEndPoints(this.intf);

                 /*
               Set baudrate as default to 2400 to read data from probe
                 */
                getBaudrate();
                if(!setLineControl(stopBits, parity, dataBits))
                    return false;
                getLineControl();
            }
            while ((!setBaudrate(baudRate)));
            getLineControl();
            Log.e(TAG, "Initialized");
        }
        return true;
    }


}
