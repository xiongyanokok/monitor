package com.xy.monitor.zookeeper;

import org.apache.zookeeper.KeeperException.ConnectionLossException;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 注册中心异常处理类.
 * 
 */
public final class ZookeeperExceptionHandler {
    
	private static final Logger logger = LoggerFactory.getLogger(ZookeeperExceptionHandler.class);
	
    /**
     * 处理异常.
     * 
     * <p>处理掉中断和连接失效异常并继续抛注册中心.</p>
     * 
     * @param cause 待处理异常.
     */
    public static void handleException(final Exception cause) {
        if (isIgnoredException(cause) || null != cause.getCause() && isIgnoredException(cause.getCause())) {
        	logger.debug("Elastic job: ignored exception for: {}", cause.getMessage());
        } else if (cause instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        } else {
            throw new ZookeeperException(cause);
        }
    }
    
    private static boolean isIgnoredException(final Throwable cause) {
        return null != cause && (cause instanceof ConnectionLossException || cause instanceof NoNodeException || cause instanceof NodeExistsException);
    }
}
