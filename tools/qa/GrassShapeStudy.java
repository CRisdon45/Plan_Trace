import com.example.engine.NorthstarGrassPaint;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Desktop mask adapter only. Android uses its own exact Path rasterizer. */
public final class GrassShapeStudy {
 public static void main(String[] args)throws Exception {
  File out=new File(args[0]);out.mkdirs();int w=720,h=540;
  Shape[] shapes=new Shape[5];String[] names={"rectangle","curved","concave","courtyard","narrow"};
  shapes[0]=new Rectangle2D.Double(0,0,w,h);
  Path2D curve=new Path2D.Double();curve.moveTo(0,240);curve.curveTo(0,0,220,0,360,70);curve.curveTo(600,-15,720,90,720,280);curve.curveTo(720,540,510,540,360,450);curve.curveTo(160,590,0,480,0,240);curve.closePath();shapes[1]=curve;
  Path2D l=new Path2D.Double();l.moveTo(0,0);l.lineTo(w,0);l.lineTo(w,h);l.lineTo(w*.58,h);l.lineTo(w*.58,h*.40);l.lineTo(0,h*.40);l.closePath();shapes[2]=l;
  Area court=new Area(new Rectangle2D.Double(0,0,w,h));court.subtract(new Area(new RoundRectangle2D.Double(150,120,420,300,85,85)));shapes[3]=court;
  Path2D n=new Path2D.Double();n.moveTo(0,0);n.lineTo(w,0);n.lineTo(w,100);n.lineTo(95,100);n.lineTo(95,h);n.lineTo(0,h);n.closePath();shapes[4]=n;
  BufferedImage board=new BufferedImage(w*3+80,h*2+130,BufferedImage.TYPE_INT_RGB);Graphics2D g=board.createGraphics();g.setColor(new Color(249,247,238));g.fillRect(0,0,board.getWidth(),board.getHeight());g.setFont(new Font("SansSerif",Font.PLAIN,22));
  long seed=0xCBF29CE484222325L;for(char c:"shape-study".toCharArray())seed=(seed^c)*0x100000001B3L;
  for(int k=0;k<shapes.length;k++) {
   BufferedImage mask=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);Graphics2D m=mask.createGraphics();m.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);m.setColor(Color.WHITE);m.fill(shapes[k]);m.dispose();float[] coverage=new float[w*h];
   for(int y=0;y<h;y++)for(int x=0;x<w;x++)coverage[y*w+x]=((mask.getRGB(x,y)>>>24)&255)/255f;
   final String name=names[k];int[] pixels=NorthstarGrassPaint.renderShape(w,h,seed,coverage,null);
   BufferedImage paint=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
   for(int y=0;y<h;y++)for(int x=0;x<w;x++){int i=y*w+x;int c=pixels[i];double a=coverage[i];int r=(int)(((c>>16)&255)*a+249*(1-a)),b=(int)((c&255)*a+238*(1-a)),v=(int)(((c>>8)&255)*a+247*(1-a));paint.setRGB(x,y,(r<<16)|(v<<8)|b);}
   ImageIO.write(paint,"png",new File(out,name+".png"));int bx=20+(k%3)*(w+20),by=45+(k/3)*(h+50);g.setColor(new Color(45,56,37));g.drawString(name,bx,by-12);g.drawImage(paint,bx,by,null);
  }
  g.dispose();ImageIO.write(board,"png",new File(out,"shapes.png"));
 }
}
