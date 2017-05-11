package com.xy.monitor.task;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;

import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.Sigar;

import com.alibaba.fastjson.JSONObject;
import com.xy.monitor.sigar.SigarUtils;
import com.xy.monitor.util.LocalHostService;
import com.xy.monitor.zookeeper.ZookeeperRegistryCenter;

/**
 * 性能监控任务
 * 
 * @author xiongyan
 * @date 2017年4月18日 下午2:56:00
 */
public final class PerformanceTask extends TimerTask {

	/**
	 * sigar
	 */
	private static final Sigar sigar = SigarUtils.sigar;
	
	/**
	 * 获取真实本机网络的服务
	 */
	private static final LocalHostService lhs = new LocalHostService();

	/**
	 * 注册中心
	 */
	private ZookeeperRegistryCenter zookeeperRegistryCenter;

	/**
	 * 构造方法
	 * 
	 * @param zookeeperRegistryCenter
	 */
	public PerformanceTask(ZookeeperRegistryCenter zookeeperRegistryCenter) {
		this.zookeeperRegistryCenter = zookeeperRegistryCenter;
	}

	@Override
	public void run() {
		try {
			Map<String, Object> map = new HashMap<>();
			int cpuNum = sigar.getCpuInfoList().length;
			map.put("cpuNum", cpuNum + "核");
			String cpuPerc = String.format("%.2f", sigar.getCpuPerc().getCombined());
			map.put("cpuPerc", cpuPerc + "%");

			Runtime runtime = Runtime.getRuntime();
			map.put("jvmFreeMem", runtime.freeMemory() / 1024 / 1024 + "MB");
			map.put("jvmTotalMem", runtime.totalMemory() / 1024 / 1024 + "MB");
			map.put("jvmMaxMem", runtime.maxMemory() / 1024 / 1024 + "MB");
			map.put("osName", System.getProperty("os.name"));
			
			map.put("threadNum", Thread.activeCount());

			Mem mem = sigar.getMem();
			map.put("totalMem", mem.getTotal() / 1024 / 1024 / 1024 + "GB");
			map.put("usedMem", mem.getUsed() / 1024 / 1024 / 1024 + "GB");
			map.put("freeMem", mem.getFree() / 1024 / 1024 / 1024 + "GB");

			FileSystem fslist[] = sigar.getFileSystemList();
			long totalFile = 0;
			long freeFile = 0;
			for (FileSystem fs : fslist) {
				FileSystemUsage usage = null;
				try {
					usage = sigar.getFileSystemUsage(fs.getDirName());
					switch (fs.getType()) {
						case FileSystem.TYPE_LOCAL_DISK :
							totalFile += usage.getTotal();
							freeFile += usage.getFree();
							break;
					}
				} catch (Exception e) {
				}
			}
			map.put("totalFile", totalFile / 1024 / 1024 + "GB");
			map.put("freeFile", freeFile / 1024 / 1024 + "GB");
			map.put("time", new Date());
			
			String serverPath = "/servers/" + lhs.getIp();
			zookeeperRegistryCenter.ephemeral(serverPath+"/performance", JSONObject.toJSONString(map));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
