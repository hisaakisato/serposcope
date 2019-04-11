package com.serphacker.serposcope.scraper.google.scraper.parser;

import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.serphacker.serposcope.scraper.google.GoogleScrapLinkEntry;
import com.serphacker.serposcope.scraper.google.GoogleScrapResult.Status;

public class GoogleMainWithoutPingMobileScrapParser extends GoogleMainDesktopScrapParser {

	@Override
	public Status parse(Element divElement, List<GoogleScrapLinkEntry> entries) {

		Elements links = divElement
				.select("#main a[href][ping] > div[role=heading],"
						+ "#main .kno-result h3 > a[href][ping]" // featured snippets
						);

		if (links.isEmpty()) {
			return super.parse(divElement, entries);
		}

		for (Element link : links) {
			if (isInnerCard(link) || isAdLink(link)) {
				continue;
			}
			StatsType type = StatsType.MOBILE_FEATURED_2;
			if (!link.tagName().equalsIgnoreCase("a")) {
				type = StatsType.MOBILE_PING_2;
				link = link.parent();
			}
			if (link.attr("ping") == null) {
				String href = link.attr("href");
				if (!PATTERN_GOOGLE_SERVICES.matcher(href).matches()) {
					// check google services
					continue;
				}
			}
			GoogleScrapLinkEntry entry = extractLink(link);
			if (entry == null) {
				continue;
			}
			incrementStats(type);
			entries.add(entry);
			setFeaturedRank(link, entries);
		}

		return Status.OK;
	}

	protected GoogleScrapLinkEntry extractLink(Element element) {
		if (element == null) {
			return null;
		}

		String href = element.attr("href");
		if (href == null) {
			return null;
		}

		// for amp pages
		if (element.hasClass("amp_r") && element.hasAttr("data-amp")) {
			GoogleScrapLinkEntry entry = new GoogleScrapLinkEntry(href);
			entry.setUrl(element.attr("data-amp"));
			entry.setNonAmpUrl(element.attr("data-amp-cur"));
			entry.setTitle(getTitle(element));
			return entry;
		}
		
		return super.extractLink(element);
	}
}
