package org.mobiloc.lobgasp.osm.parser.model;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Willy Tiengo
 */
public class OSM {

    private Set<OSMNode> nodes;
    private Set<Way> ways;
    private Set<Relation> relations;

    public OSM() {
        nodes = new HashSet<OSMNode>();
        ways = new HashSet<Way>();
        relations = new HashSet<Relation>();
    }

    public OSM(Set<OSMNode> nodes, Set<Way> ways,
            Set<Relation> relations) {
        this.nodes = nodes;
        this.ways = ways;
        this.relations = relations;
    }

    public Set<OSMNode> getNodes() {
        return nodes;
    }

    public Set<Relation> getRelations() {
        return relations;
    }

    public Set<Way> getWays() {
        return ways;
    }

    public Way getWay(String id) {
        for (Way way : ways) {
            if (way.id.equals(id)) {
                return way;
            }
        }
        return null;
    }
}
