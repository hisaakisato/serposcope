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
import com.querydsl.core.types.SubQueryExpression;
import com.querydsl.sql.SQLExpressions;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.dml.SQLDeleteClause;
import com.querydsl.sql.dml.SQLInsertClause;
import com.querydsl.sql.dml.SQLUpdateClause;
import com.serphacker.serposcope.db.AbstractDB;
import com.serphacker.serposcope.models.base.Group;
import com.serphacker.serposcope.models.base.Group.Module;
import com.serphacker.serposcope.models.base.User;
import com.serphacker.serposcope.querybuilder.QGoogleSearchGroup;
import com.serphacker.serposcope.querybuilder.QGoogleTarget;
import com.serphacker.serposcope.querybuilder.QGroup;
import com.serphacker.serposcope.querybuilder.QUser;
import com.serphacker.serposcope.querybuilder.QUserGroup;
import java.sql.Connection;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class GroupDB extends AbstractDB {
    
    QGroup t_group = QGroup.group;
    QUser t_user = QUser.user;
    QUserGroup t_user_group = QUserGroup.userGroup;
    QGoogleSearchGroup t_searchGroup = QGoogleSearchGroup.googleSearchGroup;
    QGoogleTarget t_target = QGoogleTarget.googleTarget;

    public int insert(Group group){
        int id = 0;
        
        try(Connection con = ds.getConnection()){
            
            Integer key = new SQLInsertClause(con, dbTplConf, t_group)
                .set(t_group.moduleId, group.getModule().ordinal())
                .set(t_group.name, group.getName())
                .set(t_group.ownerId, group.getOwner() == null ? 0 : group.getOwner().getId())
                .set(t_group.shared, group.isShared())
                .set(t_group.sundayEnabled, group.isSundayEnabled())
                .set(t_group.mondayEnabled, group.isMondayEnabled())
                .set(t_group.tuesdayEnabled, group.isTuesdayEnabled())
                .set(t_group.wednesdayEnabled, group.isWednesdayEnabled())
                .set(t_group.thursdayEnabled, group.isThursdayEnabled())
                .set(t_group.fridayEnabled, group.isFridayEnabled())
                .set(t_group.saturdayEnabled, group.isSaturdayEnabled())
                .executeWithKey(t_group.id);
            
            if(key != null){
                id = key;
                group.setId(id);
            }
            
        }catch(Exception ex){
            LOG.error("SQLError ex", ex);
        }
        
        return id;
    }
    
    public boolean delete(Group group){
        boolean deleted = false;
        
        try(Connection con = ds.getConnection()){
            
            deleted = new SQLDeleteClause(con, dbTplConf, t_group)
                .where(t_group.id.eq(group.getId()))
                .execute() == 1;
            
        }catch(Exception ex){
            LOG.error("SQLError ex", ex);
        }
        
        return deleted;
    }
    
    public void wipe(){
        try(Connection con = ds.getConnection()){
            new SQLDeleteClause(con, dbTplConf, t_user_group).execute();
            new SQLDeleteClause(con, dbTplConf, t_group).execute();
        }catch(Exception ex){
            LOG.error("SQLError ex", ex);
        }
    }    
    
    public boolean update(Group group){
        boolean updated = false;
        
        try(Connection con = ds.getConnection()){
            updated = new SQLUpdateClause(con, dbTplConf, t_group)
                .set(t_group.name, group.getName())
                .set(t_group.shared, group.isShared())
                .set(t_group.sundayEnabled, group.isSundayEnabled())
                .set(t_group.mondayEnabled, group.isMondayEnabled())
                .set(t_group.tuesdayEnabled, group.isTuesdayEnabled())
                .set(t_group.wednesdayEnabled, group.isWednesdayEnabled())
                .set(t_group.thursdayEnabled, group.isThursdayEnabled())
                .set(t_group.fridayEnabled, group.isFridayEnabled())
                .set(t_group.saturdayEnabled, group.isSaturdayEnabled())
                .where(t_group.id.eq(group.getId()))
                .execute() == 1;
        }catch(Exception ex){
            LOG.error("SQLError ex", ex);
        }
        
        return updated;
    }    
    
    public List<Group> list(){
        return list(null, null);
    }
    
    public List<Group> list(DayOfWeek dayOfWeek){
        return list(null, dayOfWeek);
    }
    
    public List<Group> list(Module module){
    	return list(module, null);
    }

    public List<Group> list(Module module, DayOfWeek dayOfWeek){
        List<Group> groups = new ArrayList<>();
        try(Connection con = ds.getConnection()){
            
			SubQueryExpression<Tuple> subQuery = SQLExpressions
					.select(t_target.groupId, t_target.groupId.count().castToNum(Integer.class).as(t_target.id))
					.from(t_target).groupBy(t_target.groupId);

			SQLQuery<Tuple> query = new SQLQuery<Void>(con, dbTplConf)
					.select(t_group.id, t_group.moduleId, t_group.name, t_group.shared,
							t_group.sundayEnabled, t_group.mondayEnabled,
							t_group.tuesdayEnabled, t_group.wednesdayEnabled,
							t_group.thursdayEnabled, t_group.fridayEnabled,
							t_group.saturdayEnabled,
							t_user.id, t_user.name, t_user.email, t_user.admin,
							t_group.id.count().castToNum(Integer.class).as(t_searchGroup.googleSearchId),
							t_target.id)
					.from(t_group)
					.leftJoin(t_user).on(t_group.ownerId.eq(t_user.id))
					.leftJoin(t_searchGroup).on(t_group.id.eq(t_searchGroup.groupId))
					.leftJoin(subQuery, t_target).on(t_group.id.eq(t_target.groupId))
					.groupBy(t_group.id, t_target.id)
					.orderBy(t_group.name.asc());
            
            if(module != null){
                query.where(t_group.moduleId.eq(module.ordinal()));
            }
            
            if (dayOfWeek != null) {
            	switch(dayOfWeek) {
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

            for (Tuple tuple : tuples) {
                groups.add(fromTuple(tuple));
            }
            
        }catch(Exception ex){
            LOG.error("SQLError ex", ex);
        }        
        
        return groups;
    }
    
    public List<Group> listForUser(User user, boolean shared, boolean admin){
        List<Group> groups = new ArrayList<Group>();
        try(Connection con = ds.getConnection()){

			SQLQuery<Tuple> query1 = new SQLQuery<Void>(con, dbTplConf)
					.select(t_group.id, t_group.moduleId, t_group.name, t_group.shared,
							t_group.sundayEnabled, t_group.mondayEnabled,
							t_group.tuesdayEnabled, t_group.wednesdayEnabled,
							t_group.thursdayEnabled, t_group.fridayEnabled,
							t_group.saturdayEnabled,
							t_user.id, t_user.name, t_user.email, t_user.admin)
					.from(t_group)
					.leftJoin(t_user).on(t_group.ownerId.eq(t_user.id));;
            
            if(user != null && !admin){
                query1.leftJoin(t_user_group).on(t_user_group.groupId.eq((t_group.id)));
                if (shared) {
                	query1.where(t_group.shared.isTrue()
                    		.or(t_user_group.userId.eq(user.getId())));
                } else {
                    query1.where((t_group.ownerId.eq(user.getId())));
                }
            }
            
            Map<Integer, Group> map = new HashMap<>();
            List<Tuple> tuples = query1.fetch();
            for (Tuple tuple : tuples) {
            	Group group = fromTuple(tuple);
            	map.put(group.getId(), group);
            }

            SQLQuery<Tuple> query2 = new SQLQuery<Void>(con, dbTplConf)
            		.select(t_target.groupId, t_target.id.count().castToNum(Integer.class).as(t_target.id))
					.from(t_target).groupBy(t_target.groupId)
					.where(t_target.groupId.in(map.keySet()));
            
            tuples = query2.fetch();
            for (Tuple tuple : tuples) {
            	Integer groupId = tuple.get(t_target.groupId);
            	map.get(groupId).setTargets(tuple.get(t_target.id));
            } 
            
            SQLQuery<Tuple> query3 = new SQLQuery<Void>(con, dbTplConf)
            		.select(t_searchGroup.groupId, t_searchGroup.googleSearchId.count().castToNum(Integer.class).as(t_searchGroup.googleSearchId))
					.from(t_searchGroup).groupBy(t_searchGroup.groupId)
					.where(t_searchGroup.groupId.in(map.keySet()));
            
            tuples = query3.fetch();
            for (Tuple tuple : tuples) {
            	Integer groupId = tuple.get(t_searchGroup.groupId);
            	map.get(groupId).setSearches(tuple.get(t_searchGroup.googleSearchId));
            } 

            groups.addAll(map.values());
            groups.sort((x, y) -> x.getName().compareTo(y.getName()));

        }catch(Exception ex){
            LOG.error("SQLError ex", ex);
        }        
        
        return groups;
    }
    
    public Group find(int groupId){
        Group group = null;
        try(Connection con = ds.getConnection()){
            
        	SubQueryExpression<Tuple> subQuery = SQLExpressions
					.select(t_target.groupId, t_target.groupId.count().castToNum(Integer.class).as(t_target.id))
					.from(t_target).where(t_target.groupId.eq(groupId));

			SQLQuery<Tuple> query = new SQLQuery<Void>(con, dbTplConf)
					.select(t_group.id, t_group.moduleId, t_group.name, t_group.shared,
							t_group.sundayEnabled, t_group.mondayEnabled,
							t_group.tuesdayEnabled, t_group.wednesdayEnabled,
							t_group.thursdayEnabled, t_group.fridayEnabled,
							t_group.saturdayEnabled,
							t_user.id, t_user.name, t_user.email, t_user.admin,
							t_group.id.count().castToNum(Integer.class).as(t_searchGroup.googleSearchId), t_target.id)
					.from(t_group)
					.leftJoin(t_user).on(t_group.ownerId.eq(t_user.id))
					.leftJoin(t_searchGroup).on(t_group.id.eq(groupId).and(t_group.id.eq(t_searchGroup.groupId)))
					.leftJoin(subQuery, t_target).on(t_group.id.eq(groupId).and(t_group.id.eq(t_target.groupId)))
					.where(t_group.id.eq(groupId))
					.groupBy(t_group.id, t_target.id);

			Tuple tuple = query.fetchFirst();

            group = fromTuple(tuple);
            
        }catch(Exception ex){
            LOG.error("SQLError ex", ex);
        }        
        
        return group;
    }
    
    public Group findByName(String name){
        Group group = null;
        try(Connection con = ds.getConnection()){
            
        	SubQueryExpression<Tuple> subQuery = SQLExpressions
					.select(t_target.groupId, t_target.groupId.count().castToNum(Integer.class).as(t_target.id))
					.from(t_target)
					.leftJoin(t_group).on(t_target.groupId.eq(t_group.id).and(t_group.name.eq(name)))
					.groupBy(t_target.groupId);

			SQLQuery<Tuple> query = new SQLQuery<Void>(con, dbTplConf)
					.select(t_group.id, t_group.moduleId, t_group.name, t_group.shared,
							t_group.sundayEnabled, t_group.mondayEnabled,
							t_group.tuesdayEnabled, t_group.wednesdayEnabled,
							t_group.thursdayEnabled, t_group.fridayEnabled,
							t_group.saturdayEnabled,
							t_user.id, t_user.name, t_user.email, t_user.admin,
							t_group.id.count().castToNum(Integer.class).as(t_searchGroup.googleSearchId), t_target.id)
					.from(t_group)
					.leftJoin(t_user).on(t_group.ownerId.eq(t_user.id))
					.leftJoin(t_searchGroup).on(t_group.name.eq(name).and(t_group.id.eq(t_searchGroup.groupId)))
					.leftJoin(subQuery, t_target).on(t_group.name.eq(name).and(t_group.id.eq(t_target.groupId)))
					.where(t_group.name.eq(name))
					.groupBy(t_group.id, t_target.id);

			Tuple tuple = query.fetchFirst();

            group = fromTuple(tuple);
            
        }catch(Exception ex){
            LOG.error("SQLError ex", ex);
        }        
        
        return group;
    }

    Group fromTuple(Tuple tuple){
        if(tuple == null){
            return null;
        }
        
        Group group = new Group(
            tuple.get(t_group.id), 
            Group.Module.values()[tuple.get(t_group.moduleId)], 
            tuple.get(t_group.name));
		group.setShared(tuple.get(t_group.shared) == null ? false : tuple.get(t_group.shared));
		group.setSundayEnabled(tuple.get(t_group.sundayEnabled) == null ? false : tuple.get(t_group.sundayEnabled));
		group.setMondayEnabled(tuple.get(t_group.mondayEnabled) == null ? false : tuple.get(t_group.mondayEnabled));
		group.setTuesdayEnabled(tuple.get(t_group.tuesdayEnabled) == null ? false : tuple.get(t_group.tuesdayEnabled));
		group.setWednesdayEnabled(tuple.get(t_group.wednesdayEnabled) == null ? false : tuple.get(t_group.wednesdayEnabled));
		group.setThursdayEnabled(tuple.get(t_group.thursdayEnabled) == null ? false : tuple.get(t_group.thursdayEnabled));
		group.setFridayEnabled(tuple.get(t_group.fridayEnabled) == null ? false : tuple.get(t_group.fridayEnabled));
		group.setSaturdayEnabled(tuple.get(t_group.saturdayEnabled) == null ? false : tuple.get(t_group.saturdayEnabled));
		group.setTargets(tuple.get(t_target.id));
		group.setSearches(tuple.get(t_searchGroup.googleSearchId));
		
		if (tuple.get(t_user.id) != null) {
			User user = new User();
			user.setId(tuple.get(t_user.id));
			user.setName(tuple.get(t_user.name));
			user.setEmail(tuple.get(t_user.email));
			user.setAdmin(tuple.get(t_user.admin));
			group.setOwner(user);
		}
		
        return group;
    }
    
}
