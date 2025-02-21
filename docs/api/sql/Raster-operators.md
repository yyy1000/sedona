## Raster based operators

### RS_Envelope

Introduction: Returns the envelope of the raster as a Geometry.

Format: `RS_Envelope (raster: Raster)`

Since: `v1.4.0`

Spark SQL example:
```sql
SELECT RS_Envelope(raster) FROM raster_table
```
Output:
```
POLYGON((0 0,20 0,20 60,0 60,0 0))
```

### RS_Intersects

Introduction: Returns true if the envelope of the raster intersects the given geometry. If the geometry does not have a
defined SRID, it is considered to be in the same CRS with the raster. If the geometry has a defined SRID, the geometry
will be transformed to the CRS of the raster before the intersection test.

Format: `RS_Intersects (raster: Raster, geom: Geometry)`

Since: `v1.5.0`

Spark SQL example:
```sql
SELECT RS_Intersects(raster, ST_SetSRID(ST_PolygonFromEnvelope(0, 0, 10, 10), 4326)) FROM raster_table
```
Output:
```
true
```

### RS_MetaData

Introduction: Returns the metadata of the raster as an array of double. The array contains the following values:

- 0: upper left x coordinate of the raster, in terms of CRS units (the minimum x coordinate)
- 1: upper left y coordinate of the raster, in terms of CRS units (the maximum y coordinate)
- 2: width of the raster, in terms of pixels
- 3: height of the raster, in terms of pixels
- 4: width of a pixel, in terms of CRS units (scaleX)
- 5: height of a pixel, in terms of CRS units (scaleY)
- 6: skew in x direction (rotation x)
- 7: skew in y direction (rotation y)
- 8: srid of the raster
- 9: number of bands

Format: `RS_MetaData (raster: Raster)`

Since: `v1.4.1`

SQL example:
```sql
SELECT RS_MetaData(raster) FROM raster_table
```

Output:
```
+-----------------------------------------------------------------------------------------------------------------------+
|rs_metadata(raster)                                                                                                    |
+-----------------------------------------------------------------------------------------------------------------------+
|[-1.3095817809482181E7, 4021262.7487925636, 512.0, 517.0, 72.32861272132695, -72.32861272132695, 0.0, 0.0, 3857.0, 1.0]|
+-----------------------------------------------------------------------------------------------------------------------+
```

### RS_NumBands

Introduction: Returns the number of the bands in the raster.

Format: `RS_NumBands (raster: Raster)`

Since: `v1.4.0`

Spark SQL example:
```sql
SELECT RS_NumBands(raster) FROM raster_table
```

Output:
```
4
```

### RS_SetSRID

Introduction: Sets the spatial reference system identifier (SRID) of the raster geometry.

Format: `RS_SetSRID (raster: Raster, srid: Integer)`

Since: `v1.4.1`

Spark SQL example:
```sql
SELECT RS_SetSRID(raster, 4326)
FROM raster_table
```

### RS_SRID

Introduction: Returns the spatial reference system identifier (SRID) of the raster geometry.

Format: `RS_SRID (raster: Raster)`

Since: `v1.4.1`

Spark SQL example:
```sql
SELECT RS_SRID(raster) FROM raster_table
```

Output:
```
3857
```

### RS_Value

Introduction: Returns the value at the given point in the raster.
If no band number is specified it defaults to 1. 

Format: `RS_Value (raster: Raster, point: Geometry)`

Format: `RS_Value (raster: Raster, point: Geometry, band: Int)`

Since: `v1.4.0`

Spark SQL example:
```sql
SELECT RS_Value(raster, ST_Point(-13077301.685, 4002565.802)) FROM raster_table
```

Output:
```
5.0
```

### RS_Values

Introduction: Returns the values at the given points in the raster.
If no band number is specified it defaults to 1.

RS_Values is similar to RS_Value but operates on an array of points.
RS_Values can be significantly faster since a raster only has to be loaded once for several points.

Format: `RS_Values (raster: Raster, points: Array[Geometry])`

Format: `RS_Values (raster: Raster, points: Array[Geometry], band: Int)`

Since: `v1.4.0`

Spark SQL example:
```sql
SELECT RS_Values(raster, Array(ST_Point(-1307.5, 400.8), ST_Point(-1403.3, 399.1)))
FROM raster_table
```

Output:
```
Array(5.0, 3.0)
```

Spark SQL example for joining a point dataset with a raster dataset:
```scala
val pointDf = spark.read...
val rasterDf = spark.read.format("binaryFile").load("/some/path/*.tiff")
  .withColumn("raster", expr("RS_FromGeoTiff(content)"))
  .withColumn("envelope", expr("RS_Envelope(raster)"))

// Join the points with the raster extent and aggregate points to arrays.
// We only use the path and envelope of the raster to keep the shuffle as small as possible.
val df = pointDf.join(rasterDf.select("path", "envelope"), expr("ST_Within(point_geom, envelope)"))
  .groupBy("path")
  .agg(collect_list("point_geom").alias("point"), collect_list("point_id").alias("id"))

df.join(rasterDf, "path")
  .selectExpr("explode(arrays_zip(id, point, RS_Values(raster, point))) as result")
  .selectExpr("result.*")
  .show()
```

Output:
```
+----+------------+-------+
| id | point      | value |
+----+------------+-------+
|  4 | POINT(1 1) |   3.0 |
|  5 | POINT(2 2) |   7.0 |
+----+------------+-------+
```


## Raster to Map Algebra operators

To bridge the gap between the raster and map algebra worlds, the following operators are provided. These operators convert a raster to a map algebra object. The map algebra object can then be used with the map algebra operators described in the next section.

### RS_BandAsArray

Introduction: Extract a band from a raster as an array of doubles.

Format: `RS_BandAsArray (raster: Raster, bandIndex: Int)`.

Since: `v1.4.1`

BandIndex is 1-based and must be between 1 and RS_NumBands(raster). It returns null if the bandIndex is out of range or the raster is null.

SQL example:
```sql
SELECT RS_BandAsArray(raster, 1) FROM raster_table
```

Output:

```
+--------------------+
|                band|
+--------------------+
|[0.0, 0.0, 0.0, 0...|
+--------------------+
```

### RS_AddBandFromArray

Introduction: Add a band to a raster from an array of doubles.

Format: `RS_AddBandFromArray (raster: Raster, band: Array[Double], bandIndex:Int)`.

Since: `v1.4.1`

The bandIndex is 1-based and must be between 1 and RS_NumBands(raster) + 1. It throws an exception if the bandIndex is out of range or the raster is null.

When the bandIndex is RS_NumBands(raster) + 1, it appends the band to the end of the raster. Otherwise, it replaces the existing band at the bandIndex.

Note that: `bandIndex == RS_NumBands(raster) + 1` is an experimental feature and might not lead to the loss of raster metadata and properties such as color models.

SQL example:

```sql
SELECT RS_AddBandFromArray(raster, RS_MultiplyFactor(RS_BandAsArray(RS_FromGeoTiff(content), 1), 2), 1) AS raster FROM raster_table
```

Output:
```
+--------------------+
|              raster|
+--------------------+
|GridCoverage2D["g...|
+--------------------+
```

## Map Algebra operators

Map algebra operators work on a single band of a raster. Each band is represented as an array of doubles. The operators return an array of doubles.

### RS_Add

Introduction: Add two spectral bands in a Geotiff image 

Format: `RS_Add (Band1: Array[Double], Band2: Array[Double])`

Since: `v1.1.0`

Spark SQL example:
```scala

val sumDF = spark.sql("select RS_Add(band1, band2) as sumOfBands from dataframe")

```

### RS_Append

Introduction: Appends a new band to the end of Geotiff image data and returns the new data. The new band to be appended can be a normalized difference index between two bands (example: NBR, NDBI). Normalized difference index between two bands can be calculated with RS_NormalizedDifference operator described earlier in this page. Specific bands can be retrieved using RS_GetBand operator described [here](../Raster-loader/).

Format: `RS_Append(data: Array[Double], newBand: Array[Double], nBands: Int)`

Since: `v1.2.1`

Deprecated since: `v1.4.1`

Spark SQL example:
```scala

val dfAppended = spark.sql("select RS_Append(data, normalizedDifference, nBands) as dataEdited from dataframe")

```

### RS_BitwiseAND

Introduction: Find Bitwise AND between two bands of Geotiff image

Format: `RS_BitwiseAND (Band1: Array[Double], Band2: Array[Double])`

Since: `v1.1.0`

Spark SQL example:
```scala

val biwiseandDF = spark.sql("select RS_BitwiseAND(band1, band2) as andvalue from dataframe")

```

### RS_BitwiseOR

Introduction: Find Bitwise OR between two bands of Geotiff image

Format: `RS_BitwiseOR (Band1: Array[Double], Band2: Array[Double])`

Since: `v1.1.0`

Spark SQL example:
```scala

val biwiseorDF = spark.sql("select RS_BitwiseOR(band1, band2) as or from dataframe")

```

### RS_Count

Introduction: Returns count of a particular value from a spectral band in a raster image

Format: `RS_Count (Band1: Array[Double], Target: Double)`

Since: `v1.1.0`

Spark SQL example:
```scala

val countDF = spark.sql("select RS_Count(band1, target) as count from dataframe")

```

### RS_Divide

Introduction: Divide band1 with band2 from a geotiff image

Format: `RS_Divide (Band1: Array[Double], Band2: Array[Double])`

Since: `v1.1.0`

Spark SQL example:
```scala

val multiplyDF = spark.sql("select RS_Divide(band1, band2) as divideBands from dataframe")

```

### RS_FetchRegion

Introduction: Fetch a subset of region from given Geotiff image based on minimumX, minimumY, maximumX and maximumY index as well original height and width of image

Format: `RS_FetchRegion (Band: Array[Double], coordinates: Array[Int], dimensions: Array[Int])`

Since: `v1.1.0`

Spark SQL example:
```scala

val region = spark.sql("select RS_FetchRegion(Band,Array(0, 0, 1, 2),Array(3, 3)) as Region from dataframe")
```

### RS_GreaterThan

Introduction: Mask all the values with 1 which are greater than a particular target value

Format: `RS_GreaterThan (Band: Array[Double], Target: Double)`

Since: `v1.1.0`

Spark SQL example:
```scala

val greaterDF = spark.sql("select RS_GreaterThan(band, target) as maskedvalues from dataframe")

```

### RS_GreaterThanEqual

Introduction: Mask all the values with 1 which are greater than equal to a particular target value

Format: `RS_GreaterThanEqual (Band: Array[Double], Target: Double)`

Since: `v1.1.0`

Spark SQL example:
```scala

val greaterEqualDF = spark.sql("select RS_GreaterThanEqual(band, target) as maskedvalues from dataframe")

```

### RS_LessThan

Introduction: Mask all the values with 1 which are less than a particular target value

Format: `RS_LessThan (Band: Array[Double], Target: Double)`

Since: `v1.1.0`

Spark SQL example:
```scala

val lessDF = spark.sql("select RS_LessThan(band, target) as maskedvalues from dataframe")

```

### RS_LessThanEqual

Introduction: Mask all the values with 1 which are less than equal to a particular target value

Format: `RS_LessThanEqual (Band: Array[Double], Target: Double)`

Since: `v1.1.0`

Spark SQL example:
```scala

val lessEqualDF = spark.sql("select RS_LessThanEqual(band, target) as maskedvalues from dataframe")

```

### RS_LogicalDifference

Introduction: Return value from band 1 if a value in band1 and band2 are different, else return 0

Format: `RS_LogicalDifference (Band1: Array[Double], Band2: Array[Double])`

Since: `v1.1.0`

Spark SQL example:
```scala

val logicalDifference = spark.sql("select RS_LogicalDifference(band1, band2) as logdifference from dataframe")

```

### RS_LogicalOver

Introduction: Return value from band1 if it's not equal to 0, else return band2 value

Format: `RS_LogicalOver (Band1: Array[Double], Band2: Array[Double])`

Since: `v1.1.0`

Spark SQL example:
```scala

val logicalOver = spark.sql("select RS_LogicalOver(band1, band2) as logover from dataframe")

```

### RS_Mean

Introduction: Returns Mean value for a spectral band in a Geotiff image

Format: `RS_Mean (Band: Array[Double])`

Since: `v1.1.0`

Spark SQL example:
```scala

val meanDF = spark.sql("select RS_Mean(band) as mean from dataframe")

```

### RS_Mode

Introduction: Returns Mode from a spectral band in a Geotiff image in form of an array

Format: `RS_Mode (Band: Array[Double])`

Since: `v1.1.0`

Spark SQL example:
```scala

val modeDF = spark.sql("select RS_Mode(band) as mode from dataframe")

```

### RS_Modulo

Introduction: Find modulo of pixels with respect to a particular value

Format: `RS_Modulo (Band: Array[Double], Target: Double)`

Since: `v1.1.0`

Spark SQL example:
```scala

val moduloDF = spark.sql("select RS_Modulo(band, target) as modulo from dataframe")

```

### RS_Multiply

Introduction: Multiply two spectral bands in a Geotiff image

Format: `RS_Multiply (Band1: Array[Double], Band2: Array[Double])`

Since: `v1.1.0`

Spark SQL example:
```scala

val multiplyDF = spark.sql("select RS_Multiply(band1, band2) as multiplyBands from dataframe")

```

### RS_MultiplyFactor

Introduction: Multiply a factor to a spectral band in a geotiff image

Format: `RS_MultiplyFactor (Band1: Array[Double], Factor: Double)`

Since: `v1.1.0`

Spark SQL example:
```scala

val multiplyFactorDF = spark.sql("select RS_MultiplyFactor(band1, 2) as multiplyfactor from dataframe")

```

This function only accepts integer as factor before `v1.5.0`.

### RS_Normalize

Introduction: Normalize the value in the array to [0, 255]

Since: `v1.1.0`

Spark SQL example
```sql
SELECT RS_Normalize(band)
```

### RS_NormalizedDifference

Introduction: Returns Normalized Difference between two bands(band2 and band1) in a Geotiff image(example: NDVI, NDBI)

Format: `RS_NormalizedDifference (Band1: Array[Double], Band2: Array[Double])`

Since: `v1.1.0`

Spark SQL example:
```scala

val normalizedDF = spark.sql("select RS_NormalizedDifference(band1, band2) as normdifference from dataframe")

```

### RS_SquareRoot

Introduction: Find Square root of band values in a geotiff image 

Format: `RS_SquareRoot (Band: Array[Double])`

Since: `v1.1.0`

Spark SQL example:
```scala

val rootDF = spark.sql("select RS_SquareRoot(band) as squareroot from dataframe")

```

### RS_Subtract

Introduction: Subtract two spectral bands in a Geotiff image(band2 - band1)

Format: `RS_Subtract (Band1: Array[Double], Band2: Array[Double])`

Since: `v1.1.0`

Spark SQL example:
```scala

val subtractDF = spark.sql("select RS_Subtract(band1, band2) as differenceOfOfBands from dataframe")

```
