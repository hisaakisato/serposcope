/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package com.serphacker.serposcope.task.google;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.serphacker.serposcope.db.google.GoogleDB;
import com.serphacker.serposcope.di.CaptchaSolverFactory;
import com.serphacker.serposcope.di.ScrapClientFactory;
//import com.serphacker.serposcope.di.ScraperFactory;
import com.serphacker.serposcope.models.base.Proxy;
import com.serphacker.serposcope.models.base.Run;
import com.serphacker.serposcope.models.base.User;
import com.serphacker.serposcope.models.base.Run.Mode;
import com.serphacker.serposcope.models.base.Run.Status;
import com.serphacker.serposcope.models.google.GoogleSettings;
import com.serphacker.serposcope.models.google.GoogleRank;
import com.serphacker.serposcope.models.google.GoogleSearch;
import com.serphacker.serposcope.models.google.GoogleSerp;
import com.serphacker.serposcope.models.google.GoogleSerpEntry;
import com.serphacker.serposcope.models.google.GoogleTarget;
import com.serphacker.serposcope.scraper.captcha.solver.CaptchaSolver;
import com.serphacker.serposcope.scraper.google.GoogleScrapLinkEntry;
import com.serphacker.serposcope.scraper.google.GoogleScrapResult;
import com.serphacker.serposcope.scraper.google.scraper.GoogleScraper;
import com.serphacker.serposcope.scraper.http.ScrapClient;
import com.serphacker.serposcope.scraper.http.proxy.DirectNoProxy;
import com.serphacker.serposcope.scraper.http.proxy.ProxyRotator;
import com.serphacker.serposcope.task.AbstractTask;
import com.serphacker.serposcope.task.TaskManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.serphacker.serposcope.scraper.http.proxy.ScrapProxy;
import com.serphacker.serposcope.scraper.utils.UserAgentGenerator;

import java.util.stream.Collectors;
import com.serphacker.serposcope.di.GoogleScraperFactory;
import com.serphacker.serposcope.models.google.GoogleBest;
import com.serphacker.serposcope.models.google.GoogleTargetSummary;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

public class GoogleTask extends AbstractTask {

    protected static final Logger LOG = LoggerFactory.getLogger(GoogleTask.class);

    GoogleScraperFactory googleScraperFactory;
    CaptchaSolverFactory captchaSolverFactory;
    ScrapClientFactory scrapClientFactory;
    
    GoogleDB googleDB;
    public volatile ProxyRotator rotator;

    @Inject
    protected TaskManager taskManager;
    
    Run previousRun;
    int groupCount;
    final Map<Short,Integer> previousRunsByDay = new ConcurrentHashMap<>();
    final Map<Integer,List<GoogleTarget>> targetsByGroup = new ConcurrentHashMap<>();
    final Map<Integer,GoogleTargetSummary> summariesByTarget = new ConcurrentHashMap<>();
    
    LinkedBlockingQueue<GoogleSearch> searches;
    Map<GoogleSearch, AtomicInteger> failedSearches = new HashMap<>();
    GoogleSettings googleOptions;
    protected final AtomicInteger searchDone = new AtomicInteger();
    final AtomicInteger captchaCount = new AtomicInteger();
    volatile int waitingCount;
    volatile int standbyCount;
    
    Thread[] threads;
    volatile int totalSearch;
    volatile boolean interrupted;
    public volatile boolean done;
    
    CaptchaSolver solver;
    String httpUserAgent;
    int httpTimeoutMS;
    boolean updateRun;
    boolean shuffle = true;
    
    @Inject
    public GoogleTask(
        GoogleScraperFactory googleScraperFactory,
        CaptchaSolverFactory captchaSolverFactory,
        ScrapClientFactory scrapClientFactory,
        GoogleDB googleDB,
        @Assisted Run run
    ){
        super(run);
        this.googleScraperFactory = googleScraperFactory;
        this.captchaSolverFactory = captchaSolverFactory;
        this.scrapClientFactory = scrapClientFactory;
        this.googleDB = googleDB;
        this.updateRun = run.getId() == 0 ? false : true;
        if (this.updateRun) {
        	this.totalSearch = run.getTotal();
        	this.searchDone.set(run.getDone());
        }
        
        httpUserAgent = UserAgentGenerator.getUserAgent(true);
        httpTimeoutMS = ScrapClient.DEFAULT_TIMEOUT_MS;
    }

    @Override
    public Run.Status doRun() {
        solver = initializeCaptchaSolver();
        googleOptions = googleDB.options.get();

		groupCount = this.run.getGroup() == null
				? baseDB.group.list(null, this.run.getMode() == Mode.CRON ? this.run.getDay().getDayOfWeek() : null)
						.size()
				: 1;
        initializeSearches();
        initializePreviousRuns();
        initializeTargets();
        
        // reset status
        if (run.getStatus() == Status.RETRYING) {
        	this.run.setDay(this.run.getStarted().toLocalDate());
        	this.run.setStatus(Status.RUNNING);
        	baseDB.run.updateStatus(run);
        }

        if (rotator.list().isEmpty()) {        	
        	List<ScrapProxy> proxies = baseDB.proxy.list().stream().map(Proxy::toScrapProxy).collect(Collectors.toList());        	
        	if(proxies.isEmpty()){
        		LOG.warn("no proxy configured, using direct connection");
        		proxies.add(new DirectNoProxy());
        	}
        	rotator.replace(proxies);
        }

        totalSearch = searches.size();
        this.run.setTotal(totalSearch);
        
        int nThread = Math.min(totalSearch, run.getGroup() == null ?
        		googleOptions.getMaxThreads() :
    					googleOptions.getMaxManualThreads());
        startThreads(nThread);
        waitForThreads();
        
        finalizeSummaries();
        
        if(solver != null){
            try {solver.close();} catch (IOException ex) {}
        }
        
        // LOG.debug("{} proxies failed during the task", proxies.size() - rotator.list().size());
        
        int remainingSearch = totalSearch - searchDone.get();
        if(remainingSearch > 0){
            run.setErrors(remainingSearch);
            LOG.warn("{} searches have not been checked", remainingSearch);
            return Run.Status.DONE_WITH_ERROR;
        }
        
        return Run.Status.DONE_SUCCESS;
    }
    
    public static final int MIN_THREAD_COUNT = 10;

    protected void startThreads(int nThread){
    	GoogleSettings settings = googleDB.options.get();
    	int pause = (settings.getMaxPauseBetweenPageSec() + settings.getMinPauseBetweenPageSec()) / 2;
    	int times = Math.max(1, pause / settings.getAverageDuration());
		int maxThreads = Math.max(Math.min(MIN_THREAD_COUNT, nThread), nThread / times);
        threads = new Thread[maxThreads];
        for (int iThread = 0; iThread < nThread; iThread++) {
        	if (iThread < maxThreads) {
	        	Thread t = new Thread(
	            		new GoogleTaskRunnable(this),
	            		"google-" + this.run.getId() + "-" + iThread);
	        	t.setPriority(Thread.NORM_PRIORITY - 1);
	            threads[iThread] = t;
	            threads[iThread].start();
        	} else {
        		standbyCount++;
        	}
        }        
    }
    
    protected void waitForThreads(){
        while(true){
            try {
                for (Thread thread : threads) {
                    thread.join();
                }
                return;
            }catch(InterruptedException ex){
                interruptThreads();
            }
        }
    }
    
    protected void interruptThreads(){
        interrupted = true;
        for (Thread thread : threads) {
            thread.interrupt();
        }
    }
    
    protected boolean shouldStop(){
        if(searchDone.get() == totalSearch){
            return true;
        }
        
        if(interrupted){
            return true;
        }
        
        if (searches.isEmpty()) {
        	return true;
        }
        
        if (searches.size() < waitingCount) {
        	return true;
        }
        
        return false;
    }
    
    protected void incCaptchaCount(int captchas){
        run.setCaptchas(captchaCount.addAndGet(captchas));
        baseDB.run.updateCaptchas(run);
    }
    
    protected boolean onSearchDone(GoogleSearch search, GoogleScrapResult res){
        if (!insertSearchResult(search, res)) {
        	return false;
        }
        return incSearchDone();
    }
    
    protected boolean incSearchDone(){
        run.setProgress((int) (((float)searchDone.incrementAndGet()/(float)totalSearch)*100f) );
        run.setDone(searchDone.get());
        return baseDB.run.updateProgress(run);
    }
    
    protected boolean insertSearchResult(GoogleSearch search, GoogleScrapResult res) {
        Map<Short, GoogleSerp> history = getHistory(search);

        GoogleSerp serp = new GoogleSerp(run.getId(), search.getId(), run.getStarted());
        if (res.googleResults != null) {
        	serp.setResults(res.googleResults);
        }
        for (GoogleScrapLinkEntry linkEntry : res.entries) {
            GoogleSerpEntry entry = new GoogleSerpEntry(linkEntry);
            entry.fillPreviousPosition(history);
            serp.addEntry(entry);
        }
        if (!googleDB.serp.insert(serp)) {
        	return false;
        }

		List<Integer> groups = googleDB.search.listGroups(search,
				this.run.getMode() == Mode.CRON ? this.run.getDay().getDayOfWeek() : null);
        for (Integer group : groups) {
            List<GoogleTarget> targets = targetsByGroup.get(group);
            if (targets == null) {
                continue;
            }
            Map<Integer, GoogleBest> bests = googleDB.rank.getBestByTarget(group, targets, search.getId());
            for (GoogleTarget target : targets) {
            	GoogleBest best = bests.get(target.getId());
                int rank = GoogleRank.UNRANKED;
                String rankedUrl = null;
                for (int i = 0; i < res.entries.size(); i++) {
                    if (target.match(res.entries.get(i).getUrl())) {
                        rankedUrl = res.entries.get(i).getUrl();
                        rank = i + 1;
                        break;
                    }
                }
                
                int previousRank = GoogleRank.UNRANKED;
                if (previousRun != null) {
                    previousRank = googleDB.rank.get(previousRun.getId(), group, target.getId(), search.getId());
                }
                
                GoogleRank gRank = new GoogleRank(run.getId(), group, target.getId(), search.getId(), rank, previousRank, rankedUrl);
                if (!googleDB.rank.insert(gRank)) {
                	return false;
                }
                
                GoogleTargetSummary summary = summariesByTarget.get(target.getId());
                summary.addRankCandidat(gRank);
                
                if(rank != GoogleRank.UNRANKED && best != null && rank <= best.getRank()){
					if (!googleDB.rank.insertBest(
							new GoogleBest(group, target.getId(), search.getId(), rank, run.getStarted(), rankedUrl))) {
                    	return false;
                    }
                }
            }
        }
        return true;
    }    
    
    protected void initializeSearches() {
        List<GoogleSearch> searchList;
        if (this.run.getGroup() == null) {
	        if(updateRun){
	            searchList = googleDB.search.listUnchecked(run.getId());
	        } else {
				searchList = googleDB.search.listByGroup(null,
						this.run.getMode() == Mode.CRON ? this.run.getDay().getDayOfWeek() : null);
	        }
        } else {
        	searchList = googleDB.search.listByGroup(
        			Arrays.asList(this.run.getGroup().getId()));
        }
        if(shuffle){
            Collections.shuffle(searchList);
        }
        searches = new LinkedBlockingQueue<>(searchList);
        LOG.debug("{} searches to do", searches.size());
    }
    
    protected void initializeTargets() {
        Map<Integer, Integer> previousScorePercent = new HashMap<>();
        
        if(previousRun != null){
            previousScorePercent = googleDB.targetSummary.getPreviousScore(previousRun.getId());
        } 
        
        List<GoogleTarget> targets;
        if (this.run.getGroup() == null) {
			targets = googleDB.target.list(null,
					this.run.getMode() == Mode.CRON ? this.run.getDay().getDayOfWeek() : null);
        } else {
        	targets = googleDB.target.list(
        			Arrays.asList(this.run.getGroup().getId()));
        }
        for (GoogleTarget target : targets) {
            targetsByGroup.putIfAbsent(target.getGroupId(), new ArrayList<>());
            targetsByGroup.get(target.getGroupId()).add(target);
            summariesByTarget.put(
                target.getId(), 
                new GoogleTargetSummary(target.getGroupId(), target.getId(), run.getId(), previousScorePercent.getOrDefault(target.getId(), 0))
            );
        }
        
        if(updateRun){
            List<GoogleTargetSummary> summaries = googleDB.targetSummary.list(run.getId());
            for (GoogleTargetSummary summary : summaries) {
                summariesByTarget.put(summary.getTargetId(), summary);
            }
        }
    }
    
    protected void initializePreviousRuns(){
        previousRun = baseDB.run.findPrevious(run.getId());
        if(previousRun == null){
            return;
        }
        
        short[] days = new short[]{1,7,30,90};
        
        for (short day : days) {
            List<Run> pastRuns = baseDB.run.findByDay(run.getModule(), run.getDay().minusDays(day));
            if(!pastRuns.isEmpty()){
                previousRunsByDay.put(day, pastRuns.get(0).getId());
            }
        }
    }
    
    protected Map<Short,GoogleSerp> getHistory(GoogleSearch search){
        Map<Short,GoogleSerp> history = new HashMap<>();
        
        // TODO
        for (Map.Entry<Short, Integer> entry : previousRunsByDay.entrySet()) {
            GoogleSerp serp = googleDB.serp.get(entry.getValue(), search.getId());
            if(serp != null){
                history.put(entry.getKey(), serp);
            }
        }
        return history;
    }    
    
    protected void finalizeSummaries(){
        Map<Integer, Integer> searchCountByGroup = googleDB.search.countByGroup();
        for (GoogleTargetSummary summary : summariesByTarget.values()) {
            summary.computeScoreBP(searchCountByGroup.getOrDefault(summary.getGroupId(), 0));
        }
        googleDB.targetSummary.insert(summariesByTarget.values());
    }
    
    protected GoogleScraper genScraper(){
        return googleScraperFactory.get(
            scrapClientFactory.get(httpUserAgent, httpTimeoutMS),
            solver
        );
    }

    @Override
    protected void onCrash(Exception ex) {
        
    }
    
    protected final CaptchaSolver initializeCaptchaSolver(){
        solver = captchaSolverFactory.get(baseDB.config.getConfig());
        if(solver != null){
            if(!solver.init()){
                LOG.warn("failed to init captcha solver {}", solver.getFriendlyName());
                return null;
            }
            return solver;
        } else {
            LOG.debug("no captcha service configured");
            return null;
        }
        
    }
    
    int getSearchDone(){
        return searchDone != null ? searchDone.get() : 0;
    }

    public int getRemainigSearches() {
    	return totalSearch - searchDone.intValue();
    }

    protected void removeProxy(ScrapProxy proxy) {
    	rotator.remove(proxy);
    	baseDB.proxy.updateStatus(Proxy.Status.REMOVED, proxy);
    }

    public int getWaitingCount() {
    	return waitingCount + Math.min(standbyCount, searches.size());
    }

    public int getActiveCount() {
    	if (threads == null) {
    		return 0;
    	}
    	return (int) (Arrays.stream(threads).filter(t -> t != null && t.isAlive()).count() - waitingCount);
    }

    @Override
    protected void endRun(Status status) {
    	try {
	    	super.endRun(status);
	    	int targets = targetsByGroup.values()
	    			.stream().collect(Collectors.summingInt(List::size));
	    	int searched = searchDone.intValue();
	        LOG.info("[Task Finished] id: {} status: {} "
	        		+ "duration: {} "
	        		+ "captchas: {} errors: {} "
	        		+ "groups: {} searched: {} remained: {} targets: {}",
	        		run.getId(), run.getStatus(),
	        		String.format("%.2f", run.getDurationMs() * 1.0 / 1000),
	        		run.getCaptchas(), run.getErrors(),
	        		groupCount, searched, totalSearch - searched, targets);
    	} finally {
        	done = true;
            User user = run.getUser();
            if (user != null) {
                List<Run> runs = baseDB.run.listByStatus(Arrays.asList(Status.WAITING), 1L, null, run.getUser(), true);
                if (!runs.isEmpty()) {
                	Run nextRun = runs.get(0);
                	taskManager.startGoogleTask(nextRun, user, nextRun.getGroup());
                }
            }
    	}
    }

    @Override
    public void interrupt() {
    	interrupted = true;
    	super.interrupt();
    }
}
