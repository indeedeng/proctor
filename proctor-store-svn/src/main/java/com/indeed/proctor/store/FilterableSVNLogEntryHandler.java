package com.indeed.proctor.store;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;

import java.util.List;

class FilterableSVNLogEntryHandler implements ISVNLogEntryHandler {
    final Predicate<SVNLogEntry> logentryFilter;

    final List<SVNLogEntry> logEntries = Lists.newArrayList();

    FilterableSVNLogEntryHandler() {
        this(Predicates.<SVNLogEntry>alwaysTrue());
    }

    private FilterableSVNLogEntryHandler(final Predicate<SVNLogEntry> logentryFilter) {
        this.logentryFilter = logentryFilter;
    }

    @Override
    public void handleLogEntry(SVNLogEntry entry) throws SVNException {
        if (logentryFilter.apply(entry)) {
            logEntries.add(entry);
        }
    }

    public List<SVNLogEntry> getLogEntries() {
        return logEntries;
    }
}
