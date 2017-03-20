# Database error handling using Java and Spring

This is an example demonstration on how specific database interaction errors can be captured and identified allowing to write error recoverable code. This example is written in Java 8, using [Spring][] and [Apache Commons Lang](https://commons.apache.org/proper/commons-lang/) libraries.

# Overview

There are common database errors occurring that are not properly handled, which jeopardizes the continuing operation of your software, causing manual intervention to resolve issues. Defensive programming techniques should be implemented for self recovery from these errors, making your software behave in a predicted manner. This allows for greater control of your software, a more intelligent workflow, reduction in errors and manual intervention, saving time for your business and reducing support efforts.

The most frequent and unhandled errors that I have come across are deadlocks; duplicates exisitng in the table the software is writing to; data integrity violation (such as null data in non nullable columns); Badly formatted SQL queries

I have devised a solution to help identify which specific database error caused an exception, providing the first step towards self recovery.

# Walkthrough

The solution I have written leverages the Spring JDBC library, writing a wrapper class for their [SQLErrorCodes][] and [SQLErrorCodesFactory][] class and loading the relevant database error codes from their [sql-error-codes.xml][] resource file.

The wrapper class I have written is `DatabaseExceptionUtilities`. It integrates with the Spring resources in this manner:

```java
	@Autowired
	DataSource dataSource;
	
	private SQLErrorCodes sqlErrorCodes;

	private SQLErrorCodes getSqlErrorCodes()
	{
		if (sqlErrorCodes == null)
			sqlErrorCodes = SQLErrorCodesFactory.getInstance().getErrorCodes(dataSource);
		
		return sqlErrorCodes;
	}
```
The `dataSource` variable is a dependency injection from an exisitng dataSource bean that would be created in a Spring configuration file. Based on this value the relevant database error codes are loaded and provided. 

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
The expectation is for the exception to be of the `SQLException` type. There are the specific methods `getErrorCode()` and `getSQLState()` in this class that are used to retrieve the error codes. These codes are then used to scan through the collection of error codes to match with. A match then identifies the error codes to be of the specific database error type. 

As an example implementation of this I have created a `DatabaseService` class that will catch an `SQLException` and handle it when exectuing SQL statements:

```Java
	@Autowired
	DatabaseExceptionUtilities databaseExceptionUtilities;
	
	@Autowired
	DataSource dataSource;
	
	public void executeStatement(String sql)
	{
		try
		{
			dataSource.getConnection().createStatement().execute(sql);
		}
		catch(SQLException e)
		{
			handleDatabaseException(e);
		}
		catch (Exception e)
		{
			if (ExceptionUtils.hasCause(e, SQLException.class))
			{
				Throwable sqlException = ExceptionUtils.getThrowableList(e).get(ExceptionUtils.indexOfType(e, SQLException.class));
				handleDatabaseException((SQLException) sqlException);
			}
		}
	}

	protected void handleDatabaseException(SQLException exception) 
	{
		if (databaseExceptionUtilities.isExceptionBadGrammerSQL(exception))
			System.out.println("Bad Grammar Exception: " + exception.toString());
		
		else if (databaseExceptionUtilities.isExceptionADuplicate(exception))
			System.out.println("Duplicate Exception: " + exception.toString());
		
		else if (databaseExceptionUtilities.isExceptionADeadlock(exception))
			System.out.println("Deadlock Exception: " + exception.toString());
		
		else if (databaseExceptionUtilities.isExceptionADataIntegrityViolation(exception))
			System.out.println("Data Integrity Violation Exception: " + exception.toString());
			
	}
```
The exception can be an inherited sub type of SQLException in which case it can be caught with the first catch statement, or it may be a nested exception in the hierearchy of exceptions that are thrown already, which is where the second catch statement will handle it. Looking at the second catch statement it utilises the [ExceptionUtils][] class from the Apache Commons Lang library:

```java
		catch (Exception e)
		{
			if (ExceptionUtils.hasCause(e, SQLException.class))
			{
				Throwable sqlException = ExceptionUtils.getThrowableList(e).get(ExceptionUtils.indexOfType(e, SQLException.class));
				handleDatabaseException((SQLException) sqlException);
			}
		}
```
It identifies if the `SQLException` type has been thrown in its exception hierarchy. If it has then it retrieves the specific `SQLException` object from the hierarchy to pass onto the handling method.

In the handling method it checks for each particular error type until it finds a match. For simplicity of the demonstration it currently only prints out the identified specific error. At this point is where your error handling code would be defined to ensure self recovery of your software.

# Test walkthrough

There is a `DatabaseServiceTest` that can be run to test the 3 scenarios of bad grammar, duplicate and data integrity violations. 

The `TestConfig` class creates an embedded database (as the dataSource bean) from which you specify to use [H2][], [Derby][] or [HSQL][]. The current implementation works best with H2 and Derby. It will also load in the `schema.sql` file and the 'data.sql' file ( found in the src/test/resources folder) before running the test:

### schema.sql
```sql
CREATE TABLE PERSON(FIRSTNAME VARCHAR(255) NOT NULL,LASTNAME VARCHAR(255) NOT NULL,GENDER VARCHAR(1) NOT NULL,AGE INTEGER NOT NULL)
CREATE UNIQUE INDEX IDX0 ON PERSON(FIRSTNAME,LASTNAME)
```

### data.sql
```sql
INSERT INTO PERSON VALUES('John','Smith','M',35)
INSERT INTO PERSON VALUES('Samantha','Johnson','F',27)
INSERT INTO PERSON VALUES('Thomas','Dempsey','M',63)
INSERT INTO PERSON VALUES('Jane','Peters','F',41)
```

This pre-exisitng state before the test will allow for the duplicate and data integry violation tests to be successful. The tests that are run are:

```java
	@Test
	public void testBadGrammarException() 
	{
		databaseService.executeStatement("hello");
	}
	
	@Test
	public void testDuplicateException() 
	{
		databaseService.executeStatement("INSERT INTO PERSON VALUES('John','Smith','M',35)");
	}
	
	@Test
	public void testDataIntegrityException()
	{
		databaseService.executeStatement("INSERT INTO PERSON VALUES(NULL,NULL,'M',35)");
	}
```

When the tests are run with H2 the output is:

```
Data Integrity Violation Exception: org.h2.jdbc.JdbcSQLException: NULL not allowed for column "FIRSTNAME"; SQL statement:
INSERT INTO PERSON VALUES(NULL,NULL,'M',35) [23502-194]

Duplicate Exception: org.h2.jdbc.JdbcSQLException: Unique index or primary key violation: "IDX0 ON PUBLIC.PERSON(FIRSTNAME, LASTNAME) VALUES ('John', 'Smith', 1)"; SQL statement:
INSERT INTO PERSON VALUES('John','Smith','M',35) [23505-194]

Bad Grammar Exception: org.h2.jdbc.JdbcSQLException: Syntax error in SQL statement "HELLO[*] "; expected "HELP, {"; SQL statement:
hello [42001-194]
```

Similarly with Derby:

```
Data Integrity Violation Exception: java.sql.SQLIntegrityConstraintViolationException: Column 'FIRSTNAME'  cannot accept a NULL value.

Duplicate Exception: org.apache.derby.shared.common.error.DerbySQLIntegrityConstraintViolationException: The statement was aborted because it would have caused a duplicate key value in a unique or primary key constraint or unique index identified by 'IDX0' defined on 'PERSON'.

Bad Grammar Exception: java.sql.SQLSyntaxErrorException: Syntax error: Encountered "hello" at line 1, column 1.

``` 

However when we run it with HSQL there is only 1 message output:

```
Duplicate Exception: java.sql.SQLIntegrityConstraintViolationException: integrity constraint violation: unique constraint or index violation: IDX0
```

The reason for this depends on the both the database that is used and the library used to do the database interaction with. More details in the section below 

# Caveats
Successful implementation of this strategy depends on 2 things: The database you are using and the Database I/O library you using.

The current strategy has worked well when just using Java's native DataSource class  


# Conclusion

[Spring]: https://spring.io/
[Aapache Commons Lang]: https://commons.apache.org/proper/commons-lang/
[SQLErrorCodes]: http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/jdbc/support/SQLErrorCodes.html
[SQLErrorCodesFactory]:http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/jdbc/support/SQLErrorCodesFactory.html
[sql-error-codes.xml]: https://github.com/spring-projects/spring-framework/blob/master/spring-jdbc/src/main/resources/org/springframework/jdbc/support/sql-error-codes.xml
[ExceptionUtils]: https://commons.apache.org/proper/commons-lang/apidocs/org/apache/commons/lang3/exception/ExceptionUtils.html
[H2]: https://www.h2database.com/
[Derby]:https://db.apache.org/derby/
[HSQL]:http://hsqldb.org/
