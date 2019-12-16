package com.serphacker.serposcope.querybuilder;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QUser is a Querydsl query type for QUser
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QUser extends com.querydsl.sql.RelationalPathBase<QUser> {

    private static final long serialVersionUID = -1729970537;

    public static final QUser user = new QUser("USER");

    public final BooleanPath admin = createBoolean("admin");

    public final StringPath email = createString("email");

    public final NumberPath<Integer> id = createNumber("id", Integer.class);

    public final DateTimePath<java.sql.Timestamp> logout = createDateTime("logout", java.sql.Timestamp.class);

    public final StringPath name = createString("name");

    public final SimplePath<java.sql.Blob> passwordHash = createSimple("passwordHash", java.sql.Blob.class);

    public final SimplePath<java.sql.Blob> passwordSalt = createSimple("passwordSalt", java.sql.Blob.class);

    public final com.querydsl.sql.PrimaryKey<QUser> constraint2 = createPrimaryKey(id);

    public final com.querydsl.sql.ForeignKey<QUserGroup> _constraintC6 = createInvForeignKey(id, "USER_ID");

    public QUser(String variable) {
        super(QUser.class, forVariable(variable), "PUBLIC", "USER");
        addMetadata();
    }

    public QUser(String variable, String schema, String table) {
        super(QUser.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QUser(String variable, String schema) {
        super(QUser.class, forVariable(variable), schema, "USER");
        addMetadata();
    }

    public QUser(Path<? extends QUser> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "USER");
        addMetadata();
    }

    public QUser(PathMetadata metadata) {
        super(QUser.class, metadata, "PUBLIC", "USER");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(admin, ColumnMetadata.named("ADMIN").withIndex(6).ofType(Types.BOOLEAN).withSize(1));
        addMetadata(email, ColumnMetadata.named("EMAIL").withIndex(3).ofType(Types.VARCHAR).withSize(255));
        addMetadata(id, ColumnMetadata.named("ID").withIndex(1).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(logout, ColumnMetadata.named("LOGOUT").withIndex(7).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(name, ColumnMetadata.named("NAME").withIndex(2).ofType(Types.VARCHAR).withSize(255));
        addMetadata(passwordHash, ColumnMetadata.named("PASSWORD_HASH").withIndex(4).ofType(Types.BLOB).withSize(2147483647));
        addMetadata(passwordSalt, ColumnMetadata.named("PASSWORD_SALT").withIndex(5).ofType(Types.BLOB).withSize(2147483647));
    }

}

