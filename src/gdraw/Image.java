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
   float width = 0.0f;
   TrackList.Track t0 = tl.get(i);
   float wx0 = (float) ((t0.ap.val1 * GDraw.SCALE) / scale);
   for (int j = i + 1; j < tl.size(); j++)
   {
    TrackList.Track t1 = tl.get(j);
    float w1 = (float) ((t1.ap.val1 * GDraw.SCALE) / scale);
    width = (wx0 < w1) ? wx0 : w1;
/*
    if (width <= 20)
    {
     width = (float) 3.0;
    }
*/
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
  g.setColor(Color.BLACK);

  int maxDist = (int) (GDraw.SCALE * .120);
  int minDist = (int) (GDraw.SCALE * .085);

  PadList pList = gdraw.padList;
  for (int i = 0; i < pList.size() - 1; i++)
  {
   int dist;
   boolean flag;
   PadList.Pad pi = pList.get(i);
   for (int j = i + 1; j < pList.size(); j++)
   {
    boolean swap = false;
    PadList.Pad pj = pList.get(j);

    if (pi.pt.x == pj.pt.x)
    {
     dist = pj.pt.y - pi.pt.y;
     flag = true;
     if (dbgFlag)
     {
      dbg.printf("pad v %3d %3d x %6d yi %6d yj %6d dist %6d %5.3f %5.3f\n",
		 pi.index,pj.index,pi.pt.x,pi.pt.y,pj.pt.y,dist,
		 pi.ap.val2,pj.ap.val2);
     }
    }
    else if (pi.pt.y == pj.pt.y)
    {
     dist = pj.pt.x - pi.pt.x;
     flag = false;
     if (dbgFlag)
     {
      dbg.printf("pad h %3d %3d y %6d xi %6d xj %6d dist %6d %5.3f %5.3f\n",
		 pi.index,pj.index,pi.pt.y,pi.pt.x,pj.pt.x,dist,
		 pi.ap.val1,pj.ap.val1);
     }
    }
    else
    {
     continue;
    }

    if (dist < 0)
    {
     dist = -dist;
     swap = true;
     PadList.Pad pTmp = pi;
     pi = pj;
     pj = pTmp;
    }

    if ((dist >= minDist)
    &&  (dist <= maxDist))
    {
     int x;
     int y;
     if (flag)
     {
      x = pi.pt.x;
      int yi = pi.pt.y + (int) (pi.ap.val2 * GDraw.SCALE / 2);
      int yj = pj.pt.y - (int) (pj.ap.val2 * GDraw.SCALE / 2);
      y = ((yj - yi) / 2) + yi;
     }
     else
     {
      int xi = pi.pt.x + (int) (pi.ap.val1 * GDraw.SCALE / 2);
      int xj = pj.pt.x - (int) (pj.ap.val1 * GDraw.SCALE / 2);
      x = ((xj - xi) / 2) + xi;
      y = pi.pt.y;
     }

     x = (int) (x / scale);
     y = (int) (y / scale);

     int i0 = x + y * w0;
     int w;
     int h;

     if (data[i0] == BACKGROUND)
     {
      if ((pi.ap.type == ApertureList.Aperture.SQUARE)
      &&  (pj.ap.type == ApertureList.Aperture.SQUARE))
      {
//       if (((dist >= 1100) && (dist <= 1110))
//       ||  ((dist >= 890) && (dist <= 920)))
       {
	if (flag)
	{
	 w = (int) (pi.ap.val1 * GDraw.SCALE);
	 h = dist - (int) ((pi.ap.val2 + .010) * GDraw.SCALE);
	}
	else
	{
	 w = dist - (int) ((pi.ap.val1 + .010) * GDraw.SCALE);
	 h = (int) (pi.ap.val2 * GDraw.SCALE);
	}
	w = (int) (w / scale);
	h = (int) (h / scale);
	g.fillRect(x - w / 2,y - h / 2,w,h);
	if (dbgFlag)
	{
	 dbg.printf("rec %3d x %5d y %5d w %3d h %3d\n",pi.index,x,y,w,h);
	}
       }
      }
//      else if ((pi.ap.type == ApertureList.Aperture.ROUND)
//      &&       (pj.ap.type == ApertureList.Aperture.ROUND))
//      {
//       if (false)
//       {
//	if (flag)
//	{
//	 w = (int) (pi.ap.val1 * GDraw.SCALE * .25 / scale);
//	 h = 7;
//	}
//	else
//	{
//	 w = 7;
//	 h = (int) (pi.ap.val1 * GDraw.SCALE * .25 / scale);
//	}
//	g.fillRect(x - w / 2,y - h / 2,w,h);
//	dbg.printf("rnd %3d x %5d y %5d w %3d h %3d\n",pi.index,x,y,w,h);
//       }
//      }
     }
    }
   }
  }
 }

 public void process()
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
    dist += Math.hypot(x - xCur,y - yCur);
    Line2D shape = new Line2D.Float((float) (xCur * ncScale),
				    (float) (yCur * ncScale),
				    (float) (x * ncScale),
				    (float) (y * ncScale));
    g.draw(shape);
    xCur = x;
    yCur = y;
    for (int j = 0; j < seg.size(); j++)
    {
     out.printf("%s",seg.get(j));
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

 public boolean track(int[] data, int i0)
 {
  boolean remove = false;
  seg = new Seg();
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

  if ((x <= 5)
  ||  (y <= 5))
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
   if (dbgFlag)
   {
    dbg.printf("track error seg %d\n",segNum);
   }
   data[startIndex] = STRERR;
   data[i0] = TRKERR;
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

  if (remove
  ||  (seg.size() == 4))
  {
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
  File file = new File(f + ".bmp");

  try
  {
   ImageIO.write(image,"bmp",file);
  }
  catch (IOException e)
  {
   System.out.println(e);
  }
 }

 public class Seg extends ArrayList<String>
 {
  double x;
  double y;

  public Seg()
  {
   super();
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
