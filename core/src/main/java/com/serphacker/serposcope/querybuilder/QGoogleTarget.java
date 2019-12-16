package com.serphacker.serposcope.querybuilder;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QGoogleTarget is a Querydsl query type for QGoogleTarget
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QGoogleTarget extends com.querydsl.sql.RelationalPathBase<QGoogleTarget> {

    private static final long serialVersionUID = -1996639242;

    public static final QGoogleTarget googleTarget = new QGoogleTarget("GOOGLE_TARGET");

    public final NumberPath<Integer> groupId = createNumber("groupId", Integer.class);

    public final NumberPath<Integer> id = createNumber("id", Integer.class);

    public final StringPath name = createString("name");

    public final StringPath pattern = createString("pattern");

    public final NumberPath<Byte> patternType = createNumber("patternType", Byte.class);

    public final com.querydsl.sql.PrimaryKey<QGoogleTarget> constraint71 = createPrimaryKey(id);

    public final com.querydsl.sql.ForeignKey<QGroup> constraint719 = createForeignKey(groupId, "ID");

    public final com.querydsl.sql.ForeignKey<QGoogleRankBest> _constraintB72 = createInvForeignKey(id, "GOOGLE_TARGET_ID");

    public final com.querydsl.sql.ForeignKey<QGoogleTargetSummary> _constraint41d3 = createInvForeignKey(id, "GOOGLE_TARGET_ID");

    public final com.querydsl.sql.ForeignKey<QGoogleRank> _constraint6e2226 = createInvForeignKey(id, "GOOGLE_TARGET_ID");

    public QGoogleTarget(String variable) {
        super(QGoogleTarget.class, forVariable(variable), "PUBLIC", "GOOGLE_TARGET");
        addMetadata();
    }

    public QGoogleTarget(String variable, String schema, String table) {
        super(QGoogleTarget.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QGoogleTarget(String variable, String schema) {
        super(QGoogleTarget.class, forVariable(variable), schema, "GOOGLE_TARGET");
        addMetadata();
    }

    public QGoogleTarget(Path<? extends QGoogleTarget> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "GOOGLE_TARGET");
        addMetadata();
    }

    public QGoogleTarget(PathMetadata metadata) {
        super(QGoogleTarget.class, metadata, "PUBLIC", "GOOGLE_TARGET");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(groupId, ColumnMetadata.named("GROUP_ID").withIndex(2).ofType(Types.INTEGER).withSize(10));
        addMetadata(id, ColumnMetadata.named("ID").withIndex(1).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(name, ColumnMetadata.named("NAME").withIndex(3).ofType(Types.VARCHAR).withSize(255));
        addMetadata(pattern, ColumnMetadata.named("PATTERN").withIndex(5).ofType(Types.VARCHAR).withSize(255));
        addMetadata(patternType, ColumnMetadata.named("PATTERN_TYPE").withIndex(4).ofType(Types.TINYINT).withSize(3));
    }

}

