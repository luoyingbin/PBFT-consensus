package com.pbft.view;

import com.common.StringUtils;
import com.db.DbManager;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @Author: luo
 * @Description:
 * @Data: 14:38 2021/9/9
 */
@Slf4j
public class ViewManager {
    private static ViewManager viewManager;
    public static ViewManager getProfile() {
        if (StringUtils.isNull(viewManager)) {
            viewManager = new ViewManager();
        }
        return viewManager;
    }


    private final ReentrantReadWriteLock nextViewNumberLock;
    private int nextViewNumber;


    private final ReentrantReadWriteLock viewNumberLock;
    private int viewNumber;


    private final ReentrantLock viewMapLock;
    Map<Integer, ViewSet>  viewMap;

    private ViewManager(){
        int viewNumber= DbManager.loadViewNumber();
        this.nextViewNumberLock =new ReentrantReadWriteLock();
        this.viewMapLock=new ReentrantLock();
        this.viewNumberLock=new ReentrantReadWriteLock();
        this.viewNumber=viewNumber;
        this.nextViewNumber = viewNumber;
        this.viewMap=new HashMap<>();
    }

    public int getViewNumber(){
        try {
            viewNumberLock.readLock().lock();
            return viewNumber;
        } finally {
            viewNumberLock.readLock().unlock();
        }
    }

    /**
     * @Author: luo
     * @Description: 更换view视图
     * 替换更高的viewNumber
     * 清除新viewNumber序号以下（不包含新view Number本身）的viewChange消息
     * @param viewNumber
     * @Data: 22:51 2021/9/11
     */
    public boolean tryUpViewNumber(int viewNumber){
        try {
            viewNumberLock.writeLock().lock();
            if(viewNumber > this.viewNumber){
                DbManager.saveViewNumber(viewNumber);
                this.viewNumber=viewNumber;
                setNextViewNumber(this.viewNumber);
                clearViewChangeManager(this.viewNumber);
                return true;
            }
            return false;
        } finally {
            viewNumberLock.writeLock().unlock();
        }
    }

    public int getNextViewNumber() {
        try {
            nextViewNumberLock.writeLock().lock();
            return ++this.nextViewNumber;
        }finally {
            nextViewNumberLock.writeLock().unlock();
        }
    }

    public ViewSet getViewChangeManager(int viewNumber){
        ViewSet viewSet =null;
        try {
            viewMapLock.lock();
            viewSet =viewMap.get(viewNumber);
            if(StringUtils.isNull(viewSet)){
                viewSet =new ViewSet(viewNumber);
                viewMap.put(viewNumber, viewSet);
            }
            return viewSet;
        }finally {
            viewMapLock.unlock();
        }
    }

    private void clearViewChangeManager(int viewNumber){
        try {
            viewMapLock.lock();
            List<Integer> deleteList=new ArrayList<>();
            for(Integer vn:viewMap.keySet()){
                if(viewNumber> vn){
                    deleteList.add(vn);
                }
            }
            deleteList.forEach(vn->viewMap.remove(vn));
        }finally {
            viewMapLock.unlock();
        }
    }

    private void setNextViewNumber(int viewNumber){
        try {
            nextViewNumberLock.writeLock().lock();
            this.nextViewNumber = viewNumber;
        }finally {
            nextViewNumberLock.writeLock().unlock();
        }
    }


}
