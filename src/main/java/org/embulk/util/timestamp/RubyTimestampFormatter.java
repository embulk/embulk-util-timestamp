package org.embulk.util.timestamp;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import org.embulk.util.rubytime.RubyDateTimeFormatter;
import org.embulk.util.rubytime.RubyDateTimeResolver;

class RubyTimestampFormatter extends TimestampFormatter {
    RubyTimestampFormatter(final String pattern, final ZoneOffset defaultZoneOffset) {
        if (defaultZoneOffset.equals(ZoneOffset.UTC)) {
            this.rubyFormatter = RubyDateTimeFormatter.ofPattern(pattern);
        } else {
            this.rubyFormatter = RubyDateTimeFormatter
                                         .ofPattern(pattern)
                                         .withResolver(RubyDateTimeResolver.withDefaultZoneOffset(defaultZoneOffset));
        }
        this.pattern = pattern;
        this.defaultZoneOffset = defaultZoneOffset;
    }

    @Override
    public final String format(final Instant instant) {
        if (instant == null) {
            throw new NullPointerException("instant is null.");
        }

        return this.rubyFormatter.formatWithZoneNameStyle(
                instant.atOffset(this.defaultZoneOffset),
                RubyDateTimeFormatter.ZoneNameStyle.SHORT);
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
    private final String pattern;
    private final ZoneOffset defaultZoneOffset;
}
