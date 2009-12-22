package samples.dependonservice;

import org.osgi.service.log.LogService;

public class DataGenerator {
    private volatile Store m_store;
    private volatile LogService m_log;
    
    public void generate() {
        for (int i = 0; i < 10; i++) {
            m_store.put("#" + i, "value_" + i);
        }
        m_log.log(LogService.LOG_INFO, "Data generated.");
    }
}
