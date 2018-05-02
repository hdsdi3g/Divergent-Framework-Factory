/*
 * This file is part of factory.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2018
 * 
*/
package tv.hd3g.divergentframework.factory;

import java.io.IOException;

import javax.script.ScriptException;

import junit.framework.TestCase;
import tv.hd3g.divergentframework.factory.configuration.demo.SingleCar;

public class FactoryTest extends TestCase {
	
	public void testSimpleNew() {
		SingleCar sc = new SingleCar();
		
		assertNull(sc.getColor());
	}
	
	public void testSimpleFactory() throws ReflectiveOperationException, IOException, ScriptException {
		Factory f = new Factory();
		SingleCar sc = f.create(SingleCar.class);
		
		assertNull(sc.getColor());
		assertNull(sc.getPossible_wheel_type());
		assertNull(sc.getPassager_names());
		assertNull(sc.getPoints_by_names());
	}
	
	public void testFactoryWithConf() throws ReflectiveOperationException, IOException, ScriptException {
		Factory f = new Factory();
		// push conf here, and test it
		SingleCar sc = f.create(SingleCar.class);
		
		assertNull(sc.getColor());
		assertNull(sc.getPossible_wheel_type());
		assertNull(sc.getPassager_names());
		assertNull(sc.getPoints_by_names());
	}
	
	public void testInterfaceImpl() throws ReflectiveOperationException, IOException, ScriptException {
		Factory f = new Factory();
		f.getJsToolkit().setVerboseErrors(System.err);
		
		f.getBindMap().load(FactoryTest.class.getResource("bind_map.properties").openStream());
		
		SimpleInterface result = f.create(SimpleInterface.class);
		assertNotNull(result);
		assertEquals("AZERTY", result.toUpperCase("aZeRtY"));
		assertEquals("java", result.whoami());
		
		f.getBindMap().put(SimpleInterface.class.getName(), FactoryTest.class.getResource("SimpleInterfaceImpl.js").toString());
		result = f.create(SimpleInterface.class);
		assertNotNull(result);
		assertEquals("AZERTY", result.toUpperCase("aZeRtY"));
		assertEquals("javascript", result.whoami());
		
		System.out.println(FactoryTest.class.getResource("SimpleInterfaceImpl.js"));
		
	}
	// test callbacks (first, before next, after next)
	
}
