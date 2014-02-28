/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gdraw;

import java.util.ArrayList;
import java.io.PrintWriter;
import gdraw.Util.*;

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

 public void add(Pt p, ApertureList.Aperture a)
 {
  Pad pad;
  add(pad = new Pad(max++,p,a));
  pad.print(dbg);
 }

 public Pad findPad(Pt pt)
 {
  for (int i = 0; i < max; i++)
  {
   Pad p = get(i);
   if (p.ap.connected(p.pt,pt))
   {
    return(p);
   }
  }
  return(null);
 }

 public void draw(Image image)
 {
  if (dbgFlag)
  {
   dbg.printf("\nDraw Pads\n\n");
  }
  for (int i = 0; i < max; i++)
  {
   Pad p = get(i);
   image.draw(p);
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
    dbg.printf("%3d %6d y %6d ",i,pad.pt.x,pad.pt.y);

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

 public class Pad
 {
  Pt pt;			/* pad location */
  ApertureList.Aperture ap;	/* aperture */
  int index;			/* pad number */

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

  public void print(PrintWriter dbg)
  {
   if (dbgFlag)
   {
    dbg.printf("x %6d y %6d pad %3d\n",pt.x,pt.y,index);
   }
  }
 }
}
