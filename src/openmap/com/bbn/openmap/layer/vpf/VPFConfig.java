package com.bbn.openmap.layer.vpf;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;

import com.bbn.openmap.LayerHandler;
import com.bbn.openmap.io.FormatException;
import com.bbn.openmap.omGraphics.DrawingAttributes;
import com.bbn.openmap.util.Debug;
import com.bbn.openmap.util.PaletteHelper;

/**
 * A component that can look at the VPF configuration files at the top
 * level of the VPF directory structure, and provide an interface for
 * defining an OpenMap VPFLayer for chosen features.<p>
 *
 * If the VPFConfig is provided a LayerHandler, it will have a button
 * that will create a layer with selected features.  If it doesn't
 * have a LayerHandler, it will provide a button to print out the
 * properties for a VPFLayer for the selected features.  This class
 * can be run in stand-alone mode to create properties.
 */
public class VPFConfig extends JPanel implements ActionListener {

    private static boolean DEBUG = false;

    //Optionally play with line styles.  Possible values are
    //"Angled", "Horizontal", and "None" (the default).
    private boolean playWithLineStyle = false;
    private String lineStyle = "Angled"; 
    protected boolean showAll = false;
    protected boolean standAlone = false;

    public final static String AddFeatureCmd = "AddFeatureCommand";
    public final static String ClearFeaturesCmd = "ClearFeaturesCommand";
    public final static String CreateLayerCmd = "CreateLayerCommand";
    public final static String EMPTY_FEATURE_LIST = null;

    DefaultMutableTreeNode currentFeature = null;

    protected DrawingAttributes drawingAttributes = new DrawingAttributes();
    protected boolean searchByFeature = true;
    protected String paths = "";

    protected HashSet layerCoverageTypes = new HashSet();
    protected HashSet layerFeatureTypes = new HashSet();

    public final static String AREA = "area";
    public final static String TEXT = "text";
    public final static String EDGE = "edge";
    public final static String POINT = "point";
    public final static String CPOINT = "cpoint";
    public final static String EPOINT = "epoint";
    public final static String COMPLEX = "complex";
    public final static String UNKNOWN = "unknown";

    protected Hashtable layerFeatures;
    protected Properties layerProperties;
    protected LayerHandler layerHandler;
    protected LibraryBean libraryBean;

    JButton addFeatureButton;
    JButton clearFeaturesButton;
    JButton createLayerButton;
    JTextArea currentFeatureList;

    LinkedList featureList = new LinkedList();

    public VPFConfig(String[] dataPaths) {
	this(dataPaths, null);
    }

    public VPFConfig(String[] dataPaths, LayerHandler layerHandler) {
	this(dataPaths, layerHandler, false);
    }

    protected VPFConfig(String[] dataPaths, LayerHandler layerHandler,
			boolean standAlone) {

	this.layerHandler = layerHandler;
	this.standAlone = standAlone;

	if (dataPaths != null && dataPaths.length > 0) {
	    StringBuffer buf = new StringBuffer(dataPaths[0]);
	    for (int i = 1; i < dataPaths.length; i++) {
		buf.append(";");
		buf.append(dataPaths[i]);
	    }
	    paths = buf.toString();
	}

        //Create the nodes.
        DefaultMutableTreeNode top = new DefaultMutableTreeNode("VPF Data Libraries");
	try {
	    createNodes(top, dataPaths);
	} catch (FormatException fe) {
	    Debug.output("Caught FormatException reading data: " + fe.getMessage());
	    if (standAlone) {
		System.exit(0);
	    }
	}

	init(top);
    }

    public VPFConfig(LibraryBean lb, LayerHandler layerHandler) {
	this.layerHandler = layerHandler;

        //Create the nodes.
        DefaultMutableTreeNode top = new DefaultMutableTreeNode("VPF Data Libraries");
	try {
	    createNodes(top, lb.getLibrarySelectionTable());
	} catch (FormatException fe) {
	    Debug.output("Caught FormatException reading data: " + fe.getMessage());
	}

	init(top);
    }

    public void init(DefaultMutableTreeNode top) {

	layerFeatures = new Hashtable();

        //Create a tree that allows one selection at a time.
        final JTree tree = new JTree(top);
        tree.getSelectionModel().setSelectionMode
                (TreeSelectionModel.SINGLE_TREE_SELECTION);
	tree.setVisibleRowCount(10);

        //Listen for when the selection changes.
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = 
		    (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

                if (node == null) return;

                Object nodeInfo = node.getUserObject();
                if (node.isLeaf() && nodeInfo instanceof FeatureInfo) {
		    FeatureInfo feature = (FeatureInfo)nodeInfo;
		    currentFeature = node;
		    // enable addToLayer button here.
		    addFeatureButton.setEnabled(true);
                } else {
		    // disable addToLayer button here.
		    addFeatureButton.setEnabled(false);
		}
            }
        });

        if (playWithLineStyle) {
            tree.putClientProperty("JTree.lineStyle", lineStyle);
        }

        //Create the scroll pane and add the tree to it. 
	GridBagLayout outergridbag = new GridBagLayout();
	GridBagConstraints outerc = new GridBagConstraints();

        JScrollPane treeView = new JScrollPane(tree);

	setLayout(outergridbag);

	outerc.fill = GridBagConstraints.BOTH;
	outerc.anchor = GridBagConstraints.WEST;
	outerc.insets = new Insets(10, 10, 10, 10);
	outerc.gridx = GridBagConstraints.REMAINDER;
	outerc.weighty = 1.0;
	outerc.weightx = 1.0;
	outergridbag.setConstraints(treeView, outerc);
	add(treeView);

	// Create the configuration pane
	JPanel configPanel = new JPanel();
	GridBagLayout gridbag = new GridBagLayout();
	GridBagConstraints c = new GridBagConstraints();
	configPanel.setLayout(gridbag);

	c.gridheight = GridBagConstraints.REMAINDER;
	Component da = drawingAttributes.getGUI();
	gridbag.setConstraints(da, c);
	configPanel.add(da);

	c.gridx = 1;
	c.gridheight = 1;
	c.gridy = 0;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.insets = new Insets(0, 5, 0, 5);
	addFeatureButton = new JButton("Add Feature");
	addFeatureButton.addActionListener(this);
	addFeatureButton.setActionCommand(AddFeatureCmd);
	gridbag.setConstraints(addFeatureButton, c);
	configPanel.add(addFeatureButton);
	addFeatureButton.setEnabled(false);

	clearFeaturesButton = new JButton("Clear Features");
	clearFeaturesButton.addActionListener(this);
	clearFeaturesButton.setActionCommand(ClearFeaturesCmd);
	c.gridy = GridBagConstraints.RELATIVE;
	gridbag.setConstraints(clearFeaturesButton, c);
	configPanel.add(clearFeaturesButton);
	clearFeaturesButton.setEnabled(false);

	if (layerHandler != null) {
	    createLayerButton = new JButton("Create Layer");
	} else {
	    createLayerButton = new JButton("Print Properties");
	}
	createLayerButton.addActionListener(this);
	createLayerButton.setActionCommand(CreateLayerCmd);
	gridbag.setConstraints(createLayerButton, c);
	configPanel.add(createLayerButton);
	createLayerButton.setEnabled(false);

	JPanel currentFeatureListPanel = PaletteHelper.createVerticalPanel(" Current Features: ");
	currentFeatureList = new JTextArea(EMPTY_FEATURE_LIST);
	currentFeatureList.setEditable(false);
	JScrollPane featureListScrollPane = new JScrollPane(currentFeatureList);
	featureListScrollPane.setPreferredSize(new Dimension(150, 10));
// 	currentFeatureListPanel.add(currentFeatureList);
	currentFeatureListPanel.add(featureListScrollPane);
	
	c.gridx = 2;
	c.gridy = 0;
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.NORTHWEST;
	c.gridheight = GridBagConstraints.REMAINDER;
	c.fill = GridBagConstraints.BOTH;
	gridbag.setConstraints(currentFeatureListPanel, c);
	configPanel.add(currentFeatureListPanel);

	outerc.fill = GridBagConstraints.HORIZONTAL;
	outerc.weighty = 0;
	outerc.anchor = GridBagConstraints.CENTER;
	outergridbag.setConstraints(configPanel, outerc);
	add(configPanel);
    }

    public void actionPerformed(ActionEvent ae) {
	String command = ae.getActionCommand();

	if (command == AddFeatureCmd) {

	    if (currentFeature != null) {
                FeatureInfo feature = 
		    (FeatureInfo)currentFeature.getUserObject();
		// Save the current DrawingAttributes 
		// settings for the feature.
		feature.drawingAttributes = 
		    (DrawingAttributes)drawingAttributes.clone();
		featureList.add(currentFeature);

		String cfl = currentFeatureList.getText();
		if (featureList.size() == 1) {
		    cfl = feature.toString();
		} else {
		    cfl += "\n" + feature.toString();
		}

		currentFeatureList.setText(cfl);

		currentFeature = null;
		createLayerButton.setEnabled(true);
		addFeatureButton.setEnabled(false);
		clearFeaturesButton.setEnabled(true);
	    } else {
		Debug.error("No feature selected");
	    }
	} else if (command == ClearFeaturesCmd) {
	    featureList.clear();
	    createLayerButton.setEnabled(false);
	    addFeatureButton.setEnabled(false);
	    clearFeaturesButton.setEnabled(false);
	    currentFeatureList.setText(EMPTY_FEATURE_LIST);
	} else if (command == CreateLayerCmd) {
	    if (featureList.size() == 0) {
		Debug.error("No features selected for new VPFLayer");
		createLayerButton.setEnabled(false);
		clearFeaturesButton.setEnabled(false);
		return;
	    }

	    layerProperties = new Properties();

	    layerProperties.put(VPFLayer.pathProperty, paths);
	    layerProperties.put(VPFLayer.searchByFeatureProperty, new Boolean(searchByFeature).toString());

	    // Now, build up coverageTypeProperty and featureTypesProperty
	    // from the linked list of featureNodes...
	    Iterator it = featureList.iterator();
	    while (it.hasNext()) {
		addPropertiesForFeature((DefaultMutableTreeNode)it.next(),
					layerProperties);
	    }
	    
	    // coverageTypeProperty and featureTypesProperty should 
	    // be built from above iteration, should push them into
	    // properties...
	    // List the coverages
	    layerProperties.put(VPFLayer.coverageTypeProperty, 
				stringTogether(layerCoverageTypes.iterator()));
	    // List area/edge/point/text, whatever has been set up 
	    // with the chosen features.
	    layerProperties.put(VPFLayer.featureTypesProperty, 
				stringTogether(layerFeatureTypes.iterator()));

	    // OK, now go through the layerFeature lists for 
	    // area/edge/text/point and add the property listing the
	    // features associated with each type.
	    Enumeration keys = layerFeatures.keys();
	    while (keys.hasMoreElements()) {
		String key = (String) keys.nextElement();
		HashSet featureSet = (HashSet)layerFeatures.get(key);
		layerProperties.put(key, stringTogether(featureSet.iterator()));
	    }

	    if (layerHandler != null) {
		VPFLayer layer = new VPFLayer();
		layer.setProperties(layerProperties);
		layerHandler.addLayer(layer);
	    } else {
		printProperties(layerProperties);
	    }

	    featureList.clear();
	    currentFeatureList.setText(EMPTY_FEATURE_LIST);
	    createLayerButton.setEnabled(false);
	    addFeatureButton.setEnabled(false);
	    clearFeaturesButton.setEnabled(false);
	}
    }

    private void addPropertiesForFeature(DefaultMutableTreeNode featureNode, 
					 Properties layerProperties) {
	FeatureInfo feature = (FeatureInfo)featureNode.getUserObject();
	CoverageInfo coverage = (CoverageInfo)((DefaultMutableTreeNode)featureNode.getParent()).getUserObject();

	// Adding to  coverage list
	layerCoverageTypes.add(coverage.coverageName);
	// Adding area, edge, text, point to list if it doesn't exist.
	layerFeatureTypes.add(feature.featureTypeString);

	// adding feature name to appropriate edge/area/text/point list
	HashSet featureSet = ((HashSet)layerFeatures.get(feature.featureTypeString));

	if (featureSet == null) {
	    // If it's the first category type for the feature
	    featureSet = new HashSet();
	    layerFeatures.put(feature.featureTypeString, featureSet);
	}
	// Add feature to feature type list for edge/area/text/point
	featureSet.add(feature.featureName);
	feature.drawingAttributes.setPropertyPrefix(feature.featureName);
	feature.drawingAttributes.getProperties(layerProperties);
    }

    private void printProperties(Properties props) {
	Enumeration keys = props.propertyNames();
	while (keys.hasMoreElements()) {
	    String key = (String)keys.nextElement();
	    System.out.println(key + "=" + props.getProperty(key));
	}
    }

    private String stringTogether(Iterator it) {
	StringBuffer buf = null;

	while (it.hasNext()) {
	    String val = (String) it.next();
	    
	    if (buf == null) {
		buf = new StringBuffer(val);
	    } else {
		buf.append(" " + val);
	    }
	}

	if (buf == null) {
	    return "";
	} else {
	    return buf.toString();
	}
    }

    private class FeatureInfo {
        public String featureName;
	public String featureDescription;
	public String featureTypeString;
	public int featureType;
	public CoverageTable.FeatureClassRec record;
	public DrawingAttributes drawingAttributes;

        public FeatureInfo(CoverageTable ct, CoverageTable.FeatureClassRec fcr) {
	    record = fcr;
	    
	    featureTypeString = UNKNOWN;
	    if (fcr.type == CoverageTable.TEXT_FEATURETYPE) {
		featureTypeString = TEXT;
	    } else if (fcr.type == CoverageTable.EDGE_FEATURETYPE) {
		featureTypeString = EDGE;
	    } else if (fcr.type == CoverageTable.AREA_FEATURETYPE) {
		featureTypeString = AREA;
	    } else if (fcr.type == CoverageTable.UPOINT_FEATURETYPE) {
		FeatureClassInfo fci = ct.getFeatureClassInfo(fcr.feature_class);
		if (fci == null) {
		    featureTypeString = POINT;
		} else if (fci.getFeatureType() == CoverageTable.EPOINT_FEATURETYPE) {
		    featureTypeString = EPOINT;
		} else if (fci.getFeatureType() == CoverageTable.CPOINT_FEATURETYPE) {
		    featureTypeString = CPOINT;
		} else {
		    featureTypeString = POINT;
		}
	    } else if (fcr.type == CoverageTable.COMPLEX_FEATURETYPE) {
		featureTypeString = COMPLEX;
	    }

	    featureType = fcr.type;
	    featureName = fcr.feature_class;
	    featureDescription = fcr.description;
        }

        public String toString() {
            return featureDescription + " (" + featureTypeString + ")";
        }
    }

    private class CoverageInfo {
        public String coverageName;
	public String coverageDescription;

        public CoverageInfo(CoverageAttributeTable cat, String covName) {
	    coverageName = covName;
	    coverageDescription = cat.getCoverageDescription(covName);
	}

        public String toString() {
            return coverageDescription;
        }
    }

    private boolean addFeatureNodes(DefaultMutableTreeNode coverageNode, CoverageTable ct) {
	int numFeatures = 0;
	Hashtable info = ct.getFeatureTypeInfo();
	for (Enumeration enum = info.elements(); enum.hasMoreElements();) {
	    CoverageTable.FeatureClassRec fcr = (CoverageTable.FeatureClassRec)enum.nextElement();

	    if (fcr.type == CoverageTable.SKIP_FEATURETYPE) {
		continue;
	    }

	    coverageNode.add(new DefaultMutableTreeNode(new FeatureInfo(ct, fcr)));
	    numFeatures++;
	}
	return numFeatures > 0;
    }

    private void addCoverageNodes(DefaultMutableTreeNode libraryNode, CoverageAttributeTable cat) {
	String[] coverages = cat.getCoverageNames();
	for (int covi = 0; covi < coverages.length; covi++) {
	    String coverage = coverages[covi];
	    CoverageInfo covInfo = new CoverageInfo(cat, coverage);
	    DefaultMutableTreeNode covNode = new DefaultMutableTreeNode(covInfo);
	    if (showAll || 
		addFeatureNodes(covNode, cat.getCoverageTable(coverage)) || 
		!cat.isTiledData()) {
		libraryNode.add(covNode);
	    }
	}
    }

    private void createNodes(DefaultMutableTreeNode top, LibrarySelectionTable lst) 
	throws FormatException {

        DefaultMutableTreeNode category = null;

	String[] libraries = lst.getLibraryNames();
	for (int libi = 0; libi < libraries.length; libi++) {
	    String library = libraries[libi];
	    category = new DefaultMutableTreeNode(library);
	    CoverageAttributeTable cat = lst.getCAT(library);
	    top.add(category);
	    addCoverageNodes(category, cat);
	}
    }

    private void createNodes(DefaultMutableTreeNode top, String[] dataPaths) 
	throws FormatException {

        DefaultMutableTreeNode category = null;

	for (int i = 0; i < dataPaths.length; i++) {
	    String rootpath = dataPaths[i];
	    LibrarySelectionTable lst = new LibrarySelectionTable(rootpath);
	    createNodes(top, lst);
	}
    }

    public static void createLayer(String[] vpfPaths, LayerHandler layerHandler) {
	launchFrame(new VPFConfig(vpfPaths, layerHandler), false);
    }

    public static void createLayer(LibraryBean libraryBean, LayerHandler layerHandler) {
	launchFrame(new VPFConfig(libraryBean, layerHandler), false);
    }

    protected static void launchFrame(JComponent content, boolean exitOnClose) {
        JFrame frame = new JFrame("Create VPF Data Layer");

	frame.getContentPane().add(content);
	if (exitOnClose) {
	    frame.addWindowListener(new WindowAdapter() {
		    public void windowClosing(WindowEvent e) {
			System.exit(0);
		    }
		});  
	}

        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
	if (args.length == 0) {
	    System.out.println("Usage:  java com.bbn.openmap.layer.vpf.VPFConfig <path to VPF directory> <path to VPF directory> ...");
	    System.exit(0);
	}

	VPFConfig vpfc = new VPFConfig(args, null, true);
	launchFrame(vpfc, true);
    }
}