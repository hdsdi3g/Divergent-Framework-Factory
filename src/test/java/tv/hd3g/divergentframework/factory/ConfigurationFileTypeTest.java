/*
 * This file is part of Divergent Framework Factory.
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
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import junit.framework.TestCase;

public class ConfigurationFileTypeTest extends TestCase {
	
	public void testYAMLLoading() throws IOException {
		StringWriter strOut = new StringWriter();
		
		PrintWriter pw = new PrintWriter(strOut);
		pw.println("truc:");
		pw.println("   a: 1");
		pw.println("   b: \"toto\"");
		pw.println("---");
		pw.println("truc:");
		pw.println("   a: 2");
		
		System.out.println(ConfigurationFileType.YAML.getContent(new StringReader(strOut.toString())));// XXX write real tests
	}
	
}
