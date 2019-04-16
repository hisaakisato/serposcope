/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package com.serphacker.serposcope.task.google;

import com.serphacker.serposcope.models.google.GoogleSettings;
import com.serphacker.serposcope.models.google.GoogleSearch;
import com.serphacker.serposcope.scraper.google.GoogleScrapSearch;
import com.serphacker.serposcope.scraper.google.GoogleDevice;
import com.serphacker.serposcope.scraper.google.GoogleScrapResult;
import static com.serphacker.serposcope.scraper.google.GoogleScrapResult.Status.OK;
import static com.serphacker.serposcope.scraper.google.GoogleScrapResult.Status.ERROR_PROXY_GONE;
import com.serphacker.serposcope.scraper.google.scraper.GoogleScraper;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.serphacker.serposcope.scraper.http.proxy.ScrapProxy;
import com.serphacker.serposcope.task.google.GoogleTask;
import java.util.List;
import org.apache.http.cookie.Cookie;

public class GoogleTaskRunnable implements Runnable {

	protected static final Logger LOG = LoggerFactory.getLogger(GoogleTaskRunnable.class);
    public final static int MAX_REQUEUE_TRY = 3;

	GoogleTask controller;

	GoogleScraper scraper;

	public GoogleTaskRunnable(GoogleTask controller) {
		this.controller = controller;
		scraper = controller.genScraper();
	}

	boolean cookiesStickToProxy = true;

	@Override
	@SuppressWarnings("unchecked")
	public void run() {
		GoogleSearch search = null;
		ScrapProxy proxy = null;
		int searchTry = 0;

		LOG.debug("google thread started");
		try {

			MAIN_LOOP: while (search != null || !controller.shouldStop()) {

				if (Thread.interrupted()) {
					LOG.warn("[Search Abort] interrupted, aborting the thread");
					break;
				}

				while (true) {
					try {
						if (proxy != null) {							
							controller.rotator.add(proxy);
							proxy = null;
						}
						++controller.waitingCount;
						proxy = controller.rotator.poll();
					} finally {
						--controller.waitingCount;
					}
					if (proxy != null) {
						break;
					}
					LOG.debug("no proxy available, wait a moment");
					if (search == null && controller.shouldStop()) {
						break MAIN_LOOP;
					}
				}
				if (proxy == null) {
//                  LOG.trace("no search to do, waiting for termination");
					continue;
				}
				scraper.getHttp().setProxy(proxy);

				if (search == null) {
					try {
						search = controller.searches.poll(1, TimeUnit.SECONDS);
					} catch (InterruptedException ex) {
						LOG.warn("[Search Abort] interrupted while polling, aborting the thread");
						break;
					}
					searchTry = 0;
				}

				if (search == null) {
//                    LOG.trace("no search to do, waiting for termination");
					continue;
				}

				GoogleDevice device = search.getDevice();
				int requestCount = 0;
				if (cookiesStickToProxy) {
					scraper.getHttp().clearCookies();
					String attr = device == GoogleDevice.DESKTOP ? ScrapProxy.PROXY_ATTR_DESKTOP_COOKIES
							: ScrapProxy.PROXY_ATTR_MOBILE_COOKIES;
					List<Cookie> cookies = proxy
							.getAttr(attr, List.class);
					if (cookies != null) {
						scraper.getHttp().addCookies(proxy.getAttr(attr, List.class));
					}
					attr = device == GoogleDevice.DESKTOP ? ScrapProxy.PROXY_ATTR_DESKTOP_REQUEST_COUNT
							: ScrapProxy.PROXY_ATTR_MOBILE_REQUEST_COUNT;
					if (!proxy.hasAttr(attr)) {
						proxy.setAttr(attr, 0);
					}
					requestCount = proxy.getAttr(attr, Integer.class);					
				}
				
				++searchTry;
				++requestCount;
				GoogleScrapResult res = null;
				long start = System.currentTimeMillis();
				try {
					res = scraper.scrap(getScrapConfig(controller.googleOptions, search));
				} catch (InterruptedException ex) {
					if (!controller.abort && !controller.interrupted) {
						LOG.warn("[Search Abort] interrupted while scraping, aborting the thread");
					}
					break;
				}

				// check states
				if (controller.abort || controller.interrupted) {
					break;
				}

				long duration = System.currentTimeMillis() - start;
				if (requestCount > ScrapProxy.MAX_REQUEST_COUNT) {
					// remove client info
					if (device == GoogleDevice.DESKTOP) {
						proxy.removeAttr(ScrapProxy.PROXY_ATTR_DESKTOP_USER_AGENT);
						proxy.removeAttr(ScrapProxy.PROXY_ATTR_DESKTOP_COOKIES);
						proxy.removeAttr(ScrapProxy.PROXY_ATTR_DESKTOP_REQUEST_COUNT);
					} else {
						proxy.removeAttr(ScrapProxy.PROXY_ATTR_MOBILE_USER_AGENT);
						proxy.removeAttr(ScrapProxy.PROXY_ATTR_MOBILE_COOKIES);
						proxy.removeAttr(ScrapProxy.PROXY_ATTR_MOBILE_REQUEST_COUNT);
					}
					scraper.getHttp().clearCookies();
				} else {
					if (device == GoogleDevice.DESKTOP) {
						proxy.setAttr(ScrapProxy.PROXY_ATTR_DESKTOP_REQUEST_COUNT, requestCount);
					} else {
						proxy.setAttr(ScrapProxy.PROXY_ATTR_MOBILE_REQUEST_COUNT, requestCount);						
					}
				}

				if (res.captchas > 0) {
					controller.incCaptchaCount(res.captchas);
				}

				String userAgent = proxy
						.getAttr(device == GoogleDevice.DESKTOP ? ScrapProxy.PROXY_ATTR_DESKTOP_USER_AGENT
								: ScrapProxy.PROXY_ATTR_MOBILE_USER_AGENT, String.class);

				if (res.status != OK) {// FIXME
					if (res.status == ERROR_PROXY_GONE) {
						searchTry--; // discard proxy
					} else if (searchTry > controller.googleOptions.getFetchRetry()) {
						LOG.error("[Search Failed] id: {} device: {} keyword: [{}] duration: {} retry: {} captchas: {} reason: {} proxy: [{}] user-agent: [{}] request_count: {}",
								search.getId(), device == GoogleDevice.DESKTOP ? "PC" : "SP",
								search.getKeyword(), String.format("%.2f", duration * 1.0 / 1000), searchTry - 1, res.captchas, res.status,
								proxy.toString().replaceFirst("proxy:", ""), userAgent, requestCount);
						controller.failedSearches.putIfAbsent(search, new AtomicInteger(0));
						if (controller.failedSearches.get(search).incrementAndGet() < MAX_REQUEUE_TRY) {							
							controller.searches.offer(search); // re-queue
						}
						search = null; // next
					} else {
						LOG.warn("[Search Error] id: {} device: {} keyword: [{}] duration: {} retry: {} captchas: {} reason: {} proxy: [{}] user-agent: [{}] request_count: {}",
								search.getId(), device == GoogleDevice.DESKTOP ? "PC" : "SP",
								search.getKeyword(), String.format("%.2f", duration * 1.0 / 1000), searchTry - 1, res.captchas, res.status,
								proxy.toString().replaceFirst("proxy:", ""), userAgent, requestCount);
					}
					controller.removeProxy(proxy); // mark removed
					proxy = null;
					continue;
				}

				controller.onSearchDone(search, res);

				if (res.status == OK) {
					LOG.info("[Search Done] id: {} device: {} keyword: [{}] duration: {} done: {} total: {} retry: {} captchas: {} proxy: [{}] user-agent: [{}] request_count: {}",
							search.getId(), device == GoogleDevice.DESKTOP ? "PC" : "SP",
							search.getKeyword(), String.format("%.2f", duration * 1.0 / 1000), controller.getSearchDone(),
							controller.totalSearch, searchTry - 1, res.captchas,
							proxy.toString().replaceFirst("proxy:", ""), userAgent, requestCount);
				}

				if (cookiesStickToProxy && proxy != null) {
					List<Cookie> cookies = scraper.getHttp().getCookies();
					if (cookies != null) {
						String attr = device == GoogleDevice.DESKTOP ? ScrapProxy.PROXY_ATTR_DESKTOP_COOKIES
								: ScrapProxy.PROXY_ATTR_MOBILE_COOKIES;
						proxy.setAttr(attr, cookies);
					}
				}

				search = null;
			}

		} catch (Exception ex) {
			LOG.error("[Search Error] unhandled exception, aborting the thread", ex);
			ex.printStackTrace();
		} finally {
			if (proxy != null) {
				controller.rotator.add(proxy);
			}
			if (search != null) {
				controller.searches.add(search);
			}
		}
		LOG.debug("google thread stopped");
	}

	protected GoogleScrapSearch getScrapConfig(GoogleSettings options, GoogleSearch search) {
		GoogleScrapSearch scrapSearch = new GoogleScrapSearch();

		// options.getFetchRetry(); // TODO
		scrapSearch.setSearchId(search.getId());
		scrapSearch.setPagePauseMS(options.getMinPauseBetweenPageSec() * 1000l,
				options.getMaxPauseBetweenPageSec() * 1000l);
		scrapSearch.setPages(options.getPages());
		scrapSearch.setResultPerPage(options.getResultPerPage());

		scrapSearch.setCustomParameters(search.getCustomParameters());
		scrapSearch.setDatacenter(search.getDatacenter());
		scrapSearch.setDevice(search.getDevice());
		scrapSearch.setKeyword(search.getKeyword());
		scrapSearch.setCountry(search.getCountry());
		scrapSearch.setLocal(search.getLocal());

		return scrapSearch;
	}

	public static final long serialVersionUID = 0L;

}
