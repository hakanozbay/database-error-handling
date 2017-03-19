package com.hakanozbay.database.error.handling;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.stereotype.Component;

@Component
public class DatabaseService {

	@Value("${database.name}")
	private String databaseName;
	
	@Autowired
	DatabaseExceptionUtilities databaseExceptionUtilities;
	
	
	public void executeStatement(String sql)
	{
		try
		{
			DataSource databaseConnection = getDatabaseConnection();
			databaseConnection.getConnection().createStatement().execute(sql);
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
	
	protected DataSource getDatabaseConnection() 
	{
		EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
		EmbeddedDatabase db = builder
			.setType(EmbeddedDatabaseType.valueOf(databaseName.toUpperCase()))
			.addDefaultScripts()
			.build();
		return db;
	}
}
