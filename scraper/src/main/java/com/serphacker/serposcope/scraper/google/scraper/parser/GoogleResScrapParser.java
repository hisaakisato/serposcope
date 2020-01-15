package com.serphacker.serposcope.scraper.google.scraper.parser;

import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.serphacker.serposcope.scraper.google.GoogleScrapLinkEntry;
import com.serphacker.serposcope.scraper.google.GoogleScrapResult.Status;

public class GoogleResScrapParser extends GoogleLegacyScrapParser {

	@Override
	public Status parse(Element resElement, List<GoogleScrapLinkEntry> entries) {

		Elements h3Elts = resElement.select("a > h3");
		if (h3Elts.isEmpty()) {
			return super.parse(resElement, entries);
		}

		for (Element h3Elt : h3Elts) {

			Element link = h3Elt.parent();
			GoogleScrapLinkEntry entry = extractLink(link);
			if (entry == null) {
				continue;
			}

			incrementStats(StatsType.RES);
			entries.add(entry);
			setFeaturedRank(link, entries);
		}

		return Status.OK;
	}

}
