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

import tv.hd3g.divergentframework.factory.annotations.Configurable;
import tv.hd3g.divergentframework.factory.annotations.ConfigurableValidator;
import tv.hd3g.divergentframework.factory.annotations.ConfigurableVar;
import tv.hd3g.divergentframework.factory.validation.NotNullNotEmptyValidator;

@Configurable(updatable = false)
public class SingleCar {
	
	@ConfigurableVar
	@ConfigurableValidator(NotNullNotEmptyValidator.class)
	private String color;
	
	private boolean valid_constructor = false;
	
	private String dont_configure_me;
	
	public SingleCar() {
		valid_constructor = true;
	}
	
	public SingleCar(Void nothing) {
		throw new RuntimeException("Not this constructor");
	}
	
	public String getColor() {
		return color;
	}
	
	public String getDont_configure_me() {
		return dont_configure_me;
	}
	
	public boolean isValid_constructor() {
		return valid_constructor;
	}
}
