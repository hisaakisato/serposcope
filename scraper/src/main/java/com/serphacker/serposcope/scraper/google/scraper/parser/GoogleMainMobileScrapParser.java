package com.serphacker.serposcope.scraper.google.scraper.parser;

import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.serphacker.serposcope.scraper.google.GoogleScrapLinkEntry;
import com.serphacker.serposcope.scraper.google.GoogleScrapResult.Status;

public class GoogleMainMobileScrapParser extends GoogleMainWithoutPingMobileScrapParser {

	@Override
	public Status parse(Element divElement, List<GoogleScrapLinkEntry> entries) {

		Elements links = divElement.select("#main a[href][ping] > div[role=heading]");
		
		if (links.isEmpty()) {
			// no ping attributes
			return super.parse(divElement, entries);
		}

		links = divElement
				.select("#main a[href][ping] > div[role=heading],"
						+ "#main a[href*='.google.'] > div[role=heading]," // google service sites
						+ "#main .kno-result h3 > a[href][ping]" // featured snippets
						);

		for (Element link : links) {
			if (isInnerCard(link) || isAdLink(link)) {
				continue;
			}
			StatsType type = StatsType.MOBILE_FEATURED;
			if (!link.tagName().equalsIgnoreCase("a")) {
				link = link.parent();
				type = StatsType.MOBILE_PING;
			}
			if (link.attr("ping") == null) {
				// check google services
				String href = link.attr("href");
				if (!PATTERN_GOOGLE_SERVICES.matcher(href).matches()) {
					continue;
				}
				type = StatsType.MOBILE_NO_PING;
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
