package com.github.jstumpp.uups;

import java.util.List;

public class UupsFrame {
    public String fileName;
    public String methodName;
    public int lineNumber;
    public int lineStart;
    public int lineNth;
    public String location;
    public String source;
    public String open;
    public String type;
    public List<UupsFrameComment> comments;
}

