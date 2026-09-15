package com.stockpilot.model;

/** Contract for anything that can render itself as a human-readable report/log line. */
public interface Reportable {
    String toReportLine();
}
