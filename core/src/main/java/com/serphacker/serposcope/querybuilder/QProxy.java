package com.serphacker.serposcope.querybuilder;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QProxy is a Querydsl query type for QProxy
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QProxy extends com.querydsl.sql.RelationalPathBase<QProxy> {

    private static final long serialVersionUID = -2094116574;

    public static final QProxy proxy = new QProxy("PROXY");

    public final NumberPath<Integer> id = createNumber("id", Integer.class);

    public final StringPath instanceId = createString("instanceId");

    public final StringPath ip = createString("ip");

    public final DateTimePath<java.sql.Timestamp> lastCheck = createDateTime("lastCheck", java.sql.Timestamp.class);

    public final StringPath password = createString("password");

    public final NumberPath<Integer> port = createNumber("port", Integer.class);

    public final StringPath region = createString("region");

    public final StringPath remoteIp = createString("remoteIp");

    public final NumberPath<Byte> status = createNumber("status", Byte.class);

    public final NumberPath<Integer> type = createNumber("type", Integer.class);

    public final StringPath user = createString("user");

    public final com.querydsl.sql.PrimaryKey<QProxy> constraint48 = createPrimaryKey(id);

    public QProxy(String variable) {
        super(QProxy.class, forVariable(variable), "PUBLIC", "PROXY");
        addMetadata();
    }

    public QProxy(String variable, String schema, String table) {
        super(QProxy.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QProxy(String variable, String schema) {
        super(QProxy.class, forVariable(variable), schema, "PROXY");
        addMetadata();
    }

    public QProxy(Path<? extends QProxy> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "PROXY");
        addMetadata();
    }

    public QProxy(PathMetadata metadata) {
        super(QProxy.class, metadata, "PUBLIC", "PROXY");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(id, ColumnMetadata.named("ID").withIndex(1).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(instanceId, ColumnMetadata.named("INSTANCE_ID").withIndex(10).ofType(Types.VARCHAR).withSize(32));
        addMetadata(ip, ColumnMetadata.named("IP").withIndex(3).ofType(Types.CLOB).withSize(2147483647));
        addMetadata(lastCheck, ColumnMetadata.named("LAST_CHECK").withIndex(7).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(password, ColumnMetadata.named("PASSWORD").withIndex(6).ofType(Types.CLOB).withSize(2147483647));
        addMetadata(port, ColumnMetadata.named("PORT").withIndex(4).ofType(Types.INTEGER).withSize(10));
        addMetadata(region, ColumnMetadata.named("REGION").withIndex(11).ofType(Types.VARCHAR).withSize(32));
        addMetadata(remoteIp, ColumnMetadata.named("REMOTE_IP").withIndex(9).ofType(Types.VARCHAR).withSize(256));
        addMetadata(status, ColumnMetadata.named("STATUS").withIndex(8).ofType(Types.TINYINT).withSize(3));
        addMetadata(type, ColumnMetadata.named("TYPE").withIndex(2).ofType(Types.INTEGER).withSize(10));
        addMetadata(user, ColumnMetadata.named("USER").withIndex(5).ofType(Types.CLOB).withSize(2147483647));
    }

}

