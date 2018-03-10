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
package tv.hd3g.divergentframework.factory.annotations;

/**
 * Only Configurable class will be setup with an external Configuration
 */
@SingleInstance
public interface Configurable {
	
	/**
	 * @return a name for overload the class name
	 */
	public default String configurationName() {
		return null;
	}
	
	public boolean enableConfigurationUpdate();
	
	/**
	 * Only triggered one time by Configurable Object
	 */
	public default void onAfterInjectConfiguration() {
	}
	
	/**
	 * Only triggered after the first Configuration set in a Configurable Object
	 */
	public default void onAfterUpdateConfiguration() {
	}
	
	/**
	 * Only triggered after the first Configuration set in a Configurable Object
	 */
	public default void onBeforeUpdateConfiguration() {
	}
	
}
