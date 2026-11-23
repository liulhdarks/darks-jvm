package java.sql;

public interface Types
{
    int BIT = -7;
    int TINYINT = -6;
    int SMALLINT = 5;
    int INTEGER = 4;
    int BIGINT = -5;
    int FLOAT = 6;
    int REAL = 7;
    int DOUBLE = 8;
    int NUMERIC = 2;
    int DECIMAL = 3;
    int CHAR = 1;
    int VARCHAR = 12;
    int LONGVARCHAR = -1;
    int DATE = 91;
    int TIME = 92;
    int TIMESTAMP = 93;
    int BINARY = -2;
    int VARBINARY = -3;
    int LONGVARBINARY = -4;
    int NULL = 0;
    int OTHER = 1111;
    int JAVA_OBJECT = 2000;
    int DISTINCT = 2001;
    int STRUCT = 2002;
    int ARRAY = 2003;
    int BLOB = 2004;
    int CLOB = 2005;
    int REF = 2006;
    int DATALINK = 70;
    int BOOLEAN = 16;
    int ROWID = -8;
    int NCHAR = -15;
    int NVARCHAR = -9;
    int LONGNVARCHAR = -16;
    int NCLOB = 2011;
    int SQLXML = 2009;
    int REF_CURSOR = 2012;
    int TIME_WITH_TIMEZONE = 2013;
    int TIMESTAMP_WITH_TIMEZONE = 2014;
}
