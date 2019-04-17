/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.io.OutputStream;
import java.io.PrintStream;

public class GUIPrintStream extends PrintStream {

    public static final int OUT = 0;
    public static final int ERR = 1;
    public static final int NOTICE = 2;
    public static final int PACKET = 3;
    private final JTextPane mainComponent;
    private final JTextPane component;
    private final int type;
    private final int lineLimit;

    public GUIPrintStream(OutputStream out, JTextPane mainComponent, JTextPane component, int type) {
        super(out);
        this.mainComponent = mainComponent;
        this.component = component;
        this.type = type;
        lineLimit = 100;
    }

    public GUIPrintStream(OutputStream out, JTextPane mainComponent, JTextPane component, int type, int lineLimit) {
        super(out);
        this.mainComponent = mainComponent;
        this.component = component;
        this.type = type;
        this.lineLimit = lineLimit;
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        super.write(buf, off, len);
        final String message = new String(buf, off, len);
        final Color col;
        switch (type) {
            case OUT:
                col = Color.BLACK;
                break;
            case ERR:
                col = Color.RED;
                break;
            case NOTICE:
                col = Color.BLUE;
                break;
            case PACKET:
                col = Color.GRAY;
                break;
            default:
                col = Color.BLACK;
        }

        SwingUtilities.invokeLater(() -> {
                    SimpleAttributeSet attrSet = new SimpleAttributeSet();
                    StyleConstants.setForeground(attrSet, col);

                    Document doc = component.getDocument();
                    Document docMain = mainComponent.getDocument();

                    try {
                        String[] docMainInfo = docMain.getText(0, docMain.getLength()).split("\r\n");
                        String[] docInfo = doc.getText(0, doc.getLength()).split("\r\n");
                        if (docMainInfo.length >= lineLimit + 1) {
                            for (int i = 0; i <= docMainInfo.length - lineLimit - 1; i++) {
                                docMain.remove(0, docMainInfo[i].length() + 2);
                            }
                        }
                        if (docInfo.length >= lineLimit + 1) {
                            for (int i = 0; i <= docInfo.length - lineLimit - 1; i++) {
                                doc.remove(0, docInfo[i].length() + 2);
                            }
                        }
                        docMain.insertString(docMain.getLength(), message, attrSet);
                        doc.insertString(doc.getLength(), message, attrSet);
                    } catch (BadLocationException e) {
                        component.setText("輸出出錯:" + e + "\r\n內容:" + message + "\r\n類型:" + type);
                    }
                }
        );
    }
}
