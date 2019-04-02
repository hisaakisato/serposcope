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
			if (!link.children().isEmpty() && "img".equals(link.child(0).tagName())) {
				continue;
			}

			GoogleScrapLinkEntry entry = extractLink(link);
			if (entry == null) {
				continue;
			}

			entries.add(entry);
		}

		return Status.OK;
	}

}
