package com.serphacker.serposcope.querybuilder;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QUserGroup is a Querydsl query type for QUserGroup
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QUserGroup extends com.querydsl.sql.RelationalPathBase<QUserGroup> {

    private static final long serialVersionUID = -487731672;

    public static final QUserGroup userGroup = new QUserGroup("USER_GROUP");

    public final NumberPath<Integer> groupId = createNumber("groupId", Integer.class);

    public final NumberPath<Integer> userId = createNumber("userId", Integer.class);

    public final com.querydsl.sql.PrimaryKey<QUserGroup> constraintC = createPrimaryKey(groupId, userId);

    public final com.querydsl.sql.ForeignKey<QGroup> constraintC62 = createForeignKey(groupId, "ID");

    public final com.querydsl.sql.ForeignKey<QUser> constraintC6 = createForeignKey(userId, "ID");

    public QUserGroup(String variable) {
        super(QUserGroup.class, forVariable(variable), "PUBLIC", "USER_GROUP");
        addMetadata();
    }

    public QUserGroup(String variable, String schema, String table) {
        super(QUserGroup.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QUserGroup(String variable, String schema) {
        super(QUserGroup.class, forVariable(variable), schema, "USER_GROUP");
        addMetadata();
    }

    public QUserGroup(Path<? extends QUserGroup> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "USER_GROUP");
        addMetadata();
    }

    public QUserGroup(PathMetadata metadata) {
        super(QUserGroup.class, metadata, "PUBLIC", "USER_GROUP");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(groupId, ColumnMetadata.named("GROUP_ID").withIndex(2).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(userId, ColumnMetadata.named("USER_ID").withIndex(1).ofType(Types.INTEGER).withSize(10).notNull());
    }

}

