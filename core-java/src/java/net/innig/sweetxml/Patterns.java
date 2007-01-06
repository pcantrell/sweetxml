package net.innig.sweetxml;

import java.util.regex.Pattern;

final class Patterns
    {
    public static final Pattern
        xmlNameStartChar  = Pattern.compile("[A-Za-z_/\\xC0-\\xD6\\xD8-\\xF6\\u00F8-\\u02FF\\u0370-\\u037D\\u037F-\\u1FFF\\u200C-\\u200D\\u2070-\\u218F\\u2C00-\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD]"),
        xmlNameChar       = Pattern.compile("[A-Za-z_/\\xC0-\\xD6\\xD8-\\xF6\\u00F8-\\u02FF\\u0370-\\u037D\\u037F-\\u1FFF\\u200C-\\u200D\\u2070-\\u218F\\u2C00-\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD\\-\\.0-9\\xB7\\u0300-\\u036F\\u203F-\\u2040]"),
        quotingRequired   = Pattern.compile("[^A-Za-z0-9\\-\\./_]"),
        initialWhitespace = Pattern.compile("(\\s*)(.*)", Pattern.DOTALL),
        lineBreak         = Pattern.compile("\r\n|\r|\n");
    
    private Patterns() { }
    }
