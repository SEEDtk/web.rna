package org.theseed.web.rna;

import java.util.Arrays;

import org.theseed.web.ColumnProcessor;
import org.theseed.web.WebProcessor;

/**
 * Commands for Web Pages for RNA Threonine Display.
 *
 *
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
