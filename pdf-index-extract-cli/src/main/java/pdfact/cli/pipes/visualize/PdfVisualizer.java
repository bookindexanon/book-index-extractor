package pdfact.cli.pipes.visualize;

import pdfact.cli.model.ExtractionUnit;
import pdfact.cli.util.exception.PdfActVisualizeException;
import pdfact.core.model.Document;
import pdfact.core.model.SemanticRole;

import java.util.Set;

/**
 * The interface for all concrete implementations to visualize the features of a
 * PDF text document.
 *
 * @author Claudius Korzen
 */
public interface PdfVisualizer {
    /**
     * Visualizes all features of the given document to the given stream.
     *
     * @param pdf The PDF document to process.
     * @return the visualization.
     * @throws PdfActVisualizeException If something went wrong while visualizing.
     */
    byte[] visualize(Document pdf) throws PdfActVisualizeException;

    // ==============================================================================================

    /**
     * Returns the units to visualize.
     *
     * @return The units to visualize.
     */
    Set<ExtractionUnit> getExtractionUnits();

    /**
     * Sets the units to visualize.
     *
     * @param units The units to visualize.
     */
    void setExtractiontUnits(Set<ExtractionUnit> units);

    // ==============================================================================================

    /**
     * Returns the semantic roles to include.
     *
     * @return The semantic roles to include.
     */
    Set<SemanticRole> getSemanticRolesToInclude();

    /**
     * Sets the semantic roles to include.
     *
     * @param roles The semantic roles to include.
     */
    void setSemanticRolesToInclude(Set<SemanticRole> roles);
}
