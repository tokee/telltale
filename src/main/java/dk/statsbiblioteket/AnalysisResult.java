/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket;

import java.io.File;

/**
 * Simple container for analysis result.
 */
public class AnalysisResult {
    private final File image;
    private final String testName;
    private final String analysis;

    public AnalysisResult(File image, String testName, String analysis) {
        this.image = image;
        this.testName = testName;
        this.analysis = analysis;
    }

    @Override
    public String toString() {
        return String.format("Analysis %s for image %s:\n%s", testName, image, analysis);
    }
}
