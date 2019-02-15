/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package serposcope.services;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serphacker.serposcope.db.base.BaseDB;
import com.serphacker.serposcope.db.google.GoogleDB;
import com.serphacker.serposcope.models.base.Group;
import com.serphacker.serposcope.models.google.GoogleSearch;
import com.serphacker.serposcope.scraper.google.GoogleDevice;
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

	public UsageCheckService() {
		mxbean = ManagementFactory.getMemoryMXBean();
	}

	@Schedule(delay = 1, initialDelay = 0, timeUnit = TimeUnit.MINUTES)
	public void checkMemoryUsage() {
		// memory stats
		long heapUsed = mxbean.getHeapMemoryUsage().getUsed();
		long heapComitted = mxbean.getHeapMemoryUsage().getCommitted();
		LOG.info("[Heap Usage] "
				+ "used(bytes): {} ,committed(bytes): {} ,usage(%): {}",
				heapUsed, heapComitted,
				String.format("%.2f", heapUsed * 100.0 / heapComitted));

		long nonHeapUsed = mxbean.getNonHeapMemoryUsage().getUsed();
		long nonHeapComitted = mxbean.getNonHeapMemoryUsage().getCommitted();
		LOG.info("[Non-Heap Usage] "
				+ "used(bytes): {} ,committed(bytes): {} ,usage(%): {}",
				nonHeapUsed, nonHeapComitted,
				String.format("%.2f", nonHeapUsed * 100.0 / nonHeapComitted));
	}

	@Schedule(delay = 1, initialDelay = 1, timeUnit = TimeUnit.MINUTES)
	public void checkRunningTaskStats() {
		// tasks
		int activeTasks = 0;
		int remainingSearches = 0;
		for (GoogleTask task :  taskManager.listRunningGoogleTasks()) {
			if (task.getRun().isRunning()) {
				activeTasks++;
				remainingSearches += task.getRemainigSearches();
			}
		}
		// thread stats
		int activeThreads = 0;
		int waitingThreads = 0;
		for (GoogleTask task : taskManager.listRunningGoogleTasks()) {
			if (task.isAlive()) {
				activeThreads += task.getActiveCount();
				waitingThreads += task.getWaitingCount();
			}
		}
		LOG.info("[Running Task Stats] "
				+ "tasks: {} ,remaining: {} ,"
				+ "active: {} ,waiting: {}",
				activeTasks, remainingSearches,
				activeThreads, waitingThreads);
	}

	@Schedule(delay = 1, initialDelay = 0, timeUnit = TimeUnit.HOURS)
	public void checkGroupStats() {
		int cronDisabled = 0;
		int cronEnabled = 0;
		List<Group> groups = baseDB.group.list();		
		for (Group group : groups) {
			if (group.isCronDisabled()) {
				cronDisabled++;
			} else {
				cronEnabled++;
			}
		}
		int desktopSearch = 0;
		int mobileSearch = 0;
		List<GoogleSearch> searches = googleDB.search.list();
		for (GoogleSearch search : searches) {
			if (search.getDevice() == GoogleDevice.DESKTOP) {
				desktopSearch++;
			} else {
				mobileSearch++;
			}
		}
		int targets = googleDB.target.list().size();
		LOG.info("[Group Stats] "
				+ "groups: {} ,scheduled: {} ,non-scheduled: {} ,"
				+ "keywords: {} ,desktop: {} ,mobile: {} ,"
				+ "targets: {}",
				cronEnabled + cronDisabled, cronEnabled, cronDisabled,
				desktopSearch + mobileSearch, desktopSearch, mobileSearch,
				targets);
	}
}
