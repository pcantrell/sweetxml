package net.innig.sweetxml.cli;

import net.innig.sweetxml.ConversionMode;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

class Configurator
    {
    private ConversionMode explicitMode;
    private boolean overwrite, version, stream, quiet;
    private List<File> files;
    
    public Configurator(String... args)
        {
        files = new ArrayList<File>(100);
        
        for(String arg : args)
            {
            if("-x2s".equals(arg) || "-s2x".equals(arg))
                explicitMode = ConversionMode.valueOf(arg.substring(1).toUpperCase());
            else if("-v".equals(arg) || "--version".equals(arg) || "--help".equals(arg))
                version = true;
            else if("-q".equals(arg) || "--quiet".equals(arg))
                quiet = true;
            else if("--overwrite".equals(arg))
                overwrite = true;
            else if("-".equals(arg))
                {
                stream = true;
                if(explicitMode == null)
                    {
                    System.err.println("You must specify either -x2s or -s2x to convert stdin to stdout.");
                    System.err.println("Specify --help for usage.");
                    System.exit(1);
                    }
                }
            else if(arg.startsWith("-"))
                {
                usage();
                System.err.println();
                System.err.println("Unknown option: " + arg);
                System.exit(1);
                }
            else
                {
                File file = new File(arg);
                if(!file.exists())
                    {
                    System.err.println("File does not exist: " + arg);
                    System.exit(1);
                    }
                
                getModeFor(file); // to check for usage problems
                
                files.add(file);
                }
            }
        
        if(version)
            {
            usage();
            System.exit(0);
            }
        }
    
    public ConversionMode getModeFor(File file)
        {
        if(explicitMode != null)
            return explicitMode;
        
        if(file.isDirectory())
            {
            System.err.println("Attempting to convert directory: " + file);
            System.err.println("You must specify either -x2s or -s2x to convert a directory.");
            System.err.println("Specify --help for usage.");
            System.exit(1);
            }
                
        String ext = file.getName();
        int dot = ext.lastIndexOf('.');
        ext = (dot >= 0) ? ext.substring(dot) : "";
        
        ConversionMode mode = ConversionMode.forSourceExtension(ext);
        if(mode == null)
            {
            System.err.println("Don't know how to convert: " + file);
            System.err.println("You must specify either -x2s or -s2x to convert files which do not end with either \".xml\" or \".sxml\".");
            System.err.println("Specify --help for usage.");
            System.exit(1);
            }
        
        return mode;
        }

    public List<File> getFiles()
        { return Collections.unmodifiableList(files); }

    public ConversionMode getExplicitMode()
        { return explicitMode; }

    public boolean isOverwriteEnabled()
        { return overwrite; }

    public boolean isStreamEnabled()
        { return stream; }
    
    public boolean isQuiet()
        { return quiet; }

    private static void usage()
        {
        ResourceBundle coreBundle = ResourceBundle.getBundle("net.innig.sweetxml.core");
        System.err.println("SweetXML, version " + coreBundle.getString("version"));
        System.err.println("  Copyright (c) 2006 Paul Cantrell");
        System.err.println();
        System.err.println("  options: ");
        System.err.println("    -x2s            Convert XML to SweetXML");
        System.err.println("    -s2x            Convert SweetXML to XML");
        System.err.println("    --overwrite     Overwrite existing files");
        System.err.println("    -q | --quiet    Don't print progress messages");
        System.err.println("    -v | --version  Print version and help (this message)");
        System.err.println("       | --help     ");
        System.err.println("    <file>          Convert the given file");
        System.err.println("    <dir>           Convert files with appropriate extension under dir");
        System.err.println("                        (requires either -x2s or -s2x)");
        System.err.println("    -               Convert stdin -> stdout");
        }
    }
