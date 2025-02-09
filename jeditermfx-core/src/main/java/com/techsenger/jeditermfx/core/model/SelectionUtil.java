package com.techsenger.jeditermfx.core.model;

import com.techsenger.jeditermfx.core.compatibility.Point;
import com.techsenger.jeditermfx.core.util.CharUtils;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author traff
 */
public class SelectionUtil {

    private static final List<Character> SEPARATORS = new ArrayList<>();

    static {
        SEPARATORS.add(' ');
        SEPARATORS.add('\u00A0'); // NO-BREAK SPACE
        SEPARATORS.add('\t');
        SEPARATORS.add('\'');
        SEPARATORS.add('"');
        SEPARATORS.add('$');
        SEPARATORS.add('(');
        SEPARATORS.add(')');
        SEPARATORS.add('[');
        SEPARATORS.add(']');
        SEPARATORS.add('{');
        SEPARATORS.add('}');
        SEPARATORS.add('<');
        SEPARATORS.add('>');
    }

    public static kotlin.Pair<Point, Point> sortPoints(Point a, Point b) {
        if (a.y == b.y) { /* same line */
            return new Pair<>(a.x <= b.x ? a : b, a.x > b.x ? a : b);
        } else {
            return new Pair<>(a.y < b.y ? a : b, a.y > b.y ? a : b);
        }
    }

    public static String getSelectedText(TerminalSelection selection, TerminalTextBuffer terminalTextBuffer) {
        return getSelectedText(selection.getStart(), selection.getEnd(), terminalTextBuffer);
    }

    @NotNull
    public static String getSelectedText(@NotNull Point selectionStart, @NotNull Point selectionEnd,
                                          @NotNull TerminalTextBuffer terminalTextBuffer) {
        Pair<Point, Point> pair = sortPoints(selectionStart, selectionEnd);
        pair.getFirst().y = Math.max(pair.getFirst().y, -terminalTextBuffer.getHistoryLinesCount());
        pair = sortPoints(pair.getFirst(), pair.getSecond()); // previous line may have changed the order
        Point top = pair.getFirst();
        Point bottom = pair.getSecond();
        final StringBuilder selectedText = new StringBuilder();
        for (int i = top.y; i <= bottom.y; i++) {
            TerminalLine line = terminalTextBuffer.getLine(i);
            String text = line.getText();
            if (i == top.y) {
                if (i == bottom.y) {
                    selectedText.append(processForSelection(text.substring(Math.min(text.length(), top.x),
                            Math.min(text.length(), bottom.x))));
                } else {
                    selectedText.append(processForSelection(text.substring(Math.min(text.length(), top.x))));
                }
            } else if (i == bottom.y) {
                selectedText.append(processForSelection(text.substring(0, Math.min(text.length(), bottom.x))));
            } else {
                selectedText.append(processForSelection(line.getText()));
            }
            if ((!line.isWrapped() && i < bottom.y) || bottom.x > text.length()) {
                selectedText.append("\n");
            }
        }
        return selectedText.toString();
    }

    private static String processForSelection(String text) {
        if (text.indexOf(CharUtils.DWC) != 0) {
            // remove dwc second chars
            StringBuilder sb = new StringBuilder();
            for (char c : text.toCharArray()) {
                if (c != CharUtils.DWC) {
                    sb.append(c);
                }
            }
            return sb.toString();
        } else {
            return text;
        }
    }

    public static Point getPreviousSeparator(Point charCoords, TerminalTextBuffer terminalTextBuffer) {
        return getPreviousSeparator(charCoords, terminalTextBuffer, SEPARATORS);
    }

    public static Point getPreviousSeparator(Point charCoords, TerminalTextBuffer terminalTextBuffer,
                                             @NotNull List<Character> separators) {
        int x = charCoords.x;
        int y = charCoords.y;
        int terminalWidth = terminalTextBuffer.getWidth();
        if (separators.contains(terminalTextBuffer.getBuffersCharAt(x, y))) {
            return new Point(x, y);
        }
        String line = terminalTextBuffer.getLine(y).getText();
        while (x < line.length() && !separators.contains(line.charAt(x))) {
            x--;
            if (x < 0) {
                if (y <= -terminalTextBuffer.getHistoryLinesCount()) {
                    return new Point(0, y);
                }
                y--;
                x = terminalWidth - 1;

                line = terminalTextBuffer.getLine(y).getText();
            }
        }
        x++;
        if (x >= terminalWidth) {
            y++;
            x = 0;
        }
        return new Point(x, y);
    }

    public static Point getNextSeparator(Point charCoords, TerminalTextBuffer terminalTextBuffer) {
        return getNextSeparator(charCoords, terminalTextBuffer, SEPARATORS);
    }

    public static Point getNextSeparator(Point charCoords, TerminalTextBuffer terminalTextBuffer,
                                         @NotNull List<Character> separators) {
        int x = charCoords.x;
        int y = charCoords.y;
        int terminalWidth = terminalTextBuffer.getWidth();
        int terminalHeight = terminalTextBuffer.getHeight();
        if (separators.contains(terminalTextBuffer.getBuffersCharAt(x, y))) {
            return new Point(x, y);
        }
        String line = terminalTextBuffer.getLine(y).getText();
        while (x < line.length() && !separators.contains(line.charAt(x))) {
            x++;
            if (x >= terminalWidth) {
                if (y >= terminalHeight - 1) {
                    return new Point(terminalWidth - 1, terminalHeight - 1);
                }
                y++;
                x = 0;

                line = terminalTextBuffer.getLine(y).getText();
            }
        }
        x--;
        if (x < 0) {
            y--;
            x = terminalWidth - 1;
        }
        return new Point(x, y);
    }
}
