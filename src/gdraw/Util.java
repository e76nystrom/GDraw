/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gdraw;

import java.util.Comparator;

/*
 * Util.java
 *
 * Created on Apr 7, 2008 at 7:29:04 PM
 *
 * @author Eric Nystrom
 *
 */

public class Util
{

 public static class PtCompare implements Comparator<Util.Pt>
 {
  @Override
   public int compare(Pt p0, Pt p1)
  {
   if (p0.x < p1.x)
   {
    return -1;
   }
   else if (p0.x > p1.x)
   {
    return 1;
   }
   else
   {
    if (p0.y < p1.y)
    {
     return -1;
    }
    else if (p0.y > p1.y)
    {
     return 1;
    }
    else
    {
     return 0;
    }
   }
  }
 }

/**
  * Class for the x and y values of a point location.
  */
 public static class Pt
 {
  int x;
  int y;

  /**
   * Create a point from an x and y location.
   * 
   * @param x0 x location
   * @param y0 y location
   */
  public Pt(int x0, int y0)
  {
   x = x0;
   y = y0;
  }

  /**
   * Create a point from another point.
   * 
   * @param p point to use in creating point
   */
  public Pt(Pt p)
  {
   x = p.x;
   y = p.y;
  }

  /**
   * Set the x and y values of this point from values in input parameter.
   * 
   * @param p point used to set x and y values
   */
  public void set(Pt p)
  {
   x = p.x;
   y = p.y;
  }

  /**
   * Set the x and y values of this point from the input arguments.
   * 
   * @param x0 value for x
   * @param y0 value for y
   */
  public void set(int x0, int y0)
  {
   x = x0;
   y = y0;
  }

  /**
   * Make a coyp of the point
   * 
   * @return new point
   */
  public Pt copy()
  {
   return(new Pt(this));
  }

  /**
   * Calculate distance from this point to another point p.
   * 
   * @param p point to calculate distance to
   * @return distance between two points
   */
  public double dist(Pt p)
  {
   return(Math.hypot((double) (x - p.x), (double) (y - p.y)));
  }

  /**
   * Determine if two points are at the same location.
   * 
   * @param p point to compare to
   * @return true if two points are at the same location
   */
  public boolean equals(Pt p)
  {
   return((x == p.x) && (y == p.y));
  }

  /**
   * Offset a point relative to another point
   * 
   * @param p point to offset from
   */
  public void offset(Pt p)
  {
   x -= p.x;
   y -= p.y;
  }

  /**
   * rotate swap x and y
   * 
   * @param xMax value to subtract x from
   */
  public void rotate(int xMax)
  {
   int tmp = x;
   x = xMax - y;
   y = tmp;
  }

  /**
   * mirror x and y
   * 
   * @param xSize x value for mirror
   * @param ySize y value for mirror
   */
  public void mirror(int xSize, int ySize)
  {
   if (xSize != 0)
   {
    x = xSize - x;
   }
   if (ySize != 0)
   {
    y = ySize - y;
   }
  }

  /**
   * Determine if two points are close to each other.
   * 
   * @param p point to compare to
   * @param dist minimum distance for compare
   * @return true if two points are close
   */
  public boolean near(Pt p, int dist)
  {
   return((Math.abs(x - p.x) < dist) && (Math.abs(y - p.y) < dist));
  }

  /**
   * Determine distance between point and line.
   * 
   * @param pt0 first end point of line
   * @param pt1 second end point of line
   * @return distance between point and line
   */
  public int lineDistance(Pt pt0, Pt pt1)
  {
   double xdel = pt1.x - pt0.x;
   double ydel = pt1.y - pt0.y;
   double magSqr = xdel * xdel + ydel * ydel;

   double u = ((x - pt0.x) * xdel + (y - pt0.y) * ydel) / magSqr;

   if ((u < 0.0) || u > 1.0)
   {
    return(-1);
   }

   int xt = (int) (pt1.x + u * xdel);
   int yt = (int) (pt1.y + u * ydel);

   double d = Math.hypot(xt - x, yt- y);
   return((int) Math.floor(d));
  }

  /**
   * Determine angle of line formed with this point at center and input
   * argument.
   * 
   * @param p point at other end of line
   * @return angle
   */
  public double angle(Pt p)
  {
   double angle;
   double dx = x - p.x;
   double dy = y - p.y;

   if (dy == 0)
   {
    if (dx > 0)
    {
     return(0.0);
    }
    else
    {
     return(180.0);
    }
   }

   angle = Math.atan2(dy, dx);
   if (angle <= 0)
   {
    angle = 2.0 * Math.PI + angle;
   }
   angle *= 180 / Math.PI;
   return(angle);
  }

  /**
   * Determine octant of input argument with respect to this point.
   * 
   * @param p point to determine octant of
   * @return octant
   */
  public int octant(Pt p)
  {
   int dx = x - p.x;
   int dy = y - p.y;
   return(octant(dx, dy));
  }

  /**
   * Determine octant of position (x, y) with respect to origin.
   * 
   * @param x x location
   * @param y y location
   * @return octant
   */
  public int octant(int x, int y)
  {
   if (x > 0)
   {
    if (y >= 0)
    {
     if (x > y)
     {
      return(0);
     }
     else
     {
      return(1);
     }
    }
    else
    {
     y = -y;
     if (x >= y)
     {
      return(7);
     }
     else
     {
      return(6);
     }
    }
   }
   else
   {
    x = -x;
    if (y > 0)
    {
     if (x >= y)
     {
      return(3);
     }
     else
     {
      return(2);
     }
    }
    else
    {
     y = -y;
     if (x > y)
     {
      return(4);
     }
     else
     {
      if (x == 0)
      {
       return(6);
      }
      else
      {
       return(5);
      }
     }
    }
   }
  }
 }

 public static enum D
 {
  XPOS,				/* 0 */
  XPOS_YPOS,			/* 1 */
  YPOS,				/* 2 */
  XNEG_YPOS,			/* 3 */
  XNEG,				/* 4 */
  XNEG_YNEG,			/* 5 */
  YNEG,				/* 6 */
  XPOS_YNEG,			/* 7 */
  DONE,				/* 8 */
  INVALID			/* 9 */
 };

 public static class CRet
 {
  int index;
  D dir;

  public CRet(int i, int d)
  {
   index = i;
   dir = D.XNEG_YPOS;
   switch (d)
   {
   case 0: dir = D.XNEG_YPOS; break;
   case 1: dir = D.XNEG_YNEG; break;
   case 2: dir = D.XPOS_YNEG; break;
   case 3: dir = D.XPOS_YPOS; break;
   }
  }
 }
}
