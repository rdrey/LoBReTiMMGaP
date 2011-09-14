package org.mobiloc.lobgasp.osm.parser;

import java.util.ArrayList;
import java.util.List;
import org.mobiloc.lobgasp.osm.parser.model.Member;
import org.mobiloc.lobgasp.osm.parser.model.OSM;
import org.mobiloc.lobgasp.osm.parser.model.Relation;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author zuq
 */
public class RelationParser {

    public static boolean isRelation(Node node) {
        return node.getNodeName().equals("relation");
    }

    public static Relation parseRelation(OSM osm, Node node) {
        NamedNodeMap atts = node.getAttributes();

        String id = atts.getNamedItem("id").getNodeValue();

        return new Relation(osm, id,
                getAttribute(atts, "visible"),
                getAttribute(atts, "timestamp"),
                getAttribute(atts, "version"),
                getAttribute(atts, "changeset"),
                getAttribute(atts, "user"),
                getAttribute(atts, "uid"),
                getMembers(node.getChildNodes()),
                OSMParser.parseTags(node.getChildNodes()));
    }

    // Private Methods ---------------------------------------------------------

    private static String getAttribute(NamedNodeMap atts, String key) {
        Node node = atts.getNamedItem(key);
        return (node == null) ? null : node.getNodeValue();
    }

    private static List<Member> getMembers(NodeList children) {
        List<Member> result;
        Node node;
        NamedNodeMap map;

        result = new ArrayList<Member>();

        for (int i = 0; i < children.getLength(); i++) {
            node = children.item(i);
            map = node.getAttributes();

            if (node.getNodeName().equals("member")) {
                result.add(new Member(
                        map.getNamedItem("type").getNodeValue(),
                        map.getNamedItem("ref").getNodeValue(),
                        map.getNamedItem("role").getNodeValue()));
            }
        }

        return result;
    }
}
