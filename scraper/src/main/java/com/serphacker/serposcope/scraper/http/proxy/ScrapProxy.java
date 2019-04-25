/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package com.serphacker.serposcope.scraper.http.proxy;


public interface ScrapProxy {
    
	public final static String PROXY_ATTR_SLEEP_TIMESTAMP = "proxySleepTimeStamp";
	public final static String PROXY_ATTR_DESKTOP_USER_AGENT = "proxyDesktopUserAgent";
	public final static String PROXY_ATTR_MOBILE_USER_AGENT = "proxyMobileUserAgent";
	public final static String PROXY_ATTR_DESKTOP_COOKIES = "proxyDesktopCookies";
	public final static String PROXY_ATTR_MOBILE_COOKIES = "proxyMobileCookies";
	public final static String PROXY_ATTR_DESKTOP_REQUEST_COUNT = "proxyDesktopRequestCount";
	public final static String PROXY_ATTR_MOBILE_REQUEST_COUNT = "proxyMobileRequestCount";

    public boolean hasAttr(String key);
    public void setAttr(String key, Object value);
    public  <T> T getAttr(String key, Class<T> clazz);
    public void removeAttr(String key);
    public void clearAttrs();
}
