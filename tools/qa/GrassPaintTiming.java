import java.net.URLClassLoader;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
/** Host-only uncached paint timing. Pass directories containing compiled production classes.
 * Three samples follow one warmup. This does not measure Android frames or pen latency.
 */
class GrassPaintTiming {
 public static void main(String[] args)throws Exception {
  for(String path:args)try(URLClassLoader loader=new URLClassLoader(new java.net.URL[]{new File(path).toURI().toURL()},null)) {
   Method render=loader.loadClass("com.example.engine.NorthstarGrassPaint").getMethod("render",int.class,int.class,long.class);
   long seed=0xCBF29CE484222325L;for(char c:"ground-study".toCharArray())seed=(seed^c)*0x100000001B3L;
   render.invoke(null,1024,717,seed);
   double[] times=new double[3];int hash=0;
   for(int i=0;i<3;i++){long start=System.nanoTime();int[] pixels=(int[])render.invoke(null,1024,717,seed);times[i]=(System.nanoTime()-start)/1e6;int next=Arrays.hashCode(pixels);if(i>0&&hash!=next)throw new AssertionError("Paint changed");hash=next;}
   System.out.println(path+" milliseconds "+Arrays.toString(times)+", pixel hash "+hash);
  }
 }
}
