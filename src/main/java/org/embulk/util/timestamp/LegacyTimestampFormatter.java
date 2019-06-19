package org.embulk.util.timestamp;

import java.time.Instant;
import java.time.ZoneId;
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
    }

    LegacyTimestampFormatter(final String pattern, final ZoneId defaultZoneId) {
        this(pattern, defaultZoneId, 1970, 1, 1);
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
}
