package org.embulk.util.timestamp;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import org.embulk.util.rubytime.RubyDateTimeFormatter;

class LegacyTimestampFormatter extends TimestampFormatter {
    LegacyTimestampFormatter(
            final String pattern,
            final ZoneId defaultZoneId,
            final int defaultYear,
            final int defaultMonthOfYear,
            final int defaultDayOfMonth) {
        final LegacyRubyTimeResolver resolver = new LegacyRubyTimeResolver(
                defaultZoneId,
                defaultYear,
                defaultMonthOfYear,
                defaultDayOfMonth,
                0,
                0,
                0,
                0);
        this.rubyFormatter = RubyDateTimeFormatter.ofPattern(pattern).withResolver(resolver);
        this.defaultZoneId = defaultZoneId;
        this.normalizedDefaultZoneId = defaultZoneId.normalized();
    }

    LegacyTimestampFormatter(final String pattern, final ZoneId defaultZoneId) {
        this(pattern, defaultZoneId, 1970, 1, 1);
    }

    @Override
    public final String format(final Instant instant) {
        if (instant == null) {
            throw new NullPointerException("instant is null.");
        }

        if (this.normalizedDefaultZoneId instanceof ZoneOffset) {
            return this.rubyFormatter.format(instant.atOffset((ZoneOffset) this.normalizedDefaultZoneId));
        }
        return this.rubyFormatter.format(instant.atZone(this.defaultZoneId));
    }

    @Override
    public final Instant parse(final String text) {
        if (text == null) {
            throw new DateTimeParseException("text is null.", text, 0, new NullPointerException());
        } else if (text.isEmpty()) {
            throw new DateTimeParseException("text is empty.", text, 0);
        }

        final TemporalAccessor parsed = this.rubyFormatter.parse(text);
        return Instant.from(parsed);
    }

    private final RubyDateTimeFormatter rubyFormatter;
    private final ZoneId defaultZoneId;
    private final ZoneId normalizedDefaultZoneId;
}
