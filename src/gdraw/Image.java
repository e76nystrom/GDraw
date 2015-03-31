/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gdraw;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

import java.awt.image.BufferedImage;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.ArrayList;

import javax.imageio.ImageIO;

import gdraw.Util.*;

//import org.jgap.*;
//import org.jgap.impl.*;
//import org.jgap.impl.salesman.*;

/**
 *
 * @author Eric
 */
public class Image
{
 GDraw gdraw;
 boolean dbgFlag;
 PrintWriter dbg;
 PrintWriter out;
 BufferedImage image;
 int[] data;
 Graphics2D g;
 int w0;
 int h0;
 int segNum;
 float scale;
 float ncScale;
 Seg seg;
 SegList segList;
 boolean trackError = false;

 public static final int BACKGROUND = new Color(0xe0,0xff,0xff).getRGB();
 public static final int GRID       = new Color(0xe0,0x00,0xff).getRGB();
 public static final int EDGE   = Color.RED.getRGB();
 public static final int E      = 0x000100000;
 public static final int TRACK  = Color.BLACK.getRGB();
 public static final int PAD    = Color.GRAY.getRGB();
 public static final int PATH   = 0x0000ff00;
 public static final int NOPATH = 0x00e00000;
 public static final int SHORT  = 0x00008000;
 public static final int START  = 0x000000ff;
 public static final int STRERR = 0x00000080;
 public static final int TRKERR = 0x00008080;

 public static final boolean BMP = false;

//   3 2 1
//    \|/  
//  4--+--0
//    /|\
//   5 6 7

 @SuppressWarnings("DeadBranch")
 public Image(GDraw gd, int w, int h, float s)
 {
  gdraw = gd;
  dbg = gdraw.dbg;
  dbgFlag = (dbg != null);
  out = gdraw.out;
  w0 = w;
  h0 = h;
  scale = s;
  ncScale = (float) GDraw.SCALE / s;
  image = new BufferedImage(w0,h0,BufferedImage.TYPE_INT_RGB);
  g = image.createGraphics();
  g.setColor(new Color(BACKGROUND));
  g.fillRect(0,0,w0,h0);
  g.setColor(Color.BLACK);
  data = new int[w0 * h0];
  if (false)
  {
   System.out.printf("background %08x\n",BACKGROUND);
   System.out.printf("edge       %08x\n",EDGE);
   System.out.printf("e          %08x\n",E);
   System.out.printf("track      %08x\n",TRACK);
   System.out.printf("pad        %08x\n",PAD);
   System.out.printf("path       %08x\n",PATH);
   System.out.printf("nopath     %08x\n",NOPATH);
   System.out.printf("start      %08x\n",START);
  }
 }

 public void getData()
 {
  image.getRGB(0,0,w0,h0,data,0,w0);
 }

 public void setData()
 {
  image.setRGB(0,0,w0,h0,data,0,w0);
 }

 public void circle(int x, int y, int r)
 {
  Circle c = new Circle(dbg,r);
  c.fill(data,w0,x,y);
 }

 @SuppressWarnings("DeadBranch")
 public void draw(PadList.Pad pad)
 {
  ApertureList.Aperture ap = pad.ap;
  Pt pt = pad.pt;
  if (ap != null)
  {
   if (ap.type == ApertureList.Aperture.ROUND)
   {
    g.setColor(Color.GRAY);
    int w = (int) (ap.val1 * GDraw.SCALE);
    int x = (int) ((pt.x - w / 2) / scale);
    int y = (int) ((pt.y - w / 2) / scale);
    w = (int) ((ap.val1 * GDraw.SCALE) / scale);
    if (dbgFlag)
    {
     dbg.printf("%3d x %6d y %6d x %6d y %6d w %3d\n",
		pad.index,(int) (pt.x / scale),(int) (pt.y / scale),
		x,y,w);
     dbg.flush();
    }
    if (true)
    {
     ap.draw(this,pt);
    }
    else
    {
     g.fillOval(x,y,w,w);
    }
   }
   else if (ap.type == ApertureList.Aperture.SQUARE)
   {
    g.setColor(Color.BLACK);
    int w = ((int) (ap.val1 * GDraw.SCALE));
    int h = ((int) (ap.val2 * GDraw.SCALE));
    int x = (int) ((pt.x - w / 2) / scale);
    int y = (int) ((pt.y - h / 2) / scale);
    w = (int) ((ap.val1 * GDraw.SCALE) / scale);
    h = (int) ((ap.val2 * GDraw.SCALE) / scale);
    if (dbgFlag)
    {
     dbg.printf("%3d x %6d y %6d x0 %6d y0 %6d x1 %6d y1 %6d w %3d h %3d\n",
		pad.index,(int) (pt.x / scale),(int) (pt.y / scale),
		x,y,x + w,y + h,w,h);
     dbg.flush();
    }
    if (true)
    {
     int i0 = y * w0 + x;
     for (int i = 0; i < h; i++)
     {
      int i1 = i0;
      for (int j = 0; j < w; j++)
      {
       data[i1++] = TRACK;
      }
      i0 += w0;
     }
    }
    else
    {
     g.fillRect(x,y,w,h);
    }
   }
   if (dbgFlag)
   {
    dbg.flush();
   }
  }
 }

/*
 public void draw(TrackList.Track track)
 {
  g.setColor(Color.BLACK);
  ApertureList.Aperture ap = track.ap;
  float w;
  if (ap.val1 <= .011)
  {
   w = (float) 1.0;
  }
  else
  {
   w = (float) ((ap.val1 * GDraw.SCALE) / scale);
  }
  BasicStroke stroke = new BasicStroke(w,BasicStroke.CAP_ROUND,
				       BasicStroke.JOIN_MITER);
  g.setStroke(stroke);
  Line2D s = new Line2D.Float((float) (track.pt[0].x / scale),
			      (float) (track.pt[0].y / scale),
			      (float) (track.pt[1].x / scale),
			      (float) (track.pt[1].y / scale));
  g.draw(s);
  if (dbgFlag)
  {
   dbg.flush();
  }
 }
*/

/*
 public void testDraw()
 {
  float w = 30.0F;
  BasicStroke stroke = new BasicStroke(w,BasicStroke.CAP_ROUND,
				       BasicStroke.JOIN_MITER);
  Path2D p = new Path2D.Float();
  p.moveTo(75F,2000F);
  p.lineTo(75F,75F);
  p.lineTo(2000F,75F);
  g.draw(p);
 }
*/

 public void drawTracks()
 {
  if (dbgFlag)
  {
   dbg.printf("\nDraw Tracks\n\n");
  }
  g.setColor(Color.BLACK);
  TrackList tl = gdraw.trackList;
  for (int i = 0; i < tl.size(); i++)
  {
   float width;
   TrackList.Track t0 = tl.get(i);
   float wx0 = (float) ((t0.ap.val1 * GDraw.SCALE) / scale);
   for (int j = i + 1; j < tl.size(); j++)
   {
    TrackList.Track t1 = tl.get(j);
    float w1 = (float) ((t1.ap.val1 * GDraw.SCALE) / scale);
//    width = (wx0 < w1) ? wx0 : w1;
    width = w1;

    if (width <= 20.0)
    {
     width = (float) 1.0;
    }

    if (t0.pt[0].equals(t1.pt[0]))
    {
     if (dbgFlag)
     {
      dbg.printf("trk %3d %3d w %3.0f\n",t0.index,t1.index,width);
     }
     dr(width,t0.pt[1],t0.pt[0],t1.pt[1]);
    }
    else if (t0.pt[0].equals(t1.pt[1]))
    {
     if (dbgFlag)
     {
      dbg.printf("trk %3d %3d w %3.0f\n",t0.index,t1.index,width);
     }
     dr(width,t0.pt[1],t0.pt[0],t1.pt[0]);
    }
    else if (t0.pt[1].equals(t1.pt[0]))
    {
     if (dbgFlag)
     {
      dbg.printf("trk %3d %3d w %3.0f\n",t0.index,t1.index,width);
     }
     dr(width,t0.pt[0],t0.pt[1],t1.pt[1]);
    }
    else if (t0.pt[1].equals(t1.pt[1]))
    {
     if (dbgFlag)
     {
      dbg.printf("trk %3d %3d w %3.0f\n",t0.index,t1.index,width);
     }
     dr(width,t0.pt[0],t0.pt[1],t1.pt[0]);
    }
   }

   if (wx0 <= 20.0)
   {
    wx0 = (float) 1.0;
   }

   if (dbgFlag)
   {
    dbg.printf("trk %3d x0 %5d y0 %5d x1 %5d y1 %5d w %3.0f\n",t0.index,
	       t0.pt[0].x,t0.pt[0].y,
	       t0.pt[1].x,t0.pt[1].y,wx0);
   }
   BasicStroke stroke = new BasicStroke(wx0,BasicStroke.CAP_ROUND,
					BasicStroke.JOIN_MITER);
   g.setStroke(stroke);
   Line2D s = new Line2D.Float((float) (t0.pt[0].x / scale),
			       (float) (t0.pt[0].y / scale),
			       (float) (t0.pt[1].x / scale),
			       (float) (t0.pt[1].y / scale));
   g.draw(s);
  }
 }

 public void dr(float width, Pt p0, Pt p1, Pt p2)
 {
  PadList pList = gdraw.padList;
  if (dbgFlag)
  {
   dbg.printf("x %5d y %5d\n",p1.x,p1.y);
  }
  for (int i = 0; i < pList.size() - 1; i++)
  {
   PadList.Pad pi = pList.get(i);
   if (pi.connected(p1))
   {
    if (dbgFlag)
    {
     dbg.printf("pad %3d x  %5d y  %5d\n",pi.index,p1.x,p1.y);
    }
    return;
   }
  }
  BasicStroke stroke = new BasicStroke(width,BasicStroke.CAP_ROUND,
				       BasicStroke.JOIN_ROUND);

  g.setStroke(stroke);

  Path2D p = new Path2D.Float();
  p.moveTo(p0.x / scale,p0.y / scale);
  p.lineTo(p1.x / scale,p1.y / scale);
  p.lineTo(p2.x / scale,p2.y / scale);
  g.draw(p);
 }

 public void adjacentPads()
 {
  if (dbgFlag)
  {
   dbg.printf("\nAdjacent Pads\n\n");
  }
  image.getRGB(0,0,w0,h0,data,0,w0);
  BasicStroke stroke = new BasicStroke(1,BasicStroke.CAP_ROUND,
				       BasicStroke.JOIN_MITER);
  g.setStroke(stroke);
  g.setColor(Color.BLACK);

  int maxDist = (int) (GDraw.SCALE * .125);
  int minDist = (int) (GDraw.SCALE * .075);

  PadList pList = gdraw.padList;
  for (int i = 0; i < pList.size() - 1; i++)
  {
   int dist;
   boolean vertical;
   PadList.Pad pi = pList.get(i);
   for (int j = i + 1; j < pList.size(); j++)
   {
    PadList.Pad pj = pList.get(j);
    if (pi.ap.type != pj.ap.type)
    {
     continue;
    }

    if (pi.pt.x == pj.pt.x)
    {
     dist = pj.pt.y - pi.pt.y;
     vertical = true;
    }
    else if (pi.pt.y == pj.pt.y)
    {
     dist = pj.pt.x - pi.pt.x;
     vertical = false;
    }
    else
    {
     continue;
    }

    PadList.Pad p0;
    PadList.Pad p1;
    if (dist < 0)
    {
     dist = -dist;
     p0 = pj;
     p1 = pi;
    }
    else
    {
     p0 = pi;
     p1 = pj;
    }

    if ((dist >= minDist)
    &&  (dist <= maxDist))
    {
     int x;
     int y;
     if (vertical)
     {
      if (dbgFlag)
      {
       dbg.printf("pad v %3d %3d x %6d yi %6d yj %6d dist %6d %5.3f %s %5.3f %s\n",
		  p0.index,p1.index,p0.pt.x,p0.pt.y,p1.pt.y,dist,
		  p0.ap.val1,p0.ap.typeStr,p1.ap.val1,p1.ap.typeStr);
      }
      x = p0.pt.x;
      int y0 = p0.pt.y + (int) (p0.ap.val2 * GDraw.SCALE / 2);
      int y1 = p1.pt.y - (int) (p1.ap.val2 * GDraw.SCALE / 2);
      y = ((y1 - y0) / 2) + y0;
     }
     else
     {
      if (dbgFlag)
      {
       dbg.printf("pad h %3d %3d y %6d xi %6d xj %6d dist %6d %5.3f %s %5.3f %s\n",
		  p0.index,p1.index,p0.pt.y,p0.pt.x,p1.pt.x,dist,
		  p0.ap.val1,p0.ap.typeStr,p1.ap.val1,p1.ap.typeStr);
      }
      int x0 = p0.pt.x + (int) (p0.ap.val1 * GDraw.SCALE / 2);
      int x1 = p1.pt.x - (int) (p1.ap.val1 * GDraw.SCALE / 2);
      x = ((x1 - x0) / 2) + x0;
      y = p0.pt.y;
     }

     x = (int) (x / scale);
     y = (int) (y / scale);

     int i0 = x + y * w0;
     int w;
     int h;

     if (dbgFlag)
     {
      dbg.printf("check x %6d y %6d\n",x,y);
     }
     if (p0.ap.type == ApertureList.Aperture.SQUARE)
     {
      if (data[i0] == BACKGROUND)
      {
       if (dbgFlag)
       {
	dbg.printf("r x %6d y %6d\n",x,y);
       }
//       if (((dist >= 1100) && (dist <= 1110))
//       ||  ((dist >= 890) && (dist <= 920)))
       {
	if (vertical)
	{
	 w = (int) (p0.ap.val1 * GDraw.SCALE);
	 h = dist - (int) ((p0.ap.val2 + .010) * GDraw.SCALE);
	}
	else
	{
	 w = dist - (int) ((p0.ap.val1 + .010) * GDraw.SCALE);
	 h = (int) (p0.ap.val2 * GDraw.SCALE);
	}
	w = (int) (w / scale);
	h = (int) (h / scale);
	if (checkRect(x - w / 2,y - h / 2,w,h))
	{
	 g.fillRect(x - w / 2,y - h / 2,w,h);
	}
	if (dbgFlag)
	{
	 dbg.printf("rec %3d x %5d y %5d w %3d h %3d\n",p0.index,x,y,w,h);
	}
       }
      }
     }
     else if (p0.ap.type == ApertureList.Aperture.ROUND)
     {
      if (dbgFlag)
      {
       dbg.printf("c x %6d y %6d\n",x,y);
      }
      double minAperture = p0.ap.val1;
      if (p1.ap.val1 < minAperture)
      {
       minAperture = p1.ap.val1;
      }
      int r = (int) ((minAperture * GDraw.SCALE / scale) / 2);
      if (vertical)
      {
       padVertLine(x - r,y,r);
       padVertLine(x + r,y,r);
      }
      else
      {
       padHorizLine(x,y - r,r);
       padHorizLine(x,y + r,r);
      }
     }
     if (dbgFlag)
     {
      dbg.printf("\n");
     }
    }
   }
  }
 }

 public boolean checkRect(int x0, int y0, int w, int h)
 {
  int x1 = x0 + w;
  int y1 = y0 + h;
  for (int y = y0; y <= y1; y++)
  {
   int i0 = x0 + y * w0;
   for (int x = x0; x <= x1; x++, i0++)
   {
    if (data[i0] != BACKGROUND)
     return(false);
   }
  }
  return(true);
 }

 public void padVertLine(int x, int y, int limit)
 {
  int max;
  for (max = 0; max < limit; max++)
  {
   int i0 = x + (y + max) * w0;
   if ((data[i0] != BACKGROUND)
   ||  (data[i0 - 1] != BACKGROUND)
   ||  (data[i0 + 1] != BACKGROUND))
   {
    break;
   }
  }

  int min;
  for (min = 0; min < limit; min++)
  {
   int i0 = x + (y - min) * w0;
   if ((data[i0] != BACKGROUND)
   ||  (data[i0 - 1] != BACKGROUND)
   ||  (data[i0 + 1] != BACKGROUND))
   {
    break;
   }
  }
  if (max != 0)
  {
   g.drawLine(x,y - (min - 3),x,y + (max - 3));
  }
 }

 public void padHorizLine(int x, int y, int limit)
 {
  int max;
  for (max = 0; max < limit; max++)
  {
   int i0 = (x + max) + y * w0;
   if ((data[i0] != BACKGROUND)
   ||  (data[i0 - w0] != BACKGROUND)
   ||  (data[i0 + w0] != BACKGROUND))
   {
    break;
   }
  }

  int min;
  for (min = 0; min < limit; min++)
  {
   int i0 = (x - min) + y * w0;
   if ((data[i0] != BACKGROUND)
   ||  (data[i0 - w0] != BACKGROUND)
   ||  (data[i0 + w0] != BACKGROUND))
   {
    break;
   }
  }
  if (max != 0)
  {
   g.drawLine(x - (min - 3),y,x + (max - 3),y);
  }
 }

 public void padTrack()
 {
  if (dbgFlag)
  {
   dbg.printf("\nPads near Tracks\n\n");
  }
  g.setColor(Color.BLACK);
  BasicStroke stroke = new BasicStroke(1,BasicStroke.CAP_ROUND,
				       BasicStroke.JOIN_MITER);
  g.setStroke(stroke);

  PadList pList = gdraw.padList;
  for (PadList.Pad pad : pList)
  {
   Pt pt = pad.pt;
   TrackList tl = gdraw.trackList;
   for (TrackList.Track trk : tl)
   {
    Pt p0 = trk.pt[0];
    Pt p1 = trk.pt[1];
    boolean flag = false;
    boolean vertical = false;
    boolean oblique = false;
    int dist = 0;
    String dir;
    if (p0.x == p1.x)		/* if vertical */
    {
     dir= "v";
     vertical = true;
     if (p0.y < p1.y)
     {
      if ((pt.y > p0.y)
              &&  (pt.y < p1.y))
      {
       flag = true;
      }
     }
     else
     {
      if ((pt.y > p1.y)
              &&  (pt.y < p0.y))
      {
       flag = true;
      }
     }
     if (flag)
     {
      dist = (int) ((pt.x - p0.x) / scale);
     }
    }
    else if (p0.y == p1.y)	/* if horizontal */
    {
     dir = "h";
     if (p0.x < p1.x)
     {
      if ((pt.x > p0.x)
              &&  (pt.x < p1.x))
      {
       flag = true;
      }
     }
     else
     {
      if ((pt.x > p1.x)
              &&  (pt.x < p0.x))
      {
       flag = true;
      }
     }
     if (flag)
     {
      dist = (int) ((pt.y - p0.y) / scale);
     }
    }
    else
    {
     dir = "o";
     vertical = false;
     oblique = true;
    }
    if (flag)
    {
     int tmp = dist;
     if (tmp < 0)
     {
      tmp = -tmp;
     }

     if (tmp <= 75)
     {
      String ap;
      if (pad.ap.type == ApertureList.Aperture.ROUND)
      {
       ap = "C";
      }
      else
      {
       ap = "R";
      }
      if (dbgFlag)
      {
       dbg.printf("pad %3d %s x %6d y %6d  trk %3d (%6d,%6d) (%6d,%6d) %s dist %6d\n",
               pad.index,ap,pt.x,pt.y,
               trk.index,p0.x,p0.y,p1.x,p1.y,dir,dist);
      }
      if (pad.ap.type == ApertureList.Aperture.ROUND)
      {
       int r = (int) ((pad.ap.val1 * GDraw.SCALE / scale) / 2);
       int x = (int) (pt.x / scale);
       int y = (int) (pt.y / scale);
       if (vertical)
       {
        padTrackVert(x,y - r,dist);
        padTrackVert(x,y + r,dist);
       }
       else
       {
        padTrackHoriz(x - r,y,dist);
        padTrackHoriz(x + r,y,dist);
       }
       if (dbgFlag)
       {
        dbg.printf("\n");
       }
      }
     }
    }
   }
  }
 }

 void padTrackVert(int x, int y, int dist)
 {
  if (dbgFlag)
  {
   dbg.printf("x %6d y %6d dist %6d\n",x,y,dist);
  }
  int dir = -1;
  int limit = dist;
  if (dist < 0)
  {
   dir = 1;
   limit = -limit;
  }
  int start;
  for (start = 0; start < limit; start++)
  {
   int i0 = (x + start * dir) + y * w0;
   if ((data[i0] == BACKGROUND)
   &&  (data[i0 - w0] == BACKGROUND)
   &&  (data[i0 + w0] == BACKGROUND))
   {
    break;
   }
  }
  start += 3;

  int end;
  for (end = start; end < limit; end++)
  {
   int i0 = (x + end * dir) + y * w0;
   if ((data[i0] != BACKGROUND)
   ||  (data[i0 - w0] != BACKGROUND)
   ||  (data[i0 + w0] != BACKGROUND))
   {
    break;
   }
  }
  end -= 3;
  if (true)
  {
   g.drawLine(x + (start * dir),y,x + (end * dir),y);
  }
 }

 void padTrackHoriz(int x, int y, int dist)
 {
  if (dbgFlag)
  {
   dbg.printf("x %6d y %6d dist %6d\n",x,y,dist);
  }
  int dir = -1;
  int limit = dist;
  if (dist < 0)
  {
   dir = 1;
   limit = -limit;
  }
  int start;
  for (start = 0; start < limit; start++)
  {
   int i0 = x + (y + (start * dir)) * w0;
//   if (dbgFlag)
//   {
//    dbg.printf("s x %6d y %6d i0 %10d\n",x,y + (start * dir),i0);
//    dbg.flush();
//   }
   if ((data[i0] == BACKGROUND)
   &&  (data[i0 - 1] == BACKGROUND)
   &&  (data[i0 + 1] == BACKGROUND))
   {
    break;
   }
  }
  start += 3;

//  if (dbgFlag)
//  {
//   dbg.printf("\n");
//  }

  int end;
  for (end = start; end < limit; end++)
  {
   int i0 = x + (y + (end * dir)) * w0;
//   if (dbgFlag)
//   {
//    dbg.printf("e x %6d y %6d i0 %10d\n",x,y + (end * dir),i0);
//    dbg.flush();
//   }
   if ((data[i0] != BACKGROUND)
   ||  (data[i0 - 1] != BACKGROUND)
   ||  (data[i0 + 1] != BACKGROUND))
   {
    break;
   }
  }
  end -= 3;
  if (true)
  {
   g.drawLine(x,y + start * dir,x,y + end * dir);
  }
 }

 public void process() throws Exception
 {
  image.getRGB(0,0,w0,h0,data,0,w0);

  for (int i = w0 + 1; i < data.length - (w0 + 1); i++)
  {
   if (data[i] == Circle.EDGE)
   {
    int count = 0;
    if (data[i + 1]      != BACKGROUND) { count++; }
    if (data[i - 1]      != BACKGROUND) { count++; }
    if (data[i + w0]     != BACKGROUND) { count++; }
    if (data[i - w0]     != BACKGROUND) { count++; }
    if (data[i + w0 + 1] != BACKGROUND) { count++; }
    if (data[i + w0 - 1] != BACKGROUND) { count++; }
    if (data[i - w0 + 1] != BACKGROUND) { count++; }
    if (data[i - w0 - 1] != BACKGROUND) { count++; }
    if (count >= 7)
    {
     data[i] = PAD;
    }
   }
  }

  if (BMP)
  {
   write(data,gdraw.baseFile + "0");
  }

  for (int i = w0; i < data.length - w0; i++)
  {
   if (data[i] == BACKGROUND)
   {
    if ((data[i + 1] == TRACK) || (data[i + 1] == PAD))
    {
     data[i + 1] = EDGE;
    }
    if ((data[i + w0] == TRACK) || (data[i + w0] == PAD))
    {
     data[i + w0] = EDGE;
    }
   }
  }

  for (int i = data.length - w0; i > w0; --i)
  {
   if (data[i] == BACKGROUND)
   {
    if ((data[i - 1] == TRACK) || (data[i - 1] == PAD))
    {
     data[i - 1] = EDGE;
    }
     if ((data[i - w0] == TRACK) || (data[i - w0] == PAD))
    {
     data[i - w0] = EDGE;
    }
   }
  }

  int y0 = 100;
  int y1 = h0 - 100;
  for (int x = 100; x < w0 - 100; x++)
  {
   int i0 = x + y0 * w0;
   if (data[i0] == BACKGROUND)
   {
    data[i0] = GRID;
   }
   i0 = x + y1 * w0;
   if (data[i0] == BACKGROUND)
   {
    data[i0] = GRID;
   }
  }

  int x0 = 100;
  int x1 = w0 - 100;
  for (int y = 100; y < h0 - 100; y++)
  {
   int i0 = x0 + y * w0;
   if (data[i0] == BACKGROUND)
   {
    data[i0] = GRID;
   }
   i0 = x1 + y * w0;
   if (data[i0] == BACKGROUND)
   {
    data[i0] = GRID;
   }
  }

  for (int x = 0; x < w0; x += 500)
  {
   for (int y = 0; y < h0; y++)
   {
    int i0 = x + y * w0;
    if (data[i0] == BACKGROUND)
    {
     data[i0] = GRID;
    }
   }
  }

  for (int y = 0; y < h0; y += 500)
  {
   int i0 = y * w0;
   for (int x = 0; x < w0; x++)
   {
    if (data[i0] == BACKGROUND)
    {
     data[i0] = GRID;
    }
    i0++;
   }
  }

  if (BMP)
  {
   write(data,gdraw.baseFile + "1");
  }

  segList = new SegList();
  segNum = 0;
  if (dbgFlag)
  {
   dbg.printf("\nGenerate NC Path\n");
  }
  for (int i = w0; i < data.length - w0; i++)
  {
   if ((data[i] & E) != 0)
   {
    if (dbgFlag)
    {
     dbg.printf("\nseg %3d\n",segNum);
    }
    if (!track(data,i))
    {
     break;
    }
    segNum++;
   }
  }

  setData();
  g.setColor(Color.BLUE);
  BasicStroke stroke = new BasicStroke(1,BasicStroke.CAP_ROUND,
				       BasicStroke.JOIN_MITER);
  g.setStroke(stroke);

  float xCur = 0.0F;
  float yCur = 0.0F;

  double dist;
  if (true)
  {
   segList.add(0,new Seg());
   Seg[] segs = new Seg[segList.size()];
   segList.toArray(segs);

   for (int i = 0; i < segs.length - 2; i++)
   {
    Seg s0 = segs[i];
    double minDist = 99999;
    int index = 0;
    int next = i + 1;
    for (int j = next; j < segs.length; j++)
    {
     dist = s0.dist(segs[j]);
     if (dist < minDist)
     {
      minDist = dist;
      index = j;
     }
    }
    Seg tmp = segs[next];
    segs[next] = segs[index];
    segs[index] = tmp;
   }

   if (GDraw.CSV)
   {
    gdraw.csv.printf("\"Name\",\"X\",\"Y\"\n");
    gdraw.csv.printf("\"\",0,0\n");
   }

   dist = 0.0;
   dbg.printf("\noutput NC path\n\n");
   for (int i = 1; i < segs.length; i++)
   {
    seg = segs[i];
    float x = (float) seg.x;
    float y = (float) seg.y;
    if (GDraw.CSV)
    {
     gdraw.csv.printf("\"%d\",%d,%d\n",
		      i,(int) (x * ncScale),(int) (y * ncScale));
    }
    if (dbgFlag)
    {
     dbg.printf("\nseg %4d len %3d x %6.3f y %6.3f\n\n",seg.num,seg.size(),seg.x,seg.y);
     for (String seg1 : seg)
     {
      dbg.printf("%s", seg1);
     }
    }
    dist += Math.hypot(x - xCur,y - yCur);
    Line2D shape = new Line2D.Float((float) (xCur * ncScale),
				    (float) (yCur * ncScale),
				    (float) (x * ncScale),
				    (float) (y * ncScale));
    g.draw(shape);
    xCur = x;
    yCur = y;
    for (String seg1 : seg)
    {
     out.printf("%s", seg1);
    }
   }
  }
//  else
//  {
//   try
//   {
//    System.out.printf("optimizing\n");
//
//    ImagePath path = new ImagePath();
//    IChromosome optimal = path.findOptimalPath(null);
//    Gene[] gene= optimal.getGenes();
//    System.out.printf("len %3d\n",gene.length);
//
//    for (int i = 0; i < gene.length; i++)
//    {
//     IntegerGene gi = (IntegerGene) gene[i];
//     int k = gi.intValue();
//     Seg s = segList.get(k);
//
//     float x = (float) s.x;
//     float y = (float) s.y;
//     dist += Math.hypot(x - xCur,y - yCur);
//     Line2D shape = new Line2D.Float((float) (xCur * ncScale),
//				     (float) (yCur * ncScale),
//				     (float) (x * ncScale),
//				     (float) (y * ncScale));
//     g.draw(shape);
//     xCur = x;
//     yCur = y;
//
//     for (int j = 0; j < s.size(); j++)
//     {
//      out.printf("%s",s.get(j));
//     }
//    }
//   }
//   catch (Exception ex)
//   {
//    System.out.println(ex);
//   }
//  }
  System.out.printf("dist %6.3f\n",dist);
  getData();
  write(data,gdraw.baseFile);
 }

 public boolean track(int[] data, int i0) throws Exception
 {
  boolean remove = false;
  seg = new Seg(segNum);
  segList.add(seg);

  int x = i0 % w0;
  int y = i0 / w0;

  //  boolean output = (data[i0 - wx0] == BACKGROUND);
  boolean output = true;
  D d = D.XPOS;
  D dLast = d;

  if (output
  &&  (data[i0] != EDGE))
  {
   PadList.Pad pad = gdraw.padList.findPad(new Pt((int) (x * scale),
						  (int) (y * scale)));
   if (pad != null)
   {
    if (dbgFlag)
    {
     dbg.printf("pad %3d x %5d y %5d\n",pad.index,x,y);
    }
    i0 = pad.ap.c.findStart(this,pad.pt,x,y);
    if (i0 == 0)
    {
     pad.ap.c.mark(this,pad.pt);
     double radius = pad.ap.val1 / 2.0;
     double x0 = pad.pt.x / GDraw.SCALE;
     double y0 = pad.pt.y / GDraw.SCALE - radius;
     seg.setLoc(x0,y0);
     seg.add(String.format("g0 x%5.3f y%5.3f (s %3d)\n",x0,y0,segNum));
     seg.add(String.format("g1 z[#1] f[#5]\n"));
     seg.add(String.format("g3 x%5.3f y%5.3f i%5.3f j%5.3f f[#6] " +
			   "(s %3d p %3d)\n",
			   x0,y0,0.0,radius,segNum,pad.index));
     seg.add(String.format("g0 z[#2]\n"));
     return(true);
    }
    int ofs = 0;
    if      (data[i0 + 1] == EDGE)      { d = D.XPOS;       ofs = 1;       }
    else if (data[i0 + 1 + w0] == EDGE) { d = D.XPOS_YPOS;  ofs = 1 + w0;  }
    else if (data[i0 + w0] == EDGE)     { d = D.YPOS;       ofs = w0;      }
    else if (data[i0 - 1 + w0] == EDGE) { d = D.XNEG_YPOS;  ofs = -1 + w0; }
    else if (data[i0 - 1] == EDGE)      { d = D.XNEG;       ofs = -1;      }
    else if (data[i0 - 1 - w0] == EDGE) { d = D.XNEG_YNEG;  ofs = -1 - w0; }
    else if (data[i0 - w0] == EDGE)     { d = D.YNEG;       ofs = -w0;     }
    else if (data[i0 + 1 - w0] == EDGE) { d = D.XPOS_YNEG;  ofs = 1 - w0;  }
    i0 += ofs;
   }
   else
   {
    if (dbgFlag)
    {
     dbg.printf("start pad not found at x %5d y %5d\n",x,y);
    }
    return(false);
   }
  }

  int startIndex = i0;
  x = i0 % w0;
  y = i0 / w0;
  if (dbgFlag)
  {
   dbg.printf("dir %d %9s x %5d y %5d\n",d.ordinal()," ",x,y);
  }

  if ((x < 75)
  ||  (y < 75)
  ||  (x > (w0 - 75))
  ||  (y > (h0 - 75)))
  {
   remove = true;
   if (dbgFlag)
   {
    dbg.printf("remove segment\n");
   }
  }

  if (output)
  {
   double x0 = x / ncScale;
   double y0 = y / ncScale;
   seg.setLoc(x0,y0);
   seg.add(String.format("g0 x%5.3f y%5.3f (s %3d)\n",
			 x0,y0,segNum));
   seg.add(String.format("g1 z[#1] f[#5]\n"));
  }

  int count = 0;
  int len = 0;
  int leg = 0;
  int ofs = 0;
  int i0Last = i0;
  int lastX = 0;
  int lastY = 0;
  while (true)
  {
   switch (d)
   {
   case XPOS:
    if      ((data[i0 + 1] & E) != 0)      { d = D.XPOS;      ofs = 1;       }
    else if ((data[i0 + 1 + w0] & E) != 0) { d = D.XPOS_YPOS; ofs = 1 + w0;  }
    else if ((data[i0 + w0] & E) != 0)     { d = D.YPOS;      ofs = w0;      }
    else if ((data[i0 - 1 + w0] & E) != 0) { d = D.XNEG_YPOS; ofs = -1 + w0; }
    else if ((data[i0 - 1] & E) != 0)      { d = D.XNEG;      ofs = -1;      }
    else if ((data[i0 - 1 - w0] & E) != 0) { d = D.XNEG_YNEG; ofs = -1 - w0; }
    else if ((data[i0 - w0] & E) != 0)     { d = D.YNEG;      ofs = -w0;     }
    else if ((data[i0 + 1 - w0] & E) != 0) { d = D.XPOS_YNEG; ofs = 1 - w0;  }
    else { d = D.DONE; }
    break;

   case XPOS_YPOS:
    if      ((data[i0 + w0] & E) != 0)     { d = D.YPOS;      ofs = w0;      }
    else if ((data[i0 + 1] & E) != 0)      { d = D.XPOS;      ofs = 1;       }
    else if ((data[i0 + 1 + w0] & E) != 0) { d = D.XPOS_YPOS; ofs = 1 + w0;  }
    else if ((data[i0 - 1 + w0] & E) != 0) { d = D.XNEG_YPOS; ofs = -1 + w0; }
    else if ((data[i0 - 1] & E) != 0)      { d = D.XNEG;      ofs = -1;      }
    else if ((data[i0 - 1 - w0] & E) != 0) { d = D.XNEG_YNEG; ofs = -1 - w0; }
    else if ((data[i0 - w0] & E) != 0)     { d = D.YNEG;      ofs = -w0;     }
    else if ((data[i0 + 1 - w0] & E) != 0) { d = D.XPOS_YNEG; ofs = 1 - w0;  }
    else { d = D.DONE; }
    break;

   case YPOS:
    if      ((data[i0 + w0] & E) != 0)     { d = D.YPOS;      ofs = w0;      }
    else if ((data[i0 - 1 + w0] & E) != 0) { d = D.XNEG_YPOS; ofs = -1 + w0; }
    else if ((data[i0 + 1 + w0] & E) != 0) { d = D.XPOS_YPOS; ofs = 1 + w0;  }
    else if ((data[i0 - 1] & E) != 0)      { d = D.XNEG;      ofs = -1;      }
    else if ((data[i0 + 1] & E) != 0)      { d = D.XPOS;      ofs = 1;       }
    else if ((data[i0 - 1 - w0] & E) != 0) { d = D.XNEG_YNEG; ofs = -1 - w0; }
    else if ((data[i0 - w0] & E) != 0)     { d = D.YNEG;      ofs = -w0;     }
    else if ((data[i0 + 1 - w0] & E) != 0) { d = D.XPOS_YNEG; ofs = 1 - w0;  }
    else { d = D.DONE; }
    break;

   case XNEG_YPOS:
    if      ((data[i0 - 1] & E) != 0)      { d = D.XNEG;      ofs = -1;      }
    else if ((data[i0 + w0] & E) != 0)     { d = D.YPOS;      ofs = w0;      }
    else if ((data[i0 - 1 + w0] & E) != 0) { d = D.XNEG_YPOS; ofs = -1 + w0; }
    else if ((data[i0 - 1 - w0] & E) != 0) { d = D.XNEG_YNEG; ofs = -1 - w0; }
    else if ((data[i0 - w0] & E) != 0)     { d = D.YNEG;      ofs = -w0;     }
    else if ((data[i0 + 1 + w0] & E) != 0) { d = D.XPOS_YPOS; ofs = 1 + w0;  }
    else if ((data[i0 + 1] & E) != 0)      { d = D.XPOS;      ofs = 1;       }
    else if ((data[i0 + 1 - w0] & E) != 0) { d = D.XPOS_YNEG; ofs = 1 - w0;  }
    else { d = D.DONE; }
    break;
    
   case XNEG:
    if      ((data[i0 - 1] & E) != 0)      { d = D.XNEG;      ofs = -1;      }
    else if ((data[i0 - 1 - w0] & E) != 0) { d = D.XNEG_YNEG; ofs = -1 - w0; }
    else if ((data[i0 - 1 + w0] & E) != 0) { d = D.XNEG_YPOS; ofs = -1 + w0; }
    else if ((data[i0 - w0] & E) != 0)     { d = D.YNEG;      ofs = -w0;     }
    else if ((data[i0 + w0] & E) != 0)     { d = D.YPOS;      ofs = w0;      }
    else if ((data[i0 + 1 - w0] & E) != 0) { d = D.XPOS_YNEG; ofs = 1 - w0;  }
    else if ((data[i0 + 1 + w0] & E) != 0) { d = D.XPOS_YPOS; ofs = 1 + w0;  }
    else if ((data[i0 + 1] & E) != 0)      { d = D.XPOS;      ofs = 1;       }
    else { d = D.DONE; }
    break;
    
   case XNEG_YNEG:
    if      ((data[i0 - w0] & E) != 0)     { d = D.YNEG;      ofs = -w0;     }
    else if ((data[i0 - 1] & E) != 0)      { d = D.XNEG;      ofs = -1;      }
    else if ((data[i0 - 1 - w0] & E) != 0) { d = D.XNEG_YNEG; ofs = -1 - w0; }
    else if ((data[i0 + 1 - w0] & E) != 0) { d = D.XPOS_YNEG; ofs = 1 - w0;  }
    else if ((data[i0 + 1] & E) != 0)      { d = D.XPOS;      ofs = 1;       }
    else if ((data[i0 + w0] & E) != 0)     { d = D.YPOS;      ofs = w0;      }
    else if ((data[i0 - 1 + w0] & E) != 0) { d = D.XNEG_YPOS; ofs = -1 + w0; }
    else if ((data[i0 + 1 + w0] & E) != 0) { d = D.XPOS_YPOS; ofs = 1 + w0;  }
    else { d = D.DONE; }
    break;

   case YNEG:
    if      ((data[i0 - w0] & E) != 0)     { d = D.YNEG;      ofs = -w0;     }
    else if ((data[i0 + 1 - w0] & E) != 0) { d = D.XPOS_YNEG; ofs = 1 - w0;  }
    else if ((data[i0 + 1] & E) != 0)      { d = D.XPOS;      ofs = 1;       }
    else if ((data[i0 + w0] & E) != 0)     { d = D.YPOS;      ofs = w0;      }
    else if ((data[i0 + 1 + w0] & E) != 0) { d = D.XPOS_YPOS; ofs = 1 + w0;  }
    else if ((data[i0 - 1 + w0] & E) != 0) { d = D.XNEG_YPOS; ofs = -1 + w0; }
    else if ((data[i0 - 1] & E) != 0)      { d = D.XNEG;      ofs = -1;      }
    else if ((data[i0 - 1 - w0] & E) != 0) { d = D.XNEG_YNEG; ofs = -1 - w0; }
    else { d = D.DONE; }
    break;

   case XPOS_YNEG:
    if      ((data[i0 + 1] & E) != 0)      { d = D.XPOS;      ofs = 1;       }
    else if ((data[i0 - w0] & E) != 0)     { d = D.YNEG;      ofs = -w0;     }
    else if ((data[i0 + 1 - w0] & E) != 0) { d = D.XPOS_YNEG; ofs = 1 - w0;  }
    else if ((data[i0 + 1 + w0] & E) != 0) { d = D.XPOS_YPOS; ofs = 1 + w0;  }
    else if ((data[i0 + w0] & E) != 0)     { d = D.YPOS;      ofs = w0;      }
    else if ((data[i0 - 1 + w0] & E) != 0) { d = D.XNEG_YPOS; ofs = -1 + w0; }
    else if ((data[i0 - 1] & E) != 0)      { d = D.XNEG;      ofs = -1;      }
    else if ((data[i0 - 1 - w0] & E) != 0) { d = D.XNEG_YNEG; ofs = -1 - w0; }
    else { d = D.DONE; }
    break;
   }

   if (d == D.DONE)
   {
    break;
   }
    
   i0 += ofs;

   if (output
   &&  (data[i0] != EDGE))
   {
    x = i0 % w0;
    y = i0 / w0;
    PadList.Pad pad = gdraw.padList.findPad(new Pt((int) (x * scale),
						   (int) (y * scale)));
    if (pad != null)
    {
     int i0Pad = i0;
     if (dbgFlag)
     {
      dbg.printf("pad %3d x %5d y %5d\n",pad.index,x,y);
      dbg.flush();
     }

     double x0 = x / ncScale;
     double y0 = y / ncScale;
     lastX = x;
     lastY = y;
     seg.add(String.format("g1 x%5.3f y%5.3f f[#5] (s %3d l %4d %4d)\n",
			   x0,y0,segNum,leg,len));

     i0 = pad.ap.c.markArc(this,pad.pt,x,y);
     if (i0 != i0Pad)
     {
      int ix1 = i0 % w0;
      int iy1 = i0 / w0;
      double x1 = ix1 / ncScale;
      double y1 = iy1 / ncScale;
      double dist = Math.hypot(x1 - x0,y1 - y0);
      if (dist <= .003)
      {
       if (dbgFlag)
       {
	dbg.printf("pad %3d x %5d y %5d x %5d y %5d dist %5.3f\n",
		   pad.index,x,y,ix1,iy1,dist);
	dbg.flush();
       }
       seg.add(String.format("g1 x%5.3f y%5.3f f[#5] (s %3d p %3d)\n",
			     x1,y1,segNum,pad.index));
      }
      else
      {
       double cx = (pad.pt.x / scale) / ncScale;
       double cy = (pad.pt.y / scale) / ncScale;

       double i = cx - x0;
       double j = cy - y0;
       double r0 = Math.hypot(i,j);
       double r1 = Math.hypot(cx - x1,cy - y1);
       double err = Math.abs(r0 - r1);
       if (dbgFlag)
       {
	dbg.printf("r0 %8.6f r1 %8.6f diff %8.6f\n",r0,r1,err);
       }
       if (err < .0005)
       {
	seg.add(String.format("g3 x%5.3f y%5.3f i%5.3f j%5.3f f[#6] " +
			      "(s %3d p %3d)\n",
			      x1,y1,i,j,segNum,pad.index));
       }
       else
       {
	double xt0 = x0 - cx;
	double yt0 = y0 - cy;
	double xt1 = x1 - cx;
	double yt1 = y1 - cy;
	if ((xt0 - xt1) > (yt0 - yt1))
	{
	 if (r0 > Math.abs(xt1))
	 {
	  if (yt1 > 0)
	  {
	   yt1 = Math.sqrt(r0 * r0 - xt1 * xt1);
	  }
	  else
	  {
	   yt1 = -Math.sqrt(r0 * r0 - xt1 * xt1);
	  }
	 }
	 else
	 {
	  if (xt1 > 0)
	  {
	   xt1 = Math.sqrt(r0 * r0 - yt1 * yt1);
	  }
	  else
	  {
	   xt1 = -Math.sqrt(r0 * r0 - yt1 * yt1);
	  }
	 }
	}
	else
	{
	 if (xt1 > 0)
	 {
	  xt1 = Math.sqrt(r0 * r0 - yt1 * yt1);
	 }
	 else
	 {
	  xt1 = -Math.sqrt(r0 * r0 - yt1 * yt1);
	 }
	}
	xt1 += cx;
	yt1 += cy;
	double xn = xt1 - cx;
	double yn = yt1 - cy;
	r1 = Math.hypot(xn,yn);
	err = Math.abs(r0 - r1);
	if (dbgFlag)
	{
	 dbg.printf("x %7.5f y %7.5f err %6.0f %6.4f\n",xn,yn,r1,err);
	}
	seg.add(String.format("g3 x%5.5f y%5.5f i%5.5f j%5.5f f[#6] " +
			      "(s %3d p %3d f)\n",
			      xt1,yt1,i,j,segNum,pad.index));
	seg.add(String.format("g1 x%5.5f y%5.5f f[#5]()\n",x1,y1));
       }
      }
     }
    }
    else
    {
     if (dbgFlag)
     {
      dbg.printf("pad not found at x %5d y %5d\n",x,y);
     }
     if (remove)
     {
      segList.remove(seg);
     }
     return(false);
    }

    i0Last = i0;
    dLast = D.INVALID;
    len = 0;
    leg++;
    continue;
   }

   data[i0] = output ? PATH : NOPATH;
   count++;
   len++;
   if (d != dLast)
   {
    x = i0Last % w0;
    y = i0Last / w0;
    if (dbgFlag)
    {
     dbg.printf("dir %d %4d %4d x %5d y %5d\n",
		dLast.ordinal(),count,len,x,y);
    }
    if (output)
    {
     seg.add(String.format("g1 x%5.3f y%5.3f f[#5] (s %3d l %4d %4d)\n",
                           x / ncScale,y / ncScale,segNum,leg,len));
    }
    leg++;
    len = 0;
    dLast = d;
   }
   i0Last = i0;
  }

  if (count <= 2)
  {
   data[i0] = SHORT;
   if (dbgFlag)
   {
    dbg.printf("short %10d\n",i0);
   }
  }

  x = i0 % w0;
  y = i0 / w0;
  int tmp = i0 - startIndex;
  if (dbgFlag)
  {
   dbg.printf("dir %d %4d %4d x %5d y %5d\n",d.ordinal(),count,len,x,y);
   dbg.printf("start %8d x %4d y %4d end %8d x %4d y %4d %8d\n",
	      startIndex,startIndex % w0,startIndex / w0,i0,x,y,tmp);
   dbg.flush();
  }

  if (tmp != 0)
  {
   data[startIndex] = STRERR;
   data[i0] = TRKERR;
   if (dbgFlag)
   {
    dbg.printf("track error seg %d\n",segNum);
    trackError = true;
//    System.err.printf("track error\n");
   }
//   return(false);
  }
  else
  {
   for (int i = 1; i < 10; i++)
   {
    data[i0 - i * w0] = START;
   }
  }

  if (output)
  {
   seg.add(String.format("g1 x%5.3f y%5.3f f[#5] (s %3d l %4d)\n",
			 x / ncScale,y / ncScale,segNum,leg));
   seg.add(String.format("g0 z[#2]\n"));
   out.flush();
  }

  if (remove)
//  ||  (seg.size() == 4))
  {
   if (dbgFlag)
   {
    dbg.printf("remove segment\n");
   }
   segList.remove(seg);
  }
  return(true);
 }

 public void write(int[] data, String f)
 {
  int j = h0 * w0;
  for (int i = 0; i < h0; i++)
  {
   j -= w0;
   image.setRGB(0,i,w0,1,data,j,w0);
  }
  write(f);
 }

 public void write(String f)
 {
  File file = new File(f + ".png");

  try
  {
   ImageIO.write(image,"png",file);
  }
  catch (IOException e)
  {
   System.out.println(e);
  }
 }

 public class Seg extends ArrayList<String>
 {
  int num;
  double x;
  double y;

  public Seg()
  {
   super();
   num = -1;
   x = 0;
   y = 0;
  }

  public Seg(int num)
  {
   super();
   this.num = num;
   x = 0;
   y = 0;
  }

/*
  @Override public boolean add(String s)
  {
   if (dbgFlag)
   {
    dbg.printf("%s",s);
   }
   return(super.add(s));
  }
*/

  public void setLoc(double xLoc, double yLoc)
  {
   x = xLoc;
   y = yLoc;
  }

  public double dist(Seg s)
  {
   return(Math.hypot(s.x - x,s.y - y));
  }
 }

 public class SegList extends ArrayList<Image.Seg>
 {
 }

// public class ImagePath extends Salesman
// {
//  @Override public IChromosome createSampleChromosome(Object a_initial_data)
//  {
//   try
//   {
//    int listSize = segList.size();
//    Gene[] genes = new Gene[listSize];
//    for (int i = 0; i < genes.length; i++)
//    {
//     genes[i] = new IntegerGene(getConfiguration(), 0, listSize - 1);
//     genes[i].setAllele(new Integer(i));
//    }
//
//    IChromosome sample = new Chromosome(getConfiguration(), genes);
//    return(sample);
//   }
//   catch (InvalidConfigurationException iex)
//   {
//    throw new IllegalStateException(iex.getMessage());
//   }
//  }
//
//  @Override public double distance(Gene a_from, Gene a_to)
//  {
//   IntegerGene geneA = (IntegerGene) a_from;
//   IntegerGene geneB = (IntegerGene) a_to;
//   int a = geneA.intValue();
//   int b = geneB.intValue();
//   Seg s0 = segList.get(a);
//   Seg s1 = segList.get(b);
//   return(Math.hypot(s0.x - s1.x,s0.y - s1.y));
//  }
// }
}
