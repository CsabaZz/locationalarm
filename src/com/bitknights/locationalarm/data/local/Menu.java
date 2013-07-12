package com.bitknights.locationalarm.data.local;

public class Menu extends BaseEntity {

    /**
     * 
     */
    private static final long serialVersionUID = -5988403164818767357L;
    
    private int mPosition;
    private String mTitle;
    
    public Menu(int position, String title) {
	this.mPosition = position;
	this.mTitle = title;
    }

    public int getPosition() {
	return mPosition;
    }
    
    public void setPosition(int position) {
	this.mPosition = position;
    }
    
    public String getTitle() {
	return mTitle;
    }
    
    public void setTitle(String title) {
	this.mTitle = title;
    }

}
