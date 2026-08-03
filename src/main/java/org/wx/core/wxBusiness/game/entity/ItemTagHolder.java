package org.wx.core.wxBusiness.game.entity;

import java.util.ArrayList;
import java.util.List;

public interface ItemTagHolder {

    List<String> getTags();

    void setTags(List<String> tags);

    default void setTagsFromItem(GameItem item) {
        ItemTagHelper.fillTags(this, item);
    }
}
