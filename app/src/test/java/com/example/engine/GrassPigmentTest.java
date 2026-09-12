package com.example.engine;

import org.junit.Test;
import static org.junit.Assert.*;

public class GrassPigmentTest {
    @Test public void dryingMovesPigmentToItsOwnFrontWithoutCreatingMassOrPaintingDryPaper() {
        int w=51,h=39;float[] mask=new float[w*h];
        for(int y=5;y<34;y++)for(int x=6;x<45;x++)mask[y*w+x]=1;
        // An internal paper reserve has its own drying front.
        for(int y=15;y<22;y++)for(int x=20;x<27;x++)mask[y*w+x]=0;
        float[] dried=NorthstarGrassPaint.dry(mask,w,h,.25);
        double before=0,after=0;
        for(int i=0;i<mask.length;i++) {
            before+=mask[i];after+=dried[i];
            assertTrue(Float.isFinite(dried[i])&&dried[i]>=0);
            if(mask[i]==0)assertEquals(0,dried[i],0);
        }
        assertEquals(before,after,.001);
        assertTrue(dried[10*w+6]>dried[10*w+12]);
        assertTrue(dried[18*w+19]>dried[18*w+12]);
    }
    @Test public void croppingAWetWashDoesNotInventARimAtTheImageBoundary() {
        float[] wet=new float[13*17];java.util.Arrays.fill(wet,.7f);
        assertArrayEquals(wet,NorthstarGrassPaint.dry(wet,13,17,.3),.00001f);
        float[] empty=new float[7];
        assertArrayEquals(empty,NorthstarGrassPaint.dry(empty,1,7,.3),0);
    }
    @Test public void narrowFractionalCoverageConservesPigmentAndZeroDryingIsIdentity() {
        float[] wet=new float[81];
        for(int i=5;i<76;i++)wet[i]=(float)Math.sin((i-5)*Math.PI/71);
        assertArrayEquals(wet,NorthstarGrassPaint.dry(wet,1,81,0),0);
        float[] dried=NorthstarGrassPaint.dry(wet,1,81,.3);
        double before=0,after=0;
        for(int i=0;i<wet.length;i++){before+=wet[i];after+=dried[i];assertTrue(dried[i]>=0);}
        assertEquals(before,after,.0001);
    }
}
