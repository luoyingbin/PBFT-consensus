package com.common.key;

/**
 * @ClassName KeyMember
 * @Data 2019/11/8 11:26
 * @Auther Luo
 **/
public class KeyMember {
    private byte[] RawSKey = null;
    private byte[] rawPKey = null;
    private KeyType keyType_ = null;

    public byte[] getRawSKey() {
        return RawSKey;
    }

    public void setRawSKey(byte[] rawSKey) {
        this.RawSKey = rawSKey;
    }

    public byte[] getRawPKey() {
        return rawPKey;
    }

    public void setRawPKey(byte[] rawPKey) {
        this.rawPKey = rawPKey;
    }

    public KeyType getKeyType() {
        return keyType_;
    }

    public void setKeyType(KeyType keyType) {
        this.keyType_ = keyType;
    }
}
