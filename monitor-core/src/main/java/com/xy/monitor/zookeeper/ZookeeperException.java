package com.xy.monitor.zookeeper;

/**
 * 注册中心异常.
 * 
 */
public final class ZookeeperException extends RuntimeException {
    
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ZookeeperException(final String errorMessage, final Object... args) {
        super(String.format(errorMessage, args));
    }
    
    public ZookeeperException(final Exception cause) {
        super(cause);
    }
}
