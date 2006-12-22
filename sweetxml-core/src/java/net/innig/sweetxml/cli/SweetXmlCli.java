package net.innig.sweetxml.cli;

import net.innig.sweetxml.ConversionMode;
import net.innig.sweetxml.FileConverterEngine;

import java.io.File;

public class SweetXmlCli
    {
    public static void main(String... args)
        {
        if(args.length < 1)
            {
            usage();
            return;
            }
        String modeS = args[0];
        if(!modeS.startsWith("-"))
            {
            usage();
            return;
            }
        ConversionMode mode;
        try { mode = ConversionMode.valueOf(modeS.substring(1).toUpperCase()); }
        catch(IllegalArgumentException iae)
            {
            System.err.println("No such mode \"" + modeS.substring(1) + "\"; expected \"x2s\" or \"s2x\"");
            return;
            }
        
        FileConverterEngine engine = new FileConverterEngine(mode);
        for(int i = 1; i < args.length; i++)
            try {
                engine.convert(new File(args[i]));
                }
            catch(Exception e)
                {
                System.err.println("Unable to convert " + args[i]);
                for(Throwable chain = e; chain != null; chain = chain.getCause())
                    System.err.println("    " + chain);
                }
        System.out.println("Done.");
        }

    private static void usage()
        {
        System.err.println("SweetXML <=> XML converter");
        System.err.println("options: ");
        System.err.println("    -x2s    Convert XML to SweetXML");
        System.err.println("    -s2x    Convert SweetXML to XML");
        System.err.println("    <file>  Convert the given file");
        System.err.println("    <dir>   Convert files with appropriate extension under dir");
        }
    }
