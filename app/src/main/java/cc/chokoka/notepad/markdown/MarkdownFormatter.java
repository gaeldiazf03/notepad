/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package cc.chokoka.notepad.markdown;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BulletSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.QuoteSpan;
import android.text.style.ReplacementSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UnderlineSpan;

import androidx.annotation.ColorInt;

public final class MarkdownFormatter {
    private static final int LIST_GAP_WIDTH = 28;
    private static final int ORDERED_LIST_MARGIN = 42;
    private static final int LINK_COLOR = Color.rgb(34, 98, 214);
    private static final int INLINE_CODE_TEXT_COLOR = Color.rgb(52, 58, 64);

    @ColorInt
    private final int codeColor;
    @ColorInt
    private final int quoteColor;

    public MarkdownFormatter(@ColorInt int codeColor,
                             @ColorInt int quoteColor) {
        this.codeColor = codeColor;
        this.quoteColor = quoteColor;
    }

    public SpannableStringBuilder format(CharSequence text) {
        final SpannableStringBuilder out = new SpannableStringBuilder(text);
        final int n = out.length();

        applyFencedCodeBlocks(out, n);
        applyQuotes(out, n);
        applyLists(out, n);
        applyHeadings(out, n);
        applyBold(out, n, "**");
        applyBold(out, n, "__");
        applyLinks(out, n);
        applyInlineCode(out, n);

        return out;
    }

    private void applyFencedCodeBlocks(SpannableStringBuilder out, int n) {
        int searchFrom = 0;
        while (searchFrom < n) {
            final int fenceStart = indexOf(out, "```", searchFrom, n);
            if (fenceStart < 0) {
                return;
            }

            final int fenceEnd = indexOf(out, "```", fenceStart + 3, n);
            if (fenceEnd < 0) {
                return;
            }

            int blockStart = fenceStart + 3;
            if (blockStart < n && out.charAt(blockStart) == '\r') {
                blockStart++;
            }
            if (blockStart < n && out.charAt(blockStart) == '\n') {
                blockStart++;
            }

            int blockEnd = fenceEnd;
            while (blockEnd > blockStart
                    && (out.charAt(blockEnd - 1) == '\n' || out.charAt(blockEnd - 1) == '\r')) {
                blockEnd--;
            }

            if (blockEnd > blockStart) {
                out.setSpan(new TypefaceSpan(Typeface.MONOSPACE),
                        blockStart,
                        blockEnd,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                out.setSpan(new BackgroundColorSpan(codeColor),
                        blockStart,
                        blockEnd,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            hideSyntax(out, fenceStart, fenceStart + 3);
            hideSyntax(out, fenceEnd, fenceEnd + 3);

            searchFrom = fenceEnd + 3;
        }
    }

    private void applyQuotes(SpannableStringBuilder out, int n) {
        int lineStart = 0;
        while (lineStart < n) {
            int lineEnd = lineStart;
            while (lineEnd < n && out.charAt(lineEnd) != '\n') {
                lineEnd++;
            }

            if (lineStart < lineEnd && out.charAt(lineStart) == '>') {
                int contentStart = lineStart + 1;
                if (contentStart < lineEnd && out.charAt(contentStart) == ' ') {
                    contentStart++;
                }

                if (contentStart < lineEnd) {
                    out.setSpan(new QuoteSpan(quoteColor),
                            contentStart,
                            lineEnd,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    out.setSpan(new ForegroundColorSpan(quoteColor),
                            contentStart,
                            lineEnd,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                hideSyntax(out, lineStart, contentStart);
            }

            lineStart = lineEnd + 1;
        }
    }

    private void applyLists(SpannableStringBuilder out, int n) {
        int lineStart = 0;
        while (lineStart < n) {
            int lineEnd = lineStart;
            while (lineEnd < n && out.charAt(lineEnd) != '\n') {
                lineEnd++;
            }

            if (lineEnd - lineStart > 2) {
                final char first = out.charAt(lineStart);
                final char second = out.charAt(lineStart + 1);
                if ((first == '*' || first == '-') && second == ' ') {
                    final int contentStart = lineStart + 2;
                    if (contentStart < lineEnd) {
                        out.setSpan(new BulletSpan(LIST_GAP_WIDTH),
                                contentStart,
                                lineEnd,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    hideSyntax(out, lineStart, contentStart);
                } else {
                    final int markerEnd = findOrderedMarkerEnd(out, lineStart, lineEnd);
                    if (markerEnd > lineStart) {
                        out.setSpan(new LeadingMarginSpan.Standard(ORDERED_LIST_MARGIN,
                                        ORDERED_LIST_MARGIN),
                                markerEnd,
                                lineEnd,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        hideSyntax(out, lineStart, markerEnd);
                    }
                }
            }

            lineStart = lineEnd + 1;
        }
    }

    private void applyHeadings(SpannableStringBuilder out, int n) {
        int lineStart = 0;
        while (lineStart < n) {
            int lineEnd = lineStart;
            while (lineEnd < n && out.charAt(lineEnd) != '\n') {
                lineEnd++;
            }

            int markerEnd = lineStart;
            int level = 0;
            while (markerEnd < lineEnd && out.charAt(markerEnd) == '#' && level < 3) {
                markerEnd++;
                level++;
            }
            if (level > 0 && markerEnd < lineEnd && out.charAt(markerEnd) == ' ') {
                final int contentStart = markerEnd + 1;
                if (contentStart < lineEnd) {
                    out.setSpan(new StyleSpan(Typeface.BOLD),
                            contentStart,
                            lineEnd,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                    final float scale = switch (level) {
                        case 1 -> 1.75f;
                        case 2 -> 1.5f;
                        default -> 1.25f;
                    };
                    out.setSpan(new RelativeSizeSpan(scale),
                            contentStart,
                            lineEnd,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                hideSyntax(out, lineStart, contentStart);
            }

            lineStart = lineEnd + 1;
        }
    }

    private void applyBold(SpannableStringBuilder out, int n, String marker) {
        int from = 0;
        while (from < n) {
            final int start = indexOf(out, marker, from, n);
            if (start < 0) {
                return;
            }
            final int innerStart = start + marker.length();
            final int end = indexOf(out, marker, innerStart, n);
            if (end < 0) {
                return;
            }

            if (end > innerStart) {
                out.setSpan(new StyleSpan(Typeface.BOLD),
                        innerStart,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                hideSyntax(out, start, innerStart);
                hideSyntax(out, end, end + marker.length());
            }
            from = end + marker.length();
        }
    }

    private void applyLinks(SpannableStringBuilder out, int n) {
        int from = 0;
        while (from < n) {
            final int textOpen = indexOf(out, "[", from, n);
            if (textOpen < 0) {
                return;
            }

            final int textClose = indexOf(out, "]", textOpen + 1, n);
            if (textClose < 0 || textClose + 1 >= n || out.charAt(textClose + 1) != '(') {
                from = textOpen + 1;
                continue;
            }

            final int urlClose = indexOf(out, ")", textClose + 2, n);
            if (urlClose < 0) {
                return;
            }

            final int textStart = textOpen + 1;
            final int textEnd = textClose;
            if (textEnd > textStart) {
                out.setSpan(new ForegroundColorSpan(LINK_COLOR),
                        textStart,
                        textEnd,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                out.setSpan(new UnderlineSpan(),
                        textStart,
                        textEnd,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            hideSyntax(out, textOpen, textStart);
            hideSyntax(out, textClose, urlClose + 1);
            from = urlClose + 1;
        }
    }

    private void applyInlineCode(SpannableStringBuilder out, int n) {
        int from = 0;
        while (from < n) {
            final int start = indexOf(out, "`", from, n);
            if (start < 0) {
                return;
            }

            if (start + 2 < n && out.charAt(start + 1) == '`' && out.charAt(start + 2) == '`') {
                from = start + 3;
                continue;
            }

            final int end = indexOf(out, "`", start + 1, n);
            if (end < 0) {
                return;
            }
            if (end > start + 1) {
                out.setSpan(new InlineCodeSpan(codeColor,
                        INLINE_CODE_TEXT_COLOR,
                        8f,
                        8f,
                        8f),
                        start + 1,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                hideSyntax(out, start, start + 1);
                hideSyntax(out, end, end + 1);
            }
            from = end + 1;
        }
    }

    private static void hideSyntax(SpannableStringBuilder out, int from, int to) {
        final int max = out.length();
        final int start = Math.max(0, Math.min(from, max));
        final int end = Math.max(start, Math.min(to, max));
        if (start >= end) {
            return;
        }

        out.setSpan(new ForegroundColorSpan(Color.TRANSPARENT),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        out.setSpan(new RelativeSizeSpan(0f),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private static int findOrderedMarkerEnd(CharSequence text, int lineStart, int lineEnd) {
        int i = lineStart;
        while (i < lineEnd && Character.isDigit(text.charAt(i))) {
            i++;
        }

        if (i == lineStart || i + 1 >= lineEnd) {
            return -1;
        }

        if (text.charAt(i) == '.' && text.charAt(i + 1) == ' ') {
            return i + 2;
        }

        return -1;
    }

    private static int indexOf(CharSequence text,
                               String needle,
                               int from,
                               int max) {
        final int nl = needle.length();
        if (nl == 0 || from >= max) {
            return -1;
        }

        final int limit = max - nl;
        for (int i = Math.max(from, 0); i <= limit; i++) {
            int j = 0;
            while (j < nl && text.charAt(i + j) == needle.charAt(j)) {
                j++;
            }
            if (j == nl) {
                return i;
            }
        }
        return -1;
    }

    public static final class InlineCodeSpan extends ReplacementSpan {
        private final int backgroundColor;
        private final int textColor;
        private final float horizontalPaddingPx;
        private final float verticalPaddingPx;
        private final float cornerRadiusPx;

        public InlineCodeSpan(@ColorInt int backgroundColor,
                              @ColorInt int textColor,
                              float horizontalPaddingPx,
                              float verticalPaddingPx,
                              float cornerRadiusPx) {
            this.backgroundColor = backgroundColor;
            this.textColor = textColor;
            this.horizontalPaddingPx = horizontalPaddingPx;
            this.verticalPaddingPx = verticalPaddingPx;
            this.cornerRadiusPx = cornerRadiusPx;
        }

        @Override
        public int getSize(Paint paint,
                           CharSequence text,
                           int start,
                           int end,
                           Paint.FontMetricsInt fm) {
            final int oldColor = paint.getColor();
            final Typeface oldTypeface = paint.getTypeface();

            paint.setTypeface(Typeface.MONOSPACE);
            final float textWidth = paint.measureText(text, start, end);

            if (fm != null) {
                final Paint.FontMetricsInt current = paint.getFontMetricsInt();
                fm.ascent = current.ascent - Math.round(verticalPaddingPx);
                fm.descent = current.descent + Math.round(verticalPaddingPx);
                fm.top = fm.ascent;
                fm.bottom = fm.descent;
            }

            paint.setColor(oldColor);
            paint.setTypeface(oldTypeface);
            return Math.round(textWidth + (horizontalPaddingPx * 2f));
        }

        @Override
        public void draw(Canvas canvas,
                         CharSequence text,
                         int start,
                         int end,
                         float x,
                         int top,
                         int y,
                         int bottom,
                         Paint paint) {
            final int oldColor = paint.getColor();
            final Typeface oldTypeface = paint.getTypeface();
            final Paint.Style oldStyle = paint.getStyle();

            paint.setTypeface(Typeface.MONOSPACE);
            final float textWidth = paint.measureText(text, start, end);
            final Paint.FontMetrics metrics = paint.getFontMetrics();
            final float textHeight = metrics.descent - metrics.ascent;

            final float left = x;
            final float right = x + textWidth + (horizontalPaddingPx * 2f);
            final float centerY = (top + bottom) / 2f;
            final float rectTop = centerY - (textHeight / 2f) - verticalPaddingPx;
            final float rectBottom = centerY + (textHeight / 2f) + verticalPaddingPx;

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(backgroundColor);
            canvas.drawRoundRect(new RectF(left, rectTop, right, rectBottom),
                    cornerRadiusPx,
                    cornerRadiusPx,
                    paint);

            paint.setColor(textColor);
            final float baseline = centerY - ((metrics.ascent + metrics.descent) / 2f);
            canvas.drawText(text, start, end, x + horizontalPaddingPx, baseline, paint);

            paint.setColor(oldColor);
            paint.setTypeface(oldTypeface);
            paint.setStyle(oldStyle);
        }
    }
}
