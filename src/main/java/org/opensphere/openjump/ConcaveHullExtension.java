/* 
 * This file is a plugin for OpenJUMP and part of the OpenSphere project.
 *  
 * Copyright (C) 2012 Eric Grosso
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * For more information, contact:
 * Eric Grosso, eric.grosso.os@gmail.com
 * 
 */
package org.opensphere.openjump;

import org.opensphere.openjump.plugin.ConcaveHullPlugin;

import com.vividsolutions.jump.workbench.plugin.Extension;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;


/** 
 * OpenSphere plugins initialisation.
 * 
 * @author Eric Grosso
 * 
 */
public class ConcaveHullExtension extends Extension {
	
	@Override
	public String getName() {
		return "Concave Hull Extension";
	}
	
	@Override
	public String getVersion() {
		return "1.1.0 (2021-08-15)";
	}

	/**
	 * Call the different OpenSphere plugins
	 * 
	 * @param context the plugin context
	 */
	@Override
	public void configure(PlugInContext context) {
		new ConcaveHullPlugin().initialize(context);
	}
	
}