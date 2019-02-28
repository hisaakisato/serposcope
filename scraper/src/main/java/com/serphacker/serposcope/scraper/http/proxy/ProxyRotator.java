/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package com.serphacker.serposcope.scraper.http.proxy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * thread safe
 * @author admin
 */
public class ProxyRotator {

    private volatile LinkedBlockingQueue<ScrapProxy> proxies = new LinkedBlockingQueue<>();
    private volatile LinkedBlockingQueue<ScrapProxy> used = new LinkedBlockingQueue<>();
    private final ReentrantLock lock = new ReentrantLock(true);

    public ProxyRotator(Collection<ScrapProxy> proxies) {
        this.proxies.addAll(proxies);
    }
    
    public boolean addAll(Collection<ScrapProxy> proxies){
        synchronized(proxies){
            return this.proxies.addAll(proxies);
        }
    }
    
	public boolean add(ScrapProxy proxy) {
		if (proxy == null) {
			return false;
		}
		synchronized (this.proxies) {
			this.used.remove(proxy);
			return this.proxies.add(proxy);
		}
	}
    
    public ScrapProxy poll(){
		try {
			lock.lock();
			ScrapProxy proxy = this.proxies.poll(10, TimeUnit.SECONDS);
			if (proxy != null) {
				this.used.add(proxy);
			}
			return proxy;
		} catch (InterruptedException e) {
		} finally {
			lock.unlock();
		}
		return null;
	}
    
    
    public int remaining(){
        synchronized(this.proxies){
            return this.proxies.size();
        }
    }
    
    public List<ScrapProxy> list(){
        synchronized(this.proxies){
            return new ArrayList<>(this.proxies);
        }
    } 
    
    public List<ScrapProxy> listUsed(){
        synchronized(this.used){
            return new ArrayList<>(this.used);
        }
    } 
    
	public void replace(Collection<ScrapProxy> proxies) {
		synchronized (proxies) {
			this.proxies.retainAll(proxies);
			this.used.retainAll(proxies);
			for (ScrapProxy proxy : proxies) {
				if (!this.proxies.contains(proxy) && !this.used.contains(proxy)) {
					this.proxies.add(proxy);
				}
			}
		}
	}

	public void remove(ScrapProxy proxy) {
		synchronized (proxies) {
			this.proxies.remove(proxy);
			this.used.remove(proxy);
		}
	}
    
}
