package com.vanderlande.bsminserter;

import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;

import javax.swing.*;
import java.io.Serializable;

public class GuiLogAppender extends AbstractAppender {
    private static JTextArea logArea;

    protected GuiLogAppender(String name, Filter filter, Layout<? extends Serializable> layout) {
        super(name, filter, layout);
    }

    public static GuiLogAppender createAppender() {
        return new GuiLogAppender("GuiLogAppender", null,
                PatternLayout.newBuilder().withPattern("[%d{HH:mm:ss}] [%p] %m%n").build());
    }

    public static void setLogArea(JTextArea area) {
        logArea = area;
    }

    @Override
    public void append(LogEvent event) {
        if (logArea != null) {
            String msg = new String(getLayout().toByteArray(event));
            SwingUtilities.invokeLater(() -> logArea.append(msg));
        }
    }
}
