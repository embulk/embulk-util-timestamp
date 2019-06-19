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

import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

public class TestLegacyDateTimeZones {
    @Test
    public void test() {
        // TODO: Test more practical equality. (Such as "GMT" v.s. "UTC")
        assertEquals(ZoneOffset.UTC, LegacyDateTimeZones.toZoneId("Z"));
        assertEquals(ZoneId.of("Asia/Tokyo"), LegacyDateTimeZones.toZoneId("Asia/Tokyo"));
        assertEquals(ZoneId.of("-05:00"), LegacyDateTimeZones.toZoneId("EST"));
        assertEquals(ZoneId.of("-10:00"), LegacyDateTimeZones.toZoneId("HST"));
        assertEquals(ZoneId.of("Asia/Taipei"), LegacyDateTimeZones.toZoneId("ROC"));
    }
}
