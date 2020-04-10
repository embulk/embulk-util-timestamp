/*
 * Copyright 2020 The Embulk project
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.Collections;
import org.junit.jupiter.api.Test;

/**
 * TestTimestampFormatterFormat tests the formatting feature of {@link org.embulk.util.timestamp.TimestampFormatter}.
 */
public class TestTimestampFormatterFormat {
    @Test  // Imported from test_strftime in Ruby v2.6.3's /test/ruby/test_time.rb.
    public void test_strftime() {
        final Instant t2000 = Instant.ofEpochSecond(946684800);
        assertFormat("Sat", "%a", t2000);
        assertFormat("Sat", "ruby:%a", t2000);
        assertFormat("Sat", "java:EEE", t2000);
        assertFormat("Saturday", "%A", t2000);
        assertFormat("Saturday", "ruby:%A", t2000);
        assertFormat("Saturday", "java:EEEE", t2000);
        assertFormat("Jan", "%b", t2000);
        assertFormat("Jan", "ruby:%b", t2000);
        assertFormat("Jan", "java:MMM", t2000);
        assertFormat("January", "%B", t2000);
        assertFormat("January", "ruby:%B", t2000);
        assertFormat("January", "java:MMMM", t2000);
        assertFormat("Sat Jan  1 00:00:00 2000", "%c", t2000);
        assertFormat("Sat Jan  1 00:00:00 2000", "ruby:%c", t2000);
        assertFormat("Sat Jan 01 00:00:00 2000", "java:EEE MMM dd HH:mm:ss uuuu", t2000);
        assertFormat("01", "%d", t2000);
        assertFormat("01", "ruby:%d", t2000);
        assertFormat("01", "java:dd", t2000);
        assertFormat("00", "%H", t2000);
        assertFormat("00", "ruby:%H", t2000);
        assertFormat("00", "java:HH", t2000);
        assertFormat("12", "%I", t2000);
        assertFormat("12", "ruby:%I", t2000);
        assertFormat("12", "java:hh", t2000);
        assertFormat("001", "%j", t2000);
        assertFormat("001", "ruby:%j", t2000);
        assertFormat("001", "java:DDD", t2000);
        assertFormat("01", "%m", t2000);
        assertFormat("01", "ruby:%m", t2000);
        assertFormat("01", "java:MM", t2000);
        assertFormat("00", "%M", t2000);
        assertFormat("00", "ruby:%M", t2000);
        assertFormat("00", "java:mm", t2000);
        assertFormat("AM", "%p", t2000);
        assertFormat("AM", "ruby:%p", t2000);
        assertFormat("AM", "java:a", t2000);
        assertFormat("00", "%S", t2000);
        assertFormat("00", "ruby:%S", t2000);
        assertFormat("00", "java:ss", t2000);
        assertFormat("00", "%U", t2000);
        assertFormat("00", "ruby:%U", t2000);
        // No equivalent to Ruby "%U" in Java.
        assertFormat("00", "%W", t2000);
        assertFormat("00", "ruby:%W", t2000);
        // No equivalent to Ruby "%W" in Java.
        assertFormat("6", "%w", t2000);
        assertFormat("6", "ruby:%w", t2000);
        // No equivalent to Ruby "%w" in Java.
        assertFormat("01/01/00", "%x", t2000);
        assertFormat("01/01/00", "ruby:%x", t2000);
        assertFormat("01/01/00", "java:MM/dd/uu", t2000);
        assertFormat("00:00:00", "%X", t2000);
        assertFormat("00:00:00", "ruby:%X", t2000);
        assertFormat("00:00:00", "java:HH:mm:ss", t2000);
        assertFormat("00", "%y", t2000);
        assertFormat("00", "ruby:%y", t2000);
        assertFormat("00", "java:uu", t2000);
        assertFormat("2000", "%Y", t2000);
        assertFormat("2000", "ruby:%Y", t2000);
        assertFormat("2000", "java:uuuu", t2000);
        assertFormat("UTC", "%Z", t2000);
        assertFormat("UTC", "ruby:%Z", t2000);
        assertFormat("GMT", "java:O", t2000);
        assertFormat("%", "%%", t2000);
        assertFormat("%", "ruby:%%", t2000);
        assertFormat("%", "java:%", t2000);
        assertFormat("0", "%-S", t2000);
        assertFormat("0", "ruby:%-S", t2000);
        // No equivalent to Ruby "-" in Java.
        assertFormat("12:00:00 AM", "%r", t2000);
        assertFormat("12:00:00 AM", "ruby:%r", t2000);
        assertFormat("12:00:00 AM", "java:hh:mm:ss a", t2000);
        assertFormat("Sat 2000-01-01T00:00:00", "%3a %FT%T", t2000);
        assertFormat("Sat 2000-01-01T00:00:00", "ruby:%3a %FT%T", t2000);
        assertFormat("Sat 2000-01-01T00:00:00", "java:EEE uuuu-MM-dd'T'HH:mm:ss", t2000);

        assertFormat("", "", t2000);
        assertFormat("", "ruby:", t2000);
        assertFormat("", "java:", t2000);
        // assertFormat("foo\0bar\x0000\x0000\x0000", "foo\0bar\0%H\0%M\0%S", t2000);
        assertFormat(String.join("", Collections.nCopies(30, "foo")),
                     String.join("", Collections.nCopies(30, "foo")), t2000);
        assertFormat(String.join("", Collections.nCopies(30, "foo")),
                     "ruby:" + String.join("", Collections.nCopies(30, "foo")), t2000);
        assertFormat(String.join("", Collections.nCopies(30, "foo")),
                     "java:'" + String.join("", Collections.nCopies(30, "foo")) + "'", t2000);
    }

    @Test  // Imported from test_strftime_subsec in Ruby v2.6.3's /test/ruby/test_time.rb.
    public void test_strftime_subsec() {
        final Instant t = Instant.ofEpochSecond(946684800, 123456789);
        assertFormat("123", "%3N", t);
        assertFormat("123", "ruby:%3N", t);
        assertFormat("123", "java:SSS", t);
        assertFormat("123456", "%6N", t);
        assertFormat("123456", "ruby:%6N", t);
        assertFormat("123456", "java:SSSSSS", t);
        assertFormat("123456789", "%9N", t);
        assertFormat("123456789", "ruby:%9N", t);
        assertFormat("123456789", "java:SSSSSSSSS", t);
        assertFormat("123456789", "java:nnnnnnnnn", t);
        assertFormat("1234567890", "%10N", t);
        assertFormat("1234567890", "ruby:%10N", t);
        // No equivalent to Ruby "%10N" in Java.
        assertFormat("123456789", "%0N", t);
        assertFormat("123456789", "ruby:%0N", t);
        assertFormat("123456789", "java:n", t);
    }

    @Test  // Imported from test_strftime_sec in Ruby v2.6.3's /test/ruby/test_time.rb.
    public void test_strftime_sec() {
        final Instant t2000 = Instant.ofEpochSecond(946684800);
        assertFormat("000", "%3S", t2000);
        assertFormat("000", "ruby:%3S", t2000);
        // No equivalent to Ruby "%3S" in Java.
    }

    @Test  // Imported from test_strftime_seconds_from_epoch in Ruby v2.6.3's /test/ruby/test_time.rb.
    public void test_strftime_seconds_from_epoch() {
        // No equivalent to Ruby "%s" in Java.
        final Instant t = Instant.ofEpochSecond(946684800, 123456789);
        assertFormat("946684800", "%s", t);
        assertFormat("946684800", "ruby:%s", t);
    }

    @Test  // Imported from test_strftime_zone in Ruby v2.6.3's /test/ruby/test_time.rb.
    public void test_strftime_zone() {
        final Instant t = OffsetDateTime.of(2001, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC).toInstant();
        assertFormat("2001-10-01", "%F", t);
    }

    @Test  // Imported from test_strftime_flags in Ruby v2.6.3's /test/ruby/test_time.rb.
    public void test_strftime_flags() {
        final Instant t1 = OffsetDateTime.of(2001, 10, 1, 2, 0, 0, 0, ZoneOffset.UTC).toInstant();
        assertFormat("01", "%d", t1);
        assertFormat("01", "ruby:%d", t1);
        assertFormat("01", "%0d", t1);
        assertFormat("01", "ruby:%0d", t1);
        assertFormat(" 1", "%_d", t1);
        assertFormat(" 1", "ruby:%_d", t1);
        assertFormat(" 1", "%e", t1);
        assertFormat(" 1", "ruby:%e", t1);
        assertFormat("01", "%0e", t1);
        assertFormat("01", "ruby:%0e", t1);
        assertFormat(" 1", "%_e", t1);
        assertFormat(" 1", "ruby:%_e", t1);
        assertFormat("AM", "%p", t1);
        assertFormat("AM", "ruby:%p", t1);
        assertFormat("am", "%#p", t1);
        assertFormat("am", "ruby:%#p", t1);
        assertFormat("am", "%P", t1);
        assertFormat("am", "ruby:%P", t1);
        assertFormat("AM", "%#P", t1);
        assertFormat("AM", "ruby:%#P", t1);
        assertFormat("02", "%H", t1);
        assertFormat("02", "ruby:%H", t1);
        assertFormat("02", "%0H", t1);
        assertFormat("02", "ruby:%0H", t1);
        assertFormat(" 2", "%_H", t1);
        assertFormat(" 2", "ruby:%_H", t1);
        assertFormat("02", "%I", t1);
        assertFormat("02", "ruby:%I", t1);
        assertFormat("02", "%0I", t1);
        assertFormat("02", "ruby:%0I", t1);
        assertFormat(" 2", "%_I", t1);
        assertFormat(" 2", "ruby:%_I", t1);
        assertFormat(" 2", "%k", t1);
        assertFormat(" 2", "ruby:%k", t1);
        assertFormat("02", "%0k", t1);
        assertFormat("02", "ruby:%0k", t1);
        assertFormat(" 2", "%_k", t1);
        assertFormat(" 2", "ruby:%_k", t1);
        assertFormat(" 2", "%l", t1);
        assertFormat(" 2", "ruby:%l", t1);
        assertFormat("02", "%0l", t1);
        assertFormat("02", "ruby:%0l", t1);
        assertFormat(" 2", "%_l", t1);
        assertFormat(" 2", "ruby:%_l", t1);
        final Instant t2 = OffsetDateTime.of(2001, 10, 1, 14, 0, 0, 0, ZoneOffset.UTC).toInstant();
        assertFormat("PM", "%p", t2);
        assertFormat("PM", "ruby:%p", t2);
        assertFormat("pm", "%#p", t2);
        assertFormat("pm", "ruby:%#p", t2);
        assertFormat("pm", "%P", t2);
        assertFormat("pm", "ruby:%P", t2);
        assertFormat("PM", "%#P", t2);
        assertFormat("PM", "ruby:%#P", t2);
        assertFormat("14", "%H", t2);
        assertFormat("14", "ruby:%H", t2);
        assertFormat("14", "%0H", t2);
        assertFormat("14", "ruby:%0H", t2);
        assertFormat("14", "%_H", t2);
        assertFormat("14", "ruby:%_H", t2);
        assertFormat("02", "%I", t2);
        assertFormat("02", "ruby:%I", t2);
        assertFormat("02", "%0I", t2);
        assertFormat("02", "ruby:%0I", t2);
        assertFormat(" 2", "%_I", t2);
        assertFormat(" 2", "ruby:%_I", t2);
        assertFormat("14", "%k", t2);
        assertFormat("14", "ruby:%k", t2);
        assertFormat("14", "%0k", t2);
        assertFormat("14", "ruby:%0k", t2);
        assertFormat("14", "%_k", t2);
        assertFormat("14", "ruby:%_k", t2);
        assertFormat(" 2", "%l", t2);
        assertFormat(" 2", "ruby:%l", t2);
        assertFormat("02", "%0l", t2);
        assertFormat("02", "ruby:%0l", t2);
        assertFormat(" 2", "%_l", t2);
        assertFormat(" 2", "ruby:%_l", t2);
        assertFormat("MON", "%^a", t2);
        assertFormat("MON", "ruby:%^a", t2);
        assertFormat("OCT", "%^b", t2);
        assertFormat("OCT", "ruby:%^b", t2);

        final Instant t2000 = Instant.ofEpochSecond(946684800);
        assertFormat("UTC", "%^Z", t2000);
        assertFormat("UTC", "ruby:%^Z", t2000);
        assertFormat("utc", "%#Z", t2000);
        assertFormat("utc", "ruby:%#Z", t2000);
        assertFormat("SAT JAN  1 00:00:00 2000", "%^c", t2000);
        assertFormat("SAT JAN  1 00:00:00 2000", "ruby:%^c", t2000);
    }

    @Test  // Imported from test_strftime_invalid_flags in Ruby v2.6.3's /test/ruby/test_time.rb.
    public void test_strftime_invalid_flags() {
        final Instant t = OffsetDateTime.of(2001, 10, 1, 2, 0, 0, 0, ZoneOffset.UTC).toInstant();

        // prec after flag
        assertFormat("%4^p", "%4^p", t);
        assertFormat("%4^p", "ruby:%4^p", t);
    }

    @Test  // Imported from test_strftime_year in Ruby v2.6.3's /test/ruby/test_time.rb.
    public void test_strftime_year() {
        final Instant t1 = OffsetDateTime.of(1, 1, 4, 0, 0, 0, 0, ZoneOffset.UTC).toInstant();
        assertFormat("0001", "%Y", t1);
        assertFormat("0001", "ruby:%Y", t1);
        assertFormat("0001", "java:uuuu", t1);
        assertFormat("0001", "%G", t1);
        assertFormat("0001", "ruby:%G", t1);
        assertFormat("0001", "java:YYYY", t1);

        final Instant t2 = OffsetDateTime.of(0, 1, 4, 0, 0, 0, 0, ZoneOffset.UTC).toInstant();
        assertFormat("0000", "%Y", t2);
        assertFormat("0000", "ruby:%Y", t2);
        assertFormat("0000", "java:uuuu", t2);
        assertFormat("0000", "%G", t2);
        assertFormat("0000", "ruby:%G", t2);
        assertFormat("0000", "java:YYYY", t2);

        final Instant t3 = OffsetDateTime.of(-1, 1, 4, 0, 0, 0, 0, ZoneOffset.UTC).toInstant();
        assertFormat("-0001", "%Y", t3);
        assertFormat("-0001", "ruby:%Y", t3);
        assertFormat("-0001", "java:uuuu", t3);
        assertFormat("-0001", "%G", t3);
        assertFormat("-0001", "ruby:%G", t3);
        assertFormat("-0001", "java:YYYY", t3);

        final Instant t4 = OffsetDateTime.of(100000000, 1, 4, 0, 0, 0, 0, ZoneOffset.UTC).toInstant();
        assertFormat("<<100000000>>", "<<%Y>>", t4);
        assertFormat("<<100000000>>", "ruby:<<%Y>>", t4);
        assertFormat("<<0100000000>>", "<<%10Y>>", t4);
        assertFormat("<<0100000000>>", "ruby:<<%10Y>>", t4);
        assertFormat("<<0100000000>>", "<<%010Y>>", t4);
        assertFormat("<<0100000000>>", "ruby:<<%010Y>>", t4);
        assertFormat("<< 100000000>>", "<<%_10Y>>", t4);
        assertFormat("<< 100000000>>", "ruby:<<%_10Y>>", t4);
        assertFormat("<<+100000000>>", "java:'<<'uuuu'>>'", t4);
    }

    @Test  // Imported from test_strftime_weeknum in Ruby v2.6.3's /test/ruby/test_time.rb.
    public void test_strftime_weeknum() {
        final Instant t = OffsetDateTime.of(1970, 1, 18, 0, 0, 0, 0, ZoneOffset.UTC).toInstant();
        assertFormat("0", "%w", t);
        assertFormat("0", "ruby:%w", t);
        assertFormat("7", "%u", t);
        assertFormat("7", "ruby:%u", t);
        assertFormat("Sun", "java:E", t);
    }

    @Test  // Imported from test_strftime_ctrlchar in Ruby v2.6.3's /test/ruby/test_time.rb.
    public void test_strftime_ctrlchar() {
        final Instant t2000 = Instant.ofEpochSecond(946684800);
        assertFormat("\t", "%t", t2000);
        assertFormat("\t", "ruby:%t", t2000);
        assertFormat("\t", "java:'\t'", t2000);
        assertFormat("\t", "%0t", t2000);
        assertFormat("\t", "ruby:%0t", t2000);
        assertFormat("\t", "%1t", t2000);
        assertFormat("\t", "ruby:%1t", t2000);
        assertFormat("  \t", "%3t", t2000);
        assertFormat("  \t", "ruby:%3t", t2000);
        assertFormat("00\t", "%03t", t2000);
        assertFormat("00\t", "ruby:%03t", t2000);
        assertFormat("\n", "%n", t2000);
        assertFormat("\n", "ruby:%n", t2000);
        assertFormat("\n", "java:'\n'", t2000);
        assertFormat("\n", "%0n", t2000);
        assertFormat("\n", "ruby:%0n", t2000);
        assertFormat("\n", "%1n", t2000);
        assertFormat("\n", "ruby:%1n", t2000);
        assertFormat("  \n", "%3n", t2000);
        assertFormat("  \n", "ruby:%3n", t2000);
        assertFormat("00\n", "%03n", t2000);
        assertFormat("00\n", "ruby:%03n", t2000);
    }

    @Test  // Imported from test_strftime_weekflags in Ruby v2.6.3's /test/ruby/test_time.rb.
    public void test_strftime_weekflags() {
        final Instant t2000 = Instant.ofEpochSecond(946684800);
        assertFormat("SAT", "%#a", t2000);
        assertFormat("SAT", "ruby:%#a", t2000);
        assertFormat("SATURDAY", "%#A", t2000);
        assertFormat("SATURDAY", "ruby:%#A", t2000);
        assertFormat("JAN", "%#b", t2000);
        assertFormat("JAN", "ruby:%#b", t2000);
        assertFormat("JANUARY", "%#B", t2000);
        assertFormat("JANUARY", "ruby:%#B", t2000);
        assertFormat("JAN", "%#h", t2000);
        assertFormat("JAN", "ruby:%#h", t2000);

        final Instant t = OffsetDateTime.of(2008, 1, 4, 0, 0, 0, 0, ZoneOffset.UTC).toInstant();
        assertFormat("FRIDAY", "%#A", t);
        assertFormat("FRIDAY", "ruby:%#A", t);
    }

    @Test  // Imported from test_strftime_rational in Ruby v2.6.3's /test/ruby/test_time.rb.
    public void test_strftime_rational() {
        final Instant t1 = OffsetDateTime.of(2000, 3, 14, 6, 53, 58, 979323846, ZoneOffset.UTC).toInstant();  // Pi Day
        assertFormat("03/14/2000  6:53:58.97932384600000000000000000000", "%m/%d/%Y %l:%M:%S.%29N", t1);
        assertFormat("03/14/2000  6:53:58.97932384600000000000000000000", "ruby:%m/%d/%Y %l:%M:%S.%29N", t1);
        assertFormat("03/14/2000  6:53:58.9793238460", "%m/%d/%Y %l:%M:%S.%10N", t1);
        assertFormat("03/14/2000  6:53:58.9793238460", "ruby:%m/%d/%Y %l:%M:%S.%10N", t1);
        assertFormat("03/14/2000  6:53:58.979323846", "%m/%d/%Y %l:%M:%S.%9N", t1);
        assertFormat("03/14/2000  6:53:58.979323846", "ruby:%m/%d/%Y %l:%M:%S.%9N", t1);
        assertFormat("03/14/2000 06:53:58.979323846", "java:MM'/'dd'/'uuuu HH:mm:ss.SSSSSSSSS", t1);
        assertFormat("03/14/2000  6:53:58.97932384", "%m/%d/%Y %l:%M:%S.%8N", t1);
        assertFormat("03/14/2000  6:53:58.97932384", "ruby:%m/%d/%Y %l:%M:%S.%8N", t1);
        assertFormat("03/14/2000 06:53:58.97932384", "java:MM'/'dd'/'uuuu HH:mm:ss.SSSSSSSS", t1);

        final Instant t2 = OffsetDateTime.of(1592, 3, 14, 6, 53, 58, 979323846, ZoneOffset.UTC).toInstant();  // Pi Day
        assertFormat("03/14/1592  6:53:58.97932384600000000000000000000", "%m/%d/%Y %l:%M:%S.%29N", t2);
        assertFormat("03/14/1592  6:53:58.97932384600000000000000000000", "ruby:%m/%d/%Y %l:%M:%S.%29N", t2);
        assertFormat("03/14/1592  6:53:58.9793238460", "%m/%d/%Y %l:%M:%S.%10N", t2);
        assertFormat("03/14/1592  6:53:58.9793238460", "ruby:%m/%d/%Y %l:%M:%S.%10N", t2);
        assertFormat("03/14/1592  6:53:58.979323846", "%m/%d/%Y %l:%M:%S.%9N", t2);
        assertFormat("03/14/1592  6:53:58.979323846", "ruby:%m/%d/%Y %l:%M:%S.%9N", t2);
        assertFormat("03/14/1592 06:53:58.979323846", "java:MM'/'dd'/'uuuu HH:mm:ss.SSSSSSSSS", t2);
        assertFormat("03/14/1592  6:53:58.97932384", "%m/%d/%Y %l:%M:%S.%8N", t2);
        assertFormat("03/14/1592  6:53:58.97932384", "ruby:%m/%d/%Y %l:%M:%S.%8N", t2);
        assertFormat("03/14/1592 06:53:58.97932384", "java:MM'/'dd'/'uuuu HH:mm:ss.SSSSSSSS", t2);
    }

    @Test  // Imported from test_strftime_far_future in Ruby v2.6.3's /test/ruby/test_time.rb.
    public void test_strftime_far_future() {
        final Instant t = Instant.ofEpochSecond(3000000000L);
        assertFormat("3000000000", "%s", t);
        assertFormat("3000000000", "ruby:%s", t);
    }

    @Test  // Imported from test_strftime_too_wide in Ruby v2.6.3's /test/ruby/test_time.rb.
    public void test_strftime_too_wide() {
        assertEquals(8192, TimestampFormatter.builder("%8192z", true).build().format(Instant.now()).length());
        assertEquals(8192, TimestampFormatter.builder("ruby:%8192z", true).build().format(Instant.now()).length());
    }

    @Test  // Imported from test_strfimte_zoneoffset in Ruby v2.6.3's /test/ruby/test_time.rb.
    public void test_strfimte_zoneoffset() {
        final Instant t = Instant.ofEpochSecond(946684800);
        assertFormat("+0900", "%z", t, "+09:00:00");
        assertFormat("+0900", "ruby:%z", t, "+09:00:00");
        assertFormat("+0900", "java:xx", t, "+09:00:00");
        assertFormat("+09:00", "%:z", t, "+09:00:00");
        assertFormat("+09:00", "ruby:%:z", t, "+09:00:00");
        assertFormat("+09:00", "java:xxx", t, "+09:00:00");
        assertFormat("+09:00:00", "%::z", t, "+09:00:00");
        assertFormat("+09:00:00", "ruby:%::z", t, "+09:00:00");
        // No equivalent for "+09:00:00" in Java.
        assertFormat("+09", "%:::z", t, "+09:00:00");
        assertFormat("+09", "ruby:%:::z", t, "+09:00:00");
        assertFormat("+09", "java:x", t, "+09:00:00");
        assertFormat("+09:00", "java:xxxxx", t, "+09:00:00");

        assertFormat("+0900", "%z", t, "+09:00:01");
        assertFormat("+0900", "ruby:%z", t, "+09:00:01");
        assertFormat("+0900", "java:xx", t, "+09:00:01");
        assertFormat("+09:00", "%:z", t, "+09:00:01");
        assertFormat("+09:00", "ruby:%:z", t, "+09:00:01");
        assertFormat("+09:00", "java:xxx", t, "+09:00:01");
        assertFormat("+09:00:01", "%::z", t, "+09:00:01");
        assertFormat("+09:00:01", "ruby:%::z", t, "+09:00:01");
        // No equivalent for "+09:00:01" in Java.
        assertFormat("+09:00:01", "%:::z", t, "+09:00:01");
        assertFormat("+09:00:01", "ruby:%:::z", t, "+09:00:01");
        assertFormat("+09", "java:x", t, "+09:00:01");
        assertFormat("+09:00:01", "java:xxxxx", t, "+09:00:01");
    }

    @Test  // Imported from test_strftime_padding in Ruby v2.6.3's /test/ruby/test_time.rb.
    public void test_strftime_padding() {
        final Instant t = Instant.ofEpochSecond(946684800);
        assertFormat("+0900", "%z", t, "+09:00");
        assertFormat("+0900", "ruby:%z", t, "+09:00");
        assertFormat("+09:00", "%:z", t, "+09:00");
        assertFormat("+09:00", "ruby:%:z", t, "+09:00");
        assertFormat("      +900", "%_10z", t, "+09:00");
        assertFormat("      +900", "ruby:%_10z", t, "+09:00");
        assertFormat("+000000900", "%10z", t, "+09:00");
        assertFormat("+000000900", "ruby:%10z", t, "+09:00");
        assertFormat("     +9:00", "%_10:z", t, "+09:00");
        assertFormat("     +9:00", "ruby:%_10:z", t, "+09:00");
        assertFormat("+000009:00", "%10:z", t, "+09:00");
        assertFormat("+000009:00", "ruby:%10:z", t, "+09:00");
        assertFormat("  +9:00:00", "%_10::z", t, "+09:00");
        assertFormat("  +9:00:00", "ruby:%_10::z", t, "+09:00");
        assertFormat("+009:00:00", "%10::z", t, "+09:00");
        assertFormat("+009:00:00", "ruby:%10::z", t, "+09:00");
        assertFormat("+000000009", "%10:::z", t, "+09:00");
        assertFormat("+000000009", "ruby:%10:::z", t, "+09:00");
        assertFormat("-0500", "%z", t, "-05:00");
        assertFormat("-0500", "ruby:%z", t, "-05:00");
        assertFormat("-05:00", "%:z", t, "-05:00");
        assertFormat("-05:00", "ruby:%:z", t, "-05:00");
        assertFormat("      -500", "%_10z", t, "-05:00");
        assertFormat("      -500", "ruby:%_10z", t, "-05:00");
        assertFormat("-000000500", "%10z", t, "-05:00");
        assertFormat("-000000500", "ruby:%10z", t, "-05:00");
        assertFormat("     -5:00", "%_10:z", t, "-05:00");
        assertFormat("     -5:00", "ruby:%_10:z", t, "-05:00");
        assertFormat("-000005:00", "%10:z", t, "-05:00");
        assertFormat("-000005:00", "ruby:%10:z", t, "-05:00");
        assertFormat("  -5:00:00", "%_10::z", t, "-05:00");
        assertFormat("  -5:00:00", "ruby:%_10::z", t, "-05:00");
        assertFormat("-005:00:00", "%10::z", t, "-05:00");
        assertFormat("-005:00:00", "ruby:%10::z", t, "-05:00");
        assertFormat("-000000005", "%10:::z", t, "-05:00");
        assertFormat("-000000005", "ruby:%10:::z", t, "-05:00");
        assertFormat("      +036", "%_10z", t, "+00:36");
        assertFormat("      +036", "ruby:%_10z", t, "+00:36");
        assertFormat("+000000036", "%10z", t, "+00:36");
        assertFormat("+000000036", "ruby:%10z", t, "+00:36");
        assertFormat("     +0:36", "%_10:z", t, "+00:36");
        assertFormat("     +0:36", "ruby:%_10:z", t, "+00:36");
        assertFormat("+000000:36", "%10:z", t, "+00:36");
        assertFormat("+000000:36", "ruby:%10:z", t, "+00:36");
        assertFormat("  +0:36:00", "%_10::z", t, "+00:36");
        assertFormat("  +0:36:00", "ruby:%_10::z", t, "+00:36");
        assertFormat("+000:36:00", "%10::z", t, "+00:36");
        assertFormat("+000:36:00", "ruby:%10::z", t, "+00:36");
        assertFormat("+000000:36", "%10:::z", t, "+00:36");
        assertFormat("+000000:36", "ruby:%10:::z", t, "+00:36");
        assertFormat("      -055", "%_10z", t, "-00:55");
        assertFormat("      -055", "ruby:%_10z", t, "-00:55");
        assertFormat("-000000055", "%10z", t, "-00:55");
        assertFormat("-000000055", "ruby:%10z", t, "-00:55");
        assertFormat("     -0:55", "%_10:z", t, "-00:55");
        assertFormat("     -0:55", "ruby:%_10:z", t, "-00:55");
        assertFormat("-000000:55", "%10:z", t, "-00:55");
        assertFormat("-000000:55", "ruby:%10:z", t, "-00:55");
        assertFormat("  -0:55:00", "%_10::z", t, "-00:55");
        assertFormat("  -0:55:00", "ruby:%_10::z", t, "-00:55");
        assertFormat("-000:55:00", "%10::z", t, "-00:55");
        assertFormat("-000:55:00", "ruby:%10::z", t, "-00:55");
        assertFormat("-000000:55", "%10:::z", t, "-00:55");
        assertFormat("-000000:55", "ruby:%10:::z", t, "-00:55");
    }

    @Test  // Imported from test_strftime_invalid_modifier in Ruby v2.6.3's /test/ruby/test_time.rb.
    public void test_strftime_invalid_modifier() {
        final Instant t = Instant.ofEpochSecond(946684800);
        assertFormat("%:y", "%:y", t);  // invalid conversion after : modifier
        assertFormat("%:y", "ruby:%:y", t);  // invalid conversion after : modifier
        assertFormat("%:0z", "%:0z", t);  // flag after : modifier
        assertFormat("%:0z", "ruby:%:0z", t);  // flag after : modifier
        assertFormat("%:10z", "%:10z", t);  // prec after : modifier
        assertFormat("%:10z", "ruby:%:10z", t);  // prec after : modifier
        assertFormat("%Ob", "%Ob", t);  // invalid conversion after locale modifier
        assertFormat("%Ob", "ruby:%Ob", t);  // invalid conversion after locale modifier
        assertFormat("%Eb", "%Eb", t);  // invalid conversion after locale modifier
        assertFormat("%Eb", "ruby:%Eb", t);  // invalid conversion after locale modifier
        assertFormat("%O0y", "%O0y", t);  // flag after locale modifier
        assertFormat("%O0y", "ruby:%O0y", t);  // flag after locale modifier
        assertFormat("%E0y", "%E0y", t);  // flag after locale modifier
        assertFormat("%E0y", "ruby:%E0y", t);  // flag after locale modifier
        assertFormat("%O10y", "%O10y", t);  // prec after locale modifier
        assertFormat("%O10y", "ruby:%O10y", t);  // prec after locale modifier
        assertFormat("%E10y", "%E10y", t);  // prec after locale modifier
        assertFormat("%E10y", "ruby:%E10y", t);  // prec after locale modifier
    }

    @Test  // Imported from test_strftime_yearday_on_last_day_of_year in Ruby v2.6.3's /test/ruby/test_time.rb.
    public void test_strftime_yearday_on_last_day_of_year() {
        assertFormat("365", "%j", OffsetDateTime.of(2015, 12, 31, 0, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertFormat("365", "ruby:%j", OffsetDateTime.of(2015, 12, 31, 0, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertFormat("365", "java:DDD", OffsetDateTime.of(2015, 12, 31, 0, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertFormat("366", "%j", OffsetDateTime.of(2016, 12, 31, 0, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertFormat("366", "ruby:%j", OffsetDateTime.of(2016, 12, 31, 0, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertFormat("366", "java:DDD", OffsetDateTime.of(2016, 12, 31, 0, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertFormat("365", "%j", OffsetDateTime.of(2015, 12, 30, 20, 0, 0, 0, ZoneOffset.UTC).toInstant(), "+05:00");
        assertFormat("365", "ruby:%j", OffsetDateTime.of(2015, 12, 30, 20, 0, 0, 0, ZoneOffset.UTC).toInstant(), "+05:00");
        assertFormat("365", "java:DDD", OffsetDateTime.of(2015, 12, 30, 20, 0, 0, 0, ZoneOffset.UTC).toInstant(), "+05:00");
        assertFormat("366", "%j", OffsetDateTime.of(2016, 12, 30, 20, 0, 0, 0, ZoneOffset.UTC).toInstant(), "+05:00");
        assertFormat("366", "ruby:%j", OffsetDateTime.of(2016, 12, 30, 20, 0, 0, 0, ZoneOffset.UTC).toInstant(), "+05:00");
        assertFormat("366", "java:DDD", OffsetDateTime.of(2016, 12, 30, 20, 0, 0, 0, ZoneOffset.UTC).toInstant(), "+05:00");
        assertFormat("365", "%j", OffsetDateTime.of(2016, 1, 1, 1, 0, 0, 0, ZoneOffset.UTC).toInstant(), "-05:00");
        assertFormat("365", "ruby:%j", OffsetDateTime.of(2016, 1, 1, 1, 0, 0, 0, ZoneOffset.UTC).toInstant(), "-05:00");
        assertFormat("365", "java:DDD", OffsetDateTime.of(2016, 1, 1, 1, 0, 0, 0, ZoneOffset.UTC).toInstant(), "-05:00");
        assertFormat("366", "%j", OffsetDateTime.of(2017, 1, 1, 1, 0, 0, 0, ZoneOffset.UTC).toInstant(), "-05:00");
        assertFormat("366", "ruby:%j", OffsetDateTime.of(2017, 1, 1, 1, 0, 0, 0, ZoneOffset.UTC).toInstant(), "-05:00");
        assertFormat("366", "java:DDD", OffsetDateTime.of(2017, 1, 1, 1, 0, 0, 0, ZoneOffset.UTC).toInstant(), "-05:00");
    }

    private static void assertFormat(final String expected, final String format, final Instant instant, final String zone) {
        final TimestampFormatter formatter =
                TimestampFormatter.builder(format, true).setDefaultZoneFromString(zone).build();
        assertEquals(expected, formatter.format(instant));
    }

    private static void assertFormat(final String expected, final String format, final Instant instant) {
        final TimestampFormatter formatter = TimestampFormatter.builder(format, true).build();
        assertEquals(expected, formatter.format(instant));
    }
}
