package samples.trackingserviceswithcallbacks;

import javax.swing.text.Document;

public interface Translator {
    public boolean canTranslate(String from, String to);
    public Document translate(Document document, String from, String to);
}
