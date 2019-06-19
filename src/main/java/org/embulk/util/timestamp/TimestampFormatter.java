/*
 * Copyright 2019 The Embulk project
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

package org.embulk.util.timestamp;

import java.time.Instant;

/**
 * Formatter for printing and parsing date-time strings.
 */
public abstract class TimestampFormatter {
    /**
     * Parses a timestamp string into java.time.Instant.
     *
     * @param text  the text to parse, not null
     *
     * @return the parsed instantaneous point on the time-line, {@link java.time.Instant}, not null
     *
     * @throws java.time.format.DateTimeParseException  if unable to parse the requested result
     */
    public abstract Instant parse(final String text);
}
