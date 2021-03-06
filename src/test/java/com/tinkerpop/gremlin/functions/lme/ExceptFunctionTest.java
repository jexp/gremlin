package com.tinkerpop.gremlin.functions.lme;

import com.tinkerpop.gremlin.XPathEvaluator;
import junit.framework.TestCase;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class ExceptFunctionTest extends TestCase {

    public void testExceptFunction() {
        XPathEvaluator xe = new XPathEvaluator();
        assertEquals(xe.evaluateList("g:append(1,2,3,4)[g:except(1,2)]").size(), 2);
        assertTrue(xe.evaluateList("g:append(1,2,3,4)[g:except(1,2)]").contains(3.0));
        assertTrue(xe.evaluateList("g:append(1,2,3,4)[g:except(1,2)]").contains(4.0));
        assertEquals(xe.evaluateList("g:append(1,2,3,4)[g:except(1)]").size(), 3);
        assertTrue(xe.evaluateList("g:append(1,2,3,4)[g:except(1)]").contains(2.0));
        assertTrue(xe.evaluateList("g:append(1,2,3,4)[g:except(1)]").contains(3.0));
        assertTrue(xe.evaluateList("g:append(1,2,3,4)[g:except(1)]").contains(4.0));
        assertEquals(xe.evaluateList("g:append(1,2,3,4)[g:except(1, g:append(1,2,3))]").size(), 1);
        assertTrue(xe.evaluateList("g:append(1,2,3,4)[g:except(1, g:append(1,2,3))]").contains(4.0));

    }
}
