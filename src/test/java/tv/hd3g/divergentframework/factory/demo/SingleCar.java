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
package tv.hd3g.divergentframework.factory.demo;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import tv.hd3g.divergentframework.factory.annotations.ConfigurableValidator;
import tv.hd3g.divergentframework.factory.annotations.OnAfterInjectConfiguration;
import tv.hd3g.divergentframework.factory.annotations.OnAfterUpdateConfiguration;
import tv.hd3g.divergentframework.factory.annotations.OnBeforeRemovedInConfiguration;
import tv.hd3g.divergentframework.factory.annotations.OnBeforeUpdateConfiguration;
import tv.hd3g.divergentframework.factory.annotations.TargetGenericClassType;
import tv.hd3g.divergentframework.factory.validation.NotEmptyNotZeroValidator;

public class SingleCar {
	
	@ConfigurableValidator(NotEmptyNotZeroValidator.class)
	private String color;
	
	@ConfigurableValidator(NotEmptyNotZeroValidator.class)
	private float size;
	
	@TargetGenericClassType(String.class)
	@ConfigurableValidator(NotEmptyNotZeroValidator.class)
	private ArrayList<String> passager_names;
	
	@TargetGenericClassType(Wheel.class)
	private ArrayList<Wheel> possible_wheel_type;
	
	@TargetGenericClassType(Point.class)
	private Map<String, Point> points_by_names;
	
	public enum WheelType {
		tractor, formula1, suv, sedan, truck;
	}
	
	public static class Wheel {
		int size;
		WheelType type;
		
		@OnBeforeRemovedInConfiguration
		private void callbackOnBeforeRemovedInConfiguration() {
			counter_BeforeRemovedInConfiguration.getAndIncrement();
		}
		
		public final AtomicInteger counter_BeforeRemovedInConfiguration = new AtomicInteger();
		
	}
	
	public SingleCar() {
	}
	
	/*
	 * Test zone 
	 */
	
	public SingleCar(Void nothing) {
		throw new RuntimeException("Not this constructor");
	}
	
	public String getColor() {
		return color;
	}
	
	public ArrayList<Wheel> getPossible_wheel_type() {
		return possible_wheel_type;
	}
	
	public ArrayList<String> getPassager_names() {
		return passager_names;
	}
	
	public Map<String, Point> getPoints_by_names() {
		return points_by_names;
	}
	
	public float getSize() {
		return size;
	}
	
	public final AtomicInteger counter_AfterInjectConfiguration = new AtomicInteger();
	public final AtomicInteger counter_AfterUpdateConfiguration = new AtomicInteger();
	public final AtomicInteger counter_BeforeUpdateConfiguration = new AtomicInteger();
	
	@OnAfterInjectConfiguration
	private void callbackOnAfterInjectConfiguration() {
		counter_AfterInjectConfiguration.getAndIncrement();
	}
	
	@OnAfterUpdateConfiguration
	private void callbackOnAfterUpdateConfiguration() {
		counter_AfterUpdateConfiguration.getAndIncrement();
	}
	
	@OnBeforeUpdateConfiguration
	private void callbackOnBeforeUpdateConfiguration() {
		counter_BeforeUpdateConfiguration.getAndIncrement();
	}
	
}
