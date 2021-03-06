/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gdraw;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;

import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

import java.awt.image.BufferedImage;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.Collections;

import javax.imageio.ImageIO;

import gdraw.Util.D;
import gdraw.Util.Pt;

//import org.jgap.*;
//import org.jgap.impl.*;
//import org.jgap.impl.salesman.*;

import Dxf.Dxf;

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
 int dataSize;
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

 double depth;
 double retract;
 double linearFeed;
 double circularFeed;

 boolean variables;
 boolean metric;
 String g0Fmt;
 String g1Fmt;
 String g1Fmta;
 String g3Fmt;
 String g3FmtIJ;
 String depthFmt;

 boolean dxf;
 Dxf d1;

 boolean probeFlag = false;
 Probe probe;

 int maxDistSquare;
 int maxDistRound;
 int minDist;
 int minOffsetDist;
 int roundSearchLimit;
 int roundTrackLimit;
 int rectSearchLimit;
 int rectTrackLimit;
  
 int minGap = 29;

 boolean mirror;
 int xSize;
 int ySize;

 public static final int BACKGROUND = new Color(0xe0, 0xff, 0xff).getRGB();
 public static final int GRID       = new Color(0xe0, 0x00, 0xff).getRGB();
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
  image = new BufferedImage(w0, h0, BufferedImage.TYPE_INT_RGB);
  g = image.createGraphics();
  g.setColor(new Color(BACKGROUND));
  g.fillRect(0, 0, w0, h0);
//  g.setColor(Color.BLACK);
  g.setColor(new Color(TRACK));
  data = new int[w0 * h0];
  dataSize = w0 * h0;
  if (false)
  {
   System.out.printf("background %08x\n", BACKGROUND);
   System.out.printf("grid       %08x\n", GRID);
   System.out.printf("edge       %08x\n", EDGE);
   System.out.printf("e          %08x\n", E);
   System.out.printf("track      %08x\n", TRACK);
   System.out.printf("pad        %08x\n", PAD);
   System.out.printf("cut        %08x\n", CUT);
   System.out.printf("path       %08x\n", PATH);
   System.out.printf("nopath     %08x\n", NOPATH);
   System.out.printf("short      %08x\n", SHORT);
   System.out.printf("start      %08x\n", START);
   System.out.printf("Circle.edge %08x\n", Circle.EDGE);
   System.out.printf("Circle.body %08x\n", Circle.BODY);

  }
  maxDistSquare = (int) (GDraw.SCALE * .175);
  maxDistRound = (int) (GDraw.SCALE * .125);
  minDist = (int) (GDraw.SCALE * .075);
  minOffsetDist = (int) (GDraw.SCALE * 0.012);
  roundSearchLimit = (int) ((0.100 * GDraw.SCALE) / scale);
  roundTrackLimit =  (int) ((0.050 * GDraw.SCALE) / scale);
  rectSearchLimit = (int) ((0.050 * GDraw.SCALE) / scale);
  rectTrackLimit =  (int) ((0.075 * GDraw.SCALE) / scale);
 }

 public void setMirror(boolean mirror, int xSize, int ySize)
 {
  this.mirror = mirror;
  this.xSize = xSize;
  this.ySize = ySize;
 }

 public void setVariables(boolean val)
 {
  this.variables = val;
 }

 public void setMetric(boolean val)
 {
  this.metric = val;
 }

 public void setDepth(double val)
 {
  this.depth = val;
 }

 public void setRetract(double val)
 {
  this.retract = val;
 }

 public void setLinear(double val)
 {
  this.linearFeed = val;
 }

 public void setCircular(double val)
 {
  this.circularFeed = val;
 }

 public void setProbe(boolean probeFlag, Probe p)
 {
  this.probeFlag = probeFlag;
  probe = p;
 }

 public void setDxf(boolean dxf, Dxf d1)
 {
  this.dxf = dxf;
  this.d1 = d1;
 }

 public void getData()
 {
  image.getRGB(0, 0, w0, h0, data, 0, w0);
 }

 public void setData()
 {
  image.setRGB(0, 0, w0, h0, data, 0, w0);
 }

 public void circle(int x, int y, int r)
 {
  Circle c = new Circle(dbg, r);
  c.fill(data, w0, x, y);
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
    g.setColor(new Color(PAD));
    int w = (int) (ap.iVal1 / (2 * scale));
    int x = (int) (pt.x / scale) - w;
    int y = (int) (pt.y / scale) - w;
    if (dbgFlag)
    {
     dbg.printf("%3d x %6d y %6d x %6d y %6d w %3d\n",
		pad.index, (int) (pt.x / scale), (int) (pt.y / scale),
		x, y, 2 * w);
     dbg.flush();
    }
    if (true)
    {
     ap.draw(this, pt);
    }
    else
    {
     g.fillOval(x, y, w, w);
    }
   }
   else if (ap.type == ApertureList.Aperture.SQUARE)
   {
    g.setColor(new Color(TRACK));
    int w = (int) (ap.iVal1 / (2 * scale));
    int h = (int) (ap.iVal2 / (2 * scale));
    int x = (int) (pt.x / scale) - w;
    int y = (int) (pt.y / scale) - h;
    w *= 2;
    h *= 2;
    if (dbgFlag)
    {
     dbg.printf("%3d x %6d y %6d x0 %6d y0 %6d x1 %6d y1 %6d w %3d h %3d\n",
		pad.index, (int) (pt.x / scale), (int) (pt.y / scale),
		x, y, x + w, y + h, w, h);
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
     g.fillRect(x, y, w, h);
    }
   }
  }
 }

 public void drawOval(PadList padList)
 {
  for (PadList.Pad pad : padList)
  {
   ApertureList.Aperture ap = pad.ap;
   Pt pt = pad.pt;
   if (ap != null)
   {
    if (ap.type == ApertureList.Aperture.OVAL)
    {
     g.setColor(new Color(TRACK));
     int w = (int) (ap.iVal1 / scale);
     int h = (int) (ap.iVal2 / scale);
     int x = (int) (pt.x / scale);
     int y = (int) (pt.y / scale);
     if (dbgFlag)
     {
      dbg.printf("%3d x %6d y %6d x0 %6d y0 %6d x1 %6d y1 %6d w %3d h %3d\n",
		 pad.index, (int) (pt.x / scale), (int) (pt.y / scale),
		 x, y, x + w, y + h, w, h);
      dbg.flush();
     }
     float wx0 = (float) ((ap.iVal1 < ap.iVal2) ? (float) (ap.iVal1 / scale) :
			  (ap.iVal2 / scale));
     BasicStroke stroke = new BasicStroke(wx0, BasicStroke.CAP_ROUND,
					  BasicStroke.JOIN_MITER);
     g.setStroke(stroke);
     Line2D s;
     h /= 2;
     w /= 2;
     if (h > w)
     {
      s = new Line2D.Float((float) (x), (float) (y - h + w),
			   (float) (x), (float) (y + h - w));
     }
     else
     {
      s = new Line2D.Float((float) (x - w + h), (float) (y),
			   (float) (x + w - h), (float) (y));
     }
     g.draw(s);
    }
    if (dbgFlag)
    {
     dbg.flush();
    }
   }
  }
 }
 
/*
 public void draw(TrackList.Track track)
 {
  g.setColor(new Color(Track));
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

 public void fillPolygon(Polygon p)
 {
  g.setColor(new Color(TRACK));
//  BasicStroke stroke = new BasicStroke(1.0f, BasicStroke.CAP_ROUND,
//				       BasicStroke.JOIN_MITER);
//  g.setStroke(stroke);
//  g.drawPolygon(p);
  g.fillPolygon(p);
 }

 public void drawTracks()
 {
  if (dbgFlag)
  {
   dbg.printf("\nDraw Tracks\n\n");
  }
  g.setColor(new Color(TRACK));
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
      dbg.printf("trk %3d %3d w %3.0f\n", t0.index, t1.index, width);
     }
//     dr(width, t0.pt[1], t0.pt[0], t1.pt[1]);
    }
    else if (t0.pt[0].equals(t1.pt[1]))
    {
     if (dbgFlag)
     {
      dbg.printf("trk %3d %3d w %3.0f\n", t0.index, t1.index, width);
     }
//     dr(width, t0.pt[1], t0.pt[0], t1.pt[0]);
    }
    else if (t0.pt[1].equals(t1.pt[0]))
    {
     if (dbgFlag)
     {
      dbg.printf("trk %3d %3d w %3.0f\n", t0.index, t1.index, width);
     }
//     dr(width, t0.pt[0], t0.pt[1], t1.pt[1]);
    }
    else if (t0.pt[1].equals(t1.pt[1]))
    {
     if (dbgFlag)
     {
      dbg.printf("trk %3d %3d w %3.0f\n", t0.index, t1.index, width);
     }
//     dr(width, t0.pt[0], t0.pt[1], t1.pt[0]);
    }
   }

   if (wx0 <= 20.0)
   {
    wx0 = (float) 1.0;
   }

   if (dbgFlag)
   {
    dbg.printf("trk %3d x0 %5d y0 %5d x1 %5d y1 %5d wx0 %3.0f\n", t0.index,
	       t0.pt[0].x, t0.pt[0].y,
	       t0.pt[1].x, t0.pt[1].y, wx0);
   }
   BasicStroke stroke = new BasicStroke(wx0, BasicStroke.CAP_ROUND,
					BasicStroke.JOIN_MITER);
   /* BasicStroke stroke = new BasicStroke(wx0, BasicStroke.CAP_BUTT, */
   /* 					BasicStroke.JOIN_BEVEL); */
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
   dbg.printf("x %5d y %5d\n", p1.x, p1.y);
  }
  for (int i = 0; i < pList.size() - 1; i++)
  {
   PadList.Pad pi = pList.get(i);
   if (pi.connected(p1))
   {
    if (dbgFlag)
    {
     dbg.printf("pad %3d x  %5d y  %5d\n", pi.index, p1.x, p1.y);
    }
    return;
   }
  }
  BasicStroke stroke = new BasicStroke(width, BasicStroke.CAP_ROUND,
				       BasicStroke.JOIN_ROUND);

  g.setStroke(stroke);

  Path2D p = new Path2D.Float();
  p.moveTo(p0.x / scale, p0.y / scale);
  p.lineTo(p1.x / scale, p1.y / scale);
  p.lineTo(p2.x / scale, p2.y / scale);
  g.draw(p);
 }

 public boolean checkOverlap()
 {
  PadList pList = gdraw.padList;
  for (int i = 0; i < pList.size() - 1; i++)
  {
   PadList.Pad pi = pList.get(i);
   Pt pti = pi.pt;
   for (int j = i + 1; j < pList.size(); j++)
   {
    PadList.Pad pj = pList.get(j);
    Pt ptj = pj.pt;
    int dist;
    int piSize = 0;
    int pjSize = 0;
    if (Math.abs(pti.x - ptj.x) < minOffsetDist) /* if pads vertical */
    {
     dist = Math.abs(ptj.y - pti.y);
     piSize = pi.ap.iVal2 / 2;
     pjSize = pj.ap.iVal2 / 2;
    }
    else if (Math.abs(pti.y - ptj.y) < minOffsetDist) /* if pads horizontal */
    {
     dist = Math.abs(ptj.x - pti.x);
     piSize = pi.ap.iVal1 / 2;
     pjSize = pj.ap.iVal1 / 2;
    }
    else			/* if pads oblique */
    {
     dist = (int) Math.hypot(pti.x - ptj.x, pti.y - ptj.y);
     if (pi.ap.type == ApertureList.Aperture.ROUND)
     {
      piSize = pi.ap.iVal1 / 2;
     }
     else if (pi.ap.type == ApertureList.Aperture.SQUARE)
     {
      piSize = (int) (Math.hypot(pi.ap.iVal1, pi.ap.iVal2) / 2);
     }

     if (pj.ap.type == ApertureList.Aperture.ROUND)
     {
      pjSize = pj.ap.iVal1 / 2;
     }
     else if (pj.ap.type == ApertureList.Aperture.SQUARE)
     {
      pjSize = (int) (Math.hypot(pj.ap.iVal1, pj.ap.iVal2) / 2);
     }
    }
    int gap = dist - (piSize + pjSize);
    if (gap <= minGap)
    {
     System.err.printf("overlapping pads %3d %s (%5.3f, %5.3f) " +
		       "%3d %s (%5.3f, %5.3f) d %7.4f\n",
		       pi.index, pi.ap.typeStr,
		       pti.x / GDraw.SCALE, pti.y / GDraw.SCALE,
		       pj.index, pj.ap.typeStr,
		       ptj.x / GDraw.SCALE, ptj.y / GDraw.SCALE,
		       gap / GDraw.SCALE);
     System.err.printf("dist %5d piSize %5d pjSize %5d\n",
		       dist, piSize, pjSize);
    }
   }
  }
  return(false);
 }
  
 public void orderPads(boolean scanVertical)
 {
  getData();
  if (dbgFlag)
  {
   dbg.printf("\nOrder Pads %s\n\n",
	      scanVertical ? "Vertical" : "Horizontal");
  }
  PadList p0List = new PadList(gdraw);
  for (PadList.Pad pad : gdraw.padList) /* make a copy of list */
  {
   p0List.add(pad);
  }
  p0List.setDebug(dbg);
  gdraw.padList.setCompareY(!scanVertical);
  Collections.sort(p0List);
  p0List.print();

  PadList p1List = new PadList(gdraw);
  p1List.setDebug(dbg);
  ArrayList<Integer> idxList = new ArrayList<>();
  for (int i = 0; i < p0List.size() - 1; )
  {
   PadList.Pad pi = p0List.get(i);
   idxList.clear();
   Pt pti = pi.pt;
   Pt ptPrev = pti;
   if (dbgFlag)
   {
    dbg.printf("str %3d pad %3d x %5d y %5d\n",
	       i, pi.index, pti.x, pti.y);
   }
   idxList.add(i);
   for (int j = i + 1; j < p0List.size(); j++)
   {
    if (pi.ap.type != ApertureList.Aperture.ROUND)
    {
     continue;
    }
    PadList.Pad pj = p0List.get(j);
    if (pi.ap.type == pj.ap.type)
    {
     Pt ptj = pj.pt;
     if (scanVertical)		/* if scan vertical */
     {
      if (pti.x == ptj.x)	/* if pads vertical */
      {
       int dist = ptj.y - ptPrev.y;
       if (dist > maxDistRound)
       {
	break;
       }
       if (dbgFlag)
       {
	dbg.printf("add %3d pad %3d x %5d y %5d d %5d\n",
		   j, pj.index, ptj.x, ptj.y, dist);
       }
       idxList.add(j);
       ptPrev = ptj;
      }
     }
     else			/* if scan horizontal */
     {
      if (pti.y == ptj.y)	/* if pads vertical */
      {
       int dist = ptj.x - ptPrev.x;
       if (dist > maxDistRound)
       {
	break;
       }
       if (dbgFlag)
       {
	dbg.printf("add %3d pad %3d x %5d y %5d d %5d\n",
		   j, pj.index, ptj.x, ptj.y, dist);
       }
       idxList.add(j);
       ptPrev = ptj;
      }
     }
    }
   }
   if (idxList.size() >= 3)	/* if row of pads found */
   {
    p1List.clear();		/* clear list */
    for (int k = idxList.size() - 1; k >= 0; k--) /* remove in reverse order */
    {
     int index = idxList.get(k);
     PadList.Pad pad = p0List.remove(index);
     if (dbgFlag)
     {
      dbg.printf("remove index %3d pad %3d\n", index, pad.index);
     }
     p1List.add(pad);		/* add pad to new list */
    }
    Collections.sort(p1List);	/* sort list */
    if (dbgFlag)
    {
     p1List.print();
    }
    processRow(p1List, scanVertical);
   }
   else				/* if not a row */
   {
    i++;			/* move to next one */
   }
  }
 }

 public void processRow(PadList pList, boolean scanVertical)
 {
  image.getRGB(0, 0, w0, h0, data, 0, w0);
  BasicStroke stroke = new BasicStroke(1, BasicStroke.CAP_ROUND,
				       BasicStroke.JOIN_MITER);
  g.setStroke(stroke);
  g.setColor(new Color(CUT));

  for (int i = 0; i < pList.size() - 1; i++)
  {
   PadList.Pad pi = pList.get(i);
   Pt pti = pi.pt;
   int j = i + 1;
   PadList.Pad pj = pList.get(j);
   Pt ptj = pj.pt;
   int dist = scanVertical ? Math.abs(ptj.y - pti.y) : Math.abs(ptj.x - pti.x);
   if (dist < maxDistRound)
   {
    int minAperture = pi.ap.iVal1;
    if (pj.ap.iVal1 < minAperture)
    {
     minAperture = pj.ap.iVal1;
    }
    int r = (int) (minAperture  / (2 * scale));
    int limit = (int) (dist / (2 * scale));
    if (scanVertical)
    {
     int x = (int) (pti.x / scale);
     int y = (int) ((pti.y + ptj.y) / (2 * scale));
     padVertLine(x - r, y, limit);
     padVertLine(x + r, y, limit);
    }
    else
    {
     int x = (int) ((pti.x + ptj.x) / (2 * scale));
     int y = (int) (pti.y / scale);
     padHorizLine(x, y - r, limit);
     padHorizLine(x, y + r, limit);
    }
   }
  } 
 }

 @SuppressWarnings("DeadBranch")
 public boolean adjacentPads(boolean scanVertical)
 {
  boolean overlap = false;
  
  if (dbgFlag)
  {
   dbg.printf("\nAdjacent Pads %s\n\n",
	      scanVertical ? "Vertical" : "Horizontal");
  }
  image.getRGB(0, 0, w0, h0, data, 0, w0);
  BasicStroke stroke = new BasicStroke(1, BasicStroke.CAP_ROUND,
				       BasicStroke.JOIN_MITER);
  g.setStroke(stroke);
  g.setColor(new Color(CUT));

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
    if (Math.abs(pti.x - ptj.x) < minOffsetDist) /* if pads vertical */
    {
     if (!scanVertical)
     {
      continue;
     }
     vertical = true;
     dist = ptj.y - pti.y;
     padSize = (int) ((pi.ap.iVal2 + pj.ap.iVal2) / 2);
    }
    else if (Math.abs(pti.y - ptj.y) < minOffsetDist) /* if pads horizontal */
    {
     if (scanVertical)
     {
      continue;
     }
     dist = ptj.x - pti.x;
     padSize = (int) ((pi.ap.iVal1 + pj.ap.iVal1) / 2);
    }
    else			/* if pads oblique */
    {
     oblique = true;
     dist = (int) Math.hypot(pti.x - ptj.x, pti.y - ptj.y);
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
    if (gap <= minGap)
    {
//     overlap = true;
     Pt pt0;
     Pt pt1;
     if (mirror)
     {
      pt0 = p0.pt.copy();
      pt0.mirror(xSize, ySize);
      pt1 = p1.pt.copy();
      pt1.mirror(xSize, ySize);
     }
     else
     {
      pt0 = p0.pt;
      pt1 = p1.pt;
     }
      
     String tmp = "";
     switch (p0.ap.type)
     {
      case ApertureList.Aperture.ROUND:
       tmp = "rnd";
       break;
      case ApertureList.Aperture.SQUARE:
       tmp = "sqr";
       break;
      case ApertureList.Aperture.OVAL:
       tmp = "ovl";
       break;
      default:
       break;
     }
     System.err.printf("overlapping %s pads %3d (%5.3f, %5.3f) " +
		       "%3d (%5.3f, %5.3f) d %7.4f\n", tmp,
		       p0.index, pt0.x / GDraw.SCALE, pt0.y / GDraw.SCALE,
		       p1.index, pt1.x / GDraw.SCALE, pt1.y / GDraw.SCALE,
		       gap / GDraw.SCALE);
//     continue;
    }

    if (oblique)
    {
     continue;
    }

    if (dbgFlag)
    {
     if (p0.ap.type == ApertureList.Aperture.ROUND)
     {
      dbg.printf("pad %3d %3d dist %6d gap %6d\n",
		 p0.index, p1.index, dist, gap);
     }
     else if (p0.ap.type == ApertureList.Aperture.SQUARE)
     {
      dbg.printf("pad %3d %3d dist %6d gap %6d\n",
		 p0.index, p1.index, dist, gap);
     }
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
		  "dist %6d %3d %6.4f %s %6.4f %s\n",
		  p0.index, p1.index, p0.pt.x, p0.pt.y, p1.pt.y, dist, gap,
		  p0.ap.val1, p0.ap.typeStr, p1.ap.val1, p1.ap.typeStr);
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
		  "%6d dist %6d %3d %6.4f %s %6.4f %s\n",
		  p0.index, p1.index, p0.pt.y, p0.pt.x, p1.pt.x, dist, gap,
		  p0.ap.val1, p0.ap.typeStr, p1.ap.val1, p1.ap.typeStr);
      }
      int x0 = p0.pt.x + p0.ap.iVal1 / 2;
      int x1 = p1.pt.x - p1.ap.iVal1 / 2;
      x = ((x1 - x0) / 2) + x0;
      y = p0.pt.y;
     }

     x = (int) (x / scale);
     y = (int) (y / scale);

     int w;
     int h;

     if (dbgFlag)
     {
      dbg.printf("check x %6d y %6d\n", x, y);
     }
     if (p0.ap.type == ApertureList.Aperture.SQUARE)
     {
//      if (false)
//      {
//       int i0 = x + y * w0;
//       if (data[i0] == BACKGROUND)
//       {
//	if (dbgFlag)
//	{
//	 dbg.printf("r x %6d y %6d\n", x, y);
//	}
////       if (((dist >= 1100) && (dist <= 1110))
////       ||  ((dist >= 890) && (dist <= 920)))
//	{
//	 if (vertical)
//	 {
//	  w = p0.ap.iVal1;
//	  h = dist - (p0.ap.iVal2 + (int) (.010 * GDraw.SCALE));
//	 }
//	 else
//	 {
//	  w = dist - (p0.ap.iVal1 + (int) (.010 * GDraw.SCALE));
//	  h = p0.ap.iVal2;
//	 }
//	 w = (int) (w / scale);
//	 h = (int) (h / scale);
//	 if (dbgFlag)
//	 {
//	  dbg.printf("rec %3d x %5d y %5d w %3d h %3d\n", p0.index, x, y, w, h);
//	 }
//	 if (checkRect(x - w / 2, y - h / 2, w, h))
//	 {
//	  g.fillRect(x - w / 2, y - h / 2, w, h);
//	 }
//	}
//       }
//      }
     }
     else if (p0.ap.type == ApertureList.Aperture.ROUND)
     {
      if (dbgFlag)
      {
       dbg.printf("c x %6d y %6d vertical %s\n", x, y, vertical);
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
       padVertLine(x - r, y, limit);
       padVertLine(x + r, y, limit);
      }
      else
      {
       padHorizLine(x, y - r, limit);
       padHorizLine(x, y + r, limit);
      }
     }
     if (dbgFlag)
     {
      dbg.printf("\n");
     }
    }
   }
  }
  return(overlap);
 }

 public boolean checkRect(int x0, int y0, int w, int h)
 {
  int x1 = x0 + w;
  int y1 = y0 + h;
  for (int y = y0; y <= y1; y++)
  {
   int i0 = x0 + y * w0;
   for (int x = x0; x <= x1; x++,  i0++)
   {
    if (data[i0] != BACKGROUND)
     return(false);
   }
  }
  return(true);
 }

 public void padVertLine(int x, int y, int limit)
 {
  /* x at right or left of pad and y is in the middle between two pads */
  int max;
  for (max = 0; max < limit; max++) /* while less than limit */
  {
   int i0 = x + (y + max) * w0;
   if ((data[i0] != BACKGROUND)	/* if not to background yet */
   ||  (data[i0 - 1] != BACKGROUND)
   ||  (data[i0 + 1] != BACKGROUND))
   {
    if (data[i0] == CUT)	/* if a cut mark */
    {
     return;			/* exit now */
    }
    break;			/* maximum found stop looking */
   }
  }

  if (max != 0)			/* if maximum found */
  {
   int min;
   for (min = 0; min < limit; min++) /* while less than limit */
   {
    int i0 = x + (y - min) * w0;
    if ((data[i0] != BACKGROUND) /* if not to background yet */
    ||  (data[i0 - 1] != BACKGROUND)
    ||  (data[i0 + 1] != BACKGROUND))
    {
     break;			/* if found background */
    }
   }
   g.drawLine(x, y - (min - 3), x, y + (max - 3));
  }
  else				/* if maximum not found */
  {
   int min;
   int start = 0;
   int end;
   boolean onBackground = false;
   for (min = -limit; min < limit; min++)
   {
    if (dbgFlag)
    {
     dbg.printf("%3d flag %s\n", min, onBackground);
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
       g.drawLine(x, y + (start + 3), x, y + (end - 3));
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
   g.drawLine(x - (min - 3), y, x + (max - 3), y);
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
       g.drawLine(x + (start + 3), y, x + (index - 3), y);
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

 public void scanPads()
 {
  getData();
  PadList pList = gdraw.padList;
  for (int i = 0; i < pList.size(); i++)
  {
   PadList.Pad p0 = pList.get(i);
   if (p0.ap.type == ApertureList.Aperture.ROUND)
   {
    roundPadLines(p0);
   }
   else if (p0.ap.type == ApertureList.Aperture.SQUARE)
   {
    rectPadLines(p0);
   }
  }
 }

 public void roundPadLines(PadList.Pad p0)
 {
  int limit = roundSearchLimit;
  int x = (int) (p0.pt.x / scale);
  int y = (int) (p0.pt.y / scale);
  int r = (int) (p0.ap.iVal1 / (2 * scale));
  int xMin = x - r;
  int xMax = x + r;
  int yMin = y - r;
  int yMax = y + r;

  if (dbgFlag)
  {
   dbg.printf("pad %3d x %5d y %5d r %5d " +
	      "xMin %5d xMax %5d yMin %5d YMax %5d\n",
	      p0.index, x, y, r, xMin, xMax, yMin, yMax);
  }
  roundXSearch(x, y, yMax, r, limit);
  roundXSearch(x, y, yMin, r, limit);
  roundYSearch(x, y, xMax, r, limit);
  roundYSearch(x, y, xMin, r, limit);
 }

 public int checkXFound(int i0)
 {
  int found = data[i0];
  do
  {
   if (found != BACKGROUND)
   {
    break;
   }
   found = data[i0 - w0];
   if (found != BACKGROUND)
   {
    break;
   }
   found = data[i0 + w0];
   if (found != BACKGROUND)
   {
    break;
   }
   found = BACKGROUND;
  } while(false);
  if (dbgFlag)
  {
   dbg.printf("checkXFound %08x\n", found);
  }
  return(found);
 }

 public void roundXSearch(int x, int y, int yVal, int r, int limit)
 {
  /* search to right */
  
  int start;
  int end;
  boolean found = false;
  int foundType = BACKGROUND;
  for (start = 0; start < limit; start++) /* find background */
  {
   int i0 = (x + start) + yVal * w0;
   if (i0 >= dataSize)
   {
    if (dbgFlag)
    {
     dbg.printf("index out of range r i0 %8d x %5d yVal %5d start %d\n",
		i0, x, yVal, start);
    }
    break;
   }
   if ((data[i0] == BACKGROUND)
   &&  (data[i0 - w0] == BACKGROUND)
   &&  (data[i0 + w0] == BACKGROUND))
   {
    found = true;
    break;
   }
  }

  if (found)
  {
   found = false;
   for (end = start; end < limit; end++) /* find end of background */
   {
    int i0 = (x + end) + yVal * w0;
    if ((data[i0] != BACKGROUND)
    ||  (data[i0 - w0] != BACKGROUND)
    ||  (data[i0 + w0] != BACKGROUND))
    {
     found = true;
     foundType = checkXFound(i0);
     break;
    }
   }
   if (found)
   {
    if (dbgFlag)
    {
     dbg.printf("*r* x %5d y %5d yVal %5d r %3d start %5d end %5d\n",
		x, y, yVal, r, start, end);
    }
    if ((end > r)
    &&  ((end - start) > 6))
    {
     if ((foundType == Circle.EDGE)
     ||  (foundType == CUT)
     ||  (end < roundTrackLimit))
     {
      g.drawLine(x + (start + 3), yVal, x + (end - 3), yVal);
     }
    }
   }
  }

  /* search to left */
  
  foundType = BACKGROUND;
  for (start = 0; start < limit; start++) /* find background */
  {
   int i0 = (x - start) + yVal * w0;
   if (i0 >= dataSize)
   {
    if (dbgFlag)
    {
     dbg.printf("index out of range l i0 %8d x %5d yVal %5d end %d\n",
		i0, x, yVal, start);
    }
    break;
   }
   if ((data[i0] == BACKGROUND)
   &&  (data[i0 - w0] == BACKGROUND)
   &&  (data[i0 + w0] == BACKGROUND))
   {
    found = true;
    break;
   }
  }

  if (found)
  {
   found = false;
   for (end = start; end < limit; end++) /* find end of background */
   {
    int i0 = (x - end) + yVal * w0;
    if ((data[i0] != BACKGROUND)
    ||  (data[i0 - w0] != BACKGROUND)
    ||  (data[i0 + w0] != BACKGROUND))
    {
     found = true;
     foundType = checkXFound(i0);
     break;
    }
   }

   if (found)
   {
    if (dbgFlag)
    {
     dbg.printf("*l* x %5d y %5d yVal %5d r %3d start %5d end %5d\n",
		x, y, yVal, r, start, end);
    }
    if ((end > r)
    &&  ((end - start) > 6))
    {
     if ((foundType == Circle.EDGE)
     ||  (foundType == CUT)
     ||  (end < roundTrackLimit))
     {
      g.drawLine(x - (start + 3), yVal, x - (end - 3), yVal);
     }
    }
   }
  }
 }

 public int checkYFound(int i0)
 {
  int found = data[i0];
  do
  {
   if (found != BACKGROUND)
   {
    break;
   }
   found = data[i0 - 1];
   if (found != BACKGROUND)
   {
    break;
   }
   found = data[i0 + 1];
   if (found != BACKGROUND)
   {
    break;
   }
   found = BACKGROUND;
  } while(false);
  if (dbgFlag)
  {
   dbg.printf("checkXFound %08x\n", found);
  }
  return(found);
 }

 public void roundYSearch(int x, int y, int xVal, int r, int limit)
 {
  /* search up */
  
  int start;
  int end;
  boolean found = false;
  int foundType = BACKGROUND;
  for (start = 0; start < limit; start++) /* find background */
  {
   int i0 = xVal + (y + start) * w0;
   if (i0 >= dataSize)
   {
    if (dbgFlag)
    {
     dbg.printf("index out of range up r i0 %8d xVal %5d y %5d start %d\n",
		i0, xVal, y, start);
    }
    break;
   }
   if ((data[i0] == BACKGROUND)
   &&  (data[i0 - 1] == BACKGROUND)
   &&  (data[i0 + 1] == BACKGROUND))
   {
    found = true;
    break;
   }
  }

  if (found)
  {
   found = false;
   for (end = start; end < limit; end++) /* find end of background */
   {
    int i0 = xVal + (y + end) * w0;
    if (i0 >= dataSize)
    {
     if (dbgFlag)
     {
      dbg.printf("index out of range up r i0 %8d xVal %5d y %5d end %d\n",
		i0, xVal, y, end);
     }
     break;
    }
    if ((data[i0] != BACKGROUND)
    ||  (data[i0 - 1] != BACKGROUND)
    ||  (data[i0 + 1] != BACKGROUND))
    {
     found = true;
     foundType = checkYFound(i0);
     break;
    }
   }
   if (found)
   {
    if (dbgFlag)
    {
     dbg.printf("*u* x %5d y %5d xVal %5d r %3d start %5d end %5d\n",
		x, y, xVal, r, start, end);
    }
    if ((end > r)
    &&  ((end - start) > 6))
    {
     if ((foundType == Circle.EDGE)
     ||  (foundType == CUT)
     ||  (end < roundTrackLimit))
     {
      g.drawLine(xVal, y + (start + 3), xVal, y + (end - 3));
     }
    }
   }
  }

  /* search down */
  
 foundType = BACKGROUND;
 for (start = 0; start < limit; start++) /* find background */
  {
   int i0 = xVal + (y - start) * w0;
   if (i0 >= dataSize)
   {
    if (dbgFlag)
    {
     dbg.printf("index out of range down r i0 %8d xVal %5d y %5d start %d\n",
		i0, xVal, y, start);
    }
    break;
   }
   if ((data[i0] == BACKGROUND)
   &&  (data[i0 - 1] == BACKGROUND)
   &&  (data[i0 + 1] == BACKGROUND))
   {
    found = true;
    break;
   }
  }

  if (found)
  {
   found = false;
   for (end = start; end < limit; end++) /* find end of background */
   {
    int i0 = xVal + (y - end) * w0;
    if (i0 < 0)
    {
     if (dbgFlag)
     {
      dbg.printf("index out of range down r i0 %8d xVal %5d y %5d end %d\n",
		 i0, xVal, y, end);
     }
     break;
    }
    if ((data[i0] != BACKGROUND)
    ||  (data[i0 - 1] != BACKGROUND)
    ||  (data[i0 + 1] != BACKGROUND))
    {
     found = true;
     foundType = checkYFound(i0);
     break;
    }
   }

   if (found)
   {
    if (dbgFlag)
    {
     dbg.printf("*u* x %5d y %5d xVal %5d r %3d start %5d end %5d\n",
		x, y, xVal, r, start, end);
    }
    if ((end > r)
    &&  ((end - start) > 6))
    {
     if ((foundType == Circle.EDGE)
     ||  (foundType == CUT)
     ||  (end < roundTrackLimit))
     {
      g.drawLine(xVal, y - (start + 3), xVal, y - (end - 3));
     }
    }
   }
  }
 }

 public void rectPadLines(PadList.Pad p0)
 {
  int limit = rectSearchLimit;
  int x = (int) (p0.pt.x / scale);
  int y = (int) (p0.pt.y / scale);
  int w = (int) (p0.ap.iVal1 / (2 * scale));
  int h = (int) (p0.ap.iVal2 / (2 * scale));
  int xMin = x - w;
  int xMax = x + w;
  int yMin = y - h;
  int yMax = y + h;

  if (dbgFlag)
  {
   dbg.printf("pad %3d x %5d w %5d h %5d " +
	      "xMin %5d xMax %5d yMin %5d YMax %5d\n",
	      p0.index, x, w, h, xMin, xMax, yMin, yMax);
  }
  rectXSearch(x, yMax, w, limit);
  rectXSearch(x, yMin, w, limit);
  rectYSearch(xMax, y, h, limit);
  rectYSearch(xMin, y, h, limit);
 }

 public void rectXSearch(int x, int yVal, int w, int limit)
 {
  /* search to right */
  
  int start;
  int end;
  int x0;
  x0 = x + w;
  boolean found = false;
  for (start = 0; start < limit; start++) /* find background */
  {
   int i0 = (x0 + start) + yVal * w0;
   if (i0 >= dataSize)
   {
    if (dbgFlag)
    {
     dbg.printf("index out of range r i0 %8d x %5d yVal %5d start %d\n",
		i0, x0, yVal, start);
    }
    break;
   }
   if ((data[i0] == BACKGROUND)
   &&  (data[i0 - w0] == BACKGROUND)
   &&  (data[i0 + w0] == BACKGROUND))
   {
    found = true;
    break;
   }
  }

  if (found)
  {
   found = false;
   for (end = start; end < limit; end++) /* find end of background */
   {
    int i0 = (x0 + end) + yVal * w0;
    if ((data[i0] != BACKGROUND)
    ||  (data[i0 - w0] != BACKGROUND)
    ||  (data[i0 + w0] != BACKGROUND))
    {
     found = true;
     break;
    }
   }
   if (found)
   {
    if (dbgFlag)
    {
     dbg.printf("*r+ x %5d yVal %5d w %3d start %5d end %5d\n",
		x, yVal, w, start, end);
    }
    if  ((end - start) > 6)
    {
     g.drawLine(x0 + (start + 3), yVal, x0 + (end - 3), yVal);
    }
   }
  }

  /* search to left */
  
  x0 = x - w;
  for (start = 0; start < limit; start++) /* find background */
  {
   int i0 = (x0 - start) + yVal * w0;
   if (i0 >= dataSize)
   {
    if (dbgFlag)
    {
     dbg.printf("index out of range r i0 %8d x %5d yVal %5d start %d\n",
		i0, x0, yVal, start);
    }
    break;
   }
   if ((data[i0] == BACKGROUND)
   &&  (data[i0 - w0] == BACKGROUND)
   &&  (data[i0 + w0] == BACKGROUND))
   {
    found = true;
    break;
   }
  }

  if (found)
  {
   found = false;
   for (end = start; end < limit; end++) /* find end of background */
   {
    int i0 = (x0 - end) + yVal * w0;
    if ((data[i0] != BACKGROUND)
    ||  (data[i0 - w0] != BACKGROUND)
    ||  (data[i0 + w0] != BACKGROUND))
    {
     found = true;
     break;
    }
   }
   if (found)
   {
    if (dbgFlag)
    {
     dbg.printf("*l+ x %5d yVal %5d w %3d start %5d end %5d\n",
		x, yVal, w, start, end);
    }
    if  ((end - start) > 6)
    {
     g.drawLine(x0 - (start + 3), yVal, x0 - (end - 3), yVal);
    }
   }
  }
 }

 public void rectYSearch(int xVal, int y, int h, int limit)
 {
  /* search up */
  
  int start;
  int end;
  int y0;
  y0 = y + h;
  boolean found = false;
  for (start = 0; start < limit; start++) /* find background */
  {
   int i0 = xVal + (y0 + start) * w0;
   if (i0 >= dataSize)
   {
    if (dbgFlag)
    {
     dbg.printf("index out of range r i0 %8d xVal %5d y %5d start %d\n",
		i0, xVal, y0, start);
    }
    break;
   }
   if ((data[i0] == BACKGROUND)
   &&  (data[i0 - 1] == BACKGROUND)
   &&  (data[i0 + 1] == BACKGROUND))
   {
    found = true;
    break;
   }
  }

  if (found)
  {
   found = false;
   for (end = start; end < limit; end++) /* find end of background */
   {
    int i0 = xVal + (y0 + end) * w0;
    if ((data[i0] != BACKGROUND)
    ||  (data[i0 - 1] != BACKGROUND)
    ||  (data[i0 + 1] != BACKGROUND))
    {
     found = true;
     break;
    }
   }
   if (found)
   {
    if (dbgFlag)
    {
     dbg.printf("*u+ xVal %5d y %5d w %3d start %5d end %5d\n",
		xVal, y, h, start, end);
    }
    if  ((end - start) > 6)
    {
     g.drawLine(xVal, y0 + (start + 3), xVal, y0 + (end - 3));
    }
   }
  }

  /* search down */
  
  y0 = y - h;
  for (start = 0; start < limit; start++) /* find background */
  {
   int i0 = xVal + (y0 - start) * w0;
   if (i0 >= dataSize)
   {
    if (dbgFlag)
    {
     dbg.printf("index out of range r i0 %8d xVal %5d y0 %5d start %d\n",
		i0, xVal, y0, start);
    }
    break;
   }
   if ((data[i0] == BACKGROUND)
   &&  (data[i0 - 1] == BACKGROUND)
   &&  (data[i0 + 1] == BACKGROUND))
   {
    found = true;
    break;
   }
  }

  if (found)
  {
   found = false;
   for (end = start; end < limit; end++) /* find end of background */
   {
    int i0 = xVal + (y0 - end) * w0;
    if ((data[i0] != BACKGROUND)
    ||  (data[i0 - 1] != BACKGROUND)
    ||  (data[i0 + 1] != BACKGROUND))
    {
     found = true;
     break;
    }
   }
   if (found)
   {
    if (dbgFlag)
    {
     dbg.printf("*l+ xVal %5d y %5d h %3d start %5d end %5d\n",
		xVal, y, h, start, end);
    }
    if  ((end - start) > 6)
    {
     g.drawLine(xVal, y0 - (start + 3), xVal, y0 - (end - 3));
    }
   }
  }
 }

 public void padTrack()
 {
  getData();
  PadList.PadDist padDist = null;

  if (dbgFlag)
  {
   dbg.printf("\nPads near Tracks\n\n");
  }
//  g.setColor(Color.BLACK);
  g.setColor(new Color(CUT));
  BasicStroke stroke = new BasicStroke(1, BasicStroke.CAP_ROUND,
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
      dbg.printf("pad %3d trk %3d v\n", pad.index, trk.index);
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
      dbg.printf("pad %3d trk %3d h\n", pad.index, trk.index);
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
      dbg.printf("pad %3d trk %3d o\n", pad.index, trk.index);
     }
     dir = "o";
     oblique = true;
     if (pad.ap.type == ApertureList.Aperture.ROUND)
     {
      padDist = pad.lineDistance(trk.pt[0], trk.pt[1]);
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
     dbg.printf("pad %3d trk %3d dist %6d\n", pad.index, trk.index, dist);
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
		  pad.index, ap, pt.x, pt.y,
		  trk.index, p0.x, p0.y, p1.x, p1.y, dir, dist);
      }

      if (oblique)
      {
       if (padDist != null)
       {
	padTrackOblique(pad, trk, padDist);
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
	 padTrackVert(x, y - r, trkDir, limit);
	 padTrackVert(x, y + r, trkDir, limit);
	}
	else
	{
	 padTrackHoriz(x - r, y, trkDir, limit);
	 padTrackHoriz(x + r, y, trkDir, limit);
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
		    pad.pt.x, x, pad.pt.y, y, w, h, dist, trkW, limit);
	}
	if (vertical)
	{
	 limit -= w;
	 w *= trkDir;
	 padTrackVert(x + w, y + h, trkDir, limit);
	 padTrackVert(x + w, y - h, trkDir, limit);
	}
	else
	{
	 limit -= h;
	 h *= trkDir;
	 padTrackHoriz(x + w, y + h, trkDir, limit);
	 padTrackHoriz(x - w, y + h, trkDir, limit);
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
   dbg.printf("x %6d y %6d dir %3d limit %3d\n", x, y, dir, limit);
  }
  if (limit < 10)
  {
   if (dbgFlag)
   {
    dbg.printf("limit too small\n");
   }
   return;
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
    if (data[i0] == CUT)
    {
     return;
    }
    break;
   }
  }
  end -= 3;
  if (true)
  {
   g.drawLine(x + (start * dir), y, x + (end * dir), y);
  }
 }

 public void padTrackHoriz(int x, int y, int dir, int limit)
 {
  if (dbgFlag)
  {
   dbg.printf("x %6d y %6d dir %2d limit %3d\n", x, y, dir, limit);
  }
  if (limit < 10)
  {
   if (dbgFlag)
   {
    dbg.printf("limit too small\n");
   }
   return;
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
    if (data[i0] == CUT)
    {
     return;
    }
    break;
   }
  }
  end -= 3;
  if (true)
  {
   g.drawLine(x, y + start * dir, x, y + end * dir);
  }
 }

 public void padTrackOblique(PadList.Pad pad,  TrackList.Track trk,
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
	      pt0.x, pt0.y, pt1.x, pt1.y, r0, m, b);
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
	      x, y, x0, y0, x1, y1);
  }
  int xDir = 1;
  int yDir = 1;
  if (p0.x > padDist.x)
  {
   xDir = -1;
  }
  double m0 = -1.0 / m;
  int limit = 50;
  obliqueLine(x0, y0, xDir, m0, limit);
  obliqueLine(x1, y1, xDir, m0, limit);
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
     break;
    }
    xEnd = x0 + (index - 3) * xDir;
    yEnd = (int) (m0 * xEnd + b);
    g.drawLine(xStart, yStart, xEnd, yEnd);
    break;
   }
  }
 }

 public void process(boolean bmp) throws Exception
 {
  image.getRGB(0, 0, w0, h0, data, 0, w0);

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
   write(data, gdraw.baseFile + "0");
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
   write(data, gdraw.baseFile + "1");
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
     dbg.printf("\nseg %3d\n", segNum);
    }
    if (!track(data, i))
    {
     break;
    }
    segNum++;
   }
  }

  setData();
  g.setColor(new Color(CUT));
  BasicStroke stroke = new BasicStroke(1, BasicStroke.CAP_ROUND,
				       BasicStroke.JOIN_MITER);
  g.setStroke(stroke);

  float xCur = 0.0F;
  float yCur = 0.0F;

  double dist;
  if (true)
  {
   segList.add(0, new Seg());
   Seg[] segArray = new Seg[segList.size()];
   segList.toArray(segArray);

   for (int i = 0; i < segArray.length - 2; i++)
   {
    Seg s0 = segArray[i];
    double minimumDist = 99999;
    int index = 0;
    int next = i + 1;
    for (int j = next; j < segArray.length; j++)
    {
     dist = s0.dist(segArray[j]);
     if (dist < minimumDist)
     {
      minimumDist = dist;
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
     gdraw.csv.printf("\"%d\", %d, %d\n",
		      i, (int) (x * ncScale), (int) (y * ncScale));
    }
    if (dbgFlag)
    {
     dbg.printf("\nseg %4d len %3d x %6.3f y %6.3f curX %6.3f curY %6.3f " +
		"dist %7.3f\n\n",
		seg.num, seg.size(), seg.x, seg.y,
		seg.curX, seg.curY, seg.dist);
     for (Image.GSeg seg1 : seg)
     {
      dbg.printf("%s", seg1.gCode);
     }
    }
    rapidDist += Math.hypot(x - xCur, y - yCur);
    Line2D shape = new Line2D.Float((float) (xCur * ncScale),
				    (float) (yCur * ncScale),
				    (float) (x * ncScale),
				    (float) (y * ncScale));
//    g.draw(shape);

    for (Image.GSeg gCode : seg)
    {
     out.printf("%s", gCode.gCode);
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
   System.out.printf("rapidDist %6.3f millDist %7.3f\n", rapidDist, millDist);
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
  write(data, gdraw.baseFile);
 }

 public String depth(double offset)
 {
  if (variables)
  {
   return(String.format("[#1 + %7.4f]", c(offset)));
  }
  else
  {
   return(String.format("%7.4f", depth + c(offset)));
  }
 }

 public double c(double val)
 {
  return(metric ? val * 25.4 : val);
 }

 public boolean track(int[] data, int i0) throws Exception
 {
  boolean remove = false;
  boolean lastArc = false;
  String comment;
  String cmt;
  GSeg gSeg;
  seg = new Seg(segNum);
  segList.add(seg);

  int x = i0 % w0;
  int y = i0 / w0;

  //  boolean output = (data[i0 - wx0] == BACKGROUND);
  boolean output = true;
  D d = D.XPOS;
  D dLast = d;

  String lDepth;
  String lRetract;
  String lLinearFeed;
  String lCircularFeed;

  if (this.variables)
  {
   lDepth = "[#1]";
   lRetract = "[#2]";
   lLinearFeed = "[#5]";
   lCircularFeed = "[#6]";
  }
  else
  {
   lDepth = String.format("%6.4f", this.depth);
   lRetract = String.format("%6.4f", this.retract);
   lLinearFeed = String.format("%3.1f", this.linearFeed);
   lCircularFeed = String.format("%3.1f", this.circularFeed);
  }

  if (this.metric)
  {
   g0Fmt = "g0 x%7.3f y%7.3f ";
   g1Fmt = "g1 x%7.3f y%7.3f ";
   g1Fmta = "g1 x%7.4f y%7.4f ";
   g3Fmt = "g3 x%7.3f y%7.3f ";
   g3FmtIJ = "i%7.3f j%7.3f ";
   depthFmt = "%7.3f";
  }
  else
  {
   g0Fmt = "g0 x%6.4f y%6.4f ";
   g1Fmt = "g1 x%6.4f y%6.4f ";
   g1Fmta = "g1 x%6.5f y%6.5f ";
   g3Fmt = "g3 x%6.4f y%6.4f ";
   g3FmtIJ = "i%6.4f j%6.4f ";
   depthFmt = "%7.4f";
  }

  if (output
  &&  (data[i0] != EDGE))
  {
   PadList.Pad pad = gdraw.padList.findPad(new Pt((int) (x * scale),
						  (int) (y * scale)));
   if (pad != null)
   {
    if (dbgFlag)
    {
     dbg.printf("pad %3d x %5d y %5d\n", pad.index, x, y);
    }
    i0 = pad.ap.c.findStart(this, pad.pt, x, y);
    if (i0 == 0)
    {
     pad.ap.c.mark(this, pad.pt);
     double radius = pad.ap.val1 / 2.0;
     double x0 = pad.pt.x / GDraw.SCALE;
     double y0 = pad.pt.y / GDraw.SCALE - radius;
     seg.setLoc(x0, y0);

     comment = String.format("(s %3d)", segNum);
     seg.add(x0, y0, String.format(g0Fmt + "%s\n", c(x0), c(y0), comment));
     if (!probeFlag)
     {
      seg.add(new GSeg(String.format("g1 z%s f%s\n", lDepth, lLinearFeed)));
     }
     else
     {
      double offset = probe.interpolate(x0,y0);
      seg.add(new GSeg(String.format("g1 z%s f%s\n",
				     depth(offset), lLinearFeed)));
     }
     comment = String.format("(s %3d p %3d)", segNum, pad.index);
     cmt = String.format("p%d", pad.index);
     seg.addCircle(x0, y0, radius,
		   String.format(g3Fmt + g3FmtIJ + "f%s %s\n",
				 c(x0), c(y0), 0.0, c(radius),
				 lCircularFeed, comment), cmt);
     seg.add(new GSeg(String.format("g0 z%s\n", lRetract)));
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
   seg.setLoc(x0, y0);
   comment = String.format("(s %3d)", segNum);
   seg.add(x0, y0, String.format(g0Fmt + "%s\n", c(x0), c(y0), comment));
   if (!probeFlag)
   {
    seg.add(new GSeg(String.format("g1 z%s f%s\n", lDepth, lLinearFeed)));
   }
   else
   {
    double offset = probe.interpolate(x0,y0);
    seg.add(new GSeg(String.format("g1 z%s f%s\n",
				   depth(offset), lLinearFeed)));
   }
  }

  int count = 0;
  int len = 0;
  int totalLen = 0;
  int leg = 0;
  int ofs = 0;
  int i0Last = i0;
  
  int lastX = 0;
  int lastY = 0;
  while (true)
  {
   if (i0 <= 0)
   {
    if (dbgFlag)
    {
     dbg.printf("index 0\n");
     dbg.flush();
    }
   }
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
      dbg.printf("pad %3d x %5d y %5d\n", pad.index, x, y);
      dbg.flush();
     }

     double x0 = x / ncScale;	/* path start */
     double y0 = y / ncScale;
     lastX = x;
     lastY = y;
     
     comment = String.format("(s %3d l %4d %4d a %s)",
			     segNum, leg, len, dLast.toString());
     cmt = String.format("%d-%d", segNum, leg);
     double xL = (i0Last % w0) / ncScale;
     double yL = (i0Last / w0) / ncScale;
     if (!probeFlag)
     {
      seg.add(xL, yL, String.format(g1Fmt + "f%s %s\n",
				    c(xL), c(yL), lLinearFeed, comment), cmt);
     }
     else
     {
      Probe.SegmentList segs = probe.splitLine(seg.curX, seg.curY, 0.0,
					       xL, yL, 0.0);
      String tmp = String.format(" f%s %s\n", lLinearFeed, comment);
//      boolean prt = segs.size() > 1;
      for (Probe.Point p : segs)
      {
//       if (prt)
//       {
//	seg.add(new GSeg(String.format("(" + g1Fmt + "f%s)\n", c(xL), c(yL),
//				       lLinearFeed)));
//	prt = false;
//       }
       seg.add(p.x, p.y, String.format(g1Fmt + "z%s%s",
				       c(p.x), c(p.y), depth(p.z), tmp), cmt);
       tmp = " (b)\n";
      }
     }

     seg.add(x0, y0, String.format(g1Fmt + "(c)\n", c(x0), c(y0)));

     Circle.ArcEnd arcEnd;
     arcEnd = pad.ap.c.markArc(this, pad.pt, x, y); /* mark path and find end */
     i0 = arcEnd.i0;
     if (i0 != i0Pad)		/* if on a track */
     {
      int ix1 = i0 % w0;	/* integer path end location */
      int iy1 = i0 / w0;
      double x1 = ix1 / ncScale; /* double path end location */
      double y1 = iy1 / ncScale;
      double dist = Math.hypot(x1 - x0, y1 - y0);
      if (dist <= .003)		/* if distance short line instead of arc */
      {
       if (dbgFlag)
       {
	dbg.printf("pad %3d x %5d y %5d x %5d y %5d dist %6.4f\n",
		   pad.index, x, y, ix1, iy1, dist);
	dbg.flush();
       }
       comment = String.format("(s %3d p %3d d)", segNum, pad.index);
       if (!probeFlag)
       {
	seg.add(x0, y0, String.format(g1Fmt + "f%s %s\n",
				    c(x1), c(y1), lLinearFeed, comment));
       }
       else
       {
	double offset = probe.interpolate(x0, y0);
	seg.add(x0, y0, String.format(g1Fmt + "z%s f%s %s\n",
				    c(x1), c(y1), depth(offset), 
				    lLinearFeed, comment));
       }
      }
      else			/* if arc required */
      {
       double cx = (pad.pt.x / scale) / ncScale; /* calculate center */
       double cy = (pad.pt.y / scale) / ncScale;

       double i = cx - x0;
       double j = cy - y0;
       double r0 = Math.hypot(i, j);		/* radius at start */
       double r1 = Math.hypot(cx - x1, cy - y1); /* radius at end */
       double err = Math.abs(r0 - r1);

       double endX = r0 * Math.cos(arcEnd.end) + cx;
       double endY = r0 * Math.sin(arcEnd.end) + cy;

       double rerr = Math.hypot(endX - x1, endY - y1);
       if (dbgFlag)
       {
	dbg.printf("r0 %8.6f r1 %8.6f diff %8.6f\n", r0, r1, err);
	dbg.printf("x1 %8.6f y1 %8.6f endX %8.6f endY %8.6f rerr %8.6f\n",
		   x1, y1, endX, endY, rerr);
       }
       comment = String.format("(s %3d p %3d)", segNum, pad.index);
       cmt = String.format("p%d", pad.index);
       if (!probeFlag)
       {
	seg.addArc(endX, endY, i, j,
		   String.format(g3Fmt + g3FmtIJ + "f%s %s\n",
				 c(endX), c(endY), c(i), c(j),
				 lCircularFeed, comment), cmt);
       }
       else
       {
	double offset = probe.interpolate(endX,  endY);
	seg.addArc(endX, endY, i, j,
		   String.format(g3Fmt + "z%s " + g3FmtIJ + "f%s %s\n",
				 c(endX), c(endY), depth(offset), c(i), c(j),
				 lCircularFeed, comment), cmt);
       }
       lastArc = true;
       
       if (err > .0005)
       {
	if (dbgFlag)
	{
	 dbg.printf("add segment x %5d y %6d\n", x, y);
	}
	if ((x1 > 0) && (y1 > 0))
	{
	 comment = "(e)";
	 if (!probeFlag)
	 {
	  seg.add(x1, y1, String.format(g1Fmta + " f%s %s)\n",
					c(x1), c(y1), lLinearFeed, comment));
	 }
	 else
	 {
	  double offset = probe.interpolate(x1, y1);
	  seg.add(x1, y1, String.format(g1Fmta + "z%s f%s %s\n",
					c(x1), c(y1), depth(offset),
					lLinearFeed, comment));
	 }
	}
       }
      }
     }
    }
    else
    {
     if (dbgFlag)
     {
      dbg.printf("pad not found at x %5d y %5d\n", x, y);
     }
     if (remove)
     {
      segList.remove(seg);
     }
     return(false);
    }

    i0Last = i0;
    dLast = D.INVALID;
    totalLen += len;
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
     dbg.printf("dir %d count %4d len %4d x %5d y %5d\n",
		dLast.ordinal(), count, len, x, y);
    }
    if (output)
    {
     if (lastArc && (len < 2))
     {
      lastArc = false;
     }
     else
     {
      double tx = x / ncScale;
      double ty = y / ncScale;
      comment = String.format("(s %3d l %4d %4d f %s)",
			      segNum, leg, len, dLast.toString());
      cmt = String.format("%d-%d", segNum, leg);
      if (!probeFlag)
      {
       seg.add(tx, ty, String.format(g1Fmt + "f%s %s\n", c(tx), c(ty), 
				     lLinearFeed, comment), cmt);
      }
      else
      {
       Probe.SegmentList segs = probe.splitLine(seg.curX, seg.curY, 0.0,
						tx, ty, 0.0);
       String tmp = String.format(" f%s %s\n", lLinearFeed, comment);
//       boolean prt = segs.size() > 1;
       for (Probe.Point p : segs)
       {
//	if (prt)
//	{
//	 seg.add(new GSeg(String.format("(" + g1Fmt + "f%s)\n", c(tx), c(ty),
//					lLinearFeed)));
//	 prt = false;
//	}
	seg.add(p.x, p.y, String.format(g1Fmt + "z%s%s",
					c(p.x), c(p.y), depth(p.z), tmp), cmt);
	tmp = " (g)\n";
       }
      }
     }
    }
    leg++;
    totalLen += len;
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
    dbg.printf("short totalLen %3d %10d x %5d y %5d\n",
	       totalLen, i0, i0 % w0, i0 / w0);
   }
   remove = true;
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
   dbg.printf("dir %d %4d %4d x %5d y %5d\n", d.ordinal(), count, len, x, y);
   dbg.printf("start %8d x %4d y %4d end %8d x %4d y %4d %8d\n",
	      startIndex, startIndex % w0, startIndex / w0, i0, x, y, tmp);
   dbg.flush();
  }

  if (output)
  {
   double tx = x / ncScale;
   double ty = y / ncScale;
   comment = String.format("(s %3d l %4d %4d h %s)",
			   segNum, leg, len, dLast.toString());
   cmt = String.format("%d-%d", segNum, leg);
   if (!probeFlag)
   {
    seg.add(tx, ty, String.format(g1Fmt + "f%s %s\n",
				  c(tx), c(ty), lLinearFeed, comment), cmt);
   }
   else
   {
    double offset = probe.interpolate(tx,ty);
    seg.add(tx, ty, String.format(g1Fmt + "z%s f%s %s\n",
				  c(tx), c(ty), depth(offset),
				  lLinearFeed, comment), cmt);
   }
   seg.add(new GSeg(String.format("g0 z%s\n\n", lRetract)));
   if (tmp == 0)
   {
    seg.closed = true;
   }

   if (dbgFlag)
   {
    dbg.printf("strX %6.3f strY %6.3f curX %6.3f curY %6.3f dist %6.3f\n",
	       seg.x, seg.y, seg.curX, seg.curY, seg.dist);
    dbg.printf("%s\n", tmp == 0 ? "closed" : "open");
   }

   if (tmp != 0)
   {
    if (seg.dist < .005)
    {
     data[startIndex] = STRERR;
     data[i0] = TRKERR;
     if (dbgFlag)
     {
      dbg.printf("track error seg %d\n", segNum);
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
    dbg.printf("remove segment %d\n", seg.num);
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
   image.setRGB(0, i, w0, 1, data, j, w0);
  }
  write(f);
 }

 public void write(String f)
 {
  File file = new File(f + ".png");

  try
  {
   ImageIO.write(image, "png", file);
  }
  catch (IOException e)
  {
   System.out.println(e);
  }
 }

 public static enum GCode
 {
  G0,				/* 0 */
  G1,				/* 1 */
  G2,				/* 2 */
  G3,				/* 3 */
  NONE,				/* 4 */
 };

 public class GSeg
 {
  GCode code;
  double x;
  double y;
  Double z = null;
  double i;
  double j;
  String zString;
  String comment;
  String gCode;

  public GSeg(GCode code, double z)
  {
   this.code = code;
   this.z = z;
  }

  public GSeg(GCode code, double x, double y)
  {
   this.code = code;
   this.x = x;
   this.y = y;
  }

  public GSeg(String gCode)
  {
   this.code = GCode.NONE;
   this.gCode = gCode;
  }

  public void setZ(double z)
  {
  }

  public void setIJ(double i, double j)
  {
  }

  public void setComment(String comment)
  {
  }

  public void setGCode(String gCode)
  {
  }
 }

 public class Seg extends ArrayList<Image.GSeg>
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

  public Image.GSeg add(double x, double y, String str)
  {
   double d = Math.hypot(x - curX, y - curY);
   dist += d;
   if (dbgFlag)
   {
    dbg.printf("%s", str);
    dbg.printf("curX %6.3f curY %6.3f x %6.3f y %6.3f dist %6.3f\n\n",
	       curX, curY, x, y, d);
   }
   if (dxf)
   {
    d1.setLayer(gdraw.tracks);
    d1.line(x, y);
   } 
   curX = x;
   curY = y;
   Image.GSeg gSeg = new GSeg(str);
   add(gSeg);
   return(gSeg);
  }

  public Image.GSeg add(double x, double y, String str, String comment)
  {
   double d = Math.hypot(x - curX, y - curY);
   dist += d;
   if (dbgFlag)
   {
    dbg.printf("%s", str);
    dbg.printf("curX %6.3f curY %6.3f x %6.3f y %6.3f dist %6.3f\n\n",
	       curX, curY, x, y, d);
   }
   if (dxf)
   {
    d1.setLayer(gdraw.tracks);
    d1.line(x, y);
    d1.setLayer(gdraw.trackNum);
    d1.text((x + curX) / 2, (y + curY) / 2, 0.001, comment);
   } 
   curX = x;
   curY = y;
   Image.GSeg gSeg = new GSeg(str);
   add(gSeg);
   return(gSeg);
  }

  public void addCircle(double x, double y, double r, String str)
  {
   double d = 2.0 * Math.PI * r;
   dist += d;
   if (dbgFlag)
   {
    dbg.printf("%s", str);
    dbg.printf("curX %6.3f curY %6.3f r %6.4f dist %6.3f\n\n",
	       x, y, r, d);
   }
   add(new GSeg(str));
   if (dxf)
   {
    d1.setLayer(gdraw.pads);
    d1.circle(x, y + r, r);
   }
  }

  public void addCircle(double x, double y, double r, String str,
			String comment)
  {
   double d = 2.0 * Math.PI * r;
   dist += d;
   if (dbgFlag)
   {
    dbg.printf("%s", str);
    dbg.printf("curX %6.3f curY %6.3f r %6.4f dist %6.3f\n\n",
	       x, y, r, d);
   }
   add(new GSeg(str));
   if (dxf)
   {
    d1.setLayer(gdraw.pads);
    d1.circle(x, y + r, r);
    d1.setLayer(gdraw.padNum);
    d1.text(x, y + r, .001, comment);
   }
  }

  public void addArc(double x, double y, double i, double j, String str,
		     String comment)
  {
   double cx = curX + i;
   double cy = curY + j;
   double x0 = -i;
   double y0 = -j;
   double x1 = x - cx;
   double y1 = y - cy;
   double r = Math.hypot(x0, y0);
   double theta0 = Math.atan2(y0, x0);
   if (theta0 < 0)
   {
    theta0 += 2 * Math.PI;
   }
   double theta1 = Math.atan2(y1, x1);
   if (theta1 < 0)
   {
    theta1 += 2 * Math.PI;
   }
   double delta = theta1 - theta0;
   if (delta < 0)
   {
    delta += 2 * Math.PI;
   }
   dist += r * delta;
   curX = x;
   curY = y;
   if (dbgFlag)
   {
    dbg.printf("%s", str);
    dbg.printf("curX %6.3f curY %6.3f cx %6.3f cy %6.3f\n",
	       curX, curY, cx, cy);
    dbg.printf("x0 %6.3f y0 %6.3f x1 %6.3f y1 %6.3f r %6.4f " +
	       "theta0 %4.0f theta1 %4.0f delta %4.0f\n\n",
	       x0, y0, x1, y1, r, Math.toDegrees(theta0),
	       Math.toDegrees(theta1), Math.toDegrees(delta));
   }
   if (dxf)
   {
    d1.setLayer(gdraw.pads);
    d1.arc(cx, cy, r, Math.toDegrees(theta0), Math.toDegrees(theta1));
    d1.setLoc(x, y);
    d1.setLayer(gdraw.padNum);
    d1.text(x, y, .001, comment);
   }
   add(new GSeg(str));
  }

  public void setLoc(double xLoc, double yLoc)
  {
   x = xLoc;
   y = yLoc;
   curX = xLoc;
   curY = yLoc;
   if (dxf)
   {
    d1.setLoc(xLoc, yLoc);
   }
  }

  public double dist(Seg s)
  {
   return(Math.hypot(s.x - curX, s.y - curY));
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
