package com.serphacker.serposcope.querybuilder;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QGoogleSearchGroup is a Querydsl query type for QGoogleSearchGroup
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QGoogleSearchGroup extends com.querydsl.sql.RelationalPathBase<QGoogleSearchGroup> {

    private static final long serialVersionUID = 77356658;

    public static final QGoogleSearchGroup googleSearchGroup = new QGoogleSearchGroup("GOOGLE_SEARCH_GROUP");

    public final NumberPath<Integer> googleSearchId = createNumber("googleSearchId", Integer.class);

    public final NumberPath<Integer> groupId = createNumber("groupId", Integer.class);

    public final com.querydsl.sql.PrimaryKey<QGoogleSearchGroup> constraint13 = createPrimaryKey(googleSearchId, groupId);

    public final com.querydsl.sql.ForeignKey<QGoogleSearch> constraint1359 = createForeignKey(googleSearchId, "ID");

    public final com.querydsl.sql.ForeignKey<QGroup> constraint135 = createForeignKey(groupId, "ID");

    public QGoogleSearchGroup(String variable) {
        super(QGoogleSearchGroup.class, forVariable(variable), "PUBLIC", "GOOGLE_SEARCH_GROUP");
        addMetadata();
    }

    public QGoogleSearchGroup(String variable, String schema, String table) {
        super(QGoogleSearchGroup.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QGoogleSearchGroup(String variable, String schema) {
        super(QGoogleSearchGroup.class, forVariable(variable), schema, "GOOGLE_SEARCH_GROUP");
        addMetadata();
    }

    public QGoogleSearchGroup(Path<? extends QGoogleSearchGroup> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "GOOGLE_SEARCH_GROUP");
        addMetadata();
    }

    public QGoogleSearchGroup(PathMetadata metadata) {
        super(QGoogleSearchGroup.class, metadata, "PUBLIC", "GOOGLE_SEARCH_GROUP");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(googleSearchId, ColumnMetadata.named("GOOGLE_SEARCH_ID").withIndex(1).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(groupId, ColumnMetadata.named("GROUP_ID").withIndex(2).ofType(Types.INTEGER).withSize(10).notNull());
    }

}

