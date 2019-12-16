package com.serphacker.serposcope.querybuilder;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QRun is a Querydsl query type for QRun
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QRun extends com.querydsl.sql.RelationalPathBase<QRun> {

    private static final long serialVersionUID = -194355649;

    public static final QRun run = new QRun("RUN");

    public final NumberPath<Integer> captchas = createNumber("captchas", Integer.class);

    public final DatePath<java.sql.Date> day = createDate("day", java.sql.Date.class);

    public final NumberPath<Integer> done = createNumber("done", Integer.class);

    public final NumberPath<Integer> errors = createNumber("errors", Integer.class);

    public final DateTimePath<java.sql.Timestamp> finished = createDateTime("finished", java.sql.Timestamp.class);

    public final NumberPath<Integer> groupId = createNumber("groupId", Integer.class);

    public final NumberPath<Integer> id = createNumber("id", Integer.class);

    public final NumberPath<Integer> mode = createNumber("mode", Integer.class);

    public final NumberPath<Integer> moduleId = createNumber("moduleId", Integer.class);

    public final NumberPath<Integer> progress = createNumber("progress", Integer.class);

    public final DateTimePath<java.sql.Timestamp> started = createDateTime("started", java.sql.Timestamp.class);

    public final NumberPath<Integer> status = createNumber("status", Integer.class);

    public final NumberPath<Integer> total = createNumber("total", Integer.class);

    public final NumberPath<Integer> userId = createNumber("userId", Integer.class);

    public final com.querydsl.sql.PrimaryKey<QRun> constraint1 = createPrimaryKey(id);

    public final com.querydsl.sql.ForeignKey<QGoogleTargetSummary> _constraint41d36 = createInvForeignKey(id, "RUN_ID");

    public final com.querydsl.sql.ForeignKey<QGoogleRank> _constraint6e22 = createInvForeignKey(id, "RUN_ID");

    public QRun(String variable) {
        super(QRun.class, forVariable(variable), "PUBLIC", "RUN");
        addMetadata();
    }

    public QRun(String variable, String schema, String table) {
        super(QRun.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QRun(String variable, String schema) {
        super(QRun.class, forVariable(variable), schema, "RUN");
        addMetadata();
    }

    public QRun(Path<? extends QRun> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "RUN");
        addMetadata();
    }

    public QRun(PathMetadata metadata) {
        super(QRun.class, metadata, "PUBLIC", "RUN");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(captchas, ColumnMetadata.named("CAPTCHAS").withIndex(9).ofType(Types.INTEGER).withSize(10));
        addMetadata(day, ColumnMetadata.named("DAY").withIndex(3).ofType(Types.DATE).withSize(8));
        addMetadata(done, ColumnMetadata.named("DONE").withIndex(7).ofType(Types.INTEGER).withSize(10));
        addMetadata(errors, ColumnMetadata.named("ERRORS").withIndex(10).ofType(Types.INTEGER).withSize(10));
        addMetadata(finished, ColumnMetadata.named("FINISHED").withIndex(5).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(groupId, ColumnMetadata.named("GROUP_ID").withIndex(14).ofType(Types.INTEGER).withSize(10));
        addMetadata(id, ColumnMetadata.named("ID").withIndex(1).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(mode, ColumnMetadata.named("MODE").withIndex(12).ofType(Types.INTEGER).withSize(10));
        addMetadata(moduleId, ColumnMetadata.named("MODULE_ID").withIndex(2).ofType(Types.INTEGER).withSize(10));
        addMetadata(progress, ColumnMetadata.named("PROGRESS").withIndex(8).ofType(Types.INTEGER).withSize(10));
        addMetadata(started, ColumnMetadata.named("STARTED").withIndex(4).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(status, ColumnMetadata.named("STATUS").withIndex(11).ofType(Types.INTEGER).withSize(10));
        addMetadata(total, ColumnMetadata.named("TOTAL").withIndex(6).ofType(Types.INTEGER).withSize(10));
        addMetadata(userId, ColumnMetadata.named("USER_ID").withIndex(13).ofType(Types.INTEGER).withSize(10));
    }

}

