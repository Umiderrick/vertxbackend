package io.vertx.blog.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Properties;

public class AppConfig {
	final static String SYS_PROP_PID = "pid";

	final static Properties config = new Properties();

	public static void load(String res) {
		InputStream in = null;
		try {
			in = IOUtils.getResourceAsStream(res);
			if (in != null) {
				config.load(in);
			}
		} catch (Exception e) {
			System.err.println("WRAN Resource:" + res + " not exist.");
		} finally {
			IOUtils.close(in);
		}
	}

	private static void load(File file) {
		InputStream in = null;
		try {
			if (file.exists()) {
				in = new FileInputStream(file);
				config.load(in);
			}
		} catch (Exception e) {
			System.err.println("WRAN: File :" + file.getName() + " not exist.");
		} finally {
			IOUtils.close(in);
		}
	}

	/**
	 * 取得配置项
	 * 
	 * @param key
	 * @return
	 */
	public static String getString(String key) {
		return (config.containsKey(key)) ? config.getProperty(key) : "";
	}

	public static String getPID() {
		String pid = System.getProperty(SYS_PROP_PID);
		if (pid == null) {
			RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
			String processName = runtimeMXBean.getName();
			if (processName.indexOf('@') != -1) {
				pid = processName.substring(0, processName.indexOf('@'));
				System.setProperty(SYS_PROP_PID, pid);
			}
		}
		return pid;
	}
	
	public static boolean isOSX() {
		String osName = System.getProperty("os.name");
		return osName == null ? false : osName.toUpperCase().endsWith("X");
	}
}
