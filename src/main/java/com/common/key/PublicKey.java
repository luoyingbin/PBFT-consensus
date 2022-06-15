package com.common.key;

/**
 * @ClassName PublicKey
 * @Data 2019/11/8 11:20
 * @Auther Luo
 **/

import com.common.encryption.Base58;
import com.common.encryption.HexFormat;
import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;

import java.security.MessageDigest;
import java.security.Signature;
import java.util.Arrays;

public class PublicKey {
    private KeyMember keyMember = new KeyMember();

    public PublicKey() {
    }

    /**
     * structure with encrytion public toolkit.key
     */
    public PublicKey(String encPublicKey) throws EncException {
        getPublicKey(encPublicKey, keyMember);
    }

    /**
     * set enc public toolkit.key
     *
     * @param encPublicKey toolkit.encryption public toolkit.key
     * @throws EncException
     */
    public void setEncPublicKey(String encPublicKey) throws EncException {
        getPublicKey(encPublicKey, keyMember);
    }

    /**
     * set raw public toolkit.key
     *
     * @param rawPKey raw public toolkit.key
     */
    public void setRawPublicKey(byte[] rawPKey) {
        keyMember.setRawPKey(rawPKey);
    }

    /**
     * get raw public toolkit.key
     *
     * @return raw public toolkit.key
     */
    public byte[] getRawPublicKey() {
        return keyMember.getRawPKey();
    }

    /**
     * set toolkit.key type
     *
     * @param keyType toolkit.key type
     */
    public void setKeyType(KeyType keyType) {
        keyMember.setKeyType(keyType);
    }

    /**
     * get toolkit.key type
     *
     * @return toolkit.key type
     */
    private KeyType getKeyType() {
        return keyMember.getKeyType();
    }

    /**
     * @return encode address
     * @throws EncException
     */
    public String getEncAddress() throws EncException {
        byte[] raw_pkey = keyMember.getRawPKey();
        if (null == raw_pkey) {
            throw new EncException("public toolkit.key is null");
        }

        return encAddress(keyMember.getKeyType(), raw_pkey);
    }

    /**
     * @param pKey encode public toolkit.key
     * @return encode address
     * @throws EncException
     */
    public static String getEncAddress(String pKey) throws EncException {
        KeyMember member = new KeyMember();
        getPublicKey(pKey, member);

        return encAddress(member.getKeyType(), member.getRawPKey());
    }

    /**
     * @param encAddress encode address
     * @return true or false
     */
    public static boolean isAddressValid(String encAddress) {
        return encAddressValid(encAddress);
    }

    /**
     * @param encPublicKey encode public toolkit.key
     * @return true or false
     */
    public static boolean isPublicKeyValid(String encPublicKey) {
        return encPublicKeyValid(encPublicKey);
    }

    /**
     * check sign datas
     *
     * @param msg     source message
     * @param signMsg signed message
     * @return true or false
     * @throws EncException
     */
    public boolean verify(byte[] msg, byte[] signMsg) throws EncException {
        boolean verifySuccess = verifyMessage(msg, signMsg, keyMember);

        return verifySuccess;
    }

    /**
     * check sign datas
     *
     * @param msg          source message
     * @param signMsg      signed message
     * @param encPublicKey enc public toolkit.key
     * @return true or false
     * @throws EncException
     */
    public static boolean verify(byte[] msg, byte[] signMsg, String encPublicKey) throws EncException {
        boolean verifySuccess = false;
        KeyMember member = new KeyMember();
        getPublicKey(encPublicKey, member);
        verifySuccess = verifyMessage(msg, signMsg, member);

        return verifySuccess;
    }

    private static void getPublicKey(String bPkey, KeyMember member) throws EncException {
        if (null == bPkey) {
            throw new EncException("public toolkit.key cannot be null");
        }

        if (!HexFormat.isHexString(bPkey)) {
            throw new EncException("public toolkit.key (" + bPkey + ") is invalid, please check");
        }

        KeyType type = null;
        byte[] buffPKey = HexFormat.hexToByte(bPkey);

        if (buffPKey.length < 6) {
            throw new EncException("public toolkit.key (" + bPkey + ") is invalid, please check");
        }

        if (buffPKey[0] != (byte) 0xB0) {
            throw new EncException("public toolkit.key (" + bPkey + ") is invalid, please check");
        }

        if (buffPKey[1] != 1 && buffPKey[1] != 2) {
            throw new EncException("public toolkit.key (" + bPkey + ") is invalid, please check");
        }
        type = KeyType.values()[buffPKey[1] - 1];

        // checksum
        if (!CheckKey.CheckSum(type, buffPKey)) {
            throw new EncException("public toolkit.key (" + bPkey + ") is invalid, please check");
        }

        byte[] rawPKey = new byte[buffPKey.length - 6];
        System.arraycopy(buffPKey, 2, rawPKey, 0, rawPKey.length);
        member.setRawPKey(rawPKey);
        member.setKeyType(type);
    }

    private static boolean encPublicKeyValid(String encPublicKey) {
        boolean valid;
        try {
            if (null == encPublicKey) {
                throw new EncException("Invalid publicKey");
            }
            if (!HexFormat.isHexString(encPublicKey)) {
                throw new EncException("Invalid publicKey");
            }

            byte[] buffPKey = HexFormat.hexToByte(encPublicKey);
            if (buffPKey.length < 6 || buffPKey[0] != (byte) 0xB0 || (buffPKey[1] != (byte) 0x01 && buffPKey[1] != (byte) 0x02)) {
                throw new EncException("Invalid publicKey");
            }

            int len = buffPKey.length;
            byte[] checkSum = new byte[4];
            System.arraycopy(buffPKey, len - 4, checkSum, 0, 4);

            byte[] buff = new byte[len - 4];
            System.arraycopy(buffPKey, 0, buff, 0, len - 4);

            KeyType type = KeyType.values()[buffPKey[1] - 1];
            byte[] hash1 = CheckKey.CalHash(type, buff);
            byte[] hash2 = CheckKey.CalHash(type, hash1);

            byte[] checkSumCol = new byte[4];
            System.arraycopy(hash2, 0, checkSumCol, 0, 4);
            if (!Arrays.equals(checkSum, checkSumCol)) {
                throw new EncException("Invalid publicKey");
            }

            valid = true;
        } catch (Exception exception) {
            valid = false;
        }
        return valid;
    }

    private static String encAddress(KeyType type, byte[] raw_pkey) {
        byte[] buff = new byte[23];
        //0X01 0X56 bu
        //0X05 0X35 hp
        buff[0] = (byte) 0x04;
        buff[1] = (byte) 0x8b;
        buff[2] = (byte) 0x33;
        buff[3] = (byte) (type.ordinal() + 1);

        byte[] hashPkey = CheckKey.CalHash(type, raw_pkey);
        System.arraycopy(hashPkey, 13, buff, 4, 19);

        byte[] hash1 = CheckKey.CalHash(type, buff);
        byte[] hash2 = CheckKey.CalHash(type, hash1);
        byte[] tmp = new byte[27];

        System.arraycopy(buff, 0, tmp, 0, buff.length);
        System.arraycopy(hash2, 0, tmp, buff.length, 4);

        return Base58.encode(tmp);
    }

    private static boolean encAddressValid(String encAddress) {
        boolean valid;
        try {
            if (null == encAddress) {
                throw new EncException("Invalid address");
            }
            byte[] addressTemp = Base58.decode(encAddress);
            if (addressTemp.length != 27 ||
                    addressTemp[0] != (byte) 0x04 ||
                    addressTemp[1] != (byte) 0x8b ||
                    addressTemp[2] != (byte) 0x33 ||
                    (addressTemp[3] != (byte) 0x01 && addressTemp[3] != (byte) 0x02)) {
                throw new EncException("Invalid address");
            }

            int len = addressTemp.length;
            byte[] checkSum = new byte[4];
            System.arraycopy(addressTemp, len - 4, checkSum, 0, 4);

            byte[] buff = new byte[len - 4];
            System.arraycopy(addressTemp, 0, buff, 0, len - 4);

            KeyType type = KeyType.values()[addressTemp[3] - 1];
            byte[] hash1 = CheckKey.CalHash(type, buff);
            byte[] hash2 = CheckKey.CalHash(type, hash1);

            byte[] checkSumCol = new byte[4];
            System.arraycopy(hash2, 0, checkSumCol, 0, 4);
            if (!Arrays.equals(checkSum, checkSumCol)) {
                throw new EncException("Invalid address");
            }

            valid = true;
        } catch (Exception e) {
            valid = false;
        }

        return valid;
    }

    private static boolean verifyMessage(byte[] msg, byte[] sign, KeyMember member) {
        boolean verifySuccess;
        try {
            switch (member.getKeyType()) {
                case ED25519: {
                    Signature sgr = new EdDSAEngine(MessageDigest.getInstance("SHA-512"));
                    EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName("ed25519-sha-512");
                    EdDSAPublicKeySpec eddsaPubKey = new EdDSAPublicKeySpec(member.getRawPKey(), spec);
                    EdDSAPublicKey vKey = new EdDSAPublicKey(eddsaPubKey);
                    sgr.initVerify(vKey);
                    sgr.update(msg);
                    verifySuccess = sgr.verify(sign);
                    break;
                }
                default:
                    throw new EncException("type does not exist");
            }
        } catch (Exception e) {
            verifySuccess = false;
        }

        return verifySuccess;
    }
}

