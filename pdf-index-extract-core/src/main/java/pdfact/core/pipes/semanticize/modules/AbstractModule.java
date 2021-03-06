package pdfact.core.pipes.semanticize.modules;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pdfact.core.model.Document;
import pdfact.core.model.Page;
import pdfact.core.model.SemanticRole;
import pdfact.core.model.TextBlock;

import java.util.List;

/**
 * A module that identifies the text blocks with the semantic role "abstract".
 *
 * @author Claudius Korzen
 */
public class AbstractModule implements PdfTextSemanticizerModule {
    /**
     * The logger.
     */
    protected static Logger log = LogManager.getFormatterLogger("role-detection");

    /**
     * A boolean flag that indicates whether the current text block is a member of
     * the abstract or not.
     */
    protected boolean isAbstract = false;

    @Override
    public void semanticize(Document pdf) {
        log.debug("=====================================================");
        log.debug("Detecting text blocks of semantic role '%s' ...", SemanticRole.ABSTRACT);
        log.debug("=====================================================");

        if (pdf == null) {
            return;
        }

        List<Page> pages = pdf.getPages();
        if (pages == null) {
            return;
        }

        for (Page page : pages) {
            if (page == null) {
                continue;
            }

            for (TextBlock block : page.getTextBlocks()) {
                if (block == null) {
                    continue;
                }

                SemanticRole role = block.getSemanticRole();
                SemanticRole secondaryRole = block.getSecondarySemanticRole();

                // Check if the current block is a section heading (which would
                // denote the end of the abstract).
                if (this.isAbstract && role == SemanticRole.HEADING) {
                    this.isAbstract = false;
                }

                if (this.isAbstract) {
                    log.debug("-----------------------------------------------------");
                    log.debug("Text block: \"%s\" ...", block.getText());
                    log.debug("... page:          %d", block.getPosition().getPageNumber());
                    log.debug("... assigned role: %s", SemanticRole.ABSTRACT);
                    log.debug("... role reason:   the block is located between the detected "
                            + "start/end of the Abstract section");
                    block.setSemanticRole(SemanticRole.ABSTRACT);
                }

                // Check if the current block is the heading of the abstract (which
                // would denote the start of the abstract).
                if (role == SemanticRole.HEADING && secondaryRole == SemanticRole.ABSTRACT) {
                    this.isAbstract = true;
                }
            }
        }
    }
}
