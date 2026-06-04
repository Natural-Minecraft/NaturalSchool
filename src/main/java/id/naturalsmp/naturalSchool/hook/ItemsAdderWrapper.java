package id.naturalsmp.naturalSchool.hook;

public interface ItemsAdderWrapper {
    /**
     * Replaces inline ItemsAdder image/icon placeholders (e.g. :ia_owner_icon:)
     * with their respective unicode characters.
     *
     * @param text the raw text containing placeholders
     * @return the processed text with unicode font images
     */
    String replace(String text);
}
