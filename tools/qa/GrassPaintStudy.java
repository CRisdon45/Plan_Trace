import com.example.engine.NorthstarGrassPaint;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Inspection adapter only: every pixel comes from the production grass painter. */
public final class GrassPaintStudy {
    public static void main(String[] args) throws Exception {
        File dir=new File(args.length>0?args[0]:"build/grass-study");dir.mkdirs();
        String id=args.length>1?args[1]:"ground-study";
        long seed=0xCBF29CE484222325L;
        for(char c:id.toCharArray())seed=(seed^c)*0x100000001B3L;
        long start=System.nanoTime();
        int[] pixels=NorthstarGrassPaint.render(1024,717,seed,(name,w,h,data)->{
            try { save(dir,name,w,h,data); } catch(Exception e){throw new RuntimeException(e);}
        });
        save(dir,"grass",1024,717,pixels);
        System.out.printf("Production grass paint: %.3f s, %s%n",(System.nanoTime()-start)/1e9,dir);
    }
    static void save(File dir,String name,int w,int h,int[] pixels)throws Exception{
        BufferedImage image=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0,0,w,h,pixels,0,w);ImageIO.write(image,"png",new File(dir,name+".png"));
    }
}
