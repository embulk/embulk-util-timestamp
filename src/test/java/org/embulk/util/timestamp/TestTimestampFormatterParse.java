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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import org.junit.jupiter.api.Test;

/**
 * TestTimestampParser tests org.embulk.spi.time.TimestampParser.
 *
 * Some test cases are imported from Ruby v2.3.1's test/date/test_date_strptime.rb. See its COPYING for license.
 *
 * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/test/date/test_date_strptime.rb?view=markup">test/date/test_date_strptime.rb</a>
 * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/COPYING?view=markup">COPYING</a>
 */
public class TestTimestampFormatterParse {
    @Test  // Imported from test__strptime__3 in Ruby v2.3.1's test/date/test_date_strptime.rb.
    public void testJavaIso8601() {
        testJavaToParse("2001-02-03", "yyyy-MM-dd", 981158400L);
        testJavaToParse("2001-02-03", "uuuu-MM-dd", 981158400L);

        // "Java" timestamp parser does not accept second = 60 for the time being.

        testJavaToParse("2001-02-03T23:59:59", "yyyy-MM-dd'T'HH:mm:ss", 981244799L);
        testJavaToParse("2001-02-03T23:59:59", "uuuu-MM-dd'T'HH:mm:ss", 981244799L);
        testJavaToParse("2001-02-03T23:59:59+09:00", "yyyy-MM-dd'T'HH:mm:ssXXXXX", 981212399L);
        testJavaToParse("2001-02-03T23:59:59+09:00", "uuuu-MM-dd'T'HH:mm:ssXXXXX", 981212399L);

        // "yyyy" is Year of Era, which does not accept negative years for AD.
        failJavaToParse("-2001-02-03T23:59:59+09:00", "yyyy-MM-dd'T'HH:mm:ssXXXXX");
        testJavaToParse("-2001-02-03T23:59:59+09:00", "uuuu-MM-dd'T'HH:mm:ssXXXXX", -125309754001L);

        testJavaToParse("+012345-02-03T23:59:59+09:00", "yyyy-MM-dd'T'HH:mm:ssXXXXX", 327406287599L);
        testJavaToParse("+012345-02-03T23:59:59+09:00", "uuuu-MM-dd'T'HH:mm:ssXXXXX", 327406287599L);

        // "yyyy" is Year of Era, which does not accept negative years for AD.
        failJavaToParse("-012345-02-03T23:59:59+09:00", "yyyy-MM-dd'T'HH:mm:ssXXXXX");
        testJavaToParse("-012345-02-03T23:59:59+09:00", "uuuu-MM-dd'T'HH:mm:ssXXXXX", -451734829201L);
    }

    @Test  // Imported from test__strptime__3 in Ruby v2.3.1's test/date/test_date_strptime.rb.
    public void testRuby__strptime__3_iso8601() {
        // "Ruby" timestamp parser takes second = 60 as next second.

        testRubyToParse("2001-02-03", "%Y-%m-%d", 981158400L);
        testRubyToParse("2001-02-03T23:59:60", "%Y-%m-%dT%H:%M:%S", 981244800L);
        testRubyToParse("2001-02-03T23:59:60+09:00", "%Y-%m-%dT%H:%M:%S%Z", 981212400L);
        testRubyToParse("-2001-02-03T23:59:60+09:00", "%Y-%m-%dT%H:%M:%S%Z", -125309754000L);
        testRubyToParse("+012345-02-03T23:59:60+09:00", "%Y-%m-%dT%H:%M:%S%Z", 327406287600L);
        testRubyToParse("-012345-02-03T23:59:60+09:00", "%Y-%m-%dT%H:%M:%S%Z", -451734829200L);
    }

    @Test  // Imported from test__strptime__3 in Ruby v2.3.1's test/date/test_date_strptime.rb.
    public void test__strptime__3_iso8601() {
        testToParse("2001-02-03", "%Y-%m-%d", 981158400L);
        testToParse("2001-02-03T23:59:60", "%Y-%m-%dT%H:%M:%S", 981244799L);
        testToParse("2001-02-03T23:59:60+09:00", "%Y-%m-%dT%H:%M:%S%Z", 981212399L);
        testToParse("-2001-02-03T23:59:60+09:00", "%Y-%m-%dT%H:%M:%S%Z", -125309754001L);
        testToParse("+012345-02-03T23:59:60+09:00", "%Y-%m-%dT%H:%M:%S%Z", 327406287599L);
        testToParse("-012345-02-03T23:59:60+09:00", "%Y-%m-%dT%H:%M:%S%Z", -451734829201L);
    }

    @Test  // Imported from test__strptime__3 in Ruby v2.3.1's test/date/test_date_strptime.rb.
    public void testJavaAsctime() {
        testJavaToParse("Thu Jul 29 14:47:19 1999", "EEE MMM dd HH:mm:ss uuuu", 933259639L);

        // JFYI: Jul 29 -1999 is Sunday, not Thursday.
        testJavaToParse("Sun Jul 29 14:47:19 -1999", "EEE MMM dd HH:mm:ss uuuu", -125231389961L);
    }

    @Test  // Imported from test__strptime__3 in Ruby v2.3.1's test/date/test_date_strptime.rb.
    public void test__strptime__3_ctime3_asctime3() {
        testToParse("Thu Jul 29 14:47:19 1999", "%c", 933259639L);
        testToParse("Thu Jul 29 14:47:19 -1999", "%c", -125231389961L);
    }

    @Test  // Imported from test__strptime__3 in Ruby v2.3.1's test/date/test_date_strptime.rb.
    public void testJavaTimeZones() {
        testJavaToParse("Thu Jul 29 16:39:41 -05:00 1999", "EEE MMM dd HH:mm:ss XXXXX uuuu", 933284381L);

        // The short time zone IDs "EST", "MET", "AMT", "AST", and "DST" are not accepted in Java parser.
        failJavaToParse("Thu Jul 29 16:39:41 EST 1999", "EEE MMM dd HH:mm:ss zzz uuuu");
        failJavaToParse("Thu Jul 29 16:39:41 MET DST 1999", "EEE MMM dd HH:mm:ss zzz uuuu");
        failJavaToParse("Thu Jul 29 16:39:41 AMT 1999", "EEE MMM dd HH:mm:ss zzz uuuu");
        failJavaToParse("Thu Jul 29 16:39:41 AMT -1999", "EEE MMM dd HH:mm:ss zzz uuuu");
        failJavaToParse("Thu Jul 29 16:39:41 AST 1999", "EEE MMM dd HH:mm:ss zzz uuuu");
        failJavaToParse("Thu Jul 29 16:39:41 AST -1999", "EEE MMM dd HH:mm:ss zzz uuuu");

        // All "GMT", "GMT+..." and "GMT-..." are not accepted in Java parser.
        failJavaToParse("Thu Jul 29 16:39:41 GMT 1999", "EEE MMM dd HH:mm:ss zzz uuuu");
        failJavaToParse("Thu Jul 29 16:39:41 GMT+09 1999", "EEE MMM dd HH:mm:ss zzz uuuu");
        failJavaToParse("Thu Jul 29 16:39:41 GMT+0908 1999", "EEE MMM dd HH:mm:ss zzz uuuu");
        failJavaToParse("Thu Jul 29 16:39:41 GMT+090807 1999", "EEE MMM dd HH:mm:ss zzz uuuu");
        failJavaToParse("Thu Jul 29 16:39:41 GMT-09 1999", "EEE MMM dd HH:mm:ss zzz uuuu");
        failJavaToParse("Thu Jul 29 16:39:41 GMT-09:08 1999", "EEE MMM dd HH:mm:ss zzz uuuu");
        failJavaToParse("Thu Jul 29 16:39:41 GMT-09:08:07 1999", "EEE MMM dd HH:mm:ss zzz uuuu");
        failJavaToParse("Thu Jul 29 16:39:41 GMT-3.5 1999", "EEE MMM dd HH:mm:ss zzz uuuu");
        failJavaToParse("Thu Jul 29 16:39:41 GMT-3,5 1999", "EEE MMM dd HH:mm:ss zzz uuuu");

        // String-ish time zone IDs are not accepted in Java parser.
        failJavaToParse("Thu Jul 29 16:39:41 Mountain Daylight Time 1999", "EEE MMM dd HH:mm:ss zzz uuuu");
        failJavaToParse("Thu Jul 29 16:39:41 E. Australia Standard Time 1999", "EEE MMM dd HH:mm:ss zzz uuuu");

        failJavaToParse("Thu Jul 29 16:39:41 UTC 1999", "EEE MMM dd HH:mm:ss zzz uuuu");
    }

    @Test  // Imported from test__strptime__3 in Ruby v2.3.1's test/date/test_date_strptime.rb.
    public void testRuby__strptime__3_date1() {
        // The time zone ID "EST" is recognized in Ruby parser.
        testRubyToParse("Thu Jul 29 16:39:41 EST 1999", "%a %b %d %H:%M:%S %Z %Y", 933284381L);

        // The time zone IDs "MET", "AMT", "AST", and "DST" are not recognized, and handled as "UTC", in Ruby parser.
        testRubyToParse("Thu Jul 29 16:39:41 MET DST 1999", "%a %b %d %H:%M:%S %Z %Y", 933266381L);
        testRubyToParse("Thu Jul 29 16:39:41 AMT 1999", "%a %b %d %H:%M:%S %Z %Y", 933266381L);
        testRubyToParse("Thu Jul 29 16:39:41 AMT -1999", "%a %b %d %H:%M:%S %Z %Y", -125231383219L);
        testRubyToParse("Thu Jul 29 16:39:41 AST 1999", "%a %b %d %H:%M:%S %Z %Y", 933266381L);
        testRubyToParse("Thu Jul 29 16:39:41 AST -1999", "%a %b %d %H:%M:%S %Z %Y", -125231383219L);

        // All "GMT", "GMT+..." and "GMT-..." are not recognized, and handled as "UTC", in Ruby parser.
        testRubyToParse("Thu Jul 29 16:39:41 GMT+09 1999", "%a %b %d %H:%M:%S %Z %Y", 933266381L);
        testRubyToParse("Thu Jul 29 16:39:41 GMT+0908 1999", "%a %b %d %H:%M:%S %Z %Y", 933266381L);
        testRubyToParse("Thu Jul 29 16:39:41 GMT+090807 1999", "%a %b %d %H:%M:%S %Z %Y", 933266381L);
        testRubyToParse("Thu Jul 29 16:39:41 GMT-09 1999", "%a %b %d %H:%M:%S %Z %Y", 933266381L);
        testRubyToParse("Thu Jul 29 16:39:41 GMT-09:08 1999", "%a %b %d %H:%M:%S %Z %Y", 933266381L);
        testRubyToParse("Thu Jul 29 16:39:41 GMT-09:08:07 1999", "%a %b %d %H:%M:%S %Z %Y", 933266381L);
        testRubyToParse("Thu Jul 29 16:39:41 GMT-3.5 1999", "%a %b %d %H:%M:%S %Z %Y", 933266381L);
        testRubyToParse("Thu Jul 29 16:39:41 GMT-3,5 1999", "%a %b %d %H:%M:%S %Z %Y", 933266381L);

        // String-ish time zone IDs are not recognized, and handled as "UTC", in Ruby parser.
        testRubyToParse("Thu Jul 29 16:39:41 Mountain Daylight Time 1999", "%a %b %d %H:%M:%S %Z %Y", 933266381L);
        testRubyToParse("Thu Jul 29 16:39:41 E. Australia Standard Time 1999", "%a %b %d %H:%M:%S %Z %Y", 933266381L);
    }

    @Test  // Imported from test__strptime__3 in Ruby v2.3.1's test/date/test_date_strptime.rb.
    public void test__strptime__3_date1() {
        testToParse("Thu Jul 29 16:39:41 EST 1999", "%a %b %d %H:%M:%S %Z %Y", 933284381L);
        testToParse("Thu Jul 29 16:39:41 MET DST 1999", "%a %b %d %H:%M:%S %Z %Y", 933259181L);
        // Their time zones are "AMT" actually in Ruby v2.3.1's tests, but "AST" is used here instead.
        // "AMT" is not recognized even by Ruby v2.3.1's zonetab.
        testToParse("Thu Jul 29 16:39:41 AST 1999", "%a %b %d %H:%M:%S %Z %Y", 933280781L);
        testToParse("Thu Jul 29 16:39:41 AST -1999", "%a %b %d %H:%M:%S %Z %Y", -125231368819L);
        testToParse("Thu Jul 29 16:39:41 GMT+09 1999", "%a %b %d %H:%M:%S %Z %Y", 933233981L);
        testToParse("Thu Jul 29 16:39:41 GMT+0908 1999", "%a %b %d %H:%M:%S %Z %Y", 933233501L);
        testToParse("Thu Jul 29 16:39:41 GMT+090807 1999", "%a %b %d %H:%M:%S %Z %Y", 933233494L);
        testToParse("Thu Jul 29 16:39:41 GMT-09 1999", "%a %b %d %H:%M:%S %Z %Y", 933298781L);
        testToParse("Thu Jul 29 16:39:41 GMT-09:08 1999", "%a %b %d %H:%M:%S %Z %Y", 933299261L);
        testToParse("Thu Jul 29 16:39:41 GMT-09:08:07 1999", "%a %b %d %H:%M:%S %Z %Y", 933299268L);
        testToParse("Thu Jul 29 16:39:41 GMT-3.5 1999", "%a %b %d %H:%M:%S %Z %Y", 933278981L);
        testToParse("Thu Jul 29 16:39:41 GMT-3,5 1999", "%a %b %d %H:%M:%S %Z %Y", 933278981L);
        testToParse("Thu Jul 29 16:39:41 Mountain Daylight Time 1999", "%a %b %d %H:%M:%S %Z %Y", 933287981L);
        testToParse("Thu Jul 29 16:39:41 E. Australia Standard Time 1999", "%a %b %d %H:%M:%S %Z %Y", 933230381L);
    }

    @Test  // Imported from test__strptime__3 in Ruby v2.3.1's test/date/test_date_strptime.rb.
    public void testJavaRfc822() {
        // String-ish time zone IDs such as "UT", "GMT", "PDT", and "z" are not accepted in Java parser.
        failJavaToParse("Thu, 29 Jul 1999 09:54:21 UT", "EEE, dd MMM uuuu HH:mm:ss ZZZ");
        failJavaToParse("Thu, 29 Jul 1999 09:54:21 GMT", "EEE, dd MMM uuuu HH:mm:ss ZZZ");
        failJavaToParse("Thu, 29 Jul 1999 09:54:21 PDT", "EEE, dd MMM uuuu HH:mm:ss ZZZ");
        failJavaToParse("Thu, 29 Jul 1999 09:54:21 z", "EEE, dd MMM uuuu HH:mm:ss ZZZ");

        testJavaToParse("Thu, 29 Jul 1999 09:54:21 +0900", "EEE, dd MMM uuuu HH:mm:ss ZZZ", 933209661L);
        testJavaToParse("Thu, 29 Jul 1999 09:54:21 +0430", "EEE, dd MMM uuuu HH:mm:ss ZZZ", 933225861L);
        testJavaToParse("Thu, 29 Jul 1999 09:54:21 -0430", "EEE, dd MMM uuuu HH:mm:ss ZZZ", 933258261L);

        // JFYI: Jul 29 -1999 is Sunday, not Thursday.
        testJavaToParse("Sun, 29 Jul -1999 09:54:21 -0430", "EEE, dd MMM uuuu HH:mm:ss ZZZ", -125231391339L);
    }

    @Test  // Imported from test__strptime__3 in Ruby v2.3.1's test/date/test_date_strptime.rb.
    public void testRuby__strptime__3_rfc822() {
        testRubyToParse("Thu, 29 Jul 1999 09:54:21 UT", "%a, %d %b %Y %H:%M:%S %Z", 933242061L);
        testRubyToParse("Thu, 29 Jul 1999 09:54:21 GMT", "%a, %d %b %Y %H:%M:%S %Z", 933242061L);

        // "PDT" (Pacific Daylight Time) is correctly recognized as -07:00 in Ruby parser, not like the legacy parser.
        testRubyToParse("Thu, 29 Jul 1999 09:54:21 PDT", "%a, %d %b %Y %H:%M:%S %Z", 933267261L);

        testRubyToParse("Thu, 29 Jul 1999 09:54:21 z", "%a, %d %b %Y %H:%M:%S %Z", 933242061L);
        testRubyToParse("Thu, 29 Jul 1999 09:54:21 +0900", "%a, %d %b %Y %H:%M:%S %Z", 933209661L);
        testRubyToParse("Thu, 29 Jul 1999 09:54:21 +0430", "%a, %d %b %Y %H:%M:%S %Z", 933225861L);
        testRubyToParse("Thu, 29 Jul 1999 09:54:21 -0430", "%a, %d %b %Y %H:%M:%S %Z", 933258261L);
        testRubyToParse("Thu, 29 Jul -1999 09:54:21 -0430", "%a, %d %b %Y %H:%M:%S %Z", -125231391339L);
    }

    @Test  // Imported from test__strptime__3 in Ruby v2.3.1's test/date/test_date_strptime.rb.
    public void test__strptime__3_rfc822() {
        testToParse("Thu, 29 Jul 1999 09:54:21 UT", "%a, %d %b %Y %H:%M:%S %Z", 933242061L);
        testToParse("Thu, 29 Jul 1999 09:54:21 GMT", "%a, %d %b %Y %H:%M:%S %Z", 933242061L);

        // It should be 933267261L if PDT (Pacific Daylight Time) is correctly -07:00.
        //
        // Joda-Time's DateTimeFormat.forPattern("z").parseMillis("PDT") however returns 8 hours (-08:00).
        // DateTimeFormat.forPattern("z").parseMillis("PDT") == 28800000
        // https://github.com/JodaOrg/joda-time/blob/v2.9.2/src/main/java/org/joda/time/DateTimeUtils.java#L446
        //
        // Embulk has used it to parse time zones for a very long time since it was v0.1.
        // https://github.com/embulk/embulk/commit/b97954a5c78397e1269bbb6979d6225dfceb4e05
        //
        // It is kept as -08:00 for compatibility as of now.
        //
        // TODO: Make time zone parsing consistent.
        // @see <a href="https://github.com/embulk/embulk/issues/860">https://github.com/embulk/embulk/issues/860</a>
        testToParse("Thu, 29 Jul 1999 09:54:21 PDT", "%a, %d %b %Y %H:%M:%S %Z", 933270861L);

        testToParse("Thu, 29 Jul 1999 09:54:21 z", "%a, %d %b %Y %H:%M:%S %Z", 933242061L);
        testToParse("Thu, 29 Jul 1999 09:54:21 +0900", "%a, %d %b %Y %H:%M:%S %Z", 933209661L);
        testToParse("Thu, 29 Jul 1999 09:54:21 +0430", "%a, %d %b %Y %H:%M:%S %Z", 933225861L);
        testToParse("Thu, 29 Jul 1999 09:54:21 -0430", "%a, %d %b %Y %H:%M:%S %Z", 933258261L);
        testToParse("Thu, 29 Jul -1999 09:54:21 -0430", "%a, %d %b %Y %H:%M:%S %Z", -125231391339L);
    }

    @Test  // Imported from test__strptime__3 in Ruby v2.3.1's test/date/test_date_strptime.rb.
    public void testJavaEtc() {
        // "uu" always parses two-digit years into 2000s in Java parser. ("99" goes to "2099".)
        testJavaToParse("06-DEC-99", "dd-MMM-uu", 4100198400L);

        // JFYI: October 31, 2001 is Wednesday, not Sunday.
        testJavaToParse("wEdnesDay oCtoBer 31 01", "EEEE MMMM dd uu", 1004486400L);

        // "uu" always parses two-digit years into 2000s in Java parser. ("99" goes to "2099".)
        // Whitespaces are not parsed just with " " in Java parser.
        // Their "\u000b" are actually "\v" in Ruby v2.3.1's tests. "\v" is not recognized as a character in Java.
        testJavaToParse("October\t\n\u000b\f\r 15,\t\n\u000b\f\r99", "MMMM\t\n\u000b\f\r dd,\t\n\u000b\f\ruu",
                        4095705600L);

        // Default dates are always 1970-01-01 in Java parser.
        testJavaToParse("09:02:11 AM", "KK:mm:ss a", 32531L);
        // "A.M." (with dots) is not accepted in Java parser.
        failJavaToParse("09:02:11 A.M.", "KK:mm:ss a");
        testJavaToParse("09:02:11 PM", "KK:mm:ss a", 75731L);
        // "P.M." (with dots) is not accepted in Java parser.
        failJavaToParse("09:02:11 P.M.", "KK:mm:ss a");

        // Default dates are always 1970-01-01 in Java parser.
        // Results differ between "hh" (HOUR_OF_AMPM) and "KK" (CLOCK_HOUR_OF_AMPM) in case the hour is 12.
        testJavaToParse("12:33:44 AM", "hh:mm:ss a", 2024L);
        testJavaToParse("12:33:44 AM", "KK:mm:ss a", 45224L);
        testJavaToParse("01:33:44 AM", "hh:mm:ss a", 5624L);
        testJavaToParse("01:33:44 AM", "KK:mm:ss a", 5624L);
        testJavaToParse("11:33:44 AM", "hh:mm:ss a", 41624L);
        testJavaToParse("11:33:44 AM", "KK:mm:ss a", 41624L);
        testJavaToParse("12:33:44 PM", "hh:mm:ss a", 45224L);
        failJavaToParse("12:33:44 PM", "KK:mm:ss a");
        testJavaToParse("01:33:44 PM", "hh:mm:ss a", 48824L);
        testJavaToParse("01:33:44 PM", "KK:mm:ss a", 48824L);
        testJavaToParse("11:33:44 PM", "hh:mm:ss a", 84824L);
        testJavaToParse("11:33:44 PM", "KK:mm:ss a", 84824L);

        // Their time zones are "AMT" actually in Ruby v2.3.1's tests, but "-04:00" is used here instead.
        // "AMT" is not recognized even by Ruby v2.3.1's zonetab.
        testJavaToParse("11:33:44 PM -04:00", "KK:mm:ss a XXXXX", 99224L);
        failJavaToParse("11:33:44 P.M. -04:00", "KK:mm:ss a XXXXX");

        // Just "+5" is not acceptable as a time zone offset in Java parser.
        // JFYI: February 1, 2003 is Saturday, not Friday.
        testJavaToParse("sat1feb034pm+0500", "EEEdMMMuuhaX", 1044097200L);
    }

    @Test  // Imported from test__strptime__3 in Ruby v2.3.1's test/date/test_date_strptime.rb.
    public void testRuby__strptime__3_etc() {
        testRubyToParse("06-DEC-99", "%d-%b-%y", 944438400L);
        testRubyToParse("sUnDay oCtoBer 31 01", "%A %B %d %y", 1004486400L);
        // Their "\u000b" are actually "\v" in Ruby v2.3.1's tests. "\v" is not recognized as a character in Java.
        testRubyToParse("October\t\n\u000b\f\r 15,\t\n\u000b\f\r99", "%B %d, %y", 939945600L);
        testRubyToParse("October\t\n\u000b\f\r 15,\t\n\u000b\f\r99", "%B%t%d,%n%y", 939945600L);

        testRubyToParse("09:02:11 AM", "%I:%M:%S %p", 32531L);
        testRubyToParse("09:02:11 A.M.", "%I:%M:%S %p", 32531L);
        testRubyToParse("09:02:11 PM", "%I:%M:%S %p", 75731L);
        testRubyToParse("09:02:11 P.M.", "%I:%M:%S %p", 75731L);

        testRubyToParse("12:33:44 AM", "%r", 2024L);
        testRubyToParse("01:33:44 AM", "%r", 5624L);
        testRubyToParse("11:33:44 AM", "%r", 41624L);
        testRubyToParse("12:33:44 PM", "%r", 45224L);
        testRubyToParse("01:33:44 PM", "%r", 48824L);
        testRubyToParse("11:33:44 PM", "%r", 84824L);

        testRubyToParse("11:33:44 PM AMT", "%I:%M:%S %p %Z", 84824L);
        testRubyToParse("11:33:44 P.M. AMT", "%I:%M:%S %p %Z", 84824L);
        // Their time zones are "AMT" actually in Ruby v2.3.1's tests, but "-04:00" is used here instead.
        // "AMT" is not recognized even by Ruby v2.3.1's zonetab.
        testRubyToParse("11:33:44 PM -04:00", "%I:%M:%S %p %Z", 99224L);
        testRubyToParse("11:33:44 P.M. -04:00", "%I:%M:%S %p %Z", 99224L);

        testRubyToParse("fri1feb034pm+5", "%a%d%b%y%H%p%Z", 1044115200L);
        // The time zone offset is just "+5" in Ruby v2.3.1's tests, but "+05" is used here instead.
        // "+5" is not recognized, and handled as "UTC", in Ruby parser.
        testRubyToParse("fri1feb034pm+05", "%a%d%b%y%H%p%Z", 1044097200L);
    }

    @Test  // Imported from test__strptime__3 in Ruby v2.3.1's test/date/test_date_strptime.rb.
    public void test__strptime__3_etc() {
        testToParse("06-DEC-99", "%d-%b-%y", 944438400L);
        testToParse("sUnDay oCtoBer 31 01", "%A %B %d %y", 1004486400L);
        // Their "\u000b" are actually "\v" in Ruby v2.3.1's tests. "\v" is not recognized as a character in Java.
        testToParse("October\t\n\u000b\f\r 15,\t\n\u000b\f\r99", "%B %d, %y", 939945600L);
        testToParse("October\t\n\u000b\f\r 15,\t\n\u000b\f\r99", "%B%t%d,%n%y", 939945600L);

        testToParse("09:02:11 AM", "%I:%M:%S %p", 81955357331L);
        testToParse("09:02:11 A.M.", "%I:%M:%S %p", 81955357331L);
        testToParse("09:02:11 PM", "%I:%M:%S %p", 81955400531L);
        testToParse("09:02:11 P.M.", "%I:%M:%S %p", 81955400531L);

        testToParse("12:33:44 AM", "%r", 81955326824L);
        testToParse("01:33:44 AM", "%r", 81955330424L);
        testToParse("11:33:44 AM", "%r", 81955366424L);
        testToParse("12:33:44 PM", "%r", 81955370024L);
        testToParse("01:33:44 PM", "%r", 81955373624L);
        testToParse("11:33:44 PM", "%r", 81955409624L);

        // Their time zones are "AMT" actually in Ruby v2.3.1's tests, but "AST" is used here instead.
        // "AMT" is not recognized even by Ruby v2.3.1's zonetab.
        testToParse("11:33:44 PM AST", "%I:%M:%S %p %Z", 81955424024L);
        testToParse("11:33:44 P.M. AST", "%I:%M:%S %p %Z", 81955424024L);

        testToParse("fri1feb034pm+5", "%a%d%b%y%H%p%Z", 1044097200L);
    }

    @Test  // Imported from test__strptime__width in Ruby v2.3.1's test/date/test_date_strptime.rb.
    public void testJavaWidth() {
        // "uu" always parses two-digit years into 2000s in Java parser. ("99" goes to "2099".)
        // Default dates are always 1970-01-01 in Java parser.
        testJavaToParse("99", "uu", 4070908800L);
        testJavaToParse("01", "uu", 978307200L);

        // Centuries are not accepted in Java parser.
        // testToParse("19 99", "%C %y", 917049600L);
        // testToParse("20 01", "%C %y", 980208000L);
        // testToParse("30 99", "%C %y", 35629718400L);
        // testToParse("30 01", "%C %y", 32537116800L);
        // testToParse("1999", "%C%y", 917049600L);
        // testToParse("2001", "%C%y", 980208000L);
        // testToParse("3099", "%C%y", 35629718400L);
        // testToParse("3001", "%C%y", 32537116800L);

        // Default dates are always 1970-01-01 in Java parser.
        testJavaToParse("20060806", "uuuuuuuu", 632995724851200L);
        testJavaToParse("20060806", "uuuuMMdd", 1154822400L);
        // They are not accepted in Java parser.
        failJavaToParse("2006908906", "uuuu9MM9dd");
        failJavaToParse("2006908906", "uuuu'9'MM'9'dd");
        testJavaToParse("12006 08 06", "uuuuu MM dd", 316724342400L);
        testJavaToParse("12006-08-06", "uuuuu-MM-dd", 316724342400L);
        testJavaToParse("200608 6", "uuuuMM[ ]d", 1154822400L);

        testJavaToParse("2006333", "uuuuDDD", 1164758400L);
        // They are not accepted in Java parser.
        failJavaToParse("20069333", "uuuu9DDD");
        failJavaToParse("20069333", "uuuu'9'DDD");
        testJavaToParse("12006 333", "uuuuu DDD", 316734278400L);
        testJavaToParse("12006-333", "uuuuu-DDD", 316734278400L);

        testJavaToParse("232425", "HHmmss", 84265L);
        failJavaToParse("23924925", "%H9%M9%S");
        failJavaToParse("23924925", "%H'9'%M'9'%S");
        testJavaToParse("23 24 25", "HH mm ss", 84265L);
        testJavaToParse("23:24:25", "HH:mm:ss", 84265L);
        testJavaToParse(" 32425", "[ ]hmmss", 1465L);
        testJavaToParse(" 32425", "[ ]Kmmss", 1465L);

        // They are intentionally skipped as a month and a day of week are not sufficient to build a timestamp.
        // [['FriAug', '%a%b'], [nil,8,nil,nil,nil,nil,nil,nil,5], __LINE__],
        // [['FriAug', '%A%B'], [nil,8,nil,nil,nil,nil,nil,nil,5], __LINE__],
        // [['FridayAugust', '%A%B'], [nil,8,nil,nil,nil,nil,nil,nil,5], __LINE__],
        // [['FridayAugust', '%a%b'], [nil,8,nil,nil,nil,nil,nil,nil,5], __LINE__],
    }

    @Test  // Imported from test__strptime__width in Ruby v2.3.1's test/date/test_date_strptime.rb.
    public void testRuby__strptime__width() {
        // Default dates are always 1970-01-01 in Ruby parser. If only the year is specified, the date is 01-01.
        testRubyToParse("99", "%y", 915148800L);
        testRubyToParse("01", "%y", 978307200L);
        testRubyToParse("19 99", "%C %y", 915148800L);
        testRubyToParse("20 01", "%C %y", 978307200L);
        testRubyToParse("30 99", "%C %y", 35627817600L);
        testRubyToParse("30 01", "%C %y", 32535216000L);
        testRubyToParse("1999", "%C%y", 915148800L);
        testRubyToParse("2001", "%C%y", 978307200L);
        testRubyToParse("3099", "%C%y", 35627817600L);
        testRubyToParse("3001", "%C%y", 32535216000L);

        testRubyToParse("20060806", "%Y", 632995724851200L);
        // Its " " is actually "\s" in Ruby v2.3.1's tests. "\s" is not recognized as a character in Java.
        testRubyToParse("20060806", "%Y ", 632995724851200L);
        testRubyToParse("20060806", "%Y%m%d", 1154822400L);
        testRubyToParse("2006908906", "%Y9%m9%d", 1154822400L);
        testRubyToParse("12006 08 06", "%Y %m %d", 316724342400L);
        testRubyToParse("12006-08-06", "%Y-%m-%d", 316724342400L);
        testRubyToParse("200608 6", "%Y%m%e", 1154822400L);

        // Day of the year (yday; DAY_OF_YEAR) is not recognized, and handled as January 1, in Ruby parser.
        testRubyToParse("2006333", "%Y%j", 1136073600L);
        testRubyToParse("20069333", "%Y9%j", 1136073600L);
        testRubyToParse("12006 333", "%Y %j", 316705593600L);
        testRubyToParse("12006-333", "%Y-%j", 316705593600L);

        testRubyToParse("232425", "%H%M%S", 84265L);
        testRubyToParse("23924925", "%H9%M9%S", 84265L);
        testRubyToParse("23 24 25", "%H %M %S", 84265L);
        testRubyToParse("23:24:25", "%H:%M:%S", 84265L);
        testRubyToParse(" 32425", "%k%M%S", 12265L);
        testRubyToParse(" 32425", "%l%M%S", 12265L);

        // They are intentionally skipped as a month and a day of week are not sufficient to build a timestamp.
        // [['FriAug', '%a%b'], [nil,8,nil,nil,nil,nil,nil,nil,5], __LINE__],
        // [['FriAug', '%A%B'], [nil,8,nil,nil,nil,nil,nil,nil,5], __LINE__],
        // [['FridayAugust', '%A%B'], [nil,8,nil,nil,nil,nil,nil,nil,5], __LINE__],
        // [['FridayAugust', '%a%b'], [nil,8,nil,nil,nil,nil,nil,nil,5], __LINE__],
    }

    @Test  // Imported from test__strptime__width in Ruby v2.3.1's test/date/test_date_strptime.rb.
    public void test__strptime__width() {
        testToParse("99", "%y", 917049600L);
        testToParse("01", "%y", 980208000L);
        testToParse("19 99", "%C %y", 917049600L);
        testToParse("20 01", "%C %y", 980208000L);
        testToParse("30 99", "%C %y", 35629718400L);
        testToParse("30 01", "%C %y", 32537116800L);
        testToParse("1999", "%C%y", 917049600L);
        testToParse("2001", "%C%y", 980208000L);
        testToParse("3099", "%C%y", 35629718400L);
        testToParse("3001", "%C%y", 32537116800L);

        testToParse("20060806", "%Y", 632995726752000L);
        // Its " " is actually "\s" in Ruby v2.3.1's tests. "\s" is not recognized as a character in Java.
        testToParse("20060806", "%Y ", 632995726752000L);
        testToParse("20060806", "%Y%m%d", 1154822400L);
        testToParse("2006908906", "%Y9%m9%d", 1154822400L);
        testToParse("12006 08 06", "%Y %m %d", 316724342400L);
        testToParse("12006-08-06", "%Y-%m-%d", 316724342400L);
        testToParse("200608 6", "%Y%m%e", 1154822400L);

        testToParse("2006333", "%Y%j", 1164758400L);
        testToParse("20069333", "%Y9%j", 1164758400L);
        testToParse("12006 333", "%Y %j", 316734278400L);
        testToParse("12006-333", "%Y-%j", 316734278400L);

        testToParse("232425", "%H%M%S", 81955409065L);
        testToParse("23924925", "%H9%M9%S", 81955409065L);
        testToParse("23 24 25", "%H %M %S", 81955409065L);
        testToParse("23:24:25", "%H:%M:%S", 81955409065L);
        testToParse(" 32425", "%k%M%S", 81955337065L);
        testToParse(" 32425", "%l%M%S", 81955337065L);

        // They are intentionally skipped as a month and a day of week are not sufficient to build a timestamp.
        // [['FriAug', '%a%b'], [nil,8,nil,nil,nil,nil,nil,nil,5], __LINE__],
        // [['FriAug', '%A%B'], [nil,8,nil,nil,nil,nil,nil,nil,5], __LINE__],
        // [['FridayAugust', '%A%B'], [nil,8,nil,nil,nil,nil,nil,nil,5], __LINE__],
        // [['FridayAugust', '%a%b'], [nil,8,nil,nil,nil,nil,nil,nil,5], __LINE__],
    }

    // test__strptime__fail is skipped for Java parser.

    @Test  // Imported from test__strptime__fail in Ruby v2.3.1's test/date/test_date_strptime.rb.
    public void testRuby__strptime__fail() {
        testRubyToParse("2001.", "%Y.", 978307200L);
        // Its " " is actually "\s" in Ruby v2.3.1's tests. "\s" is not recognized as a character in Java.
        testRubyToParse("2001. ", "%Y.", 978307200L);
        testRubyToParse("2001.", "%Y. ", 978307200L);
        // Its " " is actually "\s" in Ruby v2.3.1's tests. "\s" is not recognized as a character in Java.
        testRubyToParse("2001. ", "%Y. ", 978307200L);

        failRubyToParse("2001", "%Y.");
        // Its " " is actually "\s" in Ruby v2.3.1's tests. "\s" is not recognized as a character in Java.
        failRubyToParse("2001 ", "%Y.");
        failRubyToParse("2001", "%Y. ");
        // Its " " is actually "\s" in Ruby v2.3.1's tests. "\s" is not recognized as a character in Java.
        failRubyToParse("2001 ", "%Y. ");

        failRubyToParse("2001-13-31", "%Y-%m-%d");
        failRubyToParse("2001-12-00", "%Y-%m-%d");
        failRubyToParse("2001-12-32", "%Y-%m-%d");
        failRubyToParse("2001-12-00", "%Y-%m-%e");
        failRubyToParse("2001-12-32", "%Y-%m-%e");
        failRubyToParse("2001-12-31", "%y-%m-%d");

        failRubyToParse("2004-000", "%Y-%j");
        failRubyToParse("2004-367", "%Y-%j");
        failRubyToParse("2004-366", "%y-%j");

        testRubyToParse("24:59:59", "%H:%M:%S", 89999L);
        testRubyToParse("24:59:59", "%k:%M:%S", 89999L);
        testRubyToParse("24:59:60", "%H:%M:%S", 90000L);
        testRubyToParse("24:59:60", "%k:%M:%S", 90000L);

        failRubyToParse("24:60:59", "%H:%M:%S");
        failRubyToParse("24:60:59", "%k:%M:%S");
        failRubyToParse("24:59:61", "%H:%M:%S");
        failRubyToParse("24:59:61", "%k:%M:%S");
        failRubyToParse("00:59:59", "%I:%M:%S");
        failRubyToParse("13:59:59", "%I:%M:%S");
        failRubyToParse("00:59:59", "%l:%M:%S");
        failRubyToParse("13:59:59", "%l:%M:%S");

        failRubyToParse("0", "%U");  // "ruby:" version fails to parse this.
        failRubyToParse("54", "%U");
        failRubyToParse("0", "%W");  // "ruby:" version fails to parse this.
        failRubyToParse("54", "%W");
        failRubyToParse("0", "%V");
        failRubyToParse("54", "%V");
        failRubyToParse("0", "%u");
        failRubyToParse("7", "%u");  // "ruby:" version fails to parse this.
        failRubyToParse("0", "%w");  // "ruby:" version fails to parse this.
        failRubyToParse("7", "%w");

        failRubyToParse("Sanday", "%A");
        failRubyToParse("Jenuary", "%B");
        failRubyToParse("Sundai", "%A");  // "ruby:" version fails to parse this.
        testRubyToParse("Januari", "%B", 0L);
        failRubyToParse("Sundai,", "%A,");
        failRubyToParse("Januari,", "%B,");
    }

    @Test  // Imported from test__strptime__fail in Ruby v2.3.1's test/date/test_date_strptime.rb.
    public void test__strptime__fail() {
        testToParse("2001.", "%Y.", 980208000L);
        // Its " " is actually "\s" in Ruby v2.3.1's tests. "\s" is not recognized as a character in Java.
        testToParse("2001. ", "%Y.", 980208000L);
        testToParse("2001.", "%Y. ", 980208000L);
        // Its " " is actually "\s" in Ruby v2.3.1's tests. "\s" is not recognized as a character in Java.
        testToParse("2001. ", "%Y. ", 980208000L);

        failToParse("2001", "%Y.");
        // Its " " is actually "\s" in Ruby v2.3.1's tests. "\s" is not recognized as a character in Java.
        failToParse("2001 ", "%Y.");
        failToParse("2001", "%Y. ");
        // Its " " is actually "\s" in Ruby v2.3.1's tests. "\s" is not recognized as a character in Java.
        failToParse("2001 ", "%Y. ");

        failToParse("2001-13-31", "%Y-%m-%d");
        failToParse("2001-12-00", "%Y-%m-%d");
        failToParse("2001-12-32", "%Y-%m-%d");
        failToParse("2001-12-00", "%Y-%m-%e");
        failToParse("2001-12-32", "%Y-%m-%e");
        failToParse("2001-12-31", "%y-%m-%d");

        failToParse("2004-000", "%Y-%j");
        failToParse("2004-367", "%Y-%j");
        failToParse("2004-366", "%y-%j");

        testToParse("24:59:59", "%H:%M:%S", 81955414799L);
        testToParse("24:59:59", "%k:%M:%S", 81955414799L);
        testToParse("24:59:60", "%H:%M:%S", 81955414799L);
        testToParse("24:59:60", "%k:%M:%S", 81955414799L);

        failToParse("24:60:59", "%H:%M:%S");
        failToParse("24:60:59", "%k:%M:%S");
        failToParse("24:59:61", "%H:%M:%S");
        failToParse("24:59:61", "%k:%M:%S");
        failToParse("00:59:59", "%I:%M:%S");
        failToParse("13:59:59", "%I:%M:%S");
        failToParse("00:59:59", "%l:%M:%S");
        failToParse("13:59:59", "%l:%M:%S");

        testToParse("0", "%U", 81955324800L);
        failToParse("54", "%U");
        testToParse("0", "%W", 81955324800L);
        failToParse("54", "%W");
        failToParse("0", "%V");
        failToParse("54", "%V");
        failToParse("0", "%u");
        testToParse("7", "%u", 81955324800L);
        testToParse("0", "%w", 81955324800L);
        failToParse("7", "%w");

        failToParse("Sanday", "%A");
        failToParse("Jenuary", "%B");
        testToParse("Sundai", "%A", 81955324800L);
        testToParse("Januari", "%B", 81955324800L);
        failToParse("Sundai,", "%A,");
        failToParse("Januari,", "%B,");
    }

    @Test  // Imported partially from test_strptime in Ruby v2.3.1's test/date/test_date_strptime.rb.
    public void testJava() {
        // 'Z' is not accepted in Java parser. Tested with "+00:00" instead.
        testJavaToParse("2002-03-14T11:22:33+00:00", "uuuu-MM-dd'T'HH:mm:ssXXXXX", 1016104953L);
        testJavaToParse("2002-03-14T11:22:33+09:00", "uuuu-MM-dd'T'HH:mm:ssXXXXX", 1016072553L);
        testJavaToParse("2002-03-14T11:22:33-09:00", "uuuu-MM-dd'T'HH:mm:ssXXXXX", 1016137353L);
        testJavaToParse("2002-03-14T11:22:33.123456789-09:00", "uuuu-MM-dd'T'HH:mm:ss.SSSSSSSSSXXXXX",
                        1016137353L, 123456789);
        testJavaToParse("2002-03-14T11:22:33.123456789-09:00", "uuuu-MM-dd'T'HH:mm:ss.nnnnnnnnnXXXXX",
                        1016137353L, 123456789);
    }

    @Test  // Imported partially from test_strptime in Ruby v2.3.1's test/date/test_date_strptime.rb.
    public void testRuby_strptime() {
        testRubyToParse("2002-03-14T11:22:33Z", "%Y-%m-%dT%H:%M:%S%Z", 1016104953L);
        testRubyToParse("2002-03-14T11:22:33+09:00", "%Y-%m-%dT%H:%M:%S%Z", 1016072553L);
        testRubyToParse("2002-03-14T11:22:33-09:00", "%FT%T%Z", 1016137353L);
        testRubyToParse("2002-03-14T11:22:33.123456789-09:00", "%FT%T.%N%Z", 1016137353L, 123456789);
    }

    @Test  // Imported partially from test_strptime in Ruby v2.3.1's test/date/test_date_strptime.rb.
    public void test_strptime() {
        testToParse("2002-03-14T11:22:33Z", "%Y-%m-%dT%H:%M:%S%Z", 1016104953L);
        testToParse("2002-03-14T11:22:33+09:00", "%Y-%m-%dT%H:%M:%S%Z", 1016072553L);
        testToParse("2002-03-14T11:22:33-09:00", "%FT%T%Z", 1016137353L);
        testToParse("2002-03-14T11:22:33.123456789-09:00", "%FT%T.%N%Z", 1016137353L, 123456789);
    }

    // Epoch (nano) seconds are not accepted in Java parser.

    @Test  // Imported from test_strptime__minus in Ruby v2.3.1's test/date/test_date_strptime.rb.
    public void testRuby_strptime__minus() {
        testRubyToParse("-1", "%s", -1L);
        testRubyToParse("-86400", "%s", -86400L);

        // In |java.time.Instant|, it is always 0 <= nanoAdjustment < 1,000,000,000.
        // -0.9s is represented like -1s + 100ms.
        testRubyToParse("-999", "%Q", -1L, 1000000);
        testRubyToParse("-1000", "%Q", -1L);
    }

    @Test  // Imported from test_strptime__minus in Ruby v2.3.1's test/date/test_date_strptime.rb.
    public void test_strptime__minus() {
        testToParse("-1", "%s", -1L);
        testToParse("-86400", "%s", -86400L);

        // In |java.time.Instant|, it is always 0 <= nanoAdjustment < 1,000,000,000.
        // -0.9s is represented like -1s + 100ms.
        testToParse("-999", "%Q", -1L, 1000000);
        testToParse("-1000", "%Q", -1L);
    }

    @Test
    public void testEpochWithFraction() {
        testToParse("1500000000.123456789", "%s.%N", 1500000000L, 123456789);
        testToParse("1500000000456.111111111", "%Q.%N", 1500000000L, 567111111);
        testToParse("1500000000.123", "%s.%L", 1500000000L, 123000000);
        testToParse("1500000000456.111", "%Q.%L", 1500000000L, 567000000);

        testToParse("1.5", "%s.%N", 1L, 500000000);
        testToParse("-1.5", "%s.%N", -2L, 500000000);
        testToParse("1.000000001", "%s.%N", 1L, 1);
        testToParse("-1.000000001", "%s.%N", -2L, 999999999);
    }

    @Test
    public void testRubyEpochWithFraction() {
        testRubyToParse("1500000000.123456789", "%s.%N", 1500000000L, 123456789);
        testRubyToParse("1500000000456.111111111", "%Q.%N", 1500000000L, 567111111);
        testRubyToParse("1500000000.123", "%s.%L", 1500000000L, 123000000);
        testRubyToParse("1500000000456.111", "%Q.%L", 1500000000L, 567000000);

        testRubyToParse("1.5", "%s.%N", 1L, 500000000);
        testRubyToParse("-1.5", "%s.%N", -2L, 500000000);
        testRubyToParse("1.000000001", "%s.%N", 1L, 1);
        testRubyToParse("-1.000000001", "%s.%N", -2L, 999999999);
    }

    @Test
    public void testExcessDate() {
        testToParse("2018-02-31T00:00:00", "%Y-%m-%dT%H:%M:%S", 1520035200L);  // 2018-03-03
        testToParse("2016-02-31T00:00:00", "%Y-%m-%dT%H:%M:%S", 1456876800L);  // 2016-03-02
        testToParse("2018-11-31T00:00:00", "%Y-%m-%dT%H:%M:%S", 1543622400L);  // 2018-12-01

        failToParse("2018-10-32T00:00:00", "%Y-%m-%dT%H:%M:%S");
        failToParse("2018-13-01T00:00:00", "%Y-%m-%dT%H:%M:%S");

        failToParse("2018-00-01T00:00:00", "%Y-%m-%dT%H:%M:%S");
        failToParse("2018-01-00T00:00:00", "%Y-%m-%dT%H:%M:%S");
    }

    @Test
    public void testRubyExcessDate() {
        failRubyToParse("2018-02-31T00:00:00", "%Y-%m-%dT%H:%M:%S");
        failRubyToParse("2016-02-31T00:00:00", "%Y-%m-%dT%H:%M:%S");
        failRubyToParse("2018-11-31T00:00:00", "%Y-%m-%dT%H:%M:%S");

        failRubyToParse("2018-10-32T00:00:00", "%Y-%m-%dT%H:%M:%S");
        failRubyToParse("2018-13-01T00:00:00", "%Y-%m-%dT%H:%M:%S");

        failRubyToParse("2018-00-01T00:00:00", "%Y-%m-%dT%H:%M:%S");
        failRubyToParse("2018-01-00T00:00:00", "%Y-%m-%dT%H:%M:%S");
    }

    @Test
    public void testJavaExcessDate() {
        failJavaToParse("2018-02-31T00:00:00", "yyyy-MM-dd'T'HH:mm:ss");
        failJavaToParse("2016-02-31T00:00:00", "yyyy-MM-dd'T'HH:mm:ss");
        failJavaToParse("2018-11-31T00:00:00", "yyyy-MM-dd'T'HH:mm:ss");

        failJavaToParse("2018-10-32T00:00:00", "yyyy-MM-dd'T'HH:mm:ss");
        failJavaToParse("2018-13-01T00:00:00", "yyyy-MM-dd'T'HH:mm:ss");

        failJavaToParse("2018-00-01T00:00:00", "yyyy-MM-dd'T'HH:mm:ss");
        failJavaToParse("2018-01-00T00:00:00", "yyyy-MM-dd'T'HH:mm:ss");
    }

    @Test
    public void testSimple() {
        testToParse("2014-11-19 02:46:29 +0000", "%Y-%m-%d %H:%M:%S %z", 1416365189L);
    }

    @Test
    public void testRubySimple() {
        testRubyToParse("2014-11-19 02:46:29 +0000", "%Y-%m-%d %H:%M:%S %z", 1416365189L);
    }

    @Test
    public void testJavaSimple() {
        testJavaToParse("2014-11-19 02:46:29 +0000", "uuuu-MM-dd HH:mm:ss XXXX", 1416365189L);
    }

    @Test
    public void testUnixtimeFormat() {
        testToParse("1416365189", "%s", 1416365189L);
    }

    @Test
    public void testRubyUnixtimeFormat() {
        testRubyToParse("1416365189", "%s", 1416365189L);
    }

    @Test
    public void testDefaultDate() {
        final TimestampFormatter formatter = TimestampFormatter.builder("%H:%M:%S %Z", true).setDefaultDate(2016, 2, 3).build();
        final Instant instant = formatter.parse("02:46:29 +0000");
        assertEquals(instant, Instant.ofEpochSecond(1454467589L, 0));
    }

    @Test
    public void testLeapSeconds() {
        assertParsedTime("2008-12-31T23:56:00", "%Y-%m-%dT%H:%M:%S", Instant.ofEpochSecond(1230767760L, 0));
        assertParsedTime("2008-12-31T23:59:00", "%Y-%m-%dT%H:%M:%S", Instant.ofEpochSecond(1230767940L, 0));
        assertParsedTime("2008-12-31T23:59:59", "%Y-%m-%dT%H:%M:%S", Instant.ofEpochSecond(1230767999L, 0));
        assertParsedTime("2008-12-31T23:59:60", "%Y-%m-%dT%H:%M:%S", Instant.ofEpochSecond(1230767999L, 0));  // Leap
        assertParsedTime("2009-01-01T00:00:00", "%Y-%m-%dT%H:%M:%S", Instant.ofEpochSecond(1230768000L, 0));
        assertParsedTime("2009-01-01T00:00:01", "%Y-%m-%dT%H:%M:%S", Instant.ofEpochSecond(1230768001L, 0));
        assertParsedTime("2009-01-01T00:01:00", "%Y-%m-%dT%H:%M:%S", Instant.ofEpochSecond(1230768060L, 0));
        assertParsedTime("2009-01-01T00:03:00", "%Y-%m-%dT%H:%M:%S", Instant.ofEpochSecond(1230768180L, 0));
    }

    @Test
    public void testRubyLeapSeconds() {
        assertParsedTime("2008-12-31T23:56:00", "ruby:%Y-%m-%dT%H:%M:%S", Instant.ofEpochSecond(1230767760L, 0));
        assertParsedTime("2008-12-31T23:59:00", "ruby:%Y-%m-%dT%H:%M:%S", Instant.ofEpochSecond(1230767940L, 0));
        assertParsedTime("2008-12-31T23:59:59", "ruby:%Y-%m-%dT%H:%M:%S", Instant.ofEpochSecond(1230767999L, 0));
        assertParsedTime("2008-12-31T23:59:60", "ruby:%Y-%m-%dT%H:%M:%S", Instant.ofEpochSecond(1230768000L, 0));  // Leap
        assertParsedTime("2009-01-01T00:00:00", "ruby:%Y-%m-%dT%H:%M:%S", Instant.ofEpochSecond(1230768000L, 0));
        assertParsedTime("2009-01-01T00:00:01", "ruby:%Y-%m-%dT%H:%M:%S", Instant.ofEpochSecond(1230768001L, 0));
        assertParsedTime("2009-01-01T00:01:00", "ruby:%Y-%m-%dT%H:%M:%S", Instant.ofEpochSecond(1230768060L, 0));
        assertParsedTime("2009-01-01T00:03:00", "ruby:%Y-%m-%dT%H:%M:%S", Instant.ofEpochSecond(1230768180L, 0));
    }

    @Test
    public void testJavaLeapSeconds() {
        assertParsedTime("2008-12-31T23:56:00", "java:uuuu-MM-dd'T'HH:mm:ss", Instant.ofEpochSecond(1230767760L, 0));
        assertParsedTime("2008-12-31T23:59:00", "java:uuuu-MM-dd'T'HH:mm:ss", Instant.ofEpochSecond(1230767940L, 0));
        assertParsedTime("2008-12-31T23:59:59", "java:uuuu-MM-dd'T'HH:mm:ss", Instant.ofEpochSecond(1230767999L, 0));
        // Leap second is not available in "java:".
        // assertParsedTime("2008-12-31T23:59:60", "java:uuuu-MM-dd'T'HH:mm:ss", Instant.ofEpochSecond(1230768000L, 0));
        assertParsedTime("2009-01-01T00:00:00", "java:uuuu-MM-dd'T'HH:mm:ss", Instant.ofEpochSecond(1230768000L, 0));
        assertParsedTime("2009-01-01T00:00:01", "java:uuuu-MM-dd'T'HH:mm:ss", Instant.ofEpochSecond(1230768001L, 0));
        assertParsedTime("2009-01-01T00:01:00", "java:uuuu-MM-dd'T'HH:mm:ss", Instant.ofEpochSecond(1230768060L, 0));
        assertParsedTime("2009-01-01T00:03:00", "java:uuuu-MM-dd'T'HH:mm:ss", Instant.ofEpochSecond(1230768180L, 0));
    }

    @Test
    public void testLarge() {
        assertParsedTime("-999999999-01-01T00:00:00", "%Y-%m-%dT%H:%M:%S", Instant.ofEpochSecond(-31557014135596800L, 0));
        assertFailToParse("-1000000000-12-31T23:59:59", "%Y-%m-%dT%H:%M:%S");
        assertParsedTime("999999999-12-31T23:59:59", "%Y-%m-%dT%H:%M:%S", Instant.ofEpochSecond(31556889832780799L, 0));
        assertFailToParse("1000000000-01-01T00:00:00", "%Y-%m-%dT%H:%M:%S");

        assertParsedTime("9223372036854775", "%s", Instant.ofEpochSecond(9223372036854775L, 0));
        assertParsedTime("9223372036854776", "%s", Instant.ofEpochSecond(9223372036854776L, 0));
        assertParsedTime("31556889832780799", "%s", Instant.ofEpochSecond(31556889832780799L, 0));
        assertFailToParse("31556889832780800", "%s");  // To succeed? 999999999-12-31T23:59:59 + 1s
        assertFailToParse("31556889864403199", "%s");  // To succeed? Instant.MAX.
        assertFailToParse("31556889864403200", "%s");

        assertParsedTime("-9223372036854775", "%s", Instant.ofEpochSecond(-9223372036854775L, 0));
        assertParsedTime("-9223372036854776", "%s", Instant.ofEpochSecond(-9223372036854776L, 0));
        assertParsedTime("-31556889832780799", "%s", Instant.ofEpochSecond(-31556889832780799L, 0));
        assertParsedTime("-31556889832780800", "%s", Instant.ofEpochSecond(-31556889832780800L, 0));  // Sure

        assertParsedTime("-31556889864403199", "%s", Instant.ofEpochSecond(-31556889864403199L, 0));
        assertFailToParse("-31556889864403200", "%s");  // To succeed? -(Instant.MAX + 1)
        assertFailToParse("-31557014135596799", "%s");  // To succeed? -999999999-01-01T00:00:00
        assertFailToParse("-31557014167219200", "%s");  // To succeed? Instant.MIN.
        assertFailToParse("-31557014167219201", "%s");

        assertParsedTime("9223372036854775807", "%Q", Instant.ofEpochSecond(9223372036854775L, 807000000));
        assertFailToParse("9223372036854775808", "%Q");
        assertParsedTime("-9223372036854775807", "%Q", Instant.ofEpochSecond(-9223372036854776L, 193000000));
        assertFailToParse("-9223372036854775808", "%Q");
    }

    @Test
    public void testRubyLarge() {
        assertParsedTime("-999999999-01-01T00:00:00", "ruby:%Y-%m-%dT%H:%M:%S", Instant.ofEpochSecond(-31557014135596800L, 0));
        assertFailToParse("-1000000000-12-31T23:59:59", "ruby:%Y-%m-%dT%H:%M:%S");
        assertParsedTime("999999999-12-31T23:59:59", "ruby:%Y-%m-%dT%H:%M:%S", Instant.ofEpochSecond(31556889832780799L, 0));
        assertFailToParse("1000000000-01-01T00:00:00", "ruby:%Y-%m-%dT%H:%M:%S");

        assertParsedTime("9223372036854775", "ruby:%s", Instant.ofEpochSecond(9223372036854775L, 0));
        assertParsedTime("9223372036854776", "ruby:%s", Instant.ofEpochSecond(9223372036854776L, 0));
        assertParsedTime("31556889832780799", "ruby:%s", Instant.ofEpochSecond(31556889832780799L, 0));
        assertFailToParse("31556889832780800", "ruby:%s");  // To succeed? 999999999-12-31T23:59:59 + 1s
        assertFailToParse("31556889864403199", "ruby:%s");  // To succeed? Instant.MAX.
        assertFailToParse("31556889864403200", "ruby:%s");

        assertParsedTime("-9223372036854775", "ruby:%s", Instant.ofEpochSecond(-9223372036854775L, 0));
        assertParsedTime("-9223372036854776", "ruby:%s", Instant.ofEpochSecond(-9223372036854776L, 0));
        assertParsedTime("-31556889832780799", "ruby:%s", Instant.ofEpochSecond(-31556889832780799L, 0));
        assertParsedTime("-31556889832780800", "ruby:%s", Instant.ofEpochSecond(-31556889832780800L, 0));  // Sure

        assertParsedTime("-31556889864403199", "ruby:%s", Instant.ofEpochSecond(-31556889864403199L, 0));
        assertFailToParse("-31556889864403200", "ruby:%s");  // To succeed? -(Instant.MAX + 1)
        assertFailToParse("-31557014135596799", "ruby:%s");  // To succeed? -999999999-01-01T00:00:00
        assertFailToParse("-31557014167219200", "ruby:%s");  // To succeed? Instant.MIN.
        assertFailToParse("-31557014167219201", "ruby:%s");

        assertParsedTime("9223372036854775807", "ruby:%Q", Instant.ofEpochSecond(9223372036854775L, 807000000));
        assertFailToParse("9223372036854775808", "ruby:%Q");
        assertParsedTime("-9223372036854775807", "ruby:%Q", Instant.ofEpochSecond(-9223372036854776L, 193000000));
        assertFailToParse("-9223372036854775808", "ruby:%Q");
    }

    @Test
    public void testJavaLarge() {
        assertParsedTime("-999999999-01-01T00:00:00", "java:uuuuuuuuu-MM-dd'T'HH:mm:ss",
                         Instant.ofEpochSecond(-31557014135596800L, 0));
        assertFailToParse("-1000000000-12-31T23:59:59", "java:uuuuuuuuuu-MM-dd'T'HH:mm:ss");
        assertParsedTime("999999999-12-31T23:59:59", "java:uuuuuuuuu-MM-dd'T'HH:mm:ss",
                         Instant.ofEpochSecond(31556889832780799L, 0));
        assertFailToParse("1000000000-01-01T00:00:00", "java:uuuuuuuuuu-MM-dd'T'HH:mm:ss");
    }

    @Test
    public void testSubseconds() {
        assertFailToParse("2007-08-01T00:00:00.", "%Y-%m-%dT%H:%M:%S.%N");
        assertFailToParse("2007-08-01T00:00:00.-777777777", "%Y-%m-%dT%H:%M:%S.%N");
        assertParsedTime("2007-08-01T00:00:00.777777777",
                         "%Y-%m-%dT%H:%M:%S.%N",
                         Instant.ofEpochSecond(1185926400L, 777777777));
        assertParsedTime("2007-08-01T00:00:00.77777777777777",
                         "%Y-%m-%dT%H:%M:%S.%N",
                         Instant.ofEpochSecond(1185926400L, 777777777));
    }

    @Test
    public void testRubySubseconds() {
        assertFailToParse("2007-08-01T00:00:00.", "ruby:%Y-%m-%dT%H:%M:%S.%N");
        assertFailToParse("2007-08-01T00:00:00.-777777777", "ruby:%Y-%m-%dT%H:%M:%S.%N");
        assertParsedTime("2007-08-01T00:00:00.777777777",
                         "ruby:%Y-%m-%dT%H:%M:%S.%N",
                         Instant.ofEpochSecond(1185926400L, 777777777));
        assertParsedTime("2007-08-01T00:00:00.77777777777777",
                         "ruby:%Y-%m-%dT%H:%M:%S.%N",
                         Instant.ofEpochSecond(1185926400L, 777777777));
    }

    @Test
    public void testJavaSubseconds() {
        assertFailToParse("2007-08-01T00:00:00.", "java:uuuu-MM-dd'T'HH:mm:ss.nnnnnnnnn");
        assertFailToParse("2007-08-01T00:00:00.-777777777", "java:uuuu-MM-dd'T'HH:mm:ss.nnnnnnnnn");
        assertParsedTime("2007-08-01T00:00:00.777777777",
                         "java:uuuu-MM-dd'T'HH:mm:ss.nnnnnnnnn",
                         Instant.ofEpochSecond(1185926400L, 777777777));
        assertFailToParse("2007-08-01T00:00:00.77777777777777", "java:uuuu-MM-dd'T'HH:mm:ss.nnnnnnnnn");  // Fail.
    }

    @Test
    public void testDateTimeFromInstant() {
        final TimestampFormatter formatter = TimestampFormatter.builder("%Q.%N", true).build();
        final Instant instant = formatter.parse("1500000000456.111111111");

        final OffsetDateTime datetime = instant.atOffset(ZoneOffset.UTC);
        assertEquals(OffsetDateTime.of(2017, 7, 14, 02, 40, 00, 567111111, ZoneOffset.UTC), datetime);
    }

    @Test
    public void testRubyDateTimeFromInstant() {
        final TimestampFormatter formatter = TimestampFormatter.builder("ruby:%Q.%N", true).build();
        final Instant instant = formatter.parse("1500000000456.111111111");

        final OffsetDateTime datetime = instant.atOffset(ZoneOffset.UTC);
        assertEquals(OffsetDateTime.of(2017, 7, 14, 02, 40, 00, 567111111, ZoneOffset.UTC), datetime);
    }

    @Test
    public void testOffsets() {
        assertParsedTime("2019-05-03T00:00:00-00:00", "%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2019, 5, 3, 0, 0, 0, 0, ZoneOffset.UTC).toInstant());

        // Just "-5" works in legacy.
        assertParsedTime("2019-05-03T00:00:00-5", "%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2019, 5, 3, 5, 0, 0, 0, ZoneOffset.UTC).toInstant());

        assertParsedTime("2019-05-03T00:15:24-01:23:45", "%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2019, 5, 3, 1, 39, 9, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2019-05-03T23:00:00-01:00", "%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2019, 5, 4, 0, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2019-05-03T01:00:00-07:00", "%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2019, 5, 3, 8, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2019-05-03T23:00:00-07:00", "%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2019, 5, 4, 6, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2019-05-03T01:14:19+07:49:12", "%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2019, 5, 2, 17, 25, 7, 0, ZoneOffset.UTC).toInstant());

        // Go forward through the leap year day (Feb 28 or Feb 29).
        assertParsedTime("2000-02-28T23:00:00-05", "%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2000, 2, 29, 4, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2001-02-28T23:00:00-05:00", "%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2001, 3, 1, 4, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2004-02-28T23:00:00-05:00", "%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2004, 2, 29, 4, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2100-02-28T23:00:00-05:00", "%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2100, 3, 1, 4, 0, 0, 0, ZoneOffset.UTC).toInstant());

        // Go back through the leap year day (Feb 28 or Feb 29).
        assertParsedTime("2000-03-01T05:00:00+09", "%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2000, 2, 29, 20, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2001-03-01T05:00:00+09:00", "%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2001, 2, 28, 20, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2004-03-01T05:00:00+09:00", "%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2004, 2, 29, 20, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2100-03-01T05:00:00+09:00", "%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2100, 2, 28, 20, 0, 0, 0, ZoneOffset.UTC).toInstant());

        // Carry up to the year.
        assertParsedTime("2019-01-01T05:00:00+09:00", "%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2018, 12, 31, 20, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2019-12-31T21:00:00-07:00", "%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2020, 1, 1, 4, 0, 0, 0, ZoneOffset.UTC).toInstant());
    }

    @Test
    public void testRubyOffsets() {
        assertParsedTime("2019-05-03T00:00:00-00:00", "ruby:%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2019, 5, 3, 0, 0, 0, 0, ZoneOffset.UTC).toInstant());

        // Just "-5" should not work.
        assertParsedTime("2019-05-03T00:00:00-5", "ruby:%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2019, 5, 3, 0, 0, 0, 0, ZoneOffset.UTC).toInstant());

        assertParsedTime("2019-05-03T00:15:24-01:23:45", "ruby:%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2019, 5, 3, 1, 39, 9, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2019-05-03T23:00:00-01:00", "ruby:%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2019, 5, 4, 0, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2019-05-03T01:00:00-07:00", "ruby:%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2019, 5, 3, 8, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2019-05-03T23:00:00-07:00", "ruby:%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2019, 5, 4, 6, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2019-05-03T01:14:19+07:49:12", "ruby:%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2019, 5, 2, 17, 25, 7, 0, ZoneOffset.UTC).toInstant());

        // Go forward through the leap year day (Feb 28 or Feb 29).
        assertParsedTime("2000-02-28T23:00:00-05", "ruby:%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2000, 2, 29, 4, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2001-02-28T23:00:00-05:00", "ruby:%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2001, 3, 1, 4, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2004-02-28T23:00:00-05:00", "ruby:%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2004, 2, 29, 4, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2100-02-28T23:00:00-05:00", "ruby:%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2100, 3, 1, 4, 0, 0, 0, ZoneOffset.UTC).toInstant());

        // Go back through the leap year day (Feb 28 or Feb 29).
        assertParsedTime("2000-03-01T05:00:00+09", "ruby:%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2000, 2, 29, 20, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2001-03-01T05:00:00+09:00", "ruby:%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2001, 2, 28, 20, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2004-03-01T05:00:00+09:00", "ruby:%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2004, 2, 29, 20, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2100-03-01T05:00:00+09:00", "ruby:%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2100, 2, 28, 20, 0, 0, 0, ZoneOffset.UTC).toInstant());

        // Carry up to the year.
        assertParsedTime("2019-01-01T05:00:00+09:00", "ruby:%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2018, 12, 31, 20, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2019-12-31T21:00:00-07:00", "ruby:%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2020, 1, 1, 4, 0, 0, 0, ZoneOffset.UTC).toInstant());
    }

    @Test
    public void testJavaOffsets() {
        assertParsedTime("2019-05-03T00:00:00-00:00", "java:uuuu-MM-dd'T'HH:mm:ssXXXXX",
                         OffsetDateTime.of(2019, 5, 3, 0, 0, 0, 0, ZoneOffset.UTC).toInstant());

        // Just "-5" should not work.
        assertFailToParse("2019-05-03T00:00:00-5", "java:uuuu-MM-dd'T'HH:mm:ssXX");

        assertParsedTime("2019-05-03T00:15:24-01:23:45", "java:uuuu-MM-dd'T'HH:mm:ssXXXXX",
                         OffsetDateTime.of(2019, 5, 3, 1, 39, 9, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2019-05-03T23:00:00-01:00", "java:uuuu-MM-dd'T'HH:mm:ssXXXXX",
                         OffsetDateTime.of(2019, 5, 4, 0, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2019-05-03T01:00:00-07:00", "java:uuuu-MM-dd'T'HH:mm:ssXXXXX",
                         OffsetDateTime.of(2019, 5, 3, 8, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2019-05-03T23:00:00-07:00", "java:uuuu-MM-dd'T'HH:mm:ssXXXXX",
                         OffsetDateTime.of(2019, 5, 4, 6, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2019-05-03T01:14:19+07:49:12", "java:uuuu-MM-dd'T'HH:mm:ssXXXXX",
                         OffsetDateTime.of(2019, 5, 2, 17, 25, 7, 0, ZoneOffset.UTC).toInstant());

        // Just "-05" should not work.
        assertFailToParse("2000-02-28T23:00:00-05", "java:uuuu-MM-dd'T'HH:mm:ssXX");
        // Go forward through the leap year day (Feb 28 or Feb 29).
        assertParsedTime("2001-02-28T23:00:00-0500", "java:uuuu-MM-dd'T'HH:mm:ssXX",
                         OffsetDateTime.of(2001, 3, 1, 4, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2001-02-28T23:00:00-05:00", "java:uuuu-MM-dd'T'HH:mm:ssXXXXX",
                         OffsetDateTime.of(2001, 3, 1, 4, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2004-02-28T23:00:00-05:00", "java:uuuu-MM-dd'T'HH:mm:ssXXXXX",
                         OffsetDateTime.of(2004, 2, 29, 4, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2100-02-28T23:00:00-05:00", "java:uuuu-MM-dd'T'HH:mm:ssXXXXX",
                         OffsetDateTime.of(2100, 3, 1, 4, 0, 0, 0, ZoneOffset.UTC).toInstant());

        // Just "+09" should not work.
        assertFailToParse("2000-03-01T05:00:00+09", "java:uuuu-MM-dd'T'HH:mm:ssXX");
        // Go back through the leap year day (Feb 28 or Feb 29).
        assertParsedTime("2001-03-01T05:00:00+0900", "java:uuuu-MM-dd'T'HH:mm:ssXX",
                         OffsetDateTime.of(2001, 2, 28, 20, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2001-03-01T05:00:00+09:00", "java:uuuu-MM-dd'T'HH:mm:ssXXXXX",
                         OffsetDateTime.of(2001, 2, 28, 20, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2004-03-01T05:00:00+09:00", "java:uuuu-MM-dd'T'HH:mm:ssXXXXX",
                         OffsetDateTime.of(2004, 2, 29, 20, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2100-03-01T05:00:00+09:00", "java:uuuu-MM-dd'T'HH:mm:ssXXXXX",
                         OffsetDateTime.of(2100, 2, 28, 20, 0, 0, 0, ZoneOffset.UTC).toInstant());

        // Carry up to the year.
        assertParsedTime("2019-01-01T05:00:00+09:00", "java:uuuu-MM-dd'T'HH:mm:ssXXXXX",
                         OffsetDateTime.of(2018, 12, 31, 20, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2019-12-31T21:00:00-07:00", "java:uuuu-MM-dd'T'HH:mm:ssXXXXX",
                         OffsetDateTime.of(2020, 1, 1, 4, 0, 0, 0, ZoneOffset.UTC).toInstant());
    }

    @Test
    public void testDefaultOffsets() {
        final TimestampFormatter formatter = createOffsetFormatter("%Y-%m-%dT%H:%M:%S", ZoneOffset.ofHours(9));
        assertEquals(formatter.parse("2000-03-01T05:00:00"),
                     OffsetDateTime.of(2000, 2, 29, 20, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertEquals(formatter.parse("2001-03-01T05:00:00"),
                     OffsetDateTime.of(2001, 2, 28, 20, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertEquals(formatter.parse("2004-03-01T05:00:00"),
                     OffsetDateTime.of(2004, 2, 29, 20, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertEquals(formatter.parse("2100-03-01T05:00:00"),
                     OffsetDateTime.of(2100, 2, 28, 20, 0, 0, 0, ZoneOffset.UTC).toInstant());
    }

    @Test
    public void testRubyDefaultOffsets() {
        final TimestampFormatter formatter = createOffsetFormatter("ruby:%Y-%m-%dT%H:%M:%S", ZoneOffset.ofHours(9));
        assertEquals(formatter.parse("2000-03-01T05:00:00"),
                     OffsetDateTime.of(2000, 2, 29, 20, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertEquals(formatter.parse("2001-03-01T05:00:00"),
                     OffsetDateTime.of(2001, 2, 28, 20, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertEquals(formatter.parse("2004-03-01T05:00:00"),
                     OffsetDateTime.of(2004, 2, 29, 20, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertEquals(formatter.parse("2100-03-01T05:00:00"),
                     OffsetDateTime.of(2100, 2, 28, 20, 0, 0, 0, ZoneOffset.UTC).toInstant());
    }

    @Test
    public void testJavaDefaultOffsets() {
        final TimestampFormatter formatter = createOffsetFormatter("java:uuuu-MM-dd'T'HH:mm:ss", ZoneOffset.ofHours(9));
        assertEquals(formatter.parse("2000-03-01T05:00:00"),
                     OffsetDateTime.of(2000, 2, 29, 20, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertEquals(formatter.parse("2001-03-01T05:00:00"),
                     OffsetDateTime.of(2001, 2, 28, 20, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertEquals(formatter.parse("2004-03-01T05:00:00"),
                     OffsetDateTime.of(2004, 2, 29, 20, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertEquals(formatter.parse("2100-03-01T05:00:00"),
                     OffsetDateTime.of(2100, 2, 28, 20, 0, 0, 0, ZoneOffset.UTC).toInstant());
    }

    @Test
    public void testDefaultOffsetsWithParsingZones() {
        assertEquals(createZoneIdFormatter("%Y-%m-%dT%H:%M:%S%Z", ZoneId.of("Asia/Tokyo")).parse("2000-03-01T05:00:00Z"),
                     OffsetDateTime.of(2000, 3, 1, 5, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertEquals(createZoneIdFormatter("%Y-%m-%dT%H:%M:%S%z", ZoneId.of("Asia/Tokyo")).parse("2000-03-01T05:00:00Z"),
                     OffsetDateTime.of(2000, 3, 1, 5, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertEquals(createZoneIdFormatter("%Y-%m-%dT%H:%M:%S%:z", ZoneId.of("Asia/Tokyo")).parse("2000-03-01T05:00:00Z"),
                     OffsetDateTime.of(2000, 3, 1, 5, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertEquals(createZoneIdFormatter("%Y-%m-%dT%H:%M:%S%::z", ZoneId.of("Asia/Tokyo")).parse("2000-03-01T05:00:00Z"),
                     OffsetDateTime.of(2000, 3, 1, 5, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertEquals(createZoneIdFormatter("%Y-%m-%dT%H:%M:%S%:::z", ZoneId.of("Asia/Tokyo")).parse("2000-03-01T05:00:00Z"),
                     OffsetDateTime.of(2000, 3, 1, 5, 0, 0, 0, ZoneOffset.UTC).toInstant());

        assertEquals(createZoneIdFormatter("%Y-%m-%dT%H:%M:%S %Z", ZoneId.of("Europe/Paris")).parse("2000-03-01T05:00:00 UTC"),
                     OffsetDateTime.of(2000, 3, 1, 5, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertEquals(createZoneIdFormatter("%Y-%m-%dT%H:%M:%S %Z", ZoneId.of("Europe/Paris")).parse("2000-03-01T05:00:00 JST"),
                     OffsetDateTime.of(2000, 3, 1, 5, 0, 0, 0, ZoneOffset.ofHours(9)).toInstant());
    }

    @Test
    public void testRubyDefaultOffsetsWithParsingZones() {
        assertEquals(createOffsetFormatter("ruby:%Y-%m-%dT%H:%M:%S%Z", ZoneOffset.ofHours(9)).parse("2000-03-01T05:00:00Z"),
                     OffsetDateTime.of(2000, 3, 1, 5, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertEquals(createOffsetFormatter("ruby:%Y-%m-%dT%H:%M:%S%z", ZoneOffset.ofHours(9)).parse("2000-03-01T05:00:00Z"),
                     OffsetDateTime.of(2000, 3, 1, 5, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertEquals(createOffsetFormatter("ruby:%Y-%m-%dT%H:%M:%S%:z", ZoneOffset.ofHours(9)).parse("2000-03-01T05:00:00Z"),
                     OffsetDateTime.of(2000, 3, 1, 5, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertEquals(createOffsetFormatter("ruby:%Y-%m-%dT%H:%M:%S%::z", ZoneOffset.ofHours(9)).parse("2000-03-01T05:00:00Z"),
                     OffsetDateTime.of(2000, 3, 1, 5, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertEquals(createOffsetFormatter("ruby:%Y-%m-%dT%H:%M:%S%:::z", ZoneOffset.ofHours(9)).parse("2000-03-01T05:00:00Z"),
                     OffsetDateTime.of(2000, 3, 1, 5, 0, 0, 0, ZoneOffset.UTC).toInstant());

        assertEquals(createOffsetFormatter("ruby:%Y-%m-%dT%H:%M:%S %Z", ZoneOffset.ofHours(2)).parse("2000-03-01T05:00:00 UTC"),
                     OffsetDateTime.of(2000, 3, 1, 5, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertEquals(createOffsetFormatter("ruby:%Y-%m-%dT%H:%M:%S %Z", ZoneOffset.ofHours(2)).parse("2000-03-01T05:00:00 +09:00"),
                     OffsetDateTime.of(2000, 3, 1, 5, 0, 0, 0, ZoneOffset.ofHours(9)).toInstant());
    }

    private void testJavaToParse(final String string, final String format, final long second, final int nanoOfSecond) {
        final TimestampFormatter formatter = TimestampFormatter.builderWithJava(format).build();
        final Instant timestamp = formatter.parse(string);
        assertEquals(second, timestamp.getEpochSecond());
        assertEquals(nanoOfSecond, timestamp.getNano());
    }

    private void testJavaToParse(final String string, final String format, final long second) {
        testJavaToParse(string, format, second, 0);
    }

    private void failJavaToParse(final String string, final String format) {
        final TimestampFormatter formatter = TimestampFormatter.builderWithJava(format).build();
        try {
            formatter.parse(string);
        } catch (final DateTimeParseException ex) {
            return;
        } catch (final DateTimeException ex) {
            return;
        }
        fail();
    }

    private void testRubyToParse(final String string, final String format, final long second, final int nanoOfSecond) {
        final TimestampFormatter formatter = TimestampFormatter.builderWithRuby(format).build();
        final Instant timestamp = formatter.parse(string);
        assertEquals(second, timestamp.getEpochSecond());
        assertEquals(nanoOfSecond, timestamp.getNano());
    }

    private void testRubyToParse(final String string, final String format, final long second) {
        testRubyToParse(string, format, second, 0);
    }

    private void failRubyToParse(final String string, final String format) {
        final TimestampFormatter formatter = TimestampFormatter.builderWithRuby(format).build();
        try {
            formatter.parse(string);
        } catch (final DateTimeParseException ex) {
            return;
        }
        fail();
    }

    private void testToParse(final String string, final String format, final long second, final int nanoOfSecond) {
        // Note: UTC.
        final TimestampFormatter formatter = TimestampFormatter.builder(format, true).setDefaultDate(4567, 1, 23).build();
        final Instant timestamp = formatter.parse(string);
        assertEquals(second, timestamp.getEpochSecond());
        assertEquals(nanoOfSecond, timestamp.getNano());
    }

    private void testToParse(final String string, final String format, final long second) {
        testToParse(string, format, second, 0);
    }

    private void failToParse(final String string, final String format) {
        // Note: UTC.
        final TimestampFormatter formatter = TimestampFormatter.builder(format, true).setDefaultDate(4567, 1, 23).build();
        try {
            formatter.parse(string);
        } catch (final DateTimeParseException ex) {
            return;
        }
        fail();
    }

    private static void assertParsedTime(final String string, final String format, final Instant expected) {
        final TimestampFormatter formatter = TimestampFormatter.builder(format, true).build();
        final Instant actualInstant = formatter.parse(string);
        assertEquals(expected, actualInstant);
    }

    private static void assertFailToParse(final String string, final String format) {
        final TimestampFormatter formatter = TimestampFormatter.builder(format, true).build();
        try {
            formatter.parse(string);
        } catch (final DateTimeParseException ex) {
            return;
        } catch (final DateTimeException ex) {
            return;
        }
        fail();
    }

    private static TimestampFormatter createOffsetFormatter(final String format, final ZoneOffset offset) {
        return TimestampFormatter
                       .builder(format, true)
                       .setDefaultZoneOffset(offset)
                       .build();
    }

    private static TimestampFormatter createZoneIdFormatter(final String format, final ZoneId zone) {
        return TimestampFormatter
                       .builder(format, true)
                       .setDefaultZoneId(zone)
                       .build();
    }
}
