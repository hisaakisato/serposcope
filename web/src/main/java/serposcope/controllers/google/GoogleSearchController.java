/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package serposcope.controllers.google;

import static com.serphacker.serposcope.db.base.RunDB.STATUSES_DONE;
import static com.serphacker.serposcope.models.google.GoogleRank.UNRANKED;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.serphacker.serposcope.db.base.BaseDB;
import com.serphacker.serposcope.db.google.GoogleDB;
import com.serphacker.serposcope.models.base.Config;
import com.serphacker.serposcope.models.base.Group;
import com.serphacker.serposcope.models.base.Group.Module;
import com.serphacker.serposcope.models.base.Run;
import com.serphacker.serposcope.models.base.Run.Status;
import com.serphacker.serposcope.models.google.GoogleBest;
import com.serphacker.serposcope.models.google.GoogleRank;
import com.serphacker.serposcope.models.google.GoogleSearch;
import com.serphacker.serposcope.models.google.GoogleSerp;
import com.serphacker.serposcope.models.google.GoogleSerpEntry;
import com.serphacker.serposcope.models.google.GoogleTarget;
import com.serphacker.serposcope.scraper.google.GoogleDevice;
import com.serphacker.serposcope.scraper.utils.S3Utilis;

import ninja.Context;
import ninja.Result;
import ninja.Results;
import ninja.Router;
import ninja.i18n.Messages;
import ninja.params.Param;
import ninja.params.Params;
import ninja.params.PathParam;
import ninja.session.FlashScope;
import ninja.utils.ResponseStreams;
import serposcope.filters.AbstractFilter;
import serposcope.filters.CanReadFilter;

@Singleton
public class GoogleSearchController extends GoogleController {

	private static final Logger LOG = LoggerFactory.getLogger(GoogleSearchController.class);

	@Inject
	Messages messages;

	@Inject
	GoogleDB googleDB;

	@Inject
	BaseDB baseDB;

	@Inject
	Router router;

	@Inject
	Messages msg;

	@Inject
	ObjectMapper objectMapper;

	public Result search(Context context, @PathParam("searchId") Integer searchId,
			@Param("startDate") String startDateStr, @Param("endDate") String endDateStr) {
		GoogleSearch search = getSearch(context, searchId);
		Group group = context.getAttribute("group", Group.class);

		if (search == null) {
			context.getFlashScope().error("error.invalidSearch");
			return Results
					.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()));
		}

		Run minRun = baseDB.run.findFirst(Module.GOOGLE, STATUSES_DONE, null, group, null, Arrays.asList(searchId),
				null);
		Run maxRun = baseDB.run.findLast(Module.GOOGLE, STATUSES_DONE, null, group, null, Arrays.asList(searchId),
				null);
		if (maxRun == null || minRun == null) {
			return Results.ok().render("search", search);
		}

		LocalDate minDay = minRun.getDay();
		LocalDate maxDay = maxRun.getDay();

		LocalDate startDate = null;
		if (startDateStr != null) {
			try {
				startDate = LocalDate.parse(startDateStr);
			} catch (Exception ex) {
			}
		}
		LocalDate endDate = null;
		if (endDateStr != null) {
			try {
				endDate = LocalDate.parse(endDateStr);
			} catch (Exception ex) {
			}
		}

		if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
			startDate = maxDay.minusDays(30);
			endDate = maxDay;
		}

		Run firstRun = baseDB.run.findFirst(Module.GOOGLE, STATUSES_DONE, startDate, group, null,
				Arrays.asList(searchId), null);
		Run lastRun = baseDB.run.findLast(Module.GOOGLE, STATUSES_DONE, endDate, group, null, Arrays.asList(searchId),
				null);

		if (firstRun == null || lastRun == null || firstRun.getDay().isAfter(lastRun.getDay())) {
			return Results.ok()
					.render("f_warning", msg.get("error.noDataForThisPeriod", context, Optional.absent()).or(""))
					.render("startDate", startDate).render("endDate", endDate).render("minDate", minDay)
					.render("maxDate", maxDay).render("search", search);
		}

		startDate = firstRun.getDay();
		endDate = lastRun.getDay();

		String jsonEvents = null;
		try {
			jsonEvents = objectMapper.writeValueAsString(baseDB.event.list(group, startDate, endDate));
		} catch (JsonProcessingException ex) {
			jsonEvents = "[]";
		}

		GoogleSerp lastSerp = googleDB.serp.get(lastRun.getId(), search.getId());

		List<GoogleTarget> targets = getTargets(context);

		Map<Integer, GoogleBest> bestRankings = new HashMap<>();
		for (GoogleTarget target : targets) {
			GoogleBest best = googleDB.rank.getBest(target.getGroupId(), target.getId(), search.getId());
			if (best != null) {
				bestRankings.put(best.getGoogleTargetId(), best);
			}
		}

		String jsonRanks = getJsonRanks(group, targets, firstRun, lastRun, searchId);
		Config config = baseDB.config.getConfig();

		return Results.ok().render("displayMode", config.getDisplayGoogleSearch()).render("events", jsonEvents)
				.render("targets", targets).render("chart", jsonRanks).render("search", search).render("serp", lastSerp)
				.render("startDate", startDate).render("endDate", endDate).render("minDate", minDay)
				.render("maxDate", maxDay).render("bestRankings", bestRankings);
	}

	protected String getJsonRanks(Group group, List<GoogleTarget> targets, Run firstRun, Run lastRun, int searchId) {

		StringBuilder builder = new StringBuilder("{\"targets\":[");
		for (GoogleTarget target : targets) {
			builder.append("{\"id\":").append(target.getId()).append(",\"name\":\"")
					.append(StringEscapeUtils.escapeJson(target.getName())).append("\"},");
		}
		if (builder.charAt(builder.length() - 1) == ',') {
			builder.setCharAt(builder.length() - 1, ']');
		} else {
			builder.append(']');
		}
		LocalDate firstDate = firstRun.getDay();
		builder.append(",\"ranks\":[");

		final int[] maxRank = new int[1];

		AtomicInteger ai = new AtomicInteger(0);
		googleDB.serp.stream(firstRun.getDay(), lastRun.getDay(), group, Arrays.asList(searchId), (GoogleSerp serp) -> {

			while (firstDate.plusDays(ai.get()).isBefore(serp.getRunDay().toLocalDate())) {
				builder.append('[').append(ai.getAndIncrement()).append(',');
				// calendar
				builder.append("null").append(",");
				for (int i = 0; i < targets.size(); i++) {
					builder.append("null,");
				}
				if (builder.charAt(builder.length() - 1) == ',') {
					builder.setCharAt(builder.length() - 1, ']');
				}
				builder.append(',');
			}
			builder.append('[').append(ai.getAndIncrement()).append(',');

			// calendar
			builder.append("null").append(",");

			for (GoogleTarget target : targets) {
				int position = UNRANKED;
				for (int i = 0; i < serp.getEntries().size(); i++) {
					if (target.match(serp.getEntries().get(i).getUrl())) {
						position = i + 1;
						break;
					}
				}

				builder.append(position == UNRANKED ? "NaN" : position).append(',');
				if (position != UNRANKED && position > maxRank[0]) {
					maxRank[0] = position;
				}
			}

			if (builder.charAt(builder.length() - 1) == ',') {
				builder.setCharAt(builder.length() - 1, ']');
			}
			builder.append(',');
		});
		if (builder.charAt(builder.length() - 1) == ',') {
			builder.setCharAt(builder.length() - 1, ']');
		} else {
			builder.append(']');
		}

		builder.append(",\"maxRank\":").append(maxRank[0]);
		builder.append("}");

		return builder.toString();
	}

	public Result urlRanks(Context context, @PathParam("searchId") Integer searchId, @Param("url") String url,
			@Param("startDate") String startDateStr, @Param("endDate") String endDateStr) {
		Group group = (Group) context.getAttribute("group");

		GoogleSearch search = getSearch(context, searchId);
		if (search == null) {
			context.getFlashScope().error("error.invalidSearch");
			return Results
					.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()));
		}

		LocalDate startDate = null;
		if (startDateStr != null) {
			try {
				startDate = LocalDate.parse(startDateStr);
			} catch (Exception ex) {
			}
		}
		LocalDate endDate = null;
		if (endDateStr != null) {
			try {
				endDate = LocalDate.parse(endDateStr);
			} catch (Exception ex) {
			}
		}

		Run firstRun = baseDB.run.findFirst(Module.GOOGLE, STATUSES_DONE, startDate, group, null,
				Arrays.asList(searchId), null);
		Run lastRun = baseDB.run.findLast(Module.GOOGLE, STATUSES_DONE, endDate, group, null, Arrays.asList(searchId),
				null);

		if (url == null || firstRun == null || lastRun == null) {
			return Results.badRequest().text();
		}

		StringBuilder builder = new StringBuilder("{");
		googleDB.serp.stream(firstRun.getId(), lastRun.getId(), search.getId(), (GoogleSerp t) -> {
			int position = 0;
			for (int i = 0; i < t.getEntries().size(); i++) {
				if (t.getEntries().get(i).getUrl().equals(url)) {
					position = i + 1;
					break;
				}
			}

			builder.append("\"").append(t.getRunDay().toEpochSecond(ZoneOffset.UTC) * 1000l).append("\":")
					.append(position).append(",");
		});

		if (builder.charAt(builder.length() - 1) == ',') {
			builder.setCharAt(builder.length() - 1, '}');
		} else {
			builder.append('}');
		}

		return Results.ok().text().render(builder.toString());
	}

	public Result showSerp(Context context, @PathParam("searchId") Integer searchId, @Param("date") String pdate,
			@Param("rank") int rank) {
		GoogleSerp serp = null;
		GoogleSearch search = null;
		final LocalDate date;
		try {
			date = LocalDate.parse(pdate);
		} catch (Exception ex) {
			return Results.badRequest().text().renderRaw("Invalid date");
		}
		Group group = context.getAttribute("group", Group.class);
		Run run = baseDB.run.findLast(Module.GOOGLE,
				Arrays.asList(Status.DONE_SUCCESS, Status.DONE_WITH_ERROR, Status.DONE_ABORTED, Status.DONE_CRASHED),
				date, group, null, Arrays.asList(searchId), null);
		if (run != null) {
			search = getSearch(context, searchId);
			if (search != null) {
				serp = googleDB.serp.get(run.getId(), search.getId());
			}
		}

		if (serp == null) {
			return Results.notFound().text().renderRaw("SERP not found");
		}
		try {
			int page = 0; // FIXME
			context.setAttribute(AbstractFilter.SUPPRESS_EXTRA_RENDER, true);
			return Results.html().render((Context ctx, Result result) -> {
				ResponseStreams stream = ctx.finalizeHeaders(result);
				try (OutputStream out = stream.getOutputStream()) {
					S3Utilis.download(out, date, searchId, page);
				} catch (IOException e) {
					LOG.error("error while downloading serp", e);
				}
			});
		} catch (AmazonS3Exception e) {
			if (e.getStatusCode() == 404) {
				return Results.notFound().text().renderRaw("SERP not found");
			}
		}
		return Results.internalServerError();
	}

	public Result exportSerp(Context context, @PathParam("searchId") Integer searchId, @Param("date") String pdate) {
		GoogleSerp serp = null;
		GoogleSearch search = null;
		LocalDate date = null;
		try {
			date = LocalDate.parse(pdate);
		} catch (Exception ex) {
		}
		if (date != null) {
			List<Run> runs = baseDB.run.findByDay(Module.GOOGLE, date);
			if (!runs.isEmpty()) {
				search = getSearch(context, searchId);
				if (search != null) {
					serp = googleDB.serp.get(runs.get(0).getId(), search.getId());
				}
			}
		}

		if (serp == null) {
			return Results.ok().text().renderRaw("SERP not found");
		}

		boolean exportRank = context.getParameter("rank") != null;
		boolean exportD1 = context.getParameter("d1") != null;
		boolean exportD7 = context.getParameter("d7") != null;
		boolean exportD30 = context.getParameter("d30") != null;
		boolean exportD90 = context.getParameter("d90") != null;

		int position = 0;
		StringBuilder builder = new StringBuilder();
		for (GoogleSerpEntry entry : serp.getEntries()) {
			++position;
			if (exportRank) {
				builder.append(position).append(",");
			}
			builder.append(StringEscapeUtils.escapeCsv(entry.getUrl())).append(",");
			if (exportD1) {
				Short rank = entry.getMap().getOrDefault((short) 1, (short) GoogleRank.UNRANKED);
				builder.append(rank != GoogleRank.UNRANKED ? rank.intValue() : "").append(",");
			}
			if (exportD7) {
				Short rank = entry.getMap().getOrDefault((short) 7, (short) GoogleRank.UNRANKED);
				builder.append(rank != GoogleRank.UNRANKED ? rank.intValue() : "").append(",");
			}
			if (exportD30) {
				Short rank = entry.getMap().getOrDefault((short) 30, (short) GoogleRank.UNRANKED);
				builder.append(rank != GoogleRank.UNRANKED ? rank.intValue() : "").append(",");
			}
			if (exportD90) {
				Short rank = entry.getMap().getOrDefault((short) 90, (short) GoogleRank.UNRANKED);
				builder.append(rank != GoogleRank.UNRANKED ? rank.intValue() : "").append(",");
			}
			if (builder.length() > 0) {
				builder.setCharAt(builder.length() - 1, '\n');
			}
		}

		Group group = context.getAttribute("group", Group.class);
		try {
			byte[] bom = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
			byte[] bytes = builder.toString().getBytes("UTF-8");
			byte[] result = new byte[bom.length + bytes.length];
			System.arraycopy(bom, 0, result, 0, bom.length);
			System.arraycopy(bytes, 0, result, bom.length, bytes.length);

			String localDate = serp.getRunDay().toLocalDate().toString();
			String encodedFilename = String.format("ranks_%s_%s(%s).csv", localDate,
					URLEncoder.encode(group.getName(), "UTF-8"), URLEncoder.encode(search.getKeyword(), "UTF-8"));
			return Results
					.ok().contentType(Result.APPLICATION_OCTET_STREAM).addHeader("Content-Disposition", "attachment; "
							+ "filename=\"ranks_" + localDate + ".csv\"; " + "filename*=\"UTF-8''" + encodedFilename)
					.renderRaw(result);
		} catch (UnsupportedEncodingException e) {
		}
		return Results.internalServerError();
	}

	public Result export(Context context, @Params("searchIds") String[] ids, @Param("targetOnly") boolean targetOnly,
			@Param("startDate") String start, @Param("endDate") String end) {

		if (ids == null || ids.length == 0) {
			FlashScope flash = context.getFlashScope();
			Group group = context.getAttribute("group", Group.class);
			flash.error("error.noSearchSelected");
			return Results
					.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId())
							+ "#tab-searches");
		}

		LocalDate startDate = start == null || start.isEmpty() ? LocalDate.now()
				: LocalDate.parse(start, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		LocalDate endDate = end == null || end.isEmpty() ? LocalDate.now()
				: LocalDate.parse(end, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		List<Integer> searchIds = Arrays.stream(ids).map(Integer::parseInt).collect(Collectors.toList());
		Group group = context.getAttribute("group", Group.class);
		List<GoogleTarget> targets = googleDB.target.list(Arrays.asList(group.getId()));

		return Results.ok().text().addHeader("Content-Disposition", "attachment; filename=\"serps.csv\"")
				.render((Context ctx, Result result) -> {
					ResponseStreams stream = ctx.finalizeHeaders(result);
					DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd");
					try (OutputStream out = stream.getOutputStream();
							Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
						// BOM
						byte[] bom = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
						out.write(bom);
						// Header
						writer.append(
								messages.get("google.search.exportHeader", Optional.of(ctx.getAcceptLanguage())).get())
								.append("\n");
						// SERP
						LocalDate date = startDate;
						while (!date.isAfter(endDate)) {
							googleDB.serp.stream(date, date, group, searchIds, serp -> {
								try {
									GoogleSearch search = serp.getSearch();
									if (search == null) {
										return;
									}
									String dateString = serp.getRunDay().toLocalDate().format(dtf);
									StringBuilder sb = new StringBuilder();
									sb.append(search.getDevice() == GoogleDevice.DESKTOP ? "PC" : "SP").append(",");
									sb.append("\"").append(search.getCountry()).append("\",");
									sb.append("\"").append(search.getLocal()).append("\",");
									sb.append("\"").append(search.getCustomParameters()).append("\"\n");
									String tailer = sb.toString();
									List<GoogleSerpEntry> entries = serp.getEntries();
									List<GoogleTarget> founds = new ArrayList<>();
									for (int i = 0; i < entries.size(); i++) {
										GoogleSerpEntry entry = entries.get(i);
										String targetName = null;
										for (GoogleTarget target : targets) {
											if (target.match(entry.getUrl())) {
												founds.add(target);
												targetName = target.getName();
												break;
											}
										}
										if (targetOnly && targetName == null) {
											continue;
										}
										writer.append(dateString).append(",");
										writer.append("\"").append(search.getKeyword()).append("\",");
										writer.append(String.valueOf(i + 1)).append(",");
										writer.append("\"").append(entry.getUnicodeUrl()).append("\",");
										writer.append("\"").append(targetName == null ? "" : targetName).append("\",");
										writer.append(tailer);
									}
									// check out of ranks
									for (GoogleTarget target : targets) {
										if (!founds.contains(target)) {
											writer.append(dateString).append(",");
											writer.append("\"").append(search.getKeyword()).append("\",-,,");
											writer.append(target.getName()).append(",");
											writer.append(tailer);
										}
									}
									writer.flush();
								} catch (IOException e) {
									LOG.error("error while exporting csv", e);
								}
							});
							date = date.plusDays(1);
						}
					} catch (IOException e) {
						LOG.error("error while exporting csv", e);
					}
				});
	}
}