package com.hakanozbay.database.error.handling;

import java.sql.SQLException;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.support.SQLErrorCodes;
import org.springframework.jdbc.support.SQLErrorCodesFactory;
import org.springframework.stereotype.Component;

@Component
public class DatabaseExceptionUtilities {
	
	@Value("${database.name}")
	private String databaseName;
	
	private SQLErrorCodes sqlErrorCodes;
	
	public boolean isExceptionADatabaseError(Exception exception)
	{
		return (exception instanceof SQLException);
	}
	
	public boolean isExceptionADeadlock(SQLException exception)
	{
		String errorCode = getErrorCode(exception);
		String sqlState = getSqlState(exception);
		return Arrays.stream(getSqlErrorCodes().getDeadlockLoserCodes()).anyMatch(x -> x.equalsIgnoreCase(errorCode) || x.equalsIgnoreCase(sqlState));
	}
	
	public boolean isExceptionADuplicate(SQLException exception)
	{
		String errorCode = getErrorCode(exception);
		String sqlState = getSqlState(exception);
		return Arrays.stream(getSqlErrorCodes().getDuplicateKeyCodes()).anyMatch(x -> x.equalsIgnoreCase(errorCode) || x.equalsIgnoreCase(sqlState));
	}
	
	public boolean isExceptionADataIntegrityViolation(SQLException exception)
	{
		String errorCode = getErrorCode(exception);
		String sqlState = getSqlState(exception);
		return Arrays.stream(getSqlErrorCodes().getDataIntegrityViolationCodes()).anyMatch(x -> x.equalsIgnoreCase(errorCode) || x.equalsIgnoreCase(sqlState));
	}
	
	public boolean isExceptionBadGrammerSQL(SQLException exception)
	{
		String errorCode = getErrorCode(exception);
		String sqlState = getSqlState(exception);
		return Arrays.stream(getSqlErrorCodes().getBadSqlGrammarCodes()).anyMatch(x -> x.equalsIgnoreCase(errorCode) || x.equalsIgnoreCase(sqlState));
	}
	
	private String getErrorCode(SQLException exception)
	{
		return Integer.toString(exception.getErrorCode());
	}
	
	private String getSqlState(SQLException exception) 
	{
		return exception.getSQLState();
	}
	
	private SQLErrorCodes getSqlErrorCodes()
	{
		if (sqlErrorCodes == null)
			sqlErrorCodes = SQLErrorCodesFactory.getInstance().getErrorCodes(databaseName);
		
		return sqlErrorCodes;
	}

}
