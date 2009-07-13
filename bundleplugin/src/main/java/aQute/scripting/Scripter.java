package aQute.service.scripting;

import java.io.Reader;
import java.util.Map;

public abstract interface Scripter
{
  public static final String MIME_TYPE = "mime.type";

  public abstract Object eval(Map<String, Object> paramMap, Reader paramReader)
    throws Exception;
}
