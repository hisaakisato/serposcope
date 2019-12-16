package com.serphacker.serposcope.querybuilder;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QGoogleSerp is a Querydsl query type for QGoogleSerp
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QGoogleSerp extends com.querydsl.sql.RelationalPathBase<QGoogleSerp> {

    private static final long serialVersionUID = -721655883;

    public static final QGoogleSerp googleSerp = new QGoogleSerp("GOOGLE_SERP");

    public final NumberPath<Integer> googleSearchId = createNumber("googleSearchId", Integer.class);

    public final NumberPath<Long> results = createNumber("results", Long.class);

    public final DateTimePath<java.sql.Timestamp> runDay = createDateTime("runDay", java.sql.Timestamp.class);

    public final NumberPath<Integer> runId = createNumber("runId", Integer.class);

    public final SimplePath<java.sql.Blob> serp = createSimple("serp", java.sql.Blob.class);

    public final com.querydsl.sql.PrimaryKey<QGoogleSerp> constraint6 = createPrimaryKey(googleSearchId, runId);

    public final com.querydsl.sql.ForeignKey<QGoogleSearch> constraint6e = createForeignKey(googleSearchId, "ID");

    public QGoogleSerp(String variable) {
        super(QGoogleSerp.class, forVariable(variable), "PUBLIC", "GOOGLE_SERP");
        addMetadata();
    }

    public QGoogleSerp(String variable, String schema, String table) {
        super(QGoogleSerp.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QGoogleSerp(String variable, String schema) {
        super(QGoogleSerp.class, forVariable(variable), schema, "GOOGLE_SERP");
        addMetadata();
    }

    public QGoogleSerp(Path<? extends QGoogleSerp> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "GOOGLE_SERP");
        addMetadata();
    }

    public QGoogleSerp(PathMetadata metadata) {
        super(QGoogleSerp.class, metadata, "PUBLIC", "GOOGLE_SERP");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(googleSearchId, ColumnMetadata.named("GOOGLE_SEARCH_ID").withIndex(2).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(results, ColumnMetadata.named("RESULTS").withIndex(4).ofType(Types.BIGINT).withSize(19));
        addMetadata(runDay, ColumnMetadata.named("RUN_DAY").withIndex(3).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(runId, ColumnMetadata.named("RUN_ID").withIndex(1).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(serp, ColumnMetadata.named("SERP").withIndex(5).ofType(Types.BLOB).withSize(2147483647));
    }

}

