package java.sql;

import java.util.Map;

public class SQLClientInfoException extends SQLException
{
    private Map<String, ClientInfoStatus> failedProperties;

    public SQLClientInfoException()
    {
        super();
    }

    public SQLClientInfoException(String reason, Map<String, ClientInfoStatus> failedProperties)
    {
        super(reason);
        this.failedProperties = failedProperties;
    }

    public SQLClientInfoException(String reason, Map<String, ClientInfoStatus> failedProperties,
                                  Throwable cause)
    {
        super(reason, cause);
        this.failedProperties = failedProperties;
    }

    public SQLClientInfoException(String reason, String SQLState,
                                  Map<String, ClientInfoStatus> failedProperties)
    {
        super(reason, SQLState);
        this.failedProperties = failedProperties;
    }

    public SQLClientInfoException(String reason, String SQLState,
                                  Map<String, ClientInfoStatus> failedProperties, Throwable cause)
    {
        super(reason, SQLState, cause);
        this.failedProperties = failedProperties;
    }

    public SQLClientInfoException(String reason, String SQLState, int vendorCode,
                                  Map<String, ClientInfoStatus> failedProperties)
    {
        super(reason, SQLState, vendorCode);
        this.failedProperties = failedProperties;
    }

    public SQLClientInfoException(String reason, String SQLState, int vendorCode,
                                  Map<String, ClientInfoStatus> failedProperties, Throwable cause)
    {
        super(reason, SQLState, vendorCode, cause);
        this.failedProperties = failedProperties;
    }

    public Map<String, ClientInfoStatus> getFailedProperties()
    {
        return failedProperties;
    }
}
