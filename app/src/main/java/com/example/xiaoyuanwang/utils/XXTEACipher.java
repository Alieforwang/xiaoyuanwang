package com.example.xiaoyuanwang.utils;

import java.util.ArrayList;
import java.util.List;

public class XXTEACipher {
    private static final int DELTA = 0x9E3779B9;

    private XXTEACipher() {}

    /**
     * XXTEA加密
     * @param data 明文字符串
     * @param key 密钥字符串
     * @return 加密后的字节数据
     */
    public static byte[] encrypt(String data, String key) {
        if (data == null || data.length() == 0) {
            return new byte[0];
        }

        // Python: key = key[:16]
        if (key.length() > 16) {
            key = key.substring(0, 16);
        }

        int[] v = str2long(data, true);
        int[] k = str2long(key, false);

        // 确保密钥恰好是4个整数（16字节）
        if (k.length < 4) {
            int[] newK = new int[4];
            System.arraycopy(k, 0, newK, 0, k.length);
            k = newK;
        } else if (k.length > 4) {
             // 实际上 str2long 是每4个字符生成一个int，所以16个字符最多生成4个int。
             // 这里保留逻辑以防万一
             int[] newK = new int[4];
             System.arraycopy(k, 0, newK, 0, 4);
             k = newK;
        }

        int n = v.length - 1;
        int z = v[n];
        int y = v[0];
        int q = 6 + 52 / (n + 1);
        int sum = 0;

        while (q > 0) {
            sum += DELTA;
            int e = (sum >>> 2) & 3;

            for (int p = 0; p < n; p++) {
                y = v[p + 1];
                int mx = (z >>> 5 ^ y << 2) + (y >>> 3 ^ z << 4 ^ sum ^ y) + (k[(p & 3) ^ e] ^ z);
                z = v[p] += mx;
            }

            y = v[0];
            int mx = (z >>> 5 ^ y << 2) + (y >>> 3 ^ z << 4 ^ sum ^ y) + (k[(n & 3) ^ e] ^ z);
            z = v[n] += mx;

            q--;
        }

        return long2str(v, false);
    }

    private static int[] str2long(String s, boolean includeLength) {
        int length = s.length();
        List<Integer> v = new ArrayList<>();
        
        for (int i = 0; i < length; i += 4) {
            int val = (s.charAt(i) & 0xFF) |
                      ((i + 1 < length ? s.charAt(i + 1) & 0xFF : 0) << 8) |
                      ((i + 2 < length ? s.charAt(i + 2) & 0xFF : 0) << 16) |
                      ((i + 3 < length ? s.charAt(i + 3) & 0xFF : 0) << 24);
            v.add(val);
        }

        if (includeLength) {
            v.add(length);
        }

        int[] result = new int[v.size()];
        for (int i = 0; i < v.size(); i++) {
            result[i] = v.get(i);
        }
        return result;
    }

    private static byte[] long2str(int[] v, boolean includeLength) {
        int length = v.length;
        int n = (length - 1) << 2;

        if (includeLength) {
            int m = v[length - 1];
            if (m < n - 3 || m > n) {
                return new byte[0];
            }
            n = m;
        }

        byte[] result = new byte[length * 4];
        for (int i = 0; i < length; i++) {
            result[i * 4] = (byte) (v[i] & 0xFF);
            result[i * 4 + 1] = (byte) ((v[i] >>> 8) & 0xFF);
            result[i * 4 + 2] = (byte) ((v[i] >>> 16) & 0xFF);
            result[i * 4 + 3] = (byte) ((v[i] >>> 24) & 0xFF);
        }

        if (includeLength) {
            byte[] truncated = new byte[n];
            System.arraycopy(result, 0, truncated, 0, n);
            return truncated;
        } else {
            return result;
        }
    }
}
