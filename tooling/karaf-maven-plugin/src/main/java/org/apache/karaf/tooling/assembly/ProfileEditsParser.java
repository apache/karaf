package org.apache.karaf.tooling.assembly;

import org.apache.karaf.tools.utils.model.KarafPropertyEdits;
import org.apache.karaf.tools.utils.model.io.stax.KarafPropertyInstructionsModelStaxReader;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;

/**
 * Read and Parse Profile Edits.
 */
class ProfileEditsParser {

    private final KarafPropertyInstructionsModelStaxReader profileEditsReader;

    ProfileEditsParser(
            final KarafPropertyInstructionsModelStaxReader profileEditsReader
                      ) {
        this.profileEditsReader = profileEditsReader;
    }

    Optional<KarafPropertyEdits> parse(final AssemblyMojo mojo) throws FileNotFoundException {
        return Optional.ofNullable(mojo.getPropertyFileEdits())
                       .map(File::new)
                       .filter(File::exists)
                       .flatMap(this::openFile)
                       .flatMap(this::readFile);
    }

    private Optional<FileInputStream> openFile(final File file) {
        try {
            return Optional.of(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<KarafPropertyEdits> readFile(final FileInputStream is) {
        try {
            return Optional.of(profileEditsReader.read(is, true));
        } catch (IOException | XMLStreamException e) {
            return Optional.empty();
        }
    }

}
