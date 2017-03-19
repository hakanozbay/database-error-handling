package com.hakanozbay.database.error.handling;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.hakanozbay.database.error.handling.config.TestConfig;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes=TestConfig.class)
public class SpringLoadTest {

	@Autowired
	ApplicationContext applicationContext;
		
	@Test
	public void testContextLoad() 
	{
		assertNotNull(applicationContext);
	}

}
