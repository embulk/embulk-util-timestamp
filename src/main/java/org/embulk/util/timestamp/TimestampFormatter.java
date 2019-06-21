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
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

/**
 * Formatter for parsing a date-time text.
 *
 * <p>The formatter is built with a predefined matching pattern, and used for
 * parsing date-time strings. Patterns include the Ruby style, the Java style,
 * and the legacy Embulk style ("legacy non-prefixed").
 *
 * <p>The Ruby style works like Ruby's
 * <a href="https://docs.ruby-lang.org/en/2.6.0/Time.html#method-c-strptime">
 * {@code Time.strptime}</a>. A Ruby-style pattern follows a prefix
 * {@code "ruby:"}, or built with {@link #builderWithRuby(String)}. For example:
 *
 * <pre>{@code TimestampFormatter formatter1 = TimestampFormatter.builder("ruby:%Y-%m-%d %H:%M:%S %Z").build();
 * Instant instant1 = formatter1.parse("2019-02-28 12:34:56 +09:00");
 * System.out.println(instant1);  // => "2019-02-28T03:34:56Z"
 *
 * // Same as formatter1 without "ruby:".
 * TimestampFormatter formatter2 = TimestampFormatter.builderWithRuby("%Y-%m-%d %H:%M:%S %Z").build();}</pre>
 *
 * <p>The Java style works like {@link java.time.format.DateTimeFormatter}.
 * A Java-style pattern follows a prefix {@code "java:"}, or built with
 * {@link #builderWithJava(String)}. For example:
 *
 * <pre>{@code TimestampFormatter formatter3 = TimestampFormatter.builder("java:uuuu-MM-dd HH:mm:ss XXXXX").build();
 * Instant instant3 = formatter3.parse("2019-02-28 12:34:56 +09:00");
 * System.out.println(instant3);  // => "2019-02-28T03:34:56Z"
 *
 * // Same as formatter3 without "java:".
 * TimestampFormatter formatter4 = TimestampFormatter.builderWithJava("uuuu-MM-dd HH:mm:ss XXXXX").build();}</pre>
 *
 * <p>The legacy Embulk style is here for backward compatibility, but it has
 * some problems in timezones, especially around daylight saving time. A
 * legacy Embulk-style pattern does not have any prefix, and must be built
 * with {@link #builder(String, boolean)} with {@code isLegacyEnabled = true}.
 * For example:
 *
 * <pre>{@code TimestampFormatter formatter5 = TimestampFormatter.builder("%Y-%m-%d %H:%M:%S %Z", true).build();
 * Instant instant5 = formatter5.parse("2019-02-28 12:34:56 +09:00");
 * System.out.println(instant5);  // => "2019-02-28T03:34:56Z"}</pre>
 */
public abstract class TimestampFormatter {
    TimestampFormatter() {}

    /**
     * Builds a {@link TimestampFormatter} instance with configurations.
     */
    public static final class Builder {
        private Builder(final Prefix prefix, final String pattern, final boolean isLegacyEnabled) {
            this.isLegacyEnabled = isLegacyEnabled;
            this.prefix = prefix;

            if (prefix == Prefix.NONE && !isLegacyEnabled) {
                throw new IllegalArgumentException("isLegacyEnabled must be true to specify a non-prefixed pattern.");
            }

            this.pattern = pattern;
            this.defaultZoneOffset = Optional.empty();
            this.defaultZoneId = Optional.empty();
            this.defaultYear = 1970;
            this.defaultMonthOfYear = 1;
            this.defaultDayOfMonth = 1;
        }

        private Builder(final String pattern, final boolean isLegacyEnabled) {
            this.isLegacyEnabled = isLegacyEnabled;

            if (pattern.startsWith("java:")) {
                this.prefix = Prefix.JAVA;
                this.pattern = pattern.substring(5);
            } else if (pattern.startsWith("ruby:")) {
                this.prefix = Prefix.RUBY;
                this.pattern = pattern.substring(5);
            } else if (isLegacyEnabled) {
                this.prefix = Prefix.NONE;
                this.pattern = pattern;
            } else {
                throw new IllegalArgumentException("isLegacyEnabled must be true to specify a non-prefixed pattern.");
            }

            this.defaultZoneOffset = Optional.empty();
            this.defaultZoneId = Optional.empty();
        }

        /**
         * Sets the default {@link java.time.ZoneOffset}.
         *
         * @param defaultZoneOffset  the default {@link java.time.ZoneOffset}
         * @return this
         */
        public Builder setDefaultZoneOffset(final ZoneOffset defaultZoneOffset) {
            this.defaultZoneOffset = Optional.of(defaultZoneOffset);
            return this;
        }

        /**
         * Sets the default {@link java.time.ZoneId}.
         *
         * <p>Setting {@link java.time.ZoneId} is available only for a legacy non-prefixed matching pattern.
         *
         * @param defaultZoneId  the default {@link java.time.ZoneId}
         * @return this
         * @throws java.lang.IllegalArgumentException  if called for a prefixed matching pattern
         */
        public Builder setDefaultZoneId(final ZoneId defaultZoneId) {
            if (this.prefix != Prefix.NONE) {
                throw new IllegalArgumentException("Pattern must be legacy non-prefixed to set default ZoneId.");
            }
            this.defaultZoneId = Optional.of(defaultZoneId);
            return this;
        }

        /**
         * Sets the default timezone parsed from a {@link java.lang.String}.
         *
         * <p>Only for a legacy non-prefixed matching pattern, the given {@link java.lang.String}
         * is parsed into {@link java.time.ZoneId}, which accepts a geographical region such as
         * {@code "America/Los_Angeles"} and {@code "Asia/Tokyo"}. Remember that timezones based
         * on geographical regions have problems, especially around daylight saving time, as
         * documented in {@link org.embulk.util.timestamp.LegacyDateTimeZones}.
         *
         * <p>If the pattern is prefix Ruby-style or Java-style, the given {@link java.lang.String}
         * is parsed into {@link java.time.ZoneOffset}, which is only a fixed offset. The parse is
         * performed with {@link java.time.ZoneOffset#of(String)}.
         *
         * @param defaultZoneString  a {@link java.lang.String} to be parsed into the default timezone
         * @return this
         * @throws java.lang.IllegalArgumentException  if called for a prefixed matching pattern
         */
        public Builder setDefaultZoneFromString(final String defaultZoneString) {
            if (this.prefix != Prefix.NONE) {
                return this.setDefaultZoneOffset(ZoneOffset.of(defaultZoneString));
            }
            return this.setDefaultZoneId(LegacyDateTimeZones.toZoneId(defaultZoneString));
        }

        /**
         * Sets the default date.
         *
         * <p>Setting a default date is available only for a legacy non-prefixed matching pattern.
         * If it is called for a prefixed Ruby-style or Java-style pattern, with a date which is
         * not 1970-01-01, it throws {@link java.lang.IllegalArgumentException}.
         *
         * <p>Calling it for a prefixed pattern is intentionally accepted with 1970-01-01 for
         * easier migration with compatibility from Embulk's own {@code TimestampParser}.
         *
         * @param defaultYear  the default year
         * @param defaultMonthOfYear  the default month of a year (1-12)
         * @param defaultDayOfMonth  the default day of a month (1-31)
         * @return this
         * @throws java.lang.IllegalArgumentException  if called for a prefixed matching pattern
         */
        public Builder setDefaultDate(final int defaultYear, final int defaultMonthOfYear, final int defaultDayOfMonth) {
            if (this.prefix != Prefix.NONE) {
                if (defaultYear != 1970 || defaultMonthOfYear != 1 || defaultDayOfMonth != 1) {
                    throw new IllegalArgumentException("Pattern must be legacy non-prefixed to set default date.");
                }
            } else {
                this.defaultYear = defaultYear;
                this.defaultMonthOfYear = defaultMonthOfYear;
                this.defaultDayOfMonth = defaultDayOfMonth;
            }
            return this;
        }

        /**
         * Sets the default date parsed from a {@link java.lang.String}.
         *
         * <p>Setting a default date is available only for a legacy non-prefixed matching pattern.
         * If it is called for a prefixed Ruby-style or Java-style pattern, with a date which is
         * not 1970-01-01, it throws {@link java.lang.IllegalArgumentException}.
         *
         * <p>Calling it for a prefixed pattern is intentionally accepted with 1970-01-01 for
         * easier migration with compatibility from Embulk's own {@code TimestampParser}.
         *
         * @param defaultDateString  the default date in String ({@code "YYYY-MM-DD"})
         * @return this
         * @throws java.lang.IllegalArgumentException  if called for a prefixed matching pattern
         */
        public Builder setDefaultDateFromString(final String defaultDateString) {
            final LocalDate date = LocalDate.from(DATE_FORMATTER.parse(defaultDateString));
            this.setDefaultDate(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
            return this;
        }

        /**
         * Builds {@link TimestampFormatter} from the configurations.
         *
         * @return the {@link TimestampFormatter} built
         * @throws java.lang.IllegalArgumentException  if invalid
         */
        public TimestampFormatter build() {
            if (this.prefix == Prefix.JAVA) {
                return new JavaTimestampFormatter(
                        pattern,
                        this.defaultZoneOffset.orElse(ZoneOffset.UTC));
            } else if (this.prefix == Prefix.RUBY) {
                return new RubyTimestampFormatter(
                        pattern,
                        this.defaultZoneOffset.orElse(ZoneOffset.UTC));
            }

            if (this.isLegacyEnabled) {
                if (this.defaultZoneOffset.isPresent() && this.defaultZoneId.isPresent()) {
                    throw new IllegalArgumentException("Both default ZoneId and ZoneOffset are set.");
                } else if (this.defaultZoneId.isPresent()) {
                    return new LegacyTimestampFormatter(
                            pattern,
                            this.defaultZoneId.get(),
                            this.defaultYear,
                            this.defaultMonthOfYear,
                            this.defaultDayOfMonth);
                } else {
                    return new LegacyTimestampFormatter(
                            pattern,
                            this.defaultZoneOffset.orElse(ZoneOffset.UTC),
                            this.defaultYear,
                            this.defaultMonthOfYear,
                            this.defaultDayOfMonth);
                }
            }

            throw new IllegalArgumentException("isLegacyEnabled must be true to specify a non-prefixed pattern.");
        }

        private final Prefix prefix;
        private final String pattern;
        private final boolean isLegacyEnabled;

        private Optional<ZoneOffset> defaultZoneOffset;
        private Optional<ZoneId> defaultZoneId;
        private int defaultYear;
        private int defaultMonthOfYear;
        private int defaultDayOfMonth;
    }

    private enum Prefix {
        RUBY,
        JAVA,
        NONE
    }

    /**
     * Creates a {@link TimestampFormatter.Builder} from a matching pattern.
     *
     * <p>It can create only for a prefixed matching pattern. If a legacy
     * non-prefixed pattern is required, call {@link #builder(String, boolean)}
     * with {@code isLegacyEnabled = true}.
     *
     * @param pattern  the matching pattern, which must start from {@code "ruby:"} or {@code "java:"}
     * @return the {@link TimestampFormatter.Builder} created
     */
    public static Builder builder(final String pattern) {
        return builder(pattern, false);
    }

    /**
     * Creates a {@link TimestampFormatter.Builder} from a matching pattern.
     *
     * @param pattern  the matching pattern, which may start from {@code "ruby:"} or {@code "java:"}
     * @param isLegacyEnabled  {@code true} if a legacy non-prefixed pattern is required
     * @return the {@link TimestampFormatter.Builder} created
     */
    public static Builder builder(final String pattern, final boolean isLegacyEnabled) {
        return new Builder(pattern, isLegacyEnabled);
    }

    /**
     * Creates a {@link TimestampFormatter.Builder} from a Java-style matching pattern.
     *
     * <p>It builds a Java-style {@link TimestampFormatter} without the prefix {@code "java:"}.
     *
     * @param pattern  the matching pattern, which is Java-style without the prefix {@code "java:"}
     * @return the {@link TimestampFormatter.Builder} created
     */
    public static Builder builderWithJava(final String pattern) {
        return new Builder(Prefix.JAVA, pattern, false);
    }

    /**
     * Creates a {@link TimestampFormatter.Builder} from a Ruby-style matching pattern.
     *
     * <p>It builds a Ruby-style {@link TimestampFormatter} without the prefix {@code "ruby:"}.
     *
     * @param pattern  the matching pattern, which is Ruby-style without the prefix {@code "ruby:"}
     * @return the {@link TimestampFormatter.Builder} created
     */
    public static Builder builderWithRuby(final String pattern) {
        return new Builder(Prefix.RUBY, pattern, false);
    }

    /**
     * Parses a date-time text into {@link java.time.Instant}.
     *
     * @param text  the text to parse, not null nor empty
     *
     * @return the parsed instantaneous point on the time-line, {@link java.time.Instant}, not null
     *
     * @throws java.time.format.DateTimeParseException  if unable to parse the requested result
     */
    public abstract Instant parse(final String text);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("u-M-d", Locale.ROOT);
}
