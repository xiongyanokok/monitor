package com.xy.monitor.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;

/**
 * 监听所有子节点变化
 * 
 * @author xiongyan
 * @date 2017年4月18日 上午10:08:52
 */
public class ListenServersChangedListener implements TreeCacheListener {
	
	private ZookeeperRegistryCenter zookeeperRegistryCenter;
	
	public ListenServersChangedListener(ZookeeperRegistryCenter zookeeperRegistryCenter) {
		this.zookeeperRegistryCenter = zookeeperRegistryCenter;
	}
    
	@Override
	public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
		switch (event.getType()) {
			case NODE_ADDED :
				System.out.println("TreeNode added: " + event.getData().getPath() + " , data: " + new String(event.getData().getData()));
				break;
			case NODE_UPDATED :
				System.out.println("TreeNode updated: " + event.getData().getPath() + " , data: " + new String(event.getData().getData()));
				break;
			case NODE_REMOVED :
				System.out.println("TreeNode removed: " + event.getData().getPath());
				break;
			default :
				break;
		}  
	}
}
