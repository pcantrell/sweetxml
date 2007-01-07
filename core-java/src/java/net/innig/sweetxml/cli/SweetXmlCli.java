package net.innig.sweetxml.cli;

import net.innig.sweetxml.Converter;
import net.innig.sweetxml.FileConverterEngine;
import net.innig.sweetxml.SweetXmlParseException;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class SweetXmlCli
    {
    public static void main(String... args)
        {
        Configurator config = new Configurator(args);
        
        if(config.getFiles().isEmpty() && !config.isStreamEnabled() && !config.isQuiet())
            {
            System.err.println("Nothing to do.");
            System.err.println("(specify --help for options)");
            }
        
        FileConverterEngine engine = new FileConverterEngine(
            config.isOverwriteEnabled(), config.isQuiet());
        
        for(File file : config.getFiles())
            try {
                engine.convert(file, config.getModeFor(file));
                }
            catch(Exception e)
                { conversionError(file.toString(), e); }
        
        if(config.isStreamEnabled())
            try
                {
                Converter c = config.getExplicitMode().createConverter();
                c.setInput(new InputStreamReader(System.in));
                System.out.print(c.getResult());
                }
            catch(Exception e)
                { conversionError("stdin", e); }
        }

    private static void conversionError(String sourceName, Exception e)
        {
        System.err.println("Unable to convert " + sourceName);
        if(e instanceof SweetXmlParseException)
            System.err.println(e.getMessage());
        else if(e instanceof IOException)
            for(Throwable chain = e; chain != null; chain = chain.getCause())
                System.err.println("    " + chain);
        else
            e.printStackTrace();
        System.exit(1);
        }
    }
