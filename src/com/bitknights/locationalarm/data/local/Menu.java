
package com.bitknights.locationalarm.data.local;

import com.bitknights.locationalarm.R;

public class Menu extends AbstractMenu {

    /**
     * 
     */
    private static final long serialVersionUID = -5988403164818767357L;

    public Menu(int position, String title) {
        super(position, title);
    }

    @Override
    public int getTextApperance() {
        return R.style.TextAppearance_Menu_Text;
    }

}
