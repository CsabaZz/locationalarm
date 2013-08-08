
package com.bitknights.locationalarm.data.local;

public abstract class AbstractMenu extends BaseEntity {

    /**
     * 
     */
    private static final long serialVersionUID = 8061204392541571548L;

    private int mId;
    private String mTitle;

    public abstract int getTextApperance();

    public AbstractMenu(int id, String title) {
        this.mId = id;
        this.mTitle = title;
    }

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        this.mId = id;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        this.mTitle = title;
    }

}
