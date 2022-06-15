package com.pbft;


import cfca.sadk.algorithm.common.PKIException;
import com.common.StringUtils;
import com.common.encryption.HexFormat;
import com.common.key.PrivateKey;
import com.common.key.PublicKey;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: luo
 * @Description: 验证节点管理器
 * 单例类
 * @Data: 14:32 2021/8/30
 */
@Data
@Slf4j
public class ValidatorManager {

    private static ValidatorManager validatorManager;
    public static ValidatorManager getProfile() {
        if (StringUtils.isNull(validatorManager)) {
            validatorManager = new ValidatorManager();
        }
        return validatorManager;
    }

    /** 验证节点列表*/
    private List<String>  validatorList;
    /** 自身的节点 */
    private String oneSelfAddress;
    /** 判断自身是否是共识角色*/
    private boolean isValidator;
    /** 自身在验证节点列表中序号,缺省值-1*/
    private int oneSelfOrderNumber;

    private String publicKey;

    private PrivateKey privateKey;

    /**
     * ccs3D2dQHJk2KdT7HnYXbpQWFoAV5nPe4TbG privbzL6Vt2gSuGqchR9rfUHSkKsq7Pfzrv3r6kFn9GBibQFUT9dTwxE
     * ccs3GufLniMhbTRr19QP5WL22hMHGa86AhyF privbtKXxDeSeE8FBYKwSeZMJtDdCrGThDdLycwrbravmmKDwvxbHpDu
     * ccs3CUMwePYUHCzFuytbKk3KURiH2gSPsyFp privbzfV7Xm39zmsdGR3BJ4ThT5puaoVC2bo9m6FVLwNd4RB4CTPDWxh
     * ccs3Eez8KkKf36GAzNZAQaHw4q2MAPWRCCGG privbsfW4KEmhYkyYYHgswN9nTnm38qG5zopywyHymSnT38UQjvkTyL7
     * ccs3EytoEBFVDCNfhTru1jcj4WJUX1Sorgh8 privbzcX9xrB36ZRMq8LPKBwzUk91gfyZNURDkokshttANCafoKowJMb
     * */

    List<String>  privateList;
    public void init(int port) {
        privateList = new ArrayList<>();
        privateList.add("privbzL6Vt2gSuGqchR9rfUHSkKsq7Pfzrv3r6kFn9GBibQFUT9dTwxE");
        privateList.add("privbtKXxDeSeE8FBYKwSeZMJtDdCrGThDdLycwrbravmmKDwvxbHpDu");
        privateList.add("privbzfV7Xm39zmsdGR3BJ4ThT5puaoVC2bo9m6FVLwNd4RB4CTPDWxh");
        privateList.add("privbsfW4KEmhYkyYYHgswN9nTnm38qG5zopywyHymSnT38UQjvkTyL7");
        privateList.add("privbzcX9xrB36ZRMq8LPKBwzUk91gfyZNURDkokshttANCafoKowJMb");
        String privateKeyTemp = privateList.get(port % 8000);
        privateKey = new PrivateKey( privateKeyTemp);
        oneSelfAddress = privateKey.getEncAddress();
        publicKey = privateKey.getEncPublicKey();
        oneSelfOrderNumber = validatorList.indexOf(oneSelfAddress);
    }

    public void addValidator(String address){
       if(validatorList.contains(address)){
           return;
       }
       validatorList.add(address);
       oneSelfOrderNumber = validatorList.indexOf(oneSelfAddress);
    }
    public void delValidator(String address){
        if(validatorList.contains(address)){
            validatorList.remove(address);
            oneSelfOrderNumber = validatorList.indexOf(oneSelfAddress);
        }
    }

    private ValidatorManager() {
        validatorList = new ArrayList<>();
        validatorList.add("ccs3D2dQHJk2KdT7HnYXbpQWFoAV5nPe4TbG");
        validatorList.add("ccs3GufLniMhbTRr19QP5WL22hMHGa86AhyF");
        validatorList.add("ccs3CUMwePYUHCzFuytbKk3KURiH2gSPsyFp");
        validatorList.add("ccs3Eez8KkKf36GAzNZAQaHw4q2MAPWRCCGG");
        privateKey = new PrivateKey("privbzL6Vt2gSuGqchR9rfUHSkKsq7Pfzrv3r6kFn9GBibQFUT9dTwxE");
        oneSelfAddress = privateKey.getEncAddress();
        publicKey=privateKey.getEncPublicKey();
        isValidator = true;
        oneSelfOrderNumber = validatorList.indexOf(oneSelfAddress);
        if (oneSelfOrderNumber == -1) {
            isValidator = false;
        }
    }

    public String getAddress(int index){
       return validatorList.get(index);
    }

    public List<String> getList(){
        return validatorList;
    }


    public boolean isLeader(int viewNumber){
        //非共识角色直接返回
        if(!isValidator){
            return false;
        }
        //判定当前视图序号是否等于节点自身序号
       return viewNumber % validatorList.size() == oneSelfOrderNumber;
    }
    /**
     * @Author: luo
     * @Description:
     * @param byteTx 原数据
     * @return sign_data
     * @Data: 16:54 2021/8/31
     */
    public String sign(byte[] byteTx){
        try {
            char[] signByte = HexFormat.encodeHex(privateKey.sign(byteTx));
           return String.valueOf(signByte);
        } catch (UnsupportedEncodingException | PKIException e) {
            e.printStackTrace();
        }
        return "";
    }

    public int verifyAndGetOrderNumber(String pubK,String signData,byte[] data) {
        PublicKey publicKey = null;
        try {
            publicKey = new PublicKey(pubK);
            if (!publicKey.verify(data, HexFormat.decodeHex(signData.toCharArray()))) {
                return -2;
            }
            int orderNumber = validatorList.indexOf(PublicKey.getEncAddress(pubK));
            if (orderNumber == -1) {
                return orderNumber;
            }

            return orderNumber;
        } catch (Exception e) {
            log.debug(e.getMessage());
            return -2;
        }
    }

    /**
     * count:3F+1
     * quorum:2f+1
    */
    public int getQuorumSize() {
        int count = validatorList.size();
        if (count < 4) {
            return 1;
        }
        int temp = 0;
        if ((count - 1) % 3 != 0) {
            temp = 1;
        }
        return ((count - 1) / 3) * 2 + 1 + temp;
    }
}
