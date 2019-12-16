package com.serphacker.serposcope.querybuilder;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QEvent is a Querydsl query type for QEvent
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QEvent extends com.querydsl.sql.RelationalPathBase<QEvent> {

    private static final long serialVersionUID = -2104166066;

    public static final QEvent event = new QEvent("EVENT");

    public final DatePath<java.sql.Date> day = createDate("day", java.sql.Date.class);

    public final StringPath description = createString("description");

    public final NumberPath<Integer> groupId = createNumber("groupId", Integer.class);

    public final StringPath title = createString("title");

    public final com.querydsl.sql.PrimaryKey<QEvent> constraint3 = createPrimaryKey(day, groupId);

    public final com.querydsl.sql.ForeignKey<QGroup> constraint3f = createForeignKey(groupId, "ID");

    public QEvent(String variable) {
        super(QEvent.class, forVariable(variable), "PUBLIC", "EVENT");
        addMetadata();
    }

    public QEvent(String variable, String schema, String table) {
        super(QEvent.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QEvent(String variable, String schema) {
        super(QEvent.class, forVariable(variable), schema, "EVENT");
        addMetadata();
    }

    public QEvent(Path<? extends QEvent> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "EVENT");
        addMetadata();
    }

    public QEvent(PathMetadata metadata) {
        super(QEvent.class, metadata, "PUBLIC", "EVENT");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(day, ColumnMetadata.named("DAY").withIndex(2).ofType(Types.DATE).withSize(8).notNull());
        addMetadata(description, ColumnMetadata.named("DESCRIPTION").withIndex(4).ofType(Types.CLOB).withSize(2147483647));
        addMetadata(groupId, ColumnMetadata.named("GROUP_ID").withIndex(1).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(title, ColumnMetadata.named("TITLE").withIndex(3).ofType(Types.VARCHAR).withSize(255));
    }

}

