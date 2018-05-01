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
package tv.hd3g.divergentframework.factory.js;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

interface DynamicInterface {
	
	void simple();
	
	String stringSupplier();
	
	void intConsumer(int value);
	
	long biConsumer(long value1, long value2);
	
	int stringToIntFunction(String value);
	
	int varArgs(int... values);
	
	default boolean aDefault() {
		return true;
	}
	
	List<String> stringList(ArrayList<String> values);
	
	Map<String, Integer> intMap(HashMap<String, Integer> values);
	
}
