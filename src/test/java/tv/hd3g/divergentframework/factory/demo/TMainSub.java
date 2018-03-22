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
package tv.hd3g.divergentframework.factory.demo;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import tv.hd3g.divergentframework.factory.annotations.OnAfterInjectConfiguration;
import tv.hd3g.divergentframework.factory.annotations.OnAfterUpdateConfiguration;
import tv.hd3g.divergentframework.factory.annotations.OnBeforeRemovedInConfiguration;
import tv.hd3g.divergentframework.factory.annotations.OnBeforeUpdateConfiguration;
import tv.hd3g.divergentframework.factory.annotations.TargetGenericClassType;

public class TMainSub {
	
	@TargetGenericClassType(SubA.class)
	public Map<String, SubA> sub_a;
	
	public static class SubA extends Counters {
		
		@TargetGenericClassType(SubB.class)
		public Map<String, SubB> sub_b;
		
		public static class SubB extends Counters {
			
			@TargetGenericClassType(SubC.class)
			public Map<String, SubC> sub_c;
			
		}
	}
	
	public static class SubC extends Counters {
		public UUID uuid;
	}
	
	public static abstract class Counters {
		public final AtomicInteger counter_AfterInjectConfiguration = new AtomicInteger();
		public final AtomicInteger counter_AfterUpdateConfiguration = new AtomicInteger();
		public final AtomicInteger counter_BeforeUpdateConfiguration = new AtomicInteger();
		public final AtomicInteger counter_BeforeRemovedInConfiguration = new AtomicInteger();
		
		@OnBeforeRemovedInConfiguration
		public void callbackOnBeforeRemovedInConfiguration() {
			counter_BeforeRemovedInConfiguration.getAndIncrement();
		}
		
		@OnAfterInjectConfiguration
		public void callbackOnAfterInjectConfiguration() {
			counter_AfterInjectConfiguration.getAndIncrement();
		}
		
		@OnAfterUpdateConfiguration
		public void callbackOnAfterUpdateConfiguration() {
			counter_AfterUpdateConfiguration.getAndIncrement();
		}
		
		@OnBeforeUpdateConfiguration
		public void callbackOnBeforeUpdateConfiguration() {
			counter_BeforeUpdateConfiguration.getAndIncrement();
		}
		
	}
	
}
