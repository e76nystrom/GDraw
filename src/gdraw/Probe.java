/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gdraw;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

import java.util.regex.Matcher;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

/**
 *
 * @author Eric
 */
public class Probe
{
 boolean dbgFlag = false;
 PrintWriter dbg;
 
 ArrayList<Point> points = new ArrayList<>();
 double scale = 25.4;

 double xMin = 0.0;
 double yMin = 0.0;
 double zMin = 0.0;

 double xMax = 0.0;
 double yMax = 0.0;
 double zMax = 0.0;

 double xSize;
 double ySize;
 double margin;

 int xN = 0;
 int yN = 0;

 double xStep = 0;
 double yStep = 0;

 double[][] zMatrix;

 public static final double MIN_DIST = 1e-10;
 public static final double MAX_DIST = 1e10;

 public Probe()
 {
 }

 public void setDebug(boolean dbgFlag, PrintWriter dbg)
 {
  this.dbgFlag = dbgFlag;
  this.dbg = dbg;
 }
 
 public boolean readFile(String fileName)
 {
  File f = new File(fileName);
  if (!f.exists())
  {
   System.err.printf("probe file %s does not exist\n", fileName);
   return(false);
  }

  String ngcProbe = fileName.replaceFirst("\\.prb", "p.ngc");
  File f1 = new File(ngcProbe);
  if (!f.exists())
  {
   System.err.printf("probe gcode file %s does not exist\n", ngcProbe);
   return(false);
  }

  BufferedReader in;
  try
  {
   in = new BufferedReader(new FileReader(ngcProbe));
  }
  catch (FileNotFoundException e)
  {
   System.err.printf("probe file %s does not exist\n", ngcProbe);
   return(false);
  }

  LocationList locList = new LocationList();
  
  String line;
  try
  {
   String s = ("g.*?\\(\\s*" +
	       "(\\d*)\\s*" +
	       "(\\d*)\\s*" +
	       "x(\\s*-?\\d*.?\\d*)\\s*" +
	       "y(\\s*-?\\d*.?\\d*)\\s*" +
	       "\\)");
   System.out.println(s);
   Pattern p0 = Pattern.compile(s);
   Pattern p1 = Pattern.compile("[\\(\\s]");
   Pattern p2 = Pattern.compile("\\(?\\s*([a-zA-Z]+)\\s+(-?\\d*.?\\d+)");
//   Pattern p2 = Pattern.compile("\\(?(\\s*[a-zA-Z]+)");
   while ((line = in.readLine()) != null)
   {
    line = line.trim();
    Matcher m = p0.matcher(line);
    if (m.find())
    {
     MatchResult mr = m.toMatchResult();
     int count = mr.groupCount();
//     System.out.printf("%d\n", count);
     if (count == 4)
     {
       int i = Integer.valueOf(m.group(1));
       int j = Integer.valueOf(m.group(2));
       double x = Double.valueOf(m.group(3));
       double y = Double.valueOf(m.group(4));
       locList.append(i, j, x, y);
//      System.out.printf("%s %s %s %s\n", m.group(1), m.group(2),
//			m.group(3), m.group(4));
//      System.out.printf("%s\n", line);
     }
     continue;
    }
    while (true)
    {
     m = p2.matcher(line);
     if (m.find())
     {
      MatchResult mr = m.toMatchResult();
      int count = mr.groupCount();
//      System.out.printf("%d\n", count);
      String var = m.group(1);
      double val;
      try
      {
       val = Double.valueOf(m.group(2));
      }
      catch (NumberFormatException e)
      {
       val = 0.0;
      }
//      System.out.printf("|%s| %d  %6.4f\n", var, var.length(), val);
//      for (int i = 0; i < var.length(); i++)
//      {
//       System.out.printf("%d %c\n", i, var.charAt(i));
//      }
      switch (var)
      {
      case "xSize":
       xSize = val;
       break;
      case "ySize":
       ySize = val;
       break;
      case "xPoints":
       xN = (int) val;
       break;
      case "yPoints":
       yN = (int) val;
       break;
      case "margin":
       margin = val;
       break;
      case "xStep":
       xStep = val;
       break;
      case "yStep":
       yStep = val;
       break;
      }
      line = line.substring(mr.end());
     }
     else
      break;
    }
   }
  }
  catch (IOException e)
  {
   System.err.printf("error reading probe file %s\n", fileName);
   return(false);
  }
//  for (Location l : locList)
//  {
//   System.out.printf("%d %d x%6.4f y%6.4f\n", l.i, l.j, l.x, l.y);
//  }
  
//    Matcher m = p0.matcher(line);
//    if (m.find())
//    {
//     MatchResult mr = m.toMatchResult();
//     int count = mr.groupCount();
//     System.out.printf("%d\n", count);
//     if (count == 4)
//     {
//      int i = Integer.valueOf(mr.group(1));
//      int j = Integer.valueOf(mr.group(2));
//      double x = Double.valueOf(mr.group(3));
//      double y = Double.valueOf(mr.group(4));
//      locList.append(i, j, x, y);
//      System.out.printf("%s %s %s %s\n", mr.group(1), mr.group(2),
//			mr.group(3), mr.group(4));
//      System.out.printf("%s\n", line);
//     }
//    }

  try
  {
   in = new BufferedReader(new FileReader(f));
  }
  catch (FileNotFoundException e)
  {
   System.err.printf("probe file %s does not exist\n", fileName);
   return(false);
  }

//  ArrayList<Double> xList = new ArrayList<>();
//  ArrayList<Double> yList = new ArrayList<>();

  xMin = 9999.0;
  yMin = 9999.0;
  zMin = 9999.0;

  xMax = -9999.0;
  yMax = -9999.0;
  zMax = -9999.0;

  try
  {
   int lineNum = 0;
   while ((line = in.readLine()) != null)
   {
    String[] values = line.split(" ");
    double x;
    double y;
    double z;
    try
    {
     lineNum += 1;
     x = Double.valueOf(values[0]);
     y = Double.valueOf(values[1]);
     z = Double.valueOf(values[2]);
    }
    catch (NumberFormatException e)
    {
     System.err.printf("probe file number format error\n");
     System.err.printf("line %2d %s\n", lineNum, line);
     continue;
    }
//    listAdd(xList, x);
//    listAdd(yList, y);
    points.add(new Point(x, y, z));
/*
    if (x < xMin)
    {
     xMin = x;
    }
    if (y < yMin)
    {
     yMin = y;
    }
    if (z < zMin)
    {
     zMin = z;
    }

    if (x > xMax)
    {
     xMax = x;
    }
    if (y > yMax)
    {
     yMax = y;
    }
    if (z > zMax)
    {
     zMax = z;
    }
*/
   }
   in.close();
  }
  catch (IOException e)
  {
   System.err.printf("error reading probe file %s\n", fileName);
   return(false);
  }
  xMin = margin;
  yMin = margin;
//  xN = xList.size();
//  yN = yList.size();

//  Collections.sort(xList);
//  Collections.sort(yList);
//  xStep = xList.get(1) - xList.get(0);
//  yStep = yList.get(1) - yList.get(0);

  zMatrix = new double[xN][yN];
  for (Point p : points)
  {
   Location l = locList.find(p.x, p.y);
   if (l != null)
   {
    zMatrix[l.i][l.j] = p.z;
   }
//   int i = (int) Math.floor((p.x - xMin) / xStep);
//   int j = (int) Math.floor((p.y - yMin) / yStep);
//   zMatrix[i][j] = p.z;
  }

  double offset = zMatrix[0][0];
  for (int i = 0; i < xN; i++)
  {
   for (int j = 0; j < xN; j++)
   {
    zMatrix[i][j] -= offset;
   }
  }

  for (int j = yN - 1; j >= 0; j--)
  {
   for (int i = 0; i < xN; i++)
   {
    System.out.printf("%7.4f ", zMatrix[i][j]);
   }
   System.out.println();
  }
  return(true);
 }

 public void listAdd(ArrayList<Double> list, double val)
 {
  boolean found = false;
  for (int i = 0; i < list.size(); i++)
  {
   if (val == list.get(i))
   {
    found = true;
    break;
   }
  }
  if (!found)
  {
   list.add(val);
  }
 }

 public double interpolate(double x, double y)
 {
  double iX = (x - xMin) / xStep;
  double jY = (y - yMin) / yStep;
  int i = (int) Math.floor(iX);
  int j = (int) Math.floor(jY);

  if (i < 0)
  {
   i = 0;
  }
  else if (i >= xN - 1)
   i = xN - 2;

  if (j < 0)
   j = 0;
  else if (j >= yN - 1)
   j = yN - 2;

  double a = iX - i;
  double b = jY - j;
  double a1 = 1.0 - a;
  double b1 = 1.0 - b;

  return(a1 * b1 * zMatrix[j]    [i]     +
	 a1 * b  * zMatrix[j + 1][i]     +
	 a  * b1 * zMatrix[j]    [i + 1] +
	 a  * b  * zMatrix[j + 1][i + 1]);
 }

 public Probe.SegmentList splitLine(double x1, double y1, double z1,
			      double x2, double y2, double z2)
 {
  Probe.SegmentList segments = new SegmentList();

  double dx = x2 - x1;
  double dy = y2 - y1;
  double dz = z2 - z1;

  if (dbgFlag)
  {
   dbg.printf("splitLine\n");
   dbg.printf("x1 %7.4f y1 %7.4f z1 %7.4f x1 %7.4f y1 %7.4f z1 %7.4f\n",
	      x1, y1, z1, x2, y2, z2);
   dbg.printf("dx %7.4f dy %7.4f dz %7.4f\n", dx, dy, dz);
  }

  if (Math.abs(dx) < MIN_DIST)
  {
   dx = 0.0;
  }
  if (Math.abs(dy) < MIN_DIST)
  {
   dy = 0.0;
  }
  if (Math.abs(dz) < MIN_DIST)
  {
   dz = 0.0;
  }

  if (dx == 0.0 && dy == 0.0)
  {
   segments.append(x2, y2, z2 + interpolate(x2, y2));
   return(segments);
  }

  double rxy = Math.hypot(dx, dy);
  dx /= rxy;
  dy /= rxy;
  dz /= rxy;

  int i = (int) Math.floor((x1 - xMin) / xStep);
  int j = (int) Math.floor((y1 - yMin) / yStep);
  double tx;
  double tdx;
  if (dx > MIN_DIST)
  {
   tx  = ((i + 1) * xStep + xMin - x1) / dx;
   tdx = xStep / dx;
  }
  else if (dx < -MIN_DIST)
  {
   tx  = (i * xStep + xMin - x1) / dx;
   tdx = -xStep / dx;
  }
  else
  {
   tx  = MAX_DIST;
   tdx = 0.0;
  }

  double ty;
  double tdy;
  if (dy > MIN_DIST)
  {
   ty  = ((j + 1) * yStep + yMin - y1) / dy;
   tdy = yStep / dy;
  }
  else if (dy < -MIN_DIST)
  {
   ty  = (j * yStep + yMin - y1) / dy;
   tdy = -yStep / dy;
  }
  else
  {
   ty  = MAX_DIST;
   tdy = 0.0;
  }
  if (dbgFlag)
  {
   dbg.printf("i %d tx %7.4f tdx %7.4f j %d ty %7.4f tdy %7.4f\n",
	      i, tx, tdx, j, ty, tdy);
  }

  int count = 0;
  rxy *= 0.999999999;
  while (tx < rxy || ty < rxy)
  {
   if (dbgFlag)
   {
    dbg.printf("tx %7.4f ty%7.4f rxy %7.4f\n", tx, ty, rxy);
    dbg.flush();
   }
   double t;
   if (tx == ty)
   {
    t = tx;
    tx += tdx;
    ty += tdy;
   }
   else if (tx < ty)
   {
    t = tx;
    tx += tdx;
   }
   else
   {
    t = ty;
    ty += tdy;
   }
   double x = x1 + t * dx;
   double y = y1 + t * dy;
   double z = z1 + t * dz;
   segments.append(x, y, z + interpolate(x, y));
   count += 1;
   if (count > 5)
   {
    if (dbgFlag)
    {
     dbg.printf("overflow\n");
    }
    System.out.printf("overflow\n");
    break;
   }
  }

  segments.append(x2, y2, z2 + interpolate(x2, y2));
  return(segments);
 }

 public class Location
 {
  int i;
  int j;
  double x;
  double y;
  
  public Location(int i, int j, double x, double y)
  {
   this.i = i;
   this.j = j;
   this.x = x;
   this.y = y;
  }
 }

 public class LocationList extends ArrayList<Location>
 {
//  int xN;
//  int yN;
  int index;
  
  public LocationList()
  {
//   xN = 0;
//   yN = 0;
   index = 0;
  }

  public void append(int i, int j, double x, double y)
  {
/*
   if (i > xN)
   {
    xN = i;
   }
   if (j > yN)
   {
    yN = j;
   }
*/
   add(new Location(i, j, x, y));
  }

  public Location find(double x, double y)
  {
   Location l = get(index);
   if ((Math.abs(l.x - x) < .001) &&
       (Math.abs(l.y - y) < .001))
   {
    index += 1;
    return(l);
   }
   System.out.printf("searching %d\n", index);
   index = 0;
   for (Location l0 : this)
   {
    double dx = l0.x - x;
    double dy = l0.y - y;
    double d = Math.hypot(dx, dy);
//    System.out.printf("%d %d dx %7.4f dy %7.4f d %7.4f\n", l0.i, l0.j, dx, dy, d);
    if (d < 0.001)
    {
//     System.out.printf("found %d %d\n", l0.i, l0.j);
     return(l0);
    }
    index += 1;
   }
   return(null);
  }
 }
 

 public class Point
 {
  double x;
  double y;
  double z;

  public Point(double x0, double y0, double z0)
  {
   x = x0;
   y = y0;
   z = z0;
  }
 }

 public class SegmentList extends ArrayList<Probe.Point>
 {

  public SegmentList()
  {
  }

  public void append(double x, double y, double z)
  {
   add(new Point(x, y, z));
  }
 }
}
