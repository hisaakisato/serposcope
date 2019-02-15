/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package com.serphacker.serposcope.scraper.http.proxy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Queue;


/**
 * thread safe
 * @author admin
 */
public class ProxyRotator {

    final Queue<ScrapProxy> proxies = new ArrayDeque<>();
	final Queue<ScrapProxy> used = new ArrayDeque<>();

	public ScrapProxy rotate(ScrapProxy previousProxy) {
		synchronized (proxies) {
			if (previousProxy != null) {
				ScrapProxy p = used(previousProxy);
				if (p != null) {
					this.used.remove(p);
					this.proxies.add(p);
				}
			}
			ScrapProxy proxy = proxies.poll();
			if (proxy != null) {				
				used.add(proxy);
			}
			return proxy;
		}
	}


    public ProxyRotator(Collection<ScrapProxy> proxies) {
        this.proxies.addAll(proxies);
    }
    
    public boolean addAll(Collection<ScrapProxy> proxies){
        synchronized(proxies){
            return this.proxies.addAll(proxies);
        }
    }
    
	public boolean add(ScrapProxy proxy) {
		synchronized (proxies) {
			ScrapProxy p = used(proxy);
			if (p != null) {
				this.used.remove(p);
				proxy = p;
			}
			return proxies.add(proxy);
		}
	}
    
    public ScrapProxy poll(){
        return rotate(null);
    }
    
    
    public int remaining(){
        synchronized(proxies){
            return proxies.size();
        }
    }
    
    public List<ScrapProxy> list(){
        synchronized(proxies){
            return new ArrayList<>(proxies);
        }
    } 
    
    public List<ScrapProxy> listUsed(){
        synchronized(used){
            return new ArrayList<>(used);
        }
    } 
    
	public void replace(Collection<ScrapProxy> proxies) {
		synchronized (proxies) {
			this.proxies.clear();
			for (ScrapProxy proxy : proxies) {
				ScrapProxy p = used(proxy);
				if (p == null) {
					this.proxies.add(proxy);
				} else {
					this.used.remove(p);
					this.used.add(proxy);
				}
			}
		}
	}

	private ScrapProxy used(ScrapProxy proxy) {
		for (ScrapProxy p : this.used) {
			if (check(proxy, p)) {
				return p;
			}
		}
		return null;
	}
	
	private boolean check(ScrapProxy p1, ScrapProxy p2) {
		if (p1 == null) {
			return p2 == null;
		}
		if (p2 == null) {
			return false;
		}
		if (p1.getClass() != p2.getClass()) {
			return false;
		}
		if (p1 instanceof DirectNoProxy) {
			return true;
		}
		if (p1 instanceof BindProxy) {
			return ((BindProxy) p1).equals((BindProxy) p2);
		}
		if (p1 instanceof HttpProxy) {
			if (((HttpProxy) p1).getPort() != ((HttpProxy) p1).getPort()) {
				return false;
			}
			if (!Objects.equals(((HttpProxy) p1).getIp(), ((HttpProxy) p2).getIp())) {
				return false;
			}
			return true;
		}
		if (p1 instanceof SocksProxy) {
			return ((SocksProxy) p1).match((SocksProxy) p2);
		}
		return false;
	}
    
}
