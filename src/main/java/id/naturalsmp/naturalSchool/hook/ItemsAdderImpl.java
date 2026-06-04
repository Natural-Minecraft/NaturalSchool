package id.naturalsmp.naturalSchool.hook;

import dev.lone.itemsadder.api.FontImages.FontImageWrapper;

public class ItemsAdderImpl implements ItemsAdderWrapper {
    @Override
    public String replace(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        try {
            return FontImageWrapper.replaceFontImages(null, text);
        } catch (Throwable t) {
            // Return raw text if there is any issue
            return text;
        }
    }
}
