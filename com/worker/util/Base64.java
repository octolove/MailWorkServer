package com.worker.util;

import java.io.ByteArrayOutputStream;

public class Base64
{
  private static final char[] BASE64_ENCODING_TABLE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();
  private static final byte[] BASE64_DECODING_TABLE = { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 
    -1, -1, -1, -1, -1, 62, -1, -1, -1, 63, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 
    21, 22, 23, 24, 25, -1, -1, -1, -1, -1, -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, -1, -1, -1, -1, -1 };

  public static final String encode(byte[] data, int offset, int length)
  {
    if (data == null) {
      return null;
    }

    StringBuffer buffer = new StringBuffer();
    int[] temp = new int[3];
    int end = offset + length;

    while (offset < end) {
      temp[0] = (data[(offset++)] & 0xFF);

      if (offset == data.length) {
        buffer.append(BASE64_ENCODING_TABLE[(temp[0] >>> 2 & 0x3F)]);
        buffer.append(BASE64_ENCODING_TABLE[(temp[0] << 4 & 0x3F)]);
        buffer.append('=');
        buffer.append('=');

        break;
      }

      temp[1] = (data[(offset++)] & 0xFF);

      if (offset == data.length) {
        buffer.append(BASE64_ENCODING_TABLE[(temp[0] >>> 2 & 0x3F)]);
        buffer.append(BASE64_ENCODING_TABLE[((temp[0] << 4 | temp[1] >>> 4) & 0x3F)]);
        buffer.append(BASE64_ENCODING_TABLE[(temp[1] << 2 & 0x3F)]);
        buffer.append('=');

        break;
      }

      temp[2] = (data[(offset++)] & 0xFF);

      buffer.append(BASE64_ENCODING_TABLE[(temp[0] >>> 2 & 0x3F)]);
      buffer.append(BASE64_ENCODING_TABLE[((temp[0] << 4 | temp[1] >>> 4) & 0x3F)]);
      buffer.append(BASE64_ENCODING_TABLE[((temp[1] << 2 | temp[2] >>> 6) & 0x3F)]);
      buffer.append(BASE64_ENCODING_TABLE[(temp[2] & 0x3F)]);
    }

    return buffer.toString();
  }

  public static final String encode(byte[] data)
  {
    return encode(data, 0, data.length);
  }

  public static final String encode(String str)
  {
    return encode(str.getBytes());
  }

  public static final byte[] decode(String str)
  {
    if (str == null) {
      return null;
    }

    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    byte[] data = str.getBytes();
    int[] temp = new int[4];
    int index = 0;

    while (index < data.length) {
      do
        temp[0] = BASE64_DECODING_TABLE[data[(index++)]];
      while ((index < data.length) && (temp[0] == -1));

      if (temp[0] == -1)
      {
        break;
      }
      do
        temp[1] = BASE64_DECODING_TABLE[data[(index++)]];
      while ((index < data.length) && (temp[1] == -1));

      if (temp[1] == -1)
      {
        break;
      }
      buffer.write(temp[0] << 2 & 0xFF | temp[1] >>> 4 & 0xFF);
      do
      {
        temp[2] = data[(index++)];

        if (temp[2] == 61) {
          return buffer.toByteArray();
        }

        temp[2] = BASE64_DECODING_TABLE[temp[2]];
      }while ((index < data.length) && (temp[2] == -1));

      if (temp[2] == -1)
      {
        break;
      }
      buffer.write(temp[1] << 4 & 0xFF | temp[2] >>> 2 & 0xFF);
      do
      {
        temp[3] = data[(index++)];

        if (temp[3] == 61) {
          return buffer.toByteArray();
        }

        temp[3] = BASE64_DECODING_TABLE[temp[3]];
      }while ((index < data.length) && (temp[3] == -1));

      if (temp[3] == -1)
      {
        break;
      }
      buffer.write(temp[2] << 6 & 0xFF | temp[3]);
    }

    return buffer.toByteArray();
  }

  public static void main(String[] args)
  {
    String str = "RGVhciBKdWR5IFlvbmc6PGJyPjxicj4mbmJzcDsmbmJzcDsmbmJzcDsmbmJzcDtHZW5TY3JpcHQgd291bGQgbGlrZSB0byB3ZWxjb21lIHlvdSBhcyBhIG5ldyBjdXN0b21lciB0byBvdXIgZmlybS4gVGhlIGZvbGxvd2luZyBpcyB5b3VyIGFjY291bnQgaW5mb3JtYXRpb246PGJyPjxicj4mbmJzcDsmbmJzcDsmbmJzcDsmbmJzcDtBY2NvdW50IE51bWJlcjoxNDQ3MTg8YnI+Jm5ic3A7Jm5ic3A7Jm5ic3A7Jm5ic3A7RW1haWw6anVkeUBiaW9tYXJrZXRpbmcuY29tLm15PGJyPiZuYnNwOyZuYnNwOyZuYnNwOyZuYnNwO1Bhc3N3b3JkOkM4UTZ0Mjxicj48YnI+Jm5ic3A7Jm5ic3A7Jm5ic3A7Jm5ic3A7VG8gdmlldyBvciBtb2RpZnkgeW91ciBhY2NvdW50LCBwbGVhc2UgbG9nIGluIHZpYSA8YSBocmVmPSdodHRwczovL3d3dy5nZW5zY3JpcHQuY29tL3NzbC1iaW4vbXlhY2NvdW50Jz5odHRwczovL3d3dy5nZW5zY3JpcHQuY29tL3NzbC1iaW4vbXlhY2NvdW50LjwvYT48YnI+PGJyPiZuYnNwOyZuYnNwOyZuYnNwOyZuYnNwO0FzIHRoZSB3YXkgb2Ygd2VsY29taW5nIHlvdSwgd2UgaGF2ZSBlbmNsb3NlZCAxLDAwMCBFekNvdXBvblRNIHBvaW50cywgd2hpY2ggY291bGQgYmUgcmVkZWVtZWQgdG8gJDEwIGNvdXBvbi0tZW50aXRsZXMgeW91IHRvICQxMCBvZmYgb24geW91ciBuZXh0IG9yZGVyLiBUaGUgcG9pbnRzIHdpbGwgZXhwaXJlIDEyIG1vbnRocyBsYXRlci48YnI+PGJyPiZuYnNwOyZuYnNwOyZuYnNwOyZuYnNwO1RvIGxlYXJuIG1vcmUgYWJvdXQgRXpDb3Vwb25UTSBwb2ludHMsIHBsZWFzZSB2aXNpdDxhIGhyZWY9J2h0dHA6Ly93d3cuZ2Vuc2NyaXB0LmNvbS9lemNvdXBvbi5odG1sJz4gaHR0cDovL3d3dy5nZW5zY3JpcHQuY29tL2V6Y291cG9uLmh0bWw8L2E+PGJyPjxicj4mbmJzcDsmbmJzcDsmbmJzcDsmbmJzcDtHZW5TY3JpcHQgY29uc3RhbnRseSBsYXVuY2hlcyBuZXcgc2VydmljZXMgYW5kIHByb2R1Y3RzIHRvIG1lZXQgeW91ciBuZWVkcyBmb3IgbGlmZSBzY2llbmNlIHJlc2VhcmNoIGFuZCBkcnVnIGRpc2NvdmVyeS4gRmluZCBvdXQgbW9yZSBhdDxhIGhyZWY9J2h0dHA6Ly93d3cuZ2Vuc2NyaXB0LmNvbSc+IGh0dHA6Ly93d3cuZ2Vuc2NyaXB0LmNvbTwvYT48YnI+PGJyPiZuYnNwOyZuYnNwOyZuYnNwOyZuYnNwO1dlJ3JlIGxvb2tpbmcgZm9yd2FyZCB0byBzZXJ2aW5nIHlvdSBpbiB0aGUgbmVhciBmdXR1cmUuPGJyPjxicj48YnI+Jm5ic3A7Jm5ic3A7Jm5ic3A7Jm5ic3A7U2luY2VyZWx5Ljxicj4mbmJzcDsmbmJzcDsmbmJzcDsmbmJzcDtHZW5TY3JpcHQgQ3VzdG9tZXIgU2VydmljZTxicj4mbmJzcDsmbmJzcDsmbmJzcDs8YSBocmVmPSdodHRwOi8vd3d3LmdlbnNjcmlwdC5jb20nPmh0dHA6Ly93d3cuZ2Vuc2NyaXB0LmNvbTwvYT48YnI+";

    byte[] bs = decode(str);
    System.out.println(StringUtil.bytesToString(bs, 0, bs.length));
  }
}