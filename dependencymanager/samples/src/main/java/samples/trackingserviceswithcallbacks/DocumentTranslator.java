package samples.trackingserviceswithcallbacks;

import java.util.ArrayList;
import java.util.List;

import javax.swing.text.Document;

public class DocumentTranslator {
    private List<Translator> m_translators = new ArrayList<Translator>();
    
    public void added(Translator translator) {
        m_translators.add(translator);
    }
    
    public void removed(Translator translator) {
        m_translators.remove(translator);
    }
    
    public Document translate(Document document, String from, String to) {
        for (Translator translator : m_translators) {
            if (translator.canTranslate(from, to)) {
                return translator.translate(document, from, to);
            }
        }
        return null;
    }
}
