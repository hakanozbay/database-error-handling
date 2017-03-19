# Database error handling using Java and Spring

This is an example demonstration on how specific database interaction errors can be captured and identified allowing to write error recoverable code. This example is written in Java 8, using [Spring][] and [Apache Commons Lang][] libraries.

# Overview

There are common database errors occurring that are not properly handled, which jeopardizes the continuing operation of your software, causing manual intervention to resolve issues. Defensive programming techniques should be implemented for self recovery from these errors, making your software behave in a predicted manner. This allows for greater control of your software, a more intelligent workflow, reduction in errors and manual intervention, saving time for your business and reducing support efforts.

The most frequent and unhandled errors that I have come across are deadlocks; duplicates exisitng in the table the software is writing to; data integrity violation (such as null data in non nullable columns); Badly formatted SQL queries

I have devised a solution to help identify which specific database error caused an exception, providing the first step towards self recovery.

# Walkthrough

The solution I have written leverages the Spring JDBC library, writing a wrapper class for their [SQLErrorCodes][] class and loading the relevant database error codes from their [sql-error-codes.xml][] resource file.

The wrapper class I have written is `DatabaseExceptionUtilities`. It integrates with the Spring resources in this manner:

```java
@Value("${database.name}")
	private String databaseName;
	
	private SQLErrorCodes sqlErrorCodes;

	private SQLErrorCodes getSqlErrorCodes()
	{
		if (sqlErrorCodes == null)
			sqlErrorCodes = SQLErrorCodesFactory.getInstance().getErrorCodes(databaseName);
		
		return sqlErrorCodes;
	}
```
The `databaseName` variable is injected by property `database.name` which you can define in a configuration file or as a system property. Based on this value the relevant database error codes are loaded and provided. 

The remainder of the class defines wrapper methods that are error specific which can be called specifically by utilizing classes. An exmaple method defined is to check if the error is about data integrity violations:

```java
public boolean isExceptionADataIntegrityViolation(SQLException exception)
	{
		String errorCode = getErrorCode(exception);
		String sqlState = getSqlState(exception);
		return Arrays.stream(getSqlErrorCodes().getDataIntegrityViolationCodes()).anyMatch(x -> x.equalsIgnoreCase(errorCode) || x.equalsIgnoreCase(sqlState));
	}

	private String getErrorCode(SQLException exception)
	{
		return Integer.toString(exception.getErrorCode());
	}
	
	private String getSqlState(SQLException exception) 
	{
		return exception.getSQLState();
	}
```
The expectaion is for the exception to be of the `SQLException` type. There are the specific methods `getErrorCode()` and `getSQLState()` in this class that are used to retrieve the error codes. These codes are then used to scan through the collection of error codes to match with. A match then identifies the error codes to be of the specific database error type. 

[Spring]: https://spring.io/
[Aapache Commons Lang]: https://commons.apache.org/proper/commons-lang/
[SQLErrorCodes]: https://github.com/spring-projects/spring-framework/blob/master/spring-jdbc/src/main/java/org/springframework/jdbc/support/SQLErrorCodes.java
[sql-error-codes.xml]: https://github.com/spring-projects/spring-framework/blob/master/spring-jdbc/src/main/resources/org/springframework/jdbc/support/sql-error-codes.xml

