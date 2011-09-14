/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mobiloc.lobgasp.osm.parser;

import org.mobiloc.lobgasp.osm.parser.model.OSMNode;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 *
 * @author zuq
 */
public class NodeParser {

    public static boolean isNode(Node node) {
        return (node.getNodeName().equals("node"));
    }

    public static OSMNode parseNode(Node node) {
        NamedNodeMap atts = node.getAttributes();

        String id = atts.getNamedItem("id").getNodeValue();

        OSMNode osmNode = new OSMNode(id,
                getAttribute(atts, "visible"),
                getAttribute(atts, "timestamp"),
                getAttribute(atts, "version"),
                getAttribute(atts, "changeset"),
                getAttribute(atts, "user"),
                getAttribute(atts, "uid"),
                getAttribute(atts, "lat"),
                getAttribute(atts, "lon"),
                OSMParser.parseTags(node.getChildNodes()));
        
        return osmNode;
    }

    // Private Methods ---------------------------------------------------------
    
    private static String getAttribute(NamedNodeMap atts, String key) {
        Node node = atts.getNamedItem(key);
        return (node == null) ? null : node.getNodeValue();
    }
}
