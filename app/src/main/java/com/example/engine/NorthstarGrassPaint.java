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
        float[] rectangle=new float[width*height];
        Arrays.fill(rectangle,1f);
        return renderShape(width,height,seed,rectangle,observer);
    }

    /** Exact silhouette coverage is prepared by the Android path rasterizer.
     * Geometry guides media placement; it never changes the authoritative outline.
     */
    public static int[] renderShape(int width, int height, long seed, float[] coverage, Observer observer) {
        if(width<1 || height<1 || width>1024 || height>1024 || coverage.length!=width*height)
            throw new IllegalArgumentException("Invalid grass silhouette");
        for(float v:coverage)if(!Float.isFinite(v) || v<0 || v>1)
            throw new IllegalArgumentException("Invalid silhouette coverage");
        Studio s=new Studio(width,height,seed);
        s.paintShape(coverage,observer);
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
        void paintShape(float[] coverage,Observer observer) {
            float[] distance=insideDistance(coverage,w,h);
            float[] reach=localReach(coverage,w,h);
            snapshot(observer,"01-base-wash");
            underpainting=new float[][]{color[0].clone(),color[1].clone(),color[2].clone()};
            // Successive irregular inset fronts follow every contour, including
            // holes. Narrow arms get narrower washes instead of giant clipped blobs.
            float[] body=new float[w*h];
            for(int y=0;y<h;y++)for(int x=0;x<w;x++)body[y*w+x]=(float)(coverage[y*w+x]*
                    (.18+.55*noise(x*.010,y*.010,701)));
            shapeDeposit(body,.16,.08,OLIVE,702);
            float[] broad=boundaryWash(coverage,distance,reach,3.8,711);
            shapeDeposit(broad,.14,.10,OLIVE,712);
            snapshot(observer,"02-midtone");
            float[] middle=boundaryWash(coverage,distance,reach,1.35,721);
            shapeDeposit(middle,.20,.22,OLIVE,722);
            float[] margin=boundaryWash(coverage,distance,reach,.42,731);
            shapeDeposit(margin,.15,.22,GREEN,732);
            snapshot(observer,"03-drying-fronts");
            textureField=broad;
            for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
                int i=y*w+x;
                if(coverage[i]<=0)continue;
                double reserve=smooth(.57,.76,noise(x*.028+noise(x*.009,y*.009,741),
                        y*.028+noise(x*.009,y*.009,742),743));
                // Lift channels within the edge washes, retaining their prior ground.
                double lifting=reserve*broad[i]*.58;
                for(int c=0;c<3;c++)color[c][i]+=(underpainting[c][i]-color[c][i])*lifting;
                textureField[i]=(float)Math.min(1,broad[i]*(1-.75*reserve)+body[i]*.25);
            }
            shapeDeposit(textureField,.09,.18,OLIVE,744);
            snapshot(observer,"04-lifted-texture");
            for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
                int i=y*w+x;
                if(coverage[i]<=0)continue;
                double gx=x*.095+1.2*noise(x*.022,y*.022,751);
                double gy=y*.095+1.2*noise(x*.022,y*.022,752);
                double granule=smooth(.57,.80,noise(gx,gy,753));
                double group=.25+.75*noise(x*.018,y*.018,754);
                double edge=Math.exp(-distance[i]/Math.max(1.3,reach[i]*.08));
                double tooth=.3+1.15*paper[i];
                apply(i,coverage[i]*(textureField[i]*granule*.40+edge*.20)*group*tooth,GREEN);
                apply(i,coverage[i]*(textureField[i]*granule*granule*.24+edge*.12)*group*tooth,INK);
            }
            // Explicit minute marks inherit edge-wash density. Interior lawn is quiet.
            int count=(int)(w*h/650.0);
            for(int j=0;j<count;j++) {
                double x=between(0,w),y=between(0,h);
                int i=Math.min(h-1,(int)y)*w+Math.min(w-1,(int)x);
                if(coverage[i]<.9 || random.nextDouble()>textureField[i]*.35)continue;
                if(j%5==0)blade(x,y,between(3,7),between(.5,1.1),GREEN,between(.3,.8));
                else wash(x,y,between(.7,2.5),j%4==0?INK:GREEN,between(.25,.8),.10,6,1,false);
            }
            snapshot(observer,"05-final-grass");
        }

        float[] boundaryWash(float[] coverage,float[] distance,float[] reach,double width,int salt) {
            float[] mask=new float[w*h];
            for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
                int i=y*w+x;
                if(coverage[i]<=0)continue;
                double n=noise(x*.016,y*.016,salt);
                double lobes=noise(x*.055+1.4*n,y*.055+noise(x*.017,y*.017,salt+1),salt+2);
                double front=reach[i]*width*(.62+.65*n+.32*(lobes-.5));
                double wet=1-smooth(front-1.2,front+1.2,distance[i]);
                mask[i]=(float)(coverage[i]*wet*(.48+.52*noise(x*.012,y*.012,salt+3)));
            }
            return mask;
        }
        void shapeDeposit(float[] mask,double load,double drying,Pigment pigment,int salt) {
            float[] front=new float[w*h];
            for(int y=0;y<h;y++)for(int x=0;x<w;x++)
                front[y*w+x]=(float)smooth(.27,.76,noise(x*.024,y*.024,salt));
            float[] density=dry(mask,w,h,drying,front);
            for(int i=0;i<density.length;i++)apply(i,load*density[i]*(.5+paper[i]),pigment);
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

    /** Eight-neighbour chamfer distance in paint pixels. The image exterior
     * is dry too, so a rectangle touching the raster bounds keeps its boundary.
     */
    static float[] insideDistance(float[] mask,int w,int h) {
        float[] d=new float[w*h];
        for(int y=0;y<h;y++)for(int x=0;x<w;x++)d[y*w+x]=mask[y*w+x]<.5?0:
                (x==0 || y==0 || x==w-1 || y==h-1?.5f:2048f);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
            int i=y*w+x;
            if(x>0)d[i]=Math.min(d[i],d[i-1]+1);
            if(y>0){d[i]=Math.min(d[i],d[i-w]+1);
                if(x>0)d[i]=Math.min(d[i],d[i-w-1]+1.414214f);
                if(x+1<w)d[i]=Math.min(d[i],d[i-w+1]+1.414214f);}
        }
        for(int y=h-1;y>=0;y--)for(int x=w-1;x>=0;x--) {
            int i=y*w+x;
            if(x+1<w)d[i]=Math.min(d[i],d[i+1]+1);
            if(y+1<h){d[i]=Math.min(d[i],d[i+w]+1);
                if(x>0)d[i]=Math.min(d[i],d[i+w-1]+1.414214f);
                if(x+1<w)d[i]=Math.min(d[i],d[i+w+1]+1.414214f);}
        }
        return d;
    }
    /** Local cross-section limits the inset width in thin arms and corridors. */
    static float[] localReach(float[] mask,int w,int h) {
        float[] span=new float[w*h];
        for(int y=0;y<h;y++)for(int x=0;x<w;) {
            if(mask[y*w+x]<.5){x++;continue;}
            int start=x;while(x<w && mask[y*w+x]>=.5)x++;
            for(int k=start;k<x;k++)span[y*w+k]=x-start;
        }
        for(int x=0;x<w;x++)for(int y=0;y<h;) {
            if(mask[y*w+x]<.5){y++;continue;}
            int start=y;while(y<h && mask[y*w+x]>=.5)y++;
            for(int k=start;k<y;k++)span[k*w+x]=(float)Math.max(3,Math.min(48,Math.min(span[k*w+x],y-start)*.13));
        }
        // Cross-sections change at re-entrant corners. Smooth inside the
        // silhouette so those changes never become straight paint seams.
        float[] inside=new float[mask.length];
        for(int i=0;i<inside.length;i++)inside[i]=mask[i]>=.5?1:0;
        float[] weights=boxBlur(inside,w,h,16), smoothed=boxBlur(span,w,h,16);
        for(int i=0;i<span.length;i++)if(weights[i]>0)span[i]=smoothed[i]/weights[i];
        return span;
    }
    private static float[] boxBlur(float[] field,int w,int h,int radius) {
        float[] tmp=new float[field.length],out=new float[field.length];
        int size=radius*2+1;
        for(int y=0;y<h;y++) {
            double sum=0;
            for(int k=-radius;k<=radius;k++)sum+=field[y*w+Math.max(0,Math.min(w-1,k))];
            for(int x=0;x<w;x++) { tmp[y*w+x]=(float)(sum/size);
                sum+=field[y*w+Math.min(w-1,x+radius+1)]-field[y*w+Math.max(0,x-radius)]; }
        }
        for(int x=0;x<w;x++) {
            double sum=0;
            for(int k=-radius;k<=radius;k++)sum+=tmp[Math.max(0,Math.min(h-1,k))*w+x];
            for(int y=0;y<h;y++) { out[y*w+x]=(float)(sum/size);
                sum+=tmp[Math.min(h-1,y+radius+1)*w+x]-tmp[Math.max(0,y-radius)*w+x]; }
        }
        return out;
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
