package com.serphacker.serposcope.querybuilder;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QConfig is a Querydsl query type for QConfig
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QConfig extends com.querydsl.sql.RelationalPathBase<QConfig> {

    private static final long serialVersionUID = -868101362;

    public static final QConfig config = new QConfig("CONFIG");

    public final StringPath name = createString("name");

    public final StringPath value = createString("value");

    public final com.querydsl.sql.PrimaryKey<QConfig> constraint7 = createPrimaryKey(name);

    public QConfig(String variable) {
        super(QConfig.class, forVariable(variable), "PUBLIC", "CONFIG");
        addMetadata();
    }

    public QConfig(String variable, String schema, String table) {
        super(QConfig.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QConfig(String variable, String schema) {
        super(QConfig.class, forVariable(variable), schema, "CONFIG");
        addMetadata();
    }

    public QConfig(Path<? extends QConfig> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "CONFIG");
        addMetadata();
    }

    public QConfig(PathMetadata metadata) {
        super(QConfig.class, metadata, "PUBLIC", "CONFIG");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(name, ColumnMetadata.named("NAME").withIndex(1).ofType(Types.VARCHAR).withSize(255).notNull());
        addMetadata(value, ColumnMetadata.named("VALUE").withIndex(2).ofType(Types.CLOB).withSize(2147483647));
    }

}

