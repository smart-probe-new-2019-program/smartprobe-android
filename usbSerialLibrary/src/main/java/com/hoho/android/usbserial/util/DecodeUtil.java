package com.hoho.android.usbserial.util;

import java.io.PrintStream;

public class DecodeUtil {
  public static byte decodeBit(byte paramByte, int paramInt) {
    byte b = (byte) (1 << paramInt);
    PrintStream localPrintStream = System.out;
    Object[] arrayOfObject = new Object[2];
    arrayOfObject[0] = Integer.valueOf(paramInt);
    arrayOfObject[1] = Short.valueOf(b);
    localPrintStream.format("decodeBit bitIndex = %d, Mask = %x", arrayOfObject);
    return (byte) ((paramByte | b) >> paramInt);
  }

  public static int decodeInt2(byte paramByte1, byte paramByte2) {
    return 0x0 | 0xFF00 & paramByte2 << 8 | paramByte1 & 0xFF;
  }

  public static long decodeInt4(byte paramByte1, byte paramByte2, byte paramByte3, byte paramByte4) {
    return 0L | 0xFF000000 & paramByte4 << 24 | 0xFF0000 & paramByte3 << 16 | 0xFF00 & paramByte2 << 8 | paramByte1 & 0xFF;
  }

  public static String decodeString(byte[] paramArrayOfByte, int paramInt1, int paramInt2) {
    int j = 0;
    for (int i = paramInt1; i < paramInt2; i++) {
      j = 0;

      if (paramArrayOfByte[i] == 0)
        j = 1 + (i - paramInt1);


    }
    return new String(paramArrayOfByte, paramInt1, j);

  }
}

/* Location:           /home/abitha/Pictures/source/USBSerial/dex2jar/classes-dex2jar.jar
 * Qualified Name:     com.oneman.freeusbtools.util.DecodeUtil
 * JD-Core Version:    0.6.0
 */