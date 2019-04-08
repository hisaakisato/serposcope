/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package serposcope.services;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serphacker.serposcope.db.base.BaseDB;
import com.serphacker.serposcope.models.base.Proxy;
import com.serphacker.serposcope.models.base.Proxy.Status;
import com.serphacker.serposcope.scraper.http.proxy.ScrapProxy;
import com.serphacker.serposcope.task.TaskManager;

import ninja.scheduler.Schedule;

@Singleton
public class ProxyCheckService {

	private static final Logger LOG = LoggerFactory.getLogger(ProxyCheckService.class);

	@Inject
	BaseDB baseDB;

	@Inject
	TaskManager taskManager;

	@Schedule(delay = 10, initialDelay = 0, timeUnit = TimeUnit.SECONDS)
	public void check() {
		synchronized (taskManager.rotator) {
			LOG.debug("refresh rotator proxies");
			int unchecked = 0;
			int error = 0;
			int removed = 0;
			List<ScrapProxy> proxies = new ArrayList<>();
			for (Proxy proxy : baseDB.proxy.list()) {
				switch (proxy.getStatus()) {
				case REMOVED:
					removed++;
					break;
				case UNCHECKED:
					unchecked++;
					break;
				case ERROR:
					error++;
					break;
				default:
				}
				proxies.add(proxy.toScrapProxy());
			}
			taskManager.rotator.replace(proxies);
			int idle = taskManager.rotator.list().size();
			int inUse = taskManager.rotator.listUsed().size();
			LOG.info("[Proxy Usage] "
					+ "idle: {} in-use: {} "
					+ "unchecked: {} error: {} removed: {} "
					+ "total: {}",
					idle, inUse,
					unchecked, error, removed,
					idle + inUse + removed);
		}
	}

}
