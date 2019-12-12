/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package serposcope.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serphacker.serposcope.db.base.BaseDB;
import com.serphacker.serposcope.db.google.GoogleDB;
import com.serphacker.serposcope.scraper.google.scraper.parser.GoogleAbstractScrapParser;
import com.serphacker.serposcope.task.TaskManager;
import com.serphacker.serposcope.task.google.GoogleTask;

import ninja.scheduler.Schedule;

@Singleton
public class UsageCheckService {

	private static final Logger LOG = LoggerFactory.getLogger(UsageCheckService.class);

	@Inject
	BaseDB baseDB;

	@Inject
	GoogleDB googleDB;

	@Inject
	TaskManager taskManager;

	MemoryMXBean mxbean;


	private String pid = null;
	private Integer maxOpenFiles;

	public UsageCheckService() {
		mxbean = ManagementFactory.getMemoryMXBean();

		String vmName = ManagementFactory.getRuntimeMXBean().getName();
		int p = vmName.indexOf("@");
		String pid = vmName.substring(0, p);
		Process process = null;
		try {
			process = Runtime.getRuntime()
					.exec(new String[] { "cat", String.format("/proc/%s/limits", pid) });
			if (process.waitFor() == 0) {
				try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
					String line;
					while ((line = br.readLine()) != null) {
						if (line.contains("open")) {
							maxOpenFiles = Integer.parseInt(line.split("\\s+")[4]);
							this.pid = pid;
							break;
						}
					}
				}
			}
		} catch (Exception e) {
		} finally {
			if (process != null) {
				process.destroy();
			}
		}
	}

	@Schedule(delay = 1, initialDelay = 0, timeUnit = TimeUnit.MINUTES)
	public void checkMemoryUsage() {
		// memory stats
		long heapUsed = mxbean.getHeapMemoryUsage().getUsed();
		long heapComitted = mxbean.getHeapMemoryUsage().getCommitted();
		LOG.info("[Heap Usage] "
				+ "used: {} committed: {} usage: {}",
				heapUsed, heapComitted,
				String.format("%.2f", heapUsed * 100.0 / heapComitted));

		long nonHeapUsed = mxbean.getNonHeapMemoryUsage().getUsed();
		long nonHeapComitted = mxbean.getNonHeapMemoryUsage().getCommitted();
		LOG.info("[Non-Heap Usage] "
				+ "used: {} committed: {} usage: {}",
				nonHeapUsed, nonHeapComitted,
				String.format("%.2f", nonHeapUsed * 100.0 / nonHeapComitted));
	}

	@Schedule(delay = 1, initialDelay = 0, timeUnit = TimeUnit.MINUTES)
	public void checkFileDescriptorUsage() {
        
		if (this.pid == null) {
			return;
		}

		Process process = null;
		try {
			process = Runtime.getRuntime()
					.exec(new String[] { "/bin/sh", "-c", String.format("ls /proc/%s/fd | wc -l", this.pid) });
			if (process.waitFor() == 0) {
				try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
		        	int used = Integer.parseInt(br.readLine());
		    		LOG.info("[FileDescriptor Usage] "
		    				+ "used: {} max: {} usage: {}",
		    				used, this.maxOpenFiles,
		    				String.format("%.2f", used * 100.0 / this.maxOpenFiles));
				}
			}
		} catch (Exception e) {
		} finally {
			if (process != null) {
				process.destroy();
			}
		}

	}

	@Schedule(delay = 10, initialDelay = 60, timeUnit = TimeUnit.SECONDS)
	public void checkRunningTaskStats() {
		// tasks
		int activeTasks = 0;
		int remainingSearches = 0;
		for (GoogleTask task :  taskManager.listRunningGoogleTasks(null)) {
			if (task.getRun().isRunning()) {
				activeTasks++;
				remainingSearches += task.getRemainigSearches();
			}
		}
		// thread stats
		int activeThreads = 0;
		int waitingCount = 0;
		for (GoogleTask task : taskManager.listRunningGoogleTasks(null)) {
			if (task.isAlive()) {
				activeThreads += task.getActiveCount();
				waitingCount += task.getWaitingCount();
			}
		}
		LOG.info("[Running Task Stats] "
				+ "tasks: {} remaining: {} "
				+ "active: {} waiting: {}",
				activeTasks, remainingSearches,
				activeThreads, waitingCount);
	}

	@Schedule(delay = 1, initialDelay = 1, timeUnit = TimeUnit.MINUTES)
	public void checkParserStats() {
		GoogleAbstractScrapParser.printStats();
	}

}
