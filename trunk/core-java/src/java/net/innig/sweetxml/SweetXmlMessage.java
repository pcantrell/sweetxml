package net.innig.sweetxml;

import java.text.MessageFormat;
import java.util.ResourceBundle;

public enum SweetXmlMessage
    {
    ILLEGAL_NAME_CHARACTER,
    ENDLESS_QUOTE,
    INCONSISTENT_INDENTATION,
    EXPECTED_EQ_IN_ATTRIBUTE,
    SEVERED_ATTRIBUTE,
    CONTINUATION_OUTSIDE_TAG,
    MISPLACED_CONTINUATION,
    
    INDENT_SPACES,
    INDENT_TABS,
    INDENT_UNICODE,
    
    EOF_IN_XML_DECLARATION,
    EOF_IN_DIRECTIVE,
    EOF_IN_TAG,
    EOF_IN_ANONYMOUS_TAG,
    UNEXPECTED_CHAR_IN_TAG,
    UNEXPECTED_CHAR_IN_ANONYMOUS_TAG,
    UNEXPECTED_CHAR_IN_END_TAG,
    UNEXPECTED_END_TAG,
    MISMATCHED_END_TAG,
    EXPECTED_EQ_IN_XML_ATTRIBUTE,
    EXPECTED_XML_ATTRIBUTE_VALUE;
    
    public String format(Object... args)
        {
        return MessageFormat.format(
            ResourceBundle
                .getBundle(getClass().getName())
                .getString(name()),
            args);
        }
    
    public static String formatChar(int c)
        {
        if(c == -1)
            return "EOF";
        if(Character.isISOControl(c))
            return "\\x" + Integer.toHexString(0x100 | c).substring(1);
        if(c != ' ' && Character.isWhitespace(c))
            return "\\u" + Integer.toHexString(0xFFFF0000 | c).substring(4);
        return String.valueOf((char) c);
        }
    }
