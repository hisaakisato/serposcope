/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package serposcope.controllers.admin;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.serphacker.serposcope.db.base.BaseDB;
import com.serphacker.serposcope.models.base.Group;
import com.serphacker.serposcope.models.base.User;
import java.util.List;
import ninja.AuthenticityFilter;
import ninja.Context;
import ninja.FilterWith;
import ninja.Result;
import ninja.Results;
import ninja.Router;
import ninja.i18n.Messages;
import ninja.params.Param;
import ninja.params.PathParam;
import ninja.session.FlashScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import serposcope.controllers.BaseController;
import serposcope.filters.AdminFilter;
import serposcope.filters.XSRFFilter;
import serposcope.helpers.Validator;

@Singleton
public class UsersController extends BaseController {
    
    private static final Logger LOG = LoggerFactory.getLogger(UsersController.class);
    
    @Inject
    Messages msg;
    
    @Inject
    BaseDB baseDB;
    
    @Inject
    Router router;
    
    @FilterWith(AdminFilter.class)
    public Result users(){
        
        List<User> users = baseDB.user.list();
        List<Group> groups = baseDB.group.list();
        
        return Results
            .ok()
            .render("users", users)
            .render("groups", groups)
            ;
    }
    
    @FilterWith({AdminFilter.class, XSRFFilter.class})
    public Result add(
        Context context,
        @Param("name") String name,
        @Param("email") String email,
        @Param("email-confirm") String emailConfirm,
        @Param("password") String password,
        @Param("password-confirm") String passwordConfirm,        
        @Param("admin") String admin
    ){
        FlashScope flash = context.getFlashScope();
        
        if (!Validator.isEmailAddress(email)) {
            flash.error("error.invalidEmail");
            return Results.redirect(router.getReverseRoute(UsersController.class, "users"));
        }

        if (!email.equals(emailConfirm)) {
            flash.error("error.invalidEmailConfirm");
            return Results.redirect(router.getReverseRoute(UsersController.class, "users"));
        }
        
        if(baseDB.user.findByEmail(email) != null){
            flash.error("admin.users.emailAlreadyExists");
            return Results.redirect(router.getReverseRoute(UsersController.class, "users"));            
        }

        if (password == null || password.length() < 6) {
            flash.error("error.invalidPassword");
            return Results.redirect(router.getReverseRoute(UsersController.class, "users"));
        }

        if (!password.equals(passwordConfirm)) {
            flash.error("error.invalidPasswordConfirm");
            return Results.redirect(router.getReverseRoute(UsersController.class, "users"));
        }

        try {
            User user = new User();
            user.setEmail(email);
            user.setName(name);
            user.setPassword(password);
            user.setAdmin("on".equals(admin));
            if (baseDB.user.insert(user) == -1) {
                LOG.error("can't insert user in database");
                flash.error("error.internalError");
                return Results.redirect(router.getReverseRoute(UsersController.class, "users"));
            }
        } catch (Exception ex) {
            LOG.error("internal error while saving admin user", ex);
            flash.error("error.internalError");
            return Results.redirect(router.getReverseRoute(UsersController.class, "users"));
        }

        
        return Results.redirect(router.getReverseRoute(UsersController.class, "users"));
    }
    
    @FilterWith({AdminFilter.class, XSRFFilter.class})
    public Result delete(
        Context context,
        @PathParam("userId") Integer userId,
        @Param("email") String email
    ){
        
        FlashScope flash = context.getFlashScope();
        
        User user = null;
        if(userId == null || (user=baseDB.user.findById(userId)) == null) {
            flash.error("admin.users.invalidUserId");
            return Results.redirect(router.getReverseRoute(UsersController.class, "users"));
        }        
        if (email == null || email.isEmpty()) {
            flash.error("admin.users.userEmailMissing");
            return Results.redirect(router.getReverseRoute(UsersController.class, "users"));
        }
        if (!email.equals(user.getEmail())) {
            flash.error("admin.users.userEmailNotMatch");
            return Results.redirect(router.getReverseRoute(UsersController.class, "users"));
        }

        baseDB.user.delPerm(user);
        baseDB.user.delete(user.getId());
        flash.success("admin.users.userDeleted");
        return Results.redirect(router.getReverseRoute(UsersController.class, "users"));
        
    }
    
    @FilterWith({AdminFilter.class, XSRFFilter.class})
    public Result edit(
        Context context,
        @PathParam("userId") Integer userId,
        @Param("name") String name,
        @Param("email") String email,
        @Param("email-confirm") String emailConfirm,
        @Param("password") String password,
        @Param("password-confirm") String passwordConfirm,        
        @Param("admin") String admin
    ){
        
        FlashScope flash = context.getFlashScope();
        
        User user = null;
        try {            
	        if(userId == null || (user=baseDB.user.findById(userId)) == null) {
	            flash.error("admin.users.invalidUserId");
	            return Results.redirect(router.getReverseRoute(UsersController.class, "users"));
	        }
	        if (email != null && !email.isEmpty() && !user.getEmail().contentEquals(email)) {
		        if (!Validator.isEmailAddress(email)) {
		            flash.error("error.invalidEmail");
		            return Results.redirect(router.getReverseRoute(UsersController.class, "users"));
		        }
		        
		        if(baseDB.user.findByEmail(email) != null){
		            flash.error("admin.users.emailAlreadyExists");
		            return Results.redirect(router.getReverseRoute(UsersController.class, "users"));            
		        }
	            user.setEmail(email);
	        }
	
	        if (password != null && !password.isEmpty()) {
		        if (password == null || password.length() < 6) {
		            flash.error("error.invalidPassword");
		            return Results.redirect(router.getReverseRoute(UsersController.class, "users"));
		        }
		
		        if (!password.equals(passwordConfirm)) {
		            flash.error("error.invalidPasswordConfirm");
		            return Results.redirect(router.getReverseRoute(UsersController.class, "users"));
		        }
	            user.setPassword(password);
	        }
	        
	        if (name != null && !name.isEmpty()) {
	        	user.setName(name);
	        }
            user.setAdmin("on".equals(admin));
            if (!baseDB.user.update(user)) {
                LOG.error("can't update user in database");
                flash.error("error.internalError");
                return Results.redirect(router.getReverseRoute(UsersController.class, "users"));
            }
        } catch (Exception ex) {
            LOG.error("internal error while saving user", ex);
            flash.error("error.internalError");
            return Results.redirect(router.getReverseRoute(UsersController.class, "users"));
        }
        
        return Results.redirect(router.getReverseRoute(UsersController.class, "users"));
        
    }

    @FilterWith(XSRFFilter.class)
    public Result setPerm(
        Context context,
        @Param("user-id") Integer userId,
        @Param("group-id") Integer groupId,
        @Param("value") Boolean newValue
    ){
        User user = null;
        Group group = null;
        if(userId == null || (user=baseDB.user.findById(userId)) == null){
            return Results.ok().text().render("error",msg.get("error.invalidUser", context, Optional.absent()).or(""));
        }
        
        if(groupId == null || (group=baseDB.group.find(groupId)) == null){
            return Results.ok().text().render("error",msg.get("error.invalidGroup", context, Optional.absent()).or(""));
        }        
        
        boolean isPermitted = false;
        if (user.isAdmin()) {
        	isPermitted = true;
        } else if (group.getOwner() != null && user.getId() == group.getOwner().getId()){
            	isPermitted = true;
        } else {
        	isPermitted = user.getGroups().contains(group);
        }
        if (isPermitted) {
	        if(Boolean.TRUE.equals(newValue)){
	            baseDB.user.addPerm(user, group);
	        } else {
	            baseDB.user.delPerm(user, group);
	        }
        }        
        
        return Results.ok().json().render("perm", baseDB.user.hasPerm(user, group));
    }
    
    
//    public Result permissions(){
//        return Results.ok();
//    }
    
}
