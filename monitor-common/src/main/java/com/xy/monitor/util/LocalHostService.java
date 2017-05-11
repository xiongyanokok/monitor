package com.xy.monitor.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 获取真实本机网络的服务.
 * 
 * @author zhangliang
 */
public class LocalHostService {
	
	private static final Logger logger = LoggerFactory.getLogger(LocalHostService.class);
    
    private static volatile String cachedIpAddress;
    
    public static final String LOCALHOST = "127.0.0.1";
    
    public static final String ANYHOST = "0.0.0.0";
    
    private static final Pattern IP_PATTERN = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3,5}$");
    
    /**
     * 获取本机IP地址.
     * 
     * <p>
     * 有限获取外网IP地址.
     * 也有可能是链接着路由器的最终IP地址.
     * </p>
     * 
     * @return 本机IP地址
     */
    public String getIp() {
    	if (null != cachedIpAddress) {
            return cachedIpAddress;
        }
    	
    	InetAddress address = getLocalAddress0();
    	String localIpAddress = address == null ? LOCALHOST : address.getHostAddress();
        cachedIpAddress = localIpAddress;
        return localIpAddress;
    }
    
    /**
     * 获取本机Host名称.
     * 
     * @return 本机Host名称
     */
    public String getHostName() {
    	InetAddress address = getLocalAddress0();
        return address == null ? LOCALHOST : address.getHostName();
    }
    
    /**
     * 获取本机地址
     * @return
     */
	private InetAddress getLocalAddress0() {
		InetAddress localAddress = null;
		try {
			localAddress = InetAddress.getLocalHost();
			if (isValidAddress(localAddress)) {
				return localAddress;
			}
		} catch (Throwable e) {
			logger.warn("Failed to retriving ip address, " + e.getMessage(), e);
		}
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			if (interfaces != null) {
				while (interfaces.hasMoreElements()) {
					try {
						NetworkInterface network = interfaces.nextElement();
						Enumeration<InetAddress> addresses = network.getInetAddresses();
						if (addresses != null) {
							while (addresses.hasMoreElements()) {
								try {
									InetAddress address = addresses.nextElement();
									if (isValidAddress(address)) {
										return address;
									}
								} catch (Throwable e) {
									logger.warn("Failed to retriving ip address, " + e.getMessage(), e);
								}
							}
						}
					} catch (Throwable e) {
						logger.warn("Failed to retriving ip address, " + e.getMessage(), e);
					}
				}
			}
		} catch (Throwable e) {
			logger.warn("Failed to retriving ip address, " + e.getMessage(), e);
		}
		logger.error("Could not get local host ip address, will use 127.0.0.1 instead.");
		return localAddress;
	}
	
	/**
	 * 验证是否有效地址
	 * 
	 * @param address
	 * @return
	 */
	private boolean isValidAddress(InetAddress address) {
		if (address == null || address.isLoopbackAddress()) {
			return false;
		}
		String name = address.getHostAddress();
		return (name != null && !ANYHOST.equals(name) && !LOCALHOST.equals(name) && IP_PATTERN.matcher(name).matches());
	}
	
	public static void main(String[] args) {
		LocalHostService lhs = new LocalHostService();
		System.out.println(lhs.getIp()+"  "+lhs.getHostName());
	}
    
}
