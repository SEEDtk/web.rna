package org.theseed.web.rna;

import java.util.Arrays;

import org.theseed.web.ColumnProcessor;
import org.theseed.web.ColumnSaveProcessor;
import org.theseed.web.ProductionProcessor;
import org.theseed.web.RnaMetaProcessor;
import org.theseed.web.ScatterProcessor;
import org.theseed.web.WebProcessor;

/**
 * Commands for Web Pages for RNA Threonine Display.
 *
 * columns		show columnar comparison data for FPKM and TPM results
 * meta			show metadata for samples
 * saveCols		save the current column configuration under a new name
 * manage		manage saved column specifications
 *
 */
public class App
{
    public static void main( String[] args )
    {
        // Get the control parameter.
        String command = args[0];
        String[] newArgs = Arrays.copyOfRange(args, 1, args.length);
        WebProcessor processor;
        // Determine the command to process.
        switch (command) {
        case "columns" :
            processor = new ColumnProcessor();
            break;
        case "meta" :
            processor = new RnaMetaProcessor();
            break;
        case "manage" :
            processor = new ColumnManageProcessor();
            break;
        case "saveCols" :
            processor = new ColumnSaveProcessor();
            break;
        case "scatter" :
            processor = new ScatterProcessor();
            break;
        case "production" :
            processor = new ProductionProcessor();
            break;
        case "predManage" :
            processor = new ProductionManageProcessor();
            break;
        default:
            throw new RuntimeException("Invalid command " + command);
        }
        // Process it.
        boolean ok = processor.parseCommand(newArgs);
        if (ok) {
            processor.run();
        }
    }
}
