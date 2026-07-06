/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package cc.chokoka.notepad.main;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.Layout;
import android.text.Selection;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.BulletSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.QuoteSpan;
import android.text.style.ReplacementSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

import cc.chokoka.notepad.R;
import cc.chokoka.notepad.markdown.MarkdownFormatter;

public final class TextEditorView extends EditText {
    private static final long MARKDOWN_DEBOUNCE_MS = 300L;

    private BiConsumer<Integer, Integer> onCursorChanged = (start, end) -> {
    };
    private final Handler markdownHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService markdownExecutor = Executors.newSingleThreadExecutor();
    private final MarkdownFormatter markdownFormatter;
    private int markdownGeneration = 0;
    private boolean applyingMarkdown = false;

    private final TextWatcher markdownWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s,
                                      int start,
                                      int count,
                                      int after) {
        }

        @Override
        public void onTextChanged(CharSequence s,
                                  int start,
                                  int before,
                                  int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (applyingMarkdown) {
                return;
            }

            final String snapshot = s.toString();
            final int selectionStart = Selection.getSelectionStart(s);
            final int selectionEnd = Selection.getSelectionEnd(s);
            final int sourceLength = snapshot.length();
            final int generation = ++markdownGeneration;

            markdownHandler.removeCallbacksAndMessages(null);
            markdownHandler.postDelayed(() -> markdownExecutor.execute(() -> {
                final Editable current = getText();
                if (current == null) {
                    return;
                }

                if (!TextUtils.equals(current.toString(), snapshot)) {
                    return;
                }

                final Editable markdown = markdownFormatter.format(snapshot);
                post(() -> applyMarkdownSpans(generation,
                        snapshot,
                        sourceLength,
                        selectionStart,
                        selectionEnd,
                        markdown));
            }), MARKDOWN_DEBOUNCE_MS);
        }
    };

    public TextEditorView(@NonNull Context context) {
        super(context);
        markdownFormatter = createMarkdownFormatter(context);
        initView();
    }

    public TextEditorView(@NonNull Context context,
                          @Nullable AttributeSet attrs) {
        super(context, attrs);
        markdownFormatter = createMarkdownFormatter(context);
        initView();
    }

    public TextEditorView(@NonNull Context context,
                          @Nullable AttributeSet attrs,
                          int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        markdownFormatter = createMarkdownFormatter(context);
        initView();
    }

    private static MarkdownFormatter createMarkdownFormatter(Context context) {
        return new MarkdownFormatter(context.getResources().getColor(R.color.markdown_code),
            context.getResources().getColor(R.color.markdown_quote));
    }

    private void initView() {
        setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE);
        addTextChangedListener(markdownWatcher);
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        if (onCursorChanged != null) {
            onCursorChanged.accept(selStart, selEnd);
        }
        super.onSelectionChanged(selStart, selEnd);
    }

    public void setOnCursorChanged(BiConsumer<Integer, Integer> onCursorChanged) {
        this.onCursorChanged = onCursorChanged;
    }

    @Override
    protected void onDetachedFromWindow() {
        markdownHandler.removeCallbacksAndMessages(null);
        markdownExecutor.shutdownNow();
        super.onDetachedFromWindow();
    }

    private void applyMarkdownSpans(int generation,
                                    String source,
                                    int sourceLength,
                                    int selectionStart,
                                    int selectionEnd,
                                    Editable markdown) {
        if (generation != markdownGeneration) {
            return;
        }

        final Editable current = getText();
        if (current == null || !TextUtils.equals(current.toString(), source)) {
            return;
        }

        applyingMarkdown = true;
        try {
            clearMarkdownSpans(current);
            copyMarkdownSpans(markdown, current);

            final int targetLength = current.length();
            final int restoredStart = remapSelection(selectionStart, sourceLength, targetLength);
            final int restoredEnd = remapSelection(selectionEnd, sourceLength, targetLength);
            Selection.setSelection(current,
                    Math.min(restoredStart, restoredEnd),
                    Math.max(restoredStart, restoredEnd));
        } finally {
            applyingMarkdown = false;
        }
    }

    private static void clearMarkdownSpans(Editable editable) {
        removeSpans(editable, StyleSpan.class);
        removeSpans(editable, RelativeSizeSpan.class);
        removeSpans(editable, TypefaceSpan.class);
        removeSpans(editable, BackgroundColorSpan.class);
        removeSpans(editable, ForegroundColorSpan.class);
        removeSpans(editable, QuoteSpan.class);
        removeSpans(editable, BulletSpan.class);
        removeSpans(editable, LeadingMarginSpan.class);
        removeSpans(editable, UnderlineSpan.class);
        removeSpans(editable, ReplacementSpan.class);
    }

    private static <T> void removeSpans(Editable editable, Class<T> spanType) {
        for (final T span : editable.getSpans(0, editable.length(), spanType)) {
            editable.removeSpan(span);
        }
    }

    private static void copyMarkdownSpans(Editable from, Editable to) {
        for (final StyleSpan span : from.getSpans(0, from.length(), StyleSpan.class)) {
            to.setSpan(span,
                    from.getSpanStart(span),
                    from.getSpanEnd(span),
                    from.getSpanFlags(span));
        }
        for (final RelativeSizeSpan span : from.getSpans(0, from.length(), RelativeSizeSpan.class)) {
            to.setSpan(span,
                    from.getSpanStart(span),
                    from.getSpanEnd(span),
                    from.getSpanFlags(span));
        }
        for (final TypefaceSpan span : from.getSpans(0, from.length(), TypefaceSpan.class)) {
            to.setSpan(span,
                    from.getSpanStart(span),
                    from.getSpanEnd(span),
                    from.getSpanFlags(span));
        }
        for (final BackgroundColorSpan span : from.getSpans(0, from.length(), BackgroundColorSpan.class)) {
            to.setSpan(span,
                    from.getSpanStart(span),
                    from.getSpanEnd(span),
                    from.getSpanFlags(span));
        }
        for (final ForegroundColorSpan span : from.getSpans(0, from.length(), ForegroundColorSpan.class)) {
            to.setSpan(span,
                    from.getSpanStart(span),
                    from.getSpanEnd(span),
                    from.getSpanFlags(span));
        }
        for (final QuoteSpan span : from.getSpans(0, from.length(), QuoteSpan.class)) {
            to.setSpan(span,
                    from.getSpanStart(span),
                    from.getSpanEnd(span),
                    from.getSpanFlags(span));
        }
        for (final BulletSpan span : from.getSpans(0, from.length(), BulletSpan.class)) {
            to.setSpan(span,
                    from.getSpanStart(span),
                    from.getSpanEnd(span),
                    from.getSpanFlags(span));
        }
        for (final LeadingMarginSpan span : from.getSpans(0, from.length(), LeadingMarginSpan.class)) {
            to.setSpan(span,
                    from.getSpanStart(span),
                    from.getSpanEnd(span),
                    from.getSpanFlags(span));
        }
        for (final UnderlineSpan span : from.getSpans(0, from.length(), UnderlineSpan.class)) {
            to.setSpan(span,
                    from.getSpanStart(span),
                    from.getSpanEnd(span),
                    from.getSpanFlags(span));
        }
        for (final ReplacementSpan span : from.getSpans(0, from.length(), ReplacementSpan.class)) {
            to.setSpan(span,
                    from.getSpanStart(span),
                    from.getSpanEnd(span),
                    from.getSpanFlags(span));
        }
    }

    private static int remapSelection(int sourceIndex,
                                      int sourceLength,
                                      int targetLength) {
        if (sourceIndex < 0) {
            return 0;
        }

        if (sourceLength <= 0) {
            return Math.min(sourceIndex, targetLength);
        }

        final double ratio = (double) sourceIndex / (double) sourceLength;
        final int mapped = (int) Math.round(ratio * targetLength);
        return Math.max(0, Math.min(mapped, targetLength));
    }
}
