package com.serphacker.serposcope.querybuilder;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QGroup is a Querydsl query type for QGroup
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QGroup extends com.querydsl.sql.RelationalPathBase<QGroup> {

    private static final long serialVersionUID = -2102428365;

    public static final QGroup group = new QGroup("GROUP");

    public final BooleanPath fridayEnabled = createBoolean("fridayEnabled");

    public final NumberPath<Integer> id = createNumber("id", Integer.class);

    public final NumberPath<Integer> moduleId = createNumber("moduleId", Integer.class);

    public final BooleanPath mondayEnabled = createBoolean("mondayEnabled");

    public final StringPath name = createString("name");

    public final NumberPath<Integer> ownerId = createNumber("ownerId", Integer.class);

    public final BooleanPath saturdayEnabled = createBoolean("saturdayEnabled");

    public final BooleanPath shared = createBoolean("shared");

    public final BooleanPath sundayEnabled = createBoolean("sundayEnabled");

    public final BooleanPath thursdayEnabled = createBoolean("thursdayEnabled");

    public final BooleanPath tuesdayEnabled = createBoolean("tuesdayEnabled");

    public final BooleanPath wednesdayEnabled = createBoolean("wednesdayEnabled");

    public final com.querydsl.sql.PrimaryKey<QGroup> constraint4 = createPrimaryKey(id);

    public final com.querydsl.sql.ForeignKey<QGoogleRank> _constraint6e222 = createInvForeignKey(id, "GROUP_ID");

    public final com.querydsl.sql.ForeignKey<QGoogleTarget> _constraint719 = createInvForeignKey(id, "GROUP_ID");

    public final com.querydsl.sql.ForeignKey<QUserGroup> _constraintC62 = createInvForeignKey(id, "GROUP_ID");

    public final com.querydsl.sql.ForeignKey<QGoogleTargetSummary> _constraint41d = createInvForeignKey(id, "GROUP_ID");

    public final com.querydsl.sql.ForeignKey<QEvent> _constraint3f = createInvForeignKey(id, "GROUP_ID");

    public final com.querydsl.sql.ForeignKey<QGoogleRankBest> _constraintB7 = createInvForeignKey(id, "GROUP_ID");

    public final com.querydsl.sql.ForeignKey<QGoogleSearchGroup> _constraint135 = createInvForeignKey(id, "GROUP_ID");

    public QGroup(String variable) {
        super(QGroup.class, forVariable(variable), "PUBLIC", "GROUP");
        addMetadata();
    }

    public QGroup(String variable, String schema, String table) {
        super(QGroup.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QGroup(String variable, String schema) {
        super(QGroup.class, forVariable(variable), schema, "GROUP");
        addMetadata();
    }

    public QGroup(Path<? extends QGroup> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "GROUP");
        addMetadata();
    }

    public QGroup(PathMetadata metadata) {
        super(QGroup.class, metadata, "PUBLIC", "GROUP");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(fridayEnabled, ColumnMetadata.named("FRIDAY_ENABLED").withIndex(11).ofType(Types.BOOLEAN).withSize(1));
        addMetadata(id, ColumnMetadata.named("ID").withIndex(1).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(moduleId, ColumnMetadata.named("MODULE_ID").withIndex(2).ofType(Types.INTEGER).withSize(10));
        addMetadata(mondayEnabled, ColumnMetadata.named("MONDAY_ENABLED").withIndex(7).ofType(Types.BOOLEAN).withSize(1));
        addMetadata(name, ColumnMetadata.named("NAME").withIndex(3).ofType(Types.VARCHAR).withSize(255));
        addMetadata(ownerId, ColumnMetadata.named("OWNER_ID").withIndex(5).ofType(Types.INTEGER).withSize(10));
        addMetadata(saturdayEnabled, ColumnMetadata.named("SATURDAY_ENABLED").withIndex(12).ofType(Types.BOOLEAN).withSize(1));
        addMetadata(shared, ColumnMetadata.named("SHARED").withIndex(4).ofType(Types.BOOLEAN).withSize(1));
        addMetadata(sundayEnabled, ColumnMetadata.named("SUNDAY_ENABLED").withIndex(6).ofType(Types.BOOLEAN).withSize(1));
        addMetadata(thursdayEnabled, ColumnMetadata.named("THURSDAY_ENABLED").withIndex(10).ofType(Types.BOOLEAN).withSize(1));
        addMetadata(tuesdayEnabled, ColumnMetadata.named("TUESDAY_ENABLED").withIndex(8).ofType(Types.BOOLEAN).withSize(1));
        addMetadata(wednesdayEnabled, ColumnMetadata.named("WEDNESDAY_ENABLED").withIndex(9).ofType(Types.BOOLEAN).withSize(1));
    }

}

