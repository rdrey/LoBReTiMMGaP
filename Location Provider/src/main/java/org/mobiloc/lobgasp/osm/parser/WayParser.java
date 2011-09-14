package org.mobiloc.lobgasp.osm.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.mobiloc.lobgasp.osm.parser.model.OSMNode;
import org.mobiloc.lobgasp.osm.parser.model.Way;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author zuq
 */
public class WayParser {

    public static boolean isWay(Node node) {
        return node.getNodeName().equals("way");
    }

    public static Way parseWay(Node wayNode, Map<String, OSMNode> nodes) {
        Way way;

        NamedNodeMap atts = wayNode.getAttributes();

        String id = atts.getNamedItem("id").getNodeValue();

        way = new Way(
                id,
                getAttribute(atts, "visible"),
                getAttribute(atts, "timestamp"),
                getAttribute(atts, "version"),
                getAttribute(atts, "changeset"),
                getAttribute(atts, "user"),
                getAttribute(atts, "uid"),
                getNodes(wayNode.getChildNodes(), nodes),
                OSMParser.parseTags(wayNode.getChildNodes()));

        return way;
    }

    // Private Methods ---------------------------------------------------------

    private static String getAttribute(NamedNodeMap atts, String key) {
        Node node = atts.getNamedItem(key);
        return (node == null) ? null : node.getNodeValue();
    }

    private static List<OSMNode> getNodes(NodeList children, Map<String, OSMNode> nodes) {
        List<OSMNode> result = new ArrayList<OSMNode>();

        Node node;
        String nodeName;
        
        for (int i = 0; i < children.getLength(); i++) {
            
            node = children.item(i);
            nodeName = node.getNodeName();

            if (nodeName.equals("nd")) {

                result.add(nodes.get(node.getAttributes().
                        getNamedItem("ref").getNodeValue()));
                
            }
        }

        return result;
    }
}
