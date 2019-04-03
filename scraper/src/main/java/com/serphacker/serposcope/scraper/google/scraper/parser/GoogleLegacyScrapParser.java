package com.serphacker.serposcope.scraper.google.scraper.parser;

import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.serphacker.serposcope.scraper.google.GoogleScrapLinkEntry;
import com.serphacker.serposcope.scraper.google.GoogleScrapResult.Status;

public class GoogleLegacyScrapParser extends GoogleAbstractScrapParser {

	@Override
	public Status parse(Element resElement, List<GoogleScrapLinkEntry> entries) {
		Elements h3Elts = resElement.getElementsByTag("h3");
		for (Element h3Elt : h3Elts) {

			if (isSiteLinkElement(h3Elt)) {
				continue;
			}

			Element link = h3Elt.getElementsByTag("a").first();
			GoogleScrapLinkEntry entry = extractLink(link);
			if (entry != null) {
				entries.add(entry);
				setFeaturedRank(link, entries);
			}
		}

		return Status.OK;
	}

	private boolean isSiteLinkElement(Element element) {
		if (element == null) {
			return false;
		}

		Elements parents = element.parents();
		if (parents == null || parents.isEmpty()) {
			return false;
		}

		for (Element parent : parents) {
			if (parent.hasClass("mslg") || parent.hasClass("nrg") || parent.hasClass("nrgw")) {
				return true;
			}
		}

		return false;
	}

}
