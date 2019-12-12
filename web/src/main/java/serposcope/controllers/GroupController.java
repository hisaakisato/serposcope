/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package serposcope.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.serphacker.serposcope.db.base.BaseDB;
import com.serphacker.serposcope.db.google.GoogleDB;
import com.serphacker.serposcope.models.base.Group;
import com.serphacker.serposcope.models.base.Group.Module;
import com.serphacker.serposcope.models.base.User;
import conf.SerposcopeConf;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import ninja.Context;
import ninja.FilterWith;
import ninja.Result;
import ninja.Results;
import ninja.Router;
import ninja.params.Param;
import ninja.session.FlashScope;
import org.apache.commons.lang3.StringEscapeUtils;
import serposcope.controllers.google.GoogleGroupController;
import serposcope.filters.AdminFilter;
import serposcope.filters.AuthFilter;
import serposcope.filters.XSRFFilter;

@Singleton
@FilterWith(AuthFilter.class)
public class GroupController extends BaseController {
    
    @Inject
    SerposcopeConf conf;
    
    @Inject
    Router router;
    
    @Inject
    BaseDB baseDB;
    
    @Inject
    GoogleDB googleDB;

    public Result myGroups(Context context) throws JsonProcessingException{
        User user = context.getAttribute("user", User.class);
        long count = googleDB.search.countForUser(user);
        List<User> users = baseDB.user.list();
        List<Group> groups = baseDB.group.listForUser(user, false);
        return Results
            .ok()
    		.template("serposcope/views/GroupController/groups.ftl.html")
            .render("currentUser", user)
            .render("users", users)
            .render("groups", groups)
            .render("search_count", count)
            .render("h2warning", count > 2000 && conf.dbUrl != null && conf.dbUrl.contains(":h2:"))
            ;
    }
    
    public Result groups(Context context) throws JsonProcessingException{
        long count = googleDB.search.count();
        User currentUser = context.getAttribute("user", User.class);
        List<User> users = baseDB.user.list();
        return Results
            .ok()
            .render("currentUser", currentUser)
            .render("users", users)
            .render("groups", context.getAttribute("groups"))
            .render("search_count", count)
            .render("h2warning", count > 2000 && conf.dbUrl != null && conf.dbUrl.contains(":h2:"))
            ;
    }
    
    public Result jsonSuggest(
        Context context,
        @Param("query") String query
    ){
        List<Group> groups = (List<Group>) context.getAttribute("groups");
        
        StringBuilder builder = new StringBuilder("[");
        groups.stream()
            .filter((Group g) -> query == null ? true : g.getName().contains(query))
            .sorted((Group o1, Group o2) -> o1.getId() - o2.getId())
            .limit(10)
            .forEach((g) -> {
                builder.append("{")
                    .append("\"id\":").append(g.getId()).append(",")
                    .append("\"name\":\"").append(StringEscapeUtils.escapeJson(g.getName())).append("\",")
                    .append("\"module\":\"").append(g.getModule()).append("\"")
                    .append("},");
            });
        if(builder.length() > 1){
            builder.deleteCharAt(builder.length()-1);
        }
        builder.append("]");
        
        return Results.json().renderRaw(builder.toString());
    }
    
    @FilterWith({
        XSRFFilter.class
    })
    public Result create(
        Context context,
        @Param("name") String name,
        @Param("shared") boolean shared,
        @Param("sundayEnabled") boolean sundayEnabled,
        @Param("mondayEnabled") boolean mondayEnabled,
        @Param("tuesdayEnabled") boolean tuesdayEnabled,
        @Param("wednesdayEnabled") boolean wednesdayEnabled,
        @Param("thursdayEnabled") boolean thursdayEnabled,
        @Param("fridayEnabled") boolean fridayEnabled,
        @Param("saturdayEnabled") boolean saturdayEnabled,
        @Param("module") Integer moduleNum
    ){
        FlashScope flash = context.getFlashScope();
        Module module = null; 
        
        if(name == null || name.isEmpty()){
            flash.error("error.invalidName");
            return Results.redirect(router.getReverseRoute(GroupController.class, "myGroups"));
        }
        if (baseDB.group.findByName(name) != null) {
            flash.error("google.group.alreadyExists");
            return Results.redirect(router.getReverseRoute(GroupController.class, "myGroups"));
        }
        
        try {
            module = Module.values()[moduleNum];
        }catch(Exception ex){
            flash.error("error.invalidModule");
            return Results.redirect(router.getReverseRoute(GroupController.class, "myGroups"));            
        }
        
        User owner = context.getAttribute("user", User.class);
        Group group = new Group(module, name);
        group.setShared(shared);
        group.setSundayEnabled(sundayEnabled);
        group.setMondayEnabled(mondayEnabled);
        group.setTuesdayEnabled(tuesdayEnabled);
        group.setWednesdayEnabled(wednesdayEnabled);
        group.setThursdayEnabled(thursdayEnabled);
        group.setFridayEnabled(fridayEnabled);
        group.setSaturdayEnabled(saturdayEnabled);
        group.setOwner(owner);
        baseDB.group.insert(group);
        baseDB.user.addPerm(owner, group);
        
        flash.success("home.groupCreated");
        switch(group.getModule()){
            case GOOGLE:
                return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()));  
            default:
                return Results.redirect(router.getReverseRoute(GroupController.class, "myGroups"));
        }
        
        
        
    }
    
}
