package com.example.mememanager;

public class ImageTextData {

    String containsText;
    String imgPath;

    public ImageTextData() {

    }

    public String getImgPath() {
        return imgPath;
    }

    public void setImgPath(String imgPath) {
        this.imgPath = imgPath;
    }

    public String getContainsText() {
        return containsText;
    }

    public void setContainsText(String containsText) {
        this.containsText = containsText;
    }

    public ImageTextData(String imgPath, String containsText) {
        this.imgPath = imgPath;
        this.containsText = containsText;
    }
}
