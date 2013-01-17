package org.apache.karaf.shell.commands.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;
import java.util.regex.Pattern;
import jline.Terminal;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.AbstractAction;
import org.apache.karaf.util.StreamUtils;
import org.jledit.ConsoleEditor;
import org.jledit.EditorFactory;

@Command(scope = "shell", name = "edit", description = "Calls a text editor.")
public class EditAction extends AbstractAction {

        private final Pattern URL_PATTERN = Pattern.compile("[^: ]+:[^ ]+");

        @Argument(index = 0, name = "url", description = "The url of the resource to edit.", required = true, multiValued = false)
        private String url;

        private EditorFactory editorFactory;


        @Override
        protected Object doExecute() throws Exception {
            URLConnection connection = null;
            InputStream is = null;
            OutputStream os = null;
            String path = null;
            boolean isLocal = true;
            String sourceUrl = url;

            //If no url format found, assume file url.
            if (!URL_PATTERN.matcher(sourceUrl).matches()) {
                File f = new File(sourceUrl);
                sourceUrl = "file://" + f.getAbsolutePath();
            }

            URL u = new URL(sourceUrl);
            //If its not a file url.
            if (!u.getProtocol().equals("file")) {
                isLocal = false;

                try {
                    connection = u.openConnection();
                    is = connection.getInputStream();
                } catch (IOException ex) {
                    System.out.println("Failed to open " + sourceUrl + " for reading.");
                    return null;
                }
                try {
                    os = connection.getOutputStream();
                } catch (IOException ex) {
                    System.out.println("Failed to open " + sourceUrl + " for writing.");
                    return null;
                }

                //Copy the resource to a tmp location.
                FileOutputStream fos = null;
                try {
                    path = System.getProperty("karaf.data") + "/editor/" + UUID.randomUUID();
                    File f = new File(path);
                    if (!f.exists()) {
                        if (!f.getParentFile().exists()) {
                            f.getParentFile().mkdirs();
                        }
                    }

                    fos = new FileOutputStream(f);
                    copy(is, fos);
                } catch (Exception ex) {
                    System.out.println("Failed to copy resource from url:" + sourceUrl + " to tmp file: " + path + "  for editing.");
                } finally {
                    StreamUtils.close(fos);
                }
            } else {
                path = u.getFile();
            }


            File file = new File(path);
            if (!file.exists()) {
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
            }

            //Call the editor
            ConsoleEditor editor = editorFactory.create(getTerminal());
            editor.setTitle("Karaf");
            editor.open(file, url);
            editor.setOpenEnabled(false);
            editor.start();

            //If resource is not local, copy the resource back.
            if (!isLocal) {
                FileInputStream fis = new FileInputStream(path);
                try {
                    copy(fis, os);
                } finally {
                    StreamUtils.close(fis);
                }
            }

            if (is != null) {
                StreamUtils.close(is);
            }

            if (os != null) {
                StreamUtils.close(os);
            }
            return null;
        }

        /**
         * Gets the {@link jline.Terminal} from the current session.
         *
         * @return
         * @throws Exception
         */
    private Terminal getTerminal() throws Exception {
        Object terminalObject = session.get(".jline.terminal");
        if (terminalObject instanceof Terminal) {
            return (Terminal) terminalObject;

        }
        throw new IllegalStateException("Could not get Terminal from CommandSession.");
    }

    /**
     * Copies the content of {@link InputStream} to {@link OutputStream}.
     *
     * @param input
     * @param output
     * @throws IOException
     */
    private void copy(final InputStream input, final OutputStream output) throws IOException {
        byte[] buffer = new byte[1024 * 16];
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            output.flush();
        }
    }

    public EditorFactory getEditorFactory() {
        return editorFactory;
    }

    public void setEditorFactory(EditorFactory editorFactory) {
        this.editorFactory = editorFactory;
    }
}
