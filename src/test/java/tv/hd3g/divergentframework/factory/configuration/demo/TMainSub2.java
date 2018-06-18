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
package tv.hd3g.divergentframework.factory.configuration.demo;

import java.util.Map;
import java.util.UUID;

import tv.hd3g.divergentframework.factory.configuration.annotations.TargetGenericClassType;
import tv.hd3g.divergentframework.factory.configuration.demo.TMainSub.Counters;

public class TMainSub2 {
	
	@TargetGenericClassType(Sub2A.class)
	public Map<String, Sub2A> sub_a;
	
	public static class Sub2A extends Counters {
		
		@TargetGenericClassType(Sub2B.class)
		public Map<String, Sub2B> sub_b;
		
		public static class Sub2B extends Counters {
			
			@TargetGenericClassType(Sub2C.class)
			public Map<String, Sub2C> sub_c;
			
		}
	}
	
	public static class Sub2C extends Counters {
		public UUID uuid;
	}
	
}
