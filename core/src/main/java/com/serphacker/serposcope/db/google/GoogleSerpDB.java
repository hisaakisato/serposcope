/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package com.serphacker.serposcope.db.google;

import com.google.inject.Singleton;
import com.mysema.commons.lang.CloseableIterator;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.SubQueryExpression;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.sql.SQLExpressions;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.StatementOptions;
import com.querydsl.sql.dml.SQLDeleteClause;
import com.querydsl.sql.dml.SQLInsertClause;
import com.serphacker.serposcope.db.AbstractDB;
import com.serphacker.serposcope.models.base.Group;
import com.serphacker.serposcope.models.google.GoogleSearch;
import com.serphacker.serposcope.models.google.GoogleSerp;
import com.serphacker.serposcope.querybuilder.QGoogleRank;
import com.serphacker.serposcope.querybuilder.QGoogleSearch;
import com.serphacker.serposcope.querybuilder.QGoogleSearchGroup;
import com.serphacker.serposcope.querybuilder.QGoogleSerp;
import com.serphacker.serposcope.querybuilder.QRun;
import com.serphacker.serposcope.scraper.google.GoogleDevice;
import com.serphacker.serposcope.util.ThrowableConsumer;

import java.nio.ByteBuffer;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import javax.sql.rowset.serial.SerialBlob;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

@Singleton
public class GoogleSerpDB extends AbstractDB {

	QGoogleSerp t_serp = QGoogleSerp.googleSerp;
	QGoogleRank t_rank = QGoogleRank.googleRank;
	QRun t_run = QRun.run;
	QGoogleSearchGroup t_ggroup = QGoogleSearchGroup.googleSearchGroup;
	QGoogleSearch t_gsearch = QGoogleSearch.googleSearch;

	LZ4Factory factory = LZ4Factory.fastestInstance();
	LZ4Compressor compressor = factory.fastCompressor();
	LZ4FastDecompressor decompressor = factory.fastDecompressor();

	public boolean insert(GoogleSerp serp) {
		boolean inserted = false;

		try (Connection con = ds.getConnection()) {

			inserted = new SQLInsertClause(con, dbTplConf, t_serp).set(t_serp.runId, serp.getRunId())
					.set(t_serp.googleSearchId, serp.getGoogleSearchId())
					.set(t_serp.runDay, Timestamp.valueOf(serp.getRunDay()))
					.set(t_serp.serp, new SerialBlob(compress(serp.getSerializedEntries()))).execute() == 1;

		} catch (Exception ex) {
			LOG.error("SQL error", ex);
		}

		return inserted;
	}

	public void deleteByRun(int runId) {
		try (Connection con = ds.getConnection()) {
			new SQLDeleteClause(con, dbTplConf, t_serp).where(t_serp.runId.eq(runId)).execute();
		} catch (Exception ex) {
			LOG.error("SQL error", ex);
		}
	}

	public void deleteBySearch(int searchId) {
		try (Connection con = ds.getConnection()) {
			new SQLDeleteClause(con, dbTplConf, t_serp).where(t_serp.googleSearchId.eq(searchId)).execute();
		} catch (Exception ex) {
			LOG.error("SQL error", ex);
		}
	}

	public void wipe() {
		try (Connection con = ds.getConnection()) {
			new SQLDeleteClause(con, dbTplConf, t_serp).execute();
		} catch (Exception ex) {
			LOG.error("SQL error", ex);
		}
	}

	public GoogleSerp get(int runId, int googleSearchId) {
		GoogleSerp serp = null;
		try (Connection con = ds.getConnection()) {

			Tuple tuple = new SQLQuery<Void>(con, dbTplConf).select(t_serp.all()).from(t_serp)
					.where(t_serp.runId.eq(runId)).where(t_serp.googleSearchId.eq(googleSearchId)).fetchFirst();

			serp = fromTuple(tuple);

		} catch (Exception ex) {
			LOG.error("SQL error", ex);
		}
		return serp;
	}

//    public void stream(Collection<Integer> runs, int googleSearchId, Consumer<GoogleSerp> callback){
//        try(Connection con = ds.getConnection()){
//            
//            CloseableIterator<Tuple> iterate = new SQLQuery<Void>(con, dbTplConf)
//                .select(t_serp.all())
//                .from(t_serp)
//                .where(t_serp.runId.in(runs))
//                .where(t_serp.googleSearchId.eq(googleSearchId))
//                .orderBy(t_serp.runId.asc())
//                .iterate();
//            
//            while(iterate.hasNext()){
//                GoogleSerp serp = fromTuple(iterate.next());
//                callback.accept(serp);
//            }
//            
//        }catch(Exception ex){
//            LOG.error("SQL error", ex);
//        }
//    }    

	public void stream(Integer firstRun, Integer lastRun, int googleSearchId, ThrowableConsumer<GoogleSerp> callback) {
		stream(firstRun, lastRun, Arrays.asList(googleSearchId), callback);
	}

	public void stream(Integer firstRun, Integer lastRun, List<Integer> googleSearchIds, ThrowableConsumer<GoogleSerp> callback) {
		try (Connection con = ds.getConnection()) {

			BooleanExpression exp = t_serp.googleSearchId.in(googleSearchIds);
			if (firstRun != null && lastRun != null) {
				exp = exp.and(t_run.id.between(firstRun, lastRun));
			} else if (firstRun != null) {
				exp = exp.and(t_run.id.goe(firstRun));
			} else if (lastRun != null) {
				exp = exp.and(t_run.id.loe(lastRun));
			}

			SubQueryExpression<Integer> subQuery = SQLExpressions
					.select(t_serp.runId.max().as(t_serp.runId)).from(t_serp).innerJoin(t_run)
					.on(t_serp.runId.eq(t_run.id).and(exp))
					.groupBy(t_run.day);

			SQLQuery<Tuple> query = new SQLQuery<Void>(con, dbTplConf)
					.select(t_serp.runId, t_serp.googleSearchId, t_serp.runDay, t_serp.serp, t_gsearch.id,
							t_gsearch.keyword, t_gsearch.country, t_gsearch.datacenter, t_gsearch.device,
							t_gsearch.local, t_gsearch.customParameters)
					.from(t_serp).innerJoin(t_gsearch).on(t_serp.googleSearchId.eq(t_gsearch.id))
					.where(t_serp.runId.in(subQuery).and(t_serp.googleSearchId.in(googleSearchIds)))
					.orderBy(t_serp.runId.asc());

			query.setStatementOptions(StatementOptions.builder().setFetchSize(250).build());
			CloseableIterator<Tuple> iterate = query.iterate();

			while (iterate.hasNext()) {
				GoogleSerp serp = fromTuple(iterate.next());
				callback.accept(serp);
			}

		} catch (InterruptedException ex) {
			// abort
			Thread.currentThread().interrupt();
		} catch (Exception ex) {
			LOG.error("SQL error", ex);
		}
	}

	public void stream(LocalDate startDate, LocalDate endDate, Group group, List<Integer> googleSearchIds,
			ThrowableConsumer<GoogleSerp> callback) {
		try (Connection con = ds.getConnection()) {

			if (group == null) {
				LOG.error("Group was not found.");
				return;
			}

			BooleanExpression exp = null;
			if (googleSearchIds != null && !googleSearchIds.isEmpty()) {
				exp = t_serp.googleSearchId.in(googleSearchIds);
			}

			SubQueryExpression<Tuple> subQuery = SQLExpressions
					.select(t_serp.runId.max().as(t_serp.runId), t_serp.googleSearchId).from(t_serp).innerJoin(t_run)
					.on(t_serp.runId.eq(t_run.id).and(exp)
							.and(t_run.day.between(Date.valueOf(startDate), Date.valueOf(endDate))))
					.innerJoin(t_ggroup).on(t_ggroup.groupId.eq(t_ggroup.groupId).and(t_ggroup.groupId.eq(group.getId())))
					.groupBy(t_run.day, t_serp.googleSearchId);

			SQLQuery<Tuple> query = new SQLQuery<Void>(con, dbTplConf)
					.select(t_serp.runId, t_serp.googleSearchId, t_serp.runDay, t_serp.serp, t_gsearch.id,
							t_gsearch.keyword, t_gsearch.country, t_gsearch.datacenter, t_gsearch.device,
							t_gsearch.local, t_gsearch.customParameters)
					.from(t_serp).innerJoin(t_gsearch).on(t_serp.googleSearchId.eq(t_gsearch.id))
					.where(Expressions.list(t_serp.runId, t_serp.googleSearchId).in(subQuery).and(exp))
					.orderBy(t_serp.runId.asc(), t_gsearch.keyword.asc());

			query.setStatementOptions(StatementOptions.builder().setFetchSize(250).build());
			CloseableIterator<Tuple> iterate = query.iterate();

			while (iterate.hasNext()) {
				GoogleSerp serp = fromTuple(iterate.next());
				callback.accept(serp);
			}

		} catch (InterruptedException ex) {
			// abort
			Thread.currentThread().interrupt();
		} catch (Exception ex) {
			LOG.error("SQL error", ex);
		}
	}

	protected GoogleSerp fromTuple(Tuple tuple) throws Exception {
		if (tuple == null) {
			return null;
		}

		GoogleSerp serp = new GoogleSerp(tuple.get(t_serp.runId), tuple.get(t_serp.googleSearchId),
				tuple.get(t_serp.runDay).toLocalDateTime());
		Blob blob = tuple.get(t_serp.serp);
		if (blob != null) {
			byte[] compressedData = blob.getBytes(1, (int) blob.length());
			serp.setSerializedEntries(decompress(compressedData));
		}
		if (tuple.get(t_gsearch.keyword) != null) {
			GoogleSearch search = new GoogleSearch(tuple.get(t_gsearch.keyword));
				        
	        search.setId(tuple.get(t_gsearch.id));
	        search.setDatacenter(tuple.get(t_gsearch.datacenter));
	        search.setDevice(GoogleDevice.values()[tuple.get(t_gsearch.device)]);
	        search.setLocal(tuple.get(t_gsearch.local));
	        search.setCountry(tuple.get(t_gsearch.country));
	        search.setCustomParameters(tuple.get(t_gsearch.customParameters));

	        serp.setSearch(search);
		}

		return serp;

	}

	protected byte[] compress(byte[] data) {
		if (data == null || data.length < 1) {
			return null;
		}

		int decompressedLength = data.length;
		int maxCompressedLength = compressor.maxCompressedLength(decompressedLength);
		byte[] tmp = new byte[maxCompressedLength];
		int compressedLength = compressor.compress(data, 0, decompressedLength, tmp, 0, maxCompressedLength);

		byte[] compressed = new byte[4 + compressedLength];
		ByteBuffer.wrap(compressed, 0, 4).putInt(decompressedLength);
		System.arraycopy(tmp, 0, compressed, 4, compressedLength);

		return compressed;
	}

	protected byte[] decompress(byte[] compressed) {
		if (compressed == null || compressed.length < 5) {
			return null;
		}

		int decompressedLength = ByteBuffer.wrap(compressed, 0, 4).getInt();
		byte[] decompressed = new byte[decompressedLength];
		decompressor.decompress(compressed, 4, decompressed, 0, decompressedLength);

		return decompressed;
	}

}
