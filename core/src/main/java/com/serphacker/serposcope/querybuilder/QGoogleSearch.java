package com.serphacker.serposcope.querybuilder;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QGoogleSearch is a Querydsl query type for QGoogleSearch
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QGoogleSearch extends com.querydsl.sql.RelationalPathBase<QGoogleSearch> {

    private static final long serialVersionUID = -2022070259;

    public static final QGoogleSearch googleSearch = new QGoogleSearch("GOOGLE_SEARCH");

    public final StringPath country = createString("country");

    public final StringPath customParameters = createString("customParameters");

    public final StringPath datacenter = createString("datacenter");

    public final NumberPath<Byte> device = createNumber("device", Byte.class);

    public final NumberPath<Integer> id = createNumber("id", Integer.class);

    public final StringPath keyword = createString("keyword");

    public final StringPath local = createString("local");

    public final com.querydsl.sql.PrimaryKey<QGoogleSearch> constraint70 = createPrimaryKey(id);

    public final com.querydsl.sql.ForeignKey<QGoogleSearchGroup> _constraint1359 = createInvForeignKey(id, "GOOGLE_SEARCH_ID");

    public final com.querydsl.sql.ForeignKey<QGoogleRank> _constraint6e22267 = createInvForeignKey(id, "GOOGLE_SEARCH_ID");

    public final com.querydsl.sql.ForeignKey<QGoogleSerp> _constraint6e = createInvForeignKey(id, "GOOGLE_SEARCH_ID");

    public final com.querydsl.sql.ForeignKey<QGoogleRankBest> _constraintB727 = createInvForeignKey(id, "GOOGLE_SEARCH_ID");

    public QGoogleSearch(String variable) {
        super(QGoogleSearch.class, forVariable(variable), "PUBLIC", "GOOGLE_SEARCH");
        addMetadata();
    }

    public QGoogleSearch(String variable, String schema, String table) {
        super(QGoogleSearch.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QGoogleSearch(String variable, String schema) {
        super(QGoogleSearch.class, forVariable(variable), schema, "GOOGLE_SEARCH");
        addMetadata();
    }

    public QGoogleSearch(Path<? extends QGoogleSearch> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "GOOGLE_SEARCH");
        addMetadata();
    }

    public QGoogleSearch(PathMetadata metadata) {
        super(QGoogleSearch.class, metadata, "PUBLIC", "GOOGLE_SEARCH");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(country, ColumnMetadata.named("COUNTRY").withIndex(3).ofType(Types.VARCHAR).withSize(2));
        addMetadata(customParameters, ColumnMetadata.named("CUSTOM_PARAMETERS").withIndex(7).ofType(Types.VARCHAR).withSize(255));
        addMetadata(datacenter, ColumnMetadata.named("DATACENTER").withIndex(4).ofType(Types.VARCHAR).withSize(64));
        addMetadata(device, ColumnMetadata.named("DEVICE").withIndex(5).ofType(Types.TINYINT).withSize(3));
        addMetadata(id, ColumnMetadata.named("ID").withIndex(1).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(keyword, ColumnMetadata.named("KEYWORD").withIndex(2).ofType(Types.VARCHAR).withSize(255).notNull());
        addMetadata(local, ColumnMetadata.named("LOCAL").withIndex(6).ofType(Types.VARCHAR).withSize(64));
    }

}

