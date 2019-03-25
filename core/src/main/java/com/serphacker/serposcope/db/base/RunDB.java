/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package com.serphacker.serposcope.db.base;

import com.google.inject.Singleton;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.SubQueryExpression;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.sql.SQLExpressions;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.dml.SQLDeleteClause;
import com.querydsl.sql.dml.SQLInsertClause;
import com.querydsl.sql.dml.SQLUpdateClause;
import com.serphacker.serposcope.db.AbstractDB;
import com.serphacker.serposcope.models.base.Group;
import com.serphacker.serposcope.models.base.Group.Module;
import com.serphacker.serposcope.models.base.Run;
import com.serphacker.serposcope.models.base.Run.Status;
import com.serphacker.serposcope.models.google.GoogleTarget;
import com.serphacker.serposcope.models.base.User;

import static com.serphacker.serposcope.models.base.Run.Status.ABORTING;
import static com.serphacker.serposcope.models.base.Run.Status.DONE_ABORTED;
import static com.serphacker.serposcope.models.base.Run.Status.DONE_CRASHED;
import static com.serphacker.serposcope.models.base.Run.Status.DONE_SUCCESS;
import static com.serphacker.serposcope.models.base.Run.Status.DONE_WITH_ERROR;
import static com.serphacker.serposcope.models.base.Run.Status.RUNNING;

import com.serphacker.serposcope.querybuilder.QGoogleRank;
import com.serphacker.serposcope.querybuilder.QGoogleSearchGroup;
import com.serphacker.serposcope.querybuilder.QGoogleSerp;
import com.serphacker.serposcope.querybuilder.QGroup;
import com.serphacker.serposcope.querybuilder.QRun;
import com.serphacker.serposcope.querybuilder.QUser;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class RunDB extends AbstractDB {
    
    QRun t_run = QRun.run;
    QGoogleSerp t_serp = QGoogleSerp.googleSerp;
    QGoogleSearchGroup t_searchgroup = QGoogleSearchGroup.googleSearchGroup;
    QUser t_user = QUser.user;
    QGroup t_group = QGroup.group;
    QGoogleRank t_rank = QGoogleRank.googleRank;

    public int insert(Run run) {
        int id = -1;
        try(Connection conn = ds.getConnection()){
            id = new SQLInsertClause(conn, dbTplConf, t_run)
                .set(t_run.moduleId, run.getModule().ordinal())
                .set(t_run.day, Date.valueOf(run.getDay() == null ? null : run.getDay()))
                .set(t_run.started, run.getStarted()== null ? null : Timestamp.valueOf(run.getStarted()))
                .set(t_run.finished, run.getFinished()== null ? null : Timestamp.valueOf(run.getFinished()))
                .set(t_run.captchas, run.getCaptchas())
                .set(t_run.total, run.getTotal())
                .set(t_run.done, run.getDone())
                .set(t_run.progress, run.getProgress())
                .set(t_run.errors, run.getErrors())
                .set(t_run.status, run.getStatus().ordinal())
                .set(t_run.mode, run.getMode().ordinal())
                .set(t_run.userId, run.getUser() == null ? null : run.getUser().getId())
                .set(t_run.groupId, run.getGroup() == null ? null : run.getGroup().getId())
                .executeWithKey(t_run.id);
            
            run.setId(id);
                
        }catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        return id;
    }
    
    public boolean updateStatus(Run run){
        boolean updated = false;
        try(Connection conn = ds.getConnection()){
            updated = new SQLUpdateClause(conn, dbTplConf, t_run)
                .set(t_run.status, run.getStatus().ordinal())
                .where(t_run.id.eq(run.getId()))
                .execute() == 1;
                
        }catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        return updated;
    }
    
    public boolean updateStatusAborting(Run run){
        boolean updated = false;
        try(Connection conn = ds.getConnection()){
            updated = new SQLUpdateClause(conn, dbTplConf, t_run)
                .set(t_run.status, Status.ABORTING.ordinal())
                .where(t_run.id.eq(run.getId()))
                .where(t_run.status.eq(Status.RUNNING.ordinal()))
                .execute() == 1;
                
        }catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        return updated;
    }

    public boolean updateStarted(Run run){
        boolean updated = false;
        try(Connection conn = ds.getConnection()){
            updated = new SQLUpdateClause(conn, dbTplConf, t_run)
                .set(t_run.started, run.getStarted()== null ? null : Timestamp.valueOf(run.getStarted()))
                .where(t_run.id.eq(run.getId()))
                .execute() == 1;
        }catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        return updated;        
    }
    
    public boolean updateFinished(Run run){
        boolean updated = false;
        try(Connection conn = ds.getConnection()){
            updated = new SQLUpdateClause(conn, dbTplConf, t_run)
                .set(t_run.finished, run.getFinished() == null ? null : Timestamp.valueOf(run.getFinished()))
                .set(t_run.progress, run.getProgress())
                .set(t_run.total, run.getTotal())
                .set(t_run.done, run.getDone())
                .set(t_run.captchas, run.getCaptchas())
                .set(t_run.errors, run.getErrors())
                .where(t_run.id.eq(run.getId()))
                .execute() == 1;
                
        }catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        return updated;        
    }
    
    /**
     * update progress, captchas, errors
     */
    public boolean updateProgress(Run run){
        boolean updated = false;
        try(Connection conn = ds.getConnection()){
            updated = new SQLUpdateClause(conn, dbTplConf, t_run)
                .set(t_run.total, run.getTotal())
                .set(t_run.done, run.getDone())
                .set(t_run.progress, run.getProgress())
                .where(t_run.id.eq(run.getId()))
                .execute() == 1;
        }catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        return updated;        
    }
    
    public boolean updateCaptchas(Run run){
        boolean updated = false;
        try(Connection conn = ds.getConnection()){
            updated = new SQLUpdateClause(conn, dbTplConf, t_run)
                .set(t_run.captchas, run.getCaptchas())
                .where(t_run.id.eq(run.getId()))
                .execute() == 1;
        }catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        return updated;        
    }
    
    public void delete(int runId){
        try(Connection conn = ds.getConnection()){
            new SQLDeleteClause(conn, dbTplConf, t_run).where(t_run.id.eq(runId)).execute();
        }catch(Exception ex){
            LOG.error("SQL error", ex);
        }        
    }
    public List<Run> listDone(Integer firstId, Integer lastId){
    	return listDone(firstId, lastId, null);
    }
    public List<Run> listDone(Integer firstId, Integer lastId, GoogleTarget target){
        List<Run> runs = new ArrayList<>();
        try(Connection conn = ds.getConnection()){
            SQLQuery<Tuple> query = new SQLQuery<>(conn, dbTplConf)
            	.distinct()
                .select(getFields())
                .from(t_run)
                .leftJoin(t_user).on(t_run.userId.eq(t_user.id))
                .leftJoin(t_group).on(t_run.groupId.eq(t_group.id));
            
            if(firstId != null){
                query = query.where(t_run.id.goe(firstId));
            }
            
            if(lastId != null){
                query = query.where(t_run.id.loe(lastId));
            }
            
            if (target != null) {
				query.distinct().leftJoin(t_rank).on(t_run.id.eq(t_rank.runId))
						.where(t_rank.googleTargetId.eq(target.getId()));
            }
            List<Tuple> tuples = query
                .where(t_run.finished.isNotNull())
                .orderBy(t_run.id.asc())
                .fetch();
            
            for (Tuple tuple : tuples) {
                Run run = fromTuple(tuple);
                if(run != null){
                    runs.add(run);
                }
            }
            
        }catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        return runs;
    }    
    
    
    public long count(){
        Long count = -1l;
        try(Connection conn = ds.getConnection()){
            count = new SQLQuery<>(conn, dbTplConf).select(t_run.count()).from(t_run).fetchFirst();
        }catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        if(count == null){
            count = -1l;
        }
        return count;
    }        
    
    /*
    public List<Run> listRunning(){
        List<Run> runs = new ArrayList<>();
        try(Connection conn = ds.getConnection()){
            
            List<Tuple> tuples = new SQLQuery<>(conn, dbTplConf)
                .select(t_run.all())
                .from(t_run)
                .where(t_run.status.eq(Run.Status.RUNNING.ordinal()))
                .fetch();
                
            for (Tuple tuple : tuples) {
                runs.add(fromTuple(tuple));
            }
            
        }catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        return runs;        
    }
    */
    
    public final static Collection<Run.Status> STATUSES_RUNNING = Arrays.asList(RUNNING,ABORTING);
    public final static Collection<Run.Status> STATUSES_DONE = Arrays.asList(DONE_ABORTED,DONE_CRASHED,DONE_SUCCESS,DONE_WITH_ERROR);
    public List<Run> listByStatus(Collection<Run.Status> statuses, Long limit, Long offset, User user){
        List<Integer> statusesVal = null;
        if(statuses != null && !statuses.isEmpty()){
            statusesVal = statuses.stream().map(Run.Status::ordinal).collect(Collectors.toList());
        }
        
        List<Run> runs = new ArrayList<>();
        try(Connection conn = ds.getConnection()){
            
            SQLQuery<Tuple> query = new SQLQuery<>(conn, dbTplConf)
                .select(getFields())
                .from(t_run)
	            .leftJoin(t_user).on(t_run.userId.eq(t_user.id))
	            .leftJoin(t_group).on(t_run.groupId.eq(t_group.id));
            
            if(statusesVal != null){
                query.where(t_run.status.in(statusesVal));
            }
                
            query.orderBy(t_run.id.desc());
            
            if(limit != null){
                query.limit(limit);
            }
            
            if(offset != null){
                query.offset(offset);
            }
            
        	if (user != null) {
        		query.where(t_run.userId.eq(user.getId()));
        	}
        	
        	List<Tuple> tuples = query.fetch();
                
            for (Tuple tuple : tuples) {
                Run run = fromTuple(tuple);
                if(run != null){
                    runs.add(run);
                }
            }
            
        }catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        return runs;        
    }
    
    public List<Run> findByDay(Module module, LocalDate day){
        List<Run> runs = new ArrayList<>();
        try(Connection conn = ds.getConnection()){
            
            List<Tuple> tuples = new SQLQuery<>(conn, dbTplConf)
                .select(getFields())
                .from(t_run)
	            .leftJoin(t_user).on(t_run.userId.eq(t_user.id))
	            .leftJoin(t_group).on(t_run.groupId.eq(t_group.id))
                .where(t_run.moduleId.eq(module.ordinal()))
                .where(t_run.day.eq(Date.valueOf(day)))
                .fetch();

            for (Tuple tuple : tuples) {
                Run run = fromTuple(tuple);
                if(run != null){
                    runs.add(run);
                }
            }
            
        }catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        return runs;          
    }    
    
    public Run find(int runId){
        Run run = null;
        try(Connection conn = ds.getConnection()){
            
            Tuple tuple = new SQLQuery<>(conn, dbTplConf)
                .select(getFields())
                .from(t_run)
	            .leftJoin(t_user).on(t_run.userId.eq(t_user.id))
	            .leftJoin(t_group).on(t_run.groupId.eq(t_group.id))
                .where(t_run.id.eq(runId))
                .fetchFirst();
                
            run = fromTuple(tuple);
            
        }catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        return run;
    }
    
    public Run findPrevious(int runId){
        Run run = null;
        try(Connection conn = ds.getConnection()){
            
            Tuple tuple = new SQLQuery<>(conn, dbTplConf)
                .select(getFields())
                .from(t_run)
	            .leftJoin(t_user).on(t_run.userId.eq(t_user.id))
	            .leftJoin(t_group).on(t_run.groupId.eq(t_group.id))
                .where(t_run.id.lt(runId))
                .orderBy(t_run.id.desc())
                .fetchFirst();
                
            run = fromTuple(tuple);
            
        }catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        return run;
    }    
    
    public Run findLast(Module module, Collection<Run.Status> statuses, LocalDate untilDate){
    	return findLast(module, statuses, untilDate, null, null, null, null);
    }

	public Run findLast(Module module, Collection<Run.Status> statuses, LocalDate untilDate, Group group, User user) {
		return findLast(module, statuses, untilDate, group, user, null, null);
	}

	public Run findLast(Module module, Collection<Run.Status> statuses, LocalDate untilDate, Group group, User user,
			Collection<Integer> searchIds, Collection<Integer> targetIds) {
        Run run = null;
        try(Connection conn = ds.getConnection()){

            boolean sub = group != null || (searchIds != null && !searchIds.isEmpty())
					|| (targetIds != null && !targetIds.isEmpty());

            SQLQuery<Tuple> query = new SQLQuery<>(conn,dbTplConf)
                .select(getFields())
                .from(t_run)
                .leftJoin(t_user).on(t_run.userId.eq(t_user.id))
                .leftJoin(t_group).on(t_run.groupId.eq(t_group.id))
                .where(t_run.moduleId.eq(module.ordinal()));
            
            BooleanExpression cond = null;

            if(statuses != null){
            	cond = t_run.status.in(statuses.stream().map(Run.Status::ordinal).collect(Collectors.toList()));
            }
            
            if(untilDate != null){
            	cond = t_run.day.loe(Date.valueOf(untilDate)).and(cond);
            }
        
        	if (user != null) {
            	cond = t_run.userId.eq(user.getId()).and(cond);
        	}

        	if (group != null) {
        		cond = t_rank.groupId.eq(group.getId()).and(cond);
            }

        	if (searchIds != null) {
        		cond = t_rank.googleSearchId.in(searchIds).and(cond);
            }

        	if (targetIds != null) {
        		cond = t_rank.googleTargetId.in(targetIds).and(cond);
            }

        	if (cond != null) {
            	if (sub) {
					SubQueryExpression<Integer> subQuery = SQLExpressions.select(t_run.id.max().as(t_run.id))
							.from(t_run).innerJoin(t_rank).on(t_run.id.eq(t_rank.runId).and(cond)).groupBy(t_run.day);
    				query.where(t_run.id.in(subQuery));
            	} else {
            		query.where(cond);
            	}
        	}

        	Tuple tuple = query
                .orderBy(t_run.id.desc())
                .limit(1)
                .fetchFirst();

            run = fromTuple(tuple);

        }catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        return run;           
    }
    
    public Run findFirst(Module module, Collection<Run.Status> statuses, LocalDate fromDate){
    	return findFirst(module, statuses, fromDate, null, null);
    }

	public Run findFirst(Module module, Collection<Run.Status> statuses, LocalDate fromDate, Group group, User user) {
		return findFirst(module, statuses, fromDate, group, user, null, null);
	}

	public Run findFirst(Module module, Collection<Run.Status> statuses, LocalDate fromDate, Group group, User user,
			Collection<Integer> searchIds, Collection<Integer> targetIds) {
        Run run = null;
        try(Connection conn = ds.getConnection()){

            boolean sub = group != null || (searchIds != null && !searchIds.isEmpty())
					|| (targetIds != null && !targetIds.isEmpty());

            SQLQuery<Tuple> query = new SQLQuery<>(conn,dbTplConf)
                .select(getFields())
                .from(t_run)
                .leftJoin(t_user).on(t_run.userId.eq(t_user.id))
                .leftJoin(t_group).on(t_run.groupId.eq(t_group.id))
                .where(t_run.moduleId.eq(module.ordinal()));
            
            BooleanExpression cond = null;

            if(statuses != null){
            	cond = t_run.status.in(statuses.stream().map(Run.Status::ordinal).collect(Collectors.toList()));
            }
            
            if(fromDate != null){
            	cond = t_run.day.goe(Date.valueOf(fromDate)).and(cond);
            }
        
        	if (user != null) {
            	cond = t_run.userId.eq(user.getId()).and(cond);
        	}

        	if (group != null) {
        		cond = t_rank.groupId.eq(group.getId()).and(cond);
            }

        	if (searchIds != null) {
        		cond = t_rank.googleSearchId.in(searchIds).and(cond);
            }

        	if (targetIds != null) {
        		cond = t_rank.googleTargetId.in(targetIds).and(cond);
            }

        	if (cond != null) {
            	if (sub) {
					SubQueryExpression<Integer> subQuery = SQLExpressions.select(t_run.id.max().as(t_run.id))
							.from(t_run).innerJoin(t_rank).on(t_run.id.eq(t_rank.runId).and(cond)).groupBy(t_run.day);
    				query.where(t_run.id.in(subQuery));
            	} else {
            		query.where(cond);
            	}
        	}
        	
        	Tuple tuple = query
                .orderBy(t_run.id.asc())
                .fetchFirst();

            run = fromTuple(tuple);

        }catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        return run;           
    }    
    
    public void wipe(){
        try(Connection con = ds.getConnection()){
            new SQLDeleteClause(con, dbTplConf, t_run).execute();
        } catch(Exception ex){
            LOG.error("SQL error", ex);
        }
    }
    
    protected Run fromTuple(Tuple tuple){
        
        if(tuple == null){
            return null;
        }
        
        Run run = new Run();
        
        run.setId(tuple.get(t_run.id));
        run.setModule(Module.values()[tuple.get(t_run.moduleId)]);
        run.setDay(tuple.get(t_run.day) == null ? null : tuple.get(t_run.day).toLocalDate());
        run.setStarted(tuple.get(t_run.started) == null ? null : tuple.get(t_run.started).toLocalDateTime());
        run.setFinished(tuple.get(t_run.finished) == null ? null : tuple.get(t_run.finished).toLocalDateTime());
        run.setStatus(Run.Status.values()[tuple.get(t_run.status)]);
        run.setProgress(tuple.get(t_run.progress));
        run.setTotal(tuple.get(t_run.total));
        run.setDone(tuple.get(t_run.done));
        run.setErrors(tuple.get(t_run.errors));
        run.setCaptchas(tuple.get(t_run.captchas));
        run.setMode(Run.Mode.values()[tuple.get(t_run.mode)]);
        Integer userId = tuple.get(t_user.id);
        if (userId != null) {
	        User user = new User();
	        user.setId(tuple.get(t_user.id));
	        user.setEmail(tuple.get(t_user.email));
	        user.setName(tuple.get(t_user.name));
	        user.setAdmin(tuple.get(t_user.admin));
	        run.setUser(user);
        }
        Integer groupId = tuple.get(t_group.id);
        if (groupId != null) {
        	Group group = new Group(
        			groupId,
        			Group.Module.values()[tuple.get(t_group.moduleId)],
        			tuple.get(t_group.name));
    		group.setSundayEnabled(tuple.get(t_group.sundayEnabled) == null ? false : tuple.get(t_group.sundayEnabled));
    		group.setMondayEnabled(tuple.get(t_group.mondayEnabled) == null ? false : tuple.get(t_group.mondayEnabled));
    		group.setTuesdayEnabled(tuple.get(t_group.tuesdayEnabled) == null ? false : tuple.get(t_group.tuesdayEnabled));
    		group.setWednesdayEnabled(tuple.get(t_group.wednesdayEnabled) == null ? false : tuple.get(t_group.wednesdayEnabled));
    		group.setThursdayEnabled(tuple.get(t_group.thursdayEnabled) == null ? false : tuple.get(t_group.thursdayEnabled));
    		group.setFridayEnabled(tuple.get(t_group.fridayEnabled) == null ? false : tuple.get(t_group.fridayEnabled));
    		group.setSaturdayEnabled(tuple.get(t_group.saturdayEnabled) == null ? false : tuple.get(t_group.saturdayEnabled));
        	run.setGroup(group);
        }

        return run;
    }

    private Path<?>[] getFields() {
    	List<Path<?>> paths = new ArrayList<>();
    	paths.addAll(Arrays.asList(t_run.all()));
    	paths.addAll(Arrays.asList(t_user.all()));
    	paths.addAll(Arrays.asList(t_group.all()));
    	return paths.toArray(new Path[paths.size()]);
    }
}
