/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gdraw;

import java.util.ArrayList;
import java.io.PrintWriter;
import java.awt.Color;
import gdraw.Util.*;

/**
 *
 * @author Eric
 */
public class Circle
{
 boolean dbgFlag;
 PrintWriter dbg;
 int x;
 int y;
 int xinc;
 int yinc;
 ArrayList<Pt> pt;
 Pt[] pt0;
 Image image;
 Pt p;
 boolean edge = false;
 boolean track = false;

 public static final int BODY = new Color(0x80, 0x80, 0x80).getRGB();
 public static final int EDGE = new Color(0xf0, 0xf0, 0x00).getRGB();

 public Circle(PrintWriter d, int radius)
 {
  dbg = d;
  dbgFlag = (dbg != null);
  y = 0;
  x = radius;
//  dbg.printf("radius %3d\n", radius);
  pt = new ArrayList<>();

  int r2 = radius * radius;
  while (y < x)
  {
   x = (int) (Math.sqrt(r2 - y * y) + .5);
//   dbg.printf("x %3d y %3d\n", x, y);
   pt.add(new Pt(x, y));
   y++;
  }

  if (dbgFlag)
  {
   dbg.printf("change to x x %3d y %3d\n", x, y);
  }
  x = (int) (Math.sqrt(r2 - y * y) + .5);
//  dbg.printf("x %3d y %3d\n", x, y);
  pt.add(new Pt(x, y));

  while (x > 0)
  {
   x--;
   y = (int) (Math.sqrt(r2 - x * x) + .5);
//   dbg.printf("x %3d y %3d\n", x, y);
   pt.add(new Pt(x, y));
  }

  double maxErr = 0.0;
  Pt maxPt = null;
  int size = pt.size() - 1;
  int len = size * 4;
  pt0 = new Pt[len];
  int s1 = 2 * size;
  int s2 = 2 * size;
  int s3 = 4 * size;
  size++;
  for (int i = 0; i < size; i++)
  {
   p = pt.get(i);
   double err = Math.abs(radius - Math.sqrt(p.x * p.x + p.y * p.y));
   if (err > maxErr)
   {
    maxErr = err;
    maxPt = p;
   }
   pt0[i] = new Pt(p);
   pt0[s1--] = new Pt(-p.x, p.y);
   pt0[s2++] = new Pt(-p.x, -p.y);
   if (s3 < len)
   {
    pt0[s3] = new Pt(p.x, -p.y);
   }
   s3--;
  }
  if (dbgFlag
  &&  (maxPt != null))
  {
   dbg.printf("maxerr x %3d y %3d %6.4f\n", maxPt.x, maxPt.y, maxErr);
  }
 }

 public void fill(int data[], int w, int xCen, int yCen)
 {
  for (Pt pt1 : pt)
  {
   p = pt1;
   int i0 = (yCen + p.y) * w + xCen;
   int i1 = (yCen - p.y) * w + xCen;
   if (i1 < 0)
   {
    System.out.printf("error\n");
   }
   data[i0 + p.x] = EDGE;
   data[i1 + p.x] = EDGE;
   i0 -= p.x;
   i1 -= p.x;
   data[i0++] = EDGE;
   data[i1++] = EDGE;
   if (p.x > 1)
   {
    int x0 = -(p.x - 1);
    int x1 = p.x;
    while (x0 < x1)
    {
     data[i0++] = BODY;
     data[i1++] = BODY;
     x0++;
    }
   }
  }
 }

 public int findStart(Image image, Pt p0, int x, int y)
 {
  int xCen = (int) (p0.x / image.scale);
  int yCen = (int) (p0.y / image.scale);

  int x0 = x - xCen;
  int y0 = y - yCen;

  int j0;
  for (j0 = 0; j0 < pt0.length; j0++)
  {
   p = pt0[j0];
   if ((p.x == x0)
   &&  (p.y == y0))
   {
    break;
   }
  }

  if (dbgFlag)
  {
   if (j0 != pt0.length)
   {
    dbg.printf("str %3d x %3d y %3d theta %3.0f\n",
	       j0, p.x, p.y, Math.toDegrees(angle(p)));
   }
   else
   {
    dbg.printf("start not found\n");
   }
  }

  int i0;
  int i0Prev = (yCen + p.y) * image.w0 + xCen + p.x;

  for (Pt pt01 : pt0)
  {
   p = pt01;
   i0 = (yCen + p.y) * image.w0 + xCen + p.x;
   if (image.data[i0] != EDGE)
   {
    if (dbgFlag)
    {
     dbg.printf("end %3d x %3d y %3d theta %3.0f\n",
		j0, p.x, p.y, Math.toDegrees(angle(p)));
    }
    return(i0Prev);
   }
   j0++;
   if (j0 >= pt0.length)
   {
    j0 = 0;
   }
   i0Prev = i0;
  }
  if (dbgFlag)
  {
   dbg.printf("no intersection\n");
  }
  return(0);
 }

 public double angle(Pt pt)
 {
  double theta = Math.atan2((double) pt.y, (double) pt.x);
  if (theta < 0)
  {
   theta += 2 * Math.PI;
  }
  return(theta);
 }

 public void mark(Image image, Pt pt)
 {
  int xCen = (int) (pt.x / image.scale);
  int yCen = (int) (pt.y / image.scale);

  for (Pt pt01 : pt0)
  {
   p = pt01;
   int i0 = (yCen + p.y) * image.w0 + xCen + p.x;
   image.data[i0] = Image.PATH;
  }
 }

 public Circle.ArcEnd markArc(Image image, Pt pt, int x, int y) throws Exception
 {
  ArcEnd arcEnd = new ArcEnd();

  int xCen = (int) (pt.x / image.scale);
  int yCen = (int) (pt.y / image.scale);

  int x0 = x - xCen;
  int y0 = y - yCen;

  if (dbgFlag)
  {
   dbg.printf("xc %4d yc %4d x0 %3d y0 %3d\n", xCen, yCen, x0, y0);
  }

  /* follow point list to find start */

  int j0;			/* index of start */
  for (j0 = 0; j0 < pt0.length; j0++)
  {
   p = pt0[j0];
   if ((p.x == x0)
   &&  (p.y == y0))
   {
    break;
   }
  }

  arcEnd.start = angle(p);

  if (dbgFlag)
  {
   if (j0 != pt0.length)
   {
    dbg.printf("str %3d x %3d y %3d angle %3.0f\n", j0, p.x, p.y,
	       Math.toDegrees(arcEnd.start));
   }
   else
   {
    dbg.printf("start not found\n");
   }
  }

  int i0;
  int i0Prev = (yCen + p.y) * image.w0 + xCen + p.x;

  while (j0 < pt0.length)
  {
   p = pt0[j0];
   i0 = (yCen + p.y) * image.w0 + xCen + p.x;
   if (image.data[i0] != EDGE)
   {
    arcEnd.end = angle(p);
    arcEnd.i0 = i0Prev;
    if (dbgFlag)
    {
     dbg.printf("end %3d x %3d y %3d angle %3.0f\n", j0, p.x, p.y,
		Math.toDegrees(arcEnd.end));
    }
    return(arcEnd);
   }

   image.data[i0] = Image.PATH;
   j0++;
   if (j0 >= pt0.length)
   {
    j0 = 0;
   }
   i0Prev = i0;
  }

  if (dbgFlag)
  {
   dbg.printf("no intersection\n");
  }
  return(arcEnd);
 }

 public class ArcEnd
 {
  int i0;
  double start;
  double end;

  public ArcEnd()
  {
   i0 = 0;
  }
 }
}
