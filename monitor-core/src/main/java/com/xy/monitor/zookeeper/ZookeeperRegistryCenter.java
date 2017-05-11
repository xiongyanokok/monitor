package com.xy.monitor.zookeeper;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.CuratorFrameworkFactory.Builder;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.CloseableUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.xy.monitor.task.PerformanceTask;
import com.xy.monitor.util.LocalHostService;

/**
 * zookeeper 注册中心.
 * 
 * @author xiongyan
 * @date 2017年4月17日 下午5:01:04
 */
public final class ZookeeperRegistryCenter extends ZookeeperConfiguration implements RegistryCenter, InitializingBean, DisposableBean, ApplicationListener<ContextRefreshedEvent> {
	
	/**
	 * logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(ZookeeperRegistryCenter.class);
	
	/**
	 * TreeCache
	 */
	private final Map<String, TreeCache> caches = new HashMap<>();
	
	/**
	 * 获取真实本机网络的服务
	 */
	private static final LocalHostService lhs = new LocalHostService();
    
    /**
     * Zookeeper framework client
     */
    private CuratorFramework client;
    
    /**
     * RequestMappingHandlerMapping
     */
    private RequestMappingHandlerMapping requestMappingHandlerMapping;
    
    /**
     * 销毁
     */
    @Override
	public void destroy() throws Exception {
    	for (Entry<String, TreeCache> each : caches.entrySet()) {
            each.getValue().close();
        }
    	waitForCacheClose();
        CloseableUtils.closeQuietly(client);
	}

    /**
     * 初始化
     */
	@Override
	public void afterPropertiesSet() throws Exception {
		logger.debug("monitor: zookeeper registry center init, server lists is: {}.", this.getServerLists());
		Builder builder = CuratorFrameworkFactory.builder()
                .connectString(this.getServerLists())
                .retryPolicy(new ExponentialBackoffRetry(this.getBaseSleepTimeMilliseconds(), this.getMaxRetries(), this.getMaxSleepTimeMilliseconds()))
                .namespace(this.getNamespace());
        if (0 != this.getSessionTimeoutMilliseconds()) {
            builder.sessionTimeoutMs(this.getSessionTimeoutMilliseconds());
        }
        if (0 != this.getConnectionTimeoutMilliseconds()) {
            builder.connectionTimeoutMs(this.getConnectionTimeoutMilliseconds());
        }
        if (!Strings.isNullOrEmpty(this.getDigest())) {
            builder.authorization("digest", this.getDigest().getBytes(Charsets.UTF_8))
               .aclProvider(new ACLProvider() {
                   
                   @Override
                   public List<ACL> getDefaultAcl() {
                       return ZooDefs.Ids.CREATOR_ALL_ACL;
                   }
                   
                   @Override
                   public List<ACL> getAclForPath(final String path) {
                       return ZooDefs.Ids.CREATOR_ALL_ACL;
                   }
               });
        }
        client = builder.build();
        client.start();
        try {
            if (!client.blockUntilConnected(this.getMaxSleepTimeMilliseconds() * this.getMaxRetries(), TimeUnit.MILLISECONDS)) {
                client.close();
                throw new KeeperException.OperationTimeoutException();
            }
        } catch (final Exception ex) {
            ZookeeperExceptionHandler.handleException(ex);
        }
	}
	
	/**
	 * 当spring容器初始化完成后执行该方法
	 */
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (null != event.getApplicationContext().getParent()) {
			logger.debug("monitor: zookeeper registry node");
			
			String basePath = "/";
			// 对根节点下所有孩子节点的监听
			this.addCacheData(basePath);
			TreeCache cache = this.getCache(basePath);
	        cache.getListenable().addListener(new ListenServersChangedListener(this));
	        
	        // 注册请求地址
	        Map<RequestMappingInfo, HandlerMethod> map = requestMappingHandlerMapping.getHandlerMethods();
	        if (null != map && !map.isEmpty()) {
	        	for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : map.entrySet()) {
					RequestMappingInfo requestMappingInfo = entry.getKey();
					HandlerMethod handlerMethod = entry.getValue();
					String method = basePath + handlerMethod.getMethod().toString();

					String configPath = method + "/config";
					String url = requestMappingInfo.getPatternsCondition().getPatterns().iterator().next();
					this.persist(configPath + "/url", url);
					Set<RequestMethod> rm = requestMappingInfo.getMethodsCondition().getMethods();
					String type = "GET";
					if (!rm.isEmpty()) {
						type = rm.iterator().next().name();
					}
					this.persist(configPath + "/type", type);

					String serverPath = method + "/servers/" + lhs.getIp();
					this.persist(serverPath + "/hostName", lhs.getHostName());
					this.ephemeral(serverPath + "/status", "READY");
	        	}
	        }
	        
	        // 开启性能监控任务
	        try {
	        	Timer timer = new Timer();
	    		timer.schedule(new PerformanceTask(this), 10000, 10000);
			} catch (Exception e) {
			}
		}
	}
	
	
    /**
     * 等待500ms, cache先关闭再关闭client, 否则会抛异常
     * 因为异步处理, 可能会导致client先关闭而cache还未关闭结束.
     */
    private void waitForCacheClose() {
        try {
            Thread.sleep(500L);
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
    
    @Override
    public String get(final String key) {
        try {
            return new String(client.getData().forPath(key), Charsets.UTF_8);
        } catch (final Exception ex) {
            ZookeeperExceptionHandler.handleException(ex);
            return null;
        }
    }
    
    @Override
    public List<String> getChildrenKeys(final String key) {
        try {
            List<String> result = client.getChildren().forPath(key);
            Collections.sort(result, new Comparator<String>() {
                
                @Override
                public int compare(final String o1, final String o2) {
                    return o2.compareTo(o1);
                }
            });
            return result;
        } catch (final Exception ex) {
            ZookeeperExceptionHandler.handleException(ex);
            return Collections.emptyList();
        }
    }
    
    @Override
    public boolean isExisted(final String key) {
        try {
            return null != client.checkExists().forPath(key);
        } catch (final Exception ex) {
            ZookeeperExceptionHandler.handleException(ex);
            return false;
        }
    }
    
    @Override
    public void persist(final String key, final String value) {
        try {
            if (!isExisted(key)) {
                client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(key, value.getBytes(Charsets.UTF_8));
            } else {
                update(key, value);
            }
        } catch (final Exception ex) {
            ZookeeperExceptionHandler.handleException(ex);
        }
    }
    
    @Override
    public void update(final String key, final String value) {
        try {
        	if (isExisted(key)) {
        		client.setData().forPath(key, value.getBytes(Charsets.UTF_8));
        	}
        } catch (final Exception ex) {
            ZookeeperExceptionHandler.handleException(ex);
        }
    }
    
    @Override
    public void ephemeral(final String key, final String value) {
        try {
            if (isExisted(key)) {
                client.delete().deletingChildrenIfNeeded().forPath(key);
            }
            client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(key, value.getBytes(Charsets.UTF_8));
        } catch (final Exception ex) {
            ZookeeperExceptionHandler.handleException(ex);
        }
    }
    
    @Override
    public String persistSequential(final String key, final String value) {
        try {
            return client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT_SEQUENTIAL).forPath(key, value.getBytes(Charsets.UTF_8));
        } catch (final Exception ex) {
            ZookeeperExceptionHandler.handleException(ex);
        }
        return null;
    }
    
    @Override
    public void ephemeralSequential(final String key) {
        try {
            client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(key);
        } catch (final Exception ex) {
            ZookeeperExceptionHandler.handleException(ex);
        }
    }
    
    @Override
    public void remove(final String key) {
        try {
            client.delete().deletingChildrenIfNeeded().forPath(key);
        } catch (final Exception ex) {
            ZookeeperExceptionHandler.handleException(ex);
        }
    }
    
    @Override
    public void addCacheData(final String cachePath) {
        TreeCache cache = new TreeCache(client, cachePath);
        try {
            cache.start();
        } catch (final Exception ex) {
            ZookeeperExceptionHandler.handleException(ex);
        }
        caches.put(cachePath + "/", cache);
    }
    
    @Override
    public TreeCache getCache(final String cachePath) {
        return caches.get(cachePath + "/");
    }
    
    public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
		return requestMappingHandlerMapping;
	}

	public void setRequestMappingHandlerMapping(RequestMappingHandlerMapping requestMappingHandlerMapping) {
		this.requestMappingHandlerMapping = requestMappingHandlerMapping;
	}

}
