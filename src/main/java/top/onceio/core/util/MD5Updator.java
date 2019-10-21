package top.onceio.core.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Updator {
    private MessageDigest md = null;

    public MD5Updator() {
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public void update(int val) {
        byte[] bytes = new byte[]{(byte) (val >> 24), (byte) (val >> 16), (byte) (val >> 8), (byte) (val)};
        md.update(bytes);
    }

    public void update(long val) {
        byte[] bytes = new byte[]{(byte) (val >> 56), (byte) (val >> 48), (byte) (val >> 40), (byte) (val >> 32), (byte) (val >> 24), (byte) (val >> 16), (byte) (val >> 8), (byte) (val)};
        md.update(bytes);
    }

    public void update(byte[] bytes) {
        md.update(bytes);
    }

    public void update(String str) {
        md.update(str.getBytes());
    }

    public void reset() {
        md.reset();
    }

    public byte[] digest() {
        return md.digest();
    }

    public String toHexString() {
        return new BigInteger(1, md.digest()).toString(16);
    }

    public BigInteger toBigInteger() {
        return new BigInteger(1, md.digest());
    }
}