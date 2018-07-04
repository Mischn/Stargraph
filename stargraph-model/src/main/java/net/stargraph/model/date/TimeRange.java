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

import java.util.Calendar;
import java.util.Date;

/**
 * A Time-Interval.
 */
public final class TimeRange {
    private Date from;
    private Date to;

    private TimeRange(long from, long to) {
        this(new Date(from), new Date(to));
    }

    private TimeRange(Date from, Date to) {
        this.from = from;
        this.to = to;
    }

    public static TimeRange fromTo(long from, long to) {
        return new TimeRange(from, to);
    }

    public static TimeRange fromTo(Date from, Date to) {
        return new TimeRange(from, to);
    }

    public static TimeRange after(Date date) {
        return new TimeRange(new Date(date.getTime()+1), new Date());
    }

    public static TimeRange before(Date date) {
        return new TimeRange(new Date(Long.MIN_VALUE), new Date(date.getTime()-1));
    }

    public Date getFrom() {
        return from;
    }

    public Date getTo() {
        return to;
    }

    public boolean isInInterval(TimeRange interval) {
        return (from.before(interval.from) || from.equals(interval.from)) && (to.after(interval.to) || to.equals(interval.to));
    }

    @Override
    public String toString() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(from);
        int fromYear = cal.get(Calendar.YEAR);
        int fromMonth = cal.get(Calendar.MONTH)+1;
        int fromDay = cal.get(Calendar.DATE);
        cal.setTime(to);
        int toYear = cal.get(Calendar.YEAR);
        int toMonth = cal.get(Calendar.MONTH)+1;
        int toDay = cal.get(Calendar.DATE);

        return "TimeRange{" +
                "from=" + fromDay + "."+ fromMonth + "." + fromYear +
                ", to=" + toDay + "."+ toMonth + "." + toYear +
                '}';
    }
}
