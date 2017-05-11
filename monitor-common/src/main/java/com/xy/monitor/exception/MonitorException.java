package com.hexun.monitor.exception;

/**
 * 监控异常
 * 
 * @author zhouxiong
 * @date 2017年3月7日 下午4:35:32
 */
public class MonitorException extends RuntimeException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public MonitorException() {
        super();
    }

    public MonitorException(String message) {
        super(message);
    }

    public MonitorException(String message, Throwable cause) {
        super(message, cause);
    }

    public MonitorException(Throwable cause) {
        super(cause);
    }
    
}
