package com.serphacker.serposcope.scraper.google.scraper.parser;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serphacker.serposcope.scraper.google.GoogleScrapLinkEntry;
import com.serphacker.serposcope.scraper.google.GoogleScrapResult.Status;
import static com.serphacker.serposcope.scraper.google.scraper.parser.GoogleAbstractScrapParser.StatsType.*;

public abstract class GoogleAbstractScrapParser {

	protected static Pattern PATTERN_GOOGLE_SERVICES = Pattern.compile("^https:\\/\\/[^.]+\\.google\\.[^/]+\\/");

	private static final Logger LOG = LoggerFactory.getLogger(GoogleAbstractScrapParser.class);

	protected static enum StatsType {
		LEGACY,
		RES,
		DESCTOP_MAIN_1,
		DESCTOP_MAIN_2,
		MOBILE_PING,
		MOBILE_NO_PING,
		MOBILE_FEATURED,
		MOBILE_PING_2,
		MOBILE_FEATURED_2,
		MOBILE_ONMOUSEDOWN
	};

	public abstract Status parse(Element resElement, List<GoogleScrapLinkEntry> entries);

	protected GoogleScrapLinkEntry extractLink(Element element) {
		if (element == null) {
			return null;
		}

		String href = element.attr("href");
		if (href == null) {
			return null;
		}

		GoogleScrapLinkEntry entry = new GoogleScrapLinkEntry(href);

		if ((href.startsWith("http://www.google") || href.startsWith("https://www.google"))) {
			if (href.contains("/aclk?")) {
				return null;
			}
		}

		if (href.startsWith("http://") || href.startsWith("https://")) {
			entry.setTitle(getTitle(element));
			return entry;
		}

		if (href.startsWith("/url?")) {
			try {
				List<NameValuePair> parse = URLEncodedUtils.parse(href.substring(5), Charset.forName("utf-8"));
				Map<String, String> map = parse.stream()
						.collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
				entry.setUrl(map.get("q"));
				entry.setTitle(getTitle(element));
				return entry;
			} catch (Exception ex) {
				return null;
			}
		}

		return null;
	}

	protected boolean isInnerCard(Element element) {
		for (Element parent : element.parents()) {
			String tagName = parent.tagName();
			if (tagName.equalsIgnoreCase("g-inner-card") || tagName.equalsIgnoreCase("g-scrolling-carousel")) {
				return true;
			}
		}
		return false;
	}
	
	protected boolean isAdLink(Element element) {
		for (Element parent : element.parents()) {
			if (parent.hasClass("ads-fr")) {
				return true;
			}
			if (parent.id().equals("tads")) {
				return true;
			}
			String classes = parent.attr("class");
			if (classes != null && !classes.isEmpty()) {
				if (Arrays.stream(classes.split(" ")).map(String::trim)
						.anyMatch(cls -> cls.startsWith("ads-"))) {
					return true;
				};
			}
		}
		return false;
	}

	protected boolean underAreaLabel(Element element) {
		for (Element parent : element.parents()) {
			if (parent.hasAttr("aria-label")) {
				return true;
			}
		}
		return false;
	}

	protected boolean isFeaturedSnippets(Element element) {
		for (Element parent : element.parents()) {
			if (parent.hasClass("kno-result") || parent.hasClass("g-blk")) {
				return true;
			}
		}
		return false;
	}

	public void setFeaturedRank(Element element, List<GoogleScrapLinkEntry> entries) {
		GoogleScrapLinkEntry entry = entries.get(entries.size() - 1);
		if (isFeaturedSnippets(element)) {
			entry.setFeaturedRank(entries.size());
		} else {
			for (GoogleScrapLinkEntry e : entries) {
				if (e.getFeaturedRank() != null && e.getUrl().equals(entry.getUrl())) {
					entry.setFeaturedRank(e.getFeaturedRank());
					break;
				}
			}
		}
	}

	public static String getTitle(Element element) {
		if (element.children().isEmpty()) {
			// mobile featured snipetts
			return element.text();
		}
		return element.select("h3, div[role=heading]").text();
	}

	protected static volatile ConcurrentHashMap<StatsType, AtomicInteger> stats = new ConcurrentHashMap<>();

	protected void incrementStats(StatsType type) {
		if (!stats.containsKey(type)) {
			stats.putIfAbsent(type, new AtomicInteger());
		}
		stats.get(type).incrementAndGet();
	}

	public static void printStats() {
		if (stats.values().stream().mapToInt(AtomicInteger::get).sum() == 0) {
			return;
		}
		LOG.info("[Scrap Parser Stats] stats: {} {} {} {} {} {} {} {} {} {}",
				stats.getOrDefault(LEGACY, new AtomicInteger()).getAndSet(0),
				stats.getOrDefault(RES, new AtomicInteger()).getAndSet(0),
				stats.getOrDefault(DESCTOP_MAIN_1, new AtomicInteger()).getAndSet(0),
				stats.getOrDefault(DESCTOP_MAIN_2, new AtomicInteger()).getAndSet(0),
				stats.getOrDefault(MOBILE_PING, new AtomicInteger()).getAndSet(0),
				stats.getOrDefault(MOBILE_NO_PING, new AtomicInteger()).getAndSet(0),
				stats.getOrDefault(MOBILE_FEATURED, new AtomicInteger()).getAndSet(0),
				stats.getOrDefault(MOBILE_PING_2, new AtomicInteger()).getAndSet(0),
				stats.getOrDefault(MOBILE_FEATURED_2, new AtomicInteger()).getAndSet(0),
				stats.getOrDefault(MOBILE_ONMOUSEDOWN, new AtomicInteger()).getAndSet(0));
	}
}
