import com.example.engine.NorthstarGrassPaint;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.Font;
import java.awt.RenderingHints;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/** Inspection adapter only: every pixel comes from the production grass painter. */
public final class GrassPaintStudy {
    public static void main(String[] args) throws Exception {
        File dir=new File(args.length>0?args[0]:"build/grass-study");dir.mkdirs();
        String id=args.length>1?args[1]:"ground-study";
        long seed=0xCBF29CE484222325L;
        for(char c:id.toCharArray())seed=(seed^c)*0x100000001B3L;
        long start=System.nanoTime();
        List<BufferedImage> stages=new ArrayList<>();
        int[] pixels=NorthstarGrassPaint.render(1024,717,seed,(name,w,h,data)->{
            try { stages.add(save(dir,name,w,h,data)); } catch(Exception e){throw new RuntimeException(e);}
        });
        save(dir,"grass",1024,717,pixels);
        sheet(dir,stages);
        System.out.printf("Production grass paint: %.3f s, %s%n",(System.nanoTime()-start)/1e9,dir);
    }
    static BufferedImage save(File dir,String name,int w,int h,int[] pixels)throws Exception{
        BufferedImage image=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0,0,w,h,pixels,0,w);ImageIO.write(image,"png",new File(dir,name+".png"));
        return image;
    }
    static void sheet(File dir,List<BufferedImage> stages)throws Exception{
        BufferedImage sheet=new BufferedImage(1900,410,BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g=sheet.createGraphics();
        g.setColor(new Color(249,247,239));g.fillRect(0,0,sheet.getWidth(),sheet.getHeight());
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setColor(new Color(44,57,38));g.setFont(new Font("Serif",Font.PLAIN,28));
        g.drawString("GRASS / PAINT BUILDUP",25,42);
        g.setFont(new Font("SansSerif",Font.PLAIN,15));
        g.drawString("Five cumulative stages from the production renderer. Same surface and seed; full frames shown.",25,68);
        String[] labels={"1  BASE WASH","2  MIDTONE GLAZES","3  DRYING FRONTS","4  SMALL DEPOSITS","5  FINAL GRASS"};
        for(int i=0;i<stages.size();i++){
            int x=25+i*374;g.drawImage(stages.get(i),x,92,354,248,null);
            g.setFont(new Font("SansSerif",Font.BOLD,16));g.drawString(labels[i],x,369);
        }
        g.dispose();ImageIO.write(sheet,"png",new File(dir,"grass-layer-progression.png"));
    }
}
