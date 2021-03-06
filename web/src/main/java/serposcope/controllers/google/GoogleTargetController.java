/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package serposcope.controllers.google;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import ninja.Result;
import ninja.Results;

import com.google.inject.Singleton;
import com.serphacker.serposcope.db.base.BaseDB;
import com.serphacker.serposcope.db.base.RunDB;
import com.serphacker.serposcope.db.google.GoogleDB;
import com.serphacker.serposcope.models.base.Config;
import com.serphacker.serposcope.models.base.Event;
import com.serphacker.serposcope.models.base.Group;
import com.serphacker.serposcope.models.base.Run;
import com.serphacker.serposcope.models.google.GoogleBest;
import com.serphacker.serposcope.models.google.GoogleRank;
import static com.serphacker.serposcope.models.google.GoogleRank.UNRANKED;
import com.serphacker.serposcope.models.google.GoogleSearch;
import com.serphacker.serposcope.models.google.GoogleSerp;
import com.serphacker.serposcope.models.google.GoogleTarget;
import com.serphacker.serposcope.scraper.google.GoogleDevice;

import static com.serphacker.serposcope.scraper.google.GoogleDevice.SMARTPHONE;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;
import ninja.Context;
import ninja.FilterWith;
import ninja.Router;
import ninja.i18n.Messages;
import ninja.params.Param;
import ninja.params.PathParam;
import ninja.utils.ResponseStreams;
import serposcope.filters.MaintenanceFilter;
import serposcope.filters.XSRFFilter;

import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.LoggerFactory;

@Singleton
public class GoogleTargetController extends GoogleController {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(GoogleTargetController.class);

	@Inject
	Messages messages;

    @Inject
    BaseDB baseDB;

    @Inject
    GoogleDB googleDB;

    @Inject
    Router router;

    @Inject
    ObjectMapper objectMapper;

    private final static DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    public static class TargetVariation {

        public TargetVariation(GoogleSearch search, GoogleRank rank) {
            this.search = search;
            this.rank = rank;
        }

        public final GoogleSearch search;
        public final GoogleRank rank;

    }

    public static class TargetRank {

        public TargetRank(int now, int prev, String url) {
            this.now = now;
            this.prev = prev;
            this.url = url;
        }

        public final int now;
        public final int prev;
        public final String url;

        public String getRank() {
            if (now == UNRANKED) {
                return "-";
            }
            return Integer.toString(now);
        }

        public String getDiff() {
            if (prev == UNRANKED && now != UNRANKED) {
                return "in";
            }
            if (prev != UNRANKED && now == UNRANKED) {
                return "out";
            }
            int diff = prev - now;
            if (diff == 0) {
                return "=";
            }
            if (diff > 0) {
                return "+" + diff;
            }
            return Integer.toString(diff);
        }

        public String getDiffClass() {
            String diff = getDiff();
            switch (diff.charAt(0)) {
                case '+':
                case 'i':
                    return "plus";
                case '-':
                case 'o':
                    return "minus";
                default:
                    return "";
            }
        }

        public String getUrl() {
            return url;
        }

    }

    public Result target(Context context,
        @PathParam("targetId") Integer targetId,
        @Param("startDate") String startDateStr,
        @Param("endDate") String endDateStr
    ) {
        GoogleTarget target = getTarget(context, targetId);
        List<GoogleSearch> searches = context.getAttribute("searches", List.class);
        Group group = context.getAttribute("group", Group.class);
        Config config = baseDB.config.getConfig();

        String display = context.getParameter("display", config.getDisplayGoogleTarget());
        if (!Config.VALID_DISPLAY_GOOGLE_TARGET.contains(display) && !"export".equals(display)) {
            display = Config.DEFAULT_DISPLAY_GOOGLE_TARGET;
        }

        if (target == null) {
            context.getFlashScope().error("error.invalidTarget");
            return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()));
        }

        Run minRun = baseDB.run.findFirst(group.getModule(), RunDB.STATUSES_DONE, null, group, null, null, Arrays.asList(targetId));
        Run maxRun = baseDB.run.findLast(group.getModule(), RunDB.STATUSES_DONE, null, group, null, null, Arrays.asList(targetId));

        if (maxRun == null || minRun == null || searches.isEmpty()) {
            String fallbackDisplay = "export".equals(display) ? "table" : display;
            return Results.ok()
                .template("/serposcope/views/google/GoogleTargetController/" + fallbackDisplay + ".ftl.html")
                .render("startDate", "")
                .render("endDate", "")
                .render("display", fallbackDisplay)
                .render("target", target);
        }

        boolean scheduled = false;
        LocalDate minDay = minRun.getDay();        
        LocalDate maxDay = maxRun.getDay();
		if (group.isSundayEnabled() || group.isMondayEnabled() || group.isTuesdayEnabled() || group.isWednesdayEnabled()
				|| group.isThursdayEnabled() || group.isFridayEnabled() || group.isSaturdayEnabled()) {
			scheduled = true;
			minDay = null;
			for (int i = 0; i < 7; i++) {
				LocalDate date = LocalDate.now();
				DayOfWeek dayOfWeek = date.minusDays(i).getDayOfWeek();
				if ((dayOfWeek == DayOfWeek.SUNDAY && group.isSundayEnabled())
						|| (dayOfWeek == DayOfWeek.MONDAY && group.isMondayEnabled())
						|| (dayOfWeek == DayOfWeek.TUESDAY && group.isTuesdayEnabled())
						|| (dayOfWeek == DayOfWeek.WEDNESDAY && group.isWednesdayEnabled())
						|| (dayOfWeek == DayOfWeek.THURSDAY && group.isThursdayEnabled())
						|| (dayOfWeek == DayOfWeek.FRIDAY && group.isFridayEnabled())
						|| (dayOfWeek == DayOfWeek.SATURDAY && group.isSaturdayEnabled())) {
					maxDay = maxDay.compareTo(date) >= 0 ? maxDay : date;
					break;
				}
			}
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

        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            startDate = maxDay.minusDays(30);
            endDate = maxDay;
        }

        List<Run> runs = null;
        if (scheduled) {
            runs = baseDB.run.listDone((LocalDate)null, (LocalDate)null, group, target);
            if (runs.size() > 0) {
                minDay = runs.get(0).getDay();
                if (startDate.compareTo(minDay) < 0) {
                	startDate = minDay;
                }
                maxDay = runs.get(runs.size() - 1).getDay();            	
                if (endDate.compareTo(maxDay) >= 0) {
                	endDate = maxDay;
                }
            }
        } else {
        	List<Integer> targetIds = Arrays.asList(targetId);
            Run firstRun = baseDB.run.findFirst(group.getModule(), RunDB.STATUSES_DONE, startDate, group, null, null, targetIds);
            Run lastRun = baseDB.run.findLast(group.getModule(), RunDB.STATUSES_DONE, endDate, group, null, null, targetIds);

            runs = baseDB.run.listDone(firstRun.getId(), lastRun.getId(), group, target);
            startDate = firstRun.getDay();
            endDate = lastRun.getDay();
        }

        switch (display) {
            case "table":
            case "variation":
                return Results.ok()
                    .template("/serposcope/views/google/GoogleTargetController/" + display + ".ftl.html")
                    .render("target", target)
                    .render("searches", searches)
                    .render("startDate", startDate.toString())
                    .render("endDate", endDate.toString())
                    .render("minDate", minDay)
                    .render("maxDate", maxDay)
                    .render("display", display); 
            case "chart":
                return renderChart(group, target, searches, runs, minDay, maxDay, startDate, endDate);
            case "export":
                return renderExport(group, target, searches, runs, minDay, maxDay, startDate, endDate);
            default:
                throw new IllegalStateException();
        }

    }

    public Result jsonVariation(
        Context context,
        @PathParam("targetId") Integer targetId,
        @Param("endDate") String endDateStr
    ) {
        GoogleTarget target = getTarget(context, targetId);
        List<GoogleSearch> searches = context.getAttribute("searches", List.class);
        Group group = context.getAttribute("group", Group.class);
        
        final LocalDate endDate;
        try {
            endDate = LocalDate.parse(endDateStr);
        } catch (Exception ex) {
            return Results.json().renderRaw("[[],[],[]]");
        }
        Run lastRun = baseDB.run.findLast(group.getModule(), RunDB.STATUSES_DONE, endDate, group, null, null, Arrays.asList(targetId));
        
        List<TargetVariation> ranksUp = new ArrayList<>();
        List<TargetVariation> ranksDown = new ArrayList<>();
        List<TargetVariation> ranksSame = new ArrayList<>();

        Map<Integer, GoogleSearch> searchesById = searches.stream()
            .collect(Collectors.toMap(GoogleSearch::getId, Function.identity()));

        List<GoogleRank> ranks = googleDB.rank.list(lastRun.getId(), group.getId(), target.getId());
        for (GoogleRank rank : ranks) {

            GoogleSearch search = searchesById.get(rank.googleSearchId);
            if (search == null) {
                continue;
            }

            if (rank.diff > 0) {
                ranksDown.add(new TargetVariation(search, rank));
            } else if (rank.diff < 0) {
                ranksUp.add(new TargetVariation(search, rank));
            } else {
                ranksSame.add(new TargetVariation(search, rank));
            }
        }

        Collections.sort(ranksUp, (TargetVariation o1, TargetVariation o2) -> Integer.compare(o1.rank.diff, o2.rank.diff));
        Collections.sort(ranksDown, (TargetVariation o1, TargetVariation o2) -> -Integer.compare(o1.rank.diff, o2.rank.diff));
        Collections.sort(ranksSame, (TargetVariation o1, TargetVariation o2) -> Integer.compare(o1.rank.rank, o2.rank.rank));

        return Results.ok()
            .json()
            .render((Context context0, Result result) -> {
                PrintWriter writer = null;
                OutputStream os = null;
                try {

                    String acceptEncoding = context0.getHeader("Accept-Encoding");
                    if (acceptEncoding != null && acceptEncoding.contains("gzip")) {
                        result.addHeader("Content-Encoding", "gzip");
                    }

                    ResponseStreams response = context0.finalizeHeaders(result);
                    os = response.getOutputStream();
                    if (acceptEncoding != null && acceptEncoding.contains("gzip")) {
                        os = new GZIPOutputStream(os);
                    }

                    writer = new PrintWriter(os);

                    writer.append("[");
                    int id = 0;
                    
                    writer.append("[");
                    for (int i = 0; i < ranksUp.size(); i++) {
                        TargetVariation var = ranksUp.get(i);
                        writer
                            .append("{")
                            .append("\"id\":").append(Integer.toString(id++))
                            .append(",\"search\":").append(searchToJson(var.search))
                            .append(",\"now\":").append(Integer.toString(var.rank.rank))
                            .append(",\"prv\":").append(Integer.toString(var.rank.previousRank))
                            .append(",\"diff\":").append(Integer.toString(var.rank.diff))
                            .append("}");
                        
                        if(i != ranksUp.size()-1){
                            writer.append(',');
                        }
                    }
                    
                    writer.append("],[");
                    
                    for (int i = 0; i < ranksDown.size(); i++) {
                        TargetVariation var = ranksDown.get(i);
                        writer
                            .append("{")
                            .append("\"id\":").append(Integer.toString(id++))
                            .append(",\"search\":").append(searchToJson(var.search))
                            .append(",\"now\":").append(Integer.toString(var.rank.rank))
                            .append(",\"prv\":").append(Integer.toString(var.rank.previousRank))
                            .append(",\"diff\":").append(Integer.toString(var.rank.diff))
                            .append("}");
                        
                        if(i != ranksDown.size()-1){
                            writer.append(',');
                        }
                    }
                    
                    writer.append("],[");
                    
                    for (int i = 0; i < ranksSame.size(); i++) {
                        TargetVariation var = ranksSame.get(i);
                        writer
                            .append("{")
                            .append("\"id\":").append(Integer.toString(id++))
                            .append(",\"search\":").append(searchToJson(var.search))
                            .append(",\"now\":").append(Integer.toString(var.rank.rank))
                            .append("}");
                        
                        if(i != ranksSame.size()-1){
                            writer.append(',');
                        }                        
                    }
                    writer.append("]]");

				} catch (EOFException e) {
					LOG.warn("[Export Variation] Download was interrupted: {}", e.getMessage());
                } catch (Exception ex) {
                    LOG.warn("HTTP error", ex);
                } finally {
                    if (os != null) {
                        try {
                            writer.close();
                            os.close();
                        } catch (Exception ex) {
                        }
                    }
                }
            });
    }

    protected StringBuilder searchToJson(GoogleSearch search) {
        StringBuilder searchesJson = new StringBuilder("{");
        searchesJson.append("\"id\":")
            .append(search.getId())
            .append(",");
        searchesJson.append("\"keyword\":\"")
            .append(StringEscapeUtils.escapeJson(search.getKeyword()))
            .append("\",");
        searchesJson.append("\"country\":\"")
            .append(search.getCountry().name())
            .append("\",");
        searchesJson.append("\"device\":\"")
            .append(SMARTPHONE.equals(search.getDevice()) ? 'M' : 'D')
            .append("\",");
        searchesJson.append("\"local\":\"")
            .append(search.getLocal() == null ? "" : StringEscapeUtils.escapeJson(search.getLocal()))
            .append("\",");
        searchesJson.append("\"datacenter\":\"")
            .append(search.getDatacenter() == null ? "" : StringEscapeUtils.escapeJson(search.getDatacenter()))
            .append("\",");
        searchesJson.append("\"custom\":\"")
            .append(search.getCustomParameters() == null ? "" : StringEscapeUtils.escapeJson(search.getCustomParameters()))
            .append("\"");
        searchesJson.append("}");
        return searchesJson;
    }

    protected Result renderChart(
        Group group,
        GoogleTarget target,
        List<GoogleSearch> searches,
        List<Run> runs,
        LocalDate minDay,
        LocalDate maxDay,
        LocalDate startDate,
        LocalDate endDate
    ) {
        String display = "chart";
        StringBuilder builder = new StringBuilder("{\"searches\": [");
        for (GoogleSearch search : searches) {
            builder.append("\"").append(StringEscapeUtils.escapeJson(search.getKeyword())).append("\",");
        }
        builder.setCharAt(builder.length() - 1, ']');
        builder.append(",\"ranks\": [");

        int maxRank = 0;
        for (int i = 0; i < runs.size(); i++) {
        	Run run = runs.get(i);
            builder.append("\n\t[").append(i).append(",");
            // calendar
            builder.append("null,");

            Map<Integer, GoogleRank> ranks = googleDB.rank.list(run.getId(), group.getId(), target.getId())
                .stream().collect(Collectors.toMap((g) -> g.googleSearchId, Function.identity()));
            
            for (GoogleSearch search : searches) {
                GoogleRank fullRank = ranks.get(search.getId());
//                GoogleRank fullRank = googleDB.rank.getFull(run.getId(), group.getId(), target.getId(), search.getId());
                if (fullRank != null && fullRank.rank != GoogleRank.UNRANKED && fullRank.rank > maxRank) {
                    maxRank = fullRank.rank;
                }
                builder.append(fullRank == null || fullRank.rank == GoogleRank.UNRANKED ? "null" : fullRank.rank).append(',');
            }

            builder.setCharAt(builder.length() - 1, ']');
            builder.append(",");
        }
        builder.setCharAt(builder.length() - 1, ']');
        builder.append(",\n\"maxRank\": ").append(maxRank).append("}");

        return Results.ok()
            .template("/serposcope/views/google/GoogleTargetController/" + display + ".ftl.html")
            .render("target", target)
            .render("searches", searches)
            .render("startDate", startDate.toString())
            .render("endDate", endDate.toString())
            .render("minDate", minDay)
            .render("maxDate", maxDay)
            .render("display", display)
            .render("ranksJson", builder.toString());
    }

    @FilterWith({
    	MaintenanceFilter.class, XSRFFilter.class
    })
    protected Result renderExport(
        Group group,
        GoogleTarget target,
        List<GoogleSearch> searches,
        List<Run> runs,
        LocalDate minDay,
        LocalDate maxDay,
        LocalDate startDate,
        LocalDate endDate
    ) {

        return Results.ok()
            .text()
            .addHeader("Content-Disposition", "attachment; filename=\"serps.csv\"")
            .render((Context context, Result result) -> {
                ResponseStreams stream = context.finalizeHeaders(result);
                try (OutputStream out = stream.getOutputStream();
    					Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
        			byte[] bom = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
        			out.write(bom);
					writer.append(
							messages.get("google.target.exportHeader", Optional.of(context.getAcceptLanguage())).get())
							.append("\n");
                    for (Run run : runs.stream().sorted(Comparator.comparing(Run::getDay)).collect(Collectors.toList())) {
                    	LocalDate date = run.getDay();
                        String day = date.toString();
                        
                        for (GoogleSearch search : searches) {
                            GoogleRank rank = googleDB.rank.getDateRank(date, group.getId(), target.getId(), search.getId());
                            if (rank == null) {
                            	continue;
                            }
                            GoogleSerp serp = googleDB.serp.get(run.getId(), search.getId());
                            writer.append(day).append(",");
                            writer.append(StringEscapeUtils.escapeCsv(search.getKeyword())).append(",");
                            writer.append(rank.rank == UNRANKED ? "-" : Integer.toString(rank.rank)).append(",");
                            writer.append(rank.url == null ? "" : rank.url).append(",");
                            writer.append(StringEscapeUtils.escapeCsv(target.getName())).append(",");
                            writer.append(search.getDevice() == GoogleDevice.DESKTOP ? "PC" : "SP").append(",");
                            writer.append(search.getCountry().name()).append(",");
//                            writer.append(
//                                search.getDatacenter() != null
//                                    ? StringEscapeUtils.escapeCsv(search.getDatacenter())
//                                    : ""
//                            ).append(",");
                            writer.append(
                                search.getLocal() != null
                                    ? StringEscapeUtils.escapeCsv(search.getLocal())
                                    : ""
                            ).append(",");
                            writer.append(
                                search.getCustomParameters() != null
                                    ? StringEscapeUtils.escapeCsv(search.getCustomParameters())
                                    : ""
                            ).append(",");
                            writer.append(serp.getResults() == null ? "" : serp.getResults().toString());
                            writer.append("\n");
                        }
                    }

				} catch (EOFException e) {
					LOG.warn("[Export Serps] Download was interrupted: {}", e.getMessage());
                } catch (IOException ex) {
                    LOG.warn("error while exporting csv");
                }
            });

    }

    public Result jsonRanks(
        Context context,
        @PathParam("targetId") Integer targetId,
        @Param("startDate") String startDateStr,
        @Param("endDate") String endDateStr
    ) {
        final GoogleTarget target = getTarget(context, targetId);
        final List<GoogleSearch> searches = context.getAttribute("searches", List.class);
        final Group group = context.getAttribute("group", Group.class);
        final LocalDate startDate, endDate;
        try {
            startDate = LocalDate.parse(startDateStr);
            endDate = LocalDate.parse(endDateStr);
        } catch (Exception ex) {
            return Results.json().renderRaw("[]");
        }

        final List<Run> runs = baseDB.run.listDone(startDate, endDate, group, target);

        return Results.ok()
            .json()
            .render((Context context0, Result result) -> {
                PrintWriter writer = null;
                OutputStream os = null;
                try {

                    String acceptEncoding = context0.getHeader("Accept-Encoding");
                    if (acceptEncoding != null && acceptEncoding.contains("gzip")) {
                        result.addHeader("Content-Encoding", "gzip");
                    }

                    ResponseStreams response = context0.finalizeHeaders(result);
                    os = response.getOutputStream();
                    if (acceptEncoding != null && acceptEncoding.contains("gzip")) {
                        os = new GZIPOutputStream(os);
                    }

                    writer = new PrintWriter(os);
                    getTableJson(group, target, searches, runs, startDate, endDate, writer);

				} catch (EOFException e) {
					LOG.warn("[Export Ranks] Download was interrupted: {}", e.getMessage());
                } catch (Exception ex) {
                    LOG.warn("HTTP error", ex);
                } finally {
                    if (os != null) {
                        try {
                            writer.close();
                            os.close();
                        } catch (Exception ex) {
                        }
                    }
                }
            });
    }

    protected void getTableJson(
        Group group,
        GoogleTarget target,
        List<GoogleSearch> searches,
        List<Run> runs,
        LocalDate startDate,
        LocalDate endDate,
        Writer writer
    ) throws IOException {
        writer.append("[[[-1, 0, 0, [");
        if (runs.isEmpty() || searches.isEmpty()) {
            writer.append("]]],[]]");
            return;
        }

        Set<LocalDate> dates = new TreeSet<>();
        for (Run run : runs) {
        	dates.add(run.getDay());
        }
        // events
        for (Iterator<LocalDate> itr = dates.iterator(); itr.hasNext();) {
        	LocalDate date = itr.next();
            writer.append("0");

            if (itr.hasNext()) {
                writer.append(",");
            }

        }
        writer.append("]],");

        Map<Integer, StringBuilder> builders = new HashMap<>();
        Map<Integer, GoogleBest> bests = googleDB.rank.getBestBySearch(target.getGroupId(), target.getId(), searches);

        for (GoogleSearch search : searches) {
            StringBuilder builder;
            builders.put(search.getId(), builder = new StringBuilder("["));
//            GoogleBest best = googleDB.rank.getBest(target.getGroupId(), target.getId(), search.getId());

            builder
                .append(search.getId())
                .append(",[\"").append(StringEscapeUtils.escapeJson(search.getKeyword()))
                .append("\",\"").append(search.getCountry().name())
                .append("\",\"").append(SMARTPHONE.equals(search.getDevice()) ? 'M' : 'D')
                .append("\",\"").append(search.getLocal() == null ? "" : StringEscapeUtils.escapeJson(search.getLocal()))
                .append("\",\"").append(search.getDatacenter() == null ? "" : StringEscapeUtils.escapeJson(search.getDatacenter()))
                .append("\",\"").append(search.getCustomParameters() == null ? "" : StringEscapeUtils.escapeJson(search.getCustomParameters()))
                .append("\"],");

            GoogleBest best = bests.get(search.getId());
            if (best == null) {
            	best = googleDB.rank.createUnranked(target.getGroupId(), target.getId(), search.getId());
            }
            builder
                .append("[").append(best.getRank())
                .append(",\"").append(best.getRunDay() != null ? best.getRunDay().toLocalDate().toString() : "?")
                .append("\",\"").append(StringEscapeUtils.escapeJson(best.getUrl()))
                .append("\"],")
            	.append("[");
        }

    	int start = runs.stream().min(Comparator.comparingInt(Run::getId)).get().getId();
    	int end = runs.stream().max(Comparator.comparingInt(Run::getId)).get().getId();
        List<GoogleRank> allRanks = googleDB.rank.list0(start, end, group.getId(), target.getId());

        for (Iterator<LocalDate> itr = dates.iterator(); itr.hasNext();) {
        	LocalDate date = itr.next();
        	
        	List<Integer> runIds = runs.stream().filter(r -> date.equals(r.getDay()))
        			.map(Run::getId).collect(Collectors.toList());
	        Map<Integer, GoogleRank> ranks = allRanks.stream().filter(r -> runIds.contains(r.runId))
	        		.collect(Collectors.toMap((r) -> r.googleSearchId, Function.identity()));

            for (GoogleSearch search : searches) {
                StringBuilder builder = builders.get(search.getId());
                GoogleRank fullRank = ranks.get(search.getId());
                if (fullRank != null && fullRank.rank != GoogleRank.UNRANKED) {
                    builder.append("[").append(fullRank.rank)
                        .append(",").append(fullRank.previousRank)
                        .append(",\"").append(StringEscapeUtils.escapeJson(fullRank.url))
                        .append("\"],");
                } else {
                    builder.append("0,");
                }

                if (!itr.hasNext()) {
                    builder.deleteCharAt(builder.length() - 1);
                    builder.append("]]");
                }
            }
        }

        List<StringBuilder> buildersArray = new ArrayList<>(builders.values());
        for (int i = 0; i < buildersArray.size(); i++) {
            writer.append(buildersArray.get(i));
            if (i != buildersArray.size() - 1) {
                writer.append(",");
            }
        }
        writer.append("],[");
        for (Iterator<LocalDate> itr = dates.iterator(); itr.hasNext();) {
        	LocalDate date = itr.next();
            writer.append("\"").append(date.toString()).append("\"");
            if (itr.hasNext()) {
                writer.append(",");
            }
        }
        writer.append("]]");
    }

    protected String getTableJsonData0(
        Group group,
        GoogleTarget target,
        List<GoogleSearch> searches,
        List<Run> runs,
        LocalDate startDate,
        LocalDate endDate
    ) {
        StringBuilder jsonData = new StringBuilder("[{\"id\": -1, \"best\": null, \"days\": [");
        if (runs.isEmpty() || searches.isEmpty()) {
            jsonData.append("]}]");
            return jsonData.toString();
        }

        Set<LocalDate> dates = new TreeSet<>();
        for (Run run : runs) {
        	dates.add(run.getDay());
        }
        // events
        for (Iterator<LocalDate> itr = dates.iterator(); itr.hasNext();) {
        	LocalDate date = itr.next();
            jsonData.append("null,");
        }
        jsonData.deleteCharAt(jsonData.length() - 1);
        jsonData.append("]},");

        Map<Integer, StringBuilder> builders = new HashMap<>();
        Map<Integer, GoogleBest> bests = googleDB.rank.getBestBySearch(target.getGroupId(), target.getId(), searches);

        for (GoogleSearch search : searches) {
            StringBuilder builder;
            builders.put(search.getId(), builder = new StringBuilder());
            builder.append("");
            // GoogleBest best = googleDB.rank.getBest(target.getGroupId(), target.getId(), search.getId());

            builder.append("{\"id\":").append(search.getId())
                .append(",\"search\":{")
                .append("\"id\":").append(search.getId())
                .append(",\"k\":\"").append(StringEscapeUtils.escapeJson(search.getKeyword()))
                .append("\",\"t\":\"").append(search.getCountry().name())
                .append("\",\"d\":\"").append(SMARTPHONE.equals(search.getDevice()) ? 'M' : 'D')
                .append("\",\"l\":\"").append(search.getLocal() == null ? "" : StringEscapeUtils.escapeJson(search.getLocal()))
                .append("\",\"dc\":\"").append(search.getDatacenter() == null ? "" : StringEscapeUtils.escapeJson(search.getDatacenter()))
                .append("\",\"c\":\"").append(search.getCustomParameters() == null ? "" : StringEscapeUtils.escapeJson(search.getCustomParameters()))
                .append("\"}, \"best\":");

            GoogleBest best = bests.get(search.getId());
            if (best == null) {
            	best = googleDB.rank.createUnranked(target.getGroupId(), target.getId(), search.getId());
            }
            builder
                .append("{\"rank\":").append(best.getRank())
                .append(",\"date\":\"").append(best.getRunDay() != null ? best.getRunDay().toLocalDate().toString() : "?")
                .append("\",\"url\":\"").append(StringEscapeUtils.escapeJson(best.getUrl()))
                .append("\"},")
            	.append("\"days\": [");
        }

        for (Iterator<LocalDate> itr = dates.iterator(); itr.hasNext();) {
        	LocalDate date = itr.next();
        	
        	int start = -1, end = -1;
	        for (int i = 0; i < runs.size(); i++) {
	            Run run = runs.get(i);
	            if (date.equals(run.getDay())) {
	            	if (start < 1 || start > run.getId()) {
	            		start = run.getId();
	            	}
	            	if (end < 1 || end < run.getId()) {
	            		end = run.getId();
	            	}
	            }
	        }
	
            Map<Integer, GoogleRank> ranks = googleDB.rank.list0(start, end, group.getId(), target.getId())
                .stream().collect(Collectors.toMap((r) -> r.googleSearchId, Function.identity()));

            for (GoogleSearch search : searches) {
                StringBuilder builder = builders.get(search.getId());
                GoogleRank fullRank = ranks.get(search.getId());
                if (fullRank != null && fullRank.rank != GoogleRank.UNRANKED) {
                    builder.append("{\"r\":").append(fullRank.rank)
                        .append(",\"p\":").append(fullRank.previousRank)
                        .append(",\"u\":\"").append(StringEscapeUtils.escapeJson(fullRank.url))
                        .append("\"},");
                } else {
                    builder.append("{\"r\":32767,\"p\":null,\"u\":null},");
                }

                if (!itr.hasNext()) {
                    builder.deleteCharAt(builder.length() - 1);
                    builder.append("]},");
                }
            }
        }

        for (StringBuilder value : builders.values()) {
            jsonData.append(value);
        }
        jsonData.deleteCharAt(jsonData.length() - 1);
        jsonData.append("]");

        return jsonData.toString();
    }

}
