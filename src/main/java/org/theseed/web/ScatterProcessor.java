/**
 *
 */
package org.theseed.web;

import java.io.File;
import java.io.IOException;

import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.io.TabbedLineReader;
import org.theseed.reports.ColSpec;
import org.theseed.reports.HtmlForm;
import org.theseed.reports.HtmlTable;
import org.theseed.reports.Key;
import org.theseed.utils.IDescribable;
import org.theseed.web.forms.FormElement;
import org.theseed.web.graph.ScatterGraph;
import org.theseed.web.rna.ScatterSort;

import j2html.tags.DomContent;
import static j2html.TagCreator.*;

/**
 * This web page produces a scatter diagram.  The diagram shows predicted versus actual values for
 * bacterial samples.  Mousing over a dot will display the sample ID.  In addition, a range of
 * values can be chosen for display of a tabular report.
 *
 * The input file is thr.predictions.tbl in the CoreSEED data directory.  The user can specify a cutoff
 * value that is used to display pseudo-accuracy and a confusion matrix.
 *
 * The positional parameters are the name of the CoreSEED data directory and the name of the user's workspace.
 * The command-line options are as follows.
 *
 * --source			type of the input file
 * --prodBound		cutoff value to be used for production axis in confusion matrix
 * --predBound		cutoff value to be used for prediction axis in confusion matrix
 * --prodMin		minimum production value to display in report
 * --prodMax		maximum production value to display in report
 * --predMin		minimum prediction value to display in report
 * --predMax		maximum prediction value to display in report
 *
 * @author Bruce Parrello
 *
 */
public class ScatterProcessor extends WebProcessor {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(ScatterProcessor.class);
    /** scatter graph object */
    private ScatterGraph graph;
    /** confusion matrix 0 = negative, 1 = positive; first index is production, second is prediction */
    private int[][] cMatrix;
    /** tabular report */
    private HtmlTable<Key.RevFloat> tabularReport;
    /** buffer size around dot for click event */
    private static final double CLICK_RADIUS = 0.2;
    /** function body for click event */
    private static final String SET_BOUNDS = String.format(
            "function setBounds(x, y) {%n" +
            "    $(\"#runForm input[name=prodMin]\").val(x - %f);%n" +
            "    $(\"#runForm input[name=prodMax]\").val(x + %f);%n" +
            "    $(\"#runForm input[name=predMin]\").val(y - %f);%n" +
            "    $(\"#runForm input[name=predMax]\").val(y + %f);%n" +
            "};", CLICK_RADIUS, CLICK_RADIUS, CLICK_RADIUS, CLICK_RADIUS);

    // COMMAND-LINE OPTIONS

    @FormElement
    @Option(name = "--source", usage = "input source to use")
    protected InputTypes source;

    /** cutoff bound for production values */
    @FormElement
    @Option(name = "--prodBound", metaVar = "1.0", usage = "Cutoff for useful production values")
    protected double prodBound;

    /** cutoff bound for prediction values */
    @FormElement
    @Option(name = "--predBound", metaVar = "0.8", usage = "Cutoff for useful prediction values")
    protected double predBound;

    /** minimum production value for tabular report */
    @FormElement
    @Option(name = "--prodMin", metaVar = "0.5", usage = "Minimum production value for tabular report")
    protected double prodMin;

    /** maximum production value for tabular report */
    @FormElement
    @Option(name = "--prodMax", metaVar = "0.5", usage = "Maximum production value for tabular report")
    protected double prodMax;

    /** minimum prediction value for tabular report */
    @FormElement
    @Option(name = "--predMin", metaVar = "0.5", usage = "Minimum prediction value for tabular report")
    protected double predMin;

    /** maximum prediction value for tabular report */
    @FormElement
    @Option(name = "--predMax", metaVar = "0.5", usage = "Maximum prediction value for tabular report")
    protected double predMax;

    /** type of sort for tabular report */
    @FormElement
    @Option(name = "--sort", usage = "Order of tabular report")
    protected ScatterSort sortType;

    /**
     * enum for input file types, computes associated file name
     */
    public static enum InputTypes implements IDescribable {
        NORMAL("All processed samples", "thr.predictions.tbl"),
        ONLY24("Only 24-hour samples", "thr24.predictions.tbl"),
        LOW("All samples, but trained on production < 1.3", "thrLow.predictions.tbl"),
        SMALL("All samples, but trained on a random subset of 80%.", "thrSmall.predictions.tbl"),
        HALF("All samples, but trained on a random subset of 50%.", "thrHalf.predictions.tbl"),
        SMALL24("Only 24-hour samples, but trained on a random subset of 80%.", "thr24S.predictions.tbl");

        private String description;
        private String fileName;

        private InputTypes(String desc, String fName) {
            this.description = desc;
            this.fileName = fName;
        }

        @Override
        public String getDescription() {
            return this.description;
        }

        /**
         * @return the file name for this input type
         */
        public String getFileName() {
            return this.fileName;
        }

    }

    @Override
    protected void setWebDefaults() {
        this.predBound = 1.0;
        this.prodBound = 1.0;
        this.predMax = 0.0;
        this.predMin = 0.0;
        this.prodMax = 0.0;
        this.prodMin = 0.0;
        this.sortType = ScatterSort.PRODUCTION;
        this.source = InputTypes.NORMAL;
    }

    @Override
    protected boolean validateWebParms() throws IOException {
        if (this.predMin > this.predMax)
            log.warn("Minimum prediction value {} is bigger than maximum {}.", this.predMin, this.predMax);
        if (this.prodMin > this.prodMax)
            log.warn("Minimum production value {} is bigger than maximum {}.", this.prodMin, this.prodMax);
        return true;
    }

    @Override
    protected String getCookieName() {
        return HtmlForm.formCookieName("rna", "scatter");
    }

    @Override
    protected void runWebCommand(CookieFile cookies) throws Exception {
        // Save the form data.  Because we are both the processor and the form, we have to close the cookie file.
        this.saveForm(cookies);
        cookies.flush();
        // Set up the confusion matrix.
        this.cMatrix = new int[][] { {0, 0}, {0, 0} };
        // Create the form.
        HtmlForm runForm = this.buildForm(this.getClass(), "rna", "scatter");
        runForm.setId("runForm");
        // Create the scatter graph.
        this.graph = new ScatterGraph(1000, 800, "graph", 20, 20, 50, 100);
        this.graph.defineAxes("production", "predicted");
        // Set the click event.
        this.graph.setClickEvent("setBounds");
        // Create the script for the click event.
        DomContent scriptSection = script(rawHtml(SET_BOUNDS));
        // Create the HTML table.
        this.tabularReport = new HtmlTable<Key.RevFloat>(new ColSpec.Normal("Sample ID"), new ColSpec.Fraction("Production"),
                new ColSpec.Fraction("Predicted"), new ColSpec.Fraction("Error"), new ColSpec.Num("Growth"));
        // Now run through the input, building both the table and the graph.
        File inFile = new File(this.getCoreDir(), this.source.getFileName());
        try (TabbedLineReader inStream = new TabbedLineReader(inFile)) {
            int sampleCol = inStream.findField("sample_id");
            int prodCol = inStream.findField("production");
            int growthCol = inStream.findField("density");
            int predCol = inStream.findField("predicted");
            log.info("Reading data points from {}.", inFile);
            for (TabbedLineReader.Line line : inStream) {
                String sample = line.get(sampleCol);
                double prod = line.getDouble(prodCol);
                double pred = line.getDouble(predCol);
                double growth = line.getDouble(growthCol);
                // Add this point to the graph.
                this.graph.add(sample, prod, pred);
                // Count it in the confusion matrix.
                int predIdx = (pred >= this.predBound ? 1 : 0);
                int prodIdx = (prod >= this.prodBound ? 1 : 0);
                this.cMatrix[prodIdx][predIdx]++;
                // If it is in range, put it in the table.
                if (pred >= this.predMin && pred <= this.predMax && prod >= this.prodMin && prod <= this.prodMax) {
                    Key.RevFloat key = new Key.RevFloat(this.sortType.sortValue(prod, pred));
                    this.tabularReport.new Row(key).add(sample).add(prod)
                            .add(pred).add(pred - prod).add(growth);
                }
            }
            log.info("{} points added to graph, {} to tabular report.", this.graph.size(), this.tabularReport.getHeight());
        }
        // We will build the output sections in here.
        DomContent matrixSection;
        DomContent graphSection;
        DomContent tableSection;
        // The first two sections contain data from the graph points.
        int points = this.graph.size();
        if (points == 0) {
            matrixSection = p("No records found in input table.");
            graphSection = p("");
        } else {
            // Here we display stats on the accuracy.
            matrixSection = ul(
                    li(String.format("Accuracy is %4.2f%%.", (this.cMatrix[0][0] + this.cMatrix[1][1]) * 100.0 / points)),
                    li(String.format("False negatives are %4.2f%%.", this.cMatrix[1][0] * 100.0 / points)),
                    li(String.format("False positives are %4.2f%%.", this.cMatrix[0][1] * 100.0 / points))
                    );
            // Now we output the scatter graph.
            this.graph.plot();
            this.graph.drawXBound(prodBound);
            this.graph.drawYBound(predBound);
            graphSection = this.graph.getHtml();
        }
        // Next comes the tabular report.
        if (this.tabularReport.getHeight() == 0)
            tableSection = p("No points qualified for the tabular report.");
        else
            tableSection = this.tabularReport.output();
        // Now assemble all the sections.
        DomContent highlightBlock = this.getPageWriter().highlightBlock(runForm.output(),
                scriptSection, matrixSection, graphSection, tableSection);
        this.getPageWriter().writePage("Threonine Prediction Report", h1("Threonine Prediction Report and Graph"),
                highlightBlock);
    }

}
