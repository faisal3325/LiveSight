package kludge.com.livesight;

/**
 * Created by Faisal on 21-Jan-17.
 */

public class ARRadarDot {

    private boolean dimmed;
    private double translateX;
    private double translateY;

    public ARRadarDot(double translateX, double translateY,boolean dimmed) {
        super();
        this.dimmed = dimmed;
        this.translateX = translateX;
        this.translateY = translateY;
    }

    public boolean getDimmed() {
        return dimmed;
    }

    public void setDimmed(boolean dimmed) {
        this.dimmed = dimmed;
    }

    public double getTranslateX() {
        return translateX;
    }

    public void setTranslateX(double translateX) {
        this.translateX = translateX;
    }

    public double getTranslateY() {
        return translateY;
    }

    public void setTranslateY(double translateY) {
        this.translateY = translateY;
    }
}