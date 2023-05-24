package com.indeed.proctor.store;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.tmatesoft.svn.core.ISVNDirEntryHandler;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;

import java.util.List;

/**
 * SVNDirEntryHandler that will collect the SVNDirEntries based on the Predicate Filter
 *
 * <p>It will store the parent DirEntry in parent
 */
class FilterableSVNDirEntryHandler implements ISVNDirEntryHandler {
    final Predicate<SVNDirEntry> childFilter;

    private SVNDirEntry parent;
    final List<SVNDirEntry> children = Lists.newArrayList();

    FilterableSVNDirEntryHandler() {
        this(Predicates.<SVNDirEntry>alwaysTrue());
    }

    FilterableSVNDirEntryHandler(final Predicate<SVNDirEntry> childFilter) {
        this.childFilter = childFilter;
    }

    @Override
    public void handleDirEntry(SVNDirEntry svnDirEntry) throws SVNException {
        // from the svn docs: The directory entry for url is reported using an empty path.
        // If SVNDepth.IMMEDIATES, lists its immediate file and directory entries
        // So identify the parent as the one with an empty relative path
        if (StringUtils.isBlank(svnDirEntry.getRelativePath())) {
            this.parent = svnDirEntry;
        } else if (childFilter.apply(svnDirEntry)) {
            this.children.add(svnDirEntry);
        }
    }

    public SVNDirEntry getParent() {
        return parent;
    }

    public List<SVNDirEntry> getChildren() {
        return children;
    }
}
