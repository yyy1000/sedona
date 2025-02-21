/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sedona.common.raster;

import org.geotools.coverage.grid.GridCoverage2D;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.opengis.referencing.FactoryException;

import static org.junit.Assert.assertEquals;

public class RasterAccessorsTest extends RasterTestBase
{
    @Test
    public void envelope() throws FactoryException
    {
        Geometry envelope = RasterAccessors.envelope(oneBandRaster);
        assertEquals(3600.0d, envelope.getArea(), 0.1d);
        assertEquals(378922.0d + 30.0d, envelope.getCentroid().getX(), 0.1d);
        assertEquals(4072345.0d + 30.0d, envelope.getCentroid().getY(), 0.1d);

        assertEquals(4326, RasterAccessors.envelope(multiBandRaster).getSRID());
    }

    @Test
    public void testNumBands() {
        assertEquals(1, RasterAccessors.numBands(oneBandRaster));
        assertEquals(4, RasterAccessors.numBands(multiBandRaster));
    }

    @Test
    public void testSrid() throws FactoryException {
        assertEquals(0, RasterAccessors.srid(oneBandRaster));
        assertEquals(4326, RasterAccessors.srid(multiBandRaster));
    }

    @Test
    public void testMetaData()
            throws FactoryException
    {
        double upperLeftX = 1;
        double upperLeftY = 2;
        int widthInPixel = 3;
        int heightInPixel = 4;
        double pixelSize = 5;
        int numBands = 1;

        GridCoverage2D gridCoverage2D = RasterConstructors.makeEmptyRaster(numBands, widthInPixel, heightInPixel, upperLeftX, upperLeftY, pixelSize);
        double[] metadata = RasterAccessors.metadata(gridCoverage2D);
        assertEquals(upperLeftX, metadata[0], 0.1d);
        assertEquals(upperLeftY, metadata[1], 0.1d);
        assertEquals(widthInPixel, metadata[2], 0.1d);
        assertEquals(heightInPixel, metadata[3], 0.1d);
        assertEquals(pixelSize, metadata[4], 0.1d);
        assertEquals(-1 * pixelSize, metadata[5], 0.1d);
        assertEquals(0, metadata[6], 0.1d);
        assertEquals(0, metadata[7], 0.1d);
        assertEquals(0, metadata[8], 0.1d);
        assertEquals(numBands, metadata[9], 0.1d);
        assertEquals(10, metadata.length);

        upperLeftX = 5;
        upperLeftY = 6;
        widthInPixel = 7;
        heightInPixel = 8;
        pixelSize = 9;
        numBands = 10;

        gridCoverage2D = RasterConstructors.makeEmptyRaster(numBands, widthInPixel, heightInPixel, upperLeftX, upperLeftY, pixelSize);

        metadata = RasterAccessors.metadata(gridCoverage2D);

        assertEquals(upperLeftX, metadata[0], 0.1d);
        assertEquals(upperLeftY, metadata[1], 0.1d);
        assertEquals(widthInPixel, metadata[2], 0.1d);
        assertEquals(heightInPixel, metadata[3], 0.1d);
        assertEquals(pixelSize, metadata[4], 0.1d);
        assertEquals(-1 * pixelSize, metadata[5], 0.1d);
        assertEquals(0, metadata[6], 0.1d);
        assertEquals(0, metadata[7], 0.1d);
        assertEquals(0, metadata[8], 0.1d);
        assertEquals(numBands, metadata[9], 0.1d);

        assertEquals(10, metadata.length);
    }
}
