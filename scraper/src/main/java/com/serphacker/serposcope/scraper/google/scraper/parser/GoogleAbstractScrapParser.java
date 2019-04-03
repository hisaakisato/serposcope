package com.serphacker.serposcope.scraper.google.scraper.parser;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.jsoup.nodes.Element;

import com.serphacker.serposcope.scraper.google.GoogleScrapLinkEntry;
import com.serphacker.serposcope.scraper.google.GoogleScrapResult.Status;

public abstract class GoogleAbstractScrapParser {

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

		//TODO check featured snippets
		// $$('.kp-blk .mod:first-child')
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
			String classes = parent.attr("class");
			if (classes != null && !classes.isEmpty()) {
				return Arrays.stream(classes.split(" ")).map(String::trim).anyMatch(cls -> cls.startsWith("ads-"));
			}
		}
		return false;
	}

	public static String getTitle(Element element) {
		return element.select("h3, div[role=heading]").html();
	}

}
