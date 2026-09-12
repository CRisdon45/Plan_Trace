package com.example.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Deterministic grass pigment, shared by Android and the desktop inspection tool.
 * Independent implementation of recursive polygon glazes (Hobbs, 2017), with
 * finite-volume edge redistribution and Kubelka-Munk layering (Curtis et al., 1997).
 * This is an artistic drying approximation, not a shallow-water fluid solver.
 * Coordinates and the paper field belong to the object, never to the viewport.
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
                double thickness=.28+.15*noise(x*.008,y*.008,81)+.05*(paper[i]-.5);
                apply(i,thickness*(.45+1.10*paper[i]),YELLOW);
                apply(i,(.10+.10*noise(x*.014,y*.014,82))*(.45+1.10*paper[i]),OLIVE);
            }
        }
        double between(double a,double b) { return a+(b-a)*random.nextDouble(); }
        void snapshot(Observer o,String name) { if(o!=null)o.stage(name,w,h,pixels()); }
        void paint(Observer o) {
            // Large related glazes establish connected masses, leaving open yellow paper.
            for (int j=0;j<15;j++) wash(between(-w*.1,w*1.1),between(-h*.1,h*1.1),
                    between(120,280),j%3==0?YELLOW:OLIVE,between(.055,.13),.0,32,1,false);
            snapshot(o,"01-base-wash");
            underpainting=new float[][]{color[0].clone(),color[1].clone(),color[2].clone()};
            for (int j=0;j<24;j++) wash(between(0,w),between(0,h),between(35,105),
                    j%4==0?YELLOW:OLIVE,between(.10,.23),.18,32,2,true);
            snapshot(o,"02-midtone");
            for (int j=0;j<14;j++) wash(between(w*.50,w*1.05),between(-h*.10,h*1.1),
                    between(40,115),j%3==0?GREEN:OLIVE,between(.20,.46),.30,32,2,true);
            snapshot(o,"03-drying-fronts");
            // Smaller deposits follow the same wet-front construction, with much less load.
            for (int j=0;j<340;j++) {
                double x=between(0,w),y=between(0,h);
                double density=noise(x*.008,y*.008,40);
                if (density<.33) continue;
                wash(x,y,between(3,23),j%5==0?EARTH:OLIVE,between(.04,.21),.12,12,1,true);
            }
            snapshot(o,"04-granulation");
            for (int j=0;j<6500;j++) {
                double x=between(0,w),y=between(0,h);
                double density=noise(x*.012,y*.012,45);
                double side=.55+.65*x/w;
                if (random.nextDouble()>Math.max(0,(density-.25)*side)) continue;
                wash(x,y,between(.65,3.8),j%7==0?null:j%5==0?INK:GREEN,between(.38,1.6),.06,4,1,false);
            }
            snapshot(o,"05-final-grass");
        }

        List<Vertex> contour(double x,double y,double radius) {
            List<Vertex> v=new ArrayList<>();
            double stretch=radius<5?between(.5,1.65):between(.72,1.35), phase=between(0,Math.PI*2);
            double rotation=between(0,Math.PI*2);
            int lobes=20;
            double[] angle=new double[lobes], reach=new double[lobes], size=new double[lobes];
            for(int j=0;j<lobes;j++) {
                angle[j]=j*Math.PI*2/lobes+between(-.10,.10);
                reach[j]=radius*(.78+.17*Math.sin(3*angle[j]+phase));
                size[j]=radius*between(.12,.28);
            }
            int points=radius>10?192:32;
            for(int j=0;j<points;j++) {
                double a=j*Math.PI*2/points;
                double r=radius*(.79+.16*Math.sin(3*a+phase));
                for(int k=0;k<lobes;k++) {
                    double delta=a-angle[k], perp=reach[k]*Math.sin(delta);
                    if(Math.cos(delta)>0 && Math.abs(perp)<size[k])
                        r=Math.max(r,reach[k]*Math.cos(delta)+Math.sqrt(size[k]*size[k]-perp*perp));
                }
                double variance=.04+.14*(.5+.5*Math.sin(a*2+phase));
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
            List<Vertex> base=deform(contour(cx,cy,radius),refinements);
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
                double grain=.35+1.30*paper[i];
                // Pooling in paper hollows is shared by glazes, not a final noise filter.
                grain*=.50+1.00*noise((x+left)*.25,(y+top)*.25,64);
                grain*=.80+.40*noise((x+left)*1.5,(y+top)*1.5,65);
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
