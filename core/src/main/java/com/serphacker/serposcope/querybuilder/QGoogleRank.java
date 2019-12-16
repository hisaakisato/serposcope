package com.serphacker.serposcope.querybuilder;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QGoogleRank is a Querydsl query type for QGoogleRank
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QGoogleRank extends com.querydsl.sql.RelationalPathBase<QGoogleRank> {

    private static final long serialVersionUID = -721689647;

    public static final QGoogleRank googleRank = new QGoogleRank("GOOGLE_RANK");

    public final NumberPath<Short> diff = createNumber("diff", Short.class);

    public final NumberPath<Integer> googleSearchId = createNumber("googleSearchId", Integer.class);

    public final NumberPath<Integer> googleTargetId = createNumber("googleTargetId", Integer.class);

    public final NumberPath<Integer> groupId = createNumber("groupId", Integer.class);

    public final NumberPath<Short> previousRank = createNumber("previousRank", Short.class);

    public final NumberPath<Short> rank = createNumber("rank", Short.class);

    public final NumberPath<Integer> runId = createNumber("runId", Integer.class);

    public final StringPath url = createString("url");

    public final com.querydsl.sql.PrimaryKey<QGoogleRank> constraint6e2 = createPrimaryKey(googleSearchId, googleTargetId, groupId, runId);

    public final com.querydsl.sql.ForeignKey<QGoogleTarget> constraint6e2226 = createForeignKey(googleTargetId, "ID");

    public final com.querydsl.sql.ForeignKey<QGroup> constraint6e222 = createForeignKey(groupId, "ID");

    public final com.querydsl.sql.ForeignKey<QRun> constraint6e22 = createForeignKey(runId, "ID");

    public final com.querydsl.sql.ForeignKey<QGoogleSearch> constraint6e22267 = createForeignKey(googleSearchId, "ID");

    public QGoogleRank(String variable) {
        super(QGoogleRank.class, forVariable(variable), "PUBLIC", "GOOGLE_RANK");
        addMetadata();
    }

    public QGoogleRank(String variable, String schema, String table) {
        super(QGoogleRank.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QGoogleRank(String variable, String schema) {
        super(QGoogleRank.class, forVariable(variable), schema, "GOOGLE_RANK");
        addMetadata();
    }

    public QGoogleRank(Path<? extends QGoogleRank> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "GOOGLE_RANK");
        addMetadata();
    }

    public QGoogleRank(PathMetadata metadata) {
        super(QGoogleRank.class, metadata, "PUBLIC", "GOOGLE_RANK");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(diff, ColumnMetadata.named("DIFF").withIndex(7).ofType(Types.SMALLINT).withSize(5));
        addMetadata(googleSearchId, ColumnMetadata.named("GOOGLE_SEARCH_ID").withIndex(4).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(googleTargetId, ColumnMetadata.named("GOOGLE_TARGET_ID").withIndex(3).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(groupId, ColumnMetadata.named("GROUP_ID").withIndex(2).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(previousRank, ColumnMetadata.named("PREVIOUS_RANK").withIndex(6).ofType(Types.SMALLINT).withSize(5));
        addMetadata(rank, ColumnMetadata.named("RANK").withIndex(5).ofType(Types.SMALLINT).withSize(5));
        addMetadata(runId, ColumnMetadata.named("RUN_ID").withIndex(1).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(url, ColumnMetadata.named("URL").withIndex(8).ofType(Types.VARCHAR).withSize(256));
    }

}

