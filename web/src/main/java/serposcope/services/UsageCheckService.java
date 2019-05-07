/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package serposcope.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.time.DayOfWeek;
import java.util.ArrayList;
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
import com.serphacker.serposcope.models.google.GoogleTarget;
import com.serphacker.serposcope.scraper.google.GoogleDevice;
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
				+ "tasks: {} remaining: {} "
				+ "active: {} waiting: {}",
				activeTasks, remainingSearches,
				activeThreads, waitingThreads);
	}

	@Schedule(delay = 1, initialDelay = 0, timeUnit = TimeUnit.HOURS)
	public void checkGroupStats() {

		String[] types = { "Total", "Sunday Scheduled", "Monday Scheduled", "Tuesday Scheduled", "Wednesday Scheduled",
				"Thursday Scheduled", "Friday Scheduled", "Saturday Scheduled", "Non-Scheduled" };		
		
		List<Group> allGroups = baseDB.group.list();		
		List<GoogleSearch> allSearches = googleDB.search.list();
		List<GoogleTarget> allTargets = googleDB.target.list();

		for (String type : types) {
			long groupCount = 0;
			DayOfWeek dayOfWeek = null;
			switch (type) {
			case "Total":
				groupCount = allGroups.size();
				break;
			case "Sunday Scheduled":
				dayOfWeek = DayOfWeek.SUNDAY;
				groupCount = allGroups.stream().filter(Group::isSundayEnabled).count();
				break;
			case "Monday Scheduled":
				dayOfWeek = DayOfWeek.MONDAY;
				groupCount = allGroups.stream().filter(Group::isMondayEnabled).count();
				break;
			case "Tuesday Scheduled":
				dayOfWeek = DayOfWeek.TUESDAY;
				groupCount = allGroups.stream().filter(Group::isTuesdayEnabled).count();
				break;
			case "Wednesday Scheduled":
				dayOfWeek = DayOfWeek.WEDNESDAY;
				groupCount = allGroups.stream().filter(Group::isWednesdayEnabled).count();
				break;
			case "Thursday Scheduled":
				dayOfWeek = DayOfWeek.THURSDAY;
				groupCount = allGroups.stream().filter(Group::isThursdayEnabled).count();
				break;
			case "Friday Scheduled":
				dayOfWeek = DayOfWeek.FRIDAY;
				groupCount = allGroups.stream().filter(Group::isFridayEnabled).count();
				break;
			case "Saturday Scheduled":
				dayOfWeek = DayOfWeek.SATURDAY;
				groupCount = allGroups.stream().filter(Group::isSaturdayEnabled).count();
				break;
			case "Non-Scheduled":
				groupCount = allGroups.stream()
						.filter(g -> !g.isSundayEnabled() && !g.isMondayEnabled() && !g.isTuesdayEnabled()
								&& !g.isWednesdayEnabled() && !g.isThursdayEnabled() && !g.isFridayEnabled()
								&& !g.isSaturdayEnabled())
						.count();
			}

			List<GoogleSearch> searches;
			if (dayOfWeek == null) {
				searches = new ArrayList<>(allSearches);
			} else {
				searches = googleDB.search.listByGroup(null, dayOfWeek);
				allSearches.removeAll(searches);

			}
			int desktopSearch = 0;
			int mobileSearch = 0;
			for (GoogleSearch search : searches) {
				if (search.getDevice() == GoogleDevice.DESKTOP) {
					desktopSearch++;
				} else {
					mobileSearch++;
				}
			}

			List<GoogleTarget> targets;
			if (dayOfWeek == null) {
				targets = new ArrayList<>(allTargets);
			} else {
				targets = googleDB.target.list(null, dayOfWeek);
				allTargets.removeAll(targets);
			}
			int targetCount = targets.size();

			LOG.info("[Group Stats: {}] groups: {} desktop: {} mobile: {} targets: {}", type, groupCount, desktopSearch,
					mobileSearch, targetCount);
		}
	}

	@Schedule(delay = 1, initialDelay = 1, timeUnit = TimeUnit.MINUTES)
	public void checkParserStats() {
		GoogleAbstractScrapParser.printStats();
	}

}
