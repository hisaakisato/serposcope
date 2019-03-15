package com.serphacker.serposcope.scraper.google;

public class GoogleScrapLinkEntry {

    public final static byte SERIAL_VERSION = 1;

	private String url;

	private String title;
	
	private String ampUrl;
	
	private Integer featuredRank;

	public GoogleScrapLinkEntry() {		
	}

	public GoogleScrapLinkEntry(String url) {
		this.url = url;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAmpUrl() {
		return ampUrl;
	}

	public void setAmpUrl(String ampUrl) {
		this.ampUrl = ampUrl;
	}

	public Integer getFeaturedRank() {
		return featuredRank;
	}

	public void setFeaturedRank(Integer featuredRank) {
		this.featuredRank = featuredRank;
	}

}
