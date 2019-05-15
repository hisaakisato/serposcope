/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package com.serphacker.serposcope.scraper.google.scraper;

import com.serphacker.serposcope.scraper.captcha.Captcha;
import com.serphacker.serposcope.scraper.captcha.CaptchaImage;
import com.serphacker.serposcope.scraper.captcha.CaptchaRecaptcha;
import com.serphacker.serposcope.scraper.captcha.solver.CaptchaSolver;
import com.serphacker.serposcope.scraper.google.GoogleCountryCode;
import com.serphacker.serposcope.scraper.google.GoogleDevice;
import com.serphacker.serposcope.scraper.google.GoogleScrapLinkEntry;
import com.serphacker.serposcope.scraper.google.GoogleScrapResult;
import com.serphacker.serposcope.scraper.google.GoogleScrapResult.Status;
import com.serphacker.serposcope.scraper.google.GoogleScrapSearch;
import com.serphacker.serposcope.scraper.google.scraper.parser.GoogleAbstractScrapParser;
import com.serphacker.serposcope.scraper.google.scraper.parser.GoogleMainDesktopScrapParser;
import com.serphacker.serposcope.scraper.google.scraper.parser.GoogleMainMobileScrapParser;
import com.serphacker.serposcope.scraper.google.scraper.parser.GoogleResScrapParser;
import com.serphacker.serposcope.scraper.http.ScrapClient;
import com.serphacker.serposcope.scraper.http.proxy.DirectNoProxy;
import com.serphacker.serposcope.scraper.http.proxy.ScrapProxy;
import com.serphacker.serposcope.scraper.utils.S3Utilis;
import com.serphacker.serposcope.scraper.utils.UserAgentGenerator;

import java.io.File;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLException;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpHost;
import org.apache.http.TruncatedChunkException;
import org.apache.http.cookie.ClientCookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * not thread safe
 * 
 * @author admin
 */
public class GoogleScraper {

	public final static int DEFAULT_MAX_RETRY = 3;

	private static final String TAG_BASE = "<base href=\"http://www.google.com/\">";
	private static final String TAG_HEAD = "<head>";

	private static final String STYLE_ENTRY_MARK = "<style id=\"serposcope-rank-style\" type=\"text/css\">"
			+ "body"
				+ "{counter-reset:entry}"
			+ "[data-serposcope-entry]"
				+ "{counter-increment:entry}"
			+ "a[data-serposcope-entry]>h3,"
			+ "a[data-serposcope-entry]>div[role=heading],"
			+ "div.kno-result h3>a[data-serposcope-entry]"
				+ "{border:1px solid red;"
				+ "position:relative}"
			+ "a[data-serposcope-entry]>h3:before,"
			+ "a[data-serposcope-entry]>div[role=heading]:before,"
			+ "div.kno-result h3>a[data-serposcope-entry]:before"
				+ "{content:counter(entry)\"位\";"
				+ "position:absolute;"
				+ "font-size:12px;"
				+ "top:-16px}"
			+ "</style>";

	final static BasicClientCookie NCR_COOKIE = new BasicClientCookie("PREF", "ID=1111111111111111:CR=2");

	static {
		NCR_COOKIE.setDomain("google.com");
		NCR_COOKIE.setPath("/");
		NCR_COOKIE.setAttribute(ClientCookie.PATH_ATTR, "/");
		NCR_COOKIE.setAttribute(ClientCookie.DOMAIN_ATTR, ".google.com");
	}

//	public final static String DEFAULT_DESKTOP_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:58.0) Gecko/20100101 Firefox/58.0";
//	public final static String DEFAULT_SMARTPHONE_UA = "Mozilla/5.0 (Android 7.0; Mobile; rv:59.0) Gecko/59.0 Firefox/59.0 ";

	private static final Logger LOG = LoggerFactory.getLogger(GoogleScraper.class);

	int maxRetry = DEFAULT_MAX_RETRY;
	protected ScrapClient http;
	protected CaptchaSolver solver;
	Random random = new Random();

	Document lastSerpHtml = null;
	String lastHtml = null;
	String nextPage = null;
	int captchas = 0;
	GoogleDevice device;

	public GoogleScraper(ScrapClient client, CaptchaSolver solver) {
		this.http = client;
		this.solver = solver;
	}

	public GoogleScrapResult scrap(GoogleScrapSearch search) throws InterruptedException {
		device = search.getDevice();
		lastSerpHtml = null;
		captchas = 0;
		List<GoogleScrapLinkEntry> entries = new ArrayList<>();
		prepareHttpClient(search);
		Long resultsNumber = null;

		LocalDate today = LocalDate.now();
		try {
			String referrer = "https://" + buildHost(search) + "/";
			for (int page = 0; page < search.getPages(); page++) {

				if (Thread.interrupted()) {
					throw new InterruptedException();
				}

				String url = buildRequestUrl(search, page);

				Status status = null;
				for (int retry = 0; retry < maxRetry; retry++) {

					try {
						ScrapProxy proxy = http.getProxy();
						if (proxy != null && proxy.hasAttr(ScrapProxy.PROXY_ATTR_SLEEP_TIMESTAMP)) {
							long pause = proxy.getAttr(ScrapProxy.PROXY_ATTR_SLEEP_TIMESTAMP, Long.class).longValue()
									- System.currentTimeMillis();
							if (pause > 0) {
								LOG.debug("sleeping {} milliseconds [{}]", pause, Thread.currentThread().getName());
								Thread.sleep(pause);
								proxy.removeAttr(ScrapProxy.PROXY_ATTR_SLEEP_TIMESTAMP);
							}
						}
					} catch (ClassCastException ex) {
					} catch (InterruptedException ex) {
						throw ex;
					}
					LOG.debug("GET {} via {} try {}", url,
							http.getProxy() == null ? new DirectNoProxy() : http.getProxy(), retry + 1);

					this.nextPage = null;
					status = downloadSerp(url, referrer, search, retry);
					if (status == Status.OK) {
						status = parseSerp(entries);
						if (status == Status.OK) {
							break;
						}
					}
					if (status == Status.SEE_NEXT_PAGE && this.nextPage != null) {
						// upload first page as 0
						S3Utilis.upload(today, search, 0, this.lastHtml);
						url = this.nextPage;
						this.nextPage = null;
						this.lastHtml = null;
						retry--;
						Thread.sleep(5000); // wait some seconds
						continue;
					}

					if (!isRetryableStatus(status)) {
						break;
					}
				}

				if (this.lastHtml != null) {
					// upload s3
					S3Utilis.upload(today, search, page + 1, this.lastHtml);
				}

				if (status != Status.OK) {
					return new GoogleScrapResult(status, entries, captchas);
				}

				if (page == 0) {
					resultsNumber = parseResultsNumberOnFirstPage();
				}
				
				if (hasNextPage()) {
					Thread.sleep(2500);
				} else {
					break;
				}

			}
			return new GoogleScrapResult(Status.OK, entries, captchas, resultsNumber);
		} finally {
			long pause = search.getRandomPagePauseMS();
			ScrapProxy proxy = http.getProxy();
			if (proxy == null) {
				if (pause > 0) {
					LOG.debug("sleeping {} milliseconds [{}]", pause, Thread.currentThread().getName());
					Thread.sleep(pause);
				}
			} else {
				if (pause > 0) {
					proxy.setAttr(ScrapProxy.PROXY_ATTR_SLEEP_TIMESTAMP, System.currentTimeMillis() + pause);
				} else {
					proxy.removeAttr(ScrapProxy.PROXY_ATTR_SLEEP_TIMESTAMP);
				}
			}
		}
	}

	protected void prepareHttpClient(GoogleScrapSearch search) {

		String userAgent = null;
		ScrapProxy proxy = http.getProxy();
		if (proxy == null) {
			userAgent = UserAgentGenerator.getUserAgent(search.getDevice() == GoogleDevice.DESKTOP); 
		} else {
			switch (search.getDevice()) {
			case DESKTOP:
				if (!proxy.hasAttr(ScrapProxy.PROXY_ATTR_DESKTOP_USER_AGENT)) {
					proxy.setAttr(ScrapProxy.PROXY_ATTR_DESKTOP_USER_AGENT, UserAgentGenerator.getUserAgent(true));
				}
				userAgent = proxy.getAttr(ScrapProxy.PROXY_ATTR_DESKTOP_USER_AGENT, String.class);
				break;
			case SMARTPHONE:
				if (!proxy.hasAttr(ScrapProxy.PROXY_ATTR_MOBILE_USER_AGENT)) {
					proxy.setAttr(ScrapProxy.PROXY_ATTR_MOBILE_USER_AGENT, UserAgentGenerator.getUserAgent(false));
				}
				userAgent = proxy.getAttr(ScrapProxy.PROXY_ATTR_MOBILE_USER_AGENT, String.class);
				break;
			}
		}
		
		http.setUseragent(userAgent);

		String hostname = "www.google.com";
		http.removeRoutes();
		if (search.getDatacenter() != null && !search.getDatacenter().isEmpty()) {
			http.setRoute(new HttpHost(hostname, -1, "https"), new HttpHost(search.getDatacenter(), -1, "https"));
		}
	}

	protected boolean isRetryableStatus(Status status) {
		switch (status) {
		case ERROR_CAPTCHA_INCORRECT:
		case ERROR_NETWORK:
			return true;
		default:
			return false;
		}
	}

	protected Status downloadSerp(String url, String referrer, GoogleScrapSearch search, int retry) {
		if (referrer == null) {
			referrer = "https://www.google.com";
		}

		int status = http.get(url, referrer);
		switch (status) {
		case 200:
			return Status.OK;

		case 403:
			try {
				Thread.sleep((retry + 1) * 2500);
			} catch (Exception ex) {
			}
			break;

		case 302:
			++captchas;
			return handleCaptchaRedirect(url, referrer, http.getResponseHeader("location"));
		}

		Exception cause = http.getException();
		if (cause != null && http.getProxy() != null) {
			ScrapProxy proxy = http.getProxy();
			if (proxy != null) {
				if (cause instanceof SSLException || cause instanceof InterruptedIOException
						|| cause instanceof ConnectException
						|| cause instanceof ConnectionClosedException
						|| cause instanceof TruncatedChunkException) {
					// proxy was gone
					return Status.ERROR_PROXY_GONE;
				}
			}

			// unknown error
			LOG.error("[ScrapClient Error] status: {} message: {}", status, http.getException().getMessage(),
					http.getException());
		}
		return Status.ERROR_NETWORK;
	}

	protected Status parseSerp(List<GoogleScrapLinkEntry> entries) {
		String html = http.getContentAsString();
		if (html == null || html.isEmpty()) {
			return Status.ERROR_NETWORK;
		}

		lastSerpHtml = Jsoup.parse(html);
		if (lastSerpHtml == null) {
			return Status.ERROR_NETWORK;
		}

		Status status = null;
		int entryStart = entries.size();
		Element resDiv = lastSerpHtml.getElementById("res");
		if (resDiv != null) {
			status = new GoogleResScrapParser().parse(resDiv, entries);
		}

		if (status == null) {
			Element mainDiv = lastSerpHtml.getElementById("main");
			if (mainDiv == null) {
				// check nextpage
				if ((this.nextPage = getNextResultPageLink()) != null) {
					lastHtml = html;
					status = Status.SEE_NEXT_PAGE;
				}
			} else {
				if (device == GoogleDevice.DESKTOP) {
					status = new GoogleMainDesktopScrapParser().parse(mainDiv, entries);
				} else {
					status = new GoogleMainMobileScrapParser().parse(mainDiv, entries);
				}
			}
		}


		if (status == Status.OK || status == Status.SEE_NEXT_PAGE) {
			StringBuilder sb = new StringBuilder();
			Matcher m = Pattern.compile(TAG_HEAD).matcher(html);
			if (m.find()) {
				int nextIdx = m.end();
				sb.append(html.substring(0, nextIdx)).append(TAG_BASE);
				if (status == Status.OK) {
					sb.append(STYLE_ENTRY_MARK);
					// mark serp html
					for (int i = entryStart; i < entries.size(); i++) {
						GoogleScrapLinkEntry entry = entries.get(i);
						String title = entry.getTitle();
						if (title == null) {
							continue;
						}
						m.usePattern(Pattern.compile(String.format("(<a ([^>]* )?(href|data-amp)=\"%s\"[^>]*>)(.*?)(</a>)",
								Pattern.quote(entry.getUrl()).replaceAll("&", "&amp;"))));
						if (m.find(nextIdx)) {
							do {
								String anchorHtml = html.substring(m.start(), m.end());
								Element anchor = Jsoup.parse(anchorHtml).select("a").first();
								if (title.equals(GoogleAbstractScrapParser.getTitle(anchor))) {
									sb.append(html.subSequence(nextIdx, m.start()))
											.append(anchorHtml.replaceFirst("href", String
													.format(" data-serposcope-entry=%d title=%d href", i + 1, i + 1)));
									nextIdx = m.end();
									break;
								}
							} while (m.find());
						}
					}
				}
				if (nextIdx < html.length()) {
					sb.append(html.substring(nextIdx));
				}
			} else {
				sb.append(html);
			}
			lastHtml = sb.toString(); // keep for upload		
		}
		return status == null ? Status.ERROR_PARSING : status;
	}

	protected String getNextResultPageLink() {
		Element body = lastSerpHtml.select("body").first();
		if (body.attr("jsmodel") != null) {
			Element anchor = body.select("a[role=button]").last();
			return anchor.attr("href");
		}
		return null;
	}

	protected Long parseResultsNumberOnFirstPage() {
		if (lastSerpHtml == null) {
			return null;
		}

		Element resultstStatsDiv = lastSerpHtml.getElementById("resultStats");
		if (resultstStatsDiv == null) {
			return null;
		}

		return extractResultsNumber(resultstStatsDiv.html());
	}

	protected Long extractResultsNumber(String html) {
		if (html == null || html.isEmpty()) {
			return null;
		}
		html = html.replaceAll("\\(.+\\)", "").replaceAll("（.+）", "");
		html = html.replaceAll("[^0-9]+", "");
		if (!html.isEmpty()) {
			return Long.parseLong(html);
		}
		return null;
	}

	protected boolean hasNextPage() {
		if (lastSerpHtml == null) {
			return false;
		}

		if (lastSerpHtml.getElementById("pnnext") != null) {
			return true;
		}

		Elements navends = lastSerpHtml.getElementsByClass("navend");
		if (navends.size() > 1 && navends.last().children().size() > 0
				&& "a".equals(navends.last().child(0).tagName())) {
			return true;
		}

		final Elements footerLinks = lastSerpHtml.select("footer a");
		return footerLinks.stream().filter(e -> e.text().endsWith(">")).findAny().isPresent();

	}

	protected String buildRequestUrl(GoogleScrapSearch search, int page) {
		String url = "https://";
		try {
			url += buildHost(search) + "/search?q=" + URLEncoder.encode(search.getKeyword(), "utf-8");
		} catch (UnsupportedEncodingException ex) {
			url += buildHost(search) + "/search?q=" + search.getKeyword();
		}

		if (search.getCountry() != null && !GoogleCountryCode.__.equals(search.getCountry())) {
			url += "&gl=" + search.getCountry().name().toLowerCase();
		}

		String uule = buildUule(search.getLocal());
		if (uule != null) {
			url += "&uule=" + uule;
		}

		if (search.getCustomParameters() != null && !search.getCustomParameters().isEmpty()) {
			if (search.getCustomParameters().contains("gl=")) {
				LOG.warn("custom parameter contains gl= parameter, use country code instead");
			}

			if (!search.getCustomParameters().startsWith("&")) {
				url += "&";
			}
			url += search.getCustomParameters();
		}

		if (search.getResultPerPage() != 10) {
			url += "&num=" + search.getResultPerPage();
		}

		if (page > 0) {
			url += "&start=" + (page * search.getResultPerPage());
		}
		return url;
	}

	protected String buildHost(GoogleScrapSearch search) {
		return "www.google.com";
	}

	private final static String UULE_LENGTH = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";

	protected static String buildUule(String location) {
		if (location == null || location.isEmpty()) {
			return null;
		}

		byte[] locationArray = location.getBytes();
		if (locationArray.length + 1 > UULE_LENGTH.length()) {
			LOG.warn("unencodable uule location, length is too long {}", location);
			return null;
		}

		return "w+CAIQICI" + UULE_LENGTH.charAt(locationArray.length)
				+ Base64.getEncoder().encodeToString(locationArray);
	}

	protected static String decodeUule(String uule) {
		if (uule == null || uule.isEmpty()) {
			return null;
		}
		if (!uule.startsWith("w+CAIQICI")) {
			return null;
		}
		return new String(Base64.getDecoder().decode(uule.substring(10)));
	}

	final static Pattern PATTERN_CAPTCHA_ID = Pattern.compile("/sorry/image\\?id=([0-9]+)&?");

	protected Status handleCaptchaRedirect(String url, String referrer, String redirect) {

		http.clearCookies();
		if (redirect.contains(".com/search")) {
			http.addCookie(NCR_COOKIE);
		}

		int status = http.get(url, referrer);
		LOG.info("GOT[refetch] status=[{}] exception=[{}]", status, http.getException() == null ? "none"
				: (http.getException().getClass().getSimpleName() + " : " + http.getException().getMessage()));
		if (status == 200) {
			return Status.OK;
		}

		if (status == 302) {
			redirect = http.getResponseHeader("location");
		}

		if (redirect == null || !redirect.contains("?continue=")) {
			return Status.ERROR_NETWORK;
		}

		LOG.debug("captcha form detected via {}", http.getProxy() == null ? new DirectNoProxy() : http.getProxy());
		status = http.get(redirect);
		if (status == 403) {
			return Status.ERROR_IP_BANNED;
		}

		if (solver == null) {
			return Status.ERROR_CAPTCHA_NO_SOLVER;
		}

		String content = http.getContentAsString();
		if (content == null) {
			return Status.ERROR_NETWORK;
		}

		Document doc = Jsoup.parse(content, redirect);

		Elements noscript = doc.getElementsByTag("noscript");
		if (!noscript.isEmpty()) {
			LOG.debug("noscript form detected, trying with captcha image");
			return noscriptCaptchaForm(doc, redirect);
//            if(Status.OK.equals(ret)){
//                return ret;
//            }
		}

		LOG.debug("trying with captcha recaptcha");
		return recaptchaForm(doc, redirect);
	}

	protected Status recaptchaForm(Document doc, String captchaRedirect) {

		Elements siteKeys = doc.getElementsByAttribute("data-sitekey");
		if (siteKeys.isEmpty()) {
			debugDump("missing-data-sitekey-1", doc.toString());
			LOG.debug("recaptcha sitekey not detected (1)");
			return Status.ERROR_NETWORK;
		}

		String siteKey = siteKeys.first().attr("data-sitekey");
		if (siteKey.isEmpty()) {
			debugDump("missing-data-sitekey-2", doc.toString());
			LOG.debug("recaptcha sitekey not detected (2)");
			return Status.ERROR_NETWORK;
		}

		Element form = siteKeys.first().parent();
		if (form == null) {
			LOG.debug("can't find captcha form (recaptcha)");
			return Status.ERROR_NETWORK;
		}

		Map<String, Object> map = new HashMap<>();
		Elements inputs = form.getElementsByTag("input");
		String formAction = form.attr("abs:action");
		for (Element input : inputs) {

			if ("noscript".equals(input.parent().tagName())) {
				continue;
			}

			String name = input.attr("name") == null ? "" : input.attr("name");
			String value = input.attr("value") == null ? "" : input.attr("value");

			if (name.isEmpty()) {
				continue;
			}

			map.put(name, value);
		}

		if (map.isEmpty()) {
			LOG.debug("inputs empty (recaptcha)");
			return Status.ERROR_NETWORK;
		}

		if (formAction == null) {
			LOG.debug("form action is null (recaptcha)");
			return Status.ERROR_NETWORK;
		}

		CaptchaRecaptcha captcha = new CaptchaRecaptcha(siteKey, captchaRedirect);
		boolean solved = solver.solve(captcha);
		if (!solved || !Captcha.Status.SOLVED.equals(captcha.getStatus())) {
			LOG.error("solver can't resolve captcha error = {}", captcha.getError());
			if (Captcha.Error.SERVICE_OVERLOADED.equals(captcha.getError())) {
				LOG.warn("server is overloaded, increase maximum BID on {}", captcha.getLastSolver().getFriendlyName());
			}
			return Status.ERROR_CAPTCHA_INCORRECT;
		}
		LOG.debug("got captcha response {} in {} seconds from {}", captcha.getResponse(),
				captcha.getSolveDuration() / 1000l,
				(captcha.getLastSolver() == null ? "?" : captcha.getLastSolver().getFriendlyName()));
		map.put("g-recaptcha-response", captcha.getResponse());

		int postCaptchaStatus = http.post(formAction, map, ScrapClient.PostType.URL_ENCODED, "utf-8", captchaRedirect);
		if (postCaptchaStatus == 302) {
			String redirectOnSuccess = http.getResponseHeader("location");
			if (redirectOnSuccess.startsWith("http://")) {
				redirectOnSuccess = "https://" + redirectOnSuccess.substring(7);
			}

			int redirect1status = http.get(redirectOnSuccess, captchaRedirect);
			if (redirect1status == 200) {
				return Status.OK;
			}

			if (redirect1status == 302) {
				if (http.get(http.getResponseHeader("location"), captchaRedirect) == 200) {
					return Status.OK;
				}
			}
		}

		if (postCaptchaStatus == 503) {
			LOG.debug("Failed to resolve captcha (incorrect response = {}) (recaptcha)", captcha.getResponse());
//            solver.reportIncorrect(captcha);
		}

		return Status.ERROR_CAPTCHA_INCORRECT;
	}

	protected void debugDump(String name, String data) {
		if (name == null || data == null) {
			return;
		}
		File dumpFile = new File(System.getProperty("java.io.tmpdir") + File.separator + name + ".txt");
		try {
			Files.write(dumpFile.toPath(), data.getBytes());
		} catch (Exception ex) {
		}
		LOG.debug("debug dump created in {}", dumpFile.getAbsolutePath());
	}

	protected Status noscriptCaptchaForm(Document captchaDocument, String captchaRedirect) {

		String imageSrc = null;
		Elements elements = captchaDocument.getElementsByTag("img");
		for (Element element : elements) {
			String src = element.attr("abs:src");
			if (src != null && src.contains("/sorry/image")) {
				imageSrc = src;
			}
		}

		if (imageSrc == null) {
			LOG.debug("can't find captcha img tag");
			return Status.ERROR_NETWORK;
		}

		Element form = captchaDocument.getElementsByTag("form").first();
		if (form == null) {
			LOG.debug("can't find captcha form");
			return Status.ERROR_NETWORK;
		}

		String continueValue = null;
		String formIdValue = null;
		String formUrl = form.attr("abs:action");
		String formQValue = null;

		Element elementCaptchaId = form.getElementsByAttributeValue("name", "id").first();
		if (elementCaptchaId != null) {
			formIdValue = elementCaptchaId.attr("value");
		}
		Element elementContinue = form.getElementsByAttributeValue("name", "continue").first();
		if (elementContinue != null) {
			continueValue = elementContinue.attr("value");
		}
		Element elementQ = form.getElementsByAttributeValue("name", "q").first();
		if (elementQ != null) {
			formQValue = elementQ.attr("value");
		}

		if (formUrl == null || (formIdValue == null && formQValue == null) || continueValue == null) {
			LOG.debug("invalid captcha form");
			return Status.ERROR_NETWORK;
		}

		int imgStatus = http.get(imageSrc, captchaRedirect);
		if (imgStatus != 200 || http.getContent() == null) {
			LOG.debug("can't download captcha image {} (status code = {})", imageSrc, imgStatus);
			return Status.ERROR_NETWORK;
		}

		CaptchaImage captcha = new CaptchaImage(new byte[][] { http.getContent() });
		boolean solved = solver.solve(captcha);
		if (!solved || !Captcha.Status.SOLVED.equals(captcha.getStatus())) {
			LOG.error("solver can't resolve captcha (overload ?) error = {}", captcha.getError());
			return Status.ERROR_CAPTCHA_INCORRECT;
		}
		LOG.debug("got captcha response {} in {} seconds from {}", captcha.getResponse(),
				captcha.getSolveDuration() / 1000l,
				(captcha.getLastSolver() == null ? "?" : captcha.getLastSolver().getFriendlyName()));

		try {
			formUrl += "?continue=" + URLEncoder.encode(continueValue, "utf-8");
		} catch (Exception ex) {
		}
		formUrl += "&captcha=" + captcha.getResponse();

		if (formIdValue != null) {
			formUrl += "&id=" + formIdValue;
		}
		if (formQValue != null) {
			formUrl += "&q=" + formQValue;
		}

		int postCaptchaStatus = http.get(formUrl, captchaRedirect);

		if (postCaptchaStatus == 302) {
			String redirectOnSuccess = http.getResponseHeader("location");
			if (redirectOnSuccess.startsWith("http://")) {
				redirectOnSuccess = "https://" + redirectOnSuccess.substring(7);
			}

			int redirect1status = http.get(redirectOnSuccess, captchaRedirect);
			if (redirect1status == 200) {
				return Status.OK;
			}

			if (redirect1status == 302) {
				if (http.get(http.getResponseHeader("location"), captchaRedirect) == 200) {
					return Status.OK;
				}
			}
		}

		if (postCaptchaStatus == 503) {
			LOG.debug("reporting incorrect captcha (incorrect response = {})", captcha.getResponse());
			solver.reportIncorrect(captcha);
		}

		return Status.ERROR_CAPTCHA_INCORRECT;
	}

	public ScrapClient getHttp() {
		return http;
	}

	public void setHttp(ScrapClient http) {
		this.http = http;
	}

	public CaptchaSolver getSolver() {
		return solver;
	}

	public void setSolver(CaptchaSolver solver) {
		this.solver = solver;
	}

	public int getMaxRetry() {
		return maxRetry;
	}

	public void setMaxRetry(int maxRetry) {
		this.maxRetry = maxRetry;
	}

}
