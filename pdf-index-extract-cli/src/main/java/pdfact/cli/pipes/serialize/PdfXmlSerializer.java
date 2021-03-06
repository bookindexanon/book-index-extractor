package pdfact.cli.pipes.serialize;

import org.apache.commons.lang3.StringEscapeUtils;
import pdfact.cli.model.ExtractionUnit;
import pdfact.cli.util.exception.PdfActSerializeException;
import pdfact.core.model.Character;
import pdfact.core.model.*;
import pdfact.core.util.PdfActUtils;

import java.util.*;

import static pdfact.cli.pipes.serialize.PdfSerializerConstants.*;
import static pdfact.core.PdfActCoreSettings.DEFAULT_ENCODING;

/**
 * An implementation of {@link PdfXmlSerializer} that serializes a PDF document
 * in XML format.
 *
 * @author Claudius Korzen
 */
public class PdfXmlSerializer implements PdfSerializer {
    /**
     * The indentation length.
     */
    protected static final int INDENT_LENGTH = 2;

    /**
     * The line delimiter to use on joining the individual lines.
     */
    protected static final String LINE_DELIMITER = System.lineSeparator();

    // ==============================================================================================

    /**
     * The units to serialize.
     */
    protected Set<ExtractionUnit> extractionUnits;

    /**
     * The semantic roles to consider on serializing.
     */
    protected Set<SemanticRole> semanticRolesToInclude;

    /**
     * The fonts of the PDF elements which were in fact serialized.
     */
    protected Set<Font> usedFonts;

    /**
     * The colors of the PDF elements which were in fact serialized.
     */
    protected Set<Color> usedColors;

    // ==============================================================================================
    // Constructors.

    /**
     * Creates a new serializer that serializes a PDF document in XML format.
     */
    public PdfXmlSerializer() {
        this.usedFonts = new HashSet<>();
        this.usedColors = new HashSet<>();
    }

    /**
     * Creates a new serializer that serializes a PDF document in XML format.
     *
     * @param units The text unit.
     * @param roles The semantic roles to include.
     */
    public PdfXmlSerializer(Set<ExtractionUnit> units, Set<SemanticRole> roles) {
        this();
        this.extractionUnits = units;
        this.semanticRolesToInclude = roles;
    }

    // ==============================================================================================

    @Override
    public byte[] serialize(Document pdf) throws PdfActSerializeException {
        // The current indentation level.
        int level = 0;
        // The serialization to return.
        String result = "";

        if (pdf != null) {
            // The individual lines of the serialization.
            List<String> resultLines = new ArrayList<>();

            // Start the XML document.
            resultLines.add(start(DOCUMENT, level));

            // Create the section that contains all serialized PDF elements.
            List<String> elementsLines = serializePdfElements(level + 1, pdf);
            if (elementsLines != null && !elementsLines.isEmpty()) {
                resultLines.addAll(elementsLines);
            }

            // Create the section that contains the used fonts.
            List<String> fontsLines = serializeFonts(level + 2, this.usedFonts);
            if (fontsLines != null && !fontsLines.isEmpty()) {
                resultLines.add(start(FONTS, level + 1));
                resultLines.addAll(fontsLines);
                resultLines.add(end(FONTS, level + 1));
            }

            // Create the section that contains the used colors.
            List<String> colorsLines = serializeColors(level + 2, this.usedColors);
            if (colorsLines != null && !colorsLines.isEmpty()) {
                resultLines.add(start(COLORS, level + 1));
                resultLines.addAll(colorsLines);
                resultLines.add(end(COLORS, level + 1));
            }

            if (this.extractionUnits.contains(ExtractionUnit.PAGE)) {
                // Create the section that contains information about the pages.
                List<String> pagesLines = serializePages(level + 2, pdf.getPages());
                if (pagesLines != null && !pagesLines.isEmpty()) {
                    resultLines.add(start(PAGES, level + 1));
                    resultLines.addAll(pagesLines);
                    resultLines.add(end(PAGES, level + 1));
                }
            }

            // End the XML document.
            resultLines.add(end(DOCUMENT, level));

            // Join the lines to a single string.
            result = PdfActUtils.join(resultLines, LINE_DELIMITER);
        }

        return result.getBytes(DEFAULT_ENCODING);
    }

    // ==============================================================================================

    /**
     * Serializes the elements of the given PDF document.
     *
     * @param level The current indentation level.
     * @param pdf   The PDF document to process.
     * @return A list of strings that represent the serialized elements.
     */
    protected List<String> serializePdfElements(int level, Document pdf) {
        List<String> lines = new ArrayList<>();
        for (ExtractionUnit unit : this.extractionUnits) {
            switch (unit) {
                case CHARACTER:
                    lines.addAll(serializeCharacters(level, pdf));
                    break;
                case WORD:
                    lines.addAll(serializeWords(level, pdf));
                    break;
                case PARAGRAPH:
                    lines.addAll(serializeParagraphs(level, pdf));
                    break;
                case FIGURE:
                    lines.addAll(serializeFigures(level, pdf));
                    break;
                case SHAPE:
                    lines.addAll(serializeShapes(level, pdf));
                    break;
                default:
                    break;
            }
        }
        return lines;
    }

    // ==============================================================================================

    /**
     * Serializes the paragraphs of the given PDF document.
     *
     * @param level The current indentation level.
     * @param pdf   The PDF document to process.
     * @return A list of strings that represent the lines of the serialization.
     */
    protected List<String> serializeParagraphs(int level, Document pdf) {
        List<String> result = new ArrayList<>();

        if (pdf != null) {
            result.add(start(PARAGRAPHS, level));
            for (Paragraph paragraph : pdf.getParagraphs()) {
                // Ignore the paragraph if its role should not be extracted.
                if (!hasRelevantRole(paragraph)) {
                    continue;
                }

                List<String> paragraphLines = serializeParagraph(level + 1, paragraph);
                if (paragraphLines != null) {
                    result.addAll(paragraphLines);
                }
            }
            result.add(end(PARAGRAPHS, level));
        }

        return result;
    }

    /**
     * Serializes the given paragraph.
     *
     * @param level     The current indentation level.
     * @param paragraph The paragraph to serialize.
     * @return A list of strings that represent the lines of the serialization.
     */
    protected List<String> serializeParagraph(int level, Paragraph paragraph) {
        List<String> result = new ArrayList<>();
        List<String> paragraphLines = serializePdfElement(level + 1, paragraph);
        // Wrap the serialized lines with a tag that describes a paragraph.
        if (paragraphLines != null && !paragraphLines.isEmpty()) {
            result.add(start(PARAGRAPH, level));
            result.addAll(paragraphLines);
            result.add(end(PARAGRAPH, level));
        }
        return result;
    }

    // ==============================================================================================

    /**
     * Serializes the words of the given PDF document.
     *
     * @param level The current indentation level.
     * @param pdf   The PDF document to process.
     * @return A list of strings that represent the lines of the serialization.
     */
    protected List<String> serializeWords(int level, Document pdf) {
        List<String> result = new ArrayList<>();

        if (pdf != null) {
            result.add(start(WORDS, level));
            for (Paragraph paragraph : pdf.getParagraphs()) {
                // Ignore the paragraph if its role should not be extracted.
                if (!hasRelevantRole(paragraph)) {
                    continue;
                }

                for (Word word : paragraph.getWords()) {
                    List<String> wordLines = serializeWord(level + 1, word);
                    if (wordLines != null) {
                        result.addAll(wordLines);
                    }
                }
            }
            result.add(end(WORDS, level));
        }

        return result;
    }

    /**
     * Serializes the given word.
     *
     * @param level The current indentation level.
     * @param word  The word to serialize.
     * @return A list of strings that represent the lines of the serialization.
     */
    protected List<String> serializeWord(int level, Word word) {
        List<String> result = new ArrayList<>();
        List<String> wordLines = serializePdfElement(level + 1, word);
        // Wrap the serialized lines with a tag that describes a word.
        if (wordLines != null && !wordLines.isEmpty()) {
            result.add(start(WORD, level));
            result.addAll(wordLines);
            result.add(end(WORD, level));
        }
        return result;
    }

    // ==============================================================================================

    /**
     * Serializes the characters of the given PDF document.
     *
     * @param level The current indentation level.
     * @param pdf   The PDF document to process.
     * @return A list of strings that represent the lines of the serialization.
     */
    protected List<String> serializeCharacters(int level, Document pdf) {
        List<String> result = new ArrayList<>();

        if (pdf != null) {
            result.add(start(CHARACTERS, level));
            for (Paragraph paragraph : pdf.getParagraphs()) {
                // Ignore the paragraph if its role should not be extracted.
                if (!hasRelevantRole(paragraph)) {
                    continue;
                }

                for (Word word : paragraph.getWords()) {
                    for (Character character : word.getCharacters()) {
                        List<String> characterLines = serializeCharacter(level + 1, character);
                        if (characterLines != null) {
                            result.addAll(characterLines);
                        }
                    }
                }
            }
            result.add(end(CHARACTERS, level));
        }

        return result;
    }

    /**
     * Serializes the given character.
     *
     * @param level     The current indentation level.
     * @param character The character to serialize.
     * @return A list of strings that represent the lines of the serialization.
     */
    protected List<String> serializeCharacter(int level, Character character) {
        List<String> result = new ArrayList<>();
        List<String> characterLines = serializePdfElement(level + 1, character);
        // Wrap the serialized lines with a tag that describes a character.
        if (characterLines != null && !characterLines.isEmpty()) {
            result.add(start(CHARACTER, level));
            result.addAll(characterLines);
            result.add(end(CHARACTER, level));
        }
        return result;
    }

    // ==============================================================================================

    /**
     * Serializes the figures of the given PDF document.
     *
     * @param level The current indentation level.
     * @param pdf   The PDF document to process.
     * @return A list of strings that represent the lines of the serialization.
     */
    protected List<String> serializeFigures(int level, Document pdf) {
        List<String> result = new ArrayList<>();

        if (pdf != null) {
            result.add(start(FIGURES, level));
            for (Page page : pdf.getPages()) {
                for (Figure figure : page.getFigures()) {
                    List<String> figureLines = serializeFigure(level + 1, figure);
                    if (figureLines != null) {
                        result.addAll(figureLines);
                    }
                }
            }
            result.add(end(FIGURES, level));
        }

        return result;
    }

    /**
     * Serializes the given figure.
     *
     * @param level  The current indentation level.
     * @param figure The figure to serialize.
     * @return A list of strings that represent the lines of the serialization.
     */
    protected List<String> serializeFigure(int level, Figure figure) {
        List<String> result = new ArrayList<>();
        List<String> figureLines = serializePdfElement(level + 1, figure);
        // Wrap the serialized lines with a tag that describes a figure.
        if (figureLines != null && !figureLines.isEmpty()) {
            result.add(start(FIGURE, level));
            result.addAll(figureLines);
            result.add(end(FIGURE, level));
        }
        return result;
    }

    // ==============================================================================================

    /**
     * Serializes the shapes of the given PDF document.
     *
     * @param level The current indentation level.
     * @param pdf   The PDF document to process.
     * @return A list of strings that represent the lines of the serialization.
     */
    protected List<String> serializeShapes(int level, Document pdf) {
        List<String> result = new ArrayList<>();

        if (pdf != null) {
            result.add(start(SHAPES, level));
            for (Page page : pdf.getPages()) {
                for (Shape shape : page.getShapes()) {
                    List<String> shapeLines = serializeShape(level + 1, shape);
                    if (shapeLines != null) {
                        result.addAll(shapeLines);
                    }
                }
            }
            result.add(end(SHAPES, level));
        }

        return result;
    }

    /**
     * Serializes the given shape.
     *
     * @param level The current indentation level.
     * @param shape The shape to serialize.
     * @return A list of strings that represent the lines of the serialization.
     */
    protected List<String> serializeShape(int level, Shape shape) {
        List<String> result = new ArrayList<>();
        List<String> shapeLines = serializePdfElement(level + 1, shape);
        // Wrap the serialized lines with a tag that describes a shape.
        if (shapeLines != null && !shapeLines.isEmpty()) {
            result.add(start(SHAPE, level));
            result.addAll(shapeLines);
            result.add(end(SHAPE, level));
        }
        return result;
    }

    // ==============================================================================================

    /**
     * Serializes the given PDF element.
     *
     * @param level   The current indentation level.
     * @param element The PDF element to serialize.
     * @return A list of strings that represent the lines of the serialization.
     */
    protected List<String> serializePdfElement(int level, Element element) {
        List<String> result = new ArrayList<>();

        if (element != null) {
            // Serialize the list of positions of the element, if there is any.
            if (element instanceof HasPositions) {
                HasPositions hasPositions = (HasPositions) element;
                List<Position> positions = hasPositions.getPositions();

                List<String> serialized = serializePositions(level + 1, positions);
                if (serialized != null && !serialized.isEmpty()) {
                    result.add(start(POSITIONS, level));
                    result.addAll(serialized);
                    result.add(end(POSITIONS, level));
                }
            }

            // Serialize the single position of the element, if there is any.
            if (element instanceof HasPosition) {
                HasPosition hasPosition = (HasPosition) element;
                Position position = hasPosition.getPosition();

                List<String> serialized = serializePosition(level + 1, position);
                if (serialized != null) {
                    result.add(start(POSITIONS, level));
                    result.addAll(serialized);
                    result.add(end(POSITIONS, level));
                }
            }

            // Serialize the role of the element, if there is any.
            if (element instanceof HasSemanticRole) {
                HasSemanticRole hasRole = (HasSemanticRole) element;
                SemanticRole role = hasRole.getSemanticRole();

                if (role != null) {
                    result.add(start(ROLE, level) + text(role.getName()) + end(ROLE));
                }
            }

            // Serialize the font face of the element, if there is any.
            if (element instanceof HasFontFace) {
                HasFontFace hasFontFace = (HasFontFace) element;
                FontFace fontFace = hasFontFace.getFontFace();

                if (fontFace != null) {
                    Font font = fontFace.getFont();
                    if (font != null) {
                        String fontId = font.getId();
                        float size = fontFace.getFontSize();
                        if (fontId != null && size > 0) {
                            result.add(start(FONT, level++));
                            result.add(start(ID, level) + text(fontId) + end(ID));
                            result.add(start(FONTSIZE, level) + text(size) + end(FONTSIZE));
                            result.add(end(FONT, --level));
                            this.usedFonts.add(font);
                        }
                    }
                }
            }

            // Serialize the color of the element, if there is any.
            if (element instanceof HasColor) {
                HasColor hasColor = (HasColor) element;
                Color color = hasColor.getColor();

                if (color != null) {
                    String colorId = color.getId();
                    if (colorId != null) {
                        result.add(start(COLOR, level++));
                        result.add(start(ID, level) + text(colorId) + end(ID));
                        result.add(end(COLOR, --level));
                        this.usedColors.add(color);
                    }
                }
            }

            // Serialize the text of the element, if there is any.
            if (element instanceof HasText) {
                HasText hasText = (HasText) element;
                String text = hasText.getText();

                if (text != null) {
                    result.add(start(TEXT, level) + text(text) + end(TEXT));
                }
            }

        }

        return result;
    }

    // ==============================================================================================

    /**
     * Serializes the given list of PDF positions.
     *
     * @param level The current indentation level.
     * @param pos   The positions to serialize.
     * @return A list of strings that represent the lines of the serialization.
     */
    protected List<String> serializePositions(int level, Position... pos) {
        return serializePositions(level, Arrays.asList(pos));
    }

    /**
     * Serializes the given list of PDF positions.
     *
     * @param level The current indentation level.
     * @param pos   The list of positions to serialize.
     * @return A list of strings that represent the lines of the serialization.
     */
    protected List<String> serializePositions(int level, List<Position> pos) {
        List<String> result = new ArrayList<>();
        if (pos != null) {
            for (Position position : pos) {
                List<String> positionLines = serializePosition(level, position);
                if (positionLines != null) {
                    result.addAll(positionLines);
                }
            }
        }
        return result;
    }

    /**
     * Serializes the given PDF position.
     *
     * @param level    The current indentation level.
     * @param position The position to serialize.
     * @return A list of strings that represent the lines of the serialization.
     */
    protected List<String> serializePosition(int level, Position position) {
        List<String> result = new ArrayList<>();

        if (position != null) {
            Page page = position.getPage();
            int pageNumber = page.getPageNumber();
            Rectangle rect = position.getRectangle();

            if (pageNumber > 0 && rect != null) {
                result.add(start(POSITION, level++));

                result.add(start(PAGE, level) + text(pageNumber) + end(PAGE));
                result.add(start(MIN_X, level) + text(rect.getMinX()) + end(MIN_X));
                result.add(start(MIN_Y, level) + text(rect.getMinY()) + end(MIN_Y));
                result.add(start(MAX_X, level) + text(rect.getMaxX()) + end(MAX_X));
                result.add(start(MAX_Y, level) + text(rect.getMaxY()) + end(MAX_Y));

                result.add(end(POSITION, --level));
            }
        }

        return result;
    }

    // ==============================================================================================

    /**
     * Serializes the given fonts.
     *
     * @param level The current indentation level.
     * @param fonts The fonts to serialize.
     * @return A list of strings that represent the lines of the serialization.
     */
    protected List<String> serializeFonts(int level, Font... fonts) {
        return serializeFonts(level, new HashSet<>(Arrays.asList(fonts)));
    }

    /**
     * Serializes the given fonts.
     *
     * @param level The current indentation level.
     * @param fonts The fonts to serialize.
     * @return A list of strings that represent the lines of the serialization.
     */
    protected List<String> serializeFonts(int level, Set<Font> fonts) {
        List<String> result = new ArrayList<>();
        if (fonts != null) {
            for (Font font : fonts) {
                List<String> fontLines = serializeFont(level, font);
                if (fontLines != null) {
                    result.addAll(fontLines);
                }
            }
        }
        return result;
    }

    /**
     * Serializes the given font.
     *
     * @param level The current indentation level.
     * @param font  The font to serialize.
     * @return A list of strings that represent the lines of the serialization.
     */
    protected List<String> serializeFont(int level, Font font) {
        List<String> result = new ArrayList<>();
        if (font != null) {
            String fontId = font.getId();
            result.add(start(FONT, level++));
            if (fontId != null) {
                result.add(start(ID, level) + text(fontId) + end(ID));
            }

            String fontName = font.getNormalizedName();
            if (fontName != null) {
                result.add(start(NAME, level) + text(fontName) + end(NAME));
            }

            result.add(start(IS_BOLD, level) + text(font.isBold()) + end(IS_BOLD));
            result.add(start(IS_ITALIC, level) + text(font.isItalic()) + end(IS_ITALIC));
            result.add(start(IS_TYPE3, level) + text(font.isType3Font()) + end(IS_TYPE3));

            result.add(end(FONT, --level));
        }
        return result;
    }

    // ==============================================================================================
    // Methods to serialize colors.

    /**
     * Serializes the given colors.
     *
     * @param level  The current indentation level.
     * @param colors The colors to serialize.
     * @return A list of strings that represent the lines of the serialization.
     */
    protected List<String> serializeColors(int level, Color... colors) {
        return serializeColors(level, new HashSet<>(Arrays.asList(colors)));
    }

    /**
     * Serializes the given colors.
     *
     * @param level  The current indentation level.
     * @param colors The colors to serialize.
     * @return A list of strings that represent the lines of the serialization.
     */
    protected List<String> serializeColors(int level, Set<Color> colors) {
        List<String> result = new ArrayList<>();

        if (colors != null) {
            for (Color color : colors) {
                if (color != null) {
                    List<String> colorLines = serializeColor(level, color);
                    if (colorLines != null) {
                        result.addAll(colorLines);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Serializes the given color.
     *
     * @param level The current indentation level.
     * @param color The color to serialize.
     * @return A list of strings that represent the lines of the serialization.
     */
    protected List<String> serializeColor(int level, Color color) {
        List<String> result = new ArrayList<>();
        if (color != null) {
            int[] rgb = color.getRGB();

            if (rgb != null && rgb.length == 3) {
                result.add(start(COLOR, level++));
                result.add(start(ID, level) + text(color.getId()) + end(ID));
                result.add(start(R, level) + text(rgb[0]) + end(R));
                result.add(start(G, level) + text(rgb[1]) + end(G));
                result.add(start(B, level) + text(rgb[2]) + end(B));
                result.add(end(COLOR, --level));
            }
        }
        return result;
    }

    // ==============================================================================================
    // Methods to serialize the page information.

    /**
     * Serializes the metadata of the given pages.
     *
     * @param level The current indentation level.
     * @param pages The pages to serialize.
     * @return A list of strings that represent the lines of the serialization.
     */
    protected List<String> serializePages(int level, Page... pages) {
        return serializePages(level, Arrays.asList(pages));
    }

    /**
     * Serializes the metadata of the given pages.
     *
     * @param level The current indentation level.
     * @param pages The pages to serialize.
     * @return A list of strings that represent the lines of the serialization.
     */
    protected List<String> serializePages(int level, List<Page> pages) {
        List<String> result = new ArrayList<>();

        if (pages != null) {
            for (Page page : pages) {
                if (page != null) {
                    List<String> pageLines = serializePage(level, page);
                    if (pageLines != null) {
                        result.addAll(pageLines);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Serializes the metadata of the given page.
     *
     * @param level The current indentation level.
     * @param page  The page to serialize.
     * @return A list of strings that represent the lines of the serialization.
     */
    protected List<String> serializePage(int level, Page page) {
        List<String> result = new ArrayList<>();
        if (page != null) {
            result.add(start(PAGE, level++));
            result.add(start(ID, level) + text(page.getPageNumber()) + end(ID));
            result.add(start(WIDTH, level) + text(page.getWidth()) + end(WIDTH));
            result.add(start(HEIGHT, level) + text(page.getHeight()) + end(HEIGHT));
            result.add(end(PAGE, --level));
        }
        return result;
    }

    // ==============================================================================================

    /**
     * Wraps the given text in an XML start tag.
     *
     * @param text The text to wrap.
     * @return The given text wrapped in an XML start tag.
     */
    protected String startTag(String text) {
        return start(text, 0);
    }

    /**
     * Wraps the given text in an XML start tag and indents it by the given
     * indentation level.
     *
     * @param text  The text to wrap.
     * @param level The indentation level.
     * @return The given text wrapped in an XML start tag, indented by the given
     * indentation level.
     */
    protected String start(String text, int level) {
        String indent = repeat(" ", level * INDENT_LENGTH);
        return indent + "<" + text + ">";
    }

    /**
     * Wraps the given text in an XML end tag.
     *
     * @param text The text to wrap.
     * @return The given text wrapped in an XML end tag.
     */
    protected String end(String text) {
        return end(text, 0);
    }

    /**
     * Wraps the given text in an XML end tag and indents it by the given
     * indentation level.
     *
     * @param text  The text to wrap.
     * @param level The indentation level.
     * @return The given text wrapped in an XML end tag, indented by the given
     * indentation level.
     */
    protected String end(String text, int level) {
        String indent = repeat(" ", level * INDENT_LENGTH);
        return indent + "</" + text + ">";
    }

    /**
     * Transform the given object to an XML escaped string.
     *
     * @param obj The object to process.
     * @return The XML escaped string.
     */
    protected String text(Object obj) {
        return text(obj, 0);
    }

    /**
     * Transform the given object to an XML escaped string and indents it by the
     * given indentation level.
     *
     * @param obj   The object to process.
     * @param level The indentation level.
     * @return The XML escaped string, indented by the given indentation level.
     */
    protected String text(Object obj, int level) {
        String indent = repeat(" ", level * INDENT_LENGTH);
        String text = StringEscapeUtils.escapeXml11(obj.toString());
        return indent + text;
    }

    // ==============================================================================================

    @Override
    public Set<ExtractionUnit> getExtractionUnits() {
        return this.extractionUnits;
    }

    @Override
    public void setExtractionUnits(Set<ExtractionUnit> units) {
        this.extractionUnits = units;
    }

    // ==============================================================================================

    @Override
    public Set<SemanticRole> getSemanticRolesToInclude() {
        return this.semanticRolesToInclude;
    }

    @Override
    public void setSemanticRolesToInclude(Set<SemanticRole> roles) {
        this.semanticRolesToInclude = roles;
    }

    // ==============================================================================================

    /**
     * Checks if the semantic role of the given element is relevant, that is: if it
     * is included in this.semanticRolesToInclude.
     *
     * @param element The element to check.
     * @return True, if the role of the given element is relevant.
     */
    protected boolean hasRelevantRole(HasSemanticRole element) {
        if (element == null) {
            return false;
        }

        if (this.semanticRolesToInclude == null || this.semanticRolesToInclude.isEmpty()) {
            // No semantic roles to include given -> The element is not relevant.
            return false;
        }

        SemanticRole role = element.getSemanticRole();
        if (role == null) {
            return false;
        }

        return this.semanticRolesToInclude.contains(role);
    }

    // ==============================================================================================

    /**
     * Repeats the given string $repeats-times.
     *
     * @param string  The string to repeat.
     * @param repeats The number of repeats.
     * @return The string containing the given string $repeats-times.
     */
    public static String repeat(String string, int repeats) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < repeats; i++) {
            sb.append(string);
        }
        return sb.toString();
    }

    // ==============================================================================================

    /**
     * Serializes the given text block.
     *
     * @param level The current indentation level.
     * @param block The text block to serialize.
     * @return A list of strings that represent the lines of the serialization.
     */
    protected List<String> serializeTextBlock(int level, TextBlock block) {
        List<String> result = new ArrayList<>();
        List<String> blockLines = serializePdfElement(level + 1, block);
        // Wrap the serialized lines with a tag that describe a text block.
        if (blockLines != null && !blockLines.isEmpty()) {
            result.add(start(TEXT_BLOCK, level));
            result.addAll(blockLines);
            result.add(end(TEXT_BLOCK, level));
        }
        return result;
    }

    /**
     * Serializes the given text line.
     *
     * @param level The current indentation level.
     * @param line  The text line to serialize.
     * @return A list of strings that represent the lines of the serialization.
     */
    protected List<String> serializeTextLine(int level, TextLine line) {
        List<String> result = new ArrayList<>();
        List<String> lineLines = serializePdfElement(level + 1, line);
        // Wrap the serialized lines with a tag that describes a text line.
        if (lineLines != null && !lineLines.isEmpty()) {
            result.add(start(TEXT_LINE, level));
            result.addAll(lineLines);
            result.add(end(TEXT_LINE, level));
        }
        return result;
    }
}
