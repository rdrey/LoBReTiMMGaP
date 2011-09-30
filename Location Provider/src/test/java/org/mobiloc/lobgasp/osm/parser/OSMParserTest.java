/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mobiloc.lobgasp.osm.parser;

import junit.framework.TestCase;
import org.mobiloc.lobgasp.osm.parser.model.OSM;

/**
 *
 * @author rainerdreyer
 */
public class OSMParserTest extends TestCase {
    
    public OSMParserTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test of parse method, of class OSMParser.
     */
    public void testParse() throws Exception {
        String path = "src/test/map.osm";
        OSM result = null;
        try {
            result = OSMParser.parse(path);
        } catch (Exception exception) {
            LogMaker.println(exception.getMessage());
        }
        String p = result.getNodes().iterator().next().tags.get("name");
        assertEquals(p, "UCT Club");
        
    }

}
