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

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.embulk.util.rubytime.RubyDateTimeZones;

/**
 * This class contains a {@code static} method to interpret a timezone string
 * in the historical manner of legacy Embulk.
 *
 * <h3>American timezones</h3>
 *
 * <p>Legacy Embulk's {@code org.embulk.spi.time.TimestampParser} starts interpreting a timezone
 * string from Joda-Time's {@link org.joda.time.format.DateTimeFormat} like below. It was before
 * straightforward {@link org.joda.time.DateTimeZone#forID(java.lang.String) DateTimeZone#forID}
 * and {@link org.joda.time.DateTimeZone#getAvailableIDs() DateTimeZone#getAvailableIDs}.
 *
 * <pre>{@code DateTimeFormat.forPattern("z").parseMillis("PDT")}</pre>
 *
 * <p>It had been in Embulk since
 * <a href="https://github.com/embulk/embulk/commit/b97954a5c78397e1269bbb6979d6225dfceb4e05#diff-68c6408e7c936d783784945d87fb251bR49">
 * {@code v0.1.0}</a> until
 * <a href="https://github.com/embulk/embulk/blob/v0.8.20/embulk-core/src/main/java/org/embulk/spi/time/TimestampFormat.java#L40-L70">
 * {@code v0.8.20}</a>. <a href="https://github.com/embulk/embulk/issues/860">GitHub Issue #860</a>
 * describes the details.
 *
 * <p>It unfortunately caused some wrong conversions for some American timezones: {@code "EST"},
 * {@code "EDT"}, {@code "CST"}, {@code "CDT"}, {@code "MST"}, {@code "MDT"}, {@code "PST"}, and
 * {@code "PDT"}. For example, {@code "PDT"} should be recognized as {@code "-07:00"} because
 * {@code "PDT"} was a daylight saving time. But, {@code "PDT"} was actually recognized as
 * {@code "-08:00"} along with {@code "PST"}.
 *
 * <p>It was because {@code "PDT"} and {@code "PST"} were aliases of {@code "America/Los_Angeles"}
 * in Joda-Time, not aliases of a fixed offset. Joda-Time originally intends to recognize both
 * {@code "PDT"} and {@code "PST"} to be aligned with the date-time with the timezone. For example,
 * {@code "2017-08-20 12:34:56 PST"} goes to {@code "2017-08-20 12:34:56 America/Los_Angeles"},
 * and this {@code "America/Los_Angeles"} is recognized as {@code "-07:00"} because the date was
 * in the daylight saving time. Same for {@code "2017-08-20 12:34:56 PDT"}. This {@code "PDT"} is
 * eventually recognized as the same {@code "-07:00"} because of the date as well.
 *
 * <p>However, {@code DateTimeFormat.forPattern("z").parseMillis("PDT")} was always converted to
 * {@code "-08:00"} because the string, only {@code "PDT"}, does not have any date information.
 * Legacy Embulk never recognized {@code "PDT"} as {@code "-07:00"} even for summer as a result.
 *
 * <p>JFYI, the set of special timezones comes from
 * <a href="https://github.com/JodaOrg/joda-time/blob/v2.9.2/src/main/java/org/joda/time/DateTimeUtils.java#L432-L448">
 * {@code DateTimeUtils#buildDefaultTimeZoneNames}</a>.
 *
 * <h3>"HST" and "ROC"</h3>
 *
 * <p>Embulk replaced Joda-Time into {@linkplain java.time Java Date and Time API} when upgraded
 * to Embulk v0.9. It started to use {@link java.time.ZoneId#of(String, java.util.Map) ZoneId.of}
 * instead of {@link org.joda.time.DateTimeZone#forID(String) DateTimeZone.forId} at that time.
 *
 * <p>{@link java.time.ZoneId#of(String)} does not recognize {@code "HST"} nor {@code "ROC"} that
 * are recognized by {@link org.joda.time.DateTimeZone#forID(String)}.
 *
 * <p>Available timezones in Joda-Time are described in
 * <a href="http://joda-time.sourceforge.net/timezones.html">
 * Joda-Time - Java date and time API - Time Zones</a>.
 *
 * @see org.joda.time.DateTimeUtils#getDefaultTimeZoneNames()
 */
public final class LegacyDateTimeZones {
    private LegacyDateTimeZones() {
        // No instantiation.
    }

    /**
     * Converts a timezone string to {@link java.time.ZoneId} in legacy Embulk's manner.
     *
     * <p>It recognizes a timezone string in the following priority.
     *
     * <ol>
     * <li>{@code "Z"} is always recognized as UTC in the highest priority.
     * <li>Some special timezone strings are recognized by
     * {@link java.time.ZoneId#of(String, java.util.Map) ZoneId.of(String, Map)}
     * with a predefined alias map like below for legacy historical reasons.
     * <ul>
     * <li>{@code "EST"} is recognized as {@code "-05:00"}.
     * <li>{@code "EDT"} is recognized as {@code "-05:00"}, too.
     * <li>{@code "CST"} is recognized as {@code "-06:00"}.
     * <li>{@code "CDT"} is recognized as {@code "-06:00"}, too.
     * <li>{@code "MST"} is recognized as {@code "-07:00"}.
     * <li>{@code "MDT"} is recognized as {@code "-07:00"}, too.
     * <li>{@code "PST"} is recognized as {@code "-08:00"}.
     * <li>{@code "PDT"} is recognized as {@code "-08:00"}, too.
     * <li>{@code "HST"} is recognized as {@code "-10:00"}.
     * <li>{@code "ROC"} is recognized as the same as {@code "Asia/Taipei"}.
     * </ul>
     * <li>Otherwise, the given timezone string is recognized as Ruby-compatible zone tab.
     * <li>If none of the above rules does not recognize the given timezone string, it returns {@code null}.
     * </ol>
     *
     * <p>Some of its time offset transition (e.g. daylight saving time) can be different from
     * Embulk's actual historical transitions. But, the difference comes from their tz database
     * version difference. The difference should be acceptable as users should expect tz database
     * can be updated anytime.
     *
     * @param zoneName  a timezone string
     * @return a {@link java.time.ZoneId} converted
     */
    public static ZoneId toZoneId(final String zoneName) {
        if (zoneName == null) {
            return null;
        }

        if (zoneName.equals("Z")) {
            return ZoneOffset.UTC;
        }

        try {
            return ZoneId.of(zoneName, ALIASES);  // Is never returns null unless Exception is thrown.
        } catch (final DateTimeException ex) {
            // Fallback to Ruby's Date._strptime.
            final int offsetInSeconds = RubyDateTimeZones.toOffsetInSeconds(zoneName);
            if (offsetInSeconds != Integer.MIN_VALUE) {
                return ZoneOffset.ofTotalSeconds(offsetInSeconds);
            }
        }
        return null;
    }

    static {
        final HashMap<String, String> aliases = new HashMap<>();
        aliases.put("EST", "-05:00");
        aliases.put("EDT", "-05:00");
        aliases.put("CST", "-06:00");
        aliases.put("CDT", "-06:00");
        aliases.put("MST", "-07:00");
        aliases.put("MDT", "-07:00");
        aliases.put("PST", "-08:00");
        aliases.put("PDT", "-08:00");

        aliases.put("HST", "-10:00");
        aliases.put("ROC", "Asia/Taipei");

        ALIASES = Collections.unmodifiableMap(aliases);
    }

    private static final Map<String, String> ALIASES;
}
