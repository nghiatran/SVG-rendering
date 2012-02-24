package com.svg.test;

import java.util.ArrayList;

/**
 * Minimum Polygon class for Android.
 */
public class Polygon
{

    ArrayList<Float> polyX;
    ArrayList<Float> polyY;

    /**
     * Default constructor.
     * @param px Polygon y coods.
     * @param py Polygon x coods.
     * @param ps Polygon sides count.
     */
    public Polygon( ArrayList<Float> px, ArrayList<Float> py )
    {
        polyX = px;
        polyY = py;
    }

    /**
     * Checks if the Polygon contains a point.
     * @see "http://alienryderflex.com/polygon/"
     * @param x Point horizontal pos.
     * @param y Point vertical pos.
     * @return Point is in Poly flag.
     */
    public boolean contains( float x, float y )
    {
    	int polySides = polyX.size();
        boolean c = false;
        int i, j = 0;
        for (i = 0, j = polySides - 1; i < polySides; j = i++) {
            if (((polyY.get(i) > y) != (polyY.get(j) > y))
                && (x < (polyX.get(j) - polyX.get(i)) * (y - polyY.get(i)) / (polyY.get(j) - polyY.get(i)) + polyX.get(i)))
            c = !c;
        }
        return c;
    }  
}