package com.serphacker.serposcope.scraper.google.scraper.parser;

import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.serphacker.serposcope.scraper.google.GoogleScrapLinkEntry;
import com.serphacker.serposcope.scraper.google.GoogleScrapResult.Status;

public class GoogleMainMobileScrapParser extends GoogleMainDesktopScrapParser {

	@Override
	public Status parse(Element divElement, List<GoogleScrapLinkEntry> entries) {

		Elements headings = divElement.select("#main a[href][ping] > div[role=heading]");

		for (Element heading : headings) {
			if (isAdLink(heading)) {
				continue;
			}
			Element link = heading.parent();
			GoogleScrapLinkEntry entry = extractLink(link);
			if (entry == null) {
				continue;
			}
			entries.add(entry);
		}

		if (headings.isEmpty()) {
			return super.parse(divElement, entries);
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

		GoogleScrapLinkEntry entry = new GoogleScrapLinkEntry(href);
		// for amp pages
		if (element.hasClass("amp_r") && element.hasAttr("data-amp")) {
			entry.setUrl(element.attr("data-amp"));
			entry.setNonAmpUrl(element.attr("data-amp-cur"));
			return entry;
		}
		
		return super.extractLink(element);
	}
}
