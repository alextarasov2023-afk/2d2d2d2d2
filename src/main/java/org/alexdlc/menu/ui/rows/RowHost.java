package org.alexdlc.menu.ui.rows;

public interface RowHost {

    void closeOtherRows(SettingRow except);

    int popupViewportMaxY();
}
