package pdfact.core.pipes.parse.stream.pdfbox.operators.color;

import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.color.PDColorSpace;
import pdfact.core.model.Document;
import pdfact.core.model.Page;

import java.io.IOException;
import java.util.List;

/**
 * G: Set the stroking color space to DeviceGray and set the gray level to use
 * for stroking operations.
 *
 * @author Claudius Korzen
 */
public class SetStrokingDeviceGrayColor extends SetStrokingColor {
    @Override
    public void process(Document pdf, Page page, Operator op, List<COSBase> args) throws IOException {
        PDResources resources = this.engine.getResources();
        PDColorSpace cs = resources.getColorSpace(COSName.DEVICEGRAY);
        this.engine.getGraphicsState().setStrokingColorSpace(cs);
        super.process(pdf, page, op, args);
    }

    @Override
    public String getName() {
        return "G";
    }
}
