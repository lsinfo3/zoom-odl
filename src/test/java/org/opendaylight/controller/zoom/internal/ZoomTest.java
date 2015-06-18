package org.opendaylight.controller.zoom.internal;

import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.zoom.internal.Zoom;

public class ZoomTest extends TestCase {

        @Test
        public void testTestApplicationCreation() {

        		Zoom ah = null;
                ah = new Zoom();
                Assert.assertTrue(ah != null);

        }

}
