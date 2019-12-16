package com.serphacker.serposcope.querybuilder;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QGoogleRankBest is a Querydsl query type for QGoogleRankBest
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QGoogleRankBest extends com.querydsl.sql.RelationalPathBase<QGoogleRankBest> {

    private static final long serialVersionUID = 1777540437;

    public static final QGoogleRankBest googleRankBest = new QGoogleRankBest("GOOGLE_RANK_BEST");

    public final NumberPath<Integer> googleSearchId = createNumber("googleSearchId", Integer.class);

    public final NumberPath<Integer> googleTargetId = createNumber("googleTargetId", Integer.class);

    public final NumberPath<Integer> groupId = createNumber("groupId", Integer.class);

    public final NumberPath<Short> rank = createNumber("rank", Short.class);

    public final DateTimePath<java.sql.Timestamp> runDay = createDateTime("runDay", java.sql.Timestamp.class);

    public final StringPath url = createString("url");

    public final com.querydsl.sql.PrimaryKey<QGoogleRankBest> constraintB = createPrimaryKey(googleSearchId, googleTargetId, groupId);

    public final com.querydsl.sql.ForeignKey<QGoogleSearch> constraintB727 = createForeignKey(googleSearchId, "ID");

    public final com.querydsl.sql.ForeignKey<QGroup> constraintB7 = createForeignKey(groupId, "ID");

    public final com.querydsl.sql.ForeignKey<QGoogleTarget> constraintB72 = createForeignKey(googleTargetId, "ID");

    public QGoogleRankBest(String variable) {
        super(QGoogleRankBest.class, forVariable(variable), "PUBLIC", "GOOGLE_RANK_BEST");
        addMetadata();
    }

    public QGoogleRankBest(String variable, String schema, String table) {
        super(QGoogleRankBest.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QGoogleRankBest(String variable, String schema) {
        super(QGoogleRankBest.class, forVariable(variable), schema, "GOOGLE_RANK_BEST");
        addMetadata();
    }

    public QGoogleRankBest(Path<? extends QGoogleRankBest> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "GOOGLE_RANK_BEST");
        addMetadata();
    }

    public QGoogleRankBest(PathMetadata metadata) {
        super(QGoogleRankBest.class, metadata, "PUBLIC", "GOOGLE_RANK_BEST");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(googleSearchId, ColumnMetadata.named("GOOGLE_SEARCH_ID").withIndex(3).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(googleTargetId, ColumnMetadata.named("GOOGLE_TARGET_ID").withIndex(2).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(groupId, ColumnMetadata.named("GROUP_ID").withIndex(1).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(rank, ColumnMetadata.named("RANK").withIndex(4).ofType(Types.SMALLINT).withSize(5));
        addMetadata(runDay, ColumnMetadata.named("RUN_DAY").withIndex(5).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(url, ColumnMetadata.named("URL").withIndex(6).ofType(Types.VARCHAR).withSize(256));
    }

}

