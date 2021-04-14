/*
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 *
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package com.serphacker.serposcope.scraper.google.scraper;

import com.serphacker.serposcope.scraper.ResourceHelper;
import com.serphacker.serposcope.scraper.google.GoogleCountryCode;
import com.serphacker.serposcope.scraper.google.GoogleScrapLinkEntry;

import static com.serphacker.serposcope.scraper.google.GoogleScrapResult.Status.ERROR_NETWORK;
import static com.serphacker.serposcope.scraper.google.GoogleScrapResult.Status.OK;

import com.serphacker.serposcope.scraper.google.GoogleScrapSearch;
import com.serphacker.serposcope.scraper.http.ScrapClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;

import org.junit.Test;

import static org.junit.Assert.*;

import org.junit.runner.RunWith;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author admin
 */
@RunWith(MockitoJUnitRunner.class)
public class GoogleScraperTest {

    private static final Logger LOG = LoggerFactory.getLogger(GoogleScraperTest.class);

    public GoogleScraperTest() {
    }

    @Test
    public void testBuildUrl() {
        GoogleScraper scraper = new GoogleScraper(null, null);

        GoogleScrapSearch search = null;
        String url = null;

        search = new GoogleScrapSearch();
        search.setKeyword("keyword");
        assertEquals("https://www.google.com/search?q=keyword&oq=keyword&hl=ja&ie=UTF-8", scraper.buildRequestUrl(search, 0));

        search = new GoogleScrapSearch();
        search.setKeyword("keyword");
        assertEquals("https://www.google.com/search?q=keyword&oq=keyword&hl=ja&ie=UTF-8&start=10", scraper.buildRequestUrl(search, 1));

        search = new GoogleScrapSearch();
        search.setKeyword("keyword");
        search.setDatacenter("10.0.0.1");
        assertEquals("https://www.google.com/search?q=keyword&oq=keyword&hl=ja&ie=UTF-8", scraper.buildRequestUrl(search, 0));
    }

    @Test
    public void testParseSerpEmpty() throws IOException {
        ScrapClient http = mock(ScrapClient.class);
        when(http.getContentAsString()).thenReturn("");

        GoogleScraper scraper = new GoogleScraper(http, null);
        assertEquals(ERROR_NETWORK, scraper.parseSerp(new ArrayList<>()));
    }

    @Test
    public void testLastPage() throws IOException {

        List<String> files = ResourceHelper.listResourceInDirectories(new String[]{
            "/google/201804/last-page",
            "/google/201810/last-page"
        });

        for (String file : files) {
            LOG.debug("checking " + file);
            String content = ResourceHelper.toString(file);
            ScrapClient http = mock(ScrapClient.class);
            when(http.getContentAsString()).thenReturn(content);
            GoogleScraper scraper = new GoogleScraper(http, null);
            assertEquals(OK, scraper.parseSerp(new ArrayList<>()));
            assertFalse(scraper.hasNextPage());
        }

    }

    @Test
    public void testParsing() throws Exception {

        List<String> files = ResourceHelper.listResourceInDirectories(new String[]{
            "/google/201804/top-10",
            "/google/201810/top-10"
        });

        for (String file : files) {

            if (file.endsWith(".res")) {
                continue;
            }

            LOG.info("checking {}", file);

            String serpHtml = ResourceHelper.toString(file);
            List<String> serpTop10 = Arrays.asList(ResourceHelper.toString(file + ".res").split("\n"));

            assertFalse(serpHtml.isEmpty());
            assertFalse(serpTop10.isEmpty());

            ScrapClient http = mock(ScrapClient.class);
            when(http.getContentAsString()).thenReturn(serpHtml);
            GoogleScraper scraper = new GoogleScraper(http, null);
            List<GoogleScrapLinkEntry> entries = new ArrayList<>();
            assertEquals(OK, scraper.parseSerp(entries));
            assertTrue(scraper.hasNextPage());

            assertEquals(serpTop10, entries.stream().map(GoogleScrapLinkEntry::getUrl).collect(Collectors.toList()));
        }

    }

    @Test
    public void testDownloadNetworkError() throws Exception {
        ScrapClient http = mock(ScrapClient.class);
        when(http.get(any(), any())).thenReturn(-1);

        GoogleScrapSearch search = new GoogleScrapSearch();
        search.setKeyword("suivi de position");
        search.setCountry(GoogleCountryCode.FR);

        GoogleScraper scraper = new GoogleScraper(http, null);
        assertEquals(ERROR_NETWORK, scraper.scrap(search).status);
    }

    @Test
    public void testParseNetworkError() throws Exception {
        ScrapClient http = mock(ScrapClient.class);
        when(http.get(any(), any())).thenReturn(200);
        when(http.getContentAsString()).thenReturn("");

        GoogleScrapSearch search = new GoogleScrapSearch();
        search.setKeyword("suivi de position");
        search.setCountry(GoogleCountryCode.FR);

        GoogleScraper scraper = new GoogleScraper(http, null);
        assertEquals(ERROR_NETWORK, scraper.scrap(search).status);
    }


    @Test
    public void testBuildUule() {
        assertEquals(
            "w+CAIQICIpTW9udGV1eCxQcm92ZW5jZS1BbHBlcy1Db3RlIGQnQXp1cixGcmFuY2U=",
            GoogleScraper.buildUule("Monteux,Provence-Alpes-Cote d'Azur,France")
        );
        assertEquals(
            "w+CAIQICIGRnJhbmNl",
            GoogleScraper.buildUule("France")
        );
        assertEquals(
            "w+CAIQICIlQ2VudHJlLVZpbGxlLENoYW1wYWduZS1BcmRlbm5lLEZyYW5jZQ==",
            GoogleScraper.buildUule("Centre-Ville,Champagne-Ardenne,France")
        );
        assertEquals(
            "w+CAIQICIfTGlsbGUsTm9yZC1QYXMtZGUtQ2FsYWlzLEZyYW5jZQ==",
            GoogleScraper.buildUule("Lille,Nord-Pas-de-Calais,France")
        );
		// 北海道札幌市
        assertEquals(
            "w+CAIQICIWU2FwcG9ybyxIb2trYWlkbyxKYXBhbg==",
            GoogleScraper.buildUule("Sapporo,Hokkaido,Japan")
        );
		// 宮城県仙台市
        assertEquals(
            "w+CAIQICIeU2VuZGFpLE1peWFnaSBQcmVmZWN0dXJlLEphcGFu",
            GoogleScraper.buildUule("Sendai,Miyagi Prefecture,Japan")
        );
        assertEquals(
            "w+CAIQICITU2VuZGFpLE1peWFnaSxKYXBhbg==",
            GoogleScraper.buildUule("Sendai,Miyagi,Japan")
        );
		// 埼玉県さいたま市
        assertEquals(
            "w+CAIQICIgU2FpdGFtYSxTYWl0YW1hIFByZWZlY3R1cmUsSmFwYW4=",
            GoogleScraper.buildUule("Saitama,Saitama Prefecture,Japan")
        );
        assertEquals(
            "w+CAIQICIVU2FpdGFtYSxTYWl0YW1hLEphcGFu",
            GoogleScraper.buildUule("Saitama,Saitama,Japan")
        );
        // 千葉県千葉市
        assertEquals(
            "w+CAIQICIcQ2hpYmEsQ2hpYmEgUHJlZmVjdHVyZSxKYXBhbg==",
            GoogleScraper.buildUule("Chiba,Chiba Prefecture,Japan")
        );
        assertEquals(
            "w+CAIQICIRQ2hpYmEsQ2hpYmEsSmFwYW4=",
            GoogleScraper.buildUule("Chiba,Chiba,Japan")
        );
        // 東京都港区
        assertEquals(
        	"w+CAIQICISTWluYXRvLFRva3lvLEphcGFu",
        	GoogleScraper.buildUule("Minato,Tokyo,Japan")
        );
		assertEquals(
		    "w+CAIQICIXTWluYXRvIENpdHksVG9reW8sSmFwYW4=",
		    GoogleScraper.buildUule("Minato City,Tokyo,Japan")
		);
		// 東京都立川市
		assertEquals(
        	"w+CAIQICIVVGFjaGlrYXdhLFRva3lvLEphcGFu",
        	GoogleScraper.buildUule("Tachikawa,Tokyo,Japan")
        );
		// 神奈川県横浜市
        assertEquals(
        	"w+CAIQICIiWW9rb2hhbWEsS2FuYWdhd2EgUHJlZmVjdHVyZSxKYXBhbg==",
        	GoogleScraper.buildUule("Yokohama,Kanagawa Prefecture,Japan")
        );
		assertEquals(
		    "w+CAIQICIXWW9rb2hhbWEsS2FuYWdhd2EsSmFwYW4=",
        	GoogleScraper.buildUule("Yokohama,Kanagawa,Japan")
		);
		// 愛知県名古屋市
        assertEquals(
        	"w+CAIQICIdTmFnb3lhLEFpY2hpIFByZWZlY3R1cmUsSmFwYW4=",
        	GoogleScraper.buildUule("Nagoya,Aichi Prefecture,Japan")
        );
		assertEquals(
		    "w+CAIQICISTmFnb3lhLEFpY2hpLEphcGFu",
        	GoogleScraper.buildUule("Nagoya,Aichi,Japan")
		);
		// 大阪府大阪市
        assertEquals(
        	"w+CAIQICITT3Nha2EsIE9zYWthLCBKYXBhbg==",
        	GoogleScraper.buildUule("Osaka, Osaka, Japan")
        );
        assertEquals(
        	"w+CAIQICIRT3Nha2EsT3Nha2EsSmFwYW4=",
        	GoogleScraper.buildUule("Osaka,Osaka,Japan")
        );
        // 兵庫県神戸市
        assertEquals(
        	"w+CAIQICIQS29iZSxIeW9nbyxKYXBhbg==",
        	GoogleScraper.buildUule("Kobe,Hyogo,Japan")
        );
        // 広島県広島市
        assertEquals(
        	"w+CAIQICIkSGlyb3NoaW1hLEhpcm9zaGltYSBQcmVmZWN0dXJlLEphcGFu",
        	GoogleScraper.buildUule("Hiroshima,Hiroshima Prefecture,Japan")
        );
        assertEquals(
        	"w+CAIQICIZSGlyb3NoaW1hLEhpcm9zaGltYSxKYXBhbg==",
        	GoogleScraper.buildUule("Hiroshima,Hiroshima,Japan")
        );
        // 福岡県福岡市
        assertEquals(
        	"w+CAIQICIgRnVrdW9rYSxGdWt1b2thIFByZWZlY3R1cmUsSmFwYW4=",
        	GoogleScraper.buildUule("Fukuoka,Fukuoka Prefecture,Japan")
        );
        assertEquals(
        	"w+CAIQICIVRnVrdW9rYSxGdWt1b2thLEphcGFu",
        	GoogleScraper.buildUule("Fukuoka,Fukuoka,Japan")
        );
        // 沖縄県那覇市
		assertEquals(
		    "w+CAIQICIdTmFoYSxPa2luYXdhIFByZWZlY3R1cmUsSmFwYW4=",
		    GoogleScraper.buildUule("Naha,Okinawa Prefecture,Japan")
		);
		assertEquals(
		    "w+CAIQICISTmFoYSxPa2luYXdhLEphcGFu",
		    GoogleScraper.buildUule("Naha,Okinawa,Japan")
		);
    }

    @Test
    public void extractResults() {
        GoogleScraper scraper = new GoogleScraper(null, null);
        assertEquals(2490l, scraper.extractResultsNumber("Environ 2 490 résultats").longValue());
//        assertEquals(25270000000l, scraper.extractResultsNumber("Page&nbsp;10 sur environ 25&nbsp;270&nbsp;000&nbsp;000&nbsp;résultats<nobr> (0,46&nbsp;secondes)&nbsp;</nobr>"));
//        assertEquals(25270000000l, scraper.extractResultsNumber("Page 10 of about 25,270,000,000 results<nobr> (0.42 seconds)&nbsp;</nobr>"));
        assertEquals(25270000000l, scraper.extractResultsNumber("About 25,270,000,000 results<nobr> (0.28 seconds)&nbsp;</nobr>").longValue());
        assertEquals(225000l, scraper.extractResultsNumber("About 225,000 results<nobr> (0.87 seconds)&nbsp;</nobr>").longValue());
//        assertEquals(225000l, scraper.extractResultsNumber("Page 5 of about 225,000 results (0.45 seconds) "));
    }

}
