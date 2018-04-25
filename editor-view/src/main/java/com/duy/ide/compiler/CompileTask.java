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

package com.duy.ide.compiler;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.duy.ide.compiler.shell.ShellResult;

import java.io.File;

/**
 * Created by Duy on 25-Apr-18.
 */

public class CompileTask extends AsyncTask<Void, Void, ShellResult> {
    private Context mContext;
    private INativeCompiler mCompiler;
    private File[] mFiles;
    @Nullable
    private ICompileManager mCompileManager;

    public CompileTask(Context context,
                       INativeCompiler compiler,
                       @NonNull File[] files,
                       @Nullable ICompileManager compileManager) {
        mContext = context;
        mCompiler = compiler;
        mFiles = files;
        mCompileManager = compileManager;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (mCompileManager != null) {
            mCompileManager.onPrepareCompile();
        }
    }

    @Override
    protected ShellResult doInBackground(Void... voids) {
        return mCompiler.compile(mFiles);
    }

    @Override
    protected void onPostExecute(ShellResult shellResult) {
        super.onPostExecute(shellResult);
        if (mCompileManager == null) {
            return;
        }
        if (shellResult.getResultCode() == 0) {
            mCompileManager.onCompileSuccess(shellResult);
        } else {
            mCompileManager.onCompileFailed(shellResult);
        }
    }
}