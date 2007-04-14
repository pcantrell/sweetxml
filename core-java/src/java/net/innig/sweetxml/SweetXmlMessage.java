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
    
    INDENT_NONE,
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
            massageMessageArgs(args));
        }

    private Object[] massageMessageArgs(Object[] args)
        {
        for(int i = 0; i < args.length; i++)
            if(args[i] instanceof Character)
                {
                char c = (Character) args[i];
                if(c < ' ')
                    args[i] = "\\x" + Integer.toHexString(0x100 | c);
                }
        return args;
        }
    }
