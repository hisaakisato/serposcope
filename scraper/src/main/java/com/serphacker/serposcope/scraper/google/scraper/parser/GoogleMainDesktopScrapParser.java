package com.serphacker.serposcope.scraper.google.scraper.parser;

import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.serphacker.serposcope.scraper.google.GoogleScrapLinkEntry;
import com.serphacker.serposcope.scraper.google.GoogleScrapResult.Status;

public class GoogleMainDesktopScrapParser extends GoogleLegacyScrapParser {

	@Override
	public Status parse(Element divElement, List<GoogleScrapLinkEntry> entries) {

		Elements links = divElement.select("#main > div > div:first-child > div:first-child > a:first-child,"
				+ "#main > div > div:first-child > a:first-child");

		if (links.isEmpty()) {
			return super.parse(divElement, entries);
		}

		for (Element link : links) {
			if (isAdLink(link)) {
				continue;
			}
			if (underAreaLabel(link)) {
				continue;
			}
			if (!link.children().isEmpty() && "img".equals(link.child(0).tagName())) {
				continue;
			}
			GoogleScrapLinkEntry entry = extractLink(link);
			if (entry == null) {
				continue;
			}

			StatsType type = StatsType.DESCTOP_MAIN_1;
			if ("main".contentEquals(link.parent().parent().parent().attr("id"))) {
				type = StatsType.DESCTOP_MAIN_2;
			}
			incrementStats(type);
			entries.add(entry);
			setFeaturedRank(link, entries);
		}

		return Status.OK;
	}

}
