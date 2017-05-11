package com.hexun.monitor.zookeeper;

import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCache;

/**
 * 注册中心.
 * 
 * @author zhangliang
 */
public interface RegistryCenter {
    
    /**
     * 获取注册数据.
     * 
     * @param key 键
     * @return 值
     */
    String get(String key);
    
    /**
     * 获取数据是否存在.
     * 
     * @param key 键
     * @return 数据是否存在
     */
    boolean isExisted(String key);
    
    /**
     * 持久化注册数据.
     * 
     * @param key 键
     * @param value 值
     */
    void persist(String key, String value);
    
    /**
     * 更新注册数据.
     * 
     * @param key 键
     * @param value 值
     */
    void update(String key, String value);
    
    /**
     * 删除注册数据.
     * 
     * @param key 键
     */
    void remove(String key);
    
    /**
     * 添加本地缓存.
     * 
     * @param cachePath 需加入缓存的路径
     */
    void addCacheData(String cachePath);
    
    /**
     * 获取注册中心数据缓存对象.
     * 
     * @param cachePath 缓存的节点路径
     * @return 注册中心数据缓存对象
     */
    TreeCache getCache(String cachePath);
    
    /**
     * 获取子节点名称集合.
     * 
     * @param key 键
     * @return 子节点名称集合
     */
    List<String> getChildrenKeys(String key);
    
    /**
     * 临时注册数据.
     * 
     * @param key 键
     * @param value 值
     */
    void ephemeral(String key, String value);
    
    /**
     * 持久化顺序注册数据.
     *
     * @param key 键
     * @param value 值
     * @return 包含10位顺序数字的znode名称
     */
    String persistSequential(String key, String value);
    
    /**
     * 临时顺序注册数据.
     * 
     * @param key 键
     */
    void ephemeralSequential(String key);
}
