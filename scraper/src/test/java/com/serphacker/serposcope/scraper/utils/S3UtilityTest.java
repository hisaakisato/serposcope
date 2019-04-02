package com.serphacker.serposcope.scraper.utils;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.time.LocalDate;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.serphacker.serposcope.scraper.google.GoogleCountryCode;
import com.serphacker.serposcope.scraper.google.GoogleDevice;
import com.serphacker.serposcope.scraper.google.GoogleScrapSearch;

@Ignore
public class S3UtilityTest {

	GoogleScrapSearch search;
	LocalDate date;
	
	@BeforeClass
	public static void setUpClass() {
		System.setProperty("serposcope.upload.bucket", "jp.markeship.serposcope.serps-dev");
	}

	@Before
	public void setUp() {
		date = LocalDate.of(2019, 4, 1);

		search = new GoogleScrapSearch();
		search.setSearchId(111);
		search.setCountry(GoogleCountryCode.JP);
		search.setDevice(GoogleDevice.SMARTPHONE);
		search.setKeyword("hoge");
		search.setCustomParameters("hl=ja&name=sample");
		search.setLocal("Mionato-ku, Tokyo, Japan");
	}

	@Test
	public void testUpload() {
		S3Utilis.upload(date, search, 0, "result");
	}

	@Test
	public void testDownload() throws UnsupportedEncodingException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		S3Utilis.download(baos, date, search.getSearchId(), 0);
		String result = new String(baos.toByteArray(), "UTF-8");
		//System.out.println(result);
		assertEquals("result", result);
	}

//	@Test
//	public void test1() {
//		System.out.println(Pattern.quote("https://aaa.bbb.ccc?aaa=bbb"));
//	}

}
