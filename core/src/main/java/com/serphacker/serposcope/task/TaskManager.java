/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package com.serphacker.serposcope.task;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.serphacker.serposcope.db.base.BaseDB;
import com.serphacker.serposcope.di.TaskFactory;
import com.serphacker.serposcope.models.base.Group;
import com.serphacker.serposcope.models.base.Run;
import com.serphacker.serposcope.models.base.User;
import com.serphacker.serposcope.scraper.http.proxy.ProxyRotator;
import com.serphacker.serposcope.task.google.GoogleTask;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class TaskManager {
    
    private static final Logger LOG = LoggerFactory.getLogger(TaskManager.class);

    @Inject
    TaskFactory googleTaskFactory;
    
    @Inject
    BaseDB db;
    
    public final ProxyRotator rotator = new ProxyRotator(Collections.emptySet());

    final Object googleTaskLock = new Object();
    GoogleTask googleTask;
    
    final Map<String, Object> googleTaskLocks = new ConcurrentHashMap<String, Object>();
    final Map<String, GoogleTask> googleTasks = new ConcurrentHashMap<String, GoogleTask>();

    public boolean isGoogleRunning(){
        synchronized(googleTaskLock){
            if(googleTask != null && googleTask.isAlive()){
                return true;
            }
            return false;
        }        
    }
    
    public boolean startGoogleTask(Run run){
        synchronized(googleTaskLock){
            
            if(googleTask != null && googleTask.isAlive()){
                return false;
            }
            
            googleTask = googleTaskFactory.create(run);
            googleTask.rotator = rotator;
            googleTask.start();
            return true;
        }
    }
    
    public boolean startGoogleTask(Run run, User user, Group group){
    	String key = user.getEmail();
    	googleTaskLocks.putIfAbsent(key, new Object());
    	Object lock = googleTaskLocks.get(key);
        synchronized(lock){
        	GoogleTask googleTask = googleTasks.get(key);
            if(googleTask != null && googleTask.isAlive()){
                return false;
            }

            run.setUser(user);
            run.setGroup(group);
            googleTask = googleTaskFactory.create(run);
            googleTask.rotator = rotator;
            googleTasks.put(key, googleTask);
            googleTask.start();
            return true;
        }
    }

    public boolean abortGoogleTask(boolean interrupt){
        if (this.googleTask != null && this.googleTask.isAlive()){
	    	if(db.run.updateStatusAborting(this.googleTask.getRun())){
	    		this.googleTask.getRun().setStatus(Run.Status.ABORTING);
	        }
	    	this.googleTask.abort();
	        if(interrupt){
	        	this.googleTask.interrupt();
	        }
        }
		for (GoogleTask googleTask : this.googleTasks.values()) {
	        if (googleTask != null && googleTask.isAlive()){
		    	if(db.run.updateStatusAborting(googleTask.getRun())){
		    		googleTask.getRun().setStatus(Run.Status.ABORTING);
		        }
		    	googleTask.abort();
		        if(interrupt){
		        	googleTask.interrupt();
		        }
    		}
		}
    	return true;
    }

    public boolean abortGoogleTask(boolean interrupt, Integer id){
        synchronized(googleTaskLock){
        	GoogleTask googleTask = null; 
        	if (id == null) {
        		googleTask = this.googleTask;
        	} else {
        		if (this.googleTask != null &&
        				id.equals(this.googleTask.getRun().getId())) {
            		googleTask = this.googleTask;
        		}
        		if (googleTask == null) {
        			for (GoogleTask t : this.googleTasks.values()) {
                		if (t != null &&
                				id.equals(t.getRun().getId())) {
                    		googleTask = t;
                    		break;
                		}
        			}
        		}
        	}
            
            if (googleTask == null || !googleTask.isAlive()){
                return false;
            }

        	if(db.run.updateStatusAborting(googleTask.getRun())){
                googleTask.getRun().setStatus(Run.Status.ABORTING);
            }
            googleTask.abort();
            if(interrupt){
                googleTask.interrupt();
            }
            return true;
        }
    }
    
    public void joinGoogleTask() throws InterruptedException {
        synchronized(googleTaskLock){
            if(googleTask == null || !googleTask.isAlive()){
                return;
            }
            
            googleTask.join();
        }        
    }
    
    public Run getRunningGoogleTask() { 
        synchronized(googleTaskLock){
            if(googleTask == null || !googleTask.isAlive()){
                return null;
            }
            
            return googleTask.getRun();
        } 
    }
    
    public List<Run> listRunningTasks(){
        List<Run> tasks = new ArrayList<>();
        
        synchronized(googleTaskLock){
            for (GoogleTask task : listRunningGoogleTasks()) {
                tasks.add(task.getRun());
            }
        }
        
        return tasks;
    }

    public List<GoogleTask> listRunningGoogleTasks() {
        List<GoogleTask> tasks = new ArrayList<>();
        
        synchronized(googleTaskLock){
            if(googleTask != null && googleTask.isAlive()){
                tasks.add(googleTask);
            }
            for (GoogleTask task : googleTasks.values()) {
            	if(task != null && task.isAlive()){
                    tasks.add(task);
                }
            }
        }
        
        return tasks;
    }

}
