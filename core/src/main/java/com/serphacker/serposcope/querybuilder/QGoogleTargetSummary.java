package com.serphacker.serposcope.querybuilder;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QGoogleTargetSummary is a Querydsl query type for QGoogleTargetSummary
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QGoogleTargetSummary extends com.querydsl.sql.RelationalPathBase<QGoogleTargetSummary> {

    private static final long serialVersionUID = 161696464;

    public static final QGoogleTargetSummary googleTargetSummary = new QGoogleTargetSummary("GOOGLE_TARGET_SUMMARY");

    public final NumberPath<Integer> googleTargetId = createNumber("googleTargetId", Integer.class);

    public final NumberPath<Integer> groupId = createNumber("groupId", Integer.class);

    public final NumberPath<Integer> previousScoreBasisPoint = createNumber("previousScoreBasisPoint", Integer.class);

    public final NumberPath<Integer> runId = createNumber("runId", Integer.class);

    public final NumberPath<Integer> scoreBasisPoint = createNumber("scoreBasisPoint", Integer.class);

    public final NumberPath<Integer> scoreRaw = createNumber("scoreRaw", Integer.class);

    public final StringPath topImprovements = createString("topImprovements");

    public final StringPath topLosts = createString("topLosts");

    public final StringPath topRanks = createString("topRanks");

    public final NumberPath<Integer> totalOut = createNumber("totalOut", Integer.class);

    public final NumberPath<Integer> totalTop10 = createNumber("totalTop10", Integer.class);

    public final NumberPath<Integer> totalTop100 = createNumber("totalTop100", Integer.class);

    public final NumberPath<Integer> totalTop3 = createNumber("totalTop3", Integer.class);

    public final com.querydsl.sql.PrimaryKey<QGoogleTargetSummary> constraint41 = createPrimaryKey(googleTargetId, groupId, runId);

    public final com.querydsl.sql.ForeignKey<QGoogleTarget> constraint41d3 = createForeignKey(googleTargetId, "ID");

    public final com.querydsl.sql.ForeignKey<QGroup> constraint41d = createForeignKey(groupId, "ID");

    public final com.querydsl.sql.ForeignKey<QRun> constraint41d36 = createForeignKey(runId, "ID");

    public QGoogleTargetSummary(String variable) {
        super(QGoogleTargetSummary.class, forVariable(variable), "PUBLIC", "GOOGLE_TARGET_SUMMARY");
        addMetadata();
    }

    public QGoogleTargetSummary(String variable, String schema, String table) {
        super(QGoogleTargetSummary.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QGoogleTargetSummary(String variable, String schema) {
        super(QGoogleTargetSummary.class, forVariable(variable), schema, "GOOGLE_TARGET_SUMMARY");
        addMetadata();
    }

    public QGoogleTargetSummary(Path<? extends QGoogleTargetSummary> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "GOOGLE_TARGET_SUMMARY");
        addMetadata();
    }

    public QGoogleTargetSummary(PathMetadata metadata) {
        super(QGoogleTargetSummary.class, metadata, "PUBLIC", "GOOGLE_TARGET_SUMMARY");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(googleTargetId, ColumnMetadata.named("GOOGLE_TARGET_ID").withIndex(2).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(groupId, ColumnMetadata.named("GROUP_ID").withIndex(1).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(previousScoreBasisPoint, ColumnMetadata.named("PREVIOUS_SCORE_BASIS_POINT").withIndex(13).ofType(Types.INTEGER).withSize(10));
        addMetadata(runId, ColumnMetadata.named("RUN_ID").withIndex(3).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(scoreBasisPoint, ColumnMetadata.named("SCORE_BASIS_POINT").withIndex(12).ofType(Types.INTEGER).withSize(10));
        addMetadata(scoreRaw, ColumnMetadata.named("SCORE_RAW").withIndex(11).ofType(Types.INTEGER).withSize(10));
        addMetadata(topImprovements, ColumnMetadata.named("TOP_IMPROVEMENTS").withIndex(9).ofType(Types.VARCHAR).withSize(128));
        addMetadata(topLosts, ColumnMetadata.named("TOP_LOSTS").withIndex(10).ofType(Types.VARCHAR).withSize(128));
        addMetadata(topRanks, ColumnMetadata.named("TOP_RANKS").withIndex(8).ofType(Types.VARCHAR).withSize(128));
        addMetadata(totalOut, ColumnMetadata.named("TOTAL_OUT").withIndex(7).ofType(Types.INTEGER).withSize(10));
        addMetadata(totalTop10, ColumnMetadata.named("TOTAL_TOP_10").withIndex(5).ofType(Types.INTEGER).withSize(10));
        addMetadata(totalTop100, ColumnMetadata.named("TOTAL_TOP_100").withIndex(6).ofType(Types.INTEGER).withSize(10));
        addMetadata(totalTop3, ColumnMetadata.named("TOTAL_TOP_3").withIndex(4).ofType(Types.INTEGER).withSize(10));
    }

}

