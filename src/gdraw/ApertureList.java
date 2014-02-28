/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gdraw;

import java.io.PrintWriter;
import gdraw.Util.*;

/*
 * ApertureList.java
 *
 * Created on Apr 8, 2008 at 7:03:10 AM
 *
 * @author Eric Nystrom
 *
 */

public class ApertureList
{
 GDraw gerber;
 PrintWriter dbg;
 public Aperture[] aperture;
 public static final int MAXAPERTURE = 60;

 public ApertureList(GDraw g)
 {
  gerber = g;
  dbg = g.dbg;
  aperture = new Aperture[MAXAPERTURE];
 }

 public void setDebug(PrintWriter d)
 {
  dbg = d;
 }

 public void add(Aperture a)
 {
  aperture[a.index] = a;
 }

 public void add(int i, double v1)
 {
  if (i < MAXAPERTURE)
  {
   aperture[i] = new Aperture(i,v1);
  }
 }

 public void add(int i, double v1, double v2)
 {
  if (i < MAXAPERTURE)
  {
   aperture[i] = new Aperture(i,v1,v2);
  }
 }

 public Aperture get(int i)
 {
  if (i < MAXAPERTURE)
  {
   return(aperture[i]);
  }
  return(null);
 }

 public void addBitRadius(double r)
 {
  for (int i = 0; i < MAXAPERTURE; i++)
  {
   Aperture ap = aperture[i];
   if (ap != null)
   {
    if (ap.type == Aperture.ROUND)
    {
     ap.val1 += r;
     ap.val2 += r;
    }
    else if (ap.type == Aperture.SQUARE)
    {
     ap.val1 += r;
     ap.val2 += r;
    }
   }
  }
 }

 public void print()
 {
  if (dbg != null)
  {
   dbg.printf("\nAperture List\n\n");
   for (int i = 0; i < MAXAPERTURE; i++)
   {
    Aperture ap = aperture[i];
    if (ap != null)
    {
     if (ap.type == Aperture.ROUND)
     {
      dbg.printf("%2d C %4.3f\n",i,ap.val1);
     }
     else if (ap.type == Aperture.SQUARE)
     {
      dbg.printf("%2d R %4.3f %4.3f\n",i,ap.val1,ap.val2);
     }
    }
   }
  }
 }

 public class Aperture
 {
  int type;			/* aperture type */
  int index;			/* aperturn number */
  double val1;			/* size for round or width for rectangular */
  double val2;			/* height for rectangular */
  Circle c;

  public static final int ROUND = 1;
  public static final int SQUARE = 2;

  public Aperture(int i, double v1)
  {
   type = ROUND;
   index = i;
   val1 = v1;
   val2 = v1;
  }

  public Aperture(int i, double v1, double v2)
  {
   type = SQUARE;
   index = 1;

   val1 = v1;
   val2 = v2;
  }

  public void draw(Image image, Pt pt)
  {
   if (c == null)
   {
    int r = (int) ((val1 / 2 * GDraw.SCALE) / image.scale);
    c = new Circle(dbg,r);
   }
   int x = (int) (pt.x / image.scale);
   int y = (int) (pt.y / image.scale);
   c.fill(image.data,image.w0,x,y);
  }

  public boolean connected(Pt p0, Pt pt)
  {
   if (type == Aperture.ROUND) /* if round pad */
   {
    double size = (val1 / 2.0) * GDraw.SCALE;
    double dist = p0.dist(pt);
//    dbg.printf("%5d %5d %6.1f\n",p0.x,p0.y,Math.abs(dist - size));
    if (Math.abs(dist) < (size + 15))
     return(true);
   }
   else if (type == Aperture.SQUARE) /* if square pad */
   {
    int xval = (int) (val1 / 2.0 * GDraw.SCALE);
    int yval = (int) (val2 / 2.0 * GDraw.SCALE);

    int xr = p0.x + xval;	/* right side */
    int yt = p0.y + yval;	/* top side */
    int xl = p0.x - xval;	/* left side */
    int yb = p0.y - yval;	/* bottom side */

    if ((pt.x > xr)
	||  (pt.x < xl)
	||  (pt.y > yt)
	||  (pt.y < yb))
    {
    }
    else
    {
     return(true);
    }
   }
   return(false);
  }
 }
}
