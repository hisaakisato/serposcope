/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package serposcope.controllers.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.serphacker.serposcope.db.base.BaseDB;
import com.serphacker.serposcope.db.base.RunDB;
import com.serphacker.serposcope.db.google.GoogleDB;
import com.serphacker.serposcope.models.base.Group;
import static com.serphacker.serposcope.models.base.Group.Module.GOOGLE;
import com.serphacker.serposcope.models.base.Run;
import com.serphacker.serposcope.models.base.User;
import com.serphacker.serposcope.models.base.Run.Mode;
import com.serphacker.serposcope.models.base.Run.Status;
import com.serphacker.serposcope.models.google.GoogleSearch;
import com.serphacker.serposcope.models.google.GoogleTarget;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import ninja.Context;
import ninja.FilterWith;
import ninja.Result;
import ninja.Results;
import ninja.Router;
import ninja.params.Param;
import ninja.session.FlashScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import serposcope.controllers.BaseController;
import serposcope.controllers.GroupController;
import serposcope.filters.AdminFilter;
import serposcope.filters.AuthFilter;
import serposcope.filters.MaintenanceFilter;
import serposcope.filters.XSRFFilter;
import com.serphacker.serposcope.task.TaskManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import ninja.params.PathParam;
import serposcope.controllers.HomeController;
import serposcope.controllers.google.GoogleGroupController;

@Singleton
@FilterWith(AuthFilter.class)
public class TaskController extends BaseController {

    private static final Logger LOG = LoggerFactory.getLogger(TaskController.class);

    @Inject
    TaskManager taskManager;

    @Inject
    GoogleDB googleDB;

    @Inject
    BaseDB baseDB;

    @Inject
    Router router;

    public Result debug() {

        return Results
            .ok();
    }

    public Result tasks(Context context,
        @Param("page") Integer page
    ) {

        List<Run> running = taskManager.listRunningTasks();

        List<Run> waiting = baseDB.run.listByStatus(Arrays.asList(Status.WAITING), null, null, null);

        if (page == null) {
            page = 0;
        }

        long limit = 50;
        long offset = page * limit;

        List<Run> done = baseDB.run.listByStatus(RunDB.STATUSES_DONE, limit, offset, null);

        Integer previousPage = page > 0 ? (page - 1) : null;
        Integer nextPage = done.size() == limit ? (page + 1) : null;

        return Results.ok()
            .render("previousPage", previousPage)
            .render("nextPage", nextPage)
            .render("running", running)
            .render("waiting", waiting)
            .render("done", done);
    }

    @FilterWith({ MaintenanceFilter.class, AdminFilter.class, XSRFFilter.class })
    public Result startTask(
        Context context,
        @Param("module") Integer moduleId,
        @Param("update") Boolean update
    ) {
        FlashScope flash = context.getFlashScope();
//        Module module = Module.getByOrdinal(moduleId);
//        if (module == null || module != Module.GOOGLE) {
//            flash.error("error.invalidModule");
//            return Results.redirect(router.getReverseRoute(TaskController.class, "tasks"));
//        }        

        Run run = null;
        if(Boolean.TRUE.equals(update)){
            run = baseDB.run.findLast(GOOGLE, null, null);
        }
        
        if(run == null){
            run = new Run(Run.Mode.MANUAL, Group.Module.GOOGLE, LocalDateTime.now());
        } else {
            run.setStatus(Run.Status.RUNNING);
            run.setStarted(LocalDateTime.now());            
        }
        
        User user = context.getAttribute("user", User.class);
        if (!taskManager.startGoogleTask(run, user, null)) {
            flash.error("admin.task.errGoogleAlreadyRunning");
            // return Results.redirect(router.getReverseRoute(HomeController.class, "home"));
        	flash.success("admin.task.tasksAccepted");
        } else {
        	flash.success("admin.task.tasksStarted");
        }
        return Results.redirect(router.getReverseRoute(HomeController.class, "home"));
    }

    @FilterWith(XSRFFilter.class)
    public Result abortTask(
        Context context,
        @Param("id") Integer runId
    ) {
        FlashScope flash = context.getFlashScope();
        if (runId == null) {
            flash.error("error.invalidId");
            return Results.redirect(router.getReverseRoute(TaskController.class, "tasks"));
        }

        Run run = baseDB.run.find(runId);
        if (run == null) {
            flash.error("error.invalidRun");
            return Results.redirect(router.getReverseRoute(TaskController.class, "tasks"));
        }

        if (run.getStatus() != Run.Status.RUNNING) {
            flash.error("error.invalidRun");
            return Results.redirect(router.getReverseRoute(TaskController.class, "tasks"));
        }

        switch (run.getModule()) {
            case GOOGLE:
                if (taskManager.abortGoogleTask(true, run.getId())) {
                    flash.success("admin.task.abortingTask");
                } else {
                    flash.error("admin.task.failAbort");
                }
                break;

            default:
                flash.error("error.invalidModule");

        }

//        run.sets
        return Results.redirect(router.getReverseRoute(TaskController.class, "tasks"));
    }

    @FilterWith({ MaintenanceFilter.class, AdminFilter.class, XSRFFilter.class })
    public Result deleteRun(
        Context context,
        @PathParam("runId") Integer runId,
        @Param("id") Integer id
    ) {
        FlashScope flash = context.getFlashScope();

        if (id == null) {
            flash.error("error.invalidId");
            return Results.redirect(router.getReverseRoute(TaskController.class, "tasks"));
        }
        if (!id.equals(runId)) {
            flash.error("error.invalidId");
            return Results.redirect(router.getReverseRoute(TaskController.class, "tasks"));
        }

        Run run = baseDB.run.find(runId);
        if (run == null) {
            flash.error("error.invalidId");
            return Results.redirect(router.getReverseRoute(TaskController.class, "tasks"));
        }
        if (run.getStatus() != Status.WAITING && run.getFinished() == null) {
            flash.error("error.invalidId");
            return Results.redirect(router.getReverseRoute(TaskController.class, "tasks"));
        }

        switch (run.getModule()) {
            case GOOGLE:
                googleDB.targetSummary.deleteByRun(run.getId());
                googleDB.rank.deleteByRunId(run.getId());
                googleDB.serp.deleteByRun(run.getId());
                baseDB.run.delete(run.getId());
                flash.put("warning", "admin.task.googleRunDeleted");
                break;

            default:
                flash.error("error.notImplemented");
                return Results.redirect(router.getReverseRoute(TaskController.class, "tasks"));
        }

        return Results.redirect(router.getReverseRoute(TaskController.class, "tasks"));
    }

    @FilterWith({ MaintenanceFilter.class, AdminFilter.class, XSRFFilter.class })
    public Result retryRun(Context context,
            @PathParam("runId") Integer runId) {
        FlashScope flash = context.getFlashScope();

        Run run = baseDB.run.find(runId);
        if (run == null || run.isRunning()) {
            flash.error("error.invalidId");
            return Results.redirect(router.getReverseRoute(TaskController.class, "tasks"));
        }

        switch (run.getModule()) {
        case GOOGLE:
            break;
        default:
            flash.error("error.notImplemented");
            return Results.redirect(router.getReverseRoute(TaskController.class, "tasks"));
        }

        Group group = run.getGroup();

		if (googleDB.search.listByGroup(group == null ? null : Arrays.asList(group.getId()),
				run.getMode() == Mode.CRON ? run.getDay().getDayOfWeek() : null).size() == 0) {
			flash.error("admin.google.keywordEmpty");
			return Results.redirect(router.getReverseRoute(TaskController.class, "tasks"));
		}

		Run retry = new Run(run.getMode(), run.getModule(), LocalDateTime.now());
		// set previous day and retry status
		retry.setDay(run.getDay());
		retry.setStatus(Status.RETRYING);

        User user = context.getAttribute("user", User.class);
        if (!taskManager.startGoogleTask(retry, user, group)) {
            // flash.error("admin.task.errGoogleAlreadyRunning");
			// return Results.redirect(router.getReverseRoute(TaskController.class, "tasks"));
        	flash.success("admin.task.tasksAccepted");
        } else {
        	flash.success("admin.task.tasksStarted");
        }
		return Results.redirect(router.getReverseRoute(TaskController.class, "tasks"));
    }

    @FilterWith({ MaintenanceFilter.class, AdminFilter.class, XSRFFilter.class })
    public Result rescanSerp(
        Context context,
        @PathParam("runId") Integer runId
    ) {
        FlashScope flash = context.getFlashScope();

        Run run = baseDB.run.find(runId);
        if (run == null || run.getFinished() == null) {
            flash.error("error.invalidId");
            return Results.redirect(router.getReverseRoute(TaskController.class, "tasks"));
        }

		switch (run.getModule()) {
		case GOOGLE:
			// delete google ranks
			googleDB.targetSummary.deleteByRun(run.getId());
			googleDB.rank.deleteByRunId(run.getId());

			Set<Group> groups = new HashSet<>();
			if (run.getGroup() == null) {
				groups.addAll(baseDB.group.list(run.getMode() == Mode.CRON ? run.getDay().getDayOfWeek() : null));
			} else {
				groups.add(run.getGroup());
			}
			for (Group group : groups) {
				List<GoogleTarget> targets = googleDB.target.list(Arrays.asList(group.getId()));
				List<GoogleSearch> searches = googleDB.search.listByGroup(Arrays.asList(group.getId()));
				taskManager.rescan(run.getId(), group, targets, searches, true);
			}

			flash.success("admin.task.serpRescanDone");
			break;

		default:
			flash.error("error.notImplemented");
			return Results.redirect(router.getReverseRoute(TaskController.class, "tasks"));
		}

        return Results.redirect(router.getReverseRoute(TaskController.class, "tasks"));
    }

}
