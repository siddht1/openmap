// **********************************************************************
// 
// <copyright>
// 
//  BBN Technologies, a Verizon Company
//  10 Moulton Street
//  Cambridge, MA 02138
//  (617) 873-8000
// 
//  Copyright (C) BBNT Solutions LLC. All rights reserved.
// 
// </copyright>
// **********************************************************************
// 
// $Source: /cvs/distapps/openmap/src/openmap/com/bbn/openmap/examples/simple/SimpleMap2.java,v $
// $RCSfile: SimpleMap2.java,v $
// $Revision: 1.1.1.1 $
// $Date: 2003/02/14 21:35:48 $
// $Author: dietrick $
// 
// **********************************************************************


package com.bbn.openmap.examples.simple;

import java.awt.BorderLayout;
import java.util.Properties;

import com.bbn.openmap.LatLonPoint;
import com.bbn.openmap.Layer;
import com.bbn.openmap.LayerHandler;
import com.bbn.openmap.MapBean;
import com.bbn.openmap.MapHandler;
import com.bbn.openmap.MultipleSoloMapComponentException;
import com.bbn.openmap.gui.OMToolSet;
import com.bbn.openmap.gui.OpenMapFrame;
import com.bbn.openmap.gui.ToolPanel;
import com.bbn.openmap.layer.GraticuleLayer;
import com.bbn.openmap.layer.shape.ShapeLayer;

/**
 * This is a simple application that uses the OpenMap MapBean to show
 * a map.
 * <p>
 * This example shows:
 * <ul>
 * <li>MapBean
 * <li>MapHandler
 * <li>LayerHandler
 * <li>ShapeLayer with political data
 * <li>GraticuleLayer
 * <li>Local RouteLayer which draws hypothetical routing lines
 * <li>Tools to navigate around on the map
 * </ul>
 * @see RouteLayer
 */
public class SimpleMap2 {

    public static void main(String args[]) {

	// Create a Swing frame.  The OpenMapFrame knows how to use
	// the MapHandler to locate and place certain objects.
	OpenMapFrame frame = new OpenMapFrame("Simple Map 2");
	// Size the frame appropriately
	frame.setSize(640, 480);

	try {

	    MapHandler mapHandler = new MapHandler();
	    mapHandler.add(frame);

	    // Create a MapBean
	    MapBean mapBean = new MapBean();

	    // Set the map's center
	    mapBean.setCenter(new LatLonPoint(43.0f, -95.0f));

	    // Set the map's scale 1:120 million
	    mapBean.setScale(120000000f);

	    mapHandler.add(mapBean);

	    // Create and add a LayerHandler to the MapHandler.  The
	    // LayerHandler manages Layers, whether they are part of the
	    // map or not.  layer.setVisible(true) will add it to the map.
	    // The LayerHandler has methods to do this, too.  The
	    // LayerHandler will find the MapBean in the MapHandler.
	    mapHandler.add(new LayerHandler());

	    // Add a route layer.  
	    RouteLayer routeLayer = new RouteLayer();
	    routeLayer.setVisible(true);
	    // The LayerHandler will find the Layer in the MapHandler.
	    mapHandler.add(routeLayer);

	    mapHandler.add(new GraticuleLayer());

	    // Create a ShapeLayer to show world political boundaries.
	    // Set the properties of the layer.  This assumes that the
	    // datafiles "dcwpo-browse.shp" and "dcwpo-browse.ssx" are in
	    // a path specified in the CLASSPATH variable.  These files
	    // are distributed with OpenMap and reside in the toplevel
	    // "share" subdirectory.
	    ShapeLayer shapeLayer = new ShapeLayer();

	    // Since this Properties object is being used just for
	    // this layer, the properties do not have to be scoped
	    // with marker name, like the layer properties in the
	    // ../hello/HelloWorld.properties file.
	    Properties shapeLayerProps = new Properties();
	    shapeLayerProps.put("prettyName", "Political Solid");
	    shapeLayerProps.put("lineColor", "000000");
	    shapeLayerProps.put("fillColor", "BDDE83");
	    shapeLayerProps.put("shapeFile", "data/shape/dcwpo-browse.shp");
	    shapeLayerProps.put("spatialIndex", "data/shape/dcwpo-browse.ssx");
	    shapeLayer.setProperties(shapeLayerProps);
	    shapeLayer.setVisible(true);

	    mapHandler.add(shapeLayer);
	
	    // Create the directional and zoom control tool	
	    OMToolSet omts = new OMToolSet();
	    // Create an OpenMap toolbar
	    ToolPanel toolBar = new ToolPanel();

	    // Add the ToolPanel and the OMToolSet to the MapHandler.  The
	    // OpenMapFrame will find the ToolPanel and attach it to the
	    // top part of its content pane, and the ToolPanel will find
	    // the OMToolSet and add it to itself.
	    mapHandler.add(omts);
	    mapHandler.add(toolBar);
	    // Display the frame
	    frame.setVisible(true);

	} catch (MultipleSoloMapComponentException msmce) {
	    // The MapHandler is only allowed to have one of certain
	    // items.  These items implement the SoloMapComponent
	    // interface.  The MapHandler can have a policy that
	    // determines what to do when duplicate instances of the
	    // same type of object are added - replace or ignore.

	    // In this example, this will never happen, since we are
	    // controlling that one MapBean, LayerHandler,
	    // MouseDelegator, etc is being added to the MapHandler.
	}
    }
}