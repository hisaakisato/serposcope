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
import java.time.DayOfWeek;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serphacker.serposcope.db.base.BaseDB;
import com.serphacker.serposcope.db.google.GoogleDB;
import com.serphacker.serposcope.models.base.Group;
import com.serphacker.serposcope.models.google.GoogleSearch;
import com.serphacker.serposcope.models.google.GoogleTarget;
import com.serphacker.serposcope.task.TaskManager;

import ninja.scheduler.Schedule;

@Singleton
public class GroupStatsService {

	private static final Logger LOG = LoggerFactory.getLogger(GroupStatsService.class);

	@Inject
	BaseDB baseDB;

	@Inject
	GoogleDB googleDB;

	@Inject
	TaskManager taskManager;

	MemoryMXBean mxbean;

	public GroupStatsService() {
		mxbean = ManagementFactory.getMemoryMXBean();
	}

	@Schedule(delay = 1, initialDelay = 0, timeUnit = TimeUnit.HOURS)
	public void checkGroupStats() {

		String[] types = { "Total", "Monday Scheduled", "Tuesday Scheduled", "Wednesday Scheduled",
				"Thursday Scheduled", "Friday Scheduled", "Saturday Scheduled", "Sunday Scheduled", "Non-Scheduled" };		
		int[] groupCounts = new int[types.length];
		for (Group group : baseDB.group.list()) {
			groupCounts[0] ++;
			boolean scheduled = false;
			if (group.isMondayEnabled()) {
				groupCounts[1]++;
				scheduled = true;
			}
			if (group.isTuesdayEnabled()) {
				groupCounts[2]++;
				scheduled = true;
			}
			if (group.isWednesdayEnabled()) {
				groupCounts[3]++;
				scheduled = true;
			}
			if (group.isThursdayEnabled()) {
				groupCounts[4]++;
				scheduled = true;
			}
			if (group.isFridayEnabled()) {
				groupCounts[5]++;
				scheduled = true;
			}
			if (group.isSaturdayEnabled()) {
				groupCounts[6]++;
				scheduled = true;
			}
			if (group.isSundayEnabled()) {
				groupCounts[7]++;
				scheduled = true;
			}
			if (!scheduled) {
				groupCounts[8]++;
			}
		}

		Set<Integer> allSearches = googleDB.search.list().stream().map(GoogleSearch::getId)
				.collect(Collectors.toCollection(HashSet::new));
		Set<Integer> allTargets = googleDB.target.list().stream().map(GoogleTarget::getId)
				.collect(Collectors.toCollection(HashSet::new));

		for (int i = 0; i < types.length; i++) {
			String type = types[i];
			long groupCount = groupCounts[i];
			
			Set<Integer> searches;
			Set<Integer> targets;

			if (i < 1 || i > 7) {
				searches = allSearches;
				targets = allTargets;
			} else {
				DayOfWeek dayOfWeek = DayOfWeek.of(i);
				searches = googleDB.search.listByGroup(null, dayOfWeek).stream().map(GoogleSearch::getId)
						.collect(Collectors.toCollection(HashSet::new));
				for (Iterator<Integer> itr = searches.iterator(); itr.hasNext();) {
					allSearches.remove(itr.next());
				}
				targets = googleDB.target.list(null, dayOfWeek).stream().map(GoogleTarget::getId)
						.collect(Collectors.toCollection(HashSet::new));
				for (Iterator<Integer> itr = targets.iterator(); itr.hasNext();) {
					allTargets.remove(itr.next());
				}
			}

			LOG.info("[Group Stats: {}] groups: {} searches: {} targets: {}", type, groupCount,
					searches.size(), targets.size());
		}
	}

}
