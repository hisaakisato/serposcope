/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package com.serphacker.serposcope.db.google;

import com.google.inject.Singleton;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.dml.SQLDeleteClause;
import com.querydsl.sql.dml.SQLInsertClause;
import com.querydsl.sql.dml.SQLUpdateClause;
import com.serphacker.serposcope.db.AbstractDB;
import com.serphacker.serposcope.models.google.GoogleTarget;
import com.serphacker.serposcope.models.google.GoogleTarget.PatternType;
import com.serphacker.serposcope.querybuilder.QGoogleTarget;
import com.serphacker.serposcope.querybuilder.QGroup;

import java.sql.Connection;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Singleton
public class GoogleTargetDB extends AbstractDB {

	private static int BULK_INSERT_ROWS = 250;

	QGoogleTarget t_target = QGoogleTarget.googleTarget;
    QGroup t_group = QGroup.group;

    public int insert(Collection<GoogleTarget> targets){
        int inserted = 0;
        
        try(Connection con = ds.getConnection()){
            
        	boolean autoCommit = con.getAutoCommit();
        	try {
	        	con.setAutoCommit(false);
	        	int rows = 0;

	        	for (GoogleTarget target : targets) {
	                Integer key = new SQLInsertClause(con, dbTplConf, t_target)
	                    .set(t_target.groupId, target.getGroupId())
	                    .set(t_target.name,target.getName())
	                    .set(t_target.patternType, (byte)target.getType().ordinal())
	                    .set(t_target.pattern, target.getPattern())
	                    .executeWithKey(t_target.id);
	                if(key != null){
	                    target.setId(key);
	                    inserted++;
	                }
	                if (++rows % BULK_INSERT_ROWS == 0) {
	                	con.commit();
	                }
	            }

	            if (rows % BULK_INSERT_ROWS > 0) {
	            	con.commit();
	            }

        	} finally {
        		con.setAutoCommit(autoCommit);        		
        	}
            
        } catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        
        return inserted;
    }
    
    public boolean rename(GoogleTarget target){
        long inserted = 0;
        
        try(Connection con = ds.getConnection()){
            
            inserted = new SQLUpdateClause(con, dbTplConf, t_target)
                .set(t_target.name, target.getName())
                .where(t_target.id.eq(target.getId()))
                .execute();
            
        } catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        
        return inserted != 0;
    }    
    
    public boolean delete(int targetId){
        boolean deleted = false;
        try(Connection con = ds.getConnection()){
            deleted = new SQLDeleteClause(con, dbTplConf, t_target)
                .where(t_target.id.eq(targetId))
                .execute() == 1;
        }catch(Exception ex){
            LOG.error("SQLError", ex);
        }
        return deleted;
    }
    
    public void wipe(){
        try(Connection con = ds.getConnection()){
            new SQLDeleteClause(con, dbTplConf, t_target).execute();
        }catch(Exception ex){
            LOG.error("SQLError", ex);
        }
    }    
    
    /**
     * list all target
     */
    public List<GoogleTarget> list(){
        return list(false);
    }
    
    public List<GoogleTarget> list(boolean cron){
        return list(null, cron);
    }

    public boolean hasTarget(){
        Integer hasOne=null;
        
        try(Connection con = ds.getConnection()){
            
            hasOne = new SQLQuery<Void>(con, dbTplConf)
                .select(Expressions.ONE)
                .from(t_target)
                .limit(1)
                .fetchFirst();
        } catch(Exception ex){
            LOG.error("SQL error", ex);
        }        
        
        return hasOne != null;
    }
    
    /***
     * list target by group
     */
    public List<GoogleTarget> list(Collection<Integer> groups){
    	return list(groups, false);
    }

    public List<GoogleTarget> list(Collection<Integer> groups, boolean cron){
    	return list(groups, cron ? LocalDate.now().getDayOfWeek() : null);
    }

    public List<GoogleTarget> list(Collection<Integer> groups, DayOfWeek dayOfWeek){
        List<GoogleTarget> targets = new ArrayList<>();
        
        try(Connection con = ds.getConnection()){
            
            SQLQuery<Tuple> query = new SQLQuery<Void>(con, dbTplConf)
                .select(t_target.all())
                .from(t_target)
                .leftJoin(t_group).on(t_target.groupId.eq(t_group.id));
            
            if(groups != null){
                query.where(t_target.groupId.in(groups));
            }

            if (dayOfWeek != null) {
            	switch (dayOfWeek) {
            	case SUNDAY:
                    query.where(t_group.sundayEnabled.isTrue());
            		break;
            	case MONDAY:
                    query.where(t_group.mondayEnabled.isTrue());
            		break;
            	case TUESDAY:
                    query.where(t_group.tuesdayEnabled.isTrue());
            		break;
            	case WEDNESDAY:
                    query.where(t_group.wednesdayEnabled.isTrue());
            		break;
            	case THURSDAY:
                    query.where(t_group.thursdayEnabled.isTrue());
            		break;
            	case FRIDAY:
                    query.where(t_group.fridayEnabled.isTrue());
            		break;
            	case SATURDAY:
                    query.where(t_group.saturdayEnabled.isTrue());
            		break;
            	}
            }

            List<Tuple> tuples = query.fetch();
            
            if(tuples != null){
                for (Tuple tuple : tuples) {
                    targets.add(fromTuple(tuple));
                }
            }
            
        } catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        
        return targets;
    }
    
    public GoogleTarget get(int targetId){
        GoogleTarget target = null;
        
        try(Connection con = ds.getConnection()){
            
            Tuple tuple = new SQLQuery<Void>(con, dbTplConf)
                .select(t_target.all())
                .from(t_target)
                .where(t_target.id.eq(targetId))
                .fetchOne();
            
            target = fromTuple(tuple);
            
        } catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        
        return target;
    }    
    
    GoogleTarget fromTuple(Tuple tuple) throws Exception{
        return new GoogleTarget(
            tuple.get(t_target.id),
            tuple.get(t_target.groupId),
            tuple.get(t_target.name),
            tuple.get(t_target.patternType) == null ? PatternType.REGEX : GoogleTarget.PatternType.values()[tuple.get(t_target.patternType)],
            tuple.get(t_target.pattern)
        );
    }
    
}
