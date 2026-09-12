package com.example.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Deterministic grass pigment, shared by Android and the desktop inspection tool.
 * Independent implementation of recursive polygon glazes (Hobbs, 2017), with
 * mass-conserving edge redistribution and Kubelka-Munk layering (Curtis et al., 1997).
 * This is an artistic drying approximation, not a shallow-water fluid solver.
 * Coordinates and the paper field belong to the object, never to the viewport.
 *
 * Construction is a few related washes, paper showing through, sediment in the
 * drying fronts, then sparse clustered deposits. It is not a particle field of
 * blades. Quiet interiors are required, not a defect to be filled.
 */
public final class NorthstarGrassPaint {
    private NorthstarGrassPaint() {}
    public interface Observer { void stage(String name, int width, int height, int[] pixels); }

    public static int[] render(int width, int height, long seed) {
        return render(width, height, seed, null);
    }

    public static int[] render(int width, int height, long seed, Observer observer) {
        if (width < 1 || height < 1 || width > 1024 || height > 1024)
            throw new IllegalArgumentException("Grass paint dimensions must be 1..1024");
        Studio s = new Studio(width, height, seed);
        s.paint(observer);
        return s.pixels();
    }

    private static final class Vertex {
        final double x, y, variance;
        Vertex(double x, double y, double variance) { this.x=x; this.y=y; this.variance=variance; }
    }

    private static final class Pigment {
        // Ordered-glaze reflectance/transmittance tables, sampled by dry thickness.
        final float[][] r = new float[3][4097], t = new float[3][4097];
        Pigment(double[] absorption, double[] scattering) {
            for (int c=0;c<3;c++) for (int i=0;i<=4096;i++) {
                double a=1+absorption[c]/scattering[c], b=Math.sqrt(a*a-1);
                double z=b*scattering[c]*i/1024.0;
                double denominator=a*Math.sinh(z)+b*Math.cosh(z);
                r[c][i]=(float)(Math.sinh(z)/denominator);
                t[c][i]=(float)(b/denominator);
            }
        }
    }

    // Artist-selected RGB coefficients, not measured spectral pigment identities.
    private static final Pigment YELLOW = new Pigment(new double[]{.22,.16,.92}, new double[]{.55,.68,.07});
    private static final Pigment OLIVE = new Pigment(new double[]{.53,.38,1.32}, new double[]{.12,.23,.035});
    private static final Pigment GREEN = new Pigment(new double[]{.93,.54,1.22}, new double[]{.05,.13,.028});
    private static final Pigment EARTH = new Pigment(new double[]{.51,.60,1.40}, new double[]{.12,.13,.03});
    private static final Pigment INK = new Pigment(new double[]{1.7,1.45,2.3}, new double[]{.025,.045,.015});

    private static final class Studio {
        final int w,h;
        final long seed;
        final Random random;
        final float[][] color;
        final float[] paper;
        float[][] underpainting;
        float[] textureField;
        Studio(int w,int h,long seed) {
            this.w=w; this.h=h; this.seed=seed; random=new Random(seed);
            color=new float[3][w*h]; paper=new float[w*h];
            for (int y=0;y<h;y++) for (int x=0;x<w;x++) {
                int i=y*w+x;
                double fine=noise(x*.68,y*.68,17), fiber=noise(x*.23,y*.36,23);
                paper[i]=(float)(.58*fine+.42*fiber);
                double brightness=.977+.024*(paper[i]-.5);
                color[0][i]=(float)brightness;
                color[1][i]=(float)(brightness*.985);
                color[2][i]=(float)(brightness*.921);
                // A luminous initial wash shares the same paper as every later deposit.
                double thickness=.22+.12*noise(x*.007,y*.007,81)+.04*(paper[i]-.5);
                apply(i,thickness*(.78+.44*paper[i]),YELLOW);
                apply(i,(.07+.08*noise(x*.012,y*.012,82))*(.78+.44*paper[i]),OLIVE);
            }
        }
        double between(double a,double b) { return a+(b-a)*random.nextDouble(); }
        void snapshot(Observer o,String name) { if(o!=null)o.stage(name,w,h,pixels()); }
        void paint(Observer o) {
            // A handful of large related glazes. Placement is stratified so they
            // form connected masses instead of a field of independent stamps.
            int sheets = 8;
            for (int j=0;j<sheets;j++) {
                double t=(j+.35)/sheets;
                double x=w*(.08+.84*t)+between(-w*.16,w*.16);
                double y=h*(.18+.62*noise(j*.37,1.1,3))+between(-h*.14,h*.14);
                wash(x,y,between(Math.min(w,h)*.28,Math.min(w,h)*.48),
                        j%3==0?YELLOW:OLIVE,between(.045,.11),.0,24,1,false);
            }
            snapshot(o,"01-base-wash");
            underpainting=new float[][]{color[0].clone(),color[1].clone(),color[2].clone()};
            for (int j=0;j<14;j++) {
                double t=(j+.2)/14.0;
                double x=w*(.10+.80*t)+between(-w*.12,w*.12);
                double y=h*(.15+.70*noise(j*.29,2.4,5))+between(-h*.10,h*.10);
                wash(x,y,between(48,125),j%5==0?YELLOW:OLIVE,between(.09,.20),.20,24,2,true);
            }
            snapshot(o,"02-midtone");
            // Stronger pigment and real drying fronts, biased toward one side so
            // the sheet keeps a light-to-shadow reading instead of even coverage.
            for (int j=0;j<11;j++) {
                double x=between(w*.38,w*1.06), y=between(-h*.08,h*1.06);
                wash(x,y,between(52,140),j%4==0?GREEN:OLIVE,between(.18,.40),.34,24,2,true);
            }
            for (int j=0;j<5;j++) {
                wash(between(w*.04,w*.55),between(h*.08,h*.92),between(36,88),
                        OLIVE,between(.12,.26),.28,20,2,true);
            }
            snapshot(o,"03-drying-fronts");
            liftAndRepaintTexture();
            snapshot(o,"04-lifted-texture");
            finishDeposits();
            snapshot(o,"05-final-grass");
        }

        /** Lift large connected passages back to the retained underpainting, then
         * glaze the remaining islands. Reserves are scalloped paint shapes, not a
         * warped scalar field (that produced stretched ribbons) and not white
         * speckle over the finished image.
         */
        void liftAndRepaintTexture() {
            float[] difference=new float[(w+1)*h];
            int islands=Math.max(8,(int)(14.0*w*h/(1024*717)));
            for(int j=0;j<islands;j++) {
                List<Vertex> base=deform(contour(between(-50,w+50),between(-50,h+50),between(110,230)),1);
                for(int k=0;k<8;k++)raster(deform(base,2),0,0,w,h,difference);
            }
            float[] reserveDiff=new float[(w+1)*h];
            int tongues=Math.max(4,(int)(8.0*w*h/(1024*717)));
            for(int j=0;j<tongues;j++) {
                List<Vertex> base=deform(contour(between(0,w),between(0,h),between(55,110)),1);
                for(int k=0;k<8;k++)raster(deform(base,2),0,0,w,h,reserveDiff);
            }
            float[] mask=new float[w*h];
            textureField=new float[w*h];
            for(int y=0;y<h;y++) {
                float keep=0,lift=0;
                for(int x=0;x<w;x++) {
                    int i=y*w+x;
                    keep+=difference[y*(w+1)+x];
                    lift+=reserveDiff[y*(w+1)+x];
                    double island=Math.max(0,Math.min(1,keep/8));
                    double reserve=Math.max(0,Math.min(1,lift/8));
                    mask[i]=(float)(island*(1-.92*reserve));
                    textureField[i]=mask[i];
                    double lifting=(1-mask[i])*(.74+.12*paper[i]);
                    for(int c=0;c<3;c++)color[c][i]+=(underpainting[c][i]-color[c][i])*lifting;
                }
            }
            float[] deposited=dry(mask,w,h,.38);
            for(int i=0;i<mask.length;i++) {
                double tooth=.50+.95*paper[i];
                apply(i,.12*deposited[i]*tooth,OLIVE);
            }
            int blooms=Math.max(8,(int)(40.0*w*h/(1024*717)));
            for(int j=0;j<blooms;j++) {
                double x=between(0,w),y=between(0,h);
                if(islandAt(x,y)<.36) continue;
                wash(x,y,between(14,40),j%4==0?EARTH:OLIVE,between(.07,.18),.20,12,1,true);
            }
        }

        /** Minute finishing lives on fronts and dark cores. Quiet paper between
         * the islands is left alone. Granulation is paper interaction, not confetti.
         */
        void finishDeposits() {
            sedimentFronts();
            int area=w*h;
            int mid=Math.max(20,(int)(110.0*area/(1024*717)));
            for(int j=0;j<mid*3;j++) {
                double x=between(0,w),y=between(0,h);
                double density=detailDensity(x,y);
                if(random.nextDouble()>density) continue;
                wash(x,y,between(3.5,12),j%5==0?EARTH:GREEN,between(.10,.32),.16,8,1,true);
            }
            int specks=Math.max(20,(int)(110.0*area/(1024*717)));
            for(int j=0;j<specks*3;j++) {
                double x=between(0,w),y=between(0,h);
                double island=islandAt(x,y);
                if(island<.28 || island>.68) continue;
                if(random.nextDouble()>.48+grouping(x,y)*.42) continue;
                wash(x,y,between(1.0,3.6),j%4==0?INK:GREEN,between(.16,.48),.08,4,1,false);
            }
            int blades=Math.max(8,(int)(42.0*area/(1024*717)));
            for(int j=0;j<blades*3;j++) {
                double x=between(0,w),y=between(0,h);
                if(islandAt(x,y)<.52) continue;
                if(random.nextDouble()>.36+grouping(x,y)*.50) continue;
                blade(x,y,between(5,13),between(.55,1.4),j%3==0?INK:GREEN,between(.45,1.2));
            }
            int cores=Math.max(14,(int)(38.0*area/(1024*717)));
            for(int j=0;j<cores*5;j++) {
                double x=between(0,w),y=between(0,h);
                if(islandAt(x,y)<.62) continue;
                if(random.nextDouble()>.34+grouping(x,y)*.55) continue;
                wash(x,y,between(4.5,13),j%4==0?GREEN:INK,between(.32,.72),.16,8,1,false);
            }
            for(int y=0;y<h;y++) for(int x=0;x<w;x++) {
                int i=y*w+x;
                if(textureField[i]<.60) continue;
                double clump=noise(x*.04,y*.04,601);
                if(clump<.72) continue;
                if(paper[i]<.52) continue;
                apply(i,(clump-.62)*(.22+.30*textureField[i])*paper[i],INK);
            }
        }

        void sedimentFronts() {
            if(textureField==null) return;
            float[] grad=new float[w*h];
            for(int y=1;y<h-1;y++) for(int x=1;x<w-1;x++) {
                int i=y*w+x;
                double dx=textureField[i+1]-textureField[i-1];
                double dy=textureField[i+w]-textureField[i-w];
                grad[i]=(float)Math.sqrt(dx*dx+dy*dy);
            }
            for(int y=1;y<h-1;y++) for(int x=1;x<w-1;x++) {
                int i=y*w+x;
                if(grad[i]<.05) continue;
                // Only parts of a front collect extra pigment. Adjacent segments stay soft.
                double broken=noise(x*.028,y*.028,501);
                if(broken<.46) continue;
                double tooth=.42+1.05*paper[i];
                apply(i,Math.min(.32,grad[i]*1.15)*(broken-.20)*tooth,OLIVE);
                if(grad[i]>.15 && broken>.78 && paper[i]>.55)
                    apply(i,Math.min(.18,grad[i]*.7)*tooth,INK);
            }
        }

        void blade(double x,double y,double length,double width,Pigment pigment,double load) {
            double angle=between(0,Math.PI*2),curve=between(-.4,.4)*length;
            List<Vertex> outline=new ArrayList<>();
            for(int side=0;side<2;side++)for(int j=0;j<=8;j++) {
                double t=(side==0?j:8-j)/8.0;
                double u=t*length,v=curve*t*t+(side==0?1:-1)*width*Math.sin(Math.PI*t)*.5;
                outline.add(new Vertex(x+u*Math.cos(angle)-v*Math.sin(angle),
                        y+u*Math.sin(angle)+v*Math.cos(angle),.04));
            }
            deposit(outline,pigment,load,.03,4,false);
        }
        double islandAt(double x,double y) {
            int i=Math.min(h-1,Math.max(0,(int)y))*w+Math.min(w-1,Math.max(0,(int)x));
            return textureField==null?1:textureField[i];
        }
        double grouping(double x,double y) { return noise(x*.024,y*.024,401); }
        double detailDensity(double x,double y) {
            double islands=islandAt(x,y);
            return Math.max(.01,(.04+.42*islands)*(.30+1.10*grouping(x,y))*(.55+.50*x/w));
        }

        List<Vertex> contour(double x,double y,double radius) {
            List<Vertex> v=new ArrayList<>();
            double stretch=radius<5?between(.5,1.65):between(.74,1.28), phase=between(0,Math.PI*2);
            double rotation=between(0,Math.PI*2);
            int lobes=radius>40?10+(int)between(0,8):8;
            double[] angle=new double[lobes], reach=new double[lobes], size=new double[lobes];
            for(int j=0;j<lobes;j++) {
                angle[j]=j*Math.PI*2/lobes+between(-.16,.16);
                reach[j]=radius*(.70+.22*Math.sin(3*angle[j]+phase)+between(-.06,.06));
                size[j]=radius*between(.10,.36);
            }
            int points=radius>10?160:28;
            for(int j=0;j<points;j++) {
                double a=j*Math.PI*2/points;
                double r=radius*(.76+.18*Math.sin(3*a+phase));
                for(int k=0;k<lobes;k++) {
                    double delta=a-angle[k], perp=reach[k]*Math.sin(delta);
                    if(Math.cos(delta)>0 && Math.abs(perp)<size[k])
                        r=Math.max(r,reach[k]*Math.cos(delta)+Math.sqrt(size[k]*size[k]-perp*perp));
                }
                double variance=.05+.16*(.5+.5*Math.sin(a*2+phase));
                double px=Math.cos(a)*r*stretch,py=Math.sin(a)*r/stretch;
                v.add(new Vertex(x+px*Math.cos(rotation)-py*Math.sin(rotation),
                        y+px*Math.sin(rotation)+py*Math.cos(rotation),variance));
            }
            return v;
        }
        List<Vertex> deform(List<Vertex> source,int rounds) {
            for(int round=0;round<rounds;round++) {
                List<Vertex> next=new ArrayList<>(source.size()*2);
                for(int j=0;j<source.size();j++) {
                    Vertex a=source.get(j),b=source.get((j+1)%source.size());
                    double dx=b.x-a.x,dy=b.y-a.y;
                    double normal=random.nextGaussian()*a.variance;
                    double along=random.nextGaussian()*a.variance*.22;
                    next.add(new Vertex(a.x,a.y,a.variance*.89));
                    next.add(new Vertex((a.x+b.x)*.5-dy*normal+dx*along,
                            (a.y+b.y)*.5+dx*normal+dy*along,a.variance*between(.72,.96)));
                }
                source=next;
            }
            return source;
        }

        void wash(double cx,double cy,double radius,Pigment pigment,double load,double drying,
                  int layers,int refinements,boolean reservePaper) {
            deposit(deform(contour(cx,cy,radius),refinements),pigment,load,drying,layers,reservePaper);
        }
        void deposit(List<Vertex> base,Pigment pigment,double load,double drying,int layers,boolean reservePaper) {
            double minX=w,minY=h,maxX=0,maxY=0;
            for(Vertex v:base) { minX=Math.min(minX,v.x); maxX=Math.max(maxX,v.x);
                minY=Math.min(minY,v.y); maxY=Math.max(maxY,v.y); }
            int left=Math.max(0,(int)Math.floor(minX)-5),top=Math.max(0,(int)Math.floor(minY)-5);
            int right=Math.min(w,(int)Math.ceil(maxX)+6),bottom=Math.min(h,(int)Math.ceil(maxY)+6);
            int bw=right-left,bh=bottom-top;
            if(bw<=0||bh<=0)return;
            float[] difference=new float[(bw+1)*bh];
            // Each layer has inherited wet/dry edge variance. Fractional scanline coverage
            // integrates faint deposits before quantization; no separate painted outline.
            for(int k=0;k<layers;k++) raster(deform(base,2),left,top,bw,bh,difference);
            float[] mask=new float[bw*bh];
            for(int y=0;y<bh;y++) {
                float total=0;
                for(int x=0;x<bw;x++) {
                    total+=difference[y*(bw+1)+x];
                    double coverage=Math.max(0,Math.min(1,total/layers));
                    if(reservePaper) {
                        // Correlated paper reserves remove only this glaze, exposing the
                        // earlier yellow/green deposits instead of painting white on top.
                        double rx=(x+left)*.041+3*noise((x+left)*.009,(y+top)*.009,201);
                        double ry=(y+top)*.041+3*noise((x+left)*.009,(y+top)*.009,202);
                        double n=.65*noise(rx,ry,97)+.35*noise(rx*2.8,ry*2.8,99);
                        coverage*=1-.82*smooth(.63,.76,n);
                    }
                    mask[y*bw+x]=(float)coverage;
                }
            }
            float[] front=new float[mask.length];
            for(int y=0;y<bh;y++)for(int x=0;x<bw;x++)
                front[y*bw+x]=(float)(.05+.95*smooth(.25,.7,noise((x+left)*.018,(y+top)*.018,111)));
            float[] density=dry(mask,bw,bh,drying,front);
            for(int y=0;y<bh;y++) for(int x=0;x<bw;x++) {
                int i=(y+top)*w+x+left,j=y*bw+x;
                if(density[j]<=0)continue;
                // Pooling follows paper hollows. Fine digital grain is not added on top.
                double grain=.48+1.05*paper[i];
                grain*=.62+.76*noise((x+left)*.11,(y+top)*.11,64);
                double thickness=load*density[j]*grain;
                if(pigment!=null)apply(i,thickness,pigment);
                else for(int c=0;c<3;c++)color[c][i]+=(underpainting[c][i]-color[c][i])*Math.min(.9,thickness);
            }
        }

        void raster(List<Vertex> vertices,int left,int top,int bw,int bh,float[] diff) {
            double[][] crossings=new double[bh][];
            int[] count=new int[bh];
            for(int j=0;j<vertices.size();j++) {
                Vertex a=vertices.get(j),b=vertices.get((j+1)%vertices.size());
                if(a.y==b.y)continue;
                int start=Math.max(0,(int)Math.ceil(Math.min(a.y,b.y)-top-.5));
                int end=Math.min(bh,(int)Math.ceil(Math.max(a.y,b.y)-top-.5));
                for(int y=start;y<end;y++) {
                    if(crossings[y]==null)crossings[y]=new double[16];
                    if(count[y]==crossings[y].length)crossings[y]=Arrays.copyOf(crossings[y],count[y]*2);
                    crossings[y][count[y]++]=a.x+(top+y+.5-a.y)*(b.x-a.x)/(b.y-a.y)-left;
                }
            }
            for(int y=0;y<bh;y++) {
                if(count[y]<2)continue;
                Arrays.sort(crossings[y],0,count[y]);
                for(int j=0;j+1<count[y];j+=2) {
                    double a=Math.max(0,Math.min(bw,crossings[y][j]));
                    double b=Math.max(0,Math.min(bw,crossings[y][j+1]));
                    int ia=(int)a,ib=(int)b,offset=y*(bw+1);
                    if(ia==ib) { if(ia<bw){diff[offset+ia]+=b-a;diff[offset+ia+1]-=b-a;} }
                    else {
                        diff[offset+ia]+=1-(a-ia); diff[offset+ia+1]+=a-ia;
                        diff[offset+ib]-=1-(b-ib); if(ib<bw)diff[offset+ib+1]-=b-ib;
                    }
                }
            }
        }

        void apply(int i,double thickness,Pigment pigment) {
            double u=Math.max(0,Math.min(4095,thickness*1024));
            int k=(int)u; float f=(float)(u-k);
            for(int c=0;c<3;c++) {
                float r=pigment.r[c][k]+f*(pigment.r[c][k+1]-pigment.r[c][k]);
                float t=pigment.t[c][k]+f*(pigment.t[c][k+1]-pigment.t[c][k]);
                color[c][i]=r+t*t*color[c][i]/(1-r*color[c][i]);
            }
        }
        double noise(double x,double y,int salt) {
            int ix=(int)Math.floor(x),iy=(int)Math.floor(y);
            double fx=x-ix,fy=y-iy; fx=fx*fx*(3-2*fx);fy=fy*fy*(3-2*fy);
            double a=hash(ix,iy,salt),b=hash(ix+1,iy,salt),c=hash(ix,iy+1,salt),d=hash(ix+1,iy+1,salt);
            return (a+(b-a)*fx)*(1-fy)+(c+(d-c)*fx)*fy;
        }
        double hash(int x,int y,int salt) {
            long z=seed ^ (x*0x9E3779B97F4A7C15L) ^ (y*0xC2B2AE3D27D4EB4FL) ^ (salt*0x165667B19E3779F9L);
            z=(z^(z>>>30))*0xBF58476D1CE4E5B9L; z=(z^(z>>>27))*0x94D049BB133111EBL;
            return ((z^(z>>>31))>>>11)*0x1.0p-53;
        }
        int[] pixels() {
            int[] out=new int[w*h];
            for(int i=0;i<out.length;i++) {
                int r=channel(color[0][i]),g=channel(color[1][i]),b=channel(color[2][i]);
                out[i]=0xff000000|r<<16|g<<8|b;
            }
            return out;
        }
    }

    /** Redistribute a finite fraction of a glaze to its own evaporating front.
     * Total deposited pigment equals incoming coverage. Sharpness follows wet-mask
     * gradients, including internal paper reserves; no independently jittered rim.
     */
    static float[] dry(float[] mask,int w,int h,double fraction) {
        return dry(mask,w,h,fraction,null);
    }
    private static float[] dry(float[] mask,int w,int h,double fraction,float[] front) {
        if(fraction==0)return mask.clone();
        float[] edge=new float[mask.length],out=new float[mask.length];
        float[] horizontal=new float[mask.length];
        // Separable box integration is linear in pixels, independent of kernel area.
        for(int y=0;y<h;y++) {
            double sum=0;
            for(int k=-4;k<=4;k++)sum+=mask[y*w+Math.max(0,Math.min(w-1,k))];
            for(int x=0;x<w;x++) {
                horizontal[y*w+x]=(float)(sum/9);
                sum+=mask[y*w+Math.min(w-1,x+5)]-mask[y*w+Math.max(0,x-4)];
            }
        }
        double mass=0,edgeMass=0;
        for(int x=0;x<w;x++) {
            double sum=0;
            for(int k=-4;k<=4;k++)sum+=horizontal[Math.max(0,Math.min(h-1,k))*w+x];
            for(int y=0;y<h;y++) {
                int i=y*w+x;float m=mask[i];
                edge[i]=(float)(m*Math.max(0,m-sum/9)*(front==null?1:front[i]));
                mass+=m;edgeMass+=edge[i];
                sum+=horizontal[Math.min(h-1,y+5)*w+x]-horizontal[Math.max(0,y-4)*w+x];
            }
        }
        if(edgeMass<1e-8) return mask.clone();
        // Bound concentration on very large washes, preserving the unused interior mass.
        double moved=Math.min(mass*fraction,edgeMass*12.0);
        for(int i=0;i<out.length;i++)out[i]=(float)(mask[i]*(1-moved/mass)+edge[i]*moved/edgeMass);
        return out;
    }
    private static double smooth(double a,double b,double x) { x=Math.max(0,Math.min(1,(x-a)/(b-a)));return x*x*(3-2*x); }
    private static int channel(float linear) {
        double v=Math.max(0,Math.min(1,linear));
        return (int)Math.round(255*(v<=.0031308?12.92*v:1.055*Math.pow(v,1/2.4)-.055));
    }
}
