/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gdraw;

import java.io.PrintWriter;

import java.util.ArrayList;

import gdraw.Util.Pt;

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
  max = 0;
 }

 public void setDebug(PrintWriter dbg)
 {
  if (dbg != null)
  {
   dbgFlag = true;
   this.dbg = dbg;
  }
 }

 /**
  * Create a new track and add to track list.
  * 
  * @param p0 starting point of track
  * @param p1 ending point of track
  * @param a aperture used to create track
  * @return 
  */
 public Track add(Pt p0, Pt p1, ApertureList.Aperture a)
 {
  Track t = null;
  if (!p0.equals(p1))
  {
   add(t = new Track(max++, p0, p1, a));
//   t.print();
  }
  return(t);
 }

 public void rotate(int xMax)
 {
  for (Track t : this)
  {
   t.pt[0].rotate(xMax);
   t.pt[1].rotate(xMax);
  }
 }

 public void mirror(int xSize, int ySize)
 {
  for (Track t : this)
  {
   t.pt[0].mirror(xSize, ySize);
   t.pt[1].mirror(xSize, ySize);
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
   for (Track t : this)
   {
    t.print();
   }
  }
 }

 /*
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
 */

 public class Track implements Comparable<TrackList.Track>
 {
  Pt[] pt;			/* track end points */
  int index;			/* track number */
  int gIndex;			/* track number in gerber file */
  ApertureList.Aperture ap;	/* aperture for track */

  public Track(int i, Pt p0, Pt p1, ApertureList.Aperture a)
  {
   pt = new Pt[2];
   index = i;
   pt[0] = p0;
   pt[1] = p1;
   ap = a;
  }

  @Override public int compareTo(Track track)
  {
   int compare;

   compare = pt[0].x - track.pt[0].x;
   if (compare == 0)
   {
    compare = pt[0].y - track.pt[0].y;
   }
   return(compare);
  }

  public void print()
  {
   if (dbgFlag)
   {
    int len = (int) pt[0].dist(pt[1]);
    dbg.printf("trk %3d %3d x %6d y %6d " +
	       "x %6d  y %6d ap %2d %4.3f l %6d\n",
	       index, gIndex,
	       pt[0].x, pt[0].y,
	       pt[1].x, pt[1].y,
	       ap.index, ap.val1, len);
    dbg.flush();
   }
  }
 }
}
