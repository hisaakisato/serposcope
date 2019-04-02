package com.serphacker.serposcope.scraper.google.scraper.parser;

import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.serphacker.serposcope.scraper.google.GoogleScrapLinkEntry;
import com.serphacker.serposcope.scraper.google.GoogleScrapResult.Status;

public class GoogleResScrapParser extends GoogleLegacyScrapParser {

	@Override
	public Status parse(Element resElement, List<GoogleScrapLinkEntry> entries) {

		Elements h3Elts = resElement.select("a > h3:first-child");
		if (h3Elts.isEmpty()) {
			return super.parse(resElement, entries);
		}

		for (Element h3Elt : h3Elts) {

			GoogleScrapLinkEntry entry = extractLink(h3Elt.parent());
			if (entry == null) {
				continue;
			}

			entries.add(entry);
		}

		return Status.OK;
	}

}
