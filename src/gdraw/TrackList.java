/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gdraw;

import java.util.ArrayList;
import java.io.PrintWriter;
import gdraw.Util.*;

/*
 * TrackList.java
 *
 * Created on Apr 8, 2008 at 7:03:31 AM
 *
 * @author Eric Nystrom
 *
 */

public class TrackList extends ArrayList<TrackList.Track>
{
 GDraw gerber;
 boolean dbgFlag;
 PrintWriter dbg;
 int max;

 public static final int MIN_DIST = 5;

 /**
  * Create track list.
  * 
  * @param g
  */
 public TrackList(GDraw g)
 {
  gerber = g;
  dbg = g.dbg;
  dbgFlag = (dbg != null);
  max = 0;
 }

 /**
  * Create a new track and add to track list.
  * 
  * @param p0 starting point of track
  * @param p1 ending point of track
  * @param a aperture used to create track
  */
 public void add(Pt p0, Pt p1, ApertureList.Aperture a)
 {
  Track t;
  if (!p0.equals(p1))
  {
   add(t = new Track(max++,p0,p1,a));
   t.print();
  }
 }

 /**
  * Print track list to debug file.
  */
 public void print()
 {
  if (dbgFlag)
  {
   dbg.printf("\nTrack List\n\n");
   for (int i = 0; i < max; i++)
   {
    get(i).print();
   }
  }
 }

 public void draw(Image image)
 {
  if (dbgFlag)
  {
   dbg.printf("\nDraw Tracks\n\n");
  }
  for (int i = 0; i < max; i++)
  {
   Track t = get(i);
   image.draw(t);
  }
 }

 public class Track
 {
  Pt[] pt;			/* track end points */
  int index;			/* track number */
  ApertureList.Aperture ap;	/* aperture for track */

  public Track(int i, Pt p0, Pt p1, ApertureList.Aperture a)
  {
   pt = new Pt[2];
   index = i;
   pt[0] = p0;
   pt[1] = p1;
   ap = a;
  }

  public void print()
  {
   if (dbgFlag)
   {
    int len = (int) pt[0].dist(pt[1]);
    dbg.printf("%3d x %6d y %6d " +
	       "x %6d  y %6d %2d %4.3f l %6d\n",
	       index,
	       pt[0].x,pt[0].y,
	       pt[1].x,pt[1].y,
	       ap.index,ap.val1,len);
   }
  }
 }
}
