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
package org.opensphere.openjump.plugin;

import com.vividsolutions.jump.workbench.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.opensphere.geometry.algorithm.ConcaveHull;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedPlugIn;
import com.vividsolutions.jump.workbench.ui.ErrorDialog;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;

/** 
 * Concave hull plugin.
 * 
 * @author Eric Grosso
 *
 */
// 1.1.0 (2021-08-15) refactoring for new i18n and contextualization
// 1.0.0 (2021-04-20) migration to OpenJUMP2
public class ConcaveHullPlugin extends AbstractPlugIn implements ThreadedPlugIn {

	////////////////////////////////////////////////////////////////////////////////////
	// internationalisation

	private final static  I18N i18n = I18N.getInstance("org.opensphere.openjump");
	
	private final static String MENU = I18N.JUMP.get("ui.MenuNames.TOOLS");
	private final static String MENU_SUBTITLE = I18N.JUMP.get("ui.MenuNames.TOOLS.GENERATE");
	private final static String TITLE = i18n.get("plugin.ConcaveHullPlugin.title");
	private final static String DESCRIPTION = i18n.get("plugin.ConcaveHullPlugin.description");

	private final static String LAYER = i18n.get("plugin.ConcaveHullPlugin.layer");
	private final static String THRESHOLD = i18n.get("plugin.ConcaveHullPlugin.threshold");
	
	private final static String ERROR = i18n.get("error");
	private final static String EMPTY = i18n.get("error.emptyCollection");
	
	////////////////////////////////////////////////////////////////////////////////////

	// dialog interface
	private static MultiInputDialog dialog;

	private double threshold = 10.0;
	
	/** 
	 * Initialisation.
	 * 
	 * @param context the plugin context
	 */
	@Override
	public void initialize(PlugInContext context) {
		context.getFeatureInstaller().addMainMenuPlugin(
				this,
				new String[] { MENU , MENU_SUBTITLE },
				TITLE + "...",
				false,
				null,
				new MultiEnableCheck()
					.add(context.getCheckFactory().createTaskWindowMustBeActiveCheck())
					.add(context.getCheckFactory().createAtLeastNLayersMustExistCheck(1))); 
	}

	/**
	 * Returns the name of the plugin.
	 * 
	 * @return
	 * 		the name of the plugin
	 */
	@Override
	public String getName() {
		return TITLE;
	}
	
	/** 
	 * Global execution.
	 * 
	 * @param context the plugin context
	 * @return true if process is executed, otherwise false
	 */
	@Override
	public boolean execute(PlugInContext context) {
		this.reportNothingToUndoYet(context);
		dialog = new MultiInputDialog(context.getWorkbenchFrame(), getName(), true);
		setDialogValues(dialog, context);
		GUIUtil.centreOnWindow(dialog);
		dialog.setVisible(true);
		if (! dialog.wasOKPressed()) {
			return false;
		}
		return true;
	}	
	
	/** 
	 * Initialisation of the dialog interface.
	 * 
	 * @param dialog the dialog to set plugin parameters
	 * @param context the plugin context
	 */
	private void setDialogValues(MultiInputDialog dialog, PlugInContext context) {
		dialog.setSideBarDescription(DESCRIPTION);

		dialog.addLayerComboBox(LAYER,
				context.getLayerNamePanel().getLayerManager().getLayer(0),
				context.getLayerNamePanel().getLayerManager());	
		
		dialog.addDoubleField(
				THRESHOLD, 
				threshold,
				10,
				THRESHOLD
				);
	}

	/** 
	 * Execution.
	 * 
	 * @param monitor a TaskMonitor
	 * @param context the plugin context
	 */
	@Override
	public void run(TaskMonitor monitor, PlugInContext context) {
		
		createConcaveHull(context);

		System.gc();
	}

	/** 
	 * Validation.
	 * 
	 * @param context the plugin context
	 * @return true if the process can be launched, otherwise false
	 */
	private boolean createConcaveHull(PlugInContext context) {
		
		// layer to process
		Layer layer = dialog.getLayer(LAYER);

		// features of the layer to process
		final FeatureCollection collection = layer.getFeatureCollectionWrapper();

		// if the collection is empty
		if (collection.isEmpty()) {
			ErrorDialog.show(context.getWorkbenchFrame(),
					ERROR,
					EMPTY,
					"");
			Logger.warn(EMPTY);
			return false;
		}

		// create concave hull
		createConcaveHull(context, layer, collection);
		
		return true;
	}

	
	/** 
	 * Create a new {@link Layer} containing the concave hull of the {@link Feature}s.
	 * 
	 * @param context
	 * 		context
	 * @param layer 
	 * 		layer to process
	 * @param collection
	 * 		collection of features
	 * @return
	 * 		true
	 */
	private boolean createConcaveHull(PlugInContext context, Layer layer, FeatureCollection collection) {

		threshold = dialog.getDouble(THRESHOLD);

		int size = collection.size();
		
		// feature collection into geometry collection
		Geometry[] geometries = new Geometry[size];
			
		for (int i = 0 ; i < size ; i++) {
			Feature f = collection.getFeatures().get(i);
			geometries[i] = f.getGeometry();
		}

		GeometryFactory gf = (collection.getFeatures().get(0)).getGeometry().getFactory();

		GeometryCollection gc = new GeometryCollection(
				geometries,
				gf);

		// concave hull creation
		ConcaveHull ch = new ConcaveHull(gc, threshold);
		Geometry gch = ch.getConcaveHull();

		// display
		String layerNameCH = layer.getName() + "_concave_hull";

		FeatureSchema fs = new FeatureSchema();
		fs.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
		fs.addAttribute("ID", AttributeType.INTEGER);
		fs.addAttribute("COMMENT", AttributeType.STRING);
		
		FeatureDataset dsCH = new FeatureDataset(fs);
		
		Feature feature = new BasicFeature(fs);
		feature.setGeometry(gch);
		feature.setAttribute("ID", 1);
		dsCH.add(feature);
		
		context.addLayer(StandardCategoryNames.RESULT, layerNameCH, dsCH);

		return true;
	}
	
}