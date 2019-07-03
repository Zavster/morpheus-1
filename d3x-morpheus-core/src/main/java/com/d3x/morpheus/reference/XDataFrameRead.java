/*
 * Copyright (C) 2014-2018 D3X Systems - All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.d3x.morpheus.reference;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import com.d3x.morpheus.csv.CsvSource;
import com.d3x.morpheus.csv.CsvSourceDefault;
import com.d3x.morpheus.frame.DataFrameRead;
import com.d3x.morpheus.util.Resource;

/**
 * The default implementation of the DataFrame read interface
 *
 * <p><strong>This is open source software released under the <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache 2.0 License</a></strong></p>
 *
 * @author  Xavier Witdouck
 */
class XDataFrameRead implements DataFrameRead {

    @Override
    public <R> CsvSource<R> csv(File file) {
        return new CsvSourceDefault<>(Resource.of(file));
    }

    @Override
    public <R> CsvSource<R> csv(URL url) {
        return new CsvSourceDefault<>(Resource.of(url));
    }

    @Override
    public <R> CsvSource<R> csv(InputStream is) {
        return new CsvSourceDefault<>(Resource.of(is));
    }

    @Override
    public <R> CsvSource<R> csv(String resource) {
        return new CsvSourceDefault<>(Resource.of(resource));
    }
}
