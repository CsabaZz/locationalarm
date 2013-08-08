
package com.bitknights.locationalarm.data.local;

import com.bitknights.locationalarm.R;

public class MenuTitle extends AbstractMenu {

    /**
     * 
     */
    private static final long serialVersionUID = 7188147396765052527L;

    public MenuTitle(int id, String title) {
        super(id, title);
    }

    @Override
    public int getTextApperance() {
        return R.style.TextAppearance_Menu_Title;
    }

}
