/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gdraw;

import java.io.PrintWriter;

import java.util.ArrayList;

import gdraw.Util.Pt;

/*
 * PadList.java
 *
 * Created on Apr 8, 2008 at 7:03:46 AM
 *
 * @author Eric Nystrom
 *
 */

public class PadList extends ArrayList<PadList.Pad>
{
 GDraw gerber;
 boolean dbgFlag;
 PrintWriter dbg;
 int max;

 public PadList(GDraw g)
 {
  gerber = g;
  dbg = g.dbg;
  dbgFlag = (dbg != null);
  max = 0;
 }

 public Pad add(Pt p, ApertureList.Aperture a)
 {
  Pad pad;
  add(pad = new Pad(max++,p,a));
  pad.print(dbg);
  return(pad);
 }

 public Pad findPad(Pt pt)
 {
  for (int i = 0; i < size(); i++)
  {
   Pad p = get(i);
   if (p.ap.connected(p.pt,pt))
   {
    return(p);
   }
  }
  return(null);
 }

 public void rotate(int xMax)
 {
  for (int i = 0; i < size(); i++)
  {
   PadList.Pad p = get(i);
   p.pt.rotate(xMax);
  }
 }

 public void mirror(int xSize, int ySize)
 {
  for (int i = 0; i < size(); i++)
  {
   PadList.Pad p = get(i);
   p.pt.mirror(xSize,ySize);
  }
 }

 public void draw(Image image) throws Exception
 {
  if (dbgFlag)
  {
   dbg.printf("\nDraw Pads\n\n");
  }
  for (int i = 0; i < max; i++)
  {
   Pad p = get(i);
   if ((p.pt.x >= 0)
   ||  (p.pt.y >= 0))
   {
    image.draw(p);
   }
  }
 }

 public void print()
 {
  if (dbgFlag)
  {
   dbg.printf("\nPad List\n\n");
   for (int i = 0; i < max; i++)
   {
    Pad pad = (Pad) get(i);
    dbg.printf("%3d %3d %6d y %6d ",pad.index,pad.gIndex,pad.pt.x,pad.pt.y);

    ApertureList.Aperture ap = pad.ap;
    if (ap != null)
    {

     if (ap.type == ApertureList.Aperture.ROUND)
     {
      dbg.printf("C %4.3f\n",ap.val1);
     }
     else if (ap.type == ApertureList.Aperture.SQUARE)
     {
      dbg.printf("R %4.3f %4.3f\n",ap.val1,ap.val2);
     }
    }
    else
    {
     dbg.printf("null\n");
    }
   }
   dbg.flush();
  }
 }

 public class Pad implements Comparable<PadList.Pad>
 {
  Pt pt;			/* pad location */
  ApertureList.Aperture ap;	/* aperture */
  int index;			/* pad number */
  int gIndex;			/* pad number in gerber file */

  public Pad(int i, Pt p, ApertureList.Aperture a)
  {
   index = i;
   pt = p;
   ap = a;
   if (a == null)
   {
    System.out.printf("pad %3d null aperture x %6d y %6d\n",
                      index,pt.x,pt.y);
   }
  }
  
  public boolean connected(Pt p0)
  {
   return(ap.connected(pt,p0));
  }

  @Override public int compareTo(Pad pad)
  {
   int compare;

   compare = pt.x - pad.pt.x;
   if (compare == 0)
   {
    compare = pt.y - pad.pt.y;
   }
   return(compare);
  }

  /**
   * Determine distance between point and line.
   * 
   * @param pt0 first end point of line
   * @param pt1 second end point of line
   * @return distance between point and line
   */
  public PadList.PadDist lineDistance(Pt pt0, Pt pt1)
  {
   PadList.PadDist padDist = new PadDist();

   double r0 = ap.val1 * GDraw.SCALE;
   if ((Math.hypot(pt.x - pt0.x,pt.y - pt0.y) <= r0)
   ||  (Math.hypot(pt.x - pt1.x,pt.y - pt1.y) <= r0))
   {
    return(padDist);
   }

   double xdel = pt1.x - pt0.x;
   double ydel = pt1.y - pt0.y;
   double mag = xdel * xdel + ydel * ydel;

   double u = ((pt.x - pt0.x) * xdel +
	       (pt.y - pt0.y) * ydel) / mag;

   if (dbgFlag)
   {
    dbg.printf("%3d x %6d y %6d " + 
	       "pt0.x %6d pt0.y %6d " +
	       "pt1.x %6d pt1.y %6d u %6.3f\n",
	       index,pt.x,pt.y,pt0.x,pt0.y,pt1.x,pt1.y,u);
   }

   if ((u < 0.0) || u > 1.0)
   {
    return(padDist);
   }

   int xt = (int) (pt0.x + u * xdel);
   int yt = (int) (pt0.y + u * ydel);

   padDist.dist = (int) Math.floor(Math.hypot(xt - pt.x,yt - pt.y));

   if (dbgFlag)
   {
    dbg.printf("xt %6d yt %6d dist %6d\n",xt,yt,padDist.dist);
   }

   padDist.x = xt;
   padDist.y = yt;
   return(padDist);
  }

  public void print(PrintWriter dbg)
  {
   if (dbgFlag)
   {
    dbg.printf("x %6d y %6d pad %3d\n",pt.x,pt.y,index);
   }
  }
 }

 public class PadDist
 {
  int dist;
  int x;
  int y;

  public PadDist()
  {
   dist = -1;
   x = 0;
   y = 0;
  }
 }
}
