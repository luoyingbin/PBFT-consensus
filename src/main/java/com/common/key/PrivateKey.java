package com.common.key;

/**
 * @ClassName PrivateKey
 * @Data 2019/11/8 11:20
 * @Auther Luo
 **/

import cfca.sadk.algorithm.common.Mechanism;
import cfca.sadk.algorithm.common.PKIException;
import cfca.sadk.algorithm.sm2.SM2PrivateKey;
import cfca.sadk.algorithm.sm2.SM2PublicKey;
import cfca.sadk.algorithm.util.BigIntegerUtil;
import cfca.sadk.lib.crypto.JCrypto;
import cfca.sadk.lib.crypto.Session;
import cfca.sadk.org.bouncycastle.asn1.ASN1Integer;
import cfca.sadk.org.bouncycastle.asn1.ASN1Sequence;
import cfca.sadk.org.bouncycastle.util.encoders.Base64;
import cfca.sadk.util.HashUtil;
import cfca.sadk.util.KeyUtil;
import com.common.encryption.Base58;
import com.common.encryption.HexFormat;
import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.KeyPairGenerator;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.*;
import java.util.Arrays;


public class PrivateKey {
    private PublicKey publicKey = new PublicKey();
    private KeyMember keyMember = new KeyMember();

    /**
     * generate toolkit.key pair (default: ed25519)
     *
     * @throws EncException
     */
    public PrivateKey() throws EncException, PKIException {
        this(KeyType.ED25519);
    }

    /**
     * generate toolkit.key pair
     *
     * @param type the type of toolkit.key
     * @throws EncException
     */
    public PrivateKey(KeyType type) throws EncException, PKIException {
        switch (type) {
            case ED25519: {
                KeyPairGenerator keyPairGenerator = new KeyPairGenerator();
                KeyPair keyPair = keyPairGenerator.generateKeyPair();
                EdDSAPrivateKey priKey = (EdDSAPrivateKey) keyPair.getPrivate();
                EdDSAPublicKey pubKey = (EdDSAPublicKey) keyPair.getPublic();
                keyMember.setRawSKey(priKey.getSeed());
                publicKey.setRawPublicKey(pubKey.getAbyte());
                break;
            }
            case ECCSM2: {
                String deviceName = "JSOFT_LIB";
                JCrypto.getInstance().initialize("JSOFT_LIB", null);
                Session session = JCrypto.getInstance().openSession("JSOFT_LIB");
                KeyPair keypair = KeyUtil.generateKeyPair((Mechanism) new Mechanism("SM2"), (int) 256, (Session) session);
                SM2PublicKey pubKey = (SM2PublicKey) keypair.getPublic();
                SM2PrivateKey priKey = (SM2PrivateKey) keypair.getPrivate();
                keyMember.setRawSKey(priKey.getD_Bytes());//存储密钥
                keyMember.setRawPKey(getSM2PublicKey(pubKey));//存储bate类型公钥
                publicKey.setRawPublicKey(getSM2PublicKey(pubKey));//存储bate类型公钥
                break;
            }
            default:
                throw new EncException("type does not exist");
        }
        setKeyType(type);
        publicKey.setKeyType(type);
    }

    /**
     * generate toolkit.key pair
     *
     * @param seed the seed
     */
    public PrivateKey(byte[] seed) {
        EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName("ed25519-sha-512");
        EdDSAPrivateKeySpec privKey = new EdDSAPrivateKeySpec(seed, spec);
        EdDSAPublicKeySpec spec2 = new EdDSAPublicKeySpec(privKey.getA(), spec);
        EdDSAPublicKey pDsaPublicKey = new EdDSAPublicKey(spec2);
        publicKey.setRawPublicKey(pDsaPublicKey.getAbyte());
        keyMember.setRawSKey(seed);
        setKeyType(KeyType.ED25519);
        publicKey.setKeyType(KeyType.ED25519);
    }

    /**
     * generate toolkit.key pair
     *
     * @param skey private toolkit.key
     * @throws EncException
     */
    public PrivateKey(String skey) throws EncException {
        getPrivateKey(skey, keyMember);
        publicKey.setKeyType(keyMember.getKeyType());
        byte[] rawPKey = getPublicKey(keyMember);
        publicKey.setRawPublicKey(rawPKey);
        keyMember.setRawPKey(rawPKey);
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
    public KeyType getKeyType() {
        return keyMember.getKeyType();
    }

    /**
     * set raw private toolkit.key
     *
     * @param rawSKey private toolkit.key
     */
    public void setRawPrivateKey(byte[] rawSKey) {
        keyMember.setRawSKey(rawSKey);
    }

    /**
     * get raw private toolkit.key
     *
     * @return raw private toolkit.key
     */
    public byte[] getRawPrivateKey() {
        return keyMember.getRawSKey();
    }

    /**
     * get public toolkit.key
     *
     * @return public toolkit.key
     */
    public PublicKey getPublicKey() {
        return publicKey;
    }

    /**
     * @return encode private toolkit.key
     * @throws EncException
     */
    public String getEncPrivateKey() throws EncException {
        byte[] rawSKey = keyMember.getRawSKey();
        if (rawSKey == null) {
            throw new EncException("raw private toolkit.key is null");
        }
        return EncPrivateKey(keyMember.getKeyType(), keyMember.getRawSKey());
    }

    /**
     * @param encPrivateKey encode private toolkit.key
     * @return true or false
     */
    public static boolean isPrivateKeyValid(String encPrivateKey) {
        return encPrivateKeyValid(encPrivateKey);
    }


    /**
     * @return encode public toolkit.key
     * @throws EncException
     */
    public String getEncPublicKey() throws EncException {
        byte[] rawPKey = publicKey.getRawPublicKey();
        if (rawPKey == null) {
            throw new EncException("raw public toolkit.key is null");
        }
        return encPublicKey(keyMember.getKeyType(), rawPKey).toLowerCase();
    }

    /**
     * @param skey encode private toolkit.key
     * @return encode public toolkit.key
     * @throws EncException
     */
    public static String getEncPublicKey(String skey) throws EncException {
        KeyMember member = new KeyMember();
        getPrivateKey(skey, member);
        byte[] rawPKey = getPublicKey(member);
        return encPublicKey(member.getKeyType(), rawPKey).toLowerCase();
    }

    /**
     * @param encPublicKey encode public toolkit.key
     * @return true or false
     */
    public static boolean isPublicKeyValid(String encPublicKey) {
        return PublicKey.isPublicKeyValid(encPublicKey);
    }

    /**
     * @return encode address
     * @throws EncException
     */
    public String getEncAddress() throws EncException {
        return publicKey.getEncAddress();
    }

    /**
     * @param pKey encode public toolkit.key
     * @return encode address
     * @throws EncException
     */
    public static String getEncAddress(String pKey) throws EncException {
        return PublicKey.getEncAddress(pKey);
    }

    /**
     * @param encAddress encode address
     * @return true or false
     */
    public static boolean isAddressValid(String encAddress) {
        return PublicKey.isAddressValid(encAddress);
    }

    /**
     * sign message
     *
     * @param msg message
     * @return sign data
     * @throws EncException
     */
    public byte[] sign(byte[] msg) throws EncException, UnsupportedEncodingException, PKIException {
        return signMessage(msg, keyMember);
    }

    /**
     * sign message
     *
     * @param msg  message
     * @param skey private toolkit.key
     * @return sign data
     * @throws EncException
     */
    public static byte[] sign(byte[] msg, String skey) throws EncException, UnsupportedEncodingException, PKIException {
        KeyMember member = new KeyMember();
        getPrivateKey(skey, member);
        byte[] rawPKey = getPublicKey(member);
        member.setRawPKey(rawPKey);
        return signMessage(msg, member);
    }

    private static void getPrivateKey(String bSkey, KeyMember member) throws EncException {
        try {
            if (null == bSkey) {
                throw new EncException("Private toolkit.key cannot be null");
            }

            byte[] skeyTmp = Base58.decode(bSkey);
            if (skeyTmp.length <= 9) {
                throw new EncException("Private toolkit.key (" + bSkey + ") is invalid");
            }

            if (skeyTmp[3] != 1 && skeyTmp[3] != 2) {
                throw new EncException("Private toolkit.key (" + bSkey + ") is invalid");
            }
            KeyType type = KeyType.values()[skeyTmp[3] - 1];

            // checksum
            if (!CheckKey.CheckSum(type, skeyTmp)) {
                throw new EncException("Private toolkit.key (" + bSkey + ") is invalid");
            }

            byte[] rawSKey = new byte[skeyTmp.length - 9];
            System.arraycopy(skeyTmp, 4, rawSKey, 0, rawSKey.length);

            member.setKeyType(type);
            member.setRawSKey(rawSKey);
        } catch (Exception e) {
            throw new EncException("Invalid privateKey");
        }

    }

    private static byte[] getPublicKey(KeyMember member) throws EncException {
        byte[] rawPKey = null;
        switch (member.getKeyType()) {
            case ED25519: {
                EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName("ed25519-sha-512");
                EdDSAPrivateKeySpec privKey = new EdDSAPrivateKeySpec(member.getRawSKey(), spec);
                EdDSAPublicKeySpec spec2 = new EdDSAPublicKeySpec(privKey.getA(), spec);
                EdDSAPublicKey pDsaPublicKey = new EdDSAPublicKey(spec2);
                rawPKey = pDsaPublicKey.getAbyte();
                break;
            }
            case ECCSM2: {
                SM2PrivateKey privateKey = new SM2PrivateKey(member.getRawSKey());
                SM2PublicKey publicKey = privateKey.getSM2PublicKey();
                rawPKey = getSM2PublicKey(publicKey);
                break;
                // if (cfca.sadk.org.bouncycastle.util.Arrays.areEqual((byte[])getSM2PublicKey(publicKey), (byte[])rawPkey)) break;
                //throw new Exception("the private toolkit.key does not match the public toolkit.key, please check");
            }
            default:
                throw new EncException("Type does not exist");
        }
        return rawPKey;
    }

    private static String EncPrivateKey(KeyType type, byte[] raw_skey) throws EncException {
        if (null == raw_skey) {
            throw new EncException("Private toolkit.key is null");
        }
        byte[] buff = new byte[raw_skey.length + 5];
        buff[0] = (byte) 0xDA;
        buff[1] = (byte) 0x37;
        buff[2] = (byte) 0x9F;
        System.arraycopy(raw_skey, 0, buff, 4, raw_skey.length);

        buff[3] = (byte) (type.ordinal() + 1);

        byte[] hash1 = CheckKey.CalHash(type, buff);
        byte[] hash2 = CheckKey.CalHash(type, hash1);

        byte[] tmp = new byte[buff.length + 4];

        System.arraycopy(buff, 0, tmp, 0, buff.length);
        System.arraycopy(hash2, 0, tmp, buff.length, 4);

        return Base58.encode(tmp);
    }

    private static boolean encPrivateKeyValid(String encPrivateKey) {
        boolean valid;
        try {
            if (null == encPrivateKey) {
                throw new EncException("Invalid privateKey");
            }

            byte[] privateKeyTemp = Base58.decode(encPrivateKey);

            if (privateKeyTemp.length != 41 || privateKeyTemp[0] != (byte) 0xDA || privateKeyTemp[1] != (byte) 0x37 ||
                    privateKeyTemp[2] != (byte) 0x9F || (privateKeyTemp[3] != (byte) 0x01 && privateKeyTemp[3] != (byte) 0x02)) {
                throw new EncException("Invalid privateKey");
            }

            int len = privateKeyTemp.length;

            byte[] checkSum = new byte[4];
            System.arraycopy(privateKeyTemp, len - 4, checkSum, 0, 4);

            byte[] buff = new byte[len - 4];
            System.arraycopy(privateKeyTemp, 0, buff, 0, len - 4);

            KeyType type = KeyType.values()[privateKeyTemp[3] - 1];
            byte[] hash1 = CheckKey.CalHash(type, buff);
            byte[] hash2 = CheckKey.CalHash(type, hash1);

            byte[] checkSumCol = new byte[4];
            System.arraycopy(hash2, 0, checkSumCol, 0, 4);
            if (!Arrays.equals(checkSum, checkSumCol)) {
                throw new EncException("Invalid privateKey");
            }

            valid = true;
        } catch (Exception e) {
            valid = false;
        }
        return valid;
    }

    private static String encPublicKey(KeyType type, byte[] raw_pkey) throws EncException {
        if (null == raw_pkey) {
            throw new EncException("Public toolkit.key is null");
        }
        int length = raw_pkey.length + 2;
        byte[] buff = new byte[length];
        buff[0] = (byte) 0xB0;
        buff[1] = (byte) (type.ordinal() + 1);

        System.arraycopy(raw_pkey, 0, buff, 2, raw_pkey.length);

        byte[] hash1 = CheckKey.CalHash(type, buff);
        byte[] hash2 = CheckKey.CalHash(type, hash1);
        byte[] tmp = new byte[buff.length + 4];

        System.arraycopy(buff, 0, tmp, 0, buff.length);
        System.arraycopy(hash2, 0, tmp, buff.length, 4);

        return HexFormat.byteToHex(tmp);
    }

    private static byte[] signMessage(byte[] msg, KeyMember member) throws EncException, PKIException, UnsupportedEncodingException {
        if (null == member.getRawSKey()) {
            throw new EncException("Raw private toolkit.key is null");
        }
        byte[] signMsg = null;

        try {
            switch (member.getKeyType()) {
                case ED25519: {
                    Signature sgr = new EdDSAEngine(MessageDigest.getInstance("SHA-512"));
                    EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName("ed25519-sha-512");
                    EdDSAPrivateKeySpec sKeySpec = new EdDSAPrivateKeySpec(member.getRawSKey(), spec);
                    EdDSAPrivateKey sKey = new EdDSAPrivateKey(sKeySpec);
                    sgr.initSign(sKey);
                    sgr.update(msg);

                    signMsg = sgr.sign();
                    break;
                }
                case ECCSM2: {
                    String deviceName = "JSOFT_LIB";
                    JCrypto.getInstance().initialize("JSOFT_LIB", null);
                    Session session = JCrypto.getInstance().openSession("JSOFT_LIB");
                    cfca.sadk.util.Signature signature = new cfca.sadk.util.Signature();
                    SM2PrivateKey privateKey = KeyUtil.getSM2PrivateKey((byte[]) member.getRawSKey(), null, null);
                    SM2PublicKey publicKey = getSM2PublicKey(member.getRawPKey());
                    byte[] userId = "1234567812345678".getBytes("UTF8");
                    String signAlg = "sm3WithSM2Encryption";
                    byte[] hash = HashUtil.SM2HashMessageByBCWithZValue((byte[]) userId, (byte[]) msg, (BigInteger) publicKey.getPubXByInt(), (BigInteger) publicKey.getPubYByInt());
                    signMsg = ASN1toRS(Base64.decode((byte[]) signature.p1SignByHash("sm3WithSM2Encryption", hash, (java.security.PrivateKey) privateKey, session)));
                    break;
                }
                default:
                    throw new EncException("Type does not exist");
            }
        } catch (NoSuchAlgorithmException e) {
            throw new EncException("System error");
        } catch (InvalidKeyException e) {
            throw new EncException("Invalid privateKey");
        } catch (SignatureException e) {
            throw new EncException("Sign message failed");
        }

        return signMsg;
    }

    private static byte[] getSM2PublicKey(SM2PublicKey pubKey) {
        byte[] raw_pkey = new byte[65];
        byte[] x = pubKey.getPubX();
        byte[] y = pubKey.getPubY();
        raw_pkey[0] = 4;
        System.arraycopy(x, 0, raw_pkey, 1, 32);
        System.arraycopy(y, 0, raw_pkey, 33, 32);
        return raw_pkey;
    }

    private static SM2PublicKey getSM2PublicKey(byte[] raw_pkey) {
        byte[] x = new byte[32];
        byte[] y = new byte[32];
        System.arraycopy(raw_pkey, 1, x, 0, 32);
        System.arraycopy(raw_pkey, 33, y, 0, 32);
        SM2PublicKey publicKey = new SM2PublicKey(x, y);
        return publicKey;
    }

    private static byte[] ASN1toRS(byte[] asn1RS) {
        ASN1Sequence sequence = ASN1Sequence.getInstance((Object) asn1RS);
        ASN1Integer R = (ASN1Integer) sequence.getObjectAt(0);
        ASN1Integer S = (ASN1Integer) sequence.getObjectAt(1);
        byte[] r = BigIntegerUtil.asUnsigned32ByteArray((BigInteger) R.getPositiveValue());
        byte[] s = BigIntegerUtil.asUnsigned32ByteArray((BigInteger) S.getPositiveValue());
        byte[] signature = new byte[64];
        System.arraycopy(r, 0, signature, 0, 32);
        System.arraycopy(s, 0, signature, 32, 32);
        return signature;
    }

}

