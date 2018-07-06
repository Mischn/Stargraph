package net.stargraph.model.date;

/*-
 * ==========================License-Start=============================
 * stargraph-model
 * --------------------------------------------------------------------
 * Copyright (C) 2017 Lambda^3
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ==========================License-End===============================
 */

import java.time.LocalDate;

/**
 * A Time-Interval.
 */
public final class TimeRange {
    private LocalDate from;
    private LocalDate to;

    private TimeRange(long from, long to) {
        this(LocalDate.ofEpochDay(from), LocalDate.ofEpochDay(to));
    }

    private TimeRange(LocalDate from, LocalDate to) {
        this.from = from;
        this.to = to;
    }

    public static TimeRange fromTo(long from, long to) {
        return new TimeRange(from, to);
    }

    public static TimeRange fromTo(LocalDate from, LocalDate to) {
        return new TimeRange(from, to);
    }

    public static TimeRange after(LocalDate date) {
        return new TimeRange(LocalDate.ofEpochDay(date.toEpochDay() + 1), LocalDate.now());
    }

    public static TimeRange before(LocalDate date) {
        return new TimeRange(LocalDate.ofEpochDay(Long.MIN_VALUE), LocalDate.ofEpochDay(date.toEpochDay() -1));
    }

    public LocalDate getFrom() {
        return from;
    }

    public LocalDate getTo() {
        return to;
    }

    public boolean containsInterval(TimeRange interval) {
        return (from.isBefore(interval.from) || from.isEqual(interval.from)) && (to.isAfter(interval.to) || to.isEqual(interval.to));
    }

    @Override
    public String toString() {
        return "TimeRange{" +
                "from=" + from +
                ", to=" + to +
                '}';
    }
}
