package com.hakanozbay.database.error.handling;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.hakanozbay.database.error.handling.config.TestConfig;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes=TestConfig.class)
public class DatabaseServiceTest {

	@Autowired
	DatabaseService databaseService;
	
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

}
