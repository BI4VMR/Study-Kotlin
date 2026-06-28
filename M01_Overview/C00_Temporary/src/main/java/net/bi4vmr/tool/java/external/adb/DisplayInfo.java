package net.bi4vmr.tool.java.external.adb;

/**
 * 显示屏信息。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
public class DisplayInfo {

    private final int id;

    private String physicalID;

    private int width = -1;

    private int height = -1;


    public DisplayInfo(int id) {
        this.id = id;
    }


    public int getID() {
        return id;
    }

    public String getPhysicalID() {
        return physicalID;
    }

    public void setPhysicalID(String physicalID) {
        this.physicalID = physicalID;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public String toString() {
        return "DisplayInfo{" +
                "id=" + id +
                ", physicalID=" + physicalID +
                ", width=" + width +
                ", height=" + height +
                '}';
    }
}
