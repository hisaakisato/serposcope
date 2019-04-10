/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package serposcope.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import conf.Maintenance;
import ninja.Context;
import ninja.FilterChain;
import ninja.Result;
import ninja.Results;
import ninja.Router;
import serposcope.controllers.HomeController;

@Singleton
public class MaintenanceFilter extends AbstractFilter {

	private static final Logger LOG = LoggerFactory.getLogger(MaintenanceFilter.class);

	@Inject
	Router router;

	@Inject
	Maintenance maintenance;

	@Override
	public Result filter(FilterChain filterChain, Context context) {
		if (maintenance.isEnabled()) {
			context.getFlashScope().error("error.underMaintenance");
			return Results.redirect(router.getReverseRoute(HomeController.class, "home"));
		}

		return filterChain.next(context);
	}
}
