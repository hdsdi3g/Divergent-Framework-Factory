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
package tv.hd3g.divergentframework.factory.watchfolder;

import tv.hd3g.divergentframework.factory.SingleInstance;
import tv.hd3g.divergentframework.factory.configuration.annotations.OnAfterUpdateConfiguration;
import tv.hd3g.divergentframework.factory.configuration.annotations.OnBeforeUpdateConfiguration;

@SingleInstance
public class WFConfigurationDummy {
	WatchFolder wf;
	
	// XXX remove this debug things
	@OnBeforeUpdateConfiguration
	void onBeforeUpdate() {
		System.out.println("BEFORE UPDATE: wf=" + wf.hashCode());
	}
	
	@OnAfterUpdateConfiguration
	void onAfterUpdate() {
		System.out.println("AFTER UPDATE: wf=" + +wf.hashCode());
	}
	
}
