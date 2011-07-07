package org.apache.karaf.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.util.Properties;

public class InstanceInfoManager {

	static void writePid(String pidFile) {
		try {
			if (pidFile != null) {
				String pid = getMyPid();
				Writer w = new OutputStreamWriter(new FileOutputStream(pidFile));
				w.write(pid);
				w.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
    
	static void updateInstanceInfo(File karafHome, File karafBase) {
		try {
			String instanceName = System.getProperty("karaf.name");
			String pid = getMyPid();

			boolean isRoot = karafHome.equals(karafBase);

			if (instanceName != null) {
				String storage = System.getProperty("karaf.instances");
				if (storage == null) {
					throw new Exception(
							"System property 'karaf.instances' is not set. \n"
									+ "This property needs to be set to the full path of the instance.properties file.");
				}
				File storageFile = new File(storage);
				File propertiesFile = new File(storageFile,
						"instance.properties");
				Properties props = new Properties();
				if (propertiesFile.exists()) {
					FileInputStream fis = new FileInputStream(propertiesFile);
					props.load(fis);
					int count = Integer.parseInt(props.getProperty("count"));
					for (int i = 0; i < count; i++) {
						String name = props.getProperty("item." + i + ".name");
						if (name.equals(instanceName)) {
							props.setProperty("item." + i + ".pid", pid);
							writeProperties(propertiesFile, props);
							fis.close();
							return;
						}
					}
					fis.close();
					if (!isRoot) {
						throw new Exception("Instance " + instanceName
								+ " not found");
					}
				} else if (isRoot) {
					propertiesFile.getParentFile().mkdirs();
					props.setProperty("count", "1");
					props.setProperty("item.0.name", instanceName);
					props.setProperty("item.0.loc", karafHome.getAbsolutePath());
					props.setProperty("item.0.pid", pid);
					props.setProperty("item.0.root", "true");
					writeProperties(propertiesFile, props);
				}
			}
		} catch (Exception e) {
			System.err.println("Unable to update instance pid: "
					+ e.getMessage());
		}
	}

	private static String getMyPid() {
		String pid = ManagementFactory.getRuntimeMXBean().getName();
		if (pid.indexOf('@') > 0) {
			pid = pid.substring(0, pid.indexOf('@'));
		}
		return pid;
	}

	private static void writeProperties(File propertiesFile, Properties props)
			throws FileNotFoundException, IOException {
		FileOutputStream fos = new FileOutputStream(
				propertiesFile);
		props.store(fos, null);
		fos.close();
	}
}
