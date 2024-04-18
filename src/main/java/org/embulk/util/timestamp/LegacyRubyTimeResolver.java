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
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQuery;
import java.time.temporal.ValueRange;
import java.util.Optional;
import org.embulk.util.rubytime.RubyChronoFields;
import org.embulk.util.rubytime.RubyDateTimeResolver;
import org.embulk.util.rubytime.RubyTemporalQueries;
import org.embulk.util.rubytime.RubyTemporalQueryResolver;

final class LegacyRubyTimeResolver extends RubyDateTimeResolver {
    LegacyRubyTimeResolver(
            final ZoneId defaultZoneId,
            final int defaultYear,
            final int defaultMonthOfYear,
            final int defaultDayOfMonth,
            final int defaultHourOfDay,
            final int defaultMinuteOfHour,
            final int defaultSecondOfMinute,
            final int defaultNanoOfSecond) {
        this.defaultZoneId = defaultZoneId;
        this.defaultYear = defaultYear;
        this.defaultMonthOfYear = defaultMonthOfYear;
        this.defaultDayOfMonth = defaultDayOfMonth;
        this.defaultHourOfDay = defaultHourOfDay;
        this.defaultMinuteOfHour = defaultMinuteOfHour;
        this.defaultSecondOfMinute = defaultSecondOfMinute;
        this.defaultNanoOfSecond = defaultNanoOfSecond;
    }

    /**
     * Resolves {@link java.time.temporal.TemporalAccessor}, date-time data parsed by {@link RubyDateTimeFormatter}, with a rule close to Ruby's {@code Time.strptime}.
     *
     * @param original  the original date-time data parsed by {@link RubyDateTimeFormatter}, not null
     * @return the resolved temporal object, not null
     */
    @Override
    public TemporalAccessor resolve(final TemporalAccessor original) {
        return new ResolvedFromInstant(original, this.toInstant(original));
    }

    /**
     * Creates a java.time.Instant instance in legacy Embulk's way from this RubyTimeParsed instance with ZoneId.
     */
    private final Instant toInstant(final TemporalAccessor original) {
        final String zoneName = original.query(RubyTemporalQueries.zone());

        if (original.isSupported(ChronoField.INSTANT_SECONDS) || original.isSupported(RubyChronoFields.INSTANT_MILLIS)) {
            final Instant instant;
            if (original.isSupported(RubyChronoFields.INSTANT_MILLIS)) {
                // INSTANT_MILLIS (%Q) is prioritized if exists.
                final long instantMillis = original.getLong(RubyChronoFields.INSTANT_MILLIS);
                instant = Instant.ofEpochMilli(instantMillis);
            } else {
                final long instantSeconds = original.getLong(ChronoField.INSTANT_SECONDS);
                instant = Instant.ofEpochSecond(instantSeconds);
            }

            if (!this.defaultZoneId.equals(ZoneOffset.UTC)) {
                // TODO: Warn that a default time zone is specified for epoch seconds.
            }
            if (zoneName != null) {
                // TODO: Warn that the epoch second has a time zone.
            }

            if (original.isSupported(ChronoField.NANO_OF_SECOND)) {
                // The fraction part is "added" to the epoch second in case both are specified.
                // irb(main):002:0> Time.strptime("1500000000.123456789", "%s.%N").nsec
                // => 123456789
                // irb(main):003:0> Time.strptime("1500000000456.111111111", "%Q.%N").nsec
                // => 567111111
                //
                // If "sec_fraction" is specified, the value is used like |Time.at(seconds, sec_fraction * 1000000)|.
                // https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/lib/time.rb?view=markup#l427
                //
                // |Time.at| adds "seconds" (the epoch) and "sec_fraction" (the fraction part) with scaling.
                // https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/time.c?view=markup#l2528
                //
                // It behaves the same even if "seconds" is specified as a Rational, not an Integer.
                // irb(main):004:0> Time.at(Rational(1500000000789, 1000), 100123).nsec
                // => 889123000
                final int nanoOfSecond = original.get(ChronoField.NANO_OF_SECOND);
                if (!instant.isBefore(Instant.EPOCH)) {
                    return instant.plusNanos(nanoOfSecond);
                } else {
                    // NANO_OF_SECOND is a "literal" fraction part of a second, by definition.
                    // It is because "%N" (NANO_OF_SECOND) is used for calendar date-time, not only for seconds since epoch.
                    //
                    // Date._strptime("2019-06-08 12:34:56.789", "%Y-%m-%d %H:%M:%S.%N")
                    // => {:year=>2019, :mon=>6, :mday=>8, :hour=>12, :min=>34, :sec=>56, :sec_fraction=>(789/1000)}
                    // Date._strptime("1960-06-08 12:34:56.789", "%Y-%m-%d %H:%M:%S.%N")
                    // => {:year=>1960, :mon=>6, :mday=>8, :hour=>12, :min=>34, :sec=>56, :sec_fraction=>(789/1000)}
                    //
                    // Date._strptime( "123.789", "%s.%N")
                    // => {:seconds=>123, :sec_fraction=>(789/1000)}
                    // Date._strptime("-123.789", "%s.%N")
                    // => {:seconds=>-123, :sec_fraction=>(789/1000)}
                    //
                    // Then, if second's integer part is negative, the fraction part must be considered as negative.
                    // The Ruby interpreter does the same.
                    //
                    // See: https://git.ruby-lang.org/ruby.git/tree/lib/time.rb?id=v2_6_3#n449
                    return instant.minusNanos(nanoOfSecond);
                }
            } else {
                return instant;
            }
        }

        final ZoneId zoneId;
        if (zoneName != null) {
            zoneId = LegacyDateTimeZones.toZoneId(zoneName);
            if (zoneId == null) {
                final Optional<String> originalText = Optional.ofNullable(original.query(RubyTemporalQueries.originalText()));
                throw new DateTimeParseException("Invalid time zone ID '" + zoneName + "'", originalText.orElse(""), 0);
            }
        } else {
            zoneId = this.defaultZoneId;
        }

        // Leap seconds are considered as 59 when Ruby converts them to epochs.
        //
        // RubyDateTimeFormatter#parse processes a leap second to be 59, and
        // sets TemporalAccessor#query(DateTimeFormatter.parsedLeapSecond()) to true.
        //
        // Then, parsedLeapSecond is just ignored here.
        final int secondOfMinute;
        if (original.isSupported(ChronoField.SECOND_OF_MINUTE)) {
            secondOfMinute = original.get(ChronoField.SECOND_OF_MINUTE);
        } else {
            secondOfMinute = 0;
        }

        // 24h is considered as 0h of the next day.
        //
        // RubyDateTimeFormatter#parse processes 24h to be 0h, and
        // sets TemporalAccessor#query(DateTimeFormatter.parsedExcessDays()) to 1 day.
        final int hour;
        final int excessDays;
        if (original.isSupported(ChronoField.HOUR_OF_DAY)) {
            hour = original.get(ChronoField.HOUR_OF_DAY);
            final Period parsedExcessDays = original.query(DateTimeFormatter.parsedExcessDays());
            if (parsedExcessDays == null || parsedExcessDays.isZero()) {
                excessDays = 0;
            } else if (parsedExcessDays.getDays() == 1
                       && parsedExcessDays.getMonths() == 0
                       && parsedExcessDays.getYears() == 0) {
                excessDays = 1;
            } else {
                throw new DateTimeParseException("Hour is not in the range of 0-24.", "", 0);
            }
        } else {
            hour = 0;
            excessDays = 0;
        }

        final int year;
        if (original.isSupported(ChronoField.YEAR)) {
            year = original.get(ChronoField.YEAR);
        } else {
            year = this.defaultYear;
        }

        final int minuteOfHour;
        if (original.isSupported(ChronoField.MINUTE_OF_HOUR)) {
            minuteOfHour = original.get(ChronoField.MINUTE_OF_HOUR);
        } else {
            minuteOfHour = this.defaultMinuteOfHour;
        }

        final int nanoOfSecond;
        if (original.isSupported(ChronoField.NANO_OF_SECOND)) {
            nanoOfSecond = original.get(ChronoField.NANO_OF_SECOND);
        } else {
            nanoOfSecond = this.defaultNanoOfSecond;
        }

        final ZonedDateTime datetime;
        // yday is more prioritized than mon/mday in Ruby's strptime.
        if (original.isSupported(ChronoField.DAY_OF_YEAR)) {
            datetime = ZonedDateTime
                    .of(year, 1, 1, hour, minuteOfHour, secondOfMinute, nanoOfSecond, zoneId)
                    .withDayOfYear(original.get(ChronoField.DAY_OF_YEAR))
                    .plusDays(excessDays);
        } else {
            // Ruby parses some invalid "dayOfMonth" that exceeds the largest day of the month, such as 2018-02-31.
            //
            // Embulk's legacy timestamp parser follows this Ruby's behavior for a while in v0.9 for compatibility.
            // Note that it will start rejecting exceeding dates even in v0.9 after a certain interval.
            //
            // TODO: Start to reject exceeding dates such as 2018-02-31.
            //
            // irb(main):002:0> Time.strptime("2018-02-31 00:00:00", "%Y-%m-%d %H:%M:%S")
            // => 2018-03-03 00:00:00 -0800
            //
            // irb(main):003:0> Time.strptime("2016-02-31 00:00:00", "%Y-%m-%d %H:%M:%S")
            // => 2016-03-02 00:00:00 -0800
            //
            // irb(main):004:0> Time.strptime("2018-11-31 00:00:00", "%Y-%m-%d %H:%M:%S")
            // => 2018-12-01 00:00:00 -0800
            //
            // irb(main):005:0> Time.strptime("2018-02-32 00:00:00", "%Y-%m-%d %H:%M:%S")
            // ArgumentError: invalid strptime format - `%Y-%m-%d %H:%M:%S'
            //         from /usr/local/Cellar/ruby/2.4.1_1/lib/ruby/2.4.0/time.rb:433:in `strptime'
            //         from (irb):2
            //         from /usr/local/bin/irb:11:in `<main>'
            //
            // irb(main):006:0> Time.strptime("2018-02-00 00:00:00", "%Y-%m-%d %H:%M:%S")
            // ArgumentError: invalid strptime format - `%Y-%m-%d %H:%M:%S'
            //         from /usr/local/Cellar/ruby/2.4.1_1/lib/ruby/2.4.0/time.rb:433:in `strptime'
            //         from (irb):4
            //         from /usr/local/bin/irb:11:in `<main>'

            final int monthOfYear;
            if (original.isSupported(ChronoField.MONTH_OF_YEAR)) {
                monthOfYear = original.get(ChronoField.MONTH_OF_YEAR);
            } else {
                monthOfYear = this.defaultMonthOfYear;
            }

            final int dayOfMonth;
            if (original.isSupported(ChronoField.DAY_OF_MONTH)) {
                dayOfMonth = original.get(ChronoField.DAY_OF_MONTH);
            } else {
                dayOfMonth = this.defaultDayOfMonth;
            }

            int updatedYear = year;
            int updatedMonthOfYear = monthOfYear;
            int updatedDayOfMonth = dayOfMonth;

            final int daysInMonth = monthDays(updatedYear, updatedMonthOfYear);
            if (daysInMonth < updatedDayOfMonth) {
                updatedMonthOfYear += 1;
                if (12 < updatedMonthOfYear) {
                    updatedMonthOfYear = 1;
                    updatedYear += 1;
                }
                updatedDayOfMonth = updatedDayOfMonth - daysInMonth;
            }

            datetime = ZonedDateTime.of(
                    updatedYear,
                    updatedMonthOfYear,
                    updatedDayOfMonth,
                    hour,
                    minuteOfHour,
                    secondOfMinute,
                    nanoOfSecond,
                    zoneId).plusDays(excessDays);
        }

        return datetime.toInstant();
    }

    private class ResolvedFromInstant implements TemporalAccessor, RubyTemporalQueryResolver {
        private ResolvedFromInstant(final TemporalAccessor original, final Instant resolvedInstant) {
            this.original = original;
            this.resolvedInstant = resolvedInstant;
            this.resolvedDateTime = OffsetDateTime.ofInstant(resolvedInstant, ZoneOffset.UTC);
        }

        @Override
        public long getLong(final TemporalField field) {
            if (this.resolvedInstant.isSupported(field)) {
                // Its own Instant is intentionally prioritized so that a query for Instant does not return
                // not SECONDS_SINCE_EPOCH + NANO_OF_SECOND unintentionally.
                // Its Instant may need to be MILLISECONDS_SINCE_EPOCH + NANO_OF_SECOND instead.
                return this.resolvedInstant.getLong(field);
            }
            return this.resolvedDateTime.getLong(field);
        }

        @Override
        public boolean isSupported(final TemporalField field) {
            if (this.resolvedInstant.isSupported(field)) {
                // Its own Instant is intentionally prioritized so that a query for Instant does not return
                // not SECONDS_SINCE_EPOCH + NANO_OF_SECOND unintentionally.
                // Its Instant may need to be MILLISECONDS_SINCE_EPOCH + NANO_OF_SECOND instead.
                return true;
            }
            return this.resolvedDateTime.isSupported(field);
        }

        @Override
        public <R> R query(final TemporalQuery<R> query) {
            if (RubyTemporalQueries.isSpecificQuery(query)) {
                // Some special queries are prioritized.
                final R resultOriginal = this.original.query(query);
                if (resultOriginal != null) {
                    return resultOriginal;
                }
            }
            final R resultFromResolvedInstant = this.resolvedInstant.query(query);
            if (resultFromResolvedInstant != null) {
                // Its own Instant is intentionally prioritized so that a query for Instant does not return
                // not SECONDS_SINCE_EPOCH + NANO_OF_SECOND unintentionally.
                // Its Instant may need to be MILLISECONDS_SINCE_EPOCH + NANO_OF_SECOND instead.
                return resultFromResolvedInstant;
            }
            return this.resolvedDateTime.query(query);
        }

        @Override
        public ValueRange range(final TemporalField field) {
            if (this.resolvedInstant.isSupported(field)) {
                // Its own Instant is intentionally prioritized so that a query for Instant does not return
                // not SECONDS_SINCE_EPOCH + NANO_OF_SECOND unintentionally.
                // Its Instant may need to be MILLISECONDS_SINCE_EPOCH + NANO_OF_SECOND instead.
                return this.resolvedInstant.range(field);
            }
            return this.resolvedDateTime.range(field);
        }

        @Override
        public String getOriginalText() {
            if (this.original instanceof RubyTemporalQueryResolver) {
                final RubyTemporalQueryResolver resolver = (RubyTemporalQueryResolver) this.original;
                return resolver.getOriginalText();
            }
            return null;
        }

        @Override
        public String getZone() {
            if (this.original instanceof RubyTemporalQueryResolver) {
                final RubyTemporalQueryResolver resolver = (RubyTemporalQueryResolver) this.original;
                return resolver.getZone();
            }
            return null;
        }

        @Override
        public String getLeftover() {
            if (this.original instanceof RubyTemporalQueryResolver) {
                final RubyTemporalQueryResolver resolver = (RubyTemporalQueryResolver) this.original;
                return resolver.getLeftover();
            }
            return null;
        }

        private final TemporalAccessor original;
        private final Instant resolvedInstant;
        private final OffsetDateTime resolvedDateTime;
    }

    /**
     * Returns the number of days in the given month of the given year.
     */
    private static int monthDays(final int year, final int monthOfYear) {
        if (((year % 4 == 0) && (year % 100 != 0)) || (year % 400 == 0)) {
            return leapYearMonthDays[monthOfYear - 1];
        } else {
            return commonYearMonthDays[monthOfYear - 1];
        }
    }

    /**
     * Numbers of days per month and year.
     */
    private static final int[] leapYearMonthDays = { 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
    private static final int[] commonYearMonthDays = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

    private final ZoneId defaultZoneId;
    private final int defaultYear;
    private final int defaultMonthOfYear;
    private final int defaultDayOfMonth;
    private final int defaultHourOfDay;
    private final int defaultMinuteOfHour;
    private final int defaultSecondOfMinute;
    private final int defaultNanoOfSecond;
}
