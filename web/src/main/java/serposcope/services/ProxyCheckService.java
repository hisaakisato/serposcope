/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package serposcope.services;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serphacker.serposcope.db.base.BaseDB;
import com.serphacker.serposcope.models.base.Proxy;
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

	@Schedule(delay = 2, initialDelay = 0, timeUnit = TimeUnit.MINUTES)
	public void check() {
		synchronized (taskManager.rotator) {			
			List<ScrapProxy> proxies = baseDB.proxy.list().stream().map(Proxy::toScrapProxy).collect(Collectors.toList());
			LOG.debug("refresh rotator proxies");
			taskManager.rotator.replace(proxies);
		}
	}

}
