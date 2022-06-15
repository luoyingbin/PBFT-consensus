package com.common.hash;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;


public class HmacSha256Utils {

    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA256";

    private static String toHexString(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    public static String signature(String data, String key) throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), HMAC_SHA1_ALGORITHM);
        Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
        mac.init(signingKey);
        return toHexString(mac.doFinal(data.getBytes()));
    }

    public static byte[] signatureReturnBytes(String data, String key) throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), HMAC_SHA1_ALGORITHM);
        Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
        mac.init(signingKey);
        return mac.doFinal(data.getBytes());

    }

    public static String SHA256(String param) {
        if (param == null || param.length() == 0) {
            throw new IllegalArgumentException("param can not be null");
        }
        try {
            byte[] bytes = param.getBytes("utf-8");
            final MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.reset();
            md.update(bytes);
            final Base64 base64 = new Base64();
            final byte[] enbytes = base64.encode(md.digest());
            return new String(enbytes);
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("unknown algorithm SHA-256");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getSignature(String date, String body, String secret) throws InvalidKeyException, UnsupportedEncodingException, NoSuchAlgorithmException {
        StringBuilder stb = new StringBuilder();
        String content = null;
        if (body == null) {
            content = stb.append("Date: ").append(date).toString();
        } else {
            content = stb.append("Date: ").append(date).append("\n").append("Digest: SHA-256=").append(SHA256(body)).toString();
        }
        final Base64 base64 = new Base64();
        String signature = new String(base64.encode(signatureReturnBytes(content, secret)), "US-ASCII");
        //java.util.Base64.getEncoder().encode(signatureReturnBytes(content, secret)
        return signature;
    }

    public static boolean verify(String signature, String date, String body, String secret) throws UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException {
        String newSignature = getSignature(date, body, secret);
        if (newSignature.equals(signature)) {
            return true;
        }
        return false;
    }

}
