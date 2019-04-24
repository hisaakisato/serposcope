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
import com.querydsl.sql.dml.SQLMergeClause;
import com.serphacker.serposcope.db.AbstractDB;
import com.serphacker.serposcope.models.google.GoogleSearch;
import com.serphacker.serposcope.querybuilder.QGoogleRank;
import com.serphacker.serposcope.querybuilder.QGoogleRankBest;
import com.serphacker.serposcope.querybuilder.QGoogleSearch;
import com.serphacker.serposcope.querybuilder.QGoogleSearchGroup;
import com.serphacker.serposcope.querybuilder.QGoogleSerp;
import com.serphacker.serposcope.querybuilder.QGroup;
import com.serphacker.serposcope.scraper.google.GoogleDevice;
import java.sql.Connection;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class GoogleSearchDB extends AbstractDB {
    
	private static int BULK_ROWS = 2500;

	QGoogleSearch t_gsearch = QGoogleSearch.googleSearch;
    QGoogleSearchGroup t_ggroup = QGoogleSearchGroup.googleSearchGroup;
    QGoogleSerp t_gserp = QGoogleSerp.googleSerp;
    QGroup t_group = QGroup.group;
    QGoogleRank t_rank = QGoogleRank.googleRank;
    QGoogleRankBest t_best = QGoogleRankBest.googleRankBest;
	QGoogleSerp t_serp = QGoogleSerp.googleSerp;

    public int insert(Collection<GoogleSearch> searches, int groupId){
        int inserted = 0;

        try(Connection con = ds.getConnection()){
        	boolean autoCommit = con.getAutoCommit();
        	try {
	        	con.setAutoCommit(false);
	        	int rows = 0;

	        	for (GoogleSearch search : searches) {
	                if(search.getId() == 0){
	                    Integer key = new SQLInsertClause(con, dbTplConf, t_gsearch)
	                        .set(t_gsearch.keyword, search.getKeyword())
	                        .set(t_gsearch.country, search.getCountry().name())
	                        .set(t_gsearch.datacenter, search.getDatacenter())
	                        .set(t_gsearch.device, (byte)search.getDevice().ordinal())
	                        .set(t_gsearch.local, search.getLocal())
	                        .set(t_gsearch.customParameters, search.getCustomParameters())
	                        .executeWithKey(t_gsearch.id);
	                    if(key != null){
	                        search.setId(key);
	                    }
	                }	                
	                inserted += new SQLMergeClause(con, dbTplConf, t_ggroup)
	                    .set(t_ggroup.groupId, groupId)
	                    .set(t_ggroup.googleSearchId, search.getId())
	                    .execute() == 1 ? 1 : 0;	                
	                if (++rows % BULK_ROWS == 0) {
	                	con.commit();
	                }
	            }
	            
	            if (rows % BULK_ROWS > 0) {
	            	con.commit();
	            }
        	} finally {
        		con.setAutoCommit(autoCommit);        		
        	}
        } catch(Exception ex){
            LOG.error("SQL error", ex);
        } finally {
        }
        
        return inserted;
    }
    

    public int getId(GoogleSearch search){
        int id = 0;
        
        try(Connection con = ds.getConnection()){
            
            
            SQLQuery<Integer> query = new SQLQuery<Void>(con, dbTplConf)
                .select(t_gsearch.id)
                .from(t_gsearch)
                .where(t_gsearch.keyword.eq(search.getKeyword()))
                .where(t_gsearch.device.eq((byte)search.getDevice().ordinal()))
                .where(t_gsearch.country.eq(search.getCountry().name()));
            
            if(search.getDatacenter() != null){
                query.where(t_gsearch.datacenter.eq(search.getDatacenter()));
            } else {
                query.where(t_gsearch.datacenter.isNull());
            }
            
            if(search.getLocal() != null){
                query.where(t_gsearch.local.eq(search.getLocal()));
            } else {
                query.where(t_gsearch.local.isNull());
            }
            
            if(search.getCustomParameters()!= null){
                query.where(t_gsearch.customParameters.eq(search.getCustomParameters()));
            } else {
                query.where(t_gsearch.customParameters.isNull());
            }
            
            Integer fetchedId = query.fetchFirst();
            
            if(fetchedId != null){
                id = fetchedId;
            }
            
        } catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        
        return id;
    }
    
    public GoogleSearch find(Integer id){
        GoogleSearch search = null;
        
        try(Connection con = ds.getConnection()){
            
            Tuple tuple = new SQLQuery<Void>(con, dbTplConf)
                .select(t_gsearch.all())
                .from(t_gsearch)
                .where(t_gsearch.id.eq(id))
                .fetchFirst();
                
            search = fromTuple(tuple);
            
        } catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        
        return search;
    }    
    
    public boolean deleteFromGroup(GoogleSearch search, int groupId){
        boolean deleted = false;
        
        try(Connection con = ds.getConnection()){
            
            deleted = new SQLDeleteClause(con, dbTplConf, t_ggroup)
                .where(t_ggroup.googleSearchId.eq(search.getId()))
                .where(t_ggroup.groupId.eq(groupId))
                .execute() == 1;
            
        } catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        
        return deleted;
    }       
    
    public boolean hasGroup(GoogleSearch search){
        boolean hasGroup = false;
        
        try(Connection con = ds.getConnection()){
            
            hasGroup = new SQLQuery<Void>(con, dbTplConf)
                .select(Expressions.ONE)
                .from(t_ggroup)
                .where(t_ggroup.googleSearchId.eq(search.getId()))
                .fetchCount() > 0;
            
        } catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        
        return hasGroup;
    }
    
    
    public boolean delete(GoogleSearch search){
        boolean deleted = false;
        
        try(Connection con = ds.getConnection()){
            
            deleted = new SQLDeleteClause(con, dbTplConf, t_gsearch)
                .where(t_gsearch.id.eq(search.getId()))
                .execute() == 1;
            
        } catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        
        return deleted;
    }       
    
    
    public void delete(Collection<GoogleSearch> searches, int groupId){
        try(Connection con = ds.getConnection()){
        	boolean autoCommit = con.getAutoCommit();
        	try {
	        	con.setAutoCommit(false);
	        	int rows = 0;


	        	for (GoogleSearch search : searches) {
	        		int googleSearchId = search.getId();
					new SQLDeleteClause(con, dbTplConf, t_ggroup)
							.where(t_ggroup.googleSearchId.eq(googleSearchId)).where(t_ggroup.groupId.eq(groupId))
							.execute();
					new SQLDeleteClause(con, dbTplConf, t_best).where(t_best.groupId.eq(groupId))
							.where(t_best.googleSearchId.eq(googleSearchId)).execute();
					new SQLDeleteClause(con, dbTplConf, t_rank).where(t_rank.groupId.eq(groupId))
							.where(t_rank.googleSearchId.eq(googleSearchId)).execute();
					if ( new SQLQuery<Void>(con, dbTplConf).select(Expressions.ONE).from(t_ggroup)
							.where(t_ggroup.googleSearchId.eq(googleSearchId)).fetchCount() == 0) {
						// no more group
						new SQLDeleteClause(con, dbTplConf, t_serp).where(t_serp.googleSearchId.eq(googleSearchId)).execute();
						new SQLDeleteClause(con, dbTplConf, t_gsearch).where(t_gsearch.id.eq(googleSearchId))
								.execute();					
					}
	                if (++rows % BULK_ROWS == 0) {
	                	con.commit();
		            }
	        	}

	        	if (rows % BULK_ROWS > 0) {
	            	con.commit();
	            }

        	} finally {
	    		con.setAutoCommit(autoCommit);        		
        	}
                            
        } catch(Exception ex){
            LOG.error("SQL error", ex);
        }
	}


	public void wipe(){
        try(Connection con = ds.getConnection()){
            new SQLDeleteClause(con, dbTplConf, t_ggroup).execute();
            new SQLDeleteClause(con, dbTplConf, t_gsearch).execute();
        } catch(Exception ex){
            LOG.error("SQL error", ex);
        }
    }
    
    public long count(){
        Long count = null;
        try(Connection con = ds.getConnection()){
            count =new SQLQuery<Void>(con, dbTplConf)
                .select(t_gsearch.count())
                .from(t_gsearch)
                .fetchFirst();
        } catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        
        return count == null ? -1l : count;
    }
    
    public Map<Integer,Integer> countByGroup(){
        Map<Integer,Integer> map = new HashMap<>();
        try(Connection con = ds.getConnection()){
            List<Tuple> tuples = new SQLQuery<Void>(con, dbTplConf)
                .select(t_ggroup.groupId, t_ggroup.count())
                .from(t_ggroup)
                .groupBy(t_ggroup.groupId)
                .fetch();
            for (Tuple tuple : tuples) {
                map.put(tuple.get(t_ggroup.groupId), tuple.get(1, Long.class).intValue());
            }
        } catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        return map;
    }    
    
    /**
     * list all google search
     */
    public List<GoogleSearch> list(){
        return list(false);
    }
    
    public List<GoogleSearch> list(boolean cron){
        return listByGroup(null, cron);
    }
    
    /***
     * list google searches belonging to a specific group
     */
    public List<GoogleSearch> listByGroup(Collection<Integer> groups){
    	return listByGroup(groups, false);
    }

    public List<GoogleSearch> listByGroup(Collection<Integer> groups, boolean cron){
    	return listByGroup(groups, cron ? LocalDate.now().getDayOfWeek() : null);
    }

    public List<GoogleSearch> listByGroup(Collection<Integer> groups, DayOfWeek dayOfWeek){
        List<GoogleSearch> searches = new ArrayList<>();
        
        try(Connection con = ds.getConnection()){
            
            SQLQuery<Tuple> query = new SQLQuery<Void>(con, dbTplConf)
                .distinct()
                .select(t_gsearch.all())
                .from(t_gsearch)
                .join(t_ggroup).on(t_gsearch.id.eq(t_ggroup.googleSearchId))
                .leftJoin(t_group).on(t_ggroup.groupId.eq(t_group.id));
            
            if(groups != null){
                query.where(t_ggroup.groupId.in(groups));
            }
            
            if(dayOfWeek != null){
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
                    searches.add(fromTuple(tuple));
                }
            }
            
        } catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        
        return searches;
    }
    
    public List<GoogleSearch> listUnchecked(int runId){
        List<GoogleSearch> searches = new ArrayList<>();
        
        try(Connection con = ds.getConnection()){
            
            SQLQuery<Tuple> query = new SQLQuery<Void>(con, dbTplConf)
                .select(t_gsearch.all())
                .from(t_gsearch)
                .where(t_gsearch.id.notIn(
                    new SQLQuery<Void>(con, dbTplConf)
                        .select(t_gserp.googleSearchId)
                        .from(t_gserp)
                        .where(t_gserp.runId.eq(runId))
                ));
            
            List<Tuple> tuples = query.fetch();
            
            if(tuples != null){
                for (Tuple tuple : tuples) {
                    searches.add(fromTuple(tuple));
                }
            }
            
        } catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        
        return searches;
    }    
    
    public Map<Integer, GoogleSearch> mapBySearchId(Collection<Integer> searchId){
        Map<Integer, GoogleSearch> searches = new HashMap<>();
        
        try(Connection con = ds.getConnection()){
            
            SQLQuery<Tuple> query = new SQLQuery<Void>(con, dbTplConf)
                .select(t_gsearch.all())
                .from(t_gsearch)
                .where(t_gsearch.id.in(searchId));
            
            List<Tuple> tuples = query.fetch();
            
            if(tuples != null){
                for (Tuple tuple : tuples) {
                    searches.put(tuple.get(t_gsearch.id), fromTuple(tuple));
                }
            }
            
        } catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        
        return searches;
    }
    
    public List<Integer> listGroups(GoogleSearch search, boolean cron){
        List<Integer> groups = new ArrayList<>();
        
        try(Connection con = ds.getConnection()){
            
        	SQLQuery<Integer> query = new SQLQuery<Void>(con, dbTplConf)
	            .select(t_ggroup.groupId)
	            .from(t_ggroup)
	            .leftJoin(t_group).on(t_ggroup.groupId.eq(t_group.id))
	            .where(t_ggroup.googleSearchId.eq(search.getId()));
        	
        	if (cron) {
            	switch (LocalDate.now().getDayOfWeek()) {
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

        	List<Integer> ids = query.fetch();
            
            if(ids != null){
                groups.addAll(ids);
            }
            
        } catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        
        return groups;        
    }
    
    GoogleSearch fromTuple(Tuple tuple){
        if(tuple == null){
            return null;
        }
        
        GoogleSearch search = new GoogleSearch();
        
        search.setId(tuple.get(t_gsearch.id));
        search.setKeyword(tuple.get(t_gsearch.keyword));
        search.setDatacenter(tuple.get(t_gsearch.datacenter));
        search.setDevice(GoogleDevice.values()[tuple.get(t_gsearch.device)]);
        search.setLocal(tuple.get(t_gsearch.local));
        search.setCountry(tuple.get(t_gsearch.country));
        search.setCustomParameters(tuple.get(t_gsearch.customParameters));
        
        return search;
    }
    
}
