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

import gdraw.Util.D;
import gdraw.Util.Pt;

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
 int imageSize;
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
 public static final int CUT    = Color.BLUE.getRGB();
 public static final int PATH   = 0x0000ff00;
 public static final int NOPATH = 0x00e00000;
 public static final int SHORT  = 0x00008000;
 public static final int START  = 0x000000ff;
 public static final int STRERR = 0x00000080;
 public static final int TRKERR = 0x00008080;

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
  imageSize = w * h;
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
    int w = (int) (ap.iVal1 / (2 * scale));
    int x = (int) (pt.x / scale) - w;
    int y = (int) (pt.y / scale) - w;
    if (dbgFlag)
    {
     dbg.printf("%3d x %6d y %6d x %6d y %6d w %3d\n",
		pad.index,(int) (pt.x / scale),(int) (pt.y / scale),
		x,y,2 * w);
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
    int w = (int) (ap.iVal1 / (2 * scale));
    int h = (int) (ap.iVal2 / (2 * scale));
    int x = (int) (pt.x / scale) - w;
    int y = (int) (pt.y / scale) - h;
    w *= 2;
    h *= 2;
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
     for (int i = 0; i <= h; i++)
     {
      int i1 = i0;
      for (int j = 0; j <= w; j++)
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
   float wx0 = (float) (t0.ap.iVal1 / scale);
   for (int j = i + 1; j < tl.size(); j++)
   {
    TrackList.Track t1 = tl.get(j);
    float w1 = (float) (t1.ap.iVal1 / scale);
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

 public void adjacentPads(boolean scanVertical)
 {
  if (dbgFlag)
  {
   dbg.printf("\nAdjacent Pads %s\n\n",
	      scanVertical ? "Vertical" : "Horizontal");
  }
  image.getRGB(0,0,w0,h0,data,0,w0);
  BasicStroke stroke = new BasicStroke(1,BasicStroke.CAP_ROUND,
				       BasicStroke.JOIN_MITER);
  g.setStroke(stroke);
  g.setColor(Color.BLUE);

  int maxDistSquare = (int) (GDraw.SCALE * .175);
  int maxDistRound = (int) (GDraw.SCALE * .125);
  int minDist = (int) (GDraw.SCALE * .075);

  PadList pList = gdraw.padList;
  for (int i = 0; i < pList.size() - 1; i++)
  {
   int dist;
   PadList.Pad pi = pList.get(i);
   for (int j = i + 1; j < pList.size(); j++)
   {
    PadList.Pad pj = pList.get(j);
    if (pi.ap.type != pj.ap.type)
    {
     continue;
    }
    boolean vertical = false;
    boolean oblique = false;

    Pt pti = pi.pt;
    Pt ptj = pj.pt;

    int padSize;
    if (pti.x == ptj.x)
    {
     if (!scanVertical)
     {
      continue;
     }
     vertical = true;
     dist = ptj.y - pti.y;
     padSize = (int) ((pi.ap.iVal2 + pj.ap.iVal2) / 2);
    }
    else if (pti.y == ptj.y)
    {
     if (scanVertical)
     {
      continue;
     }
     dist = ptj.x - pti.x;
     padSize = (int) ((pi.ap.iVal1 + pj.ap.iVal1) / 2);
    }
    else
    {
     oblique = true;
     dist = (int) Math.hypot(pti.x - ptj.x,pti.y - ptj.y);
     padSize = (int) ((pi.ap.iVal1 + pj.ap.iVal1) / 2);
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

    int gap = dist - padSize;
    if (gap <= 0)
    {
     if (p0.ap.type == ApertureList.Aperture.ROUND)
     {
      System.err.printf("overlapping pads %d %d\n",p0.index,p1.index);
      System.exit(1);
     }
     continue;
    }

    if (oblique)
    {
     continue;
    }

    if (dbgFlag)
    {
     dbg.printf("pad %3d %3d dist %6d\n",p0.index,p1.index,dist);
    }

    if ((dist >= minDist) &&
	(((dist <= maxDistRound) &&
	  (p0.ap.type == ApertureList.Aperture.ROUND)) ||
	 ((dist <= maxDistSquare) &&
	  (p0.ap.type == ApertureList.Aperture.SQUARE))))
    {
     int x;
     int y;
     if (vertical)
     {
      if (dbgFlag)
      {
       dbg.printf("pad v %3d %3d x %6d yi %6d yj %6d " +
		  "dist %6d %3d %5.3f %s %5.3f %s\n",
		  p0.index,p1.index,p0.pt.x,p0.pt.y,p1.pt.y,dist,gap,
		  p0.ap.val1,p0.ap.typeStr,p1.ap.val1,p1.ap.typeStr);
      }
      x = p0.pt.x;
      int y0 = p0.pt.y +  p0.ap.iVal2 / 2;
      int y1 = p1.pt.y -  p1.ap.iVal2 / 2;
      y = ((y1 - y0) / 2) + y0;
     }
     else
     {
      if (dbgFlag)
      {
       dbg.printf("pad h %3d %3d y %6d xi %6d xj " +
		  "%6d dist %6d %3d %5.3f %s %5.3f %s\n",
		  p0.index,p1.index,p0.pt.y,p0.pt.x,p1.pt.x,dist,gap,
		  p0.ap.val1,p0.ap.typeStr,p1.ap.val1,p1.ap.typeStr);
      }
      int x0 = p0.pt.x + p0.ap.iVal1 / 2;
      int x1 = p1.pt.x - p1.ap.iVal1 / 2;
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
	 w = p0.ap.iVal1;
	 h = dist - (p0.ap.iVal2 + (int) (.010 * GDraw.SCALE));
	}
	else
	{
	 w = dist - (p0.ap.iVal1 + (int) (.010 * GDraw.SCALE));
	 h = p0.ap.iVal2;
	}
	w = (int) (w / scale);
	h = (int) (h / scale);
	if (dbgFlag)
	{
	 dbg.printf("rec %3d x %5d y %5d w %3d h %3d\n",p0.index,x,y,w,h);
	}
	if (checkRect(x - w / 2,y - h / 2,w,h))
	{
	 g.fillRect(x - w / 2,y - h / 2,w,h);
	}
       }
      }
     }
     else if (p0.ap.type == ApertureList.Aperture.ROUND)
     {
      if (dbgFlag)
      {
       dbg.printf("c x %6d y %6d vertical %s\n",x,y,vertical);
      }
      int minAperture = p0.ap.iVal1;
      if (p1.ap.iVal1 < minAperture)
      {
       minAperture = p1.ap.iVal1;
      }
      int r = (int) (minAperture  / (2 * scale));
      int limit = (int) (dist / (2 * scale));
      if (vertical)
      {
       padVertLine(x - r,y,limit);
       padVertLine(x + r,y,limit);
      }
      else
      {
       padHorizLine(x,y - r,limit);
       padHorizLine(x,y + r,limit);
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
    if (data[i0] == CUT)
    {
     return;
    }
    break;
   }
  }

  if (max != 0)
  {
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
   g.drawLine(x,y - (min - 3),x,y + (max - 3));
  }
  else
  {
   int min;
   int start = 0;
   int end;
   boolean onBackground = false;
   for (min = -limit; min < limit; min++)
   {
    if (dbgFlag)
    {
     dbg.printf("%3d flag %s\n",min,onBackground);
    }
    int i0 = x + (y + min) * w0;
    if (!onBackground)
    {
     if ((data[i0] == BACKGROUND)
     &&  (data[i0 - 1] == BACKGROUND)
     &&  (data[i0 + 1] == BACKGROUND))
     {
      onBackground = true;
      start = min;
     }
    }
    else
    {
     if ((data[i0] != BACKGROUND)
     ||  (data[i0 - 1] != BACKGROUND)
     ||  (data[i0 + 1] != BACKGROUND))
     {
      onBackground = false;
      end = min;
      if ((end - start) > 6)
      {
       g.drawLine(x,y + (start + 3),x,y + (end - 3));
      }
     }
    }
   }
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

  if (max != 0)
  {
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
   g.drawLine(x - (min - 3),y,x + (max - 3),y);
  }
  else
  {
   int index;
   int start = 0;
   boolean onBackground = false;
   for (index = -limit; index < limit; index++)
   {
//    if (dbgFlag)
//    {
//     dbg.printf("index %3d x %6d flag %s\n",index,x + index,onBackground);
//    }
    int i0 = (x + index) + y * w0;
    if (!onBackground)
    {
     if ((data[i0] == BACKGROUND)
     &&  (data[i0 - w0] == BACKGROUND)
     &&  (data[i0 + w0] == BACKGROUND))
     {
      onBackground = true;
      start = index;
     }
    }
    else
    {
     if ((data[i0] != BACKGROUND)
     ||  (data[i0 - w0] != BACKGROUND)
     ||  (data[i0 + w0] != BACKGROUND))
     {
      onBackground = false;
      if ((index - start) > 6)
      {
       g.drawLine(x + (start + 3),y,x + (index - 3),y);
      }
     }
    }
   }
  }
 }

 public void adjacentFix()
 {
  getData();
  for (int i = 0; i < data.length; i++)
  {
   if (data[i] == CUT)
   {
    data[i] = TRACK;
   }
  }
  setData();
 }

 public void padTrack()
 {
  PadList.PadDist padDist = null;

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
     if (dbgFlag)
     {
      dbg.printf("pad %3d trk %3d v\n",pad.index,trk.index);
     }
     vertical = true;
     dir= "v";
     if (p0.y < p1.y)
     {
      flag = ((pt.y > p0.y) &&  (pt.y < p1.y));
     }
     else
     {
      flag = ((pt.y > p1.y) &&  (pt.y < p0.y));
     }
     if (flag)
     {
      dist = (int) ((pt.x - p0.x) / scale);
     }
    }
    else if (p0.y == p1.y)	/* if horizontal */
    {
     if (dbgFlag)
     {
      dbg.printf("pad %3d trk %3d h\n",pad.index,trk.index);
     }
     dir = "h";
     if (p0.x < p1.x)
     {
      flag = ((pt.x > p0.x) &&  (pt.x < p1.x));
     }
     else
     {
      flag = ((pt.x > p1.x) &&  (pt.x < p0.x));
     }
     if (flag)
     {
      dist = (int) ((pt.y - p0.y) / scale);
     }
    }
    else
    {
     if (dbgFlag)
     {
      dbg.printf("pad %3d trk %3d o\n",pad.index,trk.index);
     }
     dir = "o";
     oblique = true;
     if (pad.ap.type == ApertureList.Aperture.ROUND)
     {
      padDist = pad.lineDistance(trk.pt[0],trk.pt[1]);
      dist = padDist.dist;
      flag = dist > 0;
      if (flag)
      {
       dist = (int) (dist / scale);
      }
     }
    }

    if (dbgFlag)
    {
     dbg.printf("pad %3d trk %3d dist %6d\n",pad.index,trk.index,dist);
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
      if (dbgFlag)
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

       dbg.printf("pad %3d %s x %6d y %6d trk %3d (%6d,%6d) (%6d,%6d) %s " +
		  "dist %6d\n",
		  pad.index,ap,pt.x,pt.y,
		  trk.index,p0.x,p0.y,p1.x,p1.y,dir,dist);
      }

      if (oblique)
      {
       if (padDist != null)
       {
	padTrackOblique(pad,trk,padDist);
       }
      }
      else
      {
       int trkW = (int) (trk.ap.iVal1 / (2 * scale));
       int limit = Math.abs(dist) - trkW;
       int x = (int) (pt.x / scale);
       int y = (int) (pt.y / scale);
       int trkDir = -1;
       if (dist < 0)
       {
	trkDir = 1;
       }
       if (pad.ap.type == ApertureList.Aperture.ROUND)
       {
	int r = (int) (pad.ap.iVal1 / (2 * scale));
	if (vertical)
	{
	 padTrackVert(x,y - r,trkDir,limit);
	 padTrackVert(x,y + r,trkDir,limit);
	}
	else
	{
	 padTrackHoriz(x - r,y,trkDir,limit);
	 padTrackHoriz(x + r,y,trkDir,limit);
	}
       }
       else if (pad.ap.type == ApertureList.Aperture.SQUARE)
       {
	int w = (int) (pad.ap.iVal1 / (2 * scale));
	int h = (int) (pad.ap.iVal2 / (2 * scale));
	if (dbgFlag)
	{
	 dbg.printf("x %6d %5d y %6d %5d w %4d h %4d dist %4d trkw %4d " +
		    "limit %4d\n",
		    pad.pt.x,x,pad.pt.y,y,w,h,dist,trkW,limit);
	}
	if (vertical)
	{
	 limit -= w;
	 w *= trkDir;
	 padTrackVert(x + w,y + h,trkDir,limit);
	 padTrackVert(x + w,y - h,trkDir,limit);
	}
	else
	{
	 limit -= h;
	 h *= trkDir;
	 padTrackHoriz(x + w,y + h,trkDir,limit);
	 padTrackHoriz(x - w,y + h,trkDir,limit);
	}
       }
      }
     }
    }
    if (dbgFlag)
    {
     dbg.printf("\n");
    }
   }
  }
 }

 public void padTrackVert(int x, int y, int dir, int limit)
 {
  if (dbgFlag)
  {
   dbg.printf("x %6d y %6d dir %3d limit %3d\n",x,y,dir,limit);
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

 public void padTrackHoriz(int x, int y, int dir, int limit)
 {
  if (dbgFlag)
  {
   dbg.printf("x %6d y %6d dir %2d limit %3d\n",x,y,dir,limit);
  }
  int start;
  for (start = 0; start < limit; start++)
  {
   int i0 = x + (y + (start * dir)) * w0;
   if ((data[i0] == BACKGROUND)
   &&  (data[i0 - 1] == BACKGROUND)
   &&  (data[i0 + 1] == BACKGROUND))
   {
    break;
   }
  }
  start += 3;

  int end;
  for (end = start; end < limit; end++)
  {
   int i0 = x + (y + (end * dir)) * w0;
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

 public void padTrackOblique(PadList.Pad pad, TrackList.Track trk,
			     PadList.PadDist padDist)
 {
  Pt p0 = pad.pt;
  Pt pt0 = trk.pt[0].copy();
  Pt pt1 = trk.pt[1].copy();
  double m = (((double) (pt0.y - pt1.y)) / ((double) (pt0.x - pt1.x)));
  double b = pt0.y - m * pt0.x;
  double r0 = (pad.ap.iVal1 / 2);
  if (dbgFlag)
  {
   dbg.printf("oblique\n");
   dbg.printf("xt0 %6d yt0 %6d xt1 %6d yt1 %6d r0 %6.0f m %5.2f b %6.0f\n",
	      pt0.x,pt0.y,pt1.x,pt1.y,r0,m,b);
  }
  double mSqr = m * m;
  double rSqr = r0 * r0;
  int x = (int) Math.sqrt(rSqr / (1 + mSqr));
  int y = (int) Math.sqrt(rSqr - x * x);
  if (m < 0)
  {
   x = -x;
  }
  int x0 = (int) ((p0.x + x) / scale);
  int y0 = (int) ((p0.y + y) / scale);
  int x1 = (int) ((p0.x - x) / scale);
  int y1 = (int) ((p0.y - y) / scale);
  if (dbgFlag)
  {
   dbg.printf("x %6d y %6d x0 %6d y0 %6d x1 %6d y1 %6d\n",
	      x,y,x0,y0,x1,y1);
  }
  int xDir = 1;
  int yDir = 1;
  if (p0.x > padDist.x)
  {
   xDir = -1;
  }
  double m0 = -1.0 / m;
  int limit = 50;
  obliqueLine(x0,y0,xDir,m0,limit);
  obliqueLine(x1,y1,xDir,m0,limit);
 }

 public void obliqueLine(int x0, int y0, int xDir, double m0, int limit)
 {
  double b = y0 - m0 * x0;
  int index;
  int xStart = 0;
  int yStart = 0;
  for (index = 0; index < limit; index++)
  {
   xStart = x0 + index * xDir;
   yStart = (int) (m0 * xStart + b);
   int i0 = xStart + yStart * w0;
   if ((i0 < 0) || (i0 >= imageSize))
   {
    break;
   }
   if ((data[i0]          == BACKGROUND)
   &&  (data[i0 + 1]      == BACKGROUND)
   &&  (data[i0 - 1]      == BACKGROUND)
   &&  (data[i0 + w0]     == BACKGROUND)
   &&  (data[i0 - w0]     == BACKGROUND)
   &&  (data[i0 + w0 + 1] == BACKGROUND)
   &&  (data[i0 + w0 - 1] == BACKGROUND)
   &&  (data[i0 - w0 + 1] == BACKGROUND)
   &&  (data[i0 - w0 - 1] == BACKGROUND))
   {
    break;
   }
  }
  int xEnd;
  int yEnd;
  for (; index < limit; index++)
  {
   xEnd = x0 + index * xDir;
   yEnd = (int) (m0 * xEnd + b);
   int i0 = xEnd + yEnd * w0;
   if ((i0 < 0) || (i0 >= imageSize))
   {
    break;
   }
   if ((data[i0] != BACKGROUND))
   {
    if (data[i0] == Circle.EDGE)
    {
     return;
    }
    break;
   }
  }
  xEnd = x0 + (index - 3) * xDir;
  yEnd = (int) (m0 * xEnd + b);
  g.drawLine(xStart,yStart,xEnd,yEnd);
 }

 public void process(boolean bmp) throws Exception
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

  if (bmp)
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

  if (bmp)
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
   Seg[] segArray = new Seg[segList.size()];
   segList.toArray(segArray);

   for (int i = 0; i < segArray.length - 2; i++)
   {
    Seg s0 = segArray[i];
    double minDist = 99999;
    int index = 0;
    int next = i + 1;
    for (int j = next; j < segArray.length; j++)
    {
     dist = s0.dist(segArray[j]);
     if (dist < minDist)
     {
      minDist = dist;
      index = j;
     }
    }
    Seg tmp = segArray[next];
    segArray[next] = segArray[index];
    segArray[index] = tmp;
   }

   if (GDraw.CSV)
   {
    gdraw.csv.printf("\"Name\",\"X\",\"Y\"\n");
    gdraw.csv.printf("\"\",0,0\n");
   }

   double rapidDist = 0.0;
   double millDist = 0.0;
   if (dbgFlag)
   {
    dbg.printf("\noutput NC path\n\n");
   }
   for (int i = 1; i < segArray.length; i++)
   {
    seg = segArray[i];
    millDist += seg.dist;
    float x = (float) seg.x;
    float y = (float) seg.y;
    if (GDraw.CSV)
    {
     gdraw.csv.printf("\"%d\",%d,%d\n",
		      i,(int) (x * ncScale),(int) (y * ncScale));
    }
    if (dbgFlag)
    {
     dbg.printf("\nseg %4d len %3d x %6.3f y %6.3f curX %6.3f curY %6.3f dist %7.3f\n\n",
		seg.num,seg.size(),seg.x,seg.y,seg.curX,seg.curY,seg.dist);
     for (String seg1 : seg)
     {
      dbg.printf("%s", seg1);
     }
    }
    rapidDist += Math.hypot(x - xCur,y - yCur);
    Line2D shape = new Line2D.Float((float) (xCur * ncScale),
				    (float) (yCur * ncScale),
				    (float) (x * ncScale),
				    (float) (y * ncScale));
//    g.draw(shape);
    for (String gCode : seg)
    {
     out.printf("%s",gCode);
    }
    if (seg.closed)
    {
     xCur = x;
     yCur = y;
    }
    else
    {
     xCur = (float) seg.curX;
     yCur = (float) seg.curY;
    }
   }
   System.out.printf("rapidDist %6.3f millDist %7.3f\n",rapidDist,millDist);
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
     seg.add(x0,y0,String.format("g0 x%5.3f y%5.3f (s %3d)\n",x0,y0,segNum));
     seg.add(String.format("g1 z[#1] f[#5]\n"));
     seg.addCircle(x0,y0,radius,
		   String.format("g3 x%5.3f y%5.3f i%5.3f j%5.3f f[#6] " +
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
   seg.add(x0,y0,String.format("g0 x%5.3f y%5.3f (s %3d)\n",
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

     double x0 = x / ncScale;	/* path start */
     double y0 = y / ncScale;
     lastX = x;
     lastY = y;
     seg.add(x0,y0,String.format("g1 x%5.3f y%5.3f f[#5] (s %3d l %4d %4d)\n",
				 x0,y0,segNum,leg,len));
     
     Circle.ArcEnd arcEnd;
     arcEnd = pad.ap.c.markArc(this,pad.pt,x,y); /* mark path and find end */
     i0 = arcEnd.i0;
     if (i0 != i0Pad)		/* if on a track */
     {
      int ix1 = i0 % w0;	/* integer path end location */
      int iy1 = i0 / w0;
      double x1 = ix1 / ncScale; /* double path end location */
      double y1 = iy1 / ncScale;
      double dist = Math.hypot(x1 - x0,y1 - y0);
      if (dist <= .003)		/* if distance short line instead of arc */
      {
       if (dbgFlag)
       {
	dbg.printf("pad %3d x %5d y %5d x %5d y %5d dist %5.3f\n",
		   pad.index,x,y,ix1,iy1,dist);
	dbg.flush();
       }
       seg.add(x0,y0,String.format("g1 x%5.3f y%5.3f f[#5] (s %3d p %3d)\n",
				   x1,y1,segNum,pad.index));
      }
      else			/* if arc required */
      {
       double cx = (pad.pt.x / scale) / ncScale; /* calculate center */
       double cy = (pad.pt.y / scale) / ncScale;

       double i = cx - x0;
       double j = cy - y0;
       double r0 = Math.hypot(i,j);		/* radius at start */
       double r1 = Math.hypot(cx - x1,cy - y1); /* radius at end */
       double err = Math.abs(r0 - r1);

       double endX = r0 * Math.cos(arcEnd.end) + cx;
       double endY = r0 * Math.sin(arcEnd.end) + cy;

       double rerr = Math.hypot(endX - x1,endY - y1);
       if (dbgFlag)
       {
	dbg.printf("r0 %8.6f r1 %8.6f diff %8.6f\n",r0,r1,err);
	dbg.printf("x1 %8.6f y1 %8.6f endX %8.6f endY %8.6f rerr %8.6f\n",
		   x1,y1,endX,endY,rerr);
       }
       seg.addArc(endX,endY,i,j,
		  String.format("g3 x%5.3f y%5.3f i%5.3f j%5.3f f[#6] " +
				"(s %3d p %3d)\n",
				endX,endY,i,j,segNum,pad.index));

       if (err > .0005)
       {
	if (dbgFlag)
	{
	 dbg.printf("add segment\n");
	}
	seg.add(x1,y1,String.format("g1 x%5.5f y%5.5f f[#5]()\n",x1,y1));
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
     double tx = x / ncScale;
     double ty = y / ncScale;
     seg.add(tx,ty,String.format("g1 x%5.3f y%5.3f f[#5] (s %3d l %4d %4d)\n",
				 tx,ty,segNum,leg,len));
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

//  for (int i = 1; i < 3; i++)
//  {
//   data[startIndex - i * w0] = START;
//  }

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

  if (output)
  {
   double tx = x / ncScale;
   double ty = y / ncScale;
   seg.add(tx,ty,String.format("g1 x%5.3f y%5.3f f[#5] (s %3d l %4d)\n",
			       tx,ty,segNum,leg));
   seg.add(String.format("g0 z[#2]\n"));
   if (tmp == 0)
   {
    seg.closed = true;
   }

   if (dbgFlag)
   {
    dbg.printf("strX %6.3f strY %6.3f curX %6.3f curY %6.3f dist %6.3f\n",
	       seg.x,seg.y,seg.curX,seg.curY,seg.dist);
    dbg.printf("%s\n",tmp == 0 ? "closed" : "open");
   }

   if (tmp != 0)
   {
    if (seg.dist < .005)
    {
     data[startIndex] = STRERR;
     data[i0] = TRKERR;
     if (dbgFlag)
     {
      dbg.printf("track error seg %d\n",segNum);
//      System.err.printf("track error\n");
     }
     trackError = true;
     remove = true;
    }
   }
  }

  if (remove)
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
  boolean closed;
  double x;
  double y;
  double curX;
  double curY;
  double dist;

  public Seg()
  {
   super();
   num = -1;
   init();
  }

  public Seg(int num)
  {
   super();
   this.num = num;
   init();
  }

  private void init()
  {
   closed = false;
   x = 0.0;
   y = 0.0;
   curX = 0.0;
   curY = 0.0;
   dist = 0.0;
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

  public void add(double x, double y, String str)
  {
   double d = Math.hypot(x - curX,y - curY);
   dist += d;
   if (dbgFlag)
   {
    dbg.printf("%s",str);
    dbg.printf("curX %6.3f curY %6.3f x %6.3f y %6.3f dist %6.3f\n\n",
	       curX,curY,x,y,d);
   }
   curX = x;
   curY = y;
   add(str);
  }

  public void addCircle(double x, double y, double r, String str)
  {
   double d = 2.0 * Math.PI * r;
   dist += d;
   if (dbgFlag)
   {
    dbg.printf("%s",str);
    dbg.printf("curX %6.3f curY %6.3f r %5.3f dist %6.3f\n\n",
	       x,y,r,d);
   }
   add(str);
  }

  public void addArc(double x, double y, double i, double j, String str)
  {
   double cx = curX + i;
   double cy = curY + j;
   double x0 = -i;
   double y0 = -j;
   double x1 = x - cx;
   double y1 = y - cy;
   double r = Math.hypot(x0,y0);
   double theta0 = Math.atan2(y0,x0);
   if (theta0 < 0)
   {
    theta0 += 2 * Math.PI;
   }
   double theta1 = Math.atan2(y1,x1);
   if (theta1 < 0)
   {
    theta1 += 2 * Math.PI;
   }
   double delta = theta1 - theta0;
   if (delta < 0)
   {
    delta += 2 * Math.PI;
   }
   if (dbgFlag)
   {
    dbg.printf("%s",str);
    dbg.printf("curX %6.3f curY %6.3f cx %6.3f cy %6.3f\n",
	       curX,curY,cx,cy);
    dbg.printf("x0 %6.3f y0 %6.3f x1 %6.3f y1 %6.3f r %5.3f " +
	       "theta0 %4.0f theta1 %4.0f delta %4.0f\n\n",
	       x0,y0,x1,y1,r,Math.toDegrees(theta0),Math.toDegrees(theta1),
	       Math.toDegrees(delta));
   }
   dist += r * delta;
   add(str);
  }

  public void setLoc(double xLoc, double yLoc)
  {
   x = xLoc;
   y = yLoc;
   curX = xLoc;
   curY = yLoc;
  }

  public double dist(Seg s)
  {
   return(Math.hypot(s.x - curX,s.y - curY));
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
