/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package com.serphacker.serposcope.models.google;

import it.unimi.dsi.fastutil.shorts.Short2ShortArrayMap;
import java.net.IDN;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.serphacker.serposcope.scraper.google.GoogleScrapLinkEntry;

public class GoogleSerpEntry {
    
    protected final static byte SERIAL_VERSION = 1;

	GoogleScrapLinkEntry entry;
	String url;
	String title;
	String ampUrl;
	Integer featuredRank;
    Short2ShortArrayMap map = new Short2ShortArrayMap();

    public GoogleSerpEntry(GoogleScrapLinkEntry entry) {
    	this.entry = entry;
        this.url = entry.getUrl();
        this.title = entry.getTitle();
        this.ampUrl = entry.getAmpUrl();
        this.featuredRank = entry.getFeaturedRank();
    }
    
    public void fillPreviousPosition(Map<Short,GoogleSerp> history){
        for (Map.Entry<Short, GoogleSerp> entrySet : history.entrySet()) {
            short day = entrySet.getKey();
            List<GoogleSerpEntry> entries = entrySet.getValue().entries;
            int position = 0;
            
            for (int i = 0; i < entries.size(); i++) {
                if(this.equals(entries.get(i))){
                    position = i + 1;
                }
            }
            
            map.put(day, (short)position);
        }
    }

    public String getUrl() {
        return url;
    }
    
    public String getUnicodeUrl() {
        if(url == null){
            return null;
        }
        
        if(!url.contains("xn--")){
            return url;
        }
        
        try {
            URL u = new URL(url);
            return u.getProtocol() + "://" + IDN.toUnicode(u.getHost()) + u.getFile();
        }catch(Exception ex){
            return url;
        }
    }
    
    public Short2ShortArrayMap getMap() {
        return map;
    }

    public void setMap(Short2ShortArrayMap map) {
        this.map = map;
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

	public void setFeaturedRank(Integer rank) {
		this.featuredRank = rank;
	}

	@Override
    public int hashCode() {
        int hash = 7;
        hash = 73 * hash + Objects.hashCode(this.url);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GoogleSerpEntry other = (GoogleSerpEntry) obj;
        if (!Objects.equals(this.url, other.url)) {
            return false;
        }
        return true;
    }
    
    @Override
    public String toString() {
        return "GoogleSerpEntry{" + "url=" + url + ", map=" + map + '}';
    }
    
}
