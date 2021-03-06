/*
 * Copyright 2018 Mr Duy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jecelyin.editor.v2.ui.editor;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.core.widget.BaseEditorView;
import android.core.widget.EditAreaView;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import com.duy.ccppcompiler.R;
import com.duy.ide.editor.span.ErrorSpan;
import com.duy.ide.filemanager.SaveListener;
import com.jecelyin.common.utils.DLog;
import com.jecelyin.editor.v2.Preferences;
import com.jecelyin.editor.v2.common.Command;
import com.jecelyin.editor.v2.ui.activities.EditorActivity;
import com.jecelyin.editor.v2.ui.dialog.DocumentInfoDialog;
import com.jecelyin.editor.v2.ui.dialog.FinderDialog;
import com.jecelyin.editor.v2.ui.widget.menu.MenuDef;
import com.jecelyin.editor.v2.view.EditorView;

import org.gjt.sp.jedit.Catalog;
import org.gjt.sp.jedit.Mode;
import org.gjt.sp.jedit.syntax.ModeProvider;

import java.io.File;
import java.util.Locale;

/**
 * @author Jecelyin Peng <jecelyin@gmail.com>
 */
public class EditorDelegate implements TextWatcher {
    public final static String KEY_CLUSTER = "is_cluster";
    private static final String TAG = "EditorDelegate";
    EditAreaView mEditText;
    private Context mContext;
    private EditorView mEditorView;

    private Document mDocument;
    @NonNull
    private SavedState savedState;

    private int mOrientation;
    private boolean loaded = true;
    private int findResultsKeywordColor;

    public EditorDelegate(@NonNull SavedState ss) {
        savedState = ss;
    }

    public EditorDelegate(@NonNull File file, int offset, String encoding) {
        savedState = new SavedState();
        savedState.encoding = encoding;
        savedState.cursorOffset = offset;
        setCurrentFileToEdit(file);
    }

    private void setCurrentFileToEdit(File file) {
        savedState.file = file;
        savedState.title = savedState.file.getName();
    }

    void onLoadStart() {
        loaded = false;
        mEditText.setEnabled(false);
        mEditorView.setLoading(true);
    }

    void onLoadFinish() {
        mEditorView.setLoading(false);
        mEditText.setEnabled(true);
        mEditText.post(new Runnable() {
            @Override
            public void run() {
                if (savedState.cursorOffset < mEditText.getText().length())
                    mEditText.setSelection(savedState.cursorOffset);
            }
        });

        noticeDocumentChanged();
        loaded = true;
    }

    public Context getContext() {
        return mContext;
    }

    private EditorActivity getMainActivity() {
        return (EditorActivity) mContext;
    }

    public String getTitle() {
        return savedState.title;
    }

    public String getPath() {
        return mDocument == null ? savedState.file.getPath() : mDocument.getPath();
    }

    public String getEncoding() {
        return mDocument == null ? null : mDocument.getEncoding();
    }

    public String getText() {
        return mEditText.getText().toString();
    }

    public Editable getEditableText() {
        return mEditText.getText();
    }

    public EditAreaView getEditText() {
        return mEditText;
    }

    public void setEditorView(EditorView editorView) {
        mContext = editorView.getContext();
        mEditorView = editorView;
        mEditText = editorView.getEditText();
        mOrientation = mContext.getResources().getConfiguration().orientation;

        TypedArray a = mContext.obtainStyledAttributes(new int[]{R.attr.findResultsKeyword});
        findResultsKeywordColor = a.getColor(0, Color.BLACK);
        a.recycle();

        mDocument = new Document(mContext, this, savedState.file);
        mEditText.setReadOnly(Preferences.getInstance(mContext).isReadOnly());
        mEditText.setCustomSelectionActionModeCallback(new EditorSelectionActionModeCallback());

        if (savedState.editorState != null) {
            mDocument.onRestoreInstanceState(savedState);
            mEditText.onRestoreInstanceState(savedState.editorState);
        } else {
            mDocument.loadFile(savedState.file, savedState.encoding);
        }

        mEditText.addTextChangedListener(this);
        noticeDocumentChanged();
    }

    public void onDestroy() {
        mEditText.removeTextChangedListener(mDocument);
        mEditText.removeTextChangedListener(this);
    }


    public CharSequence getSelectedText() {
        return mEditText.hasSelection() ? mEditText.getEditableText().subSequence(mEditText.getSelectionStart(), mEditText.getSelectionEnd()) : "";
    }

    public boolean isChanged() {
        return mDocument != null && mDocument.isChanged();
    }

    public CharSequence getToolbarText() {
        String encode = mDocument == null ? "UTF-8" : mDocument.getEncoding();
        String fileMode = mDocument == null || mDocument.getModeName() == null ? "" : mDocument.getModeName();
        String title = getTitle();
        String changed = isChanged() ? "*" : "";
        String cursor = "";
        if (mEditText != null && mEditText.getLayout() != null && getCursorOffset() >= 0) {
            int cursorOffset = getCursorOffset();
            int line = mEditText.getLayout().getLineForOffset(cursorOffset);
            cursor += line + ":" + cursorOffset;
        }
        return String.format(Locale.US, "%s%s  \t|\t  %s \t %s \t %s", changed, title, encode, fileMode, cursor);
    }

    private void startSaveFileSelectorActivity() {
        if (mDocument != null) {
            getMainActivity().startPickPathActivity(mDocument.getPath(), mDocument.getEncoding());
        }
    }

    /**
     * Should be call in save as action
     *
     * @param file - new file to write
     */
    public void saveTo(File file, String encoding) {
        if (mDocument != null) {
            mDocument.saveTo(file, encoding == null ? mDocument.getEncoding() : encoding);
        }
    }

    public void save(boolean background) {
        mDocument.save(background, null);
    }


    public void addHighlight(int start, int end) {
        mEditText.getText().setSpan(new BackgroundColorSpan(findResultsKeywordColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        mEditText.setSelection(end, end);
    }

    public int getCursorOffset() {
        if (mEditText == null)
            return -1;
        return mEditText.getSelectionEnd();
    }

    public boolean doCommand(Command command) {
        if (mEditText == null)
            return false;
        boolean readonly = Preferences.getInstance(mContext).isReadOnly();
        switch (command.what) {
            case HIDE_SOFT_INPUT:
                mEditText.hideSoftInput();
                break;
            case SHOW_SOFT_INPUT:
                mEditText.showSoftInput();
                break;
            case UNDO:
                if (!readonly)
                    mEditText.undo();
                break;
            case REDO:
                if (!readonly)
                    mEditText.redo();
                break;
            case CUT:
                if (!readonly)
                    return mEditText.cut();
            case COPY:
                return mEditText.copy();
            case PASTE:
                if (!readonly)
                    return mEditText.paste();
            case SELECT_ALL:
                return mEditText.selectAll();
            case DUPLICATION:
                if (!readonly)
                    mEditText.duplication();
                break;
            case CONVERT_WRAP_CHAR:
                if (!readonly)
                    mEditText.convertWrapCharTo((String) command.object);
                break;
            case GOTO_INDEX:
                int col = command.args.getInt("col", -1);
                int line = command.args.getInt("line", -1);
                mEditText.requestFocus();
                mEditText.gotoLine(line, col);
                break;
            case GOTO_TOP:
                mEditText.gotoTop();
                break;
            case GOTO_END:
                mEditText.gotoEnd();
                break;
            case DOC_INFO:
                DocumentInfoDialog documentInfoDialog = new DocumentInfoDialog(mContext);
                documentInfoDialog.setDocument(mDocument);
                documentInfoDialog.setEditAreaView(mEditText);
                documentInfoDialog.setPath(mDocument.getPath());
                documentInfoDialog.show();
                break;
            case READONLY_MODE:
                Preferences preferences = Preferences.getInstance(mContext);
                boolean readOnly = preferences.isReadOnly();
                mEditText.setReadOnly(readOnly);
                break;
            case SAVE:
                if (!readonly)
                    mDocument.save(command.args.getBoolean(KEY_CLUSTER, false), (SaveListener) command.object);
                break;
            case SAVE_AS:
                startSaveFileSelectorActivity();
                break;
            case FIND:
                FinderDialog.showFindDialog(this);
                break;
            case HIGHLIGHT:
                String scope = (String) command.object;
                if (scope == null) {
                    Mode mode;
                    String firstLine = getEditableText().subSequence(0, Math.min(80, getEditableText().length())).toString();
                    if (TextUtils.isEmpty(mDocument.getPath()) || TextUtils.isEmpty(firstLine)) {
                        mode = ModeProvider.instance.getMode(Catalog.DEFAULT_MODE_NAME);
                    } else {
                        mode = ModeProvider.instance.getModeForFile(mDocument.getPath(), null, firstLine);
                    }

                    if (mode == null) {
                        mode = ModeProvider.instance.getMode(Catalog.DEFAULT_MODE_NAME);
                    }

                    scope = mode.getName();
                }
                mDocument.setMode(scope);
                break;
            case INSERT_TEXT:
                if (!readonly) {
                    int selStart = mEditText.getSelectionStart();
                    int selEnd = mEditText.getSelectionEnd();
                    if (selStart == -1 || selEnd == -1) {
                        mEditText.getText().insert(0, (CharSequence) command.object);
                    } else {
                        mEditText.getText().replace(selStart, selEnd, (CharSequence) command.object);
                    }
                }
                break;
            case RELOAD_WITH_ENCODING:
                reOpenWithEncoding((String) command.object);
                break;
            case FORWARD:
                mEditText.forwardLocation();
                break;
            case BACK:
                mEditText.backLocation();
                break;
            case REQUEST_FOCUS:
                mEditText.requestFocus();
                break;
            case HIGHLIGHT_ERROR:
                highlightError(command.args);
                break;
            case CLEAR_ERROR_SPAN:
                clearErrorSpan();
                break;
        }
        return true;
    }

    private void clearErrorSpan() {
        Editable editableText = mEditText.getEditableText();
        ErrorSpan[] spans = editableText.getSpans(0, mEditText.length(), ErrorSpan.class);
        for (ErrorSpan span : spans) {
            editableText.removeSpan(span);
        }
    }

    /**
     * Set {@link com.duy.ide.editor.span.ErrorSpan} from line:col to lineEnd:colEnd
     * If it hasn't end index, this method will be set span for all line
     *
     * @param args - contains four key
     */
    private void highlightError(Bundle args) {
        if (DLog.DEBUG) DLog.d(TAG, "highlightError() called with: args = [" + args + "]");
        int realLine = args.getInt("line", -1);
        int virtualLine = mEditText.realLineToVirtualLine(realLine);
        if (virtualLine != -1) { //found
            Editable editableText = mEditText.getEditableText();
            int startIndex;
            int endIndex;
            if (args.containsKey("lineEnd")) {
                int lineEnd = args.getInt("lineEnd");
                int colEnd = args.getInt("colEnd", 1);
                int colStart = args.getInt("col", 1);
                startIndex = mEditText.getCursorIndex(realLine, colStart).offset;
                endIndex = mEditText.getCursorIndex(lineEnd, colEnd).offset;

            } else {
                startIndex = mEditText.getLayout().getLineStart(virtualLine);
                endIndex = mEditText.getLayout().getLineEnd(virtualLine);
                //remove white space, tab or line terminate
                while (startIndex < endIndex) {
                    if (Character.isWhitespace(editableText.charAt(startIndex))) {
                        startIndex++;
                        continue;
                    }
                    if (Character.isWhitespace(editableText.charAt(endIndex - 1))) {
                        endIndex--;
                        continue;
                    }
                    break;
                }
            }
            ErrorSpan[] spans = editableText.getSpans(startIndex, endIndex, ErrorSpan.class);
            for (ErrorSpan span : spans) {
                editableText.removeSpan(span);
            }
            if (startIndex < endIndex) {
                editableText.setSpan(new ErrorSpan(Color.RED), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    private void reOpenWithEncoding(final String encoding) {
        final File file = mDocument.getFile();
        if (mDocument.isChanged()) {
            new AlertDialog.Builder(mContext)
                    .setTitle(R.string.document_changed)
                    .setMessage(R.string.give_up_document_changed_message)
                    .setPositiveButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            mDocument.loadFile(file, encoding);
                        }
                    })
                    .create()
                    .show();
            return;
        }
        mDocument.loadFile(file, encoding);
    }

    /**
     * This method will be called when document changed file
     */
    @MainThread
    void noticeDocumentChanged() {
        savedState.title = mDocument.getFile().getName();
        noticeMenuChanged();
    }

    @MainThread
    private void noticeMenuChanged() {
        EditorActivity editorActivity = (EditorActivity) this.mContext;
        editorActivity.setMenuStatus(R.id.action_save, isChanged() ? MenuDef.STATUS_NORMAL : MenuDef.STATUS_DISABLED);
        editorActivity.setMenuStatus(R.id.m_undo, mEditText != null && mEditText.canUndo() ? MenuDef.STATUS_NORMAL : MenuDef.STATUS_DISABLED);
        editorActivity.setMenuStatus(R.id.m_redo, mEditText != null && mEditText.canRedo() ? MenuDef.STATUS_NORMAL : MenuDef.STATUS_DISABLED);
        ((EditorActivity) mContext).getTabManager().onDocumentChanged();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        if (loaded) {
            noticeMenuChanged();
        }
    }

    @Nullable
    public String getLang() {
        if (mDocument == null) {
            return null;
        }
        return mDocument.getModeName();
    }

    private void convertSelectedText(int id) {
        if (mEditText == null || !mEditText.hasSelection()) {
            return;
        }

        int start = mEditText.getSelectionStart();
        int end = mEditText.getSelectionEnd();

        String selectedText = getEditableText().subSequence(start, end).toString();

        switch (id) {
            case R.id.m_convert_to_uppercase:
                selectedText = selectedText.toUpperCase();
                break;
            case R.id.m_convert_to_lowercase:
                selectedText = selectedText.toLowerCase();
                break;
        }
        getEditableText().replace(start, end, selectedText);
    }

    Parcelable onSaveInstanceState() {
        if (mDocument != null) {
            mDocument.onSaveInstanceState(savedState);
        }
        if (mEditText != null) {
            mEditText.setFreezesText(true);
            savedState.editorState = (BaseEditorView.SavedState) mEditText.onSaveInstanceState();
        }

        if (loaded && mDocument != null) {
            if (Preferences.getInstance(mContext).isAutoSave()) {
                int newOrientation = mContext.getResources().getConfiguration().orientation;
                if (mOrientation != newOrientation) {
                    DLog.d("current is screen orientation, discard auto save!");
                    mOrientation = newOrientation;
                } else {
                    mDocument.save(true, null);
                }
            }
        }

        return savedState;
    }

    public Document getDocument() {
        return mDocument;
    }

    public static class SavedState implements Parcelable {
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel source) {
                return new SavedState(source);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        int cursorOffset;
        int lineNumber;
        File file;
        String title;
        String encoding;
        String modeName;
        BaseEditorView.SavedState editorState;
        byte[] textMd5;
        int textLength;

        SavedState() {
        }

        SavedState(Parcel in) {
            this.cursorOffset = in.readInt();
            this.lineNumber = in.readInt();
            String file = in.readString();
            this.file = new File(file);
            this.title = in.readString();
            this.encoding = in.readString();
            this.modeName = in.readString();
            int hasState = in.readInt();
            if (hasState == 1) {
                this.editorState = in.readParcelable(BaseEditorView.SavedState.class.getClassLoader());
            }
            this.textMd5 = in.createByteArray();
            this.textLength = in.readInt();
        }


        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.cursorOffset);
            dest.writeInt(this.lineNumber);
            dest.writeString(this.file.getPath());
            dest.writeString(this.title);
            dest.writeString(this.encoding);
            dest.writeString(this.modeName);
            dest.writeInt(this.editorState == null ? 0 : 1);
            if (this.editorState != null) {
                dest.writeParcelable(this.editorState, flags);
            }
            dest.writeByteArray(this.textMd5);
            dest.writeInt(textLength);
        }
    }

    private class EditorSelectionActionModeCallback implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            final TypedArray arr = mContext.obtainStyledAttributes(
                    R.styleable.SelectionModeDrawables);

            boolean readOnly = Preferences.getInstance(mContext).isReadOnly();
            boolean selected = mEditText.hasSelection();
            if (selected) {
                menu.add(0, R.id.m_find_replace, 0, R.string.find).
                        setIcon(R.drawable.ic_find_replace_white_24dp).
                        setAlphabeticShortcut('f').
                        setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

                if (!readOnly) {
                    menu.add(0, R.id.m_convert_to_uppercase, 0, R.string.convert_to_uppercase)
                            .setIcon(R.drawable.m_uppercase)
                            .setAlphabeticShortcut('U')
                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

                    menu.add(0, R.id.m_convert_to_lowercase, 0, R.string.convert_to_lowercase)
                            .setIcon(R.drawable.m_lowercase)
                            .setAlphabeticShortcut('L')
                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
                }
            }

            if (!readOnly) {
                menu.add(0, R.id.m_duplication, 0, selected ? R.string.duplication_text : R.string.duplication_line)
                        .setIcon(R.drawable.ic_control_point_duplicate_white_24dp)
                        .setAlphabeticShortcut('L')
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
            }

            arr.recycle();
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.m_find_replace:
                    doCommand(new Command(Command.CommandEnum.FIND));
                    return true;
                case R.id.m_convert_to_uppercase:
                case R.id.m_convert_to_lowercase:
                    convertSelectedText(item.getItemId());
                    return true;
                case R.id.m_duplication:
                    mEditText.duplication();
                    return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {

        }
    }

}
