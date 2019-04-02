/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package com.serphacker.serposcope.scraper.google;

import java.util.Objects;
import java.util.Random;


public class GoogleScrapSearch {
    
    private final static Random random = new Random();

    public GoogleScrapSearch() {
    }
    
    int searchId = 0;
    int resultPerPage = 10;
    int pages = 5;
    long minPauseBetweenPageMS = 0l;
    long maxPauseBetweenPageMS = 0l;
    String keyword;
    GoogleCountryCode country;
    String datacenter;
    GoogleDevice device = GoogleDevice.DESKTOP;
    String local;
    String customParameters;

    public int getSearchId() {
        return searchId;
    }

    public void setSearchId(int searchId) {
        this. searchId = searchId;
    }
    
    public int getResultPerPage() {
        return resultPerPage;
    }

    public void setResultPerPage(int resultPerPage) {
        this.resultPerPage = resultPerPage;
    }

    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public GoogleCountryCode getCountry() {
        return country;
    }

    public void setCountry(GoogleCountryCode countryCode) {
        this.country = countryCode;
    }

    public String getDatacenter() {
        return datacenter;
    }

    public void setDatacenter(String datacenter) {
        this.datacenter = datacenter;
    }

    public GoogleDevice getDevice() {
        return device;
    }

    public void setDevice(GoogleDevice device) {
        this.device = device;
    }

    public String getLocal() {
        return local;
    }

    public void setLocal(String local) {
        this.local = local;
    }

    public String getCustomParameters() {
        return customParameters;
    }

    public void setCustomParameters(String customParameters) {
        this.customParameters = customParameters;
    }
    
    public void setPagePauseMS(long minMs, long maxMs){
        if(minMs > maxMs){
            throw new IllegalArgumentException("minMs > maxMs");
        }
        minPauseBetweenPageMS = minMs;
        maxPauseBetweenPageMS = maxMs;
    }
    
    public long getRandomPagePauseMS(){
        if(minPauseBetweenPageMS == maxPauseBetweenPageMS){
            return maxPauseBetweenPageMS;
        }
        return minPauseBetweenPageMS + Math.abs(random.nextLong()%(maxPauseBetweenPageMS-minPauseBetweenPageMS));
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + this.resultPerPage;
        hash = 79 * hash + this.pages;
        hash = 79 * hash + (int) (this.minPauseBetweenPageMS ^ (this.minPauseBetweenPageMS >>> 32));
        hash = 79 * hash + (int) (this.maxPauseBetweenPageMS ^ (this.maxPauseBetweenPageMS >>> 32));
        hash = 79 * hash + Objects.hashCode(this.keyword);
        hash = 79 * hash + (this.country == null ? 0 : (this.country.ordinal()+1) )*100;
        hash = 79 * hash + Objects.hashCode(this.datacenter);
        hash = 79 * hash + (this.device == null ? 0 : (this.device.ordinal()+1) );
        hash = 79 * hash + Objects.hashCode(this.local);
        hash = 79 * hash + Objects.hashCode(this.customParameters);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GoogleScrapSearch other = (GoogleScrapSearch) obj;
        if (this.resultPerPage != other.resultPerPage) {
            return false;
        }
        if (this.pages != other.pages) {
            return false;
        }
        if (this.minPauseBetweenPageMS != other.minPauseBetweenPageMS) {
            return false;
        }
        if (this.maxPauseBetweenPageMS != other.maxPauseBetweenPageMS) {
            return false;
        }
        if (!Objects.equals(this.keyword, other.keyword)) {
            return false;
        }
        if (this.country != other.country) {
            return false;
        }
        if (!Objects.equals(this.datacenter, other.datacenter)) {
            return false;
        }
        if (!Objects.equals(this.local, other.local)) {
            return false;
        }
        if (!Objects.equals(this.customParameters, other.customParameters)) {
            return false;
        }
        if (this.device != other.device) {
            return false;
        }
        return true;
    }
    
}