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

package android.core.widget.model;

/**
 * Created by Duy on 30-Apr-18.
 */

public class EditorIndex {
    public final int line;
    public final int col;
    public final int offset;

    public EditorIndex(int line, int col, int offset) {
        this.line = line;
        this.col = col;
        this.offset = offset;
    }
}
