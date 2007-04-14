package net.innig.sweetxml;

import java.io.IOException;

/**
 * An error during a conversion to or from SweetXML.
 */
public class SweetXmlParseException
    extends IOException
    {
    private final DocumentPosition position;
    private final SweetXmlMessage message;
    private final Object[] messageArgs;

    public SweetXmlParseException(
            DocumentPosition position,
            SweetXmlMessage message,
            Object... messageArgs)
        {
        super(position + ": " + message.format(messageArgs));
        this.position = position;
        this.message = message;
        this.messageArgs = messageArgs;
        }
    
    public DocumentPosition getPosition()
        { return position; }

    public SweetXmlMessage getMessageId()
        { return message; }

    public Object[] getMessageArgs()
        { return messageArgs; }
    }
