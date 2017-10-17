package com.hello2mao.xlogging.urlconnection.io.parser;

import com.hello2mao.xlogging.urlconnection.CharBuffer;

import junit.framework.Assert;

public abstract class AbstractParser {

    private HttpParserHandler handler;
    private int maxBufferSize;
    int charactersInMessage;
    long currentTimeStamp;
    protected CharBuffer buffer;

    public AbstractParser(AbstractParser parser) {
        initialize(parser.handler, parser.charactersInMessage);
    }

    public AbstractParser(HttpParserHandler httpParserHandler) {
        initialize(httpParserHandler, 0);
    }

    private void initialize(HttpParserHandler handler, int charactersInMessage) {
        this.handler = handler;
        this.maxBufferSize = getMaxBufferSize();
        this.buffer = new CharBuffer(getInitialBufferSize());
        this.charactersInMessage = charactersInMessage;
    }

    /**
     * 把单个字节加入缓存buffer进行parser
     *
     * @param oneByte the next byte of data from the stream.
     *             The value byte is an <code>int</code> in the range <code>0</code> to <code>255</code>.
     *             Or <code>-1</code> if the end of the stream is reached.
     * @return boolean
     */
    public boolean add(int oneByte) {
        if (oneByte == -1) {
            // 流结束
            reachedEOF();
            return true;
        }
        charactersInMessage += 1;
        char character = (char) oneByte;
        AbstractParser parser;
        if (character == '\n') { // 遇到换行符，则进行解析，会调用相应parser
            if (parse(buffer)) { // 对缓冲的buffer进行解析
                // 解析成功，则获取下个parser
                parser = nextParserAfterSuccessfulParse();
            } else {
                parser = NoopLineParser.DEFAULT;
            }
        } else if (buffer.length < maxBufferSize) { // 不是换行符，且buffer大小在max内，则缓存在buffer
            int targetLength = buffer.length + 1;
            // buffer空间不够，则double空间大小
            if (targetLength > buffer.charArray.length) {
                final char[] dest = new char[Math.max(buffer.charArray.length << 1, targetLength)];
                System.arraycopy(buffer.charArray, 0, dest, 0, buffer.length);
                buffer.charArray = dest;
            }
            buffer.charArray[buffer.length] = character;
            buffer.length = targetLength;
            parser = this;
        } else { // buffer满
            parser = nextParserAfterBufferFull();
        }
        Assert.assertNotNull(parser);
        if (parser != this) {
            handler.setNextParserState(parser);
        }
        return parser != this;
    }

    /**
     * 把内容块加入缓存buffer进行parser
     *
     * @param buffer byte[]
     * @param offset int
     * @param count int
     */
    public void add(byte[] buffer, int offset, int count) {
        int j;
        for (int i = addBlock(buffer, offset, count); i > 0 && i < count; i += j) {
            j = handler.getCurrentParser().addBlock(buffer, offset + i, count - i);
            if (j <= 0) {
                break;
            }
        }
    }

    protected int addBlock(byte[] buffer, int offset, int count) {
        if (count == -1) {
            reachedEOF();
            return -1;
        }
        if (buffer == null || count == 0) {
            return -1;
        }
        boolean bool = false;
        int i = 0;
        while (!bool && i < count) {
            bool = add((char) buffer[offset + i]);
            ++i;
        }
        return i;
    }

    public CharBuffer getBuffer()
    {
        return buffer;
    }

    int getCharactersInMessage() {
        return charactersInMessage;
    }

    public HttpParserHandler getHandler() {
        return handler;
    }

    protected abstract int getInitialBufferSize();

    protected abstract int getMaxBufferSize();

    public abstract AbstractParser nextParserAfterBufferFull();

    public abstract AbstractParser nextParserAfterSuccessfulParse();

    public abstract boolean parse(CharBuffer charBuffer);

    private void reachedEOF() {
        getHandler().setNextParserState(NoopLineParser.DEFAULT);
    }

    void setCharactersInMessage(int charactersInMessage) {
        this.charactersInMessage = charactersInMessage;
    }

    public String toString() {
        return buffer.toString();
    }

    public void close() {
        if (handler != null) {
            handler.setNextParserState(NoopLineParser.DEFAULT);
        }
    }
}