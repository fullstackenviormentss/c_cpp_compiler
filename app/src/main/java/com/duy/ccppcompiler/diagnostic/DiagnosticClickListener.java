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

package com.duy.ccppcompiler.diagnostic;

import android.view.View;

import com.duy.ccppcompiler.compiler.diagnostic.Diagnostic;
import com.duy.ccppcompiler.compiler.diagnostic.suggestion.ISuggestion;

/**
 * Created by Duy on 28-Apr-18.
 */

public interface DiagnosticClickListener {
    void onDiagnosisClick(Diagnostic diagnostic, View view);

    void onSuggestionClick(View v, Diagnostic diagnostic, ISuggestion suggestion);
}
